package com.spundev.webrtcshare.di

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.spundev.webrtcshare.utils.MLKitManager
import com.spundev.webrtcshare.utils.MLKitManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLKitModule {

    @Provides
    fun provideModuleInstallClient(
        @ApplicationContext context: Context
    ): ModuleInstallClient = ModuleInstall.getClient(context)

    @Singleton
    @Provides
    fun provideGoogleApiAvailability(): GoogleApiAvailability = GoogleApiAvailability.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MLKitManagerModule {

    @Binds
    abstract fun bindLocalSignalingRepository(
        impl: MLKitManagerImpl
    ): MLKitManager
}
