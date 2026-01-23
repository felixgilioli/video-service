package br.com.felixgilioli.videoservice.controller

import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.dto.response.VideoResponse
import br.com.felixgilioli.videoservice.dto.response.toResponse
import br.com.felixgilioli.videoservice.service.VideoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
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
}
