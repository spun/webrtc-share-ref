package com.spundev.webrtcshare.ui.screens.localDemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.di.Local
import com.spundev.webrtcshare.model.TextMessage
import com.spundev.webrtcshare.repositories.SignalingRepository
import com.spundev.webrtcshare.utils.WebRTCManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalDemoViewModel @Inject constructor(
    webRTCManagerFactory: WebRTCManager.Factory,
    @Local signalingRepository: SignalingRepository,
) : ViewModel() {

    // Use Factory to create two WebRTCManager with a shared signaling repository
    private var localWebRTCManager = webRTCManagerFactory.create(signalingRepository)
    private var remoteWebRTCManager = webRTCManagerFactory.create(signalingRepository)

    init {
        // Initialization
        viewModelScope.launch {
            val roomId = signalingRepository.createRoom()
            addCloseable(localWebRTCManager.start(isInitiator = true, roomId = roomId))
            addCloseable(remoteWebRTCManager.start(isInitiator = false, roomId = roomId))
        }
    }

    val uiState = combine(
        localWebRTCManager.isConnected,
        localWebRTCManager.messages,
        remoteWebRTCManager.isConnected,
        remoteWebRTCManager.messages,
    ) { localIsConnected, localMessages, remoteIsConnected, remoteMessages ->
        LocalDemoUiState.Success(
            localClient = DemoClientData(
                isConnected = localIsConnected == true,
                messages = localMessages
            ),
            remoteClient = DemoClientData(
                isConnected = remoteIsConnected == true,
                messages = remoteMessages
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalDemoUiState.Loading
    )

    fun sendMessageFromLocal(message: String) {
        localWebRTCManager.sendMessage(message)
    }

    fun sendMessageFromRemote(message: String) {
        remoteWebRTCManager.sendMessage(message)
    }
}

sealed interface LocalDemoUiState {
    data object Loading : LocalDemoUiState
    data class Success(
        val localClient: DemoClientData,
        val remoteClient: DemoClientData,
    ) : LocalDemoUiState
}

data class DemoClientData(
    val isConnected: Boolean,
    val messages: List<TextMessage>
)
