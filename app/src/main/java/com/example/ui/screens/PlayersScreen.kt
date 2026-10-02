package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.model.SeasonAward
import com.example.model.WeeklyAward
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel

@Composable
fun PlayersScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val players by viewModel.players.collectAsState()
    val weeklyAwards by viewModel.weeklyAwards.collectAsState()
    val seasonAwards by viewModel.seasonAwards.collectAsState()
    val activeTab by viewModel.playersTab.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 16.dp)
    ) {
        // Tab Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "SCORERS" to "🏆 الهدافون",
                "ASSISTS" to "🎯 الصناعة",
                "RATINGS" to "⭐ التقييم",
                "AWARDS" to "🎖️ الجوائز التذكارية",
                "ALL" to "قائمة اللاعبين"
            )
            items(tabs) { (key, label) ->
                val isSelected = activeTab == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setPlayersTab(key) },
                    label = {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PitchGreenPrimary,
                        containerColor = CharcoalSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) PitchGreenVibrant else CharcoalBorder
                    )
                )
            }
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (activeTab) {
                "SCORERS" -> {
                    val sorted = players.sortedByDescending { it.goals }
                    items(sorted) { player ->
                        LeaderboardCard(
                            rank = sorted.indexOf(player) + 1,
                            player = player,
                            mainStat = "${player.goals} أهداف",
                            subStat = "${player.appearances} مباريات",
                            badgeColor = GoldBright
                        )
                    }
                }
                "ASSISTS" -> {
                    val sorted = players.sortedByDescending { it.assists }
                    items(sorted) { player ->
                        LeaderboardCard(
                            rank = sorted.indexOf(player) + 1,
                            player = player,
                            mainStat = "${player.assists} صناعة",
                            subStat = "${player.appearances} مباريات",
                            badgeColor = PitchGreenVibrant
                        )
                    }
                }
                "RATINGS" -> {
                    val sorted = players.sortedByDescending { it.averageRating }
                    items(sorted) { player ->
                        LeaderboardCard(
                            rank = sorted.indexOf(player) + 1,
                            player = player,
                            mainStat = "⭐ ${player.averageRating}",
                            subStat = "${player.manOfTheMatchCount} رجل المباراة",
                            badgeColor = GoldAccent
                        )
                    }
                }
                "AWARDS" -> {
                    // Weekly and Season Awards
                    item {
                        Text(
                            text = "⭐ جوائز أفضل لاعب في الأسبوع:",
                            color = GoldBright,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(weeklyAwards) { wa ->
                        WeeklyAwardCardItem(wa)
                    }

                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "🏆 جوائز وتكريمات الموسم الدائمة:",
                            color = GoldBright,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(seasonAwards) { sa ->
                        SeasonAwardCardItem(sa)
                    }
                }
                "ALL" -> {
                    items(players.sortedBy { it.teamName }) { player ->
                        AllPlayerRowItem(player)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(
    rank: Int,
    player: Player,
    mainStat: String,
    subStat: String,
    badgeColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) Color(0xFF1E261D) else CharcoalSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (rank == 1) GoldAccent else CharcoalBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (rank <= 3) GoldContainer else CharcolaCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (rank == 1) "🥇" else if (rank == 2) "🥈" else if (rank == 3) "🥉" else "$rank",
                        fontSize = if (rank <= 3) 14.sp else 12.sp,
                        color = if (rank <= 3) GoldBright else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Avatar / Shirt
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PitchGreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${player.shirtNumber}",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = player.name,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${player.teamName} • ${player.position}",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = mainStat,
                    color = badgeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subStat,
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun WeeklyAwardCardItem(wa: WeeklyAward) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الجولة ${wa.weekNumber}",
                    color = GoldBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldAccent)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("تقييم ${wa.rating}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${wa.playerName} (${wa.teamName})",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = wa.reason,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SeasonAwardCardItem(sa: SeasonAward) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211D13)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBright),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GoldDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldBright, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = sa.title, color = GoldBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "${sa.winnerName} • ${sa.teamName}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (sa.prizeInfo.isNotBlank()) {
                    Text(text = "الجائزة التكريمية: ${sa.prizeInfo}", color = TextDim, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AllPlayerRowItem(player: Player) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CharcolaCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${player.shirtNumber}", color = GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(player.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${player.teamName} • ${player.position}", color = TextDim, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚽ ${player.goals}", color = TextWhite, fontSize = 12.sp)
                Text("🎯 ${player.assists}", color = TextMuted, fontSize = 12.sp)
                Text("🟨 ${player.yellowCards}", color = CardYellow, fontSize = 12.sp)
            }
        }
    }
}
