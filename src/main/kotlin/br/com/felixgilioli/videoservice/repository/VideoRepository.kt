package br.com.felixgilioli.videoservice.repository

import br.com.felixgilioli.videoservice.entity.Video
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VideoRepository : JpaRepository<Video, UUID> {

    fun findByUserId(userId: String): List<Video>

}