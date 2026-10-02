package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LeagueStanding
import com.example.ui.components.TeamLogoView
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel

@Composable
fun StandingsScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val standings by viewModel.standings.collectAsState()
    val league by viewModel.currentLeague.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // League info banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "جدول ترتيب ${league.name}",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحديث فوري وتلقائي مع نهاية كل مباراة",
                            color = PitchGreenVibrant,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "فوز = 3ن | تعادل = 1ن",
                        color = GoldBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Standings Table Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                // Table Columns Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المركز", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text("الفريق", color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("لعب", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("فوز", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("تعادل", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("خسارة", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("له", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("عليه", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("فارق", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("نقاط", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)

                // Table Rows
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(standings) { row ->
                        StandingRowItem(row)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rules footer note
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = TextDim, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "يتم الترتيب حسب: النقاط أولاً، ثم فارق الأهداف، ثم الأهداف المسجلة.",
                color = TextDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StandingRowItem(row: LeagueStanding) {
    val isLeader = row.position == 1
    val isRunnerUp = row.position == 2

    val rowBg = when {
        isLeader -> Color(0xFF1B2A1E)
        isRunnerUp -> Color(0xFF1C222B)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .border(
                width = if (isLeader) 1.dp else 0.dp,
                color = if (isLeader) GoldAccent.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Row(
            modifier = Modifier.width(36.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLeader) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = "${row.position}",
                color = if (isLeader) GoldBright else TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Team Name & Badge
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamLogoView(
                logo = row.teamLogo,
                teamName = row.teamName,
                colorHex = row.teamColor,
                size = 24.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = row.teamName,
                color = if (isLeader) GoldBright else TextWhite,
                fontSize = 12.sp,
                fontWeight = if (isLeader) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Stats
        Text("${row.played}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        Text("${row.won}", color = TextWhite, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        Text("${row.drawn}", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        Text("${row.lost}", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        Text("${row.goalsFor}", color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        Text("${row.goalsAgainst}", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        Text(
            text = if (row.goalDifference > 0) "+${row.goalDifference}" else "${row.goalDifference}",
            color = if (row.goalDifference > 0) PitchGreenVibrant else if (row.goalDifference < 0) CardRed else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${row.points}",
            color = if (isLeader) GoldBright else PitchGreenVibrant,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.Center
        )
    }
}
