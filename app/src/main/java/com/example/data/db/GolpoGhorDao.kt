package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GolpoGhorDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = 'READER'")
    fun getAllReaders(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("UPDATE users SET role = :newRole WHERE id = :userId")
    suspend fun updateUserRole(userId: String, newRole: UserRole)

    @Query("UPDATE users SET isEmailVerified = :isVerified, role = :role, verificationCode = NULL WHERE id = :userId")
    suspend fun verifyUserEmail(userId: String, isVerified: Boolean, role: UserRole)

    @Update
    suspend fun updateUser(user: User)

    // --- Stories ---
    @Query("SELECT * FROM stories WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getActiveStories(): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashStories(): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    fun getStoryByIdFlow(storyId: String): Flow<Story?>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    suspend fun getStoryById(storyId: String): Story?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Update
    suspend fun updateStory(story: Story)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun permanentlyDeleteStory(storyId: String)

    // --- Story Parts ---
    @Query("SELECT * FROM story_parts WHERE storyId = :storyId AND isDeleted = 0 ORDER BY partNumber ASC")
    fun getPartsForStory(storyId: String): Flow<List<StoryPart>>

    @Query("SELECT * FROM story_parts WHERE storyId = :storyId AND isDeleted = 0 ORDER BY partNumber ASC")
    suspend fun getPartsForStorySync(storyId: String): List<StoryPart>

    @Query("SELECT * FROM story_parts WHERE isDeleted = 0 ORDER BY publishedAt DESC")
    fun getAllPublishedParts(): Flow<List<StoryPart>>

    @Query("SELECT * FROM story_parts WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashParts(): Flow<List<StoryPart>>

    @Query("SELECT * FROM story_parts WHERE id = :partId LIMIT 1")
    fun getPartByIdFlow(partId: String): Flow<StoryPart?>

    @Query("SELECT * FROM story_parts WHERE id = :partId LIMIT 1")
    suspend fun getPartById(partId: String): StoryPart?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryPart(part: StoryPart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryParts(parts: List<StoryPart>)

    @Update
    suspend fun updateStoryPart(part: StoryPart)

    @Query("DELETE FROM story_parts WHERE id = :partId")
    suspend fun permanentlyDeletePart(partId: String)

    @Query("DELETE FROM story_parts WHERE storyId = :storyId")
    suspend fun permanentlyDeletePartsByStory(storyId: String)

    @Query("SELECT * FROM story_parts WHERE status = 'SCHEDULED' AND scheduledAt <= :nowTime")
    suspend fun getDueScheduledParts(nowTime: Long): List<StoryPart>

    // --- Reading Progress ---
    @Query("SELECT * FROM reading_progress WHERE userId = :userId")
    fun getUserReadingProgress(userId: String): Flow<List<ReadingProgress>>

    @Query("SELECT * FROM reading_progress WHERE userId = :userId AND storyId = :storyId AND partId = :partId LIMIT 1")
    suspend fun getProgress(userId: String, storyId: String, partId: String): ReadingProgress?

    @Query("SELECT * FROM reading_progress")
    fun getAllReadingProgress(): Flow<List<ReadingProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgress)

    // --- Overrides ---
    @Query("SELECT * FROM user_overrides WHERE userId = :userId AND partId = :partId LIMIT 1")
    suspend fun getAccessOverride(userId: String, partId: String): UserAccessOverride?

    @Query("SELECT * FROM user_overrides WHERE userId = :userId")
    fun getUserOverrides(userId: String): Flow<List<UserAccessOverride>>

    @Query("SELECT * FROM user_overrides")
    fun getAllOverrides(): Flow<List<UserAccessOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: UserAccessOverride)

    @Query("DELETE FROM user_overrides WHERE userId = :userId AND partId = :partId")
    suspend fun deleteOverride(userId: String, partId: String)

    // --- Audit Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: UnlockAuditLog)

    @Query("SELECT * FROM unlock_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<UnlockAuditLog>>

    // --- Reviews & Comments ---
    @Query("SELECT * FROM passage_reviews WHERE storyId = :storyId AND partId = :partId ORDER BY createdAt DESC")
    fun getReviewsForPart(storyId: String, partId: String): Flow<List<PassageReview>>

    @Query("SELECT * FROM passage_reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<PassageReview>>

    @Query("SELECT * FROM passage_reviews WHERE id = :reviewId LIMIT 1")
    suspend fun getReviewById(reviewId: String): PassageReview?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: PassageReview)

    @Update
    suspend fun updateReview(review: PassageReview)

    @Query("DELETE FROM passage_reviews WHERE id = :reviewId")
    suspend fun deleteReview(reviewId: String)

    // --- Review Replies ---
    @Query("SELECT * FROM review_replies WHERE reviewId = :reviewId ORDER BY createdAt ASC")
    fun getRepliesForReview(reviewId: String): Flow<List<ReviewReply>>

    @Query("SELECT * FROM review_replies ORDER BY createdAt ASC")
    fun getAllReplies(): Flow<List<ReviewReply>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: ReviewReply)

    // --- Reactions ---
    @Query("SELECT * FROM reactions WHERE targetId = :targetId")
    fun getReactionsForTarget(targetId: String): Flow<List<Reaction>>

    @Query("SELECT * FROM reactions")
    fun getAllReactions(): Flow<List<Reaction>>

    @Query("SELECT * FROM reactions WHERE userId = :userId AND targetId = :targetId AND emojiType = :emojiType LIMIT 1")
    suspend fun getReaction(userId: String, targetId: String, emojiType: ReactionEmoji): Reaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: Reaction)

    @Delete
    suspend fun deleteReaction(reaction: Reaction)

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookmarksForUser(userId: String): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationItem>)

    @Update
    suspend fun updateNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllNotifications(userId: String)

    // --- User Settings ---
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun getUserSettingsFlow(userId: String): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getUserSettings(userId: String): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserSettings(settings: UserSettings)
}
