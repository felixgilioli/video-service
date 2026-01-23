package br.com.felixgilioli.videoservice.dto

import java.util.UUID

data class VideoProcessingMessage(
    val videoId: UUID,
    val userId: String,
    val videoUrl: String
)