package com.k2fsa.sherpa.onnx.voicebridge.domain.usecase

import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
import javax.inject.Inject

class StopConversationUseCase @Inject constructor(
    private val vpsRepository: VpsRepository,
) {
    suspend operator fun invoke() {
        vpsRepository.disconnect()
    }
}
