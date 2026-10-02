package com.example.model

enum class UserRole {
    USER,
    ADMIN
}

data class UserAccount(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: UserRole = UserRole.USER,
    val shirtNumber: Int = 10,
    val position: String = "وسط",
    val favoriteTeamId: String = "",
    val favoriteTeamName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val provider: String = "password" // "google", "facebook", "github", "password"
)

enum class LeagueStatus {
    UPCOMING,
    ACTIVE,
    FINISHED
}

data class League(
    val leagueId: String = "",
    val name: String = "",
    val logo: String = "",
    val description: String = "",
    val season: String = "2026-2027",
    val numberOfTeams: Int = 8,
    val matchDurationMinutes: Int = 30, // Custom duration for school/friends tournaments (e.g. 30, 45, 90)
    val startDate: String = "",
    val endDate: String = "",
    val status: LeagueStatus = LeagueStatus.ACTIVE,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Team(
    val teamId: String = "",
    val leagueId: String = "",
    val name: String = "",
    val logo: String = "",
    val colorHex: String = "#10B981",
    val captainId: String = "",
    val captainName: String = "",
    val playerIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class Player(
    val playerId: String = "",
    val userId: String = "",
    val name: String = "",
    val profileImage: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val leagueId: String = "",
    val shirtNumber: Int = 1,
    val position: String = "وسط", // "حارس مرمى", "مدافع", "وسط", "مهاجم"
    val appearances: Int = 0,
    val starts: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val manOfTheMatchCount: Int = 0,
    val averageRating: Double = 0.0,
    val weeklyAwardsCount: Int = 0
)

enum class MatchStatus {
    SCHEDULED,
    LIVE,
    HALFTIME,
    FINISHED,
    CANCELLED
}

enum class EventType {
    GOAL,
    ASSIST,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION,
    MATCH_START,
    HALF_TIME,
    SECOND_HALF,
    FULL_TIME
}

data class MatchEvent(
    val eventId: String = "",
    val matchId: String = "",
    val minute: Int = 0,
    val teamId: String = "",
    val teamName: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val subPlayerId: String = "",
    val subPlayerName: String = "",
    val type: EventType = EventType.GOAL,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FootballMatch(
    val matchId: String = "",
    val leagueId: String = "",
    val homeTeamId: String = "",
    val homeTeamName: String = "",
    val homeTeamLogo: String = "",
    val homeTeamColor: String = "#10B981",
    val awayTeamId: String = "",
    val awayTeamName: String = "",
    val awayTeamLogo: String = "",
    val awayTeamColor: String = "#3B82F6",
    val date: String = "",
    val startTime: String = "",
    val venue: String = "ملعب المدرسة الرئيسي",
    val referee: String = "",
    val round: Int = 1,
    val roundName: String = "الجولة 1",
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val currentMinute: Int = 0,
    val manOfTheMatchPlayerId: String = "",
    val manOfTheMatchPlayerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Lineup(
    val matchId: String = "",
    val teamId: String = "",
    val formation: String = "1-2-1", // 5-a-side, 7-a-side, or 11-a-side
    val startingPlayerIds: List<String> = emptyList(),
    val substitutePlayerIds: List<String> = emptyList()
)

data class PlayerRating(
    val ratingId: String = "",
    val matchId: String = "",
    val playerId: String = "",
    val ratedByUserId: String = "",
    val score: Int = 7,
    val ratingScore: Double = 7.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class WeeklyAward(
    val awardId: String = "",
    val leagueId: String = "",
    val weekNumber: Int = 1,
    val playerId: String = "",
    val playerName: String = "",
    val playerImage: String = "",
    val teamName: String = "",
    val rating: Double = 9.5,
    val goals: Int = 3,
    val assists: Int = 2,
    val reason: String = "أداء استثنائي وهاتريك حاسم",
    val createdAt: Long = System.currentTimeMillis()
)

data class SeasonAward(
    val awardId: String = "",
    val leagueId: String = "",
    val title: String = "", // "بطل الدوري", "هداف الدوري", "أفضل صانع أهداف", "أفضل لاعب", "أفضل حارس"
    val winnerName: String = "",
    val winnerImage: String = "",
    val teamName: String = "",
    val statValue: String = "",
    val prizeInfo: String = "", // e.g. "كأس البطولة التذكاري" (Informational only, no gambling)
    val createdAt: Long = System.currentTimeMillis()
)

data class Announcement(
    val id: String = "",
    val leagueId: String = "",
    val title: String = "",
    val content: String = "",
    val authorName: String = "إدارة دوريسيو",
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

data class LeagueStanding(
    val position: Int = 1,
    val teamId: String = "",
    val teamName: String = "",
    val teamLogo: String = "",
    val teamColor: String = "#10B981",
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDifference: Int = 0,
    val points: Int = 0
)
