package com.spundev.webrtcshare.ui.screens.createRoom

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.model.TextMessage
import com.spundev.webrtcshare.ui.components.QRCode
import com.spundev.webrtcshare.ui.components.TextMessagesViewer
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateRoomRoute(
    onNavigateBack: () -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val uiState: CreateRoomUiState by viewModel.uiState.collectAsStateWithLifecycle()
    CreateRoomScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSendMessage = { viewModel.sendMessage("Eagle") }
    )
}

@Composable
private fun CreateRoomScreen(
    uiState: CreateRoomUiState,
    onNavigateBack: () -> Unit,
    onSendMessage: () -> Unit,
) {
    Column {
        // Show connection status in TopAppBar
        val topAppBarTitleRes = when (uiState) {
            CreateRoomUiState.Loading -> R.string.create_room_loading_title
            is CreateRoomUiState.WaitingForGuest -> R.string.create_room_waiting_title
            is CreateRoomUiState.Conversation -> {
                if (uiState.isConnected) {
                    R.string.create_room_connected_title
                } else {
                    R.string.create_room_disconnected_title
                }
            }
        }
        CreateRoomTopAppBar(
            title = stringResource(topAppBarTitleRes),
            onNavigateBack = onNavigateBack
        )

        // Top inset is handled by our TopAppBar, but it won't appear as consumed
        // to sibling composables
        val contentInsets = WindowInsets.safeDrawing.only(
            sides = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
        Box(modifier = Modifier.windowInsetsPadding(contentInsets)) {
            when (uiState) {
                // Waiting for the room id to be generated. Nothing to show.
                CreateRoomUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // We have a room id, and we are waiting for the other device.
                is CreateRoomUiState.WaitingForGuest -> {
                    // We are using a scrollable Column to make all elements visible on
                    // short screens, but this doesn't solve cases where the screen is
                    // short and wide. If we only use our 400.dp limit, the QR code
                    // might never be fully visible at once.
                    // This is not as simple as creating a custom Layout. A scrollable
                    // custom layout will receive an "infinite" maxHeight, and we won't
                    // be able to force the QR code composable to fit without knowing
                    // the true height.
                    // To solve this, we use BoxWithConstraints to get the "viewport"
                    // size before we compose our scrollable Column with the QR code.
                    // This makes our QR code always scannable.
                    BoxWithConstraints {
                        val qrSize = min(min(maxWidth, maxHeight), 400.dp)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(
                                space = 16.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header with the copyable room id
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(
                                    space = 8.dp,
                                    alignment = Alignment.CenterHorizontally
                                ),
                                verticalArrangement = Arrangement.Center,
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.create_room_waiting_header_room_id_label))
                                CopyableRoomIdButton(uiState.roomId)
                            }
                            // QR code
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.size(qrSize)
                            ) {
                                QRCode(
                                    text = uiState.roomId,
                                    snapToPixel = true,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            // Instructions with the scan or "Share" text
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Scan text
                                Text(stringResource(R.string.create_room_waiting_footer_scan_text))
                                // Share text
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.create_room_waiting_footer_share_start_text))
                                    ShareRoomButton(uiState.roomId)
                                    Text(stringResource(R.string.create_room_waiting_footer_share_end_text))
                                }
                            }
                        }
                    }
                }

                // We are connected with the other device. Show conversation layout
                is CreateRoomUiState.Conversation -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            TextMessagesViewer(
                                messages = uiState.messages,
                                contentPadding = PaddingValues(16.dp)
                            )
                        }
                        Button(
                            onClick = onSendMessage,
                            enabled = uiState.isConnected
                        ) {
                            Text(stringResource(R.string.create_room_connected_send_button))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CopyableRoomIdButton(
    roomId: String
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val size = ButtonDefaults.ExtraSmallContainerHeight
    OutlinedButton(
        onClick = {
            val clipData = ClipData.newPlainText("Room id", roomId)
            val clipEntry = ClipEntry(clipData)
            scope.launch { clipboard.setClipEntry(clipEntry) }
        },
        contentPadding = ButtonDefaults.contentPaddingFor(size),
        modifier = Modifier.heightIn(size),
    ) {
        Icon(
            painterResource(R.drawable.ic_content_copy_24),
            contentDescription = "Copy room id",
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(roomId, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShareRoomButton(
    roomId: String
) {
    val context = LocalContext.current
    val size = ButtonDefaults.ExtraSmallContainerHeight
    OutlinedButton(
        onClick = {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, roomId)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        },
        contentPadding = ButtonDefaults.contentPaddingFor(size),
        modifier = Modifier.heightIn(size),
    ) {
        Icon(
            painterResource(R.drawable.ic_share_24),
            contentDescription = stringResource(R.string.create_room_waiting_footer_share_button_content_description),
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(stringResource(R.string.create_room_waiting_footer_share_button), maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoomTopAppBar(
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
@Composable
private fun CreateRoomScreenLoadingPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CreateRoomScreen(
                uiState = CreateRoomUiState.Loading,
                onNavigateBack = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview
@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun CreateRoomScreenWaitingPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CreateRoomScreen(
                uiState = CreateRoomUiState.WaitingForGuest(roomId = "Preview roomId"),
                onNavigateBack = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview
@Composable
private fun CreateRoomScreenConnectedPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CreateRoomScreen(
                uiState = CreateRoomUiState.Conversation(
                    roomId = "Preview roomId",
                    isConnected = true,
                    messages = listOf(
                        TextMessage(
                            isMine = true,
                            text = "Hello"
                        )
                    ),
                ),
                onNavigateBack = {},
                onSendMessage = {}
            )
        }
    }
}
