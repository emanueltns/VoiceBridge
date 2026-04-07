package com.k2fsa.sherpa.onnx.voicebridge.domain.usecase

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
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
