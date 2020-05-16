package com.spundev.webrtcshare.ui.join

import android.app.Application
import androidx.lifecycle.*
import com.spundev.webrtcshare.utils.WebRTCConnection
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.webrtc.PeerConnectionFactory

class JoinViewModel(val app: Application) : AndroidViewModel(app) {

    // WebRTC Connection helper
    private val webRTCConnection = WebRTCConnection("room_002", false)

    // isConnected value
    val isConnected: LiveData<Boolean> = liveData {
        webRTCConnection.isConnected.consumeEach { emit(it) }
    }

    // List of messages sent and received
    val messages: LiveData<List<String>> = liveData {
        webRTCConnection.messages.consumeEach { emit(it) }
    }

    init {
        // Initialization
        val initializationOptions =
            PeerConnectionFactory.InitializationOptions.builder(app)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        viewModelScope.launch {
            webRTCConnection.start()
        }
    }

    fun sendMessage(message: String) {
        webRTCConnection.sendMessage(message)
    }
}
