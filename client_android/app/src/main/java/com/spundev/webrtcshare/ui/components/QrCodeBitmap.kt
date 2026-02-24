package com.spundev.webrtcshare.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Alternative implementation using a Bitmap instead of a Painter

@Composable
fun QRCodeBitmap(
    text: String,
    foregroundColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, text) {
        withContext(Dispatchers.Default) {
            val bitMatrix = generateQrCodeBitMatrix(text = text)
            val bitmap = generateBitmapFromBitMatrix(
                bitMatrix = bitMatrix,
                foregroundColor = foregroundColor.toArgb(),
                backgroundColor = backgroundColor.toArgb()
            )
            value = bitmap.asImageBitmap()
        }
    }

    qrBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "QR code for $text",
            filterQuality = FilterQuality.None,
            modifier = modifier
        )
    }
}

private fun generateBitmapFromBitMatrix(
    bitMatrix: BitMatrix,
    foregroundColor: Int,
    backgroundColor: Int
): Bitmap {
    val width = bitMatrix.width
    val height = bitMatrix.height

    val bitmap = createBitmap(width, height)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap[x, y] = if (bitMatrix[x, y]) {
                foregroundColor
            } else {
                backgroundColor
            }
        }
    }
    return bitmap
}