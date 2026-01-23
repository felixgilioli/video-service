package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.repository.VideoRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class VideoService(private val repository: VideoRepository) {

    fun create(userId: String, request: CreateVideoRequest): Video {
        val video = Video(userId = userId, title = request.title, description = request.description)
        return repository.save(video)
    }

    fun findById(id: UUID): Video =
        repository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
}