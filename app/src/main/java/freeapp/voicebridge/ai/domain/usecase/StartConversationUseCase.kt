package freeapp.voicebridge.ai.domain.usecase

import freeapp.voicebridge.ai.domain.model.Conversation
import freeapp.voicebridge.ai.domain.repository.ConversationRepository
import freeapp.voicebridge.ai.domain.repository.SettingsRepository
import freeapp.voicebridge.ai.domain.repository.VpsRepository
import javax.inject.Inject

class StartConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val vpsRepository: VpsRepository,
) {
    suspend operator fun invoke(): Result<Conversation> {
        return try {
            val settings = settingsRepository.getSettings()
            vpsRepository.connect(settings.host, settings.port)

            val lastId = conversationRepository.getLastActiveConversationId()
            val conversation = if (lastId != null) {
                conversationRepository.getConversation(lastId) ?: conversationRepository.createConversation()
            } else {
                conversationRepository.createConversation()
            }

            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
