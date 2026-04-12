package freeapp.voicebridge.ai.domain.usecase

import freeapp.voicebridge.ai.domain.model.Conversation
import freeapp.voicebridge.ai.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return conversationRepository.getConversations()
    }
}
