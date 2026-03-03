package com.spundev.webrtcshare.ui.screens.joinRoom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.di.Realtime
import com.spundev.webrtcshare.model.TextMessage
import com.spundev.webrtcshare.repositories.SignalingRepository
import com.spundev.webrtcshare.utils.WebRTCManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    init {
        addCloseable(webRTCManager.start(isInitiator = false, roomId = roomId))
    }

    val uiState: StateFlow<JoinRoomUiState> = combine(
        webRTCManager.isConnected,
        webRTCManager.messages
    ) { isConnected, messages ->
        if (isConnected != null) {
            JoinRoomUiState.Conversation(roomId, isConnected, messages)
        } else {
            JoinRoomUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JoinRoomUiState.Loading
    )

    fun sendMessage(message: String) {
        webRTCManager.sendMessage(message)
    }
}

sealed interface JoinRoomUiState {
    data object Loading : JoinRoomUiState
    data class Conversation(
        val roomId: String,
        val isConnected: Boolean,
        val messages: List<TextMessage>
    ) : JoinRoomUiState
}
