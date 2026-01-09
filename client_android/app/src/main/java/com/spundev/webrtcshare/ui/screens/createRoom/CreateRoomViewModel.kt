package com.spundev.webrtcshare.ui.screens.createRoom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.utils.WebRTCConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.PeerConnectionFactory
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    initializationOptions: PeerConnectionFactory.InitializationOptions?
) : ViewModel() {
    // WebRTC Connection helper
    private val webRTCConnection = WebRTCConnection("room_002", true)

    // isConnected value
    val isConnected: StateFlow<Boolean> = webRTCConnection.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    // List of messages sent and received
    val messages: StateFlow<List<String>> = webRTCConnection.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        // Initialization
        PeerConnectionFactory.initialize(initializationOptions)

        viewModelScope.launch {
            webRTCConnection.start()
        }
    }

    fun sendMessage(message: String) {
        webRTCConnection.sendMessage(message)
    }
}
