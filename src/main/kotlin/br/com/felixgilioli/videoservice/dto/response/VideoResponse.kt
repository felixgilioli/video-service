package br.com.felixgilioli.videoservice.dto.response

import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import java.time.LocalDateTime
import java.util.*

data class VideoResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val status: VideoStatus,
    val createdAt: LocalDateTime
)

fun Video.toResponse() = VideoResponse(id!!, title, description, status, createdAt)