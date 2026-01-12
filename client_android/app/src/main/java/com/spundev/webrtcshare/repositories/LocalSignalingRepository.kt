package com.spundev.webrtcshare.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

data class LocalSignalingMessage(
    val fromInitiator: Boolean,
    val roomId: String,
    val message: SignalingMessage
)

/**
 * Just a local implementation of the RealTimeSignalingRepository until we refactor the way
 * we use signaling servers.
 */
class LocalSignalingRepository(val isInitiator: Boolean) : SignalingRepository {

    // Use dedicated scope instead of making sendMessage suspend to avoid blocks.
    // With a suspending "sendMessage", if a collector calls "sendMessage" from within their collect
    // lambda, the "receive" operation will not end.
    // This happens because the "receive"/collect operation will wait for the new "sendMessage"
    // to be completed.
    //   1) Receive OP triggers a sendMessage OP.
    //   2) Send OP is blocked because MutableSharedFlow SUSPENDS on overflow, and it is blocked by
    //      the Receive OP.
    //   3) Receive OP cannot complete to "free" the MutableSharedFlow until Send OP is completed.
    // We could avoid this by using a bigger buffer (default is 0) but the best options is to detach
    // "sendMessage" from the client scope and use our own.
    private val signalingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val name = if (isInitiator) "LOCAL" else "REMOTE"

    override fun sendMessage(
        roomId: String,
        message: SignalingMessage
    ) {
        signalingScope.launch {
            Timber.d("$name@SEND: ($roomId) $message")
            localSharedFlow.emit(
                LocalSignalingMessage(
                    fromInitiator = isInitiator,
                    roomId = roomId,
                    message = message
                )
            )
        }
    }

    override fun receiveMessagesFlow(roomId: String): Flow<SignalingMessage> {
        return localSharedFlow
            .filter { it.fromInitiator != isInitiator && it.roomId == roomId }
            .map { it.message }
            .onEach { message -> Timber.d("$name@RECEIVE: ($roomId) $message") }
    }

    companion object {
        private val localSharedFlow = MutableSharedFlow<LocalSignalingMessage>()
    }
}
