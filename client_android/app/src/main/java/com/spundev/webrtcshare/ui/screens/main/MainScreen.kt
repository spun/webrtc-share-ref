package com.spundev.webrtcshare.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainRoute(
    onNavigateToCreate: () -> Unit,
    onNavigateToJoin: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(onClick = onNavigateToCreate) {
            Text("Create room")
        }
        Button(onClick = onNavigateToJoin) {
            Text("Join room")
        }
    }
}
