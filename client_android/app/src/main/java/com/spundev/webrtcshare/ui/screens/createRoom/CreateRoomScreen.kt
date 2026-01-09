package com.spundev.webrtcshare.ui.screens.createRoom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateRoomRoute(
    onNavigateBack: () -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Text("isConnected: $isConnected")
        Button(
            enabled = isConnected,
            onClick = { viewModel.sendMessage("Eagle") }
        ) {
            Text("Send Eagle")
        }
        messages.forEach {
            Text(it)
        }
    }
}