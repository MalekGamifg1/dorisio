package com.example.ui.screens

import androidx.compose.animation.core.*
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
fun HomeScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val league by viewModel.currentLeague.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val standings by viewModel.standings.collectAsState()
    val players by viewModel.players.collectAsState()
    val weeklyAwards by viewModel.weeklyAwards.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val liveMatch = matches.find { it.status == MatchStatus.LIVE }
    val nextMatch = matches.find { it.status == MatchStatus.SCHEDULED }
    val latestFinished = matches.filter { it.status == MatchStatus.FINISHED }
    val topScorer = players.maxByOrNull { it.goals }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // League Header Card
        item {
            LeagueHeaderCard(league)
        }

        // Empty state for fresh setup
        if (matches.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ModernEmeraldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = ModernEmerald,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "الدوري بانتظار فرقك ومبارياتك!",
                            color = TextDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تم تجهيز التطبيق نظيفاً لتتمكن من إنشاء فرقك الخاصة، إضافة اللاعبين بصور وجوههم، وجدولة أولى المباريات.",
                            color = TextSecondarySlate,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) },
                            colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إدارة الدوري وإضافة الفرق والمباريات")
                        }
                    }
                }
            }
        }

        // Live Match Center Banner
        if (liveMatch != null) {
            item {
                LiveMatchBanner(
                    match = liveMatch,
                    isAdmin = currentUser?.role == UserRole.ADMIN,
                    onClick = { viewModel.selectMatch(liveMatch.matchId, openLiveAdmin = false) },
                    onAdminClick = { viewModel.selectMatch(liveMatch.matchId, openLiveAdmin = true) }
                )
            }
        }

        // Quick Standings Widget
        if (standings.isNotEmpty()) {
            item {
                StandingsPreviewCard(
                    standings = standings.take(3),
                    onViewAll = { viewModel.navigateTo(Screen.STANDINGS) }
                )
            }
        }

        // Player of the Week Feature
        weeklyAwards.firstOrNull()?.let { award ->
            item {
                PlayerOfTheWeekCard(award)
            }
        }

        // Top Scorer & Key Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Scorer
                topScorer?.let { player ->
                    StatHighlightCard(
                        title = "هداف الدوري",
                        subtitle = player.teamName,
                        name = player.name,
                        value = "${player.goals} أهداف",
                        icon = Icons.Default.EmojiEvents,
                        accent = GoldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(Screen.PLAYERS) }
                    )
                }

                // Next Match
                nextMatch?.let { match ->
                    StatHighlightCard(
                        title = "المباراة القادمة",
                        subtitle = match.startTime,
                        name = "${match.homeTeamName} ضد ${match.awayTeamName}",
                        value = match.date,
                        icon = Icons.Default.CalendarToday,
                        accent = PitchGreenVibrant,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectMatch(match.matchId) }
                    )
                }
            }
        }

        // Latest Announcements
        if (announcements.isNotEmpty()) {
            item {
                AnnouncementsSection(announcements)
            }
        }
    }
}

@Composable
fun LeagueHeaderCard(league: League) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PitchGreenDark)
                            .border(1.dp, PitchGreenVibrant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = league.name,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${league.season} • ${league.numberOfTeams} فرق",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PitchGreenContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "جاري الآن",
                        color = PitchGreenVibrant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = league.description,
                color = TextDim,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun LiveMatchBanner(
    match: FootballMatch,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    // Pulsing live badge animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PitchGreenVibrant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_live_match")
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live indicator & Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(LiveRed.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مباشر الآن",
                        color = LiveRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F3A22))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "الدقيقة ${match.currentMinute}'",
                        color = GoldBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scoreboard Row
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
                    TeamLogoView(
                        logo = match.homeTeamLogo,
                        teamName = match.homeTeamName,
                        colorHex = match.homeTeamColor,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = match.homeTeamName,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Scores
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "${match.homeScore}",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = " : ",
                        color = TextDim,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Text(
                        text = "${match.awayScore}",
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Away Team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoView(
                        logo = match.awayTeamLogo,
                        teamName = match.awayTeamName,
                        colorHex = match.awayTeamColor,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = match.awayTeamName,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اضغط لمشاهدة مجريات المباراة والتشكيلة ⬅",
                    color = PitchGreenVibrant,
                    fontSize = 11.sp
                )

                if (isAdmin) {
                    Button(
                        onClick = onAdminClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_admin_live_center")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تسجيل الأحداث",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StandingsPreviewCard(
    standings: List<LeagueStanding>,
    onViewAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ترتيب الدوري (المراكز الأولى)",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "عرض الكل",
                    color = PitchGreenVibrant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("الفريق", color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("لعب", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("+/-", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("نقاط", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)

            standings.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${row.position}",
                        color = if (row.position == 1) GoldBright else TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamLogoView(
                            logo = row.teamLogo,
                            teamName = row.teamName,
                            colorHex = row.teamColor,
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = row.teamName,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${row.played}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (row.goalDifference > 0) "+${row.goalDifference}" else "${row.goalDifference}",
                        color = if (row.goalDifference > 0) PitchGreenVibrant else if (row.goalDifference < 0) CardRed else TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${row.points}",
                        color = GoldBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerOfTheWeekCard(award: WeeklyAward) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF241E10)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⭐ نجم الأسبوع (الجولة ${award.weekNumber})",
                        color = GoldBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldAccent)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "تقييم ${award.rating}",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(GoldDark)
                        .border(2.dp, GoldBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = award.playerName,
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${award.teamName} • ${award.goals} أهداف • ${award.assists} صناعة",
                        color = GoldBright,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = award.reason,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun StatHighlightCard(
    title: String,
    subtitle: String,
    name: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, color = TextDim, fontSize = 11.sp)
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                Text(text = value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnnouncementsSection(announcements: List<Announcement>) {
    Column {
        Text(
            text = "📢 إعلانات وتنبيهات الدوري",
            color = TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        announcements.forEach { ann ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (ann.isPinned) GoldAccent.copy(alpha = 0.5f) else CharcoalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ann.title,
                            color = if (ann.isPinned) GoldBright else TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (ann.isPinned) {
                            Text(
                                text = "مثبت",
                                color = GoldBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ann.content,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
