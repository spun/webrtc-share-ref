package com.spundev.webrtcshare.utils

import android.util.Log
import com.spundev.webrtcshare.extensions.createAndSetLocalDescription
import com.spundev.webrtcshare.extensions.setRemoteDescriptionSuspend
import com.spundev.webrtcshare.repositories.RealTimeSignalingRepository
import com.spundev.webrtcshare.repositories.SignalingMessage.SignalingCandidate
import com.spundev.webrtcshare.repositories.SignalingMessage.SignalingDescription
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.webrtc.*
import java.nio.ByteBuffer

class WebRTCConnection(val roomId: String, val isInitiator: Boolean) {

    // Connection state
    val isConnected = ConflatedBroadcastChannel(false)

    // List of messages sent and received
    val messages = ConflatedBroadcastChannel(listOf<String>())

    // Signaling server we use to kick start the WebRTC connection
    val signalingRepository = RealTimeSignalingRepository(isInitiator)

    // The DataChannel used to send an receive messages
    var myDataChannel: DataChannel? = null

    suspend fun start() = coroutineScope {
        // keep track of some negotiation state to prevent races and errors
        var ignoreOffer: Boolean
        var makingOffer = false
        // The polite peer uses rollback to avoid collision with an incoming offer.
        // The impolite peer ignores an incoming offer when this would collide with its own.
        val polite = true

        // Create connection
        val rtcConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        // Use stun servers
        val iceServers = mutableListOf<PeerConnection.IceServer>(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        // PeerConnection creation
        val peerConnectionConfig = PeerConnection.RTCConfiguration(iceServers)
        lateinit var peerConnection: PeerConnection
        peerConnection = rtcConnectionFactory.createPeerConnection(
            peerConnectionConfig,
            object : MyPeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    p0?.let { candidate ->
                        // Send the candidate to the other peer using our signaling server
                        signalingRepository.sendMessage(roomId, SignalingCandidate(candidate))
                    }
                }

                override fun onRenegotiationNeeded() {
                    // Leave the renegotiation for the initiator
                    // TODO: Check if the renegotiation is something the non initiator peer should be able to do
                    if (isInitiator) {
                        try {
                            makingOffer = true
                            launch {
                                val description = peerConnection.createAndSetLocalDescription()
                                signalingRepository.sendMessage(
                                    roomId,
                                    SignalingDescription(description)
                                )
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "onRenegotiationNeeded: ", e)
                        } finally {
                            makingOffer = false
                        }
                    }
                }
            }
        )!!

        // Channel creation
        val localDataChannelInit = DataChannel.Init()
        localDataChannelInit.id = 0
        localDataChannelInit.negotiated = true
        val dataChannel = peerConnection.createDataChannel("chat", localDataChannelInit)
        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer?) {
                if (buffer?.binary == false) {
                    val destinationByteArray = ByteArray(buffer.data.limit())
                    buffer.data.get(destinationByteArray)
                    val message = String(destinationByteArray)
                    messages.offer(messages.value + message)
                }
            }

            override fun onBufferedAmountChange(p0: Long) {
                Log.d(TAG, "onBufferedAmountChange: $p0")
            }

            override fun onStateChange() {
                Log.d(TAG, "onStateChange: ")
                if (dataChannel.state() == DataChannel.State.OPEN) {
                    // Notify connection change
                    isConnected.offer(true)
                    // Save channel
                    myDataChannel = dataChannel
                } else {
                    // Notify connection change
                    isConnected.offer(false)
                }
            }
        })

        launch {
            // Signaling server new message received flow
            signalingRepository.receiveMessagesFlow(roomId).collect { message ->
                when (message) {
                    // If the new message contains a SessionDescription
                    is SignalingDescription -> {
                        val offerCollision =
                            message.description.type == SessionDescription.Type.OFFER && (makingOffer || peerConnection.signalingState() != PeerConnection.SignalingState.STABLE)

                        ignoreOffer = !polite && offerCollision

                        if (!ignoreOffer) {
                            peerConnection.setRemoteDescriptionSuspend(message.description)
                            if (message.description.type == SessionDescription.Type.OFFER) {
                                peerConnection.createAndSetLocalDescription()
                                signalingRepository.sendMessage(
                                    roomId,
                                    SignalingDescription(peerConnection.localDescription)
                                )
                            }


                        }
                    }
                    // If the new message contains an IceCandidate
                    is SignalingCandidate -> {
                        peerConnection.addIceCandidate(message.candidate)
                    }
                }
            }
        }
    }

    // Send a new message using WebRTC to the other peer
    fun sendMessage(message: String) {
        myDataChannel?.let { dataChannel ->
            val buffer = ByteBuffer.wrap(message.toByteArray())
            dataChannel.send(DataChannel.Buffer(buffer, false))
            // Also update our messages list with the new message
            messages.offer(messages.value + message)
        }
    }
}

private const val TAG = "WebRTCConnection"