package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.GolpoGhorDatabase
import com.example.data.model.*
import com.example.data.repository.GolpoGhorRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Home : ScreenDestination()
    object Library : ScreenDestination()
    object Stories : ScreenDestination()
    object Reviews : ScreenDestination()
    data class StoryDetail(val storyId: String) : ScreenDestination()
    data class Reading(
        val storyId: String,
        val partId: String,
        val highlightPassage: String? = null,
        val highlightAnchor: String? = null
    ) : ScreenDestination()
    object WriterDashboard : ScreenDestination()
    object ReviewsManagement : ScreenDestination()
    object Notifications : ScreenDestination()
    object Settings : ScreenDestination()
    object Login : ScreenDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository: GolpoGhorRepository
    val firestoreManager: com.example.data.repository.FirestoreManager = com.example.data.repository.FirestoreManager(application)

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.BADAMI_BROWN)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _readingMood = MutableStateFlow(ReadingMood.LIGHT)
    val readingMood: StateFlow<ReadingMood> = _readingMood.asStateFlow()

    private val _readingFont = MutableStateFlow(ReadingFont.NOTO_SERIF)
    val readingFont: StateFlow<ReadingFont> = _readingFont.asStateFlow()

    private val _fontSize = MutableStateFlow(19f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _lineSpacing = MutableStateFlow(1.6f)
    val lineSpacing: StateFlow<Float> = _lineSpacing.asStateFlow()

    private val _isPageTurnMode = MutableStateFlow(false)
    val isPageTurnMode: StateFlow<Boolean> = _isPageTurnMode.asStateFlow()

    val allStories = MutableStateFlow<List<Story>>(emptyList())
    val allParts = MutableStateFlow<List<StoryPart>>(emptyList())
    val allUsers = MutableStateFlow<List<User>>(emptyList())
    val allReaders = MutableStateFlow<List<User>>(emptyList())
    val allReviews = MutableStateFlow<List<PassageReview>>(emptyList())
    val allReplies = MutableStateFlow<List<ReviewReply>>(emptyList())
    val allReactions = MutableStateFlow<List<Reaction>>(emptyList())
    val allReadingProgress = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val userBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val userOverrides = MutableStateFlow<List<UserAccessOverride>>(emptyList())
    val allAuditLogs = MutableStateFlow<List<UnlockAuditLog>>(emptyList())
    private val _offlineSavedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val offlineSavedStoryIds: StateFlow<Set<String>> = _offlineSavedStoryIds.asStateFlow()

    init {
        val db = GolpoGhorDatabase.getDatabase(application)
        repository = GolpoGhorRepository(db.dao(), application)

        val prefs = application.getSharedPreferences("golpoghor_offline", android.content.Context.MODE_PRIVATE)
        _offlineSavedStoryIds.value = prefs.getStringSet("saved_stories", emptySet()) ?: emptySet()

        val themePrefs = application.getSharedPreferences("golpoghor_theme_prefs", android.content.Context.MODE_PRIVATE)
        _isDarkMode.value = themePrefs.getBoolean("is_dark_mode", false)
        val savedTheme = themePrefs.getString("app_theme", null)
        if (savedTheme != null) {
            try {
                _appTheme.value = AppTheme.valueOf(savedTheme)
            } catch (_: Exception) {}
        }

        // Automatic Cloud Sync: Restore existing stories, parts and reviews from Cloud Firestore
        refreshContent()

        val currentFbUser = try { Firebase.auth.currentUser } catch (_: Exception) { null }
        if (currentFbUser != null) {
            viewModelScope.launch {
                try {
                    val user = firestoreManager.syncFirebaseUser(currentFbUser)
                    repository.dao.insertUser(user)
                    _currentUser.value = user
                    refreshContent()
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Auto session restore error: ${e.message}")
                }
            }
        }

        // Continuous server/app-level publisher checking due scheduled parts every 10 seconds
        viewModelScope.launch {
            while (true) {
                try {
                    repository.checkScheduledPublications()
                } catch (e: Exception) {
                    // Ignore transient error
                }
                kotlinx.coroutines.delay(10_000L)
            }
        }

        // Observe reactive streams
        viewModelScope.launch {
            repository.activeStories.collect { allStories.value = it }
        }
        viewModelScope.launch {
            repository.allPublishedParts.collect { allParts.value = it }
        }
        viewModelScope.launch {
            repository.allUsers.collect { allUsers.value = it }
        }
        viewModelScope.launch {
            repository.allReaders.collect { allReaders.value = it }
        }
        viewModelScope.launch {
            repository.allReviews.collect { allReviews.value = it }
        }
        viewModelScope.launch {
            repository.allReplies.collect { allReplies.value = it }
        }
        viewModelScope.launch {
            repository.allReactions.collect { allReactions.value = it }
        }
        viewModelScope.launch {
            repository.allReadingProgress.collect { allReadingProgress.value = it }
        }
        viewModelScope.launch {
            repository.allAuditLogs.collect { allAuditLogs.value = it }
        }
        viewModelScope.launch {
            repository.allOverrides.collect { allOverridesList ->
                // If current user is privileged, show all overrides; otherwise reader overrides are updated on login
                if (_currentUser.value?.role == UserRole.ADMIN || _currentUser.value?.role == UserRole.WRITER) {
                    userOverrides.value = allOverridesList
                }
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    suspend fun registerUser(email: String, pass: String, displayName: String): Result<User> {
        val res = repository.register(email, pass, displayName)
        if (res.isSuccess) {
            val user = res.getOrThrow()
            // If regular reader, login directly; if admin email, wait for verification or let verification do login
            val isAdminEmail = user.username.equals(GolpoGhorRepository.ADMIN_EMAIL, ignoreCase = true)
            if (!isAdminEmail) {
                loginUser(user)
            }
        }
        return res
    }

    suspend fun verifyEmailIdentity(emailOrUserId: String, code: String): Result<User> {
        val res = repository.verifyEmailIdentity(emailOrUserId, code)
        if (res.isSuccess) {
            val user = res.getOrThrow()
            loginUser(user)
        }
        return res
    }

    suspend fun resendVerificationCode(emailOrUserId: String): Result<String> {
        return repository.resendVerificationCode(emailOrUserId)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("সঠিক ও বৈধ ইমেইল ঠিকানা প্রদান করুন।"))
        }
        return try {
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val user = repository.dao.getUserByEmail(cleanEmail)
            if (user != null) {
                repository.requestPasswordResetOtp(cleanEmail)
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun requestPasswordResetOtp(email: String): Result<String> {
        return repository.requestPasswordResetOtp(email)
    }

    suspend fun resetPasswordWithOtp(email: String, otp: String, newPass: String): Result<User> {
        return repository.resetPasswordWithOtp(email, otp, newPass)
    }

    suspend fun loginWithCredentials(email: String, pass: String): Result<User> {
        val res = repository.login(email, pass)
        if (res.isSuccess) {
            val user = res.getOrThrow()
            loginUser(user)
        }
        return res
    }

    suspend fun loginWithGoogleUser(firebaseUser: com.google.firebase.auth.FirebaseUser): Result<User> {
        return try {
            val user = firestoreManager.syncGoogleUser(firebaseUser)
            repository.dao.insertUser(user)
            loginUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithDirectGoogleAccount(email: String, name: String? = null): Result<User> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("সঠিক গুগল ইমেইল দিন।"))
        }
        val isOwner = cleanEmail.equals("mdnajrulislam771@gmail.com", ignoreCase = true) ||
                      cleanEmail.equals("mdnajulislam771@gmail.com", ignoreCase = true)
        val role = if (isOwner) UserRole.ADMIN else UserRole.READER
        val finalName = name?.ifBlank { null } ?: cleanEmail.substringBefore("@")
        val userId = "google_" + cleanEmail.replace("@", "_").replace(".", "_")

        val existing = repository.dao.getUserByEmail(cleanEmail)
        val user = if (existing != null) {
            existing.copy(
                displayName = if (existing.displayName.isNotBlank()) existing.displayName else finalName,
                role = if (isOwner) UserRole.ADMIN else existing.role,
                isEmailVerified = true
            )
        } else {
            User(
                id = userId,
                username = cleanEmail,
                passwordHash = "",
                displayName = finalName,
                role = role,
                status = UserStatus.ACTIVE,
                avatarLetter = finalName.take(1),
                isEmailVerified = true,
                verificationCode = null
            )
        }
        repository.dao.insertUser(user)
        loginUser(user)
        return Result.success(user)
    }

    suspend fun updateUserRole(targetUserId: String, newRole: UserRole): Result<Unit> {
        val adminId = _currentUser.value?.id ?: return Result.failure(SecurityException("অ্যাডমিন লগইন থাকা আবশ্যক।"))
        val res = repository.updateUserRole(adminId, targetUserId, newRole)
        if (res.isSuccess) {
            viewModelScope.launch {
                firestoreManager.updateUserRole(targetUserId, newRole)
            }
        }
        return res
    }

    suspend fun updateUserStatus(targetUserId: String, newStatus: UserStatus): Result<Unit> {
        val adminId = _currentUser.value?.id ?: return Result.failure(SecurityException("অ্যাডমিন লগইন থাকা আবশ্যক।"))
        val res = repository.updateUserStatus(adminId, targetUserId, newStatus)
        return res
    }

    fun refreshContent(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                firestoreManager.syncAllFromCloud(
                    onStories = { stories ->
                        stories.forEach { repository.dao.insertStory(it) }
                    },
                    onParts = { parts ->
                        repository.dao.insertStoryParts(parts)
                    },
                    onReviews = { reviews ->
                        reviews.forEach { repository.dao.insertReview(it) }
                    }
                )
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Content refresh failed: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    fun loginUser(user: User) {
        _currentUser.value = user
        refreshContent()
        viewModelScope.launch {
            // Load user settings
            repository.getUserSettings(user.id).collect { settings ->
                if (settings != null) {
                    _appTheme.value = settings.appTheme
                    _isDarkMode.value = settings.isDarkMode
                    _readingMood.value = settings.readingMood
                    _readingFont.value = settings.readingFont
                    _fontSize.value = settings.fontSizeSp
                    _lineSpacing.value = settings.lineSpacingMultiplier
                    _isPageTurnMode.value = settings.isPageTurnMode
                }
            }
        }
        // Observe user notifications & bookmarks
        viewModelScope.launch {
            repository.getNotificationsForUser(user.id).collect {
                notifications.value = it
            }
        }
        viewModelScope.launch {
            repository.getBookmarksForUser(user.id).collect {
                userBookmarks.value = it
            }
        }
        viewModelScope.launch {
            repository.getUserOverrides(user.id).collect {
                userOverrides.value = it
            }
        }
    }

    fun logout() {
        try {
            Firebase.auth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
        _currentScreen.value = ScreenDestination.Home
    }

    fun updateTheme(newTheme: AppTheme) {
        _appTheme.value = newTheme
        val themePrefs = getApplication<android.app.Application>().getSharedPreferences("golpoghor_theme_prefs", android.content.Context.MODE_PRIVATE)
        themePrefs.edit().putString("app_theme", newTheme.name).apply()
        saveCurrentSettings()
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        val themePrefs = getApplication<android.app.Application>().getSharedPreferences("golpoghor_theme_prefs", android.content.Context.MODE_PRIVATE)
        themePrefs.edit().putBoolean("is_dark_mode", isDark).apply()
        saveCurrentSettings()
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun updateMood(newMood: ReadingMood) {
        _readingMood.value = newMood
        saveCurrentSettings()
    }

    fun updateFont(newFont: ReadingFont) {
        _readingFont.value = newFont
        saveCurrentSettings()
    }

    fun updateFontSize(newSize: Float) {
        _fontSize.value = newSize.coerceIn(14f, 36f)
        saveCurrentSettings()
    }

    fun updateLineSpacing(newSpacing: Float) {
        _lineSpacing.value = newSpacing
        saveCurrentSettings()
    }

    fun togglePageTurnMode() {
        _isPageTurnMode.value = !_isPageTurnMode.value
        saveCurrentSettings()
    }

    private fun saveCurrentSettings() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserSettings(
                UserSettings(
                    userId = user.id,
                    appTheme = _appTheme.value,
                    isDarkMode = _isDarkMode.value,
                    readingMood = _readingMood.value,
                    readingFont = _readingFont.value,
                    fontSizeSp = _fontSize.value,
                    lineSpacingMultiplier = _lineSpacing.value,
                    isPageTurnMode = _isPageTurnMode.value
                )
            )
        }
    }

    fun markAllNotificationsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(user.id)
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun deleteAllNotifications() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteAllNotifications(user.id)
        }
    }

    suspend fun setManualUnlock(
        userId: String,
        storyId: String,
        partId: String,
        partNumber: Int,
        unlock: Boolean,
        notes: String? = null
    ) {
        val actor = _currentUser.value
        repository.setManualAccessOverride(
            userId = userId,
            storyId = storyId,
            partId = partId,
            partNumber = partNumber,
            unlock = unlock,
            actorId = actor?.id ?: "WRITER",
            actorName = actor?.displayName ?: "লেখক",
            notes = notes
        )
    }

    suspend fun updateStorySettings(
        storyId: String,
        sequentialReadingEnabled: Boolean,
        minReadingPercent: Int,
        requireActiveReadingTime: Boolean,
        minActiveReadingSeconds: Long,
        requireEndOfPart: Boolean,
        autoUnlockNextPart: Boolean
    ) {
        repository.updateStorySettings(
            storyId = storyId,
            sequentialReadingEnabled = sequentialReadingEnabled,
            minReadingPercent = minReadingPercent,
            requireActiveReadingTime = requireActiveReadingTime,
            minActiveReadingSeconds = minActiveReadingSeconds,
            requireEndOfPart = requireEndOfPart,
            autoUnlockNextPart = autoUnlockNextPart
        )
    }

    fun updateUserProfile(
        displayName: String,
        avatarUri: String?,
        onComplete: (Result<User>) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.updateUserProfile(user.id, displayName, avatarUri)
            result.onSuccess { updated ->
                _currentUser.value = updated
            }
            onComplete(result)
        }
    }

    fun updateStoryDetails(
        storyId: String,
        title: String,
        description: String,
        coverResName: String?,
        coverUri: String?,
        onComplete: (Result<Story>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateStoryDetails(storyId, title, description, coverResName, coverUri)
            onComplete(result)
        }
    }

    fun checkScheduledPublications() {
        viewModelScope.launch {
            repository.checkScheduledPublications()
        }
    }

    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    fun updateReview(
        reviewId: String,
        newComment: String,
        newRating: Int?,
        onComplete: (Result<PassageReview>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val existing = repository.dao.getReviewById(reviewId)
                if (existing != null) {
                    val updated = existing.copy(
                        comment = newComment,
                        rating = newRating
                    )
                    repository.dao.updateReview(updated)
                    onComplete(Result.success(updated))
                } else {
                    onComplete(Result.failure(IllegalArgumentException("রিভিউ পাওয়া যায়নি।")))
                }
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    fun deleteReview(reviewId: String, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.dao.deleteReview(reviewId)
                onComplete(Result.success(Unit))
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    fun toggleStoryCompletion(storyId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val story = repository.dao.getStoryById(storyId)
            if (story != null) {
                val newStatus = !story.isCompleted
                val updated = story.copy(isCompleted = newStatus, updatedAt = System.currentTimeMillis())
                repository.dao.updateStory(updated)
                firestoreManager.createOrUpdateStory(updated)
                onComplete(newStatus)
            }
        }
    }

    fun toggleStoryPublished(storyId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val story = repository.dao.getStoryById(storyId)
            if (story != null) {
                val newPublished = !story.isPublished
                val updated = story.copy(isPublished = newPublished, updatedAt = System.currentTimeMillis())
                repository.dao.updateStory(updated)
                firestoreManager.createOrUpdateStory(updated)
                onComplete(newPublished)
            }
        }
    }

    fun deleteStoryPermanently(storyId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.permanentlyDeleteStory(storyId)
            onComplete()
        }
    }

    fun toggleBookmark(storyId: String, partId: String, onComplete: (Boolean) -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val existing = userBookmarks.value.find { it.userId == user.id && it.storyId == storyId && it.partId == partId }
            if (existing != null) {
                repository.deleteBookmark(existing.id)
                onComplete(false)
            } else {
                repository.addBookmark(user.id, storyId, partId, null, null)
                onComplete(true)
            }
        }
    }

    fun saveStoryOffline(storyId: String) {
        val current = _offlineSavedStoryIds.value
        _offlineSavedStoryIds.value = current + storyId
        val prefs = getApplication<Application>().getSharedPreferences("golpoghor_offline", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("saved_stories", _offlineSavedStoryIds.value).apply()
    }

    fun removeStoryOffline(storyId: String) {
        val current = _offlineSavedStoryIds.value
        _offlineSavedStoryIds.value = current - storyId
        val prefs = getApplication<Application>().getSharedPreferences("golpoghor_offline", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("saved_stories", _offlineSavedStoryIds.value).apply()
    }
}
