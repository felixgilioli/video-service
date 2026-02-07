package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.repository.VideoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class VideoServiceTest {

    private val repository: VideoRepository = mockk()
    private val storageService: StorageService = mockk()
    private val sqsService: SqsService = mockk()

    private val service = VideoService(
        repository = repository,
        storageService = storageService,
        sqsService = sqsService
    )

    @Test
    fun `create deve salvar video com userId, title e description`() {
        val userId = "user-123"
        val request = CreateVideoRequest(title = "Meu vídeo", description = "descr")

        val repoReturn = Video(
            id = UUID.randomUUID(),
            userId = userId,
            title = request.title,
            description = request.description,
            status = VideoStatus.PENDING
        )

        every { repository.save(any()) } returns repoReturn

        val result = service.create(userId, request)

        assertEquals(repoReturn, result)

        val videoSlot = slot<Video>()
        verify(exactly = 1) { repository.save(capture(videoSlot)) }

        val saved = videoSlot.captured
        assertEquals(userId, saved.userId)
        assertEquals(request.title, saved.title)
        assertEquals(request.description, saved.description)
        assertEquals(VideoStatus.PENDING, saved.status)
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }
}
