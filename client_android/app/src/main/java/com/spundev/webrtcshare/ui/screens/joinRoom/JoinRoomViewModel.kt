package com.spundev.webrtcshare.ui.screens.joinRoom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.di.Realtime
import com.spundev.webrtcshare.repositories.SignalingRepository
import com.spundev.webrtcshare.utils.WebRTCManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = JoinRoomViewModel.Factory::class)
class JoinRoomViewModel @AssistedInject constructor(
    @Assisted val roomId: String,
    webRTCManagerFactory: WebRTCManager.Factory,
    @Realtime signalingRepository: SignalingRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): JoinRoomViewModel
    }

    private val webRTCManager = webRTCManagerFactory.create(signalingRepository)

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
        addCloseable(webRTCManager.start(isInitiator = false, roomId = roomId))
    }

    fun sendMessage(message: String) {
        webRTCManager.sendMessage(message)
    }
}
