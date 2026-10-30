package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    WRITER,
    READER
}

enum class UserStatus {
    ACTIVE,
    SUSPENDED
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val passwordHash: String = "",
    val displayName: String,
    val role: UserRole,
    val status: UserStatus = UserStatus.ACTIVE,
    val avatarLetter: String = displayName.take(1),
    val avatarUri: String? = null,
    val isEmailVerified: Boolean = false,
    val verificationCode: String? = null
)

enum class PartStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED
}

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val coverResName: String? = null,
    val coverUri: String? = null,
    val authorId: String,
    val authorName: String = "শ্রাবণ মজুমদার",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val sequentialReadingEnabled: Boolean = true,
    val minReadingPercent: Int = 90,
    val requireActiveReadingTime: Boolean = true,
    val minActiveReadingSeconds: Long = 60L,
    val requireEndOfPart: Boolean = true,
    val autoUnlockNextPart: Boolean = true,
    val isCompleted: Boolean = false,
    val isPublished: Boolean = true
)

@Entity(tableName = "story_parts")
data class StoryPart(
    @PrimaryKey val id: String,
    val storyId: String,
    val partNumber: Int,
    val title: String,
    val content: String,
    val status: PartStatus = PartStatus.PUBLISHED,
    val scheduledAt: Long? = null,
    val publishedAt: Long? = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val viewCount: Int = 0
)

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey val id: String, // "${userId}_${storyId}_${partId}"
    val userId: String,
    val storyId: String,
    val partId: String,
    val progressPercent: Int = 0, // 0 to 100
    val isCompleted: Boolean = false,
    val activeSeconds: Long = 0,
    val lastReadAt: Long = System.currentTimeMillis(),
    val lastScrollIndex: Int = 0,
    val lastScrollOffset: Int = 0,
    val reachedEndOfPart: Boolean = false,
    val isManuallyUnlocked: Boolean = false
)

enum class ReviewType {
    PASSAGE,
    WHOLE_PART,
    STORY
}

@Entity(tableName = "passage_reviews")
data class PassageReview(
    @PrimaryKey val id: String,
    val storyId: String,
    val partId: String,
    val userId: String,
    val userName: String,
    val passageText: String? = null,
    val passageAnchor: String? = null,
    val reviewType: ReviewType = ReviewType.PASSAGE,
    val rating: Int? = null, // 1 to 5 for whole part / story
    val comment: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isAnswered: Boolean = false
)

@Entity(tableName = "review_replies")
data class ReviewReply(
    @PrimaryKey val id: String,
    val reviewId: String,
    val userId: String,
    val userName: String,
    val userRole: UserRole,
    val replyText: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReactionEmoji(val symbol: String, val label: String) {
    LOVE("❤️", "ভালোবাসা"),
    HAHA("😂", "হাসি"),
    SAD("😢", "দুঃখ"),
    ANGRY("😠", "রাগ")
}

@Entity(tableName = "reactions")
data class Reaction(
    @PrimaryKey val id: String, // "${userId}_${targetId}_${emojiType}"
    val userId: String,
    val targetType: String, // "PART", "PASSAGE_REVIEW", "PASSAGE"
    val targetId: String, // partId or reviewId
    val passageText: String? = null,
    val emojiType: ReactionEmoji,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val id: String,
    val userId: String,
    val storyId: String,
    val partId: String,
    val passageText: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_overrides")
data class UserAccessOverride(
    @PrimaryKey val id: String, // "${userId}_${partId}"
    val userId: String,
    val partId: String,
    val storyId: String = "",
    val isUnlocked: Boolean = true,
    val unlockedBy: String = "",
    val unlockedAt: Long = System.currentTimeMillis(),
    val isManual: Boolean = true
)

@Entity(tableName = "unlock_audit_logs")
data class UnlockAuditLog(
    @PrimaryKey val id: String,
    val userId: String,
    val storyId: String,
    val partId: String,
    val partNumber: Int = 1,
    val action: String, // "MANUAL_UNLOCK", "REVOKE_UNLOCK", "AUTO_UNLOCK"
    val actorId: String,
    val actorName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

enum class NotificationType {
    REPLY_ON_REVIEW,
    NEW_REVIEW_RECEIVED,
    NEW_PART_PUBLISHED,
    SCHEDULED_PUBLISHED,
    ACCOUNT_STATUS_CHANGED,
    PART_UNLOCKED
}

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val storyId: String? = null,
    val partId: String? = null,
    val reviewId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppTheme(val displayName: String, val description: String) {
    BADAMI_BROWN("বাদামী ব্রাউন", "উষ্ণ বাদামী ও অফ-হোয়াইট (ডিফল্ট)"),
    PURE_WHITE("পিওর হোয়াইট", "শুভ্র নির্মল রূপ"),
    WHITE_AND_RED("হোয়াইট ও লাল", "উজ্জ্বল লাল ও শ্বেত স্নিগ্ধতা"),
    BLUE_SKY("নীল আকাশ", "আকাশী নীল ও প্রশান্তির আভা"),
    FOREST_GREEN("বনানী সবুজ", "প্রাকৃতিক সবুজ ও সতেজ ভাব"),
    LAVENDER_DREAM("ল্যাভেন্ডার স্বপ্ন", "কোমল বেগুনি ও স্বপ্নিল ছোঁয়া"),
    OLIVE_MOSS("অলিভ মস", "স্নিগ্ধ জলপাই ও মস মাখা রূপ (#F5F2E9)")
}

enum class ReadingMood(val displayName: String) {
    LIGHT("লাইট"),
    DARK("ডার্ক"),
    SEPIA("সেপিয়া"),
    WALNUT("ওয়ালনাট")
}

enum class ReadingFont(val displayName: String) {
    NOTO_SERIF("নোটো সেরিফ বাংলা"),
    NOTO_SANS("নোটো সান্স বাংলা"),
    HIND_SILIGURI("হিন্দ শিলিগুড়ি"),
    TIRO_BANGLA("তিরো বাংলা"),
    SYSTEM("ডিভাইস ডিফল্ট")
}

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val userId: String,
    val appTheme: AppTheme = AppTheme.BADAMI_BROWN,
    val readingMood: ReadingMood = ReadingMood.LIGHT,
    val readingFont: ReadingFont = ReadingFont.NOTO_SERIF,
    val fontSizeSp: Float = 19f,
    val lineSpacingMultiplier: Float = 1.6f,
    val isPageTurnMode: Boolean = false,
    val isDarkMode: Boolean = false
)
