package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DorisioRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    MATCHES,
    STANDINGS,
    PLAYERS,
    PROFILE,
    MATCH_DETAILS,
    LIVE_ADMIN_CENTER,
    ADMIN_PANEL,
    FIREBASE_SETUP_GUIDE
}

class DorisioViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DorisioRepository(application)

    // Current Screen
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation Stack for seamless back handling
    private val navigationStack = mutableListOf(Screen.HOME)

    // Selected Match for details & live center
    private val _selectedMatchId = MutableStateFlow<String?>(null)
    val selectedMatchId: StateFlow<String?> = _selectedMatchId.asStateFlow()

    // Filter/Tab states
    private val _matchesFilter = MutableStateFlow("ALL") // "ALL", "LIVE", "SCHEDULED", "FINISHED"
    val matchesFilter: StateFlow<String> = _matchesFilter.asStateFlow()

    private val _playersTab = MutableStateFlow("SCORERS") // "SCORERS", "ASSISTS", "AWARDS", "ALL"
    val playersTab: StateFlow<String> = _playersTab.asStateFlow()

    // Exposed repository state
    val currentUser = repository.currentUser
    val currentLeague = repository.currentLeague
    val teams = repository.teams
    val players = repository.players
    val matches = repository.matches
    val matchEvents = repository.matchEvents
    val standings = repository.standings
    val weeklyAwards = repository.weeklyAwards
    val seasonAwards = repository.seasonAwards
    val announcements = repository.announcements
    val isFirebaseConfigured = repository.isFirebaseConfigured
    val connectionStatus = repository.connectionStatus

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            navigationStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
            _currentScreen.value = navigationStack.last()
            return true
        }
        return false
    }

    fun selectMatch(matchId: String, openLiveAdmin: Boolean = false) {
        _selectedMatchId.value = matchId
        if (openLiveAdmin) {
            navigateTo(Screen.LIVE_ADMIN_CENTER)
        } else {
            navigateTo(Screen.MATCH_DETAILS)
        }
    }

    fun setMatchesFilter(filter: String) {
        _matchesFilter.value = filter
    }

    fun setPlayersTab(tab: String) {
        _playersTab.value = tab
    }

    // Auth actions
    suspend fun loginUser(email: String, password: String): Pair<Boolean, String> {
        return repository.loginUser(email, password)
    }

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        shirtNumber: Int,
        position: String,
        photoBase64: String
    ): Pair<Boolean, String> {
        return repository.registerUser(name, email, password, shirtNumber, position, photoBase64)
    }

    suspend fun loginWithGoogle(context: android.content.Context): Pair<Boolean, String> {
        return repository.loginWithGoogle(context)
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Pair<Boolean, String> {
        return repository.loginWithGoogleIdToken(idToken)
    }

    suspend fun loginWithGitHub(activity: android.app.Activity): Pair<Boolean, String> {
        return repository.loginWithGitHub(activity)
    }

    fun claimAdmin(code: String): Boolean {
        return repository.claimAdminRole(code)
    }

    fun updateProfile(name: String, shirt: Int, pos: String, team: String, photoBase64: String = "") {
        repository.updateUserProfile(name, shirt, pos, team, photoBase64)
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = Screen.HOME
    }

    // Match actions
    fun addEvent(
        matchId: String,
        minute: Int,
        teamId: String,
        teamName: String,
        playerId: String,
        playerName: String,
        type: EventType,
        description: String
    ) {
        repository.addMatchEvent(matchId, minute, teamId, teamName, playerId, playerName, type, description)
    }

    fun deleteEvent(matchId: String, eventId: String) {
        repository.deleteMatchEvent(matchId, eventId)
    }

    fun updateMatchStatus(matchId: String, status: MatchStatus) {
        repository.updateMatchStatus(matchId, status)
    }

    fun setManOfTheMatch(matchId: String, playerId: String, playerName: String) {
        repository.setManOfTheMatch(matchId, playerId, playerName)
    }

    fun submitRating(matchId: String, playerId: String, score: Double): Boolean {
        return repository.submitRating(matchId, playerId, score)
    }

    fun submitRating(matchId: String, playerId: String, score: Int): Boolean {
        return repository.submitRating(matchId, playerId, score.toDouble())
    }

    fun announcePlayerOfWeek(week: Int, playerId: String, reason: String) {
        repository.announcePlayerOfWeek(week, playerId, reason)
    }

    fun addAnnouncement(title: String, content: String, isPinned: Boolean) {
        repository.addAnnouncement(title, content, isPinned)
    }

    fun createTeam(name: String, colorHex: String, logo: String = "") {
        repository.createTeam(name, colorHex, logo)
    }

    fun addPlayer(name: String, teamId: String, shirtNumber: Int, position: String) {
        repository.addPlayerToTeam(name, teamId, shirtNumber, position)
    }

    fun deletePlayer(playerId: String) {
        repository.deletePlayer(playerId)
    }

    fun movePlayer(playerId: String, newTeamId: String) {
        repository.movePlayerToTeam(playerId, newTeamId)
    }

    fun deleteTeam(teamId: String) {
        repository.deleteTeam(teamId)
    }

    fun updateTeam(teamId: String, name: String, colorHex: String, logo: String = "") {
        repository.updateTeam(teamId, name, colorHex, logo)
    }

    fun createMatch(homeTeamId: String, awayTeamId: String, date: String, time: String, venue: String) {
        repository.createMatch(homeTeamId, awayTeamId, date, time, venue)
    }

    fun deleteMatch(matchId: String) {
        repository.deleteMatch(matchId)
    }

    fun generateAutomaticSchedule(doubleRound: Boolean = false): Pair<Boolean, String> {
        return repository.generateAutomaticSchedule(doubleRound)
    }

    fun clearAllMatches() {
        repository.clearAllMatches()
    }
}
