package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.READER)

    @TypeConverter
    fun fromUserStatus(value: UserStatus): String = value.name

    @TypeConverter
    fun toUserStatus(value: String): UserStatus = runCatching { UserStatus.valueOf(value) }.getOrDefault(UserStatus.ACTIVE)

    @TypeConverter
    fun fromPartStatus(value: PartStatus): String = value.name

    @TypeConverter
    fun toPartStatus(value: String): PartStatus = runCatching { PartStatus.valueOf(value) }.getOrDefault(PartStatus.PUBLISHED)

    @TypeConverter
    fun fromReviewType(value: ReviewType): String = value.name

    @TypeConverter
    fun toReviewType(value: String): ReviewType = runCatching { ReviewType.valueOf(value) }.getOrDefault(ReviewType.PASSAGE)

    @TypeConverter
    fun fromReactionEmoji(value: ReactionEmoji): String = value.name

    @TypeConverter
    fun toReactionEmoji(value: String): ReactionEmoji = runCatching { ReactionEmoji.valueOf(value) }.getOrDefault(ReactionEmoji.LOVE)

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = runCatching { NotificationType.valueOf(value) }.getOrDefault(NotificationType.NEW_REVIEW_RECEIVED)

    @TypeConverter
    fun fromAppTheme(value: AppTheme): String = value.name

    @TypeConverter
    fun toAppTheme(value: String): AppTheme = runCatching { AppTheme.valueOf(value) }.getOrDefault(AppTheme.BADAMI_BROWN)

    @TypeConverter
    fun fromReadingMood(value: ReadingMood): String = value.name

    @TypeConverter
    fun toReadingMood(value: String): ReadingMood = runCatching { ReadingMood.valueOf(value) }.getOrDefault(ReadingMood.LIGHT)

    @TypeConverter
    fun fromReadingFont(value: ReadingFont): String = value.name

    @TypeConverter
    fun toReadingFont(value: String): ReadingFont = runCatching { ReadingFont.valueOf(value) }.getOrDefault(ReadingFont.NOTO_SERIF)
}
