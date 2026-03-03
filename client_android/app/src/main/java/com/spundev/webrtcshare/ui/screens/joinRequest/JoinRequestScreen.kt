package com.spundev.webrtcshare.ui.screens.joinRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Composable
fun JoinRequestRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit
) {
    JoinRequestScreen(
        onNavigateBack = onNavigateBack,
        onRoomId = onNavigateToRoom
    )
}

@Composable
private fun JoinRequestScreen(
    onNavigateBack: () -> Unit,
    onRoomId: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        JoinRequestTopAppBar(
            title = stringResource(R.string.join_request_title),
            onNavigateBack = onNavigateBack
        )

        // Top inset is handled by our TopAppBar, but it won't appear as consumed
        // to sibling composables.
        val contentInsets = WindowInsets.safeDrawing.only(
            sides = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterHorizontally)
                .width(IntrinsicSize.Min)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(contentInsets)
        ) {
            CodeScannerSection(
                onRoomCodeScan = onRoomId,
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            RoomIdFormSection(
                onRoomId = onRoomId,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun CodeScannerSection(
    onRoomCodeScan: (String) -> Unit,
    modifier: Modifier = Modifier,
    isScannerApiAvailable: Boolean = rememberScannerApiAvailability(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Intro text
        Text(
            text = stringResource(R.string.join_request_scan_section_label),
            style = MaterialTheme.typography.titleMedium
        )
        // Button to launch the scanner
        CodeScannerButton(
            onRoomCodeScan = onRoomCodeScan,
            enabled = isScannerApiAvailable
        )
        // Explain if scanner is not available
        if (!isScannerApiAvailable) {
            Text(
                text = stringResource(R.string.join_request_scan_unavailable_error),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun CodeScannerButton(
    onRoomCodeScan: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = modifier
    ) {
        Icon(
            painterResource(R.drawable.ic_qr_code_scanner_24),
            contentDescription = stringResource(R.string.join_request_scan_button_content_description),
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.join_request_scan_button))
    }
}

@Composable
private fun rememberScannerApiAvailability(): Boolean {
    // Indicates if the device can access the scanner api from Google play services
    val isScannerApiAvailable = remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(context) {
        val moduleInstallClient = ModuleInstall.getClient(context)
        val scanner = GmsBarcodeScanning.getClient(context)
        isScannerApiAvailable.value = try {
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
                Timber.d("- isUserResolvable: $isUserResolvable")
            }
            false
        }
    }
    return isScannerApiAvailable.value
}

@Composable
private fun RoomIdFormSection(
    onRoomId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val fieldState = rememberTextFieldState()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Intro text
        Text(
            text = stringResource(R.string.join_request_form_section_label),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        // Field for room id
        OutlinedTextField(
            state = fieldState,
            label = { Text(stringResource(R.string.join_request_form_field_label)) }
        )
        // Buttons to paste from clipboard or join
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.join_request_form_paste_button))
            }
            // Join button
            Button(
                onClick = { onRoomId(fieldState.text.toString()) },
                enabled = fieldState.text.isNotEmpty()
            ) {
                Text(stringResource(R.string.join_request_form_join_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRequestTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above
                ),
                tooltip = { PlainTooltip { Text(stringResource(R.string.toolbar_navigate_up)) } },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back_24),
                        contentDescription = stringResource(R.string.toolbar_navigate_up)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent)
    )
}


@Preview
@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
fun JoinRequestScreenPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            JoinRequestScreen(
                onNavigateBack = {},
                onRoomId = {}
            )
        }
    }
}

@Preview
@Composable
fun CodeScannerSectionAvailablePreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CodeScannerSection(
                onRoomCodeScan = {},
                isScannerApiAvailable = true
            )
        }
    }
}

@Preview
@Composable
fun CodeScannerSectionUnavailablePreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CodeScannerSection(
                onRoomCodeScan = {},
                isScannerApiAvailable = false
            )
        }
    }
}
