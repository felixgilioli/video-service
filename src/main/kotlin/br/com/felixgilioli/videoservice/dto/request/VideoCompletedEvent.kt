package br.com.felixgilioli.videoservice.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class VideoCompletedEvent(
    val videoId: UUID,
    val userId: String,
    val status: String,
    val zipUrl: String?
)

data class SnsMessage(
    @JsonProperty("Message") val message: String
)