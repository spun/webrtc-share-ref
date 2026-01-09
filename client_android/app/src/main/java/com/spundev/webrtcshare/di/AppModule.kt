package com.spundev.webrtcshare.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.webrtc.PeerConnectionFactory

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    @Provides
    fun providesPeerConnectionFactoryInitializationOptions(
        @ApplicationContext context: Context,
    ): PeerConnectionFactory.InitializationOptions? {
        return PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
    }
}
