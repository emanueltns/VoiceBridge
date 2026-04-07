package com.k2fsa.sherpa.onnx.voicebridge.domain.usecase

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Conversation
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import javax.inject.Inject

class ResumeConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(): Conversation? {
        val lastId = conversationRepository.getLastActiveConversationId() ?: return null
        return conversationRepository.getConversation(lastId)
    }
}
