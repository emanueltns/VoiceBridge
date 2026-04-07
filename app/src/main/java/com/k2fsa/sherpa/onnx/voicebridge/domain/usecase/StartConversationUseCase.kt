package com.k2fsa.sherpa.onnx.voicebridge.domain.usecase

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Conversation
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.SettingsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
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
