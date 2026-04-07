package com.k2fsa.sherpa.onnx.voicebridge.domain.repository

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Conversation
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole
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
