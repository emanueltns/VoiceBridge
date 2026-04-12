package freeapp.voicebridge.ai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import freeapp.voicebridge.ai.data.local.db.entity.ConversationEntity
import freeapp.voicebridge.ai.data.local.db.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class VoiceBridgeDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
