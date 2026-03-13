package com.spundev.webrtcshare.ui.screens.joinRequest

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Composable
fun JoinRequestRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit,
    viewModel: JoinRequestViewModel = hiltViewModel()
) {
    val uiState: JoinRequestUiState by viewModel.uiState.collectAsStateWithLifecycle()
    JoinRequestScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onScanRequest = viewModel::scanRequest,
        onRoomId = onNavigateToRoom,
        // onInstallScannerRequest = viewModel::installScannerModule
    )

    // --- events from viewModel ---

    val screenEvents by viewModel.screenEvents.collectAsStateWithLifecycle()

    // always refer to the latest onNavigateToRoom function
    val currentOnNavigateToRoom by rememberUpdatedState(onNavigateToRoom)

    val context = LocalContext.current
    screenEvents?.let { event ->
        LaunchedEffect(context, event) {
            if (event is JoinRequestEvents.LaunchScanner) {
                try {
                    val options = GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .enableAutoZoom()
                        .build()

                    val scanner = GmsBarcodeScanning.getClient(context, options)
                    val result = scanner.startScan().await()
                    result.rawValue?.let {
                        Timber.d("code: $it")
                        currentOnNavigateToRoom(it)
                    }
                } catch (e: MlKitException) {
                    // We checked availability before starting the scanner, so any exception
                    // should be about the scanning process and not the module.
                    // Since we don't know what kind of exceptions the scanner can throw,
                    // just show the message in a toast.
                    Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
                } finally {
                    viewModel.clearScanEvent()
                }
            }
        }
    }
}

@Composable
private fun JoinRequestScreen(
    uiState: JoinRequestUiState,
    onNavigateBack: () -> Unit,
    onScanRequest: () -> Unit,
    onRoomId: (String) -> Unit,
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

        when (uiState) {
            JoinRequestUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(contentInsets)
                ) {
                    CircularProgressIndicator()
                }
            }

            is JoinRequestUiState.Success -> {
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
                        scannerState = uiState.scannerState,
                        onScanRequest = onScanRequest,
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
    }
}

@Composable
private fun CodeScannerSection(
    scannerState: ScannerState,
    onScanRequest: () -> Unit,
    modifier: Modifier = Modifier,
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
            onScanRequest = onScanRequest,
            enabled = scannerState == ScannerState.Ready
        )
        // Extra info message
        CodeScannerInfoMessage(scannerState)
    }
}

@Composable
private fun CodeScannerButton(
    onScanRequest: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onScanRequest,
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
private fun CodeScannerInfoMessage(
    scannerState: ScannerState,
    modifier: Modifier = Modifier
) {
    // This is the easiest animation we can add
    // to smooth out the switch between states
    AnimatedContent(
        targetState = scannerState,
        modifier = modifier
    ) { scannerState ->
        when (scannerState) {
            // Display installation progress
            is ScannerState.Installing -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.join_request_scan_installing_message),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    val progress by scannerState.progress.collectAsStateWithLifecycle()
                    LinearProgressIndicator(progress = { progress / 100f })
                }
            }

            // The scanner is not available for this device. Notify user.
            ScannerState.Unavailable -> {
                Text(
                    text = stringResource(R.string.join_request_scan_unavailable_message),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                )
            }

            ScannerState.Error -> {
                Text(
                    text = stringResource(R.string.join_request_scan_error_message),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                )
            }

            ScannerState.Ready -> {}
        }
    }
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
            FilledTonalButton(
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
fun JoinRequestScreenScannerReadyPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            JoinRequestScreen(
                uiState = JoinRequestUiState.Success(
                    scannerState = ScannerState.Ready
                ),
                onNavigateBack = {},
                onScanRequest = {},
                onRoomId = {},
            )
        }
    }
}

@Preview
@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
fun JoinRequestScreenLoadingPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            JoinRequestScreen(
                uiState = JoinRequestUiState.Loading,
                onNavigateBack = {},
                onScanRequest = {},
                onRoomId = {},
            )
        }
    }
}

@Preview
@Composable
fun CodeScannerSectionReadyPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CodeScannerSection(
                scannerState = ScannerState.Ready,
                onScanRequest = {},
            )
        }
    }
}

@Preview
@Composable
fun CodeScannerSectionInstallingPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CodeScannerSection(
                scannerState = ScannerState.Installing(MutableStateFlow(30)),
                onScanRequest = {},
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
                scannerState = ScannerState.Unavailable,
                onScanRequest = {},
            )
        }
    }
}

@Preview
@Composable
fun CodeScannerSectionErrorPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CodeScannerSection(
                scannerState = ScannerState.Error,
                onScanRequest = {},
            )
        }
    }
}
