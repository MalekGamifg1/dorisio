package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DorisioRepository
import com.example.model.FootballMatch
import com.example.model.Player
import com.example.model.Team
import com.example.ui.components.TeamLogoView
import com.example.ui.theme.*
import com.example.viewmodel.DorisioViewModel

@Composable
fun AdminPanelScreen(
    viewModel: DorisioViewModel,
    modifier: Modifier = Modifier
) {
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val league by viewModel.currentLeague.collectAsState()

    var selectedAdminTab by remember { mutableStateOf(0) } // 0: Overview/Schedule, 1: Teams, 2: Players, 3: Matches
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Dialogs
    var showAddTeamDialog by remember { mutableStateOf(false) }
    var showEditTeamDialog by remember { mutableStateOf<Team?>(null) }
    var showDeleteTeamDialog by remember { mutableStateOf<Team?>(null) }

    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showTransferPlayerDialog by remember { mutableStateOf<Player?>(null) }
    var showDeletePlayerDialog by remember { mutableStateOf<Player?>(null) }

    var showCreateMatchDialog by remember { mutableStateOf(false) }
    var showDeleteMatchDialog by remember { mutableStateOf<FootballMatch?>(null) }
    var showAnnounceMotwDialog by remember { mutableStateOf(false) }
    var showAdminGuideDialog by remember { mutableStateOf(false) }

    // Inputs
    var teamName by remember { mutableStateOf("") }
    var teamColor by remember { mutableStateOf("#059669") }
    var teamLogo by remember { mutableStateOf("🦁") }

    val teamGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                teamLogo = DorisioRepository.bitmapToBase64(scaled)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val teamCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            val scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true)
            teamLogo = DorisioRepository.bitmapToBase64(scaled)
        }
    }

    val presetBadges = listOf("🦁", "🦅", "👑", "⚡", "🐺", "🛡️", "🔥", "🏆", "🌟", "🐆", "🚀", "⚽", "🔴", "🔵", "🟡", "⚪", "🟢", "⚫")

    var playerName by remember { mutableStateOf("") }
    var playerShirt by remember { mutableStateOf(10) }
    var playerPos by remember { mutableStateOf("مهاجم") }
    var playerTeamId by remember { mutableStateOf(teams.firstOrNull()?.teamId ?: "") }
    var playerFaceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val playerCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) playerFaceBitmap = bmp
    }

    var matchHomeId by remember { mutableStateOf(teams.firstOrNull()?.teamId ?: "") }
    var matchAwayId by remember { mutableStateOf(teams.getOrNull(1)?.teamId ?: "") }
    var matchDate by remember { mutableStateOf("غداً") }
    var matchTime by remember { mutableStateOf("17:00") }
    var matchVenue by remember { mutableStateOf("ملعب المدرسة الرئيسي") }

    val colorOptions = listOf("#059669", "#2563EB", "#DC2626", "#D97706", "#7C3AED", "#0891B2", "#E11D48", "#1E293B")
    val positions = listOf("حارس مرمى", "مدافع", "وسط", "مهاجم")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CleanWhiteBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LuxuryGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("لوحة إدارة دوريسيو الكاملة", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { showAdminGuideDialog = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "دليل الإدارة", tint = ModernEmerald)
                        }
                    }

                    Text(
                        "تحكم كامل في كافة مفاصل البطولة: الفرق، اللاعبين، جدول المباريات التلقائي، وحذف أو تعديل أي عنصر.",
                        color = TextSecondarySlate,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAdminGuideDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ModernEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = ModernEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("شرح الإدارة بالتفصيل", color = ModernEmerald, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.repository.clearAllData()
                                snackbarMessage = "تم تصفير الدوري بالكامل والعودة لنقطة البداية"
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardRed.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CardRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصفير الدوري (فارغ)", color = CardRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Notification Banner
        if (snackbarMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ModernEmeraldLight),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ModernEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(snackbarMessage!!, color = ModernEmeraldDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = ModernEmeraldDark)
                        }
                    }
                }
            }
        }

        // Navigation Tabs (Sections)
        item {
            TabRow(
                selectedTabIndex = selectedAdminTab,
                containerColor = CleanWhiteCardElevated,
                contentColor = ModernEmerald,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(selected = selectedAdminTab == 0, onClick = { selectedAdminTab = 0 }, text = { Text("⚡ الجدولة", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedAdminTab == 1, onClick = { selectedAdminTab = 1 }, text = { Text("🛡️ الفرق (${teams.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedAdminTab == 2, onClick = { selectedAdminTab = 2 }, text = { Text("🏃 اللاعبين (${players.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedAdminTab == 3, onClick = { selectedAdminTab = 3 }, text = { Text("⚽ المباريات (${matches.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            }
        }

        // TAB 0: Automatic Scheduling & Overview
        if (selectedAdminTab == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ModernEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ModernEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد جدول الدوري التلقائي (Round-Robin)", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "يقوم النظام تلقائياً بإنشاء مواجهات متوازنة لكل جولة بحيث يلاقي كل فريق جميع الفرق الأخرى بدون تكرار، وتقسيمها لأسابيع وجولات تلقائية.",
                            color = TextSecondarySlate,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val result = viewModel.generateAutomaticSchedule(doubleRound = false)
                                    snackbarMessage = result.second
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("توليد مباريات (ذهاب)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val result = viewModel.generateAutomaticSchedule(doubleRound = true)
                                    snackbarMessage = result.second
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("توليد (ذهاب وإياب)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (matches.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearAllMatches()
                                    snackbarMessage = "تم مسح جدول المباريات بالكامل لتتمكن من إعادة توليده"
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CardRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسح المباريات المجدولة لإعادة التوليد", color = CardRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Quick add buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddTeamDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة فريق جديد", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            playerTeamId = teams.firstOrNull()?.teamId ?: ""
                            showAddPlayerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmeraldDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة لاعب وتصويره", fontSize = 12.sp)
                    }
                }
            }
        }

        // TAB 1: Teams Management (Full CRUD)
        if (selectedAdminTab == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قائمة فرق الدوري (${teams.size}):", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showAddTeamDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فريق جديد", fontSize = 12.sp)
                    }
                }
            }

            if (teams.isEmpty()) {
                item {
                    Text("لا توجد فرق بعد. اضغط 'فريق جديد' لإضافة أول فريق في الدوري.", color = TextMutedSlate, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
                }
            }

            items(teams) { team ->
                val teamPlayersCount = players.count { it.teamId == team.teamId }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            TeamLogoView(
                                logo = team.logo,
                                teamName = team.name,
                                colorHex = team.colorHex,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(team.name, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("$teamPlayersCount لاعبين", color = TextSecondarySlate, fontSize = 11.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showEditTeamDialog = team }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = ModernEmerald)
                            }
                            IconButton(onClick = { showDeleteTeamDialog = team }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CardRed)
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: Players Management (Full CRUD + Transfer)
        if (selectedAdminTab == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قائمة اللاعبين (${players.size}):", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            playerTeamId = teams.firstOrNull()?.teamId ?: ""
                            showAddPlayerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("لاعب جديد", fontSize = 12.sp)
                    }
                }
            }

            if (players.isEmpty()) {
                item {
                    Text("لا يوجد لاعبين بعد. اضغط 'لاعب جديد' لإضافة لاعب والتقاط صورة وجهه.", color = TextMutedSlate, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
                }
            }

            items(players) { p ->
                val faceBitmap = remember(p.profileImage) {
                    if (p.profileImage.isNotBlank()) DorisioRepository.base64ToBitmap(p.profileImage) else null
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CleanWhiteCardElevated)
                                    .border(1.5.dp, ModernEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (faceBitmap != null) {
                                    Image(bitmap = faceBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Text("#${p.shirtNumber}", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(p.name, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${p.teamName} • ${p.position} (#${p.shirtNumber})", color = TextSecondarySlate, fontSize = 11.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Transfer button
                            IconButton(onClick = { showTransferPlayerDialog = p }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "نقل لفريق آخر", tint = LuxuryGold)
                            }
                            // Delete button
                            IconButton(onClick = { showDeletePlayerDialog = p }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف اللاعب", tint = CardRed)
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: Matches Management (Schedule, Delete, Details)
        if (selectedAdminTab == 3) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جدول المباريات (${matches.size}):", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showCreateMatchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مباراة يدوية", fontSize = 12.sp)
                    }
                }
            }

            if (matches.isEmpty()) {
                item {
                    Text("لا توجد مباريات بعد. انتقل لتبويب 'الجدولة' لتوليد جميع الجولات بضغطة واحدة.", color = TextMutedSlate, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
                }
            }

            items(matches) { m ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CleanWhiteSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ModernEmeraldLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(m.roundName, color = ModernEmeraldDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${m.date} - ${m.startTime}", color = TextSecondarySlate, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${m.homeTeamName} vs ${m.awayTeamName}", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { showDeleteMatchDialog = m }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف المباراة", tint = CardRed)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // ALL DIALOGS (CREATE, EDIT, DELETE, TRANSFER)
    // ==========================================

    // Add Team Dialog
    if (showAddTeamDialog) {
        AlertDialog(
            onDismissRequest = { showAddTeamDialog = false },
            title = { Text("إضافة فريق جديد", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Live Logo Preview & Upload Options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CleanWhiteCardElevated, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamLogoView(
                                logo = teamLogo,
                                teamName = teamName.ifBlank { "فريق" },
                                colorHex = teamColor,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("شعار الفريق (لوقو)", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("اختر صورة أو أيقونة للفريق", color = TextSecondarySlate, fontSize = 10.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { teamCameraLauncher.launch() },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(ModernEmeraldLight)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "كاميرا", tint = ModernEmeraldDark, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    teamGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(ModernEmeraldLight)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "معرض الصور", tint = ModernEmeraldDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Preset Badges Row
                    Text("أو اختر شعاراً جاهزاً:", color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetBadges) { badge ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (teamLogo == badge) ModernEmeraldLight else CleanWhiteSurface)
                                    .border(if (teamLogo == badge) 2.dp else 1.dp, if (teamLogo == badge) ModernEmerald else CleanWhiteBorder, RoundedCornerShape(8.dp))
                                    .clickable { teamLogo = badge },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(badge, fontSize = 18.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("اسم الفريق") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("اختر لون قميص الفريق الأساسي:", color = TextDark, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        colorOptions.take(4).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(if (teamColor == hex) 3.dp else 1.dp, if (teamColor == hex) TextDark else CleanWhiteBorder, CircleShape)
                                    .clickable { teamColor = hex }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        colorOptions.drop(4).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(if (teamColor == hex) 3.dp else 1.dp, if (teamColor == hex) TextDark else CleanWhiteBorder, CircleShape)
                                    .clickable { teamColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (teamName.isNotBlank()) {
                            viewModel.createTeam(teamName, teamColor, teamLogo)
                            showAddTeamDialog = false
                            teamName = ""
                            teamLogo = "🦁"
                            snackbarMessage = "تم إنشاء الفريق وإضافة شعاره بنجاح ⚽"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("إنشاء الفريق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTeamDialog = false }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Edit Team Dialog
    if (showEditTeamDialog != null) {
        var editName by remember { mutableStateOf(showEditTeamDialog!!.name) }
        var editColor by remember { mutableStateOf(showEditTeamDialog!!.colorHex) }
        var editLogo by remember { mutableStateOf(showEditTeamDialog!!.logo.ifBlank { "🦁" }) }

        val editGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                    editLogo = DorisioRepository.bitmapToBase64(scaled)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        val editCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bmp ->
            if (bmp != null) {
                val scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true)
                editLogo = DorisioRepository.bitmapToBase64(scaled)
            }
        }

        AlertDialog(
            onDismissRequest = { showEditTeamDialog = null },
            title = { Text("تعديل الفريق وشعاره", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Live Logo Preview & Upload Options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CleanWhiteCardElevated, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamLogoView(
                                logo = editLogo,
                                teamName = editName.ifBlank { "فريق" },
                                colorHex = editColor,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("شعار الفريق (لوقو)", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("تغيير شعار الفريق", color = TextSecondarySlate, fontSize = 10.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { editCameraLauncher.launch() },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(ModernEmeraldLight)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "كاميرا", tint = ModernEmeraldDark, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    editGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(ModernEmeraldLight)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "معرض الصور", tint = ModernEmeraldDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Preset Badges Row
                    Text("أو اختر شعاراً جاهزاً:", color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetBadges) { badge ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (editLogo == badge) ModernEmeraldLight else CleanWhiteSurface)
                                    .border(if (editLogo == badge) 2.dp else 1.dp, if (editLogo == badge) ModernEmerald else CleanWhiteBorder, RoundedCornerShape(8.dp))
                                    .clickable { editLogo = badge },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(badge, fontSize = 18.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم الفريق") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("لون القميص:", color = TextDark, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.take(4).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(if (editColor == hex) 3.dp else 1.dp, if (editColor == hex) TextDark else CleanWhiteBorder, CircleShape)
                                    .clickable { editColor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateTeam(showEditTeamDialog!!.teamId, editName, editColor, editLogo)
                            showEditTeamDialog = null
                            snackbarMessage = "تم تحديث بيانات الفريق وشعاره بنجاح"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTeamDialog = null }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Delete Team Dialog
    if (showDeleteTeamDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTeamDialog = null },
            title = { Text("حذف الفريق نهائياً", color = CardRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من حذف فريق '${showDeleteTeamDialog!!.name}'؟ سيتم إلغاء المباريات المجدولة الخاصة به.",
                    color = TextDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTeam(showDeleteTeamDialog!!.teamId)
                        showDeleteTeamDialog = null
                        snackbarMessage = "تم حذف الفريق بنجاح"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardRed)
                ) {
                    Text("نعم، احذف الفريق", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTeamDialog = null }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Add Player Dialog (with Face Photo)
    if (showAddPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlayerDialog = false },
            title = { Text("إضافة لاعب وتصوير وجهه", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Face capture for lineup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("صورة سيلفي لوجه اللاعب:", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("ستظهر في التشكيلة على الملعب", color = TextSecondarySlate, fontSize = 10.sp)
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CleanWhiteCardElevated)
                                .border(1.5.dp, ModernEmerald, CircleShape)
                                .clickable { playerCameraLauncher.launch() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (playerFaceBitmap != null) {
                                Image(
                                    bitmap = playerFaceBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ModernEmerald)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("اسم اللاعب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = playerShirt.toString(),
                        onValueChange = { playerShirt = it.toIntOrNull() ?: 1 },
                        label = { Text("رقم القميص") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("اختر الفريق:", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(90.dp)) {
                        items(teams) { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playerTeamId = t.teamId }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = playerTeamId == t.teamId, onClick = { playerTeamId = t.teamId })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.name, color = TextDark, fontSize = 13.sp)
                            }
                        }
                    }

                    Text("المركز:", color = TextDark, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        positions.forEach { pos ->
                            FilterChip(
                                selected = playerPos == pos,
                                onClick = { playerPos = pos },
                                label = { Text(pos, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playerName.isNotBlank() && playerTeamId.isNotBlank()) {
                            val photoBase64 = if (playerFaceBitmap != null) {
                                DorisioRepository.bitmapToBase64(playerFaceBitmap!!)
                            } else ""
                            viewModel.repository.addPlayerToTeam(playerName, playerTeamId, playerShirt, playerPos, photoBase64)
                            showAddPlayerDialog = false
                            playerName = ""
                            playerFaceBitmap = null
                            snackbarMessage = "تمت إضافة اللاعب بنجاح"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("حفظ اللاعب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerDialog = false }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Transfer Player Dialog
    if (showTransferPlayerDialog != null) {
        var selectedNewTeamId by remember { mutableStateOf(teams.firstOrNull { it.teamId != showTransferPlayerDialog!!.teamId }?.teamId ?: "") }

        AlertDialog(
            onDismissRequest = { showTransferPlayerDialog = null },
            title = { Text("نقل لاعب إلى فريق آخر", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نقل اللاعب: ${showTransferPlayerDialog!!.name}", color = ModernEmeraldDark, fontWeight = FontWeight.Bold)
                    Text("الفريق الحالي: ${showTransferPlayerDialog!!.teamName}", color = TextSecondarySlate, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("اختر الفريق الجديد:", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    LazyColumn(modifier = Modifier.height(120.dp)) {
                        items(teams) { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNewTeamId = t.teamId }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedNewTeamId == t.teamId, onClick = { selectedNewTeamId = t.teamId })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.name, color = TextDark, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedNewTeamId.isNotBlank()) {
                            viewModel.movePlayer(showTransferPlayerDialog!!.playerId, selectedNewTeamId)
                            showTransferPlayerDialog = null
                            snackbarMessage = "تم نقل اللاعب إلى الفريق الجديد بنجاح"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("تأكيد النقل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferPlayerDialog = null }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Delete Player Dialog
    if (showDeletePlayerDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeletePlayerDialog = null },
            title = { Text("حذف اللاعب نهائياً", color = CardRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف اللاعب '${showDeletePlayerDialog!!.name}' من صفوف ${showDeletePlayerDialog!!.teamName}؟", color = TextDark, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(showDeletePlayerDialog!!.playerId)
                        showDeletePlayerDialog = null
                        snackbarMessage = "تم حذف اللاعب"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardRed)
                ) {
                    Text("نعم، احذف اللاعب", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlayerDialog = null }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Delete Match Dialog
    if (showDeleteMatchDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteMatchDialog = null },
            title = { Text("إلغاء أو حذف المباراة", color = CardRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد حذف مباراة ${showDeleteMatchDialog!!.homeTeamName} ضد ${showDeleteMatchDialog!!.awayTeamName}؟", color = TextDark, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMatch(showDeleteMatchDialog!!.matchId)
                        showDeleteMatchDialog = null
                        snackbarMessage = "تم حذف المباراة من الجدول"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardRed)
                ) {
                    Text("حذف المباراة", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMatchDialog = null }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Manual Create Match Dialog
    if (showCreateMatchDialog) {
        AlertDialog(
            onDismissRequest = { showCreateMatchDialog = false },
            title = { Text("جدولة مباراة يدوية", color = TextDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الفريق المستضيف:", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(70.dp)) {
                        items(teams) { t ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { matchHomeId = t.teamId }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = matchHomeId == t.teamId, onClick = { matchHomeId = t.teamId })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.name, color = TextDark, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("الفريق الضيف:", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(70.dp)) {
                        items(teams) { t ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { matchAwayId = t.teamId }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = matchAwayId == t.teamId, onClick = { matchAwayId = t.teamId })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.name, color = TextDark, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(value = matchDate, onValueChange = { matchDate = it }, label = { Text("الموعد / اليوم") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = matchTime, onValueChange = { matchTime = it }, label = { Text("التوقيت") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (matchHomeId.isNotBlank() && matchAwayId.isNotBlank() && matchHomeId != matchAwayId) {
                            viewModel.createMatch(matchHomeId, matchAwayId, matchDate, matchTime, matchVenue)
                            showCreateMatchDialog = false
                            snackbarMessage = "تمت جدولة المباراة بنجاح"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("جدولة اللقاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMatchDialog = false }) { Text("إلغاء", color = TextSecondarySlate) }
            },
            containerColor = CleanWhiteSurface
        )
    }

    // Admin How-to Guide Dialog
    if (showAdminGuideDialog) {
        AlertDialog(
            onDismissRequest = { showAdminGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = ModernEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("دليل المشرف: كيف تدير دوريسيو؟", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        GuideStepCard(
                            step = "1",
                            title = "إنشاء الفرق",
                            desc = "من تبويب 'الفرق'، اضغط على 'فريق جديد' وحدد اسم الفريق ولون قميصه."
                        )
                    }
                    item {
                        GuideStepCard(
                            step = "2",
                            title = "إضافة اللاعبين وتصوير الوجه 📸",
                            desc = "من تبويب 'اللاعبين'، اضغط 'لاعب جديد' وحدد فريقه ورقمه والتقط صورة سيلفي لوجهه."
                        )
                    }
                    item {
                        GuideStepCard(
                            step = "3",
                            title = "توليد جدول الدوري تلقائياً ⚡",
                            desc = "بعد إضافة الفرق، ادخل على تبويب 'الجدولة' واضغط 'توليد مباريات (ذهاب)' أو 'ذهاب وإياب'. سيقوم النظام بحساب كل جولة ومواجهات جميع الفرق تلقائياً!"
                        )
                    }
                    item {
                        GuideStepCard(
                            step = "4",
                            title = "التحكم الكامل (نقل وحذف)",
                            desc = "يمكنك في أي وقت حذف فريق، تعديل اسمه، حذف أي لاعب، أو نقله إلى فريق آخر بضغطة زر."
                        )
                    }
                    item {
                        GuideStepCard(
                            step = "5",
                            title = "تسجيل أحداث المباراة المباشرة ⚡",
                            desc = "من تبويب 'المباريات'، اضغط على 'التحكم بالمباراة' للدخول لمركز البث المباشر وتسجيل الأهداف والكروت والتبديلات."
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAdminGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernEmerald)
                ) {
                    Text("فهمت، شكراً!")
                }
            },
            containerColor = CleanWhiteSurface
        )
    }
}

@Composable
fun GuideStepCard(step: String, title: String, desc: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CleanWhiteCardElevated),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ModernEmerald),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = TextSecondarySlate, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
