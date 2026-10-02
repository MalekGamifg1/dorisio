package com.example.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.snapshotFlow
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.UUID

class DorisioRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dorisio_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val firebaseBridge = FirebaseBridge(context)
    val cloudSyncBridge = CloudSyncBridge(context)
    private val _allUsers = MutableStateFlow<List<UserAccount>>(emptyList())

    // State flows
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _currentLeague = MutableStateFlow<League>(createDefaultLeague())
    val currentLeague: StateFlow<League> = _currentLeague.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _matches = MutableStateFlow<List<FootballMatch>>(emptyList())
    val matches: StateFlow<List<FootballMatch>> = _matches.asStateFlow()

    private val _matchEvents = MutableStateFlow<Map<String, List<MatchEvent>>>(emptyMap())
    val matchEvents: StateFlow<Map<String, List<MatchEvent>>> = _matchEvents.asStateFlow()

    private val _standings = MutableStateFlow<List<LeagueStanding>>(emptyList())
    val standings: StateFlow<List<LeagueStanding>> = _standings.asStateFlow()

    private val _playerRatings = MutableStateFlow<List<PlayerRating>>(emptyList())
    val playerRatings: StateFlow<List<PlayerRating>> = _playerRatings.asStateFlow()

    private val _weeklyAwards = MutableStateFlow<List<WeeklyAward>>(emptyList())
    val weeklyAwards: StateFlow<List<WeeklyAward>> = _weeklyAwards.asStateFlow()

    private val _seasonAwards = MutableStateFlow<List<SeasonAward>>(emptyList())
    val seasonAwards: StateFlow<List<SeasonAward>> = _seasonAwards.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _isFirebaseConfigured = MutableStateFlow(true)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    private val _connectionStatus = MutableStateFlow("متصل بالسحابة (مزامنة فورية ⚡)")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    init {
        loadSavedUser()
        loadSavedLeagueData()
        startLiveMatchTicker()
        initRealtimeCloudListeners()
    }

    private val deletedTeamIds = mutableSetOf<String>()
    private val deletedPlayerIds = mutableSetOf<String>()
    private val deletedMatchIds = mutableSetOf<String>()

    fun triggerGlobalCloudPush() {
        val currUser = _currentUser.value
        val usersList = if (currUser != null) {
            val existing = _allUsers.value.filterNot { it.email.equals(currUser.email, ignoreCase = true) || it.userId == currUser.userId }
            listOf(currUser) + existing
        } else {
            _allUsers.value
        }
        _allUsers.value = usersList

        scope.launch {
            val mergedPayload = cloudSyncBridge.pushPayloadToCloud(
                users = usersList,
                teams = _teams.value,
                players = _players.value,
                matches = _matches.value,
                matchEvents = _matchEvents.value,
                announcements = _announcements.value,
                deletedTeamIds = deletedTeamIds,
                deletedPlayerIds = deletedPlayerIds,
                deletedMatchIds = deletedMatchIds
            )
            if (mergedPayload != null) {
                withContext(Dispatchers.Main) {
                    if (mergedPayload.teams.isNotEmpty()) _teams.value = mergedPayload.teams
                    if (mergedPayload.players.isNotEmpty()) _players.value = mergedPayload.players
                    if (mergedPayload.matches.isNotEmpty()) _matches.value = mergedPayload.matches
                    if (mergedPayload.matchEvents.isNotEmpty()) _matchEvents.value = mergedPayload.matchEvents
                    if (mergedPayload.announcements.isNotEmpty()) _announcements.value = mergedPayload.announcements
                    recalculateStandings()
                    saveLocalCache()
                }
            }
        }
    }

    private fun initRealtimeCloudListeners() {
        // Universal High-Speed Cloud Sync Loop
        cloudSyncBridge.startSyncLoop { payload ->
            if (payload.users.isNotEmpty()) {
                _allUsers.value = payload.users
                val current = _currentUser.value
                if (current != null) {
                    val updated = payload.users.find { it.email.equals(current.email, ignoreCase = true) || it.userId == current.userId }
                    if (updated != null && (updated.photoUrl.isNotBlank() || updated.displayName != current.displayName)) {
                        _currentUser.value = updated
                    }
                }
            }
            if (payload.teams.isNotEmpty()) {
                _teams.value = payload.teams
                recalculateStandings()
            }
            if (payload.players.isNotEmpty()) {
                _players.value = payload.players
            }
            if (payload.matches.isNotEmpty()) {
                _matches.value = payload.matches
                recalculateStandings()
            }
            if (payload.matchEvents.isNotEmpty()) {
                _matchEvents.value = payload.matchEvents
                recalculatePlayerStats()
            }
            if (payload.announcements.isNotEmpty()) {
                _announcements.value = payload.announcements
            }
            saveLocalCache()
        }

        // Firestore Reactive Flow Collection Streams
        if (firebaseBridge.isFirebaseInitialized) {
            scope.launch {
                firebaseBridge.getTeamsFlow().collect { cloudTeams ->
                    if (cloudTeams.isNotEmpty()) {
                        _teams.value = cloudTeams
                        recalculateStandings()
                    }
                }
            }

            scope.launch {
                firebaseBridge.getPlayersFlow().collect { cloudPlayers ->
                    if (cloudPlayers.isNotEmpty()) {
                        _players.value = cloudPlayers
                    }
                }
            }

            scope.launch {
                firebaseBridge.getMatchesFlow().collect { cloudMatches ->
                    if (cloudMatches.isNotEmpty()) {
                        _matches.value = cloudMatches
                        recalculateStandings()
                    }
                }
            }

            scope.launch {
                firebaseBridge.getMatchEventsFlow().collect { cloudEventsMap ->
                    if (cloudEventsMap.isNotEmpty()) {
                        _matchEvents.value = cloudEventsMap
                        recalculatePlayerStats()
                    }
                }
            }

            scope.launch {
                firebaseBridge.getAnnouncementsFlow().collect { cloudAnnouncements ->
                    if (cloudAnnouncements.isNotEmpty()) {
                        _announcements.value = cloudAnnouncements
                    }
                }
            }
        }

        // Use snapshotFlow to reactively trigger real-time standings & stats calculation on any state change
        scope.launch {
            snapshotFlow { Pair(_teams.value, _matches.value) }.collect { (currTeams, currMatches) ->
                if (currTeams.isNotEmpty()) {
                    recalculateStandings()
                }
            }
        }

        scope.launch {
            snapshotFlow { Pair(_players.value, _matchEvents.value) }.collect { (currPlayers, currEvents) ->
                if (currPlayers.isNotEmpty()) {
                    recalculatePlayerStats()
                }
            }
        }
    }

    private fun createDefaultLeague(): League {
        return League(
            leagueId = "league_dorisio_1",
            name = "دوري دوريسيو المدرسي",
            logo = "",
            description = "دوري كرة القدم الحقيقي للمدارس والأصدقاء — نظم فريقك وسجل المباريات مباشرة.",
            season = "موسم 2026",
            numberOfTeams = 0,
            matchDurationMinutes = 30,
            startDate = "2026-10-01",
            endDate = "2026-11-30",
            status = LeagueStatus.ACTIVE,
            createdBy = "admin"
        )
    }

    // ==========================================
    // PERSISTENCE & CLEAN STATE
    // ==========================================

    fun saveLocalCache() {
        try {
            val teamsArr = org.json.JSONArray()
            _teams.value.forEach { t ->
                val tj = org.json.JSONObject()
                tj.put("teamId", t.teamId)
                tj.put("leagueId", t.leagueId)
                tj.put("name", t.name)
                tj.put("logo", t.logo)
                tj.put("colorHex", t.colorHex)
                tj.put("captainId", t.captainId)
                tj.put("captainName", t.captainName)
                tj.put("createdAt", t.createdAt)
                teamsArr.put(tj)
            }

            val playersArr = org.json.JSONArray()
            _players.value.forEach { p ->
                val pj = org.json.JSONObject()
                pj.put("playerId", p.playerId)
                pj.put("userId", p.userId)
                pj.put("name", p.name)
                pj.put("profileImage", p.profileImage)
                pj.put("teamId", p.teamId)
                pj.put("teamName", p.teamName)
                pj.put("leagueId", p.leagueId)
                pj.put("shirtNumber", p.shirtNumber)
                pj.put("position", p.position)
                pj.put("appearances", p.appearances)
                pj.put("goals", p.goals)
                pj.put("assists", p.assists)
                pj.put("yellowCards", p.yellowCards)
                pj.put("redCards", p.redCards)
                pj.put("averageRating", p.averageRating)
                pj.put("manOfTheMatchCount", p.manOfTheMatchCount)
                pj.put("weeklyAwardsCount", p.weeklyAwardsCount)
                playersArr.put(pj)
            }

            val matchesArr = org.json.JSONArray()
            _matches.value.forEach { m ->
                val mj = org.json.JSONObject()
                mj.put("matchId", m.matchId)
                mj.put("leagueId", m.leagueId)
                mj.put("homeTeamId", m.homeTeamId)
                mj.put("homeTeamName", m.homeTeamName)
                mj.put("homeTeamLogo", m.homeTeamLogo)
                mj.put("homeTeamColor", m.homeTeamColor)
                mj.put("awayTeamId", m.awayTeamId)
                mj.put("awayTeamName", m.awayTeamName)
                mj.put("awayTeamLogo", m.awayTeamLogo)
                mj.put("awayTeamColor", m.awayTeamColor)
                mj.put("date", m.date)
                mj.put("startTime", m.startTime)
                mj.put("venue", m.venue)
                mj.put("referee", m.referee)
                mj.put("status", m.status.name)
                mj.put("homeScore", m.homeScore)
                mj.put("awayScore", m.awayScore)
                mj.put("currentMinute", m.currentMinute)
                mj.put("round", m.round)
                mj.put("roundName", m.roundName)
                mj.put("manOfTheMatchPlayerId", m.manOfTheMatchPlayerId)
                mj.put("manOfTheMatchPlayerName", m.manOfTheMatchPlayerName)
                mj.put("createdAt", m.createdAt)
                matchesArr.put(mj)
            }

            prefs.edit()
                .putString("cache_teams", teamsArr.toString())
                .putString("cache_players", playersArr.toString())
                .putString("cache_matches", matchesArr.toString())
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("DorisioRepository", "Error saving local cache: ${e.message}")
        }
    }

    private fun loadSavedLeagueData() {
        try {
            val teamsStr = prefs.getString("cache_teams", null)
            if (!teamsStr.isNullOrBlank()) {
                val teamsArr = org.json.JSONArray(teamsStr)
                val teamsList = mutableListOf<Team>()
                for (i in 0 until teamsArr.length()) {
                    val tj = teamsArr.getJSONObject(i)
                    teamsList.add(
                        Team(
                            teamId = tj.optString("teamId", ""),
                            leagueId = tj.optString("leagueId", "league_dorisio_1"),
                            name = tj.optString("name", "فريق"),
                            logo = tj.optString("logo", ""),
                            colorHex = tj.optString("colorHex", "#059669"),
                            captainId = tj.optString("captainId", ""),
                            captainName = tj.optString("captainName", ""),
                            createdAt = tj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _teams.value = teamsList
            }

            val playersStr = prefs.getString("cache_players", null)
            if (!playersStr.isNullOrBlank()) {
                val playersArr = org.json.JSONArray(playersStr)
                val playersList = mutableListOf<Player>()
                for (i in 0 until playersArr.length()) {
                    val pj = playersArr.getJSONObject(i)
                    playersList.add(
                        Player(
                            playerId = pj.optString("playerId", ""),
                            userId = pj.optString("userId", ""),
                            name = pj.optString("name", "لاعب"),
                            profileImage = pj.optString("profileImage", ""),
                            teamId = pj.optString("teamId", ""),
                            teamName = pj.optString("teamName", ""),
                            leagueId = pj.optString("leagueId", "league_dorisio_1"),
                            shirtNumber = pj.optInt("shirtNumber", 10),
                            position = pj.optString("position", "وسط"),
                            appearances = pj.optInt("appearances", 0),
                            goals = pj.optInt("goals", 0),
                            assists = pj.optInt("assists", 0),
                            yellowCards = pj.optInt("yellowCards", 0),
                            redCards = pj.optInt("redCards", 0),
                            averageRating = pj.optDouble("averageRating", 7.0),
                            manOfTheMatchCount = pj.optInt("manOfTheMatchCount", 0),
                            weeklyAwardsCount = pj.optInt("weeklyAwardsCount", 0)
                        )
                    )
                }
                _players.value = playersList
            }

            val matchesStr = prefs.getString("cache_matches", null)
            if (!matchesStr.isNullOrBlank()) {
                val matchesArr = org.json.JSONArray(matchesStr)
                val matchesList = mutableListOf<FootballMatch>()
                for (i in 0 until matchesArr.length()) {
                    val mj = matchesArr.getJSONObject(i)
                    matchesList.add(
                        FootballMatch(
                            matchId = mj.optString("matchId", ""),
                            leagueId = mj.optString("leagueId", "league_dorisio_1"),
                            homeTeamId = mj.optString("homeTeamId", ""),
                            homeTeamName = mj.optString("homeTeamName", ""),
                            homeTeamLogo = mj.optString("homeTeamLogo", ""),
                            homeTeamColor = mj.optString("homeTeamColor", "#059669"),
                            awayTeamId = mj.optString("awayTeamId", ""),
                            awayTeamName = mj.optString("awayTeamName", ""),
                            awayTeamLogo = mj.optString("awayTeamLogo", ""),
                            awayTeamColor = mj.optString("awayTeamColor", "#2563EB"),
                            date = mj.optString("date", ""),
                            startTime = mj.optString("startTime", ""),
                            venue = mj.optString("venue", ""),
                            referee = mj.optString("referee", ""),
                            status = try { MatchStatus.valueOf(mj.optString("status", MatchStatus.SCHEDULED.name)) } catch (e: Exception) { MatchStatus.SCHEDULED },
                            homeScore = mj.optInt("homeScore", 0),
                            awayScore = mj.optInt("awayScore", 0),
                            currentMinute = mj.optInt("currentMinute", 0),
                            round = mj.optInt("round", 1),
                            roundName = mj.optString("roundName", "الجولة 1"),
                            manOfTheMatchPlayerId = mj.optString("manOfTheMatchPlayerId", ""),
                            manOfTheMatchPlayerName = mj.optString("manOfTheMatchPlayerName", ""),
                            createdAt = mj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _matches.value = matchesList
            }

            recalculateStandings()
            recalculatePlayerStats()
        } catch (e: Exception) {
            android.util.Log.e("DorisioRepository", "Error loading local cache: ${e.message}")
        }
    }

    fun loadSampleData() {
        // Option for user to populate demo data if they want to preview
        val sampleTeams = listOf(
            Team("t1", "league_dorisio_1", "صقور المجد", "", "#059669", "p1", "محمد الشمري"),
            Team("t2", "league_dorisio_1", "نجوم المستقبل", "", "#2563EB", "p5", "عمر القحطاني"),
            Team("t3", "league_dorisio_1", "فرسان التحدي", "", "#DC2626", "p9", "خالد العتيبي"),
            Team("t4", "league_dorisio_1", "أبطال الملعب", "", "#D97706", "p13", "يوسف الدوسري")
        )
        _teams.value = sampleTeams

        val samplePlayers = listOf(
            Player("p1", "", "محمد الشمري", "", "t1", "صقور المجد", "league_dorisio_1", 10, "مهاجم", 2, 2, 4, 1, 1, 0, 1, 9.2, 1),
            Player("p2", "", "أحمد الحربي", "", "t1", "صقور المجد", "league_dorisio_1", 7, "وسط", 2, 2, 1, 2, 0, 0, 0, 8.4, 0),
            Player("p3", "", "سعود الغامدي", "", "t1", "صقور المجد", "league_dorisio_1", 4, "مدافع", 2, 2, 0, 0, 1, 0, 0, 7.8, 0),
            Player("p4", "", "عبدالله السالم", "", "t1", "صقور المجد", "league_dorisio_1", 1, "حارس مرمى", 2, 2, 0, 0, 0, 0, 0, 8.5, 0),

            Player("p5", "", "عمر القحطاني", "", "t2", "نجوم المستقبل", "league_dorisio_1", 9, "مهاجم", 2, 2, 3, 1, 0, 0, 1, 8.9, 0),
            Player("p6", "", "ياسر الشهري", "", "t2", "نجوم المستقبل", "league_dorisio_1", 8, "وسط", 2, 2, 2, 1, 1, 0, 0, 8.1, 0),
            Player("p7", "", "فهد المالكي", "", "t2", "نجوم المستقبل", "league_dorisio_1", 3, "مدافع", 2, 2, 0, 0, 0, 0, 0, 7.5, 0),
            Player("p8", "", "سلطان العنزي", "", "t2", "نجوم المستقبل", "league_dorisio_1", 1, "حارس مرمى", 2, 2, 0, 0, 0, 0, 0, 8.0, 0)
        )
        _players.value = samplePlayers

        val match1 = FootballMatch(
            matchId = "m1",
            leagueId = "league_dorisio_1",
            homeTeamId = "t1",
            homeTeamName = "صقور المجد",
            homeTeamColor = "#059669",
            awayTeamId = "t2",
            awayTeamName = "نجوم المستقبل",
            awayTeamColor = "#2563EB",
            date = "اليوم",
            startTime = "16:30",
            venue = "الملعب الرئيسي",
            referee = "الكابتن طارق",
            status = MatchStatus.LIVE,
            homeScore = 2,
            awayScore = 1,
            currentMinute = 18
        )
        _matches.value = listOf(match1)

        _matchEvents.value = mapOf(
            "m1" to listOf(
                MatchEvent("e1", "m1", 5, "t1", "صقور المجد", "p1", "محمد الشمري", "", "", EventType.GOAL, "تسديدة رائعة في الشباك"),
                MatchEvent("e2", "m1", 11, "t2", "نجوم المستقبل", "p5", "عمر القحطاني", "", "", EventType.GOAL, "هدف التعادل"),
                MatchEvent("e3", "m1", 17, "t1", "صقور المجد", "p1", "محمد الشمري", "", "", EventType.GOAL, "متابعة رأسية حاسمة")
            )
        )

        prefs.edit().putBoolean("has_initialized_data", true).apply()
        recalculateStandings()
    }

    fun clearAllData() {
        _teams.value = emptyList()
        _players.value = emptyList()
        _matches.value = emptyList()
        _matchEvents.value = emptyMap()
        _standings.value = emptyList()
        _weeklyAwards.value = emptyList()
        prefs.edit().putBoolean("has_initialized_data", false).apply()
    }

    private fun startLiveMatchTicker() {
        scope.launch {
            while (isActive) {
                delay(3000)
                val currentMatches = _matches.value
                val hasLive = currentMatches.any { it.status == MatchStatus.LIVE }
                if (hasLive) {
                    _matches.value = currentMatches.map { match ->
                        if (match.status == MatchStatus.LIVE) {
                            val nextMin = if (match.currentMinute < 30) match.currentMinute + 1 else 30
                            match.copy(currentMinute = nextMin)
                        } else match
                    }
                }
            }
        }
    }

    // ==========================================
    // AUTHENTICATION & SESSIONS
    // ==========================================

    private fun loadSavedUser() {
        val uid = prefs.getString("user_uid", null)
        if (uid != null) {
            val email = prefs.getString("user_email", "") ?: ""
            val name = prefs.getString("user_name", "") ?: ""
            val roleStr = prefs.getString("user_role", UserRole.USER.name) ?: UserRole.USER.name
            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.USER }
            val shirtNumber = prefs.getInt("user_shirt", 10)
            val position = prefs.getString("user_pos", "وسط") ?: "وسط"
            val teamName = prefs.getString("user_team", "") ?: ""
            val photoBase64 = prefs.getString("user_photo", "") ?: ""

            _currentUser.value = UserAccount(
                userId = uid,
                email = email,
                displayName = name,
                photoUrl = photoBase64,
                role = role,
                shirtNumber = shirtNumber,
                position = position,
                favoriteTeamName = teamName
            )
        } else {
            _currentUser.value = null // Not logged in initially so user sees the Auth Screen
        }
    }

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        shirtNumber: Int,
        position: String,
        photoBase64: String
    ): Pair<Boolean, String> {
        val normalizedEmail = email.trim().lowercase()
        val uid = "user_" + UUID.nameUUIDFromBytes(normalizedEmail.toByteArray()).toString().take(10)

        val user = UserAccount(
            userId = uid,
            email = normalizedEmail,
            displayName = name.ifBlank { "كابتن دوريسيو" },
            photoUrl = photoBase64,
            role = UserRole.ADMIN,
            shirtNumber = shirtNumber,
            position = position,
            favoriteTeamName = ""
        )

        // Save locally first
        prefs.edit()
            .putString("pwd_$normalizedEmail", password)
            .putString("uid_$normalizedEmail", uid)
            .putString("name_$normalizedEmail", name)
            .putString("role_$normalizedEmail", user.role.name)
            .putInt("shirt_$normalizedEmail", shirtNumber)
            .putString("pos_$normalizedEmail", position)
            .putString("photo_$normalizedEmail", photoBase64)
            .apply()

        // Sync with Firebase Auth and Firestore immediately
        if (firebaseBridge.isFirebaseInitialized) {
            val fbRes = firebaseBridge.signUpWithFirebase(
                email = normalizedEmail,
                password = password,
                displayName = name,
                shirtNumber = shirtNumber,
                position = position,
                photoBase64 = photoBase64
            )
            if (fbRes.isSuccess) {
                val createdUser = fbRes.getOrThrow()
                saveUser(createdUser)
                return Pair(true, "تم إنشاء الحساب وحفظ بياناتك بالكامل في السحابة 🏆")
            } else {
                // If user exists in Firebase Auth, sign in and save profile
                val signInRes = firebaseBridge.signInWithFirebase(normalizedEmail, password)
                if (signInRes.isSuccess) {
                    val loggedUser = signInRes.getOrThrow().copy(
                        displayName = name.ifBlank { "لاعب دوريسيو" },
                        photoUrl = if (photoBase64.isNotBlank()) photoBase64 else signInRes.getOrThrow().photoUrl,
                        shirtNumber = shirtNumber,
                        position = position
                    )
                    saveUser(loggedUser)
                    return Pair(true, "تم تسجيل الحساب وتحديث بياناتك بالسحابة 🏆")
                }
            }
        }

        saveUser(user)
        return Pair(true, "تم إنشاء الحساب بنجاح كلاعب في الدوري 🏆")
    }

    suspend fun loginUser(email: String, password: String): Pair<Boolean, String> {
        val normalizedEmail = email.trim().lowercase()

        // Master admin shortcut
        if (normalizedEmail == "admin@dorisio.com" && (password == "DORISIO2026" || password == "admin")) {
            val adminUser = UserAccount(
                userId = "admin_master_1",
                email = normalizedEmail,
                displayName = "مدير دوريسيو (Admin)",
                photoUrl = "",
                role = UserRole.ADMIN,
                shirtNumber = 10,
                position = "وسط"
            )
            saveUser(adminUser)
            return Pair(true, "تم تسجيل الدخول كمسؤول")
        }

        // 1. Try Cloud Firebase Auth / Firestore User Document first
        if (firebaseBridge.isFirebaseInitialized) {
            val result = firebaseBridge.signInWithFirebase(normalizedEmail, password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                saveUser(user)
                return Pair(true, "تم تسجيل الدخول بنجاح")
            }

            // Search Firestore by email if user account document exists
            val cloudUserByEmail = firebaseBridge.getUserByEmail(normalizedEmail)
            if (cloudUserByEmail != null) {
                val savedPwd = prefs.getString("pwd_$normalizedEmail", null)
                if (savedPwd == null || savedPwd == password) {
                    saveUser(cloudUserByEmail)
                    prefs.edit().putString("pwd_$normalizedEmail", password).apply()
                    return Pair(true, "تم تسجيل الدخول واسترجاع حسابك السحابي بنجاح")
                }
            }
        }

        // 2. Check local saved credentials
        val savedPwd = prefs.getString("pwd_$normalizedEmail", null)
        if (savedPwd != null && savedPwd == password) {
            val uid = prefs.getString("uid_$normalizedEmail", "user_1") ?: "user_1"
            val name = prefs.getString("name_$normalizedEmail", "كابتن دوريسيو") ?: "كابتن دوريسيو"
            val roleStr = prefs.getString("role_$normalizedEmail", UserRole.USER.name) ?: UserRole.USER.name
            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.USER }
            val shirt = prefs.getInt("shirt_$normalizedEmail", 10)
            val pos = prefs.getString("pos_$normalizedEmail", "وسط") ?: "وسط"
            val photo = prefs.getString("photo_$normalizedEmail", "") ?: ""

            val user = UserAccount(
                userId = uid,
                email = normalizedEmail,
                displayName = name,
                photoUrl = photo,
                role = role,
                shirtNumber = shirt,
                position = pos
            )
            saveUser(user)
            return Pair(true, "تم تسجيل الدخول بنجاح")
        }

        return Pair(false, "عفواً، البريد الإلكتروني أو كلمة المرور غير صحيحة")
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Pair<Boolean, String> {
        val result = firebaseBridge.signInWithGoogleIdToken(idToken)
        return if (result.isSuccess) {
            val user = result.getOrThrow()
            saveUser(user)
            Pair(true, "تم تسجيل الدخول بحساب Google بنجاح 🎉")
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "فشل تسجيل الدخول بحساب Google"
            Pair(false, errorMsg)
        }
    }

    suspend fun loginWithGoogle(context: Context): Pair<Boolean, String> {
        val result = firebaseBridge.signInWithGoogle(context)
        return if (result.isSuccess) {
            val user = result.getOrThrow()
            saveUser(user)
            Pair(true, "تم تسجيل الدخول بحساب Google بنجاح 🎉")
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "فشل تسجيل الدخول بحساب Google"
            Pair(false, errorMsg)
        }
    }

    suspend fun loginWithGitHub(activity: Activity): Pair<Boolean, String> {
        val result = firebaseBridge.signInWithGitHub(activity)
        return if (result.isSuccess) {
            val user = result.getOrThrow()
            saveUser(user)
            Pair(true, "تم تسجيل الدخول بحساب GitHub بنجاح 🚀")
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "فشل تسجيل الدخول بحساب GitHub"
            Pair(false, errorMsg)
        }
    }

    fun saveUser(user: UserAccount) {
        val adminUser = user.copy(role = UserRole.ADMIN)
        prefs.edit().apply {
            putString("user_uid", adminUser.userId)
            putString("user_email", adminUser.email)
            putString("user_name", adminUser.displayName)
            putString("user_role", adminUser.role.name)
            putInt("user_shirt", adminUser.shirtNumber)
            putString("user_pos", adminUser.position)
            putString("user_team", adminUser.favoriteTeamName)
            putString("user_photo", adminUser.photoUrl)
            apply()
        }
        _currentUser.value = adminUser
        triggerGlobalCloudPush()
        scope.launch {
            firebaseBridge.saveUserDocument(adminUser)
        }
    }

    fun logout() {
        // ONLY clear current active session keys, NEVER wipe registered user accounts or passwords!
        prefs.edit()
            .remove("user_uid")
            .remove("user_email")
            .remove("user_name")
            .remove("user_role")
            .remove("user_shirt")
            .remove("user_pos")
            .remove("user_team")
            .remove("user_photo")
            .apply()
        _currentUser.value = null
    }

    fun updateUserProfile(name: String, shirtNumber: Int, position: String, teamName: String, photoBase64: String = "") {
        val curr = _currentUser.value ?: return
        val updated = curr.copy(
            displayName = name,
            shirtNumber = shirtNumber,
            position = position,
            favoriteTeamName = teamName,
            photoUrl = if (photoBase64.isNotBlank()) photoBase64 else curr.photoUrl
        )
        saveUser(updated)
    }

    fun claimAdminRole(secretCode: String): Boolean {
        if (secretCode.trim() == "DORISIO2026") {
            val curr = _currentUser.value ?: return false
            val updated = curr.copy(role = UserRole.ADMIN)
            prefs.edit().putString("role_${curr.email}", UserRole.ADMIN.name).apply()
            saveUser(updated)
            return true
        }
        return false
    }

    // ==========================================
    // MATCH EVENTS & LIVE CENTER
    // ==========================================

    fun addMatchEvent(
        matchId: String,
        minute: Int,
        teamId: String,
        teamName: String,
        playerId: String,
        playerName: String,
        type: EventType,
        description: String
    ) {
        val newEvent = MatchEvent(
            eventId = UUID.randomUUID().toString(),
            matchId = matchId,
            minute = minute,
            teamId = teamId,
            teamName = teamName,
            playerId = playerId,
            playerName = playerName,
            type = type,
            description = description,
            createdAt = System.currentTimeMillis()
        )

        val currentMap = _matchEvents.value.toMutableMap()
        val list = (currentMap[matchId] ?: emptyList()).toMutableList()
        list.add(newEvent)
        list.sortBy { it.minute }
        currentMap[matchId] = list
        _matchEvents.value = currentMap

        scope.launch {
            firebaseBridge.saveMatchEvent(newEvent)
        }

        recalculateScores(matchId)
        recalculatePlayerStats()
        triggerGlobalCloudPush()
    }

    fun deleteMatchEvent(matchId: String, eventId: String) {
        val currentMap = _matchEvents.value.toMutableMap()
        val list = (currentMap[matchId] ?: emptyList()).filterNot { it.eventId == eventId }
        currentMap[matchId] = list
        _matchEvents.value = currentMap

        scope.launch {
            firebaseBridge.deleteMatchEvent(eventId)
        }

        recalculateScores(matchId)
        recalculatePlayerStats()
        triggerGlobalCloudPush()
    }

    private fun recalculateScores(matchId: String) {
        val events = _matchEvents.value[matchId] ?: emptyList()
        val match = _matches.value.find { it.matchId == matchId } ?: return

        val homeGoals = events.count { it.teamId == match.homeTeamId && it.type == EventType.GOAL }
        val awayGoals = events.count { it.teamId == match.awayTeamId && it.type == EventType.GOAL }

        val updatedMatch = match.copy(
            homeScore = homeGoals,
            awayScore = awayGoals
        )

        _matches.value = _matches.value.map {
            if (it.matchId == matchId) updatedMatch else it
        }

        scope.launch {
            firebaseBridge.saveMatch(updatedMatch)
        }

        if (updatedMatch.status == MatchStatus.FINISHED) {
            recalculateStandings()
        }
    }

    fun updateMatchStatus(matchId: String, newStatus: MatchStatus) {
        val match = _matches.value.find { it.matchId == matchId } ?: return
        val updated = match.copy(status = newStatus)

        if (newStatus == MatchStatus.FINISHED) {
            val events = _matchEvents.value[matchId] ?: emptyList()
            if (events.none { it.type == EventType.FULL_TIME }) {
                addMatchEvent(
                    matchId = matchId,
                    minute = match.currentMinute.coerceAtLeast(30),
                    teamId = "",
                    teamName = "",
                    playerId = "",
                    playerName = "",
                    type = EventType.FULL_TIME,
                    description = "صفارة نهاية المباراة بنتيجة ${match.homeScore} - ${match.awayScore}"
                )
            }
        }

        _matches.value = _matches.value.map {
            if (it.matchId == matchId) updated else it
        }

        scope.launch {
            firebaseBridge.saveMatch(updated)
        }

        recalculateStandings()
    }

    fun setManOfTheMatch(matchId: String, playerId: String, playerName: String) {
        val match = _matches.value.find { it.matchId == matchId } ?: return
        val updated = match.copy(
            manOfTheMatchPlayerId = playerId,
            manOfTheMatchPlayerName = playerName
        )
        _matches.value = _matches.value.map {
            if (it.matchId == matchId) updated else it
        }
        recalculatePlayerStats()
    }

    // ==========================================
    // STANDINGS CALCULATION
    // ==========================================

    fun recalculateStandings() {
        val currentTeams = _teams.value
        val finishedMatches = _matches.value.filter { it.status == MatchStatus.FINISHED }

        val statsMap = currentTeams.associate { it.teamId to MutableStanding(it) }

        for (m in finishedMatches) {
            val home = statsMap[m.homeTeamId]
            val away = statsMap[m.awayTeamId]
            if (home != null && away != null) {
                home.played++
                away.played++
                home.gf += m.homeScore
                home.ga += m.awayScore
                away.gf += m.awayScore
                away.ga += m.homeScore

                when {
                    m.homeScore > m.awayScore -> {
                        home.won++
                        home.pts += 3
                        away.lost++
                    }
                    m.homeScore < m.awayScore -> {
                        away.won++
                        away.pts += 3
                        home.lost++
                    }
                    else -> {
                        home.drawn++
                        home.pts += 1
                        away.drawn++
                        away.pts += 1
                    }
                }
            }
        }

        val sortedList = statsMap.values
            .sortedWith(
                compareByDescending<MutableStanding> { it.pts }
                    .thenByDescending { it.gf - it.ga }
                    .thenByDescending { it.gf }
            )
            .mapIndexed { index, item ->
                LeagueStanding(
                    position = index + 1,
                    teamId = item.team.teamId,
                    teamName = item.team.name,
                    teamLogo = item.team.logo,
                    teamColor = item.team.colorHex,
                    played = item.played,
                    won = item.won,
                    drawn = item.drawn,
                    lost = item.lost,
                    goalsFor = item.gf,
                    goalsAgainst = item.ga,
                    goalDifference = item.gf - item.ga,
                    points = item.pts
                )
            }

        _standings.value = sortedList
    }

    private class MutableStanding(val team: Team) {
        var played: Int = 0
        var won: Int = 0
        var drawn: Int = 0
        var lost: Int = 0
        var gf: Int = 0
        var ga: Int = 0
        var pts: Int = 0
    }

    private fun recalculatePlayerStats() {
        val allEvents = _matchEvents.value.values.flatten()
        val allMatches = _matches.value

        _players.value = _players.value.map { player ->
            val goals = allEvents.count { it.playerId == player.playerId && it.type == EventType.GOAL }
            val assists = allEvents.count { it.playerId == player.playerId && it.type == EventType.ASSIST }
            val yellows = allEvents.count { it.playerId == player.playerId && it.type == EventType.YELLOW_CARD }
            val reds = allEvents.count { it.playerId == player.playerId && it.type == EventType.RED_CARD }
            val motm = allMatches.count { it.manOfTheMatchPlayerId == player.playerId }

            player.copy(
                goals = goals,
                assists = assists,
                yellowCards = yellows,
                redCards = reds,
                manOfTheMatchCount = motm
            )
        }
    }

    // ==========================================
    // PLAYER RATINGS
    // ==========================================

    fun submitRating(matchId: String, playerId: String, score: Double): Boolean {
        val user = _currentUser.value ?: return false
        val currentList = _playerRatings.value.toMutableList()

        val existing = currentList.find {
            it.matchId == matchId && it.playerId == playerId && it.ratedByUserId == user.userId
        }
        if (existing != null) {
            currentList.remove(existing)
            currentList.add(existing.copy(score = score.toInt(), ratingScore = score))
        } else {
            currentList.add(
                PlayerRating(
                    ratingId = UUID.randomUUID().toString(),
                    matchId = matchId,
                    playerId = playerId,
                    ratedByUserId = user.userId,
                    score = score.toInt(),
                    ratingScore = score
                )
            )
        }

        _playerRatings.value = currentList

        val playerRatingsList = currentList.filter { it.playerId == playerId }
        if (playerRatingsList.isNotEmpty()) {
            val avg = playerRatingsList.map { it.ratingScore }.average()
            val rounded = (avg * 10).toInt() / 10.0
            _players.value = _players.value.map {
                if (it.playerId == playerId) it.copy(averageRating = rounded) else it
            }
        }
        return true
    }

    fun submitRating(matchId: String, playerId: String, score: Int): Boolean {
        return submitRating(matchId, playerId, score.toDouble())
    }

    // ==========================================
    // AWARDS & ANNOUNCEMENTS
    // ==========================================

    fun announcePlayerOfWeek(week: Int, playerId: String, reason: String) {
        val player = _players.value.find { it.playerId == playerId } ?: return
        val award = WeeklyAward(
            awardId = UUID.randomUUID().toString(),
            leagueId = _currentLeague.value.leagueId,
            weekNumber = week,
            playerId = player.playerId,
            playerName = player.name,
            playerImage = player.profileImage,
            teamName = player.teamName,
            rating = player.averageRating,
            goals = player.goals,
            assists = player.assists,
            reason = reason
        )
        _weeklyAwards.value = listOf(award) + _weeklyAwards.value

        addAnnouncement(
            title = "⭐ إعلان لاعب الأسبوع (الجولة $week)",
            content = "تهانينا للاعب ${player.name} من فريق ${player.teamName} على فوزه بجائزة أفضل لاعب في الأسبوع! $reason",
            isPinned = true
        )
    }

    fun addAnnouncement(title: String, content: String, isPinned: Boolean) {
        val announcement = Announcement(
            id = UUID.randomUUID().toString(),
            leagueId = _currentLeague.value.leagueId,
            title = title,
            content = content,
            authorName = _currentUser.value?.displayName ?: "إدارة دوريسيو",
            createdAt = System.currentTimeMillis(),
            isPinned = isPinned
        )
        _announcements.value = listOf(announcement) + _announcements.value
        scope.launch { firebaseBridge.saveAnnouncement(announcement) }
    }

    // ==========================================
    // TEAM & PLAYER & MATCH COMPLETE ADMIN CONTROL
    // ==========================================

    fun createTeam(name: String, colorHex: String, logo: String = "") {
        val newTeam = Team(
            teamId = "t_" + UUID.randomUUID().toString().take(6),
            leagueId = _currentLeague.value.leagueId,
            name = name.trim(),
            logo = logo,
            colorHex = colorHex
        )
        _teams.value = _teams.value + newTeam
        recalculateStandings()
        triggerGlobalCloudPush()
        scope.launch { firebaseBridge.saveTeam(newTeam) }
    }

    fun deleteTeam(teamId: String) {
        deletedTeamIds.add(teamId)
        _teams.value = _teams.value.filterNot { it.teamId == teamId }
        // Update players who were on this team
        _players.value = _players.value.map {
            if (it.teamId == teamId) it.copy(teamId = "", teamName = "بدون فريق") else it
        }
        // Remove scheduled matches involving this team
        _matches.value = _matches.value.filterNot {
            (it.homeTeamId == teamId || it.awayTeamId == teamId) && it.status == MatchStatus.SCHEDULED
        }
        recalculateStandings()
        triggerGlobalCloudPush()
        scope.launch { firebaseBridge.deleteTeam(teamId) }
    }

    fun updateTeam(teamId: String, name: String, colorHex: String, logo: String = "") {
        var updated: Team? = null
        _teams.value = _teams.value.map {
            if (it.teamId == teamId) {
                val t = it.copy(
                    name = name.trim(),
                    colorHex = colorHex,
                    logo = if (logo.isNotBlank()) logo else it.logo
                )
                updated = t
                t
            } else it
        }
        // Update players belonging to this team
        _players.value = _players.value.map {
            if (it.teamId == teamId) it.copy(teamName = name.trim()) else it
        }
        // Update matches logos and names
        _matches.value = _matches.value.map {
            var m = it
            if (m.homeTeamId == teamId) {
                m = m.copy(
                    homeTeamName = name.trim(),
                    homeTeamColor = colorHex,
                    homeTeamLogo = if (logo.isNotBlank()) logo else m.homeTeamLogo
                )
            }
            if (m.awayTeamId == teamId) {
                m = m.copy(
                    awayTeamName = name.trim(),
                    awayTeamColor = colorHex,
                    awayTeamLogo = if (logo.isNotBlank()) logo else m.awayTeamLogo
                )
            }
            m
        }
        recalculateStandings()
        triggerGlobalCloudPush()
        if (updated != null) {
            scope.launch { firebaseBridge.saveTeam(updated!!) }
        }
    }

    fun addPlayerToTeam(name: String, teamId: String, shirtNumber: Int, position: String, photoBase64: String = "") {
        val team = _teams.value.find { it.teamId == teamId }
        val newPlayer = Player(
            playerId = "p_" + UUID.randomUUID().toString().take(6),
            name = name.trim(),
            profileImage = photoBase64,
            teamId = teamId,
            teamName = team?.name ?: "بدون فريق",
            leagueId = _currentLeague.value.leagueId,
            shirtNumber = shirtNumber,
            position = position
        )
        _players.value = _players.value + newPlayer
        triggerGlobalCloudPush()
        scope.launch { firebaseBridge.savePlayer(newPlayer) }
    }

    fun deletePlayer(playerId: String) {
        deletedPlayerIds.add(playerId)
        _players.value = _players.value.filterNot { it.playerId == playerId }
        // Clean events
        val updatedEvents = _matchEvents.value.mapValues { (_, events) ->
            events.filterNot { it.playerId == playerId }
        }
        _matchEvents.value = updatedEvents
        recalculatePlayerStats()
        triggerGlobalCloudPush()
        scope.launch { firebaseBridge.deletePlayer(playerId) }
    }

    fun movePlayerToTeam(playerId: String, newTeamId: String) {
        val targetTeam = _teams.value.find { it.teamId == newTeamId } ?: return
        var updated: Player? = null
        _players.value = _players.value.map {
            if (it.playerId == playerId) {
                val p = it.copy(teamId = targetTeam.teamId, teamName = targetTeam.name)
                updated = p
                p
            } else it
        }
        if (updated != null) {
            scope.launch { firebaseBridge.savePlayer(updated!!) }
        }
    }

    fun updatePlayer(
        playerId: String,
        name: String,
        shirtNumber: Int,
        position: String,
        photoBase64: String = ""
    ) {
        var updated: Player? = null
        _players.value = _players.value.map {
            if (it.playerId == playerId) {
                val p = it.copy(
                    name = name.trim(),
                    shirtNumber = shirtNumber,
                    position = position,
                    profileImage = if (photoBase64.isNotBlank()) photoBase64 else it.profileImage
                )
                updated = p
                p
            } else it
        }
        if (updated != null) {
            scope.launch { firebaseBridge.savePlayer(updated!!) }
        }
    }

    fun createMatch(homeTeamId: String, awayTeamId: String, date: String, time: String, venue: String, round: Int = 1) {
        val home = _teams.value.find { it.teamId == homeTeamId } ?: return
        val away = _teams.value.find { it.teamId == awayTeamId } ?: return

        val newMatch = FootballMatch(
            matchId = "m_" + UUID.randomUUID().toString().take(6),
            leagueId = _currentLeague.value.leagueId,
            homeTeamId = home.teamId,
            homeTeamName = home.name,
            homeTeamColor = home.colorHex,
            awayTeamId = away.teamId,
            awayTeamName = away.name,
            awayTeamColor = away.colorHex,
            date = date,
            startTime = time,
            venue = venue.ifBlank { "الملعب الرئيسي" },
            referee = "حكم معتمد",
            round = round,
            roundName = "الجولة $round",
            status = MatchStatus.SCHEDULED
        )
        _matches.value = _matches.value + newMatch
        scope.launch { firebaseBridge.saveMatch(newMatch) }
    }

    fun deleteMatch(matchId: String) {
        deletedMatchIds.add(matchId)
        _matches.value = _matches.value.filterNot { it.matchId == matchId }
        val updatedEvents = _matchEvents.value.toMutableMap()
        updatedEvents.remove(matchId)
        _matchEvents.value = updatedEvents
        recalculateStandings()
        triggerGlobalCloudPush()
        scope.launch { firebaseBridge.deleteMatch(matchId) }
    }

    fun updateMatchDetails(matchId: String, date: String, time: String, venue: String) {
        var updated: FootballMatch? = null
        _matches.value = _matches.value.map {
            if (it.matchId == matchId) {
                val m = it.copy(date = date, startTime = time, venue = venue)
                updated = m
                m
            } else it
        }
        if (updated != null) {
            scope.launch { firebaseBridge.saveMatch(updated!!) }
        }
    }

    fun clearAllMatches() {
        _matches.value = emptyList()
        _matchEvents.value = emptyMap()
        recalculateStandings()
    }

    // ==========================================
    // AUTOMATIC ROUND-ROBIN LEAGUE GENERATOR
    // ==========================================

    fun generateAutomaticSchedule(doubleRound: Boolean = false): Pair<Boolean, String> {
        val currentTeams = _teams.value
        if (currentTeams.size < 2) {
            return Pair(false, "يجب إضافة فريقين على الأقل لتوليد جدول مباريات الدوري!")
        }

        // Keep finished matches, replace scheduled ones
        val finishedMatches = _matches.value.filter { it.status == MatchStatus.FINISHED }

        val teamList = currentTeams.toMutableList()
        val isOdd = teamList.size % 2 != 0
        val dummyTeam = Team(teamId = "BYE", name = "BYE", colorHex = "#FFFFFF")
        if (isOdd) {
            teamList.add(dummyTeam)
        }

        val numTeams = teamList.size
        val numRounds = numTeams - 1
        val halfSize = numTeams / 2

        val generatedMatches = mutableListOf<FootballMatch>()

        val daysOfWeek = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        val timesOfDay = listOf("16:00", "17:00", "18:00", "19:00")

        // First leg rounds
        for (roundIndex in 0 until numRounds) {
            val roundNumber = roundIndex + 1
            val dayName = daysOfWeek[roundIndex % daysOfWeek.size]

            for (matchIndex in 0 until halfSize) {
                val t1 = teamList[matchIndex]
                val t2 = teamList[numTeams - 1 - matchIndex]

                if (t1.teamId != "BYE" && t2.teamId != "BYE") {
                    val home = if (roundIndex % 2 == 0) t1 else t2
                    val away = if (roundIndex % 2 == 0) t2 else t1
                    val time = timesOfDay[matchIndex % timesOfDay.size]

                    generatedMatches.add(
                        FootballMatch(
                            matchId = "m_auto_${roundNumber}_${matchIndex}_" + UUID.randomUUID().toString().take(4),
                            leagueId = _currentLeague.value.leagueId,
                            homeTeamId = home.teamId,
                            homeTeamName = home.name,
                            homeTeamColor = home.colorHex,
                            awayTeamId = away.teamId,
                            awayTeamName = away.name,
                            awayTeamColor = away.colorHex,
                            date = "$dayName (الأسبوع $roundNumber)",
                            startTime = time,
                            venue = "الملعب الرئيسي",
                            referee = "حكم معتمد",
                            round = roundNumber,
                            roundName = "الجولة $roundNumber (الأسبوع $roundNumber)",
                            status = MatchStatus.SCHEDULED
                        )
                    )
                }
            }

            // Rotate teams (keep index 0 fixed)
            val last = teamList.removeAt(teamList.size - 1)
            teamList.add(1, last)
        }

        // Optional second leg (Home/Away reverse)
        if (doubleRound) {
            val firstLegCount = generatedMatches.size
            for (i in 0 until firstLegCount) {
                val m = generatedMatches[i]
                val roundNumber = m.round + numRounds
                val dayName = daysOfWeek[(roundNumber - 1) % daysOfWeek.size]

                generatedMatches.add(
                    FootballMatch(
                        matchId = "m_auto_${roundNumber}_" + UUID.randomUUID().toString().take(4),
                        leagueId = _currentLeague.value.leagueId,
                        homeTeamId = m.awayTeamId,
                        homeTeamName = m.awayTeamName,
                        homeTeamColor = m.awayTeamColor,
                        awayTeamId = m.homeTeamId,
                        awayTeamName = m.homeTeamName,
                        awayTeamColor = m.homeTeamColor,
                        date = "$dayName (الأسبوع $roundNumber - إياب)",
                        startTime = m.startTime,
                        venue = "الملعب الرئيسي",
                        referee = "حكم معتمد",
                        round = roundNumber,
                        roundName = "الجولة $roundNumber (إياب)",
                        status = MatchStatus.SCHEDULED
                    )
                )
            }
        }

        _matches.value = finishedMatches + generatedMatches
        recalculateStandings()
        scope.launch {
            generatedMatches.forEach { firebaseBridge.saveMatch(it) }
        }

        val totalRounds = if (doubleRound) numRounds * 2 else numRounds
        return Pair(true, "تم توليد جدول الدوري بنجاح! تم إنشاء ${generatedMatches.size} مباراة مقسمة على $totalRounds جولات متوازنة.")
    }

    companion object {
        fun bitmapToBase64(bitmap: Bitmap): String {
            val maxDim = 160
            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxDim || height > maxDim) {
                val scale = maxDim.toFloat() / Math.max(width, height)
                Bitmap.createScaledBitmap(bitmap, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
            } else {
                bitmap
            }
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }

        fun base64ToBitmap(base64Str: String): Bitmap? {
            return try {
                val decoded = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                null
            }
        }
    }
}
