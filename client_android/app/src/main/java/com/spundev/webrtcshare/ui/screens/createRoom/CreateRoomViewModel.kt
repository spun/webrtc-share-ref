package com.spundev.webrtcshare.ui.screens.createRoom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.di.Realtime
import com.spundev.webrtcshare.utils.WebRTCManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    @param:Realtime val webRTCManager: WebRTCManager,
) : ViewModel() {

    // isConnected value
    val isConnected: StateFlow<Boolean> = webRTCManager.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    // List of messages sent and received
    val messages: StateFlow<List<String>> = webRTCManager.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        addCloseable(webRTCManager.start(isInitiator = true))
    }

    fun sendMessage(message: String) {
        webRTCManager.sendMessage(message)
    }
}
