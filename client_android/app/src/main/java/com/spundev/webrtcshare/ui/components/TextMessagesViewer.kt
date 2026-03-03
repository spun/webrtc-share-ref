package com.spundev.webrtcshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.spundev.webrtcshare.model.TextMessage

@Composable
fun TextMessagesViewer(
    messages: List<TextMessage>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
        modifier = modifier.fillMaxWidth()
    ) {
        items(messages) { message ->
            TextMessageRow(message)
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
        modifier = Modifier.fillMaxWidth()
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
