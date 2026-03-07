package com.spundev.webrtcshare.extensions

import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface ModuleInstallState {
    data object Requested : ModuleInstallState
    data object RequestError : ModuleInstallState
    data object AlreadyInstalled : ModuleInstallState
    data class Installing(val progress: Int) : ModuleInstallState
    data object Complete : ModuleInstallState
    data class Failed(val state: Int) : ModuleInstallState
}

fun ModuleInstallClient.installFlow(
    api: OptionalModuleApi
) = callbackFlow {

    val listener: InstallStatusListener = { update ->
        // Progress info is only set when modules are in the progress of downloading.
        update.progressInfo?.let {
            val progress = if (it.totalBytesToDownload > 0) {
                (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt()
            } else 0
            // Notify progress for the progress bar.
            trySendBlocking(ModuleInstallState.Installing(progress))
        }

        when (update.installState) {
            STATE_COMPLETED -> {
                trySendBlocking(ModuleInstallState.Complete)
                channel.close()
            }

            STATE_CANCELED, STATE_FAILED -> {
                trySendBlocking(ModuleInstallState.Failed(update.installState))
                channel.close()
            }
        }
    }

    val request = ModuleInstallRequest.newBuilder()
        .addApi(api)
        .setListener(listener)
        .build()

    trySendBlocking(ModuleInstallState.Requested)
    val installResponse = installModules(request).await()
    if (installResponse != null) {
        if (installResponse.areModulesAlreadyInstalled()) {
            trySendBlocking(ModuleInstallState.AlreadyInstalled)
            channel.close()
        }
    } else {
        trySendBlocking(ModuleInstallState.RequestError)
    }

    awaitClose {
        unregisterListener(listener)
    }
}