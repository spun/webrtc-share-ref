package com.spundev.webrtcshare.ui.screens.createRoom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber

@Composable
fun CreateRoomRoute(
    onNavigateBack: () -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val roomId by viewModel.roomId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Text("isConnected: $isConnected")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("roomId: $roomId")
            TextButton(onClick = {
                Timber.d("TODO: Copy room id $roomId")
            }) {
                Text("Copy (TODO)")
            }
        }
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