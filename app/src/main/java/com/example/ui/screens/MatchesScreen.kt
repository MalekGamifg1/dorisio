package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.TeamLogoView
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel
import com.example.viewmodel.Screen

@Composable
fun MatchesScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val matches by viewModel.matches.collectAsState()
    val filter by viewModel.matchesFilter.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN

    var selectedRound by remember { mutableStateOf(0) } // 0: All rounds

    val distinctRounds = remember(matches) {
        matches.map { it.round }.distinct().sorted()
    }

    val filteredMatches = matches.filter { m ->
        val statusMatches = when (filter) {
            "LIVE" -> m.status == MatchStatus.LIVE
            "SCHEDULED" -> m.status == MatchStatus.SCHEDULED
            "FINISHED" -> m.status == MatchStatus.FINISHED
            else -> true
        }
        val roundMatches = if (selectedRound == 0) true else m.round == selectedRound
        statusMatches && roundMatches
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CleanWhiteBg)
            .padding(horizontal = 16.dp)
    ) {
        // Status Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                "ALL" to "جميع المباريات",
                "LIVE" to "مباشر 🔴",
                "SCHEDULED" to "القادمة ⏱️",
                "FINISHED" to "المنتهية 🏁"
            )
            items(chips) { (key, label) ->
                val isSelected = filter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setMatchesFilter(key) },
                    label = {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ModernEmerald,
                        containerColor = CleanWhiteSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ModernEmerald else CleanWhiteBorder
                    )
                )
            }
        }

        // Weeks / Rounds Selector (e.g. الأسبوع 1، الأسبوع 2 ... حتى الأسبوع 8)
        if (distinctRounds.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { selectedRound = 0 },
                        label = { Text("كل الجولات (${matches.size})", fontSize = 11.sp, fontWeight = if (selectedRound == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedRound == 0) LuxuryGoldLight else CleanWhiteSurface,
                            labelColor = if (selectedRound == 0) LuxuryGold else TextSecondarySlate
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (selectedRound == 0) LuxuryGold else CleanWhiteBorder
                        )
                    )
                }

                items(distinctRounds) { roundNum ->
                    val isRoundSelected = selectedRound == roundNum
                    AssistChip(
                        onClick = { selectedRound = roundNum },
                        label = { Text("الجولة $roundNum (الأسبوع $roundNum)", fontSize = 11.sp, fontWeight = if (isRoundSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isRoundSelected) ModernEmeraldLight else CleanWhiteSurface,
                            labelColor = if (isRoundSelected) ModernEmeraldDark else TextSecondarySlate
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isRoundSelected) ModernEmerald else CleanWhiteBorder
                        )
                    )
                }
            }
        }

        // Matches List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredMatches.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = null,
                                tint = ModernEmerald,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (matches.isEmpty()) "لم يتم إنشاء جدول المباريات بعد" else "لا توجد مباريات في هذه الجولة",
                                color = TextDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (matches.isEmpty()) "يمكنك توليد جدول دوري كامل (Round-Robin) لجميع الجولات بضغطة زر واحدة من لوحة المشرف." else "جرّب اختيار جولة أخرى أو فلتر مختلف.",
                                color = TextSecondarySlate,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            if (matches.isEmpty() && isAdmin) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("توليد جدول الدوري تلقائياً")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredMatches) { match ->
                    MatchCardItem(
                        match = match,
                        isAdmin = isAdmin,
                        onClick = { viewModel.selectMatch(match.matchId, openLiveAdmin = false) },
                        onAdminLiveClick = { viewModel.selectMatch(match.matchId, openLiveAdmin = true) }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCardItem(
    match: FootballMatch,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onAdminLiveClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (match.status == MatchStatus.LIVE) ModernEmerald else CleanWhiteBorder
        ),
        elevation = CardDefaults.cardElevation(if (match.status == MatchStatus.LIVE) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Status & Round Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CleanWhiteCardElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = match.roundName,
                        color = TextSecondarySlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Match Status Badge
                when (match.status) {
                    MatchStatus.LIVE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مباشر • د ${match.currentMinute}'",
                                color = LiveRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    MatchStatus.FINISHED -> {
                        Text(
                            text = "انتهت المباراة",
                            color = TextMutedSlate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    MatchStatus.SCHEDULED -> {
                        Text(
                            text = "${match.date} • ${match.startTime}",
                            color = ModernEmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams Scoreboard Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoView(
                        logo = match.homeTeamLogo,
                        teamName = match.homeTeamName,
                        colorHex = match.homeTeamColor,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.homeTeamName,
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Score Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CleanWhiteCardElevated)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (match.status in listOf(MatchStatus.LIVE, MatchStatus.FINISHED, MatchStatus.HALFTIME)) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            color = TextDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Text(
                            text = match.startTime,
                            color = TextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Away Team
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.awayTeamName,
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TeamLogoView(
                        logo = match.awayTeamLogo,
                        teamName = match.awayTeamName,
                        colorHex = match.awayTeamColor,
                        size = 32.dp
                    )
                }
            }

            // Venue
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextMutedSlate,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.venue,
                        color = TextSecondarySlate,
                        fontSize = 11.sp
                    )
                }

                // Admin Live Center Button
                if (isAdmin) {
                    TextButton(
                        onClick = onAdminLiveClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sports,
                            contentDescription = null,
                            tint = ModernEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "التحكم بالمباراة ⚡",
                            color = ModernEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
