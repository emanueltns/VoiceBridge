package freeapp.voicebridge.ai.domain.usecase

import freeapp.voicebridge.ai.domain.model.Message
import freeapp.voicebridge.ai.domain.model.MessageRole
import freeapp.voicebridge.ai.domain.repository.ConversationRepository
import freeapp.voicebridge.ai.domain.repository.VpsRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val vpsRepository: VpsRepository,
) {
    suspend operator fun invoke(conversationId: String, text: String): Result<Message> {
        conversationRepository.addMessage(conversationId, MessageRole.USER, text)

        return vpsRepository.sendMessage(text).map { response ->
            conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, response)
        }
    }
}
