package com.spundev.webrtcshare.utils

import com.spundev.webrtcshare.extensions.DataChannelEvent
import com.spundev.webrtcshare.extensions.observerFlow
import com.spundev.webrtcshare.repositories.SignalingMessage
import com.spundev.webrtcshare.repositories.SignalingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.properties.Delegates

class WebRTCManager @AssistedInject constructor(
    rtcConnectionFactory: PeerConnectionFactory,
    rtcConfiguration: PeerConnection.RTCConfiguration,
    @Assisted val signalingRepository: SignalingRepository
) {
    @AssistedFactory
    interface Factory {
        fun create(signalingRepository: SignalingRepository): WebRTCManager
    }

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Defines if this WebRTCManager was the one that started the process
    private var isInitiator by Delegates.notNull<Boolean>()

    private var signalingRoom: String? = null

    // Name for this WebRTCManager in logs.
    // This is useful when we have more than one WebRTCManager in the same screen (see LocalDemo)
    private val logsName: String
        get() = if (isInitiator) "Main" else "Seco"

    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // List of messages sent and received
    private val _messages = MutableStateFlow(listOf<String>())
    val messages: StateFlow<List<String>> = _messages

    // The DataChannel used to send and receive messages
    private lateinit var dataChannel: DataChannel

    // keep track of some negotiation state to prevent races and errors
    private var makingOffer = false

    /**
     *  - A polite peer may send out offers, but then responds if an offer arrives from the other peer
     *  with "Okay, never mind, drop my offer and I'll consider yours instead."
     *  - An impolite peer always ignores incoming offers that collide with its own offers. Any time
     *  a collision occurs, the impolite peer wins.
     */
    private val polite = true

    private val peerConnection: MyPeerConnection by lazy {
        // Our custom MyPeerConnection works as a PeerConnection.Observer and as a PeerConnection
        // manager with methods to manage the connection like createOffer, addIceCandidate, etc.
        val peerConnection = MyPeerConnection(
            name = logsName,
            onNegotiationNeeded = {
                // Leave the renegotiation for the initiator
                // TODO: Check if the renegotiation is something the non initiator peer
                //  should be able to do
                if (isInitiator) {
                    makingOffer = true
                    sessionScope.launch {
                        val offer = it.createOffer()
                        it.setLocalDescription(offer)
                        val roomId = signalingRoom
                        requireNotNull(roomId)
                        signalingRepository.sendMessage(
                            isInitiator = isInitiator,
                            roomId = roomId,
                            message = SignalingMessage.SignalingDescription(offer)
                        )
                    }
                    makingOffer = false
                }
            },
            onIceCandidate = { iceCandidate ->
                val roomId = signalingRoom
                requireNotNull(roomId)
                signalingRepository.sendMessage(
                    isInitiator = isInitiator,
                    roomId = roomId,
                    message = SignalingMessage.SignalingCandidate(iceCandidate)
                )
            }
        )

        // Create the WebRTCConnection with the configuration and our new observer
        val rawPeerConnection = requireNotNull(
            rtcConnectionFactory.createPeerConnection(
                /* rtcConfig =*/ rtcConfiguration,
                /* observer = */ peerConnection
            )
        )

        // Set the new PeerConnection in our MyPeerConnection
        peerConnection.initialize(rawPeerConnection)
        peerConnection
    }

    /**
     * Create DataChannel and start listening the signaling server.
     */
    fun start(isInitiator: Boolean, roomId: String): AutoCloseable {
        this.isInitiator = isInitiator
        this.signalingRoom = roomId
        Timber.d("$logsName:[start]")

        val localDataChannelInit = DataChannel.Init().apply {
            id = 0
            ordered = true
            negotiated = true
        }

        dataChannel = peerConnection.createDataChannel("chat", localDataChannelInit)
        sessionScope.launch { collectDataChannelEvents() }
        sessionScope.launch { collectSignalingServerEvents(roomId) }

        return AutoCloseable { close() }
    }

    /**
     * Collect DataChannel events
     */
    private suspend fun collectDataChannelEvents() {
        dataChannel.observerFlow().collect { event ->
            when (event) {
                is DataChannelEvent.BufferedAmountChange -> {
                    Timber.d("$logsName:[onBufferedAmountChange] ${event.l}")
                }

                is DataChannelEvent.StateChange -> {
                    Timber.d("$logsName:[onStateChange] ${event.newState}")
                    if (event.newState == DataChannel.State.OPEN) {
                        // Notify connection change
                        _isConnected.value = true
                    } else {
                        // Notify connection change
                        _isConnected.value = false
                    }
                }

                is DataChannelEvent.Message -> {
                    Timber.d("$logsName:[onMessage] isOpen: ${dataChannel.state() == DataChannel.State.OPEN}")
                    val buffer = event.buffer
                    if (buffer?.binary == false) {
                        val destinationByteArray = ByteArray(buffer.data.limit())
                        buffer.data.get(destinationByteArray)
                        val message = String(destinationByteArray)
                        Timber.d("$logsName:[onMessage] message: $message")
                        _messages.update { it + message }
                    }
                }
            }
        }
    }

    /**
     * Collect messages from the [SignalingRepository]
     */
    private suspend fun collectSignalingServerEvents(roomId: String) {
        signalingRepository
            .receiveMessagesFlow(isInitiator, roomId)
            .collect { message ->
                when (message) {
                    is SignalingMessage.SignalingCandidate -> {
                        peerConnection.addIceCandidate(message.candidate)
                    }

                    is SignalingMessage.SignalingDescription -> {
                        // Check if the incoming message is an OFFER
                        val isAnOfferMessage =
                            message.description.type == SessionDescription.Type.OFFER
                        // Check if the current peerConnection is already STABLE
                        val isConnectionStable =
                            peerConnection.signalingState != PeerConnection.SignalingState.STABLE
                        // Decide if we have an offer collision
                        val haveOfferCollision =
                            isAnOfferMessage && (makingOffer || isConnectionStable)

                        // If we have an offer collision, and we are an impolite peer (always
                        // ignores incoming offers that collide with its own offers), skip.
                        val ignoreOffer = !polite && haveOfferCollision
                        if (!ignoreOffer) {
                            peerConnection.setRemoteDescription(message.description)
                            if (message.description.type == SessionDescription.Type.OFFER) {
                                val answer = peerConnection.createAnswer()
                                peerConnection.setLocalDescription(answer)
                                signalingRepository.sendMessage(
                                    isInitiator = isInitiator,
                                    roomId = roomId,
                                    message = SignalingMessage.SignalingDescription(answer)
                                )
                            }
                        }
                    }
                }
            }
    }

    // Send a new message to the other peer
    fun sendMessage(message: String) {
        val buffer = ByteBuffer.wrap(message.toByteArray())
        dataChannel.send(DataChannel.Buffer(buffer, false))
        // Also update our messages list with the new message
        _messages.update { it + message }
    }

    fun close() {
        dataChannel.close()
        peerConnection.close()
        sessionScope.cancel()
    }
}
