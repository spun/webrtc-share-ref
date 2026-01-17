package com.spundev.webrtcshare.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WebRTCModule {

    @Singleton
    @Provides
    fun providesPeerConnectionFactory(
        @ApplicationContext context: Context,
    ): PeerConnectionFactory {
        val initOptions = PeerConnectionFactory.InitializationOptions
            .builder(context)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(initOptions)

        return PeerConnectionFactory.builder()
            // .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    @Singleton
    @Provides
    fun providesRTCConfiguration(): PeerConnection.RTCConfiguration {
        return PeerConnection.RTCConfiguration(
            arrayListOf(
                // adding google's stun server
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        ).apply {
            // use new unified sdp semantics PLAN_B is deprecated
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
    }
}
