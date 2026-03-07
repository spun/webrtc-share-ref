package com.spundev.webrtcshare.di

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@Module
@InstallIn(ActivityComponent::class)
object MLKitModule {

    @Provides
    fun provideModuleInstallClient(
        @ActivityContext context: Context
    ): ModuleInstallClient = ModuleInstall.getClient(context)
}
