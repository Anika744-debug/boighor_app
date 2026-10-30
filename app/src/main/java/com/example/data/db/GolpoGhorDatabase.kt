package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        User::class,
        Story::class,
        StoryPart::class,
        ReadingProgress::class,
        PassageReview::class,
        ReviewReply::class,
        Reaction::class,
        Bookmark::class,
        UserAccessOverride::class,
        UnlockAuditLog::class,
        NotificationItem::class,
        UserSettings::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GolpoGhorDatabase : RoomDatabase() {
    abstract fun dao(): GolpoGhorDao

    companion object {
        @Volatile
        private var INSTANCE: GolpoGhorDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN isEmailVerified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN verificationCode TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stories ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stories ADD COLUMN isPublished INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN isDarkMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): GolpoGhorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GolpoGhorDatabase::class.java,
                    "golpoghor.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
