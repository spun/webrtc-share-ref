package com.spundev.webrtcshare.ui.screens.localDemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LocalDemoRoute(
    onNavigateBack: () -> Unit,
    viewModel: LocalDemoViewModel = hiltViewModel()
) {
    val uiState: LocalDemoUiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalDemoScreen(
        uiState = uiState,
        onSendMessageFromLocal = viewModel::sendMessageFromLocal,
        onSendMessageFromRemote = viewModel::sendMessageFromRemote,
    )
}

@Composable
private fun LocalDemoScreen(
    uiState: LocalDemoUiState,
    onSendMessageFromLocal: () -> Unit,
    onSendMessageFromRemote: () -> Unit,
) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                ClientPane(
                    clientData = uiState.localClient,
                    onSendMessage = onSendMessageFromLocal,
                    modifier = Modifier.weight(1f)
                )
                HorizontalDivider()
                ClientPane(
                    clientData = uiState.remoteClient,
                    onSendMessage = onSendMessageFromRemote,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClientPane(
    clientData: DemoClientData,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("is connected: ${clientData.isConnected}")
            Button(onClick = onSendMessage) {
                Text("Send message")
            }
        }
        LazyColumn {
            items(clientData.messages) {
                Text(it)
            }
        }
    }
}