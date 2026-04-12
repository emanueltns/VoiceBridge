package freeapp.voicebridge.ai.domain.model

data class AudioSegment(
    val samples: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioSegment) return false
        return samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int = samples.contentHashCode()
}
