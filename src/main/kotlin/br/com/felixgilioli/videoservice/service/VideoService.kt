package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.dto.request.UpdateVideoRequest
import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.repository.VideoRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@Service
class VideoService(private val repository: VideoRepository) {

    fun create(userId: String, request: CreateVideoRequest): Video {
        val video = Video(userId = userId, title = request.title, description = request.description)
        return repository.save(video)
    }

    fun findById(id: UUID): Video =
        repository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    fun findByUser(userId: String): List<Video> = repository.findByUserId(userId)

    fun update(id: UUID, userId: String, request: UpdateVideoRequest): Video {
        val video = findByIdAndUser(id, userId)
        return repository.save(video.copy(title = request.title, description = request.description, updatedAt = LocalDateTime.now()))
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
}