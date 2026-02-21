package com.spundev.webrtcshare.ui.screens.joinRequest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import com.spundev.webrtcshare.R
import kotlinx.coroutines.launch

@Composable
fun JoinRequestRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val fieldState = rememberTextFieldState()
    Column(modifier = Modifier.safeDrawingPadding()) {
        TextField(state = fieldState)

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
                contentDescription = "Paste",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Paste")
        }
        Button(onClick = {
            onNavigateToRoom(fieldState.text.toString())
        }) {
            Text("Join room")
        }
    }
}
