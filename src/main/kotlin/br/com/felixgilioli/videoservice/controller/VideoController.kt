package br.com.felixgilioli.videoservice.controller

import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.dto.request.UpdateVideoRequest
import br.com.felixgilioli.videoservice.dto.response.VideoResponse
import br.com.felixgilioli.videoservice.dto.response.toResponse
import br.com.felixgilioli.videoservice.service.VideoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/videos")
class VideoController(private val service: VideoService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: CreateVideoRequest
    ): VideoResponse = service.create(userId, request).toResponse()

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): VideoResponse = service.findById(id).toResponse()

    @GetMapping
    fun findByUser(@RequestHeader("X-User-Id") userId: String): List<VideoResponse> =
        service.findByUser(userId).map { it.toResponse() }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: UpdateVideoRequest
    ): VideoResponse = service.update(id, userId, request).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: String) =
        service.delete(id, userId)

    @PostMapping("/{id}/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam("file") file: MultipartFile
    ): VideoResponse = service.upload(id, userId, file).toResponse()
}
