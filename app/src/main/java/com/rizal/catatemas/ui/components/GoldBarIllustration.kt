package com.rizal.catatemas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizal.catatemas.data.PhotoStore
import com.rizal.catatemas.ui.theme.GoldIllustrationGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ilustrasi "batang emas" sebagai foto merk: ada emblem merk + berat,
 * kalau user menyimpan foto sendiri maka fotonya yang ditampilkan.
 */
@Composable
fun GoldBarIllustration(
    emblem: String,
    weightText: String,
    accent: Color,
    modifier: Modifier = Modifier,
    photoPath: String? = null,
    cornerPercent: Int = 28,
    showText: Boolean = true,
) {
    val bitmap = rememberFileBitmap(photoPath)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = cornerPercent))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            IngotArt(emblem = emblem, weightText = weightText, showText = showText)
        }
    }
}

@Composable
private fun IngotArt(emblem: String, weightText: String, showText: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val top = height * 0.22f
            val bottom = height * 0.76f

            val body = Path().apply {
                moveTo(width * 0.30f, top)
                lineTo(width * 0.70f, top)
                lineTo(width * 0.87f, bottom)
                lineTo(width * 0.13f, bottom)
                close()
            }
            drawPath(body, brush = GoldIllustrationGradient)
            drawPath(
                path = body,
                color = Color(0x33000000),
                style = Stroke(width = height * 0.012f),
            )

            val shine = Path().apply {
                moveTo(width * 0.30f, top)
                lineTo(width * 0.70f, top)
                lineTo(width * 0.745f, height * 0.37f)
                lineTo(width * 0.255f, height * 0.37f)
                close()
            }
            drawPath(shine, Color.White.copy(alpha = 0.4f))

            drawCircle(
                color = Color(0x33FFFFFF),
                radius = height * 0.03f,
                center = Offset(width * 0.76f, height * 0.30f),
            )
        }

        if (showText) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = emblem,
                    color = Color(0xFF453206),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = weightText,
                    color = Color(0xFF5C4410),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun rememberFileBitmap(path: String?, maxSize: Int = 640): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, path, maxSize) {
        value = if (path.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                PhotoStore.decodeFile(path, maxSize)?.asImageBitmap()
            }
        }
    }.value
}
