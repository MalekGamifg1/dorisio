package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DorisioRepository
import com.example.model.*
import com.example.ui.components.PitchVisualizer
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel
import com.example.viewmodel.Screen

@Composable
fun MatchDetailsScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val selectedId by viewModel.selectedMatchId.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val allEvents by viewModel.matchEvents.collectAsState()
    val players by viewModel.players.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN

    val match = matches.find { it.matchId == selectedId }
    if (match == null) {
        Box(
            modifier = modifier.fillMaxSize().background(CleanWhiteBg),
            contentAlignment = Alignment.Center
        ) {
            Text("لم يتم تحديد مباراة", color = TextDark)
        }
        return
    }

    val events = allEvents[match.matchId] ?: emptyList()
    val homePlayers = players.filter { it.teamId == match.homeTeamId }
    val awayPlayers = players.filter { it.teamId == match.awayTeamId }

    var selectedTab by remember { mutableStateOf(2) } // Default to Ratings tab (2) or Timeline (0)
    var ratingDialogPlayer by remember { mutableStateOf<Player?>(null) }
    var userRatingScore by remember { mutableStateOf(8.0) }
    var ratingSubmittedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CleanWhiteBg)
    ) {
        // Hero Scoreboard Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status, Round & Venue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (match.status == MatchStatus.LIVE) Color(0xFFFEE2E2) else ModernEmeraldLight
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (match.status) {
                                MatchStatus.LIVE -> "مباشر • د ${match.currentMinute}'"
                                MatchStatus.FINISHED -> "انتهت المباراة 🏁"
                                MatchStatus.SCHEDULED -> "${match.roundName} • ${match.date}"
                                else -> match.status.name
                            },
                            color = if (match.status == MatchStatus.LIVE) LiveRed else ModernEmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${match.venue}",
                        color = TextSecondarySlate,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Teams and Scores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Team
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(match.homeTeamColor))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(match.homeTeamName.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(match.homeTeamName, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }

                    // Scoreboard Display
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CleanWhiteCardElevated)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (match.status in listOf(MatchStatus.LIVE, MatchStatus.FINISHED, MatchStatus.HALFTIME)) {
                            Text(
                                text = "${match.homeScore} - ${match.awayScore}",
                                color = TextDark,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(
                                text = match.startTime,
                                color = TextDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Away Team
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(match.awayTeamColor))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(match.awayTeamName.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(match.awayTeamName, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }

                // Admin Live Center Button
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.LIVE_ADMIN_CENTER) },
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sports, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح لوحة التحكم المباشر بالمباراة")
                    }
                }
            }
        }

        // Feedback Banner
        if (ratingSubmittedMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ModernEmeraldLight),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ModernEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ratingSubmittedMessage!!, color = ModernEmeraldDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { ratingSubmittedMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = ModernEmeraldDark)
                    }
                }
            }
        }

        // Tabs Row: Timeline, Lineup, Ratings, MOTM
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CleanWhiteSurface,
            contentColor = ModernEmerald,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(10.dp))
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("أحداث اللقاء", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("التشكيلة", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("⭐ تقييم اللاعبين", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("👑 رجل المباراة", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (selectedTab) {
                0 -> MatchTimelineTab(events = events, isAdmin = isAdmin, onDeleteEvent = { ev -> viewModel.deleteEvent(match.matchId, ev.eventId) })
                1 -> MatchLineupsTab(match = match, homePlayers = homePlayers, awayPlayers = awayPlayers)
                2 -> MatchRatingsTab(
                    match = match,
                    homePlayers = homePlayers,
                    awayPlayers = awayPlayers,
                    onRatePlayer = { p ->
                        ratingDialogPlayer = p
                        userRatingScore = p.averageRating.coerceAtLeast(6.0)
                    }
                )
                3 -> MatchMotmTab(
                    match = match,
                    players = homePlayers + awayPlayers,
                    isAdmin = isAdmin,
                    onSelectMotm = { p ->
                        viewModel.setManOfTheMatch(match.matchId, p.playerId, p.name)
                        ratingSubmittedMessage = "تم تتويج ${p.name} كرجل المباراة 🏆"
                    }
                )
            }
        }
    }

    // ==========================================
    // Interactive Rating Dialog
    // ==========================================
    if (ratingDialogPlayer != null) {
        val player = ratingDialogPlayer!!
        val faceBitmap = remember(player.profileImage) {
            if (player.profileImage.isNotBlank()) DorisioRepository.base64ToBitmap(player.profileImage) else null
        }

        AlertDialog(
            onDismissRequest = { ratingDialogPlayer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CleanWhiteCardElevated)
                            .border(1.dp, ModernEmerald, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (faceBitmap != null) {
                            Image(bitmap = faceBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Text("#${player.shirtNumber}", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("تقييم اللاعب: ${player.name}", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${player.teamName} • ${player.position}", color = TextSecondarySlate, fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "اختر التقييم الفني للاعب بعد نهاية المباراة:",
                        color = TextSecondarySlate,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Large Score Display
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (userRatingScore >= 8.5) LuxuryGoldLight else if (userRatingScore >= 7.0) ModernEmeraldLight else CleanWhiteCardElevated
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%.1f", userRatingScore),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (userRatingScore >= 8.5) LuxuryGold else if (userRatingScore >= 7.0) ModernEmeraldDark else TextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("★ / 10", fontSize = 14.sp, color = LuxuryGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Slider (1.0 to 10.0 with 0.5 increments)
                    Slider(
                        value = userRatingScore.toFloat(),
                        onValueChange = {
                            val rounded = (Math.round(it * 2) / 2.0)
                            userRatingScore = rounded.coerceIn(1.0, 10.0)
                        },
                        valueRange = 1f..10f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = ModernEmerald,
                            activeTrackColor = ModernEmerald,
                            inactiveTrackColor = CleanWhiteBorder
                        )
                    )

                    // Quick Score Chips
                    Text("أو اختر مباشرة:", color = TextSecondarySlate, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(6.0, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
                        items(presets) { scoreVal ->
                            val isSelected = userRatingScore == scoreVal
                            AssistChip(
                                onClick = { userRatingScore = scoreVal },
                                label = { Text(scoreVal.toString(), fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) ModernEmeraldLight else CleanWhiteCardElevated,
                                    labelColor = if (isSelected) ModernEmeraldDark else TextDark
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (isSelected) ModernEmerald else CleanWhiteBorder
                                )
                            )
                        }
                    }

                    // Admin Shortcut: Set Man of the Match directly
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.setManOfTheMatch(match.matchId, player.playerId, player.name)
                                viewModel.submitRating(match.matchId, player.playerId, userRatingScore.coerceAtLeast(8.5))
                                ratingSubmittedMessage = "تم حفظ تقييم $userRatingScore وتتويج ${player.name} كرجل المباراة! 🏆"
                                ratingDialogPlayer = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تتويج كرجل المباراة (MOTM) ⭐", color = LuxuryGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitRating(match.matchId, player.playerId, userRatingScore)
                        ratingSubmittedMessage = "تم حفظ تقييمك للاعب ${player.name} ($userRatingScore ★) بنجاح!"
                        ratingDialogPlayer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("حفظ التقييم ⭐")
                }
            },
            dismissButton = {
                TextButton(onClick = { ratingDialogPlayer = null }) {
                    Text("إلغاء", color = TextSecondarySlate)
                }
            },
            containerColor = CleanWhiteSurface
        )
    }
}

@Composable
fun MatchRatingsTab(
    match: FootballMatch,
    homePlayers: List<Player>,
    awayPlayers: List<Player>,
    onRatePlayer: (Player) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("تقييم أداء اللاعبين بعد المباراة", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("اضغط على أي لاعب لوضع تقييمه الفني من 1 إلى 10 بعد نهاية اللقاء", color = TextSecondarySlate, fontSize = 11.sp)
                    }
                }
            }
        }

        // Home Team Section
        item {
            Text("🛡️ لاعبو ${match.homeTeamName}:", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }

        items(homePlayers) { player ->
            PlayerRatingCard(player = player, onRate = { onRatePlayer(player) })
        }

        // Away Team Section
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("🛡️ لاعبو ${match.awayTeamName}:", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        items(awayPlayers) { player ->
            PlayerRatingCard(player = player, onRate = { onRatePlayer(player) })
        }
    }
}

@Composable
fun PlayerRatingCard(player: Player, onRate: () -> Unit) {
    val faceBitmap = remember(player.profileImage) {
        if (player.profileImage.isNotBlank()) DorisioRepository.base64ToBitmap(player.profileImage) else null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRate() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CleanWhiteCardElevated)
                        .border(1.dp, ModernEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (faceBitmap != null) {
                        Image(bitmap = faceBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Text("#${player.shirtNumber}", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(player.name, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${player.position} (#${player.shirtNumber})", color = TextSecondarySlate, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Score Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (player.averageRating >= 8.0) ModernEmeraldLight else CleanWhiteCardElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (player.averageRating > 0) "${player.averageRating} ★" else "- ★",
                        color = if (player.averageRating >= 8.0) ModernEmeraldDark else TextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRate,
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("قيّم ⭐", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MatchTimelineTab(
    events: List<MatchEvent>,
    isAdmin: Boolean,
    onDeleteEvent: (MatchEvent) -> Unit
) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد أحداث مسجلة في هذا اللقاء حتى الآن", color = TextSecondarySlate, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(events.sortedBy { it.minute }) { event ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${event.minute}'", color = ModernEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        val (icon, label) = when (event.type) {
                            EventType.GOAL -> "⚽" to "هدف: ${event.playerName} (${event.teamName})"
                            EventType.ASSIST -> "🎯" to "صناعة: ${event.playerName}"
                            EventType.YELLOW_CARD -> "🟨" to "إنذار: ${event.playerName}"
                            EventType.RED_CARD -> "🟥" to "طرد: ${event.playerName}"
                            EventType.SUBSTITUTION -> "🔄" to "تبديل: ${event.playerName} ⬅️ ${event.subPlayerName}"
                            EventType.MATCH_START -> "⏱️" to "صافرة البداية"
                            EventType.HALF_TIME -> "⏸️" to "نهاية الشوط الأول"
                            EventType.SECOND_HALF -> "▶️" to "الشوط الثاني"
                            EventType.FULL_TIME -> "🏁" to "صافرة النهاية"
                        }
                        Text(icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    if (isAdmin) {
                        IconButton(onClick = { onDeleteEvent(event) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الحدث", tint = CardRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchLineupsTab(
    match: FootballMatch,
    homePlayers: List<Player>,
    awayPlayers: List<Player>
) {
    var showHomeTeam by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showHomeTeam = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showHomeTeam) ModernEmerald else CleanWhiteSurface
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showHomeTeam) ModernEmerald else CleanWhiteBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text(match.homeTeamName, color = if (showHomeTeam) Color.White else TextDark, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showHomeTeam = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showHomeTeam) ModernEmerald else CleanWhiteSurface
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (!showHomeTeam) ModernEmerald else CleanWhiteBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text(match.awayTeamName, color = if (!showHomeTeam) Color.White else TextDark, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showHomeTeam) {
            PitchVisualizer(
                teamName = match.homeTeamName,
                teamColorHex = match.homeTeamColor,
                players = homePlayers
            )
        } else {
            PitchVisualizer(
                teamName = match.awayTeamName,
                teamColorHex = match.awayTeamColor,
                players = awayPlayers
            )
        }
    }
}

@Composable
fun MatchMotmTab(
    match: FootballMatch,
    players: List<Player>,
    isAdmin: Boolean,
    onSelectMotm: (Player) -> Unit
) {
    val motmPlayer = players.find { it.playerId == match.manOfTheMatchPlayerId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (match.manOfTheMatchPlayerName.isNotBlank()) {
            com.example.ui.components.MotmHeroCard(
                playerName = match.manOfTheMatchPlayerName,
                teamName = motmPlayer?.teamName ?: "الفريق الفائز",
                ratingScore = motmPlayer?.averageRating?.coerceAtLeast(8.5) ?: 9.5,
                goals = motmPlayer?.goals ?: 2,
                assists = motmPlayer?.assists ?: 1,
                customPhotoBase64 = motmPlayer?.profileImage ?: ""
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = LuxuryGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لم يتم اختيار رجل المباراة بعد",
                            color = TextDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يقوم المشرف بتحديد نجم اللقاء ليحصل على بطاقة التتويج الذهبية",
                            color = TextSecondarySlate,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (isAdmin) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "اختيار أو تغيير رجل المباراة:",
                color = TextDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(players) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectMotm(p) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${p.name} (${p.teamName})", color = TextDark, fontSize = 13.sp)
                            Text("تتويج 🏆", color = LuxuryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
