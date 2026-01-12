package com.spundev.webrtcshare.ui.screens.localDemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.utils.WebRTCConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.PeerConnectionFactory
import javax.inject.Inject

@HiltViewModel
class LocalDemoViewModel @Inject constructor(
    initializationOptions: PeerConnectionFactory.InitializationOptions?
) : ViewModel() {

    // WebRTC Connection helpers
    private val localWebRTCConnection = WebRTCConnection("room_002", true)
    private val remoteWebRTCConnection = WebRTCConnection("room_002", false)

    init {
        // Initialization
        PeerConnectionFactory.initialize(initializationOptions)

        viewModelScope.launch {
            localWebRTCConnection.start()
        }
        viewModelScope.launch {
            remoteWebRTCConnection.start()
        }
    }

    val uiState = combine(
        localWebRTCConnection.isConnected,
        localWebRTCConnection.messages,
        remoteWebRTCConnection.isConnected,
        remoteWebRTCConnection.messages,
    ) { localIsConnected, localMessages, remoteIsConnected, remoteMessages ->
        LocalDemoUiState.Success(
            localClient = DemoClientData(
                isConnected = localIsConnected,
                messages = localMessages
            ),
            remoteClient = DemoClientData(
                isConnected = remoteIsConnected,
                messages = remoteMessages
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalDemoUiState.Loading
    )

    fun sendMessageFromLocal() {
        localWebRTCConnection.sendMessage("From local")
    }

    fun sendMessageFromRemote() {
        remoteWebRTCConnection.sendMessage("From remote")
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
    val messages: List<String>
)
