package com.spundev.webrtcshare.extensions

import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AddIceObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun PeerConnection.addIceCandidateSuspend(
    iceCandidate: IceCandidate
) = suspendCancellableCoroutine { cont ->
    addIceCandidate(
        iceCandidate,
        object : AddIceObserver {
            override fun onAddSuccess() {
                cont.resume(Unit)
            }

            override fun onAddFailure(error: String?) {
                cont.resumeWithException(RuntimeException(error))
            }
        }
    )
}

suspend fun PeerConnection.setRemoteDescriptionSuspend(
    sessionDescription: SessionDescription
) = setValue {
    setRemoteDescription(it, sessionDescription)
}

suspend fun PeerConnection.setLocalDescriptionSuspend(
    sessionDescription: SessionDescription
) = setValue {
    setLocalDescription(it, sessionDescription)
}

suspend fun PeerConnection.createOfferSuspend(
    constraints: MediaConstraints = MediaConstraints()
) = createValue {
    createOffer(it, constraints)
}

suspend fun PeerConnection.createAnswerSuspend(
    constraints: MediaConstraints = MediaConstraints()
) = createValue {
    createAnswer(it, constraints)
}

// ---

/**
 * Utility function to set a [PeerConnection] value suspending.
 * Source: Similar to createValue from GetStream/webrtc-in-jetpack-compose without Result.
 */
private suspend inline fun createValue(
    crossinline call: (SdpObserver) -> Unit
): SessionDescription = suspendCancellableCoroutine {
    val observer = object : SdpObserver {

        override fun onCreateSuccess(description: SessionDescription?) {
            if (description != null) {
                it.resume(description)
            } else {
                it.resumeWithException(RuntimeException("SessionDescription was null"))
            }
        }

        override fun onCreateFailure(message: String?) {
            it.resumeWithException(RuntimeException(message))
        }

        /**
         * These are not used when creating a value.
         */
        override fun onSetSuccess() {
            throw RuntimeException("onSetSuccess was called from createValue")
        }

        override fun onSetFailure(p0: String?) {
            throw RuntimeException("onSetFailure was called from createValue")
        }
    }

    call(observer)
}

/**
 * Utility function to set a [PeerConnection] value suspending.
 * Source: Similar to setValue from GetStream/webrtc-in-jetpack-compose without Result.
 */
private suspend inline fun setValue(
    crossinline call: (SdpObserver) -> Unit
): Unit = suspendCancellableCoroutine {
    val observer = object : SdpObserver {

        override fun onSetSuccess() {
            it.resume(Unit)
        }

        override fun onSetFailure(message: String?) {
            it.resumeWithException(RuntimeException(message))
        }

        /**
         * These are not used when setting a value.
         */
        override fun onCreateSuccess(p0: SessionDescription?) {
            throw RuntimeException("onCreateSuccess was called from setValue")
        }

        override fun onCreateFailure(p0: String?) {
            throw RuntimeException("onCreateFailure was called from setValue")
        }
    }

    call(observer)
}
