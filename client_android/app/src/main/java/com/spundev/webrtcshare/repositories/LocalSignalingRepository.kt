package com.spundev.webrtcshare.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private data class LocalSignalingMessage(
    val fromInitiator: Boolean,
    val roomId: String,
    val message: SignalingMessage
)

/**
 * Local implementation of SignalingRepository
 */
class LocalSignalingRepository @Inject constructor() : SignalingRepository {

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

    // Name for this SignalingRepository in logs.
    // This is useful when we have more than one WebRTCManager in the same screen (see LocalDemo)
    private fun getLogsName(isInitiator: Boolean) = if (isInitiator) "Main" else "Seco"

    override fun sendMessage(
        isInitiator: Boolean,
        roomId: String,
        message: SignalingMessage
    ) {
        val name = getLogsName(isInitiator)
        signalingScope.launch {
            Timber.d("$name@SEND: ($roomId) $message")
            val message = LocalSignalingMessage(
                fromInitiator = isInitiator,
                roomId = roomId,
                message = message
            )
            backlogStateFlow.update { it + message }
            activeSharedFlow.emit(message)
        }
    }

    override fun receiveMessagesFlow(
        isInitiator: Boolean,
        roomId: String
    ): Flow<SignalingMessage> {
        val name = getLogsName(isInitiator)
        return activeSharedFlow
            .onStart {
                // Emit backlog content to the collector
                backlogStateFlow.value.forEach {
                    emit(it)
                }
            }
            .filter { it.fromInitiator != isInitiator && it.roomId == roomId }
            .map { it.message }
            .onEach { message -> Timber.d("$name@RECEIVE: ($roomId) $message") }
    }

    companion object {
        private val backlogStateFlow = MutableStateFlow<List<LocalSignalingMessage>>(emptyList())
        private val activeSharedFlow = MutableSharedFlow<LocalSignalingMessage>()
    }
}
