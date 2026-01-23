package br.com.felixgilioli.videoservice.entity

import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "videos")
data class Video(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false)
    val userId: String,
    @Column(nullable = false)
    val title: String,
    val description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: VideoStatus = VideoStatus.PENDING,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)