package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Universal High-Speed Cloud Sync Bridge for Dorisio League.
 * Guarantees zero-permission real-time cloud data synchronization across all devices
 * for Users, Profiles, Teams, Players, Matches, Events, and Announcements with Smart Merging.
 */
class CloudSyncBridge(private val context: Context) {

    private val tag = "CloudSyncBridge"
    private val cloudUrl = "https://api.restful-api.dev/objects/ff808181a09d98f701a0fdd4f4e0657a"
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var syncJob: Job? = null

    data class CloudPayload(
        val users: List<UserAccount>,
        val teams: List<Team>,
        val players: List<Player>,
        val matches: List<FootballMatch>,
        val matchEvents: Map<String, List<MatchEvent>>,
        val announcements: List<Announcement>
    )

    fun startSyncLoop(
        onCloudDataReceived: (CloudPayload) -> Unit
    ) {
        syncJob?.cancel()
        syncJob = syncScope.launch {
            while (isActive) {
                try {
                    val payload = fetchCloudPayload()
                    if (payload != null) {
                        withContext(Dispatchers.Main) {
                            onCloudDataReceived(payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Sync loop error: ${e.message}")
                }
                delay(3000L) // Poll every 3 seconds for instant multi-device sync
            }
        }
    }

    suspend fun pushPayloadToCloud(
        users: List<UserAccount>,
        teams: List<Team>,
        players: List<Player>,
        matches: List<FootballMatch>,
        matchEvents: Map<String, List<MatchEvent>>,
        announcements: List<Announcement>,
        deletedTeamIds: Set<String> = emptySet(),
        deletedPlayerIds: Set<String> = emptySet(),
        deletedMatchIds: Set<String> = emptySet()
    ): CloudPayload? = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch latest remote cloud payload first to prevent overwriting other devices' data!
            val remote = fetchCloudPayload()

            // 2. Smart Merge Users (by userId/email)
            val mergedUsersMap = mutableMapOf<String, UserAccount>()
            remote?.users?.forEach { u -> if (u.userId.isNotBlank()) mergedUsersMap[u.userId] = u }
            users.forEach { u ->
                if (u.userId.isNotBlank()) {
                    val existing = mergedUsersMap[u.userId]
                    if (existing == null || u.photoUrl.isNotBlank() || u.displayName.isNotBlank()) {
                        mergedUsersMap[u.userId] = u
                    }
                }
            }
            val mergedUsers = mergedUsersMap.values.toList()

            // 3. Smart Merge Teams (by teamId)
            val mergedTeamsMap = mutableMapOf<String, Team>()
            remote?.teams?.forEach { t -> if (t.teamId.isNotBlank() && t.teamId !in deletedTeamIds) mergedTeamsMap[t.teamId] = t }
            teams.forEach { t -> if (t.teamId.isNotBlank() && t.teamId !in deletedTeamIds) mergedTeamsMap[t.teamId] = t }
            val mergedTeams = mergedTeamsMap.values.toList()

            // 4. Smart Merge Players (by playerId)
            val mergedPlayersMap = mutableMapOf<String, Player>()
            remote?.players?.forEach { p -> if (p.playerId.isNotBlank() && p.playerId !in deletedPlayerIds) mergedPlayersMap[p.playerId] = p }
            players.forEach { p -> if (p.playerId.isNotBlank() && p.playerId !in deletedPlayerIds) mergedPlayersMap[p.playerId] = p }
            val mergedPlayers = mergedPlayersMap.values.toList()

            // 5. Smart Merge Matches (by matchId)
            val mergedMatchesMap = mutableMapOf<String, FootballMatch>()
            remote?.matches?.forEach { m -> if (m.matchId.isNotBlank() && m.matchId !in deletedMatchIds) mergedMatchesMap[m.matchId] = m }
            matches.forEach { m -> if (m.matchId.isNotBlank() && m.matchId !in deletedMatchIds) mergedMatchesMap[m.matchId] = m }
            val mergedMatches = mergedMatchesMap.values.toList()

            // 6. Smart Merge Match Events
            val mergedEventsMap = mutableMapOf<String, MutableList<MatchEvent>>()
            remote?.matchEvents?.forEach { (mId, evList) ->
                mergedEventsMap.getOrPut(mId) { mutableListOf() }.addAll(evList)
            }
            matchEvents.forEach { (mId, evList) ->
                val existingList = mergedEventsMap.getOrPut(mId) { mutableListOf() }
                evList.forEach { ev ->
                    if (existingList.none { it.eventId == ev.eventId }) {
                        existingList.add(ev)
                    }
                }
            }

            // 7. Smart Merge Announcements
            val mergedAnnMap = mutableMapOf<String, Announcement>()
            remote?.announcements?.forEach { a -> if (a.id.isNotBlank()) mergedAnnMap[a.id] = a }
            announcements.forEach { a -> if (a.id.isNotBlank()) mergedAnnMap[a.id] = a }
            val mergedAnnouncements = mergedAnnMap.values.sortedByDescending { it.createdAt }

            // Construct JSON payload
            val rootJson = JSONObject()
            rootJson.put("name", "dorisio_league_cloud_v2_main")

            val dataJson = JSONObject()

            // Users Array
            val usersArray = JSONArray()
            mergedUsers.forEach { u ->
                val uj = JSONObject()
                uj.put("userId", u.userId)
                uj.put("email", u.email)
                uj.put("displayName", u.displayName)
                uj.put("photoUrl", u.photoUrl)
                uj.put("role", u.role.name)
                uj.put("shirtNumber", u.shirtNumber)
                uj.put("position", u.position)
                uj.put("favoriteTeamId", u.favoriteTeamId)
                uj.put("favoriteTeamName", u.favoriteTeamName)
                uj.put("createdAt", u.createdAt)
                uj.put("provider", u.provider)
                usersArray.put(uj)
            }
            dataJson.put("users", usersArray)

            // Teams Array
            val teamsArray = JSONArray()
            mergedTeams.forEach { t ->
                val tj = JSONObject()
                tj.put("teamId", t.teamId)
                tj.put("leagueId", t.leagueId)
                tj.put("name", t.name)
                tj.put("logo", t.logo)
                tj.put("colorHex", t.colorHex)
                tj.put("captainId", t.captainId)
                tj.put("captainName", t.captainName)
                tj.put("createdAt", t.createdAt)
                teamsArray.put(tj)
            }
            dataJson.put("teams", teamsArray)

            // Players Array
            val playersArray = JSONArray()
            mergedPlayers.forEach { p ->
                val pj = JSONObject()
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
                playersArray.put(pj)
            }
            dataJson.put("players", playersArray)

            // Matches Array
            val matchesArray = JSONArray()
            mergedMatches.forEach { m ->
                val mj = JSONObject()
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
                matchesArray.put(mj)
            }
            dataJson.put("matches", matchesArray)

            // Events Map
            val eventsObj = JSONObject()
            mergedEventsMap.forEach { (mId, evList) ->
                val evArray = JSONArray()
                evList.forEach { e ->
                    val ej = JSONObject()
                    ej.put("eventId", e.eventId)
                    ej.put("matchId", e.matchId)
                    ej.put("minute", e.minute)
                    ej.put("teamId", e.teamId)
                    ej.put("teamName", e.teamName)
                    ej.put("playerId", e.playerId)
                    ej.put("playerName", e.playerName)
                    ej.put("subPlayerId", e.subPlayerId)
                    ej.put("subPlayerName", e.subPlayerName)
                    ej.put("type", e.type.name)
                    ej.put("description", e.description)
                    ej.put("createdAt", e.createdAt)
                    evArray.put(ej)
                }
                eventsObj.put(mId, evArray)
            }
            dataJson.put("matchEvents", eventsObj)

            // Announcements Array
            val annArray = JSONArray()
            mergedAnnouncements.forEach { a ->
                val aj = JSONObject()
                aj.put("id", a.id)
                aj.put("leagueId", a.leagueId)
                aj.put("title", a.title)
                aj.put("content", a.content)
                aj.put("authorName", a.authorName)
                aj.put("isPinned", a.isPinned)
                aj.put("createdAt", a.createdAt)
                annArray.put(aj)
            }
            dataJson.put("announcements", annArray)

            rootJson.put("data", dataJson)

            val url = URL(cloudUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val success = responseCode in 200..299
            Log.d(tag, "Push payload result: code $responseCode, success $success")

            if (success) {
                CloudPayload(
                    users = mergedUsers,
                    teams = mergedTeams,
                    players = mergedPlayers,
                    matches = mergedMatches,
                    matchEvents = mergedEventsMap,
                    announcements = mergedAnnouncements
                )
            } else null
        } catch (e: Exception) {
            Log.e(tag, "Failed to push payload to Cloud Sync: ${e.message}", e)
            null
        }
    }

    suspend fun fetchCloudPayload(): CloudPayload? = withContext(Dispatchers.IO) {
        try {
            val url = URL(cloudUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode !in 200..299) return@withContext null

            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                }
            }

            val rootJson = JSONObject(sb.toString())
            val dataJson = rootJson.optJSONObject("data") ?: return@withContext null

            // Users
            val usersList = mutableListOf<UserAccount>()
            val usersArray = dataJson.optJSONArray("users")
            if (usersArray != null) {
                for (i in 0 until usersArray.length()) {
                    val uj = usersArray.getJSONObject(i)
                    val u = UserAccount(
                        userId = uj.optString("userId", ""),
                        email = uj.optString("email", ""),
                        displayName = uj.optString("displayName", ""),
                        photoUrl = uj.optString("photoUrl", ""),
                        role = try { UserRole.valueOf(uj.optString("role", UserRole.USER.name)) } catch (e: Exception) { UserRole.USER },
                        shirtNumber = uj.optInt("shirtNumber", 10),
                        position = uj.optString("position", "وسط"),
                        favoriteTeamId = uj.optString("favoriteTeamId", ""),
                        favoriteTeamName = uj.optString("favoriteTeamName", ""),
                        createdAt = uj.optLong("createdAt", System.currentTimeMillis()),
                        provider = uj.optString("provider", "password")
                    )
                    if (u.userId.isNotBlank()) usersList.add(u)
                }
            }

            // Teams
            val teamsList = mutableListOf<Team>()
            val teamsArray = dataJson.optJSONArray("teams")
            if (teamsArray != null) {
                for (i in 0 until teamsArray.length()) {
                    val tj = teamsArray.getJSONObject(i)
                    val t = Team(
                        teamId = tj.optString("teamId", ""),
                        leagueId = tj.optString("leagueId", "league_dorisio_1"),
                        name = tj.optString("name", "فريق"),
                        logo = tj.optString("logo", ""),
                        colorHex = tj.optString("colorHex", "#059669"),
                        captainId = tj.optString("captainId", ""),
                        captainName = tj.optString("captainName", ""),
                        createdAt = tj.optLong("createdAt", System.currentTimeMillis())
                    )
                    if (t.teamId.isNotBlank()) teamsList.add(t)
                }
            }

            // Players
            val playersList = mutableListOf<Player>()
            val playersArray = dataJson.optJSONArray("players")
            if (playersArray != null) {
                for (i in 0 until playersArray.length()) {
                    val pj = playersArray.getJSONObject(i)
                    val p = Player(
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
                    if (p.playerId.isNotBlank()) playersList.add(p)
                }
            }

            // Matches
            val matchesList = mutableListOf<FootballMatch>()
            val matchesArray = dataJson.optJSONArray("matches")
            if (matchesArray != null) {
                for (i in 0 until matchesArray.length()) {
                    val mj = matchesArray.getJSONObject(i)
                    val m = FootballMatch(
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
                    if (m.matchId.isNotBlank()) matchesList.add(m)
                }
            }

            // Events
            val eventsMap = mutableMapOf<String, List<MatchEvent>>()
            val eventsObj = dataJson.optJSONObject("matchEvents")
            if (eventsObj != null) {
                val keys = eventsObj.keys()
                while (keys.hasNext()) {
                    val matchId = keys.next()
                    val evArray = eventsObj.optJSONArray(matchId)
                    if (evArray != null) {
                        val evList = mutableListOf<MatchEvent>()
                        for (j in 0 until evArray.length()) {
                            val ej = evArray.getJSONObject(j)
                            val e = MatchEvent(
                                eventId = ej.optString("eventId", ""),
                                matchId = ej.optString("matchId", matchId),
                                minute = ej.optInt("minute", 0),
                                teamId = ej.optString("teamId", ""),
                                teamName = ej.optString("teamName", ""),
                                playerId = ej.optString("playerId", ""),
                                playerName = ej.optString("playerName", ""),
                                subPlayerId = ej.optString("subPlayerId", ""),
                                subPlayerName = ej.optString("subPlayerName", ""),
                                type = try { EventType.valueOf(ej.optString("type", EventType.GOAL.name)) } catch (ex: Exception) { EventType.GOAL },
                                description = ej.optString("description", ""),
                                createdAt = ej.optLong("createdAt", System.currentTimeMillis())
                            )
                            if (e.eventId.isNotBlank()) evList.add(e)
                        }
                        eventsMap[matchId] = evList
                    }
                }
            }

            // Announcements
            val announcementsList = mutableListOf<Announcement>()
            val annArray = dataJson.optJSONArray("announcements")
            if (annArray != null) {
                for (i in 0 until annArray.length()) {
                    val aj = annArray.getJSONObject(i)
                    val a = Announcement(
                        id = aj.optString("id", ""),
                        leagueId = aj.optString("leagueId", "league_dorisio_1"),
                        title = aj.optString("title", ""),
                        content = aj.optString("content", ""),
                        authorName = aj.optString("authorName", "إدارة دوريسيو"),
                        isPinned = aj.optBoolean("isPinned", false),
                        createdAt = aj.optLong("createdAt", System.currentTimeMillis())
                    )
                    if (a.id.isNotBlank()) announcementsList.add(a)
                }
            }

            CloudPayload(
                users = usersList,
                teams = teamsList,
                players = playersList,
                matches = matchesList,
                matchEvents = eventsMap,
                announcements = announcementsList
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch Cloud Payload: ${e.message}")
            null
        }
    }
}
