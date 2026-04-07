package com.k2fsa.sherpa.onnx.voicebridge.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.entity.ConversationEntity
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class VoiceBridgeDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
