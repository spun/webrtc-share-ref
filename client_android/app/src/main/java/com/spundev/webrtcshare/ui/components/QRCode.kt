package com.spundev.webrtcshare.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.spundev.webrtcshare.ui.theme.WebRTCShareTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.min

@Composable
fun QRCode(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "QR code for $text",
    color: Color = Color.Unspecified,
    alignment: Alignment = Alignment.Center,
    snapToPixel: Boolean = false,
) {
    val qrPathData by produceState<QrPathData?>(initialValue = null, text) {
        withContext(Dispatchers.Default) {
            val bitMatrix = generateQrCodeBitMatrix(text = text)
            value = QrPathData.fromBitMatrix(bitMatrix)
        }
    }

    qrPathData?.let {
        QRCodeContent(
            qrPathData = it,
            modifier = modifier,
            contentDescription = contentDescription,
            color = color,
            alignment = alignment,
            snapToPixel = snapToPixel,
        )
    }
}

@Composable
private fun QRCodeContent(
    qrPathData: QrPathData,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    alignment: Alignment = Alignment.Center,
    snapToPixel: Boolean = false,
) {
    val qrColor = color.takeOrElse { LocalContentColor.current }

    val painter = remember(qrPathData, qrColor, snapToPixel) {
        QRCodePainter(
            qrPathData = qrPathData,
            foregroundColor = qrColor,
            snapToPixel = snapToPixel
        )
    }

    // We use Image instead of Canvas to use its semantics configuration
    // instead of recreating them.
    Image(
        painter = painter,
        contentDescription = contentDescription,
        alignment = alignment,
        modifier = modifier
    )
}

// Wrapper around the qr Path we use to generate (and cache) the qr path in a
// different thread instead of doing it inside the Painter.
private data class QrPathData(
    val path: Path,
    val width: Int,
    val height: Int,
) {
    companion object {
        fun fromBitMatrix(bitMatrix: BitMatrix): QrPathData {
            val path = Path().apply {
                for (y in 0 until bitMatrix.height) {
                    // merge multiple qr modules in row
                    var rowModulesStartX = -1
                    for (x in 0 until bitMatrix.width) {
                        if (bitMatrix[x, y]) {
                            if (rowModulesStartX == -1) {
                                rowModulesStartX = x
                            }
                        } else if (rowModulesStartX != -1) {
                            addRect(
                                Rect(
                                    rowModulesStartX.toFloat(),
                                    y.toFloat(),
                                    x.toFloat(),
                                    y + 1f
                                )
                            )
                            rowModulesStartX = -1
                        }

                    }
                    // flush last run if needed
                    if (rowModulesStartX != -1) {
                        addRect(
                            Rect(
                                rowModulesStartX.toFloat(),
                                y.toFloat(),
                                bitMatrix.width.toFloat(),
                                y + 1f
                            )
                        )
                    }
                }
            }
            return QrPathData(
                path = path,
                width = bitMatrix.width,
                height = bitMatrix.height
            )
        }
    }
}

/**
 * An [Image] [Painter] that draws the path of a QR code
 * @param qrPathData Precalculated path of the QR code
 * @param foregroundColor Color of the QR "modules"/squares
 * @param snapToPixel When true, QR modules will be aligned to pixel boundaries.
 *  This avoids blurry lines but might leave extra space around the QR code.
 */
private class QRCodePainter(
    val qrPathData: QrPathData,
    val foregroundColor: Color,
    val snapToPixel: Boolean
) : Painter() {

    override val intrinsicSize: Size = Size(qrPathData.width.toFloat(), qrPathData.height.toFloat())

    override fun DrawScope.onDraw() {

        // calculate the size of each square or "module"
        val minModuleSize = min(
            size.width / qrPathData.width,
            size.height / qrPathData.height
        )
        val moduleSize = if (snapToPixel) {
            floor(minModuleSize)
        } else minModuleSize

        // calculate the size of the final qr
        val qrWidth = qrPathData.width * moduleSize
        val qrHeight = qrPathData.height * moduleSize

        // get the offset required to center the QR
        val offsetX = (size.width - qrWidth) / 2f
        val offsetY = (size.height - qrHeight) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(moduleSize, moduleSize, Offset(0f, 0f))
        }) {
            // use path to avoid the faint divider lines we were
            // getting with multiple drawRect calls
            drawPath(
                path = qrPathData.path,
                color = foregroundColor,
            )
        }
    }
}

internal fun generateQrCodeBitMatrix(
    text: String,
): BitMatrix {
    val writer = QRCodeWriter()
    return writer.encode(
        text, BarcodeFormat.QR_CODE,
        0, // use the minimum required width
        0, // use the minimum required height
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
    )
}


@Preview
@Composable
private fun QrCodePreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(120.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}

@Preview
@Composable
private fun QrCodeAlignedToPixelPreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(120.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            snapToPixel = true,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}

@Preview
@Composable
private fun QrCodeColorPreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(120.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            color = Color.DarkGray,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QrCodeThemeColorPreview() {
    WebRTCShareTheme {
        val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp)
        ) {
            QRCodeContent(
                qrPathData = qrPathData,
                contentDescription = "QR code preview",
                modifier = Modifier
                    .padding(8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun TallQrCodePreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(60.dp, 120.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}

@Preview
@Composable
private fun WideQrCodePreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(120.dp, 60.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}

@Preview
@Composable
private fun TallQrCodeWithAlignmentPreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(60.dp, 120.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}

@Preview
@Composable
private fun WideQrCodeWithAlignmentPreview() {
    val qrPathData = QrPathData.fromBitMatrix(generateQrCodeBitMatrix("Preview"))
    Box(modifier = Modifier.size(120.dp, 60.dp)) {
        QRCodeContent(
            qrPathData = qrPathData,
            contentDescription = "QR code preview",
            alignment = Alignment.CenterEnd,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}
