package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.ui.theme.*

@Composable
fun PitchVisualizer(
    teamName: String,
    teamColorHex: String,
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    val teamColor = try {
        Color(android.graphics.Color.parseColor(teamColorHex))
    } catch (e: Exception) {
        PitchGreenVibrant
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Team indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(teamColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تشكيلة: $teamName",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Green Pitch Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F4725))
                .border(2.dp, Color(0xFF1E824C), RoundedCornerShape(16.dp))
        ) {
            // Pitch field lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grass stripes
                val stripes = 6
                val stripeH = h / stripes
                for (i in 0 until stripes) {
                    if (i % 2 == 0) {
                        drawRect(
                            color = Color(0x15FFFFFF),
                            topLeft = Offset(0f, i * stripeH),
                            size = Size(w, stripeH)
                        )
                    }
                }

                val lineCol = Color(0x60FFFFFF)
                val stroke = Stroke(width = 3f)

                // Outer border
                drawRect(
                    color = lineCol,
                    topLeft = Offset(20f, 20f),
                    size = Size(w - 40f, h - 40f),
                    style = stroke
                )

                // Halfway line
                drawLine(
                    color = lineCol,
                    start = Offset(20f, h / 2),
                    end = Offset(w - 20f, h / 2),
                    strokeWidth = 3f
                )

                // Center circle
                drawCircle(
                    color = lineCol,
                    radius = 45f,
                    center = Offset(w / 2, h / 2),
                    style = stroke
                )

                // Center dot
                drawCircle(
                    color = lineCol,
                    radius = 4f,
                    center = Offset(w / 2, h / 2)
                )

                // Penalty box top
                drawRect(
                    color = lineCol,
                    topLeft = Offset(w * 0.25f, 20f),
                    size = Size(w * 0.5f, 55f),
                    style = stroke
                )

                // Penalty box bottom
                drawRect(
                    color = lineCol,
                    topLeft = Offset(w * 0.25f, h - 75f),
                    size = Size(w * 0.5f, 55f),
                    style = stroke
                )
            }

            // Overlay Players on Pitch: GK at bottom, DEF above, MID, ATT at top
            val gk = players.find { it.position.contains("حارس") } ?: players.getOrNull(0)
            val defenders = players.filter { it.position.contains("مدافع") }
            val midfielders = players.filter { it.position.contains("وسط") }
            val attackers = players.filter { it.position.contains("مهاجم") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Attackers row (Top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (attackers.isNotEmpty()) {
                        attackers.forEach { PitchPlayerNode(it, teamColor) }
                    } else {
                        players.getOrNull(1)?.let { PitchPlayerNode(it, teamColor) }
                    }
                }

                // Midfielders row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (midfielders.isNotEmpty()) {
                        midfielders.forEach { PitchPlayerNode(it, teamColor) }
                    } else {
                        players.getOrNull(2)?.let { PitchPlayerNode(it, teamColor) }
                    }
                }

                // Defenders row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (defenders.isNotEmpty()) {
                        defenders.forEach { PitchPlayerNode(it, teamColor) }
                    } else {
                        players.getOrNull(3)?.let { PitchPlayerNode(it, teamColor) }
                    }
                }

                // Goalkeeper (Bottom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    gk?.let { PitchPlayerNode(it, Color(0xFFF59E0B), isGk = true) }
                }
            }
        }
    }
}

@Composable
fun PitchPlayerNode(
    player: Player,
    accentColor: Color,
    isGk: Boolean = false
) {
    val faceBitmap = remember(player.profileImage) {
        if (player.profileImage.isNotBlank()) {
            com.example.data.DorisioRepository.base64ToBitmap(player.profileImage)
        } else null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isGk) GoldDark else Color.White)
                .border(2.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (faceBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = faceBitmap.asImageBitmap(),
                    contentDescription = player.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = "${player.shirtNumber}",
                    color = if (isGk) Color.White else Color(0xFF0F172A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                text = player.name.split(" ").firstOrNull() ?: player.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
