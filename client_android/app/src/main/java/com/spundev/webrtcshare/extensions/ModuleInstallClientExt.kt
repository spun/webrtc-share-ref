package com.spundev.webrtcshare.extensions

import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

sealed class ModuleInstallException : Exception() {
    class RequestFailed : ModuleInstallException()
    class InstallFailed : ModuleInstallException()
}

fun ModuleInstallClient.installFlow(
    api: OptionalModuleApi
): Flow<Int> = callbackFlow {

    val listener: InstallStatusListener = { update ->
        // Progress info is only set when modules are in the progress of downloading.
        update.progressInfo?.let {
            val progress = if (it.totalBytesToDownload > 0) {
                (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt()
            } else 0
            // Notify progress for the progress bar.
            trySendBlocking(progress)
        }

        when (update.installState) {
            STATE_FAILED -> {
                channel.close(ModuleInstallException.InstallFailed())
            }

            STATE_CANCELED -> {
                cancel()
            }

            STATE_COMPLETED -> channel.close()
            else -> {
                Timber.w("Unhandled install state: ${update.installState}")
            }
        }
    }

    val request = ModuleInstallRequest.newBuilder()
        .addApi(api)
        .setListener(listener)
        .build()

    try {
        val installResponse = installModules(request).await()
        if (installResponse != null) {
            if (installResponse.areModulesAlreadyInstalled()) {
                channel.close()
            }
        } else {
            channel.close(ModuleInstallException.RequestFailed())
        }
    } catch (_: Exception) {
        channel.close(ModuleInstallException.RequestFailed())
    }

    awaitClose {
        unregisterListener(listener)
        // Initiates a request to release the optional module if no longer needed by any apps.
        // This does not guarantee that the module will be removed. However, since we were
        // installing it up to this point, we can assume no other app needs it and Google Play
        // services attempt to remove it.
        // This is the closest thing we have to a cancel.
        releaseModules(api)
    }
}