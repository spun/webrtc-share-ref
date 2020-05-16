package com.spundev.webrtcshare.extensions

import com.spundev.webrtcshare.utils.MySessionDescriptionObserver
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.SignalingState.*
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 *
 */
suspend fun PeerConnection.createAndSetLocalDescription(): SessionDescription {
    val description = createLocalDescription()
    setLocalDescriptionSuspend(description)
    return description
}

/**
 * Creates a new offer os answer as needed.
 * The rules to decide if an offer or an answer is created are extracted from the w3c webRTC
 * documentation. {@linktourl http://google.com}
 * @return A [SessionDescription] that can be set with [PeerConnection.setLocalDescription]
 * @see <a href="https://www.w3.org/TR/webrtc/#dom-peerconnection-setlocaldescription">w3c webRTC</a>
 */
suspend fun PeerConnection.createLocalDescription() =
    suspendCoroutine<SessionDescription> { cont ->

        val sessionDescriptionObserver = object : MySessionDescriptionObserver() {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    cont.resume(p0)
                } else {
                    cont.resumeWithException(Exception("The SessionDescription created was null"))
                }
            }

            override fun onCreateFailure(p0: String?) {
                cont.resumeWithException(Exception(p0))
            }
        }

        // The rules to decide if we need an offer or an answer are from the w3c webRTC documentation
        when (this.signalingState()) {
            STABLE, HAVE_LOCAL_OFFER, HAVE_LOCAL_PRANSWER -> createOffer(
                sessionDescriptionObserver,
                MediaConstraints()
            )
            else -> createAnswer(sessionDescriptionObserver, MediaConstraints())
        }
    }


/**
 * Sets the Local SessionDescription with the value passed as parameter
 * @param description The [SessionDescription] we want to set as local description
 * @return The same [SessionDescription] passed as parameter
 */
suspend fun PeerConnection.setLocalDescriptionSuspend(description: SessionDescription) =
    suspendCoroutine<SessionDescription> { cont ->
        val sessionDescriptionObserver = object : MySessionDescriptionObserver() {
            override fun onSetSuccess() {
                cont.resume(description)
            }

            override fun onSetFailure(p0: String?) {
                cont.resumeWithException(Exception(p0))
            }
        }
        setLocalDescription(sessionDescriptionObserver, description)
    }

suspend fun PeerConnection.setRemoteDescriptionSuspend(description: SessionDescription) =
    suspendCoroutine<SessionDescription> { cont ->
        val sessionDescriptionObserver = object : MySessionDescriptionObserver() {
            override fun onSetSuccess() {
                cont.resume(description)
            }

            override fun onSetFailure(p0: String?) {
                cont.resumeWithException(Exception(p0))
            }
        }
        setRemoteDescription(sessionDescriptionObserver, description)
    }

