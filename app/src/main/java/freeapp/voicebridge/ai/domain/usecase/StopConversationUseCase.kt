package freeapp.voicebridge.ai.domain.usecase

import freeapp.voicebridge.ai.domain.repository.VpsRepository
import javax.inject.Inject

class StopConversationUseCase @Inject constructor(
    private val vpsRepository: VpsRepository,
) {
    suspend operator fun invoke() {
        vpsRepository.disconnect()
    }
}
