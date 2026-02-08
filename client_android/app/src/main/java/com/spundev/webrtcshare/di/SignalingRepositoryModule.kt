package com.spundev.webrtcshare.di

import com.spundev.webrtcshare.repositories.LocalSignalingRepository
import com.spundev.webrtcshare.repositories.RealTimeSignalingRepository
import com.spundev.webrtcshare.repositories.SignalingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
internal abstract class SignalingRepositoryModule {

    @Binds
    @Local
    abstract fun bindLocalSignalingRepository(
        impl: LocalSignalingRepository
    ): SignalingRepository

    @Binds
    @Realtime
    abstract fun bindRealtimeSignalingRepository(
        impl: RealTimeSignalingRepository
    ): SignalingRepository
}
