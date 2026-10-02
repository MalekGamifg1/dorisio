package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun TeamLogoView(
    logo: String,
    teamName: String,
    colorHex: String,
    size: Dp = 32.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    val teamColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF10B981)
        }
    }

    val parsedBitmap = remember(logo) {
        if (logo.isNotBlank() && !logo.startsWith("http") && logo.length > 20) {
            try {
                val cleanBase64 = if (logo.contains(",")) logo.substringAfter(",") else logo
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    listOf(
                        teamColor.copy(alpha = 0.85f),
                        teamColor
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            parsedBitmap != null -> {
                Image(
                    bitmap = parsedBitmap.asImageBitmap(),
                    contentDescription = "شعار $teamName",
                    modifier = Modifier.fillMaxSize().clip(shape),
                    contentScale = ContentScale.Crop
                )
            }
            logo.startsWith("http") -> {
                AsyncImage(
                    model = logo,
                    contentDescription = "شعار $teamName",
                    modifier = Modifier.fillMaxSize().clip(shape),
                    contentScale = ContentScale.Crop
                )
            }
            logo.isNotBlank() && logo.length <= 4 -> {
                // Emoji badge
                Text(
                    text = logo,
                    fontSize = (size.value * 0.55f).sp
                )
            }
            else -> {
                // Default letter badge
                Text(
                    text = teamName.take(1).ifBlank { "⚽" },
                    color = Color.White,
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
