package br.com.felixgilioli.videoservice.dto.request

import jakarta.validation.constraints.NotBlank

data class UpdateVideoRequest(
    @field:NotBlank val title: String,
    val description: String?
)