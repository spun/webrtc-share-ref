package com.spundev.webrtcshare.extensions

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.DataChannel

sealed interface DataChannelEvent {
    data class BufferedAmountChange(val l: Long) : DataChannelEvent
    data class StateChange(val newState: DataChannel.State) : DataChannelEvent
    data class Message(val buffer: DataChannel.Buffer?) : DataChannelEvent
}

fun DataChannel.observerFlow() = callbackFlow<DataChannelEvent> {
    val observer = object : DataChannel.Observer {
        override fun onBufferedAmountChange(p0: Long) {
            trySend(DataChannelEvent.BufferedAmountChange(p0))
        }

        override fun onStateChange() {
            val newState = state()
            trySend(DataChannelEvent.StateChange(newState))
        }

        override fun onMessage(buffer: DataChannel.Buffer?) {
            trySend(DataChannelEvent.Message(buffer))
        }
    }

    registerObserver(observer)
    awaitClose { unregisterObserver() }
}
