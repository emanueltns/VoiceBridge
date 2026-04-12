package freeapp.voicebridge.ai.domain.usecase

import freeapp.voicebridge.ai.domain.repository.ConversationRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
    }
}
