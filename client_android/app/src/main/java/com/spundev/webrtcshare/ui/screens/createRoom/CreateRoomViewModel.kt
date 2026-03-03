package com.spundev.webrtcshare.ui.screens.createRoom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.di.Realtime
import com.spundev.webrtcshare.model.TextMessage
import com.spundev.webrtcshare.repositories.SignalingRepository
import com.spundev.webrtcshare.utils.WebRTCManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    webRTCManagerFactory: WebRTCManager.Factory,
    @Realtime signalingRepository: SignalingRepository
) : ViewModel() {

    private val webRTCManager = webRTCManagerFactory.create(signalingRepository)

    private val _roomId: MutableStateFlow<String?> = MutableStateFlow(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    init {
        viewModelScope.launch {
            val newRoomId = signalingRepository.createRoom()
            addCloseable(webRTCManager.start(isInitiator = true, roomId = newRoomId))
            _roomId.value = newRoomId
        }
    }

    val uiState: StateFlow<CreateRoomUiState> = combine(
        roomId,
        webRTCManager.isConnected,
        webRTCManager.messages
    ) { roomId, isConnected, messages ->
        if (roomId != null) {
            if (isConnected != null) {
                CreateRoomUiState.Conversation(roomId, isConnected, messages)
            } else {
                CreateRoomUiState.WaitingForGuest(roomId)
            }
        } else {
            CreateRoomUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CreateRoomUiState.Loading
    )

    fun sendMessage(message: String) {
        webRTCManager.sendMessage(message)
    }
}

sealed interface CreateRoomUiState {
    data object Loading : CreateRoomUiState
    data class WaitingForGuest(val roomId: String) : CreateRoomUiState
    data class Conversation(
        val roomId: String,
        val isConnected: Boolean,
        val messages: List<TextMessage>
    ) : CreateRoomUiState
}
