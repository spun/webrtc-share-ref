package com.spundev.webrtcshare.di

import com.spundev.webrtcshare.repositories.SignalingRepository
import com.spundev.webrtcshare.utils.WebRTCManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory

@Module
@InstallIn(SingletonComponent::class)
internal object WebRTCManagerModule {

    @Provides
    @Local
    fun providesLocalWebRTCManager(
        rtcConnectionFactory: PeerConnectionFactory,
        rtcConfiguration: PeerConnection.RTCConfiguration,
        @Local signalingRepository: SignalingRepository
    ) = WebRTCManager(rtcConnectionFactory, rtcConfiguration, signalingRepository)

    @Provides
    @Realtime
    fun providesRealtimeWebRTCManager(
        rtcConnectionFactory: PeerConnectionFactory,
        rtcConfiguration: PeerConnection.RTCConfiguration,
        @Realtime signalingRepository: SignalingRepository
    ) = WebRTCManager(rtcConnectionFactory, rtcConfiguration, signalingRepository)
}
