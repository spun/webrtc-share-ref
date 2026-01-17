package com.spundev.webrtcshare.utils

import com.spundev.webrtcshare.extensions.addIceCandidateSuspend
import com.spundev.webrtcshare.extensions.createAnswerSuspend
import com.spundev.webrtcshare.extensions.createOfferSuspend
import com.spundev.webrtcshare.extensions.setLocalDescriptionSuspend
import com.spundev.webrtcshare.extensions.setRemoteDescriptionSuspend
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import timber.log.Timber

class MyPeerConnection(
    val name: String,
    private val onNegotiationNeeded: (MyPeerConnection) -> Unit,
    private val onIceCandidate: (IceCandidate) -> Unit,
) : VerbosePeerConnectionObserver() {

    private lateinit var connection: PeerConnection

    val signalingState: PeerConnection.SignalingState
        get() = connection.signalingState()

    // Store IceCandidates received before the RemoteDescription is set.
    private val pendingIceCandidatesMutex = Mutex()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    // Receive the WebRTC PeerConnection
    fun initialize(peerConnection: PeerConnection) {
        this.connection = peerConnection
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Timber.d("$name:[onIceCandidate] $candidate")
        if (candidate == null) return
        onIceCandidate.invoke(candidate)
    }

    override fun onRenegotiationNeeded() {
        Timber.d("$name:[onRenegotiationNeeded]")
        onNegotiationNeeded.invoke(this)
    }

    suspend fun createOffer(): SessionDescription {
        Timber.d("$name:[createOffer]")
        return connection.createOfferSuspend()
    }

    suspend fun createAnswer(): SessionDescription {
        Timber.d("$name:[createAnswer]")
        return connection.createAnswerSuspend()
    }

    suspend fun setLocalDescription(sessionDescription: SessionDescription) {
        Timber.d("$name:[setLocalDescription]: $sessionDescription")
        connection.setLocalDescriptionSuspend(sessionDescription)
    }

    suspend fun setRemoteDescription(sessionDescription: SessionDescription) {
        Timber.d("$name:[setRemoteDescription]: $sessionDescription")
        connection.setRemoteDescriptionSuspend(sessionDescription)
        pendingIceCandidatesMutex.withLock {
            pendingIceCandidates.forEach { iceCandidate ->
                Timber.d("$name:[setRemoteDescription] pendingIceCandidate: $iceCandidate")
                connection.addIceCandidateSuspend(iceCandidate)
            }
            pendingIceCandidates.clear()
        }
    }

    suspend fun addIceCandidate(iceCandidate: IceCandidate) {
        // If we receive request to add an IceCandidates before the RemoteDescription is set, we
        // should save the candidates and wait until setRemoteDescription is called.
        if (connection.remoteDescription == null) {
            Timber.d("$name:[addIceCandidate] stored (no remoteDescription): $iceCandidate")
            pendingIceCandidatesMutex.withLock {
                pendingIceCandidates.add(iceCandidate)
            }
            return
        }
        Timber.d("$name:[addIceCandidate] rtcIceCandidate: $iceCandidate")
        return connection.addIceCandidateSuspend(iceCandidate).also {
            Timber.d("$name:[addIceCandidate] completed: $it")
        }
    }

    fun createDataChannel(label: String, init: DataChannel.Init): DataChannel {
        return connection.createDataChannel(label, init)
    }
}


/**
 * PeerConnection.Observer that logs when any of its methods is called.
 */
abstract class VerbosePeerConnectionObserver : PeerConnection.Observer {

    override fun onIceCandidate(candidate: IceCandidate?) {
        Timber.v("[onIceCandidate]: $candidate")
    }

    override fun onDataChannel(channel: DataChannel?) {
        Timber.v("[onDataChannel]: $channel")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Timber.v("[onIceConnectionReceivingChange]: $receiving")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Timber.v("[onIceConnectionChange] $newState")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Timber.v("[onIceGatheringChange] $newState")
    }

    override fun onAddStream(stream: MediaStream?) {
        Timber.v("[onAddStream]: $stream")
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Timber.v("[onSignalingChange] $newState")
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
        Timber.v("[onIceCandidatesRemoved]: $iceCandidates")
    }

    override fun onRemoveStream(stream: MediaStream?) {
        Timber.v("[onRemoveStream]: $stream")
    }

    override fun onRenegotiationNeeded() {
        Timber.v("[onRenegotiationNeeded]: ")
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        Timber.v("[onAddTrack]: $receiver / $mediaStreams")
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        Timber.v("[onConnectionChange]: $newState")
    }
}
