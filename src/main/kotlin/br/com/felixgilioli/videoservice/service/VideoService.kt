package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.dto.VideoProcessingMessage
import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.dto.request.UpdateVideoRequest
import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.repository.VideoRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.*

@Service
class VideoService(
    private val repository: VideoRepository,
    private val storageService: StorageService,
    private val sqsService: SqsService
) {

    fun create(userId: String, request: CreateVideoRequest): Video {
        val video = Video(userId = userId, title = request.title, description = request.description)
        return repository.save(video)
    }

    fun findById(id: UUID): Video =
        repository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    fun findByUser(userId: String): List<Video> = repository.findByUserId(userId)

    fun update(id: UUID, userId: String, request: UpdateVideoRequest): Video {
        val video = findByIdAndUser(id, userId)
        return repository.save(
            video.copy(
                title = request.title,
                description = request.description,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    fun delete(id: UUID, userId: String) {
        val video = findByIdAndUser(id, userId)
        repository.delete(video)
    }

    private fun findByIdAndUser(id: UUID, userId: String): Video {
        val video = findById(id)
        if (video.userId != userId) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        return video
    }

    fun upload(id: UUID, userId: String, file: MultipartFile): Video {
        val video = findByIdAndUser(id, userId)
        val key = "videos/${video.id}/${file.originalFilename}"
        val url = storageService.upload(key, file)
        val updatedVideo = repository.save(
            video.copy(
                videoUrl = url,
                status = VideoStatus.PROCESSING,
                updatedAt = LocalDateTime.now()
            )
        )

        sqsService.sendVideoProcessingMessage(
            VideoProcessingMessage(
                videoId = updatedVideo.id!!,
                userId = updatedVideo.userId,
                videoUrl = url
            )
        )

        return updatedVideo
    }
}