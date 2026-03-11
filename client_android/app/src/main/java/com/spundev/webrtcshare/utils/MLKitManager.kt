package com.spundev.webrtcshare.utils

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_READY_TO_DOWNLOAD
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_UNKNOWN_MODULE
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spundev.webrtcshare.extensions.installFlow
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface AvailabilityStatus {
    data object AlreadyAvailable : AvailabilityStatus
    data object ReadyToDownload : AvailabilityStatus
    data object Unavailable : AvailabilityStatus
    data class Error(val reason: String) : AvailabilityStatus
}

interface MLKitManager {
    suspend fun getBarcodeScannerAvailability(): AvailabilityStatus
    fun installScannerFlow(): Flow<Int>

    suspend fun getModuleAvailabilityStatus(api: OptionalModuleApi): AvailabilityStatus
    fun installFlow(api: OptionalModuleApi): Flow<Int>
}

class MLKitManagerImpl @Inject constructor(
    @ApplicationContext val context: Context,
    private val moduleInstallClient: ModuleInstallClient,
    private val gApiAvailability: Lazy<GoogleApiAvailability>
) : MLKitManager {

    override suspend fun getBarcodeScannerAvailability(): AvailabilityStatus {
        return getModuleAvailabilityStatus(GmsBarcodeScanning.getClient(context))
    }

    override suspend fun getModuleAvailabilityStatus(api: OptionalModuleApi): AvailabilityStatus {
        return try {
            val response = moduleInstallClient.areModulesAvailable(api).await()
            when (response.availabilityStatus) {
                STATUS_ALREADY_AVAILABLE -> AvailabilityStatus.AlreadyAvailable
                STATUS_READY_TO_DOWNLOAD -> AvailabilityStatus.ReadyToDownload
                STATUS_UNKNOWN_MODULE -> AvailabilityStatus.Error("Unknown module")
                else -> AvailabilityStatus.Error("Unexpected availability status: ${response.availabilityStatus}")
            }
        } catch (e: ApiException) {
            val resultErrorCode = e.status.connectionResult?.errorCode
            if (resultErrorCode != null) {
                // Check if the exception was because the api is not available on the device.
                if (resultErrorCode == ConnectionResult.API_UNAVAILABLE) {
                    AvailabilityStatus.Unavailable
                } else {
                    // Use GoogleApiAvailability to know more about the caught exception
                    val googleApiAvailability = gApiAvailability.get()
                    val errorString = googleApiAvailability.getErrorString(resultErrorCode)
                    val isUserResolvable =
                        googleApiAvailability.isUserResolvableError(resultErrorCode)
                    AvailabilityStatus.Error("$errorString | resolvable by user ($isUserResolvable)")
                }
            } else {
                // We don't know when a ConnectionResult could be null. Just send the message.
                AvailabilityStatus.Error("ConnectionResult was null. ${e.message}")
            }
        } catch (e: Exception) {
            // If we got an Exception different from ApiException, just send the message.
            AvailabilityStatus.Error("${e.message}")
        }
    }

    override fun installScannerFlow(): Flow<Int> =
        installFlow(GmsBarcodeScanning.getClient(context))

    override fun installFlow(api: OptionalModuleApi) = moduleInstallClient.installFlow(api)
}

/**
 * FAKE Implementation
 * We have these implementation to check how the different flows feel when we use the app.
 * We already have a MLKitManager test file and compose previews for each state but, since
 * we cannot uninstall mlkit modules from a device, this is a simple way to check each
 * situation while using the app.
 */

class MLKitManagerFakeReadyToInstall @Inject constructor() : MLKitManager {

    var initialStatus: AvailabilityStatus = AvailabilityStatus.ReadyToDownload

    override suspend fun getBarcodeScannerAvailability() = initialStatus

    override fun installScannerFlow(): Flow<Int> {
        return (1..100).asFlow().map {
            delay(100)
            it
        }.onCompletion {
            initialStatus = AvailabilityStatus.AlreadyAvailable
        }
    }

    override suspend fun getModuleAvailabilityStatus(
        api: OptionalModuleApi
    ) = TODO("Not needed")

    override fun installFlow(
        api: OptionalModuleApi
    ) = TODO("Not needed")
}