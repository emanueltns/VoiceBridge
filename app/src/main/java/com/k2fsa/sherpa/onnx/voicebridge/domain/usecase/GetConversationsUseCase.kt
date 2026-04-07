package com.k2fsa.sherpa.onnx.voicebridge.domain.usecase

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Conversation
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return conversationRepository.getConversations()
    }
}
