package com.spundev.webrtcshare.ui.screens.createRoom

import android.content.ClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.ui.components.QRCode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateRoomRoute(
    onNavigateBack: () -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val roomId by viewModel.roomId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Text("isConnected: $isConnected")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("roomId: $roomId")

            // Copy to clipboard buton
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
                Text("Copy")
            }
        }

        // Send message button
        Button(
            enabled = isConnected,
            onClick = { viewModel.sendMessage("Eagle") }
        ) {
            Text("Send Eagle")
        }

       roomId?.let {
            QRCode(
                text = it,
                snapToPixel = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp)
            )
        }

        messages.forEach {
            Text(it)
        }
    }
}
