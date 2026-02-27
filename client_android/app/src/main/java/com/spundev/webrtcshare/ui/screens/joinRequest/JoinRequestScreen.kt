package com.spundev.webrtcshare.ui.screens.joinRequest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spundev.webrtcshare.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Composable
fun JoinRequestRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val fieldState = rememberTextFieldState()
    Column(modifier = Modifier.safeDrawingPadding()) {
        TextField(state = fieldState)

        // Paste from clipboard button
        OutlinedButton(
            onClick = {
                scope.launch {
                    val clipEntry = clipboard.getClipEntry()
                    if (clipEntry != null && clipEntry.clipData.itemCount > 0) {
                        val text = clipEntry.clipData.getItemAt(0).text.toString()
                        fieldState.setTextAndPlaceCursorAtEnd(text)
                    }
                }
            },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                painterResource(R.drawable.ic_content_paste_24),
                contentDescription = "Paste",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Paste")
        }
        Button(onClick = {
            onNavigateToRoom(fieldState.text.toString())
        }) {
            Text("Join room")
        }

        CodeScannerButton(
            onRoomCodeScan = onNavigateToRoom
        )
    }
}

@Composable
private fun CodeScannerButton(
    onRoomCodeScan: (String) -> Unit
) {
    // Indicates if the device can access the scanner api from Google play services
    var isScannerApiAvailable by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(context) {
        val moduleInstallClient = ModuleInstall.getClient(context)
        val scanner = GmsBarcodeScanning.getClient(context)
        isScannerApiAvailable = try {
            val result = moduleInstallClient.areModulesAvailable(scanner).await()
            // The other options are STATUS_READY_TO_DOWNLOAD and STATUS_UNKNOWN_MODULE
            // Even if we set the barcode_ui dependency in our AndroidManifest, we don't
            // know how they are handled when installing the apk directly.
            // We don't know if the call to "startScan" will trigger the download
            // automatically or if it will throw an error.
            // TODO: We need to check if there is a way to uninstall downloaded modules
            //  to test these situations.
            result.availabilityStatus == STATUS_ALREADY_AVAILABLE
        } catch (e: ApiException) {
            // Debug values
            Timber.w(e, "Error checking module availability")
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            Timber.d("- status: ${e.status}")
            Timber.d("- statusCode: ${e.statusCode}")
            Timber.d("- statusMessage: ${e.message}")
            Timber.d("- connectionResult: ${e.status.connectionResult}")
            val errorCode = e.status.connectionResult?.errorCode
            if (errorCode != null) {
                Timber.d("- message: ${googleApiAvailability.getErrorString(errorCode)}")
                val isUserResolvable = googleApiAvailability.isUserResolvableError(errorCode)
                Timber.d("- isUserResolvable: $isUserResolvable");
            }
            false
        }
    }

    if (isScannerApiAvailable) {
        val scope = rememberCoroutineScope()
        OutlinedButton(
            onClick = {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .enableAutoZoom()
                    .build()

                val scanner = GmsBarcodeScanning.getClient(context, options)
                scope.launch {
                    val result = scanner.startScan().await()
                    result.rawValue?.let { onRoomCodeScan(it) }
                }
            },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                painterResource(R.drawable.ic_qr_code_scanner_24),
                contentDescription = "Scan room code",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Scan code")
        }
    }
}
