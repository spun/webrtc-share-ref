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

/**
 * Different representations of an mlKit module status
 */
sealed interface AvailabilityStatus {
    /**
     * The module was installed previously, and it's ready to be used.
     */
    data object AlreadyAvailable : AvailabilityStatus

    /**
     * The module can be used on this device, but it needs to be installed first.
     */
    data object ReadyToDownload : AvailabilityStatus

    /**
     * The module cannot be used on this device.
     */
    data object Unavailable : AvailabilityStatus

    /**
     * There was an error while checking the module availability status.
     */
    data class Error(val reason: String) : AvailabilityStatus
}

interface MLKitManager {
    /**
     * Get the [AvailabilityStatus] for the barcode scanner module.
     */
    suspend fun getBarcodeScannerAvailability(): AvailabilityStatus

    /**
     * Starts the barcode scanner module urgent installation and emits the progress.
     */
    fun installScannerFlow(): Flow<Int>

    /**
     * Get the [AvailabilityStatus] for the specified module/api.
     */
    suspend fun getModuleAvailabilityStatus(api: OptionalModuleApi): AvailabilityStatus

    /**
     * Starts the specified module/api urgent installation and emits the progress.
     */
    fun installFlow(api: OptionalModuleApi): Flow<Int>
}

/**
 * [MLKitManager] that uses [ModuleInstallClient] to check availability and
 *  module installation against the real play services api.
 */
class MLKitManagerImpl @Inject constructor(
    @param:ApplicationContext val context: Context,
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
 * FAKE [MLKitManager] implementation
 * We have this implementation to check how different flows feel when we use the app.
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
