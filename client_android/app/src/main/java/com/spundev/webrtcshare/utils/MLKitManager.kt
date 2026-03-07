package com.spundev.webrtcshare.utils

import android.content.Context
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_READY_TO_DOWNLOAD
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_UNKNOWN_MODULE
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spundev.webrtcshare.extensions.installFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface AvailabilityStatus {
    data object AlreadyAvailable : AvailabilityStatus
    data object ReadyToDownload : AvailabilityStatus
    data class Unavailable(val reason: String?) : AvailabilityStatus
    data object UnknownModule : AvailabilityStatus
    data class UnknownStatus(val status: Int) : AvailabilityStatus
}

class MLKitManager @Inject constructor(
    @ApplicationContext val context: Context,
    val moduleInstallClient: ModuleInstallClient
) {

    suspend fun getBarcodeScannerAvailability(): AvailabilityStatus {
        return getModuleAvailabilityStatus(GmsBarcodeScanning.getClient(context))
    }

    private suspend fun getModuleAvailabilityStatus(api: OptionalModuleApi): AvailabilityStatus {
        return try {
            val response = moduleInstallClient.areModulesAvailable(api).await()
            when (response.availabilityStatus) {
                STATUS_ALREADY_AVAILABLE -> AvailabilityStatus.AlreadyAvailable
                STATUS_READY_TO_DOWNLOAD -> AvailabilityStatus.ReadyToDownload
                STATUS_UNKNOWN_MODULE -> AvailabilityStatus.UnknownModule
                else -> AvailabilityStatus.UnknownStatus(response.availabilityStatus)
            }
        } catch (e: Exception) {
            AvailabilityStatus.Unavailable(e.message)
        }
    }

    fun installFlow(api: OptionalModuleApi) = moduleInstallClient.installFlow(api)
}