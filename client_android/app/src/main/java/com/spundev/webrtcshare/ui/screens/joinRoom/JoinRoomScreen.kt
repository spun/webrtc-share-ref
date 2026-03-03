package com.spundev.webrtcshare.ui.screens.joinRoom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.model.TextMessage
import com.spundev.webrtcshare.ui.components.TextMessagesViewer
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme

@Composable
fun JoinRoomRoute(
    onNavigateBack: () -> Unit,
    viewModel: JoinRoomViewModel = hiltViewModel()
) {
    val uiState: JoinRoomUiState by viewModel.uiState.collectAsStateWithLifecycle()
    JoinRoomScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSendMessage = { viewModel.sendMessage("Fox") }
    )
}

@Composable
private fun JoinRoomScreen(
    uiState: JoinRoomUiState,
    onNavigateBack: () -> Unit,
    onSendMessage: () -> Unit,
) {
    Column {
        // Show connection status in TopAppBar
        val topAppBarTitleRes = when (uiState) {
            JoinRoomUiState.Loading -> R.string.join_room_loading_title
            is JoinRoomUiState.Conversation -> {
                if (uiState.isConnected) {
                    R.string.join_room_connected_title
                } else {
                    R.string.join_room_disconnected_title
                }
            }
        }
        JoinRoomTopAppBar(
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
                JoinRoomUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is JoinRoomUiState.Conversation -> {
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
                            Text(stringResource(R.string.join_room_connected_send_button))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRoomTopAppBar(
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
private fun JoinRoomScreenLoadingPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            JoinRoomScreen(
                uiState = JoinRoomUiState.Loading,
                onNavigateBack = {},
                onSendMessage = {}
            )
        }
    }
}

@Preview
@Composable
private fun JoinRoomScreenConnectedPreview() {
    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            JoinRoomScreen(
                uiState = JoinRoomUiState.Conversation(
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
