package com.spundev.webrtcshare.ui.screens.localDemo

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.model.TextMessage
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import kotlin.math.min

@Composable
fun LocalDemoRoute(
    onNavigateBack: () -> Unit,
    viewModel: LocalDemoViewModel = hiltViewModel()
) {
    val uiState: LocalDemoUiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalDemoScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSendMessageFromLocal = viewModel::sendMessageFromLocal,
        onSendMessageFromRemote = viewModel::sendMessageFromRemote,
    )
}

@Composable
private fun LocalDemoScreen(
    uiState: LocalDemoUiState,
    onNavigateBack: () -> Unit,
    onSendMessageFromLocal: (String) -> Unit,
    onSendMessageFromRemote: (String) -> Unit,
) {
    Column {
        LocalDemoTopAppBar(onNavigateBack = onNavigateBack)

        // Top inset is handled by our TopAppBar, but it won't appear as consumed
        // to sibling composables
        val contentInsets = WindowInsets.safeDrawing.only(
            sides = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
        Box(modifier = Modifier.windowInsetsPadding(contentInsets)) {
            when (uiState) {
                LocalDemoUiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is LocalDemoUiState.Success -> {
                    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
                    val showHorizontal = windowSizeClass.isWidthAtLeastBreakpoint(
                        widthDpBreakpoint = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    )
                    ClientsColumnRow(
                        localClient = {
                            ClientPane(
                                clientData = uiState.localClient,
                                onSendMessage = onSendMessageFromLocal,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        remoteClient = {
                            ClientPane(
                                clientData = uiState.remoteClient,
                                onSendMessage = onSendMessageFromRemote,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        vertical = !showHorizontal,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientsColumnRow(
    localClient: @Composable () -> Unit,
    remoteClient: @Composable () -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier
) {
    // Use movableContentOf to keep the scroll positions
    // We could hoist the scroll states instead, but this is good enough for now.
    val movableLocalClient = remember(localClient) { movableContentOf(localClient) }
    val movableRemoteClient = remember(remoteClient) { movableContentOf(remoteClient) }

    if (vertical) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            Box(modifier = Modifier.weight(1f)) { movableLocalClient() }
            Box(modifier = Modifier.weight(1f)) { movableRemoteClient() }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            Box(modifier = Modifier.weight(1f)) { movableLocalClient() }
            Box(modifier = Modifier.weight(1f)) { movableRemoteClient() }
        }
    }
}

@Composable
private fun ClientPane(
    clientData: DemoClientData,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .fillMaxWidth()
    ) {
        // Messages
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                top = 40.dp + 4.dp, // ConnectionStatusIndicator height + small padding
                bottom = 64.dp + 4.dp, // EmojiSelector height + small padding
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(clientData.messages) { message ->
                TextMessageRow(message)
            }
        }

        // Connection status
        ConnectionStatusIndicator(
            isConnected = clientData.isConnected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .height(28.dp)
        )

        // Emoji selector
        EmojiSelector(
            onEmojiSelected = onSendMessage,
            enabled = clientData.isConnected,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .height(48.dp)
        )
    }
}

@Composable
private fun ConnectionStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val connectionStatusValues = if (isConnected) {
        R.string.local_demo_connected to Color.Green
    } else {
        R.string.local_demo_disconnected to Color.Red
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .drawBehind {
                    val radius = (min(size.width, size.height) / 2)
                    drawCircle(
                        color = connectionStatusValues.second,
                        radius = radius,
                        center = Offset(radius, radius)
                    )
                }
        )
        Text(
            text = stringResource(connectionStatusValues.first),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun EmojiSelector(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        setOf("😜", "💀", "❤️", "👍").forEach { emojiString ->
            IconButton(
                onClick = { onEmojiSelected(emojiString) },
                enabled = enabled
            ) {
                Text(emojiString)
            }
        }
    }
}

@Composable
private fun TextMessageRow(
    message: TextMessage
) {
    val backgroundColor = if (message.isMine) {
        MaterialTheme.colorScheme.inversePrimary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val color = if (message.isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val horizontalArrangement = if (message.isMine) {
        Arrangement.End
    } else {
        Arrangement.Start
    }
    Row(
        horizontalArrangement = horizontalArrangement,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = message.text,
            color = color,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalDemoTopAppBar(
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.local_demo_title)) },
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(device = "spec:parent=pixel_10,orientation=landscape")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:parent=pixel_10,orientation=landscape"
)
@Composable
private fun LocalDemoScreenPreview() {
    val uiState = LocalDemoUiState.Success(
        localClient = DemoClientData(
            isConnected = true,
            messages = listOf(
                TextMessage(isMine = true, text = "Hello"),
                TextMessage(isMine = false, text = "Hi")
            )
        ),
        remoteClient = DemoClientData(
            isConnected = true,
            messages = listOf(
                TextMessage(isMine = false, text = "Hello"),
                TextMessage(isMine = true, text = "Hi")
            )
        )
    )

    WebRTCShareTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            LocalDemoScreen(
                uiState = uiState,
                onNavigateBack = {},
                onSendMessageFromLocal = {},
                onSendMessageFromRemote = {},
            )
        }
    }
}