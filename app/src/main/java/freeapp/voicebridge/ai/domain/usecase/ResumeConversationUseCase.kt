package freeapp.voicebridge.ai.domain.usecase

import freeapp.voicebridge.ai.domain.model.Conversation
import freeapp.voicebridge.ai.domain.repository.ConversationRepository
import javax.inject.Inject

class ResumeConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(): Conversation? {
        val lastId = conversationRepository.getLastActiveConversationId() ?: return null
        return conversationRepository.getConversation(lastId)
    }
}
