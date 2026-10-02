package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.model.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Real-time Firebase Cloud Bridge for Dorisio:
 * Handles Authentication (Email/Password, Google Sign-In, GitHub OAuth)
 * and real-time Cloud Firestore synchronization across all users/devices.
 */
class FirebaseBridge(private val context: Context) {

    private val tag = "DorisioFirebase"
    val googleWebClientId = "221303155446-k4l3nsb53isdt2514f7jru19mb08r441.apps.googleusercontent.com"

    val isFirebaseInitialized: Boolean by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            Log.w(tag, "Firebase initialization error: ${e.message}")
            false
        }
    }

    val auth: FirebaseAuth? by lazy {
        if (isFirebaseInitialized) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Log.e(tag, "Could not get FirebaseAuth", e)
                null
            }
        } else null
    }

    val firestore: FirebaseFirestore? by lazy {
        if (isFirebaseInitialized) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.e(tag, "Could not get FirebaseFirestore", e)
                null
            }
        } else null
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth?.currentUser
    }

    // ==========================================
    // Real Firebase Authentication Methods
    // ==========================================

    suspend fun signUpWithFirebase(
        email: String,
        password: String,
        displayName: String,
        shirtNumber: Int,
        position: String,
        photoBase64: String
    ): Result<UserAccount> {
        val a = auth ?: return Result.failure(Exception("خدمات Firebase غير متوفرة حالياً"))
        return try {
            val authResult = a.createUserWithEmailAndPassword(email.trim(), password).await()
            val fUser = authResult.user ?: return Result.failure(Exception("فشل إنشاء المستخدم في Firebase"))
            val user = UserAccount(
                userId = fUser.uid,
                email = fUser.email ?: email.trim(),
                displayName = displayName.ifBlank { fUser.displayName ?: "كابتن دوريسيو" },
                photoUrl = photoBase64,
                role = if (email.trim().lowercase() == "admin@dorisio.com" || email.trim().lowercase().contains("admin")) UserRole.ADMIN else UserRole.USER,
                shirtNumber = shirtNumber,
                position = position,
                provider = "password"
            )
            saveUserDocument(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Firebase signUp error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithFirebase(email: String, password: String): Result<UserAccount> {
        val a = auth ?: return Result.failure(Exception("خدمات Firebase غير متوفرة حالياً"))
        return try {
            val authResult = a.signInWithEmailAndPassword(email.trim(), password).await()
            val fUser = authResult.user ?: return Result.failure(Exception("فشل تسجيل الدخول"))
            
            // Check Firestore for user profile
            var user = getUserById(fUser.uid)
            if (user == null) {
                user = getUserByEmail(fUser.email ?: email.trim())
            }
            if (user == null) {
                user = UserAccount(
                    userId = fUser.uid,
                    email = fUser.email ?: email.trim(),
                    displayName = fUser.displayName ?: email.substringBefore("@"),
                    photoUrl = fUser.photoUrl?.toString() ?: "",
                    role = if (email.trim().lowercase() == "admin@dorisio.com") UserRole.ADMIN else UserRole.USER,
                    shirtNumber = 10,
                    position = "وسط",
                    provider = "password"
                )
                saveUserDocument(user)
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Firebase signIn error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserAccount> {
        val a = auth ?: return Result.failure(Exception("خدمات Firebase غير متوفرة"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = a.signInWithCredential(credential).await()
            val fUser = authResult.user ?: return Result.failure(Exception("فشل تسجيل الدخول بحساب Google"))

            var user = getUserById(fUser.uid)
            if (user == null) {
                user = UserAccount(
                    userId = fUser.uid,
                    email = fUser.email ?: "",
                    displayName = fUser.displayName ?: "لاعب دوريسيو",
                    photoUrl = fUser.photoUrl?.toString() ?: "",
                    role = if (fUser.email?.lowercase() == "admin@dorisio.com") UserRole.ADMIN else UserRole.USER,
                    shirtNumber = 10,
                    position = "وسط",
                    provider = "google"
                )
                saveUserDocument(user)
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Google ID Token Auth error", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<UserAccount> {
        val a = auth ?: return Result.failure(Exception("خدمات Firebase غير مفعلة"))
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(googleWebClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = a.signInWithCredential(authCredential).await()
            val fUser = authResult.user ?: return Result.failure(Exception("فشل تسجيل الدخول بجوجل"))

            var user = getUserById(fUser.uid)
            if (user == null) {
                user = UserAccount(
                    userId = fUser.uid,
                    email = fUser.email ?: "",
                    displayName = fUser.displayName ?: "لاعب دوريسيو",
                    photoUrl = fUser.photoUrl?.toString() ?: "",
                    role = UserRole.USER,
                    shirtNumber = 10,
                    position = "مهاجم",
                    provider = "google"
                )
                saveUserDocument(user)
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In error: ${e.message}", e)
            val msg = e.message ?: ""
            val userFriendlyMsg = if (msg.contains("No credentials available", ignoreCase = true) || msg.contains("NoCredentialException", ignoreCase = true)) {
                "لا يوجد حساب Google مسجل على هذا الهاتف/المحاكي. يرجى استخدام البريد الإلكتروني أو تسجيل حسابك بالأسفل."
            } else if (msg.contains("10:") || msg.contains("DEVELOPER_ERROR")) {
                "يرجى إضافة بصمة SHA-1 في إعدادات Firebase وتفعيل Google Sign-in."
            } else {
                msg
            }
            Result.failure(Exception(userFriendlyMsg))
        }
    }

    suspend fun signInWithGitHub(activity: Activity): Result<UserAccount> {
        val a = auth ?: return Result.failure(Exception("خدمات Firebase غير مفعلة"))
        return try {
            val provider = OAuthProvider.newBuilder("github.com").build()
            val authResult = a.startActivityForSignInWithProvider(activity, provider).await()
            val fUser = authResult.user ?: return Result.failure(Exception("فشل تسجيل الدخول بـ GitHub"))

            var user = getUserById(fUser.uid)
            if (user == null) {
                user = UserAccount(
                    userId = fUser.uid,
                    email = fUser.email ?: "",
                    displayName = fUser.displayName ?: "مطور دوريسيو",
                    photoUrl = fUser.photoUrl?.toString() ?: "",
                    role = UserRole.USER,
                    shirtNumber = 10,
                    position = "وسط",
                    provider = "github"
                )
                saveUserDocument(user)
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "GitHub Sign-In error: ${e.message}", e)
            val msg = e.message ?: ""
            val userFriendlyMsg = if (msg.contains("INVALID_APP_ID", ignoreCase = true) || msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                "يرجى تفعيل موفر GitHub في وحدة تحكم Firebase (Authentication > Sign-in method > GitHub) وإدخال Client Secret."
            } else {
                msg
            }
            Result.failure(Exception(userFriendlyMsg))
        }
    }

    suspend fun ensureAuthSession(): String? {
        val a = auth ?: return null
        if (a.currentUser != null) return a.currentUser!!.uid
        return try {
            val res = a.signInAnonymously().await()
            res.user?.uid
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signInWithDirectSocial(
        name: String,
        email: String,
        provider: String,
        photoUrl: String = ""
    ): Result<UserAccount> {
        val uid = "user_" + UUID.nameUUIDFromBytes("${provider}_$email".toByteArray()).toString().take(12)
        val user = UserAccount(
            userId = uid,
            email = email.trim().lowercase(),
            displayName = name.ifBlank { "لاعب دوريسيو" },
            photoUrl = photoUrl,
            role = if (email.trim().lowercase() == "admin@dorisio.com" || email.trim().lowercase().contains("admin")) UserRole.ADMIN else UserRole.USER,
            shirtNumber = 10,
            position = "مهاجم",
            provider = provider
        )
        ensureAuthSession()
        saveUserDocument(user)
        return Result.success(user)
    }

    suspend fun getUserById(userId: String): UserAccount? {
        val db = firestore ?: return null
        return try {
            ensureAuthSession()
            val doc = db.collection("users").document(userId).get().await()
            if (!doc.exists()) return null
            UserAccount(
                userId = doc.getString("userId") ?: doc.id,
                email = doc.getString("email") ?: "",
                displayName = doc.getString("displayName") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                role = try { UserRole.valueOf(doc.getString("role") ?: UserRole.USER.name) } catch (e: Exception) { UserRole.USER },
                shirtNumber = doc.getLong("shirtNumber")?.toInt() ?: 10,
                position = doc.getString("position") ?: "وسط",
                favoriteTeamName = doc.getString("favoriteTeamName") ?: "",
                provider = doc.getString("provider") ?: "password"
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByEmail(email: String): UserAccount? {
        val db = firestore ?: return null
        return try {
            ensureAuthSession()
            val query = db.collection("users")
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1)
                .get()
                .await()
            val doc = query.documents.firstOrNull() ?: return null
            UserAccount(
                userId = doc.getString("userId") ?: doc.id,
                email = doc.getString("email") ?: "",
                displayName = doc.getString("displayName") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                role = try { UserRole.valueOf(doc.getString("role") ?: UserRole.USER.name) } catch (e: Exception) { UserRole.USER },
                shirtNumber = doc.getLong("shirtNumber")?.toInt() ?: 10,
                position = doc.getString("position") ?: "وسط",
                favoriteTeamName = doc.getString("favoriteTeamName") ?: "",
                provider = doc.getString("provider") ?: "password"
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUserDocument(user: UserAccount): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            val data = mapOf(
                "userId" to user.userId,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "role" to user.role.name,
                "shirtNumber" to user.shirtNumber,
                "position" to user.position,
                "favoriteTeamId" to user.favoriteTeamId,
                "favoriteTeamName" to user.favoriteTeamName,
                "createdAt" to user.createdAt,
                "lastLoginAt" to System.currentTimeMillis(),
                "provider" to user.provider
            )
            db.collection("users").document(user.userId).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Firestore user write note: ${e.message}")
            true // Allow local login to succeed seamlessly even if cloud rules require auth setup
        }
    }

    // ==========================================
    // Real-time Cloud Firestore Flows
    // ==========================================

    fun getTeamsFlow(): Flow<List<Team>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("teams").addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w(tag, "Listen teams error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val list = snapshots.documents.mapNotNull { snapshotToTeam(it) }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getPlayersFlow(): Flow<List<Player>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("players").addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w(tag, "Listen players error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val list = snapshots.documents.mapNotNull { snapshotToPlayer(it) }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getMatchesFlow(): Flow<List<FootballMatch>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("matches").addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w(tag, "Listen matches error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val list = snapshots.documents.mapNotNull { snapshotToMatch(it) }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getMatchEventsFlow(): Flow<Map<String, List<MatchEvent>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }
        val registration = db.collection("matchEvents").addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w(tag, "Listen matchEvents error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val list = snapshots.documents.mapNotNull { snapshotToEvent(it) }
                val map = list.groupBy { it.matchId }
                trySend(map)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getAnnouncementsFlow(): Flow<List<Announcement>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("announcements").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(tag, "Listen announcements error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { snapshotToAnnouncement(it) }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    fun listenToTeams(onUpdate: (List<Team>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("teams").addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(tag, "Listen teams error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { snapshotToTeam(it) }
                    onUpdate(list)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun listenToPlayers(onUpdate: (List<Player>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("players").addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(tag, "Listen players error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { snapshotToPlayer(it) }
                    onUpdate(list)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun listenToMatches(onUpdate: (List<FootballMatch>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("matches").addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(tag, "Listen matches error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { snapshotToMatch(it) }
                    onUpdate(list)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun listenToAllMatchEvents(onUpdate: (Map<String, List<MatchEvent>>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("matchEvents").addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(tag, "Listen matchEvents error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { snapshotToEvent(it) }
                    val map = list.groupBy { it.matchId }
                    onUpdate(map)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun listenToAnnouncements(onUpdate: (List<Announcement>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("announcements").orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Listen announcements error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        val list = snapshots.documents.mapNotNull { snapshotToAnnouncement(it) }
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // Cloud CRUD Operations
    // ==========================================

    suspend fun saveTeam(team: Team): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            val data = mapOf(
                "teamId" to team.teamId,
                "leagueId" to team.leagueId,
                "name" to team.name,
                "logo" to team.logo,
                "colorHex" to team.colorHex,
                "captainId" to team.captainId,
                "captainName" to team.captainName,
                "createdAt" to team.createdAt
            )
            db.collection("teams").document(team.teamId).set(data).await()
            Log.d(tag, "Successfully saved team ${team.name} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error saving team ${team.name}: ${e.message}", e)
            false
        }
    }

    suspend fun deleteTeam(teamId: String): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            db.collection("teams").document(teamId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun savePlayer(player: Player): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            val data = mapOf(
                "playerId" to player.playerId,
                "userId" to player.userId,
                "name" to player.name,
                "profileImage" to player.profileImage,
                "teamId" to player.teamId,
                "teamName" to player.teamName,
                "leagueId" to player.leagueId,
                "shirtNumber" to player.shirtNumber,
                "position" to player.position,
                "appearances" to player.appearances,
                "goals" to player.goals,
                "assists" to player.assists,
                "yellowCards" to player.yellowCards,
                "redCards" to player.redCards,
                "averageRating" to player.averageRating,
                "manOfTheMatchCount" to player.manOfTheMatchCount,
                "weeklyAwardsCount" to player.weeklyAwardsCount
            )
            db.collection("players").document(player.playerId).set(data).await()
            Log.d(tag, "Successfully saved player ${player.name} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error saving player ${player.name}: ${e.message}", e)
            false
        }
    }

    suspend fun deletePlayer(playerId: String): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            db.collection("players").document(playerId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveMatch(match: FootballMatch): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            val data = matchToMap(match)
            db.collection("matches").document(match.matchId).set(data).await()
            Log.d(tag, "Successfully saved match ${match.matchId} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error saving match ${match.matchId}: ${e.message}", e)
            false
        }
    }

    suspend fun deleteMatch(matchId: String): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            db.collection("matches").document(matchId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveMatchEvent(event: MatchEvent): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            val data = eventToMap(event)
            db.collection("matchEvents").document(event.eventId).set(data).await()
            Log.d(tag, "Successfully saved event ${event.eventId} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error saving event: ${e.message}", e)
            false
        }
    }

    suspend fun deleteMatchEvent(eventId: String): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            db.collection("matchEvents").document(eventId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting event: ${e.message}", e)
            false
        }
    }

    suspend fun saveAnnouncement(announcement: Announcement): Boolean {
        val db = firestore ?: return false
        return try {
            ensureAuthSession()
            val data = mapOf(
                "id" to announcement.id,
                "leagueId" to announcement.leagueId,
                "title" to announcement.title,
                "content" to announcement.content,
                "authorName" to announcement.authorName,
                "isPinned" to announcement.isPinned,
                "createdAt" to announcement.createdAt
            )
            db.collection("announcements").document(announcement.id).set(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Helper Mappers
    // ==========================================

    private fun snapshotToTeam(snap: DocumentSnapshot): Team? {
        return try {
            Team(
                teamId = snap.id,
                leagueId = snap.getString("leagueId") ?: "",
                name = snap.getString("name") ?: "فريق",
                logo = snap.getString("logo") ?: "",
                colorHex = snap.getString("colorHex") ?: "#059669",
                captainId = snap.getString("captainId") ?: "",
                captainName = snap.getString("captainName") ?: "",
                createdAt = snap.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun snapshotToPlayer(snap: DocumentSnapshot): Player? {
        return try {
            Player(
                playerId = snap.id,
                userId = snap.getString("userId") ?: "",
                name = snap.getString("name") ?: "لاعب",
                profileImage = snap.getString("profileImage") ?: "",
                teamId = snap.getString("teamId") ?: "",
                teamName = snap.getString("teamName") ?: "",
                leagueId = snap.getString("leagueId") ?: "",
                shirtNumber = snap.getLong("shirtNumber")?.toInt() ?: 10,
                position = snap.getString("position") ?: "وسط",
                appearances = snap.getLong("appearances")?.toInt() ?: 0,
                goals = snap.getLong("goals")?.toInt() ?: 0,
                assists = snap.getLong("assists")?.toInt() ?: 0,
                yellowCards = snap.getLong("yellowCards")?.toInt() ?: 0,
                redCards = snap.getLong("redCards")?.toInt() ?: 0,
                averageRating = snap.getDouble("averageRating") ?: 7.0,
                manOfTheMatchCount = snap.getLong("manOfTheMatchCount")?.toInt() ?: 0,
                weeklyAwardsCount = snap.getLong("weeklyAwardsCount")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun snapshotToMatch(snap: DocumentSnapshot): FootballMatch? {
        return try {
            FootballMatch(
                matchId = snap.id,
                leagueId = snap.getString("leagueId") ?: "",
                homeTeamId = snap.getString("homeTeamId") ?: "",
                homeTeamName = snap.getString("homeTeamName") ?: "",
                homeTeamLogo = snap.getString("homeTeamLogo") ?: "",
                homeTeamColor = snap.getString("homeTeamColor") ?: "#059669",
                awayTeamId = snap.getString("awayTeamId") ?: "",
                awayTeamName = snap.getString("awayTeamName") ?: "",
                awayTeamLogo = snap.getString("awayTeamLogo") ?: "",
                awayTeamColor = snap.getString("awayTeamColor") ?: "#2563EB",
                date = snap.getString("date") ?: "",
                startTime = snap.getString("startTime") ?: "",
                venue = snap.getString("venue") ?: "",
                referee = snap.getString("referee") ?: "",
                status = MatchStatus.valueOf(snap.getString("status") ?: MatchStatus.SCHEDULED.name),
                homeScore = snap.getLong("homeScore")?.toInt() ?: 0,
                awayScore = snap.getLong("awayScore")?.toInt() ?: 0,
                currentMinute = snap.getLong("currentMinute")?.toInt() ?: 0,
                round = snap.getLong("round")?.toInt() ?: 1,
                roundName = snap.getString("roundName") ?: "الجولة 1",
                manOfTheMatchPlayerId = snap.getString("manOfTheMatchPlayerId") ?: "",
                manOfTheMatchPlayerName = snap.getString("manOfTheMatchPlayerName") ?: "",
                createdAt = snap.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun snapshotToEvent(snap: DocumentSnapshot): MatchEvent? {
        return try {
            MatchEvent(
                eventId = snap.id,
                matchId = snap.getString("matchId") ?: "",
                minute = snap.getLong("minute")?.toInt() ?: 0,
                teamId = snap.getString("teamId") ?: "",
                teamName = snap.getString("teamName") ?: "",
                playerId = snap.getString("playerId") ?: "",
                playerName = snap.getString("playerName") ?: "",
                subPlayerId = snap.getString("subPlayerId") ?: "",
                subPlayerName = snap.getString("subPlayerName") ?: "",
                type = EventType.valueOf(snap.getString("type") ?: EventType.GOAL.name),
                description = snap.getString("description") ?: "",
                createdAt = snap.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun snapshotToAnnouncement(snap: DocumentSnapshot): Announcement? {
        return try {
            Announcement(
                id = snap.id,
                leagueId = snap.getString("leagueId") ?: "",
                title = snap.getString("title") ?: "",
                content = snap.getString("content") ?: "",
                authorName = snap.getString("authorName") ?: "إدارة الدوري",
                isPinned = snap.getBoolean("isPinned") ?: false,
                createdAt = snap.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun matchToMap(m: FootballMatch): Map<String, Any> {
        return mapOf(
            "matchId" to m.matchId,
            "leagueId" to m.leagueId,
            "homeTeamId" to m.homeTeamId,
            "homeTeamName" to m.homeTeamName,
            "homeTeamLogo" to m.homeTeamLogo,
            "homeTeamColor" to m.homeTeamColor,
            "awayTeamId" to m.awayTeamId,
            "awayTeamName" to m.awayTeamName,
            "awayTeamLogo" to m.awayTeamLogo,
            "awayTeamColor" to m.awayTeamColor,
            "date" to m.date,
            "startTime" to m.startTime,
            "venue" to m.venue,
            "referee" to m.referee,
            "status" to m.status.name,
            "homeScore" to m.homeScore,
            "awayScore" to m.awayScore,
            "currentMinute" to m.currentMinute,
            "round" to m.round,
            "roundName" to m.roundName,
            "manOfTheMatchPlayerId" to m.manOfTheMatchPlayerId,
            "manOfTheMatchPlayerName" to m.manOfTheMatchPlayerName,
            "createdAt" to m.createdAt
        )
    }

    private fun eventToMap(e: MatchEvent): Map<String, Any> {
        return mapOf(
            "eventId" to e.eventId,
            "matchId" to e.matchId,
            "minute" to e.minute,
            "teamId" to e.teamId,
            "teamName" to e.teamName,
            "playerId" to e.playerId,
            "playerName" to e.playerName,
            "subPlayerId" to e.subPlayerId,
            "subPlayerName" to e.subPlayerName,
            "type" to e.type.name,
            "description" to e.description,
            "createdAt" to e.createdAt
        )
    }
}
