package com.spundev.webrtcshare.ui.screens.joinRequest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun JoinRequestRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit
) {
    val fieldState = rememberTextFieldState()
    Column(modifier = Modifier.safeDrawingPadding()) {
        TextField(state = fieldState)
        Button(onClick = {
            onNavigateToRoom(fieldState.text.toString())
        }) {
            Text("Join room")
        }
    }
}
