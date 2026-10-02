package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel
import com.example.viewmodel.Screen

@Composable
fun LiveAdminCenterScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val selectedId by viewModel.selectedMatchId.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val allEvents by viewModel.matchEvents.collectAsState()
    val players by viewModel.players.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val match = matches.find { it.matchId == selectedId }
    if (match == null) {
        Box(modifier = modifier.fillMaxSize().background(CharcoalBg), contentAlignment = Alignment.Center) {
            Text("المباراة غير متوفرة", color = TextWhite)
        }
        return
    }

    val events = allEvents[match.matchId] ?: emptyList()
    val homePlayers = players.filter { it.teamId == match.homeTeamId }
    val awayPlayers = players.filter { it.teamId == match.awayTeamId }

    // Dialog state for adding event
    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEventType by remember { mutableStateOf(EventType.GOAL) }
    var selectedTeamId by remember { mutableStateOf(match.homeTeamId) }
    var selectedPlayerId by remember { mutableStateOf("") }
    var subPlayerId by remember { mutableStateOf("") }
    var eventMinute by remember { mutableStateOf(match.currentMinute.coerceAtLeast(1)) }
    var eventDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection & Pitch Admin Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PitchGreenDark),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PitchGreenVibrant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape).background(PitchGreenVibrant)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مركز البث المباشر (المشرف الميداني)",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = connectionStatus,
                        color = GoldBright,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live Scoreboard & Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Match State Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AssistChip(
                            onClick = { viewModel.updateMatchStatus(match.matchId, MatchStatus.LIVE) },
                            label = { Text("بدء اللقاء ⏱️", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (match.status == MatchStatus.LIVE) PitchGreenPrimary else CharcoalCard
                            )
                        )
                        AssistChip(
                            onClick = { viewModel.updateMatchStatus(match.matchId, MatchStatus.HALFTIME) },
                            label = { Text("استراحة ⏸️", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (match.status == MatchStatus.HALFTIME) GoldDark else CharcoalCard
                            )
                        )
                        AssistChip(
                            onClick = { viewModel.updateMatchStatus(match.matchId, MatchStatus.FINISHED) },
                            label = { Text("صافرة النهاية 🏁", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (match.status == MatchStatus.FINISHED) CardRed else CharcoalCard
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Teams and Live Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Team
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(match.homeTeamName, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${match.homeScore}", color = TextWhite, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        // Minute Pill
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CharcolaCardElevated)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${match.currentMinute}'",
                                    color = GoldBright,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (match.status) {
                                    MatchStatus.LIVE -> "مباشر"
                                    MatchStatus.HALFTIME -> "استراحة"
                                    MatchStatus.FINISHED -> "منتهية"
                                    else -> "مجدولة"
                                },
                                color = if (match.status == MatchStatus.LIVE) LiveRed else TextDim,
                                fontSize = 11.sp
                            )
                        }

                        // Away Team
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(match.awayTeamName, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${match.awayScore}", color = TextWhite, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Post-match Ratings & MOTM Action
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LuxuryGoldLight),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold),
                modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Screen.MATCH_DETAILS) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = LuxuryGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("وضع تقييمات اللاعبين ورجل المباراة ⭐", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تقييم كل لاعب من 1 إلى 10 وتتويج نجم اللقاء", color = TextSecondarySlate, fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo(Screen.MATCH_DETAILS) },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("التقييمات", color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Event Adder Bar (One-Tap Recording)
        item {
            Text(
                text = "⚡ تسجيل حدث مباشر وسريع:",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Goal
                Button(
                    onClick = {
                        selectedEventType = EventType.GOAL
                        selectedTeamId = match.homeTeamId
                        eventMinute = match.currentMinute.coerceAtLeast(1)
                        selectedPlayerId = homePlayers.firstOrNull()?.playerId ?: ""
                        showAddEventDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_quick_goal")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚽", fontSize = 18.sp)
                        Text("هدف", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }

                // Assist
                Button(
                    onClick = {
                        selectedEventType = EventType.ASSIST
                        selectedTeamId = match.homeTeamId
                        eventMinute = match.currentMinute.coerceAtLeast(1)
                        selectedPlayerId = homePlayers.firstOrNull()?.playerId ?: ""
                        showAddEventDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 18.sp)
                        Text("صناعة", fontSize = 11.sp, color = GoldBright, fontWeight = FontWeight.Bold)
                    }
                }

                // Yellow Card
                Button(
                    onClick = {
                        selectedEventType = EventType.YELLOW_CARD
                        selectedTeamId = match.homeTeamId
                        eventMinute = match.currentMinute.coerceAtLeast(1)
                        selectedPlayerId = homePlayers.firstOrNull()?.playerId ?: ""
                        showAddEventDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟨", fontSize = 18.sp)
                        Text("إنذار", fontSize = 11.sp, color = CardYellow, fontWeight = FontWeight.Bold)
                    }
                }

                // Red Card
                Button(
                    onClick = {
                        selectedEventType = EventType.RED_CARD
                        selectedTeamId = match.homeTeamId
                        eventMinute = match.currentMinute.coerceAtLeast(1)
                        selectedPlayerId = homePlayers.firstOrNull()?.playerId ?: ""
                        showAddEventDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1214)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟥", fontSize = 18.sp)
                        Text("طرد", fontSize = 11.sp, color = CardRed, fontWeight = FontWeight.Bold)
                    }
                }

                // Substitution
                Button(
                    onClick = {
                        selectedEventType = EventType.SUBSTITUTION
                        selectedTeamId = match.homeTeamId
                        eventMinute = match.currentMinute.coerceAtLeast(1)
                        selectedPlayerId = homePlayers.firstOrNull()?.playerId ?: ""
                        subPlayerId = homePlayers.getOrNull(1)?.playerId ?: ""
                        showAddEventDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 18.sp)
                        Text("تبديل", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Timeline of Recorded Events with Delete capability
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل أحداث المباراة المباشرة (${events.size}):",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "حذف أي هدف يعيد احتساب النتيجة تلقائياً",
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
        }

        if (events.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم تسجيل أي أحداث حتى الآن. استخدم الأزرار السريعة بالأعلى.", color = TextDim, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(events.sortedByDescending { it.minute }) { ev ->
                TimelineEventCard(
                    event = ev,
                    isAdmin = true,
                    onDelete = { viewModel.deleteEvent(match.matchId, ev.eventId) }
                )
            }
        }
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        val currentTeamPlayers = if (selectedTeamId == match.homeTeamId) homePlayers else awayPlayers

        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = {
                Text(
                    text = "تسجيل حدث: ${
                        when (selectedEventType) {
                            EventType.GOAL -> "⚽ هدف جديد"
                            EventType.ASSIST -> "🎯 صناعة هدف"
                            EventType.YELLOW_CARD -> "🟨 بطاقة صفراء"
                            EventType.RED_CARD -> "🟥 بطاقة حمراء"
                            EventType.SUBSTITUTION -> "🔄 تبديل لاعب"
                            else -> "حدث"
                        }
                    }",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Minute selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الدقيقة: $eventMinute'", color = GoldBright, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { if (eventMinute > 1) eventMinute-- }) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = TextWhite)
                            }
                            IconButton(onClick = { eventMinute++ }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = TextWhite)
                            }
                        }
                    }

                    // Team selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTeamId == match.homeTeamId,
                            onClick = {
                                selectedTeamId = match.homeTeamId
                                selectedPlayerId = homePlayers.firstOrNull()?.playerId ?: ""
                            },
                            label = { Text(match.homeTeamName) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedTeamId == match.awayTeamId,
                            onClick = {
                                selectedTeamId = match.awayTeamId
                                selectedPlayerId = awayPlayers.firstOrNull()?.playerId ?: ""
                            },
                            label = { Text(match.awayTeamName) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Player picker
                    Text("اللاعب:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(110.dp)) {
                        items(currentTeamPlayers) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerId = p.playerId }
                                    .background(
                                        if (selectedPlayerId == p.playerId) PitchGreenContainer else Color.Transparent
                                    )
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlayerId == p.playerId,
                                    onClick = { selectedPlayerId = p.playerId }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("#${p.shirtNumber} - ${p.name}", color = TextWhite, fontSize = 13.sp)
                            }
                        }
                    }

                    // Optional Description
                    OutlinedTextField(
                        value = eventDesc,
                        onValueChange = { eventDesc = it },
                        label = { Text("ملاحظة أو تفاصيل (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PitchGreenVibrant,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val teamName = if (selectedTeamId == match.homeTeamId) match.homeTeamName else match.awayTeamName
                        val player = (homePlayers + awayPlayers).find { it.playerId == selectedPlayerId }
                        val pName = player?.name ?: "لاعب"

                        viewModel.addEvent(
                            matchId = match.matchId,
                            minute = eventMinute,
                            teamId = selectedTeamId,
                            teamName = teamName,
                            playerId = selectedPlayerId,
                            playerName = pName,
                            type = selectedEventType,
                            description = eventDesc
                        )
                        showAddEventDialog = false
                        eventDesc = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreenVibrant)
                ) {
                    Text("حفظ الحدث فوراً")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            },
            containerColor = CharcoalSurface
        )
    }
}

@Composable
fun TimelineEventCard(
    event: MatchEvent,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
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
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الحدث", tint = CardRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
