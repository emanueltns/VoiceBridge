package freeapp.voicebridge.ai.domain.repository

import freeapp.voicebridge.ai.domain.model.Conversation
import freeapp.voicebridge.ai.domain.model.Message
import freeapp.voicebridge.ai.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun getConversation(id: String): Conversation?
    suspend fun createConversation(): Conversation
    suspend fun deleteConversation(id: String)
    suspend fun getLastActiveConversationId(): String?
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun addMessage(conversationId: String, role: MessageRole, text: String): Message
}
