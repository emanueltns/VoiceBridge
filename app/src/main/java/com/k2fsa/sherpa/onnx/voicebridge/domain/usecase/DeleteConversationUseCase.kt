package com.k2fsa.sherpa.onnx.voicebridge.domain.usecase

import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(conversationId: String) {
        conversationRepository.deleteConversation(conversationId)
    }
}
