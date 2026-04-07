package com.k2fsa.sherpa.onnx.voicebridge.data.repository

import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.ConversationDao
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.MessageDao
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.entity.ConversationEntity
import com.k2fsa.sherpa.onnx.voicebridge.data.local.db.entity.MessageEntity
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Conversation
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) : ConversationRepository {

    override fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversation(id: String): Conversation? {
        return conversationDao.getById(id)?.toDomain()
    }

    override suspend fun createConversation(): Conversation {
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = "New Conversation",
            createdAt = now,
            updatedAt = now,
        )
        conversationDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.delete(id)
    }

    override suspend fun getLastActiveConversationId(): String? {
        return conversationDao.getLastActiveId()
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getByConversationId(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMessage(
        conversationId: String,
        role: MessageRole,
        text: String,
    ): Message {
        val now = System.currentTimeMillis()
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role.name,
            text = text,
            timestamp = now,
        )
        messageDao.insert(entity)
        conversationDao.updateTimestamp(conversationId, now)

        // Auto-title from first user message
        if (role == MessageRole.USER) {
            val count = messageDao.getMessageCount(conversationId)
            if (count == 1) {
                val title = text.take(50).let { if (text.length > 50) "$it..." else it }
                conversationDao.updateTitleAndTimestamp(conversationId, title, now)
            }
        }

        return entity.toDomain()
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.valueOf(role),
        text = text,
        timestamp = timestamp,
    )
}
