package com.phoneclaw.ai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phoneclaw.ai.data.db.dao.*
import com.phoneclaw.ai.data.db.entity.*

@Database(
    entities = [
        SpaceEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        PerChatSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun perChatSettingsDao(): PerChatSettingsDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN thought TEXT")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN webviewUrl TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN isIframe INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN aspectRatio REAL NOT NULL DEFAULT 1.333")
            }
        }
    }
}
