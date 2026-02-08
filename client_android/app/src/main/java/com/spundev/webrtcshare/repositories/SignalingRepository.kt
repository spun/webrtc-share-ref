package com.spundev.webrtcshare.repositories

import kotlinx.coroutines.flow.Flow
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

sealed class SignalingMessage {
    data class SignalingDescription(val description: SessionDescription) : SignalingMessage()
    data class SignalingCandidate(val candidate: IceCandidate) : SignalingMessage()
}

interface SignalingRepository {
    fun sendMessage(isInitiator: Boolean, roomId: String, message: SignalingMessage)
    fun receiveMessagesFlow(isInitiator: Boolean, roomId: String): Flow<SignalingMessage>
}
