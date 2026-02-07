package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.dto.VideoProcessingMessage
import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.dto.request.UpdateVideoRequest
import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.repository.VideoRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.*

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

    @Test
    fun `findById deve retornar o video quando encontrado`() {
        val id = UUID.randomUUID()
        val video = Video(id = id, userId = "u1", title = "t1")

        every { repository.findById(id) } returns Optional.of(video)

        val result = service.findById(id)

        assertEquals(video, result)
        verify(exactly = 1) { repository.findById(id) }
    }

    @Test
    fun `findById deve lancar 404 quando nao encontrado`() {
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns Optional.empty()

        val ex = assertThrows(ResponseStatusException::class.java) { service.findById(id) }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)

        verify(exactly = 1) { repository.findById(id) }
    }

    @Test
    fun `findByUser deve retornar lista do repositorio`() {
        val userId = "user-1"
        val videos = listOf(
            Video(id = UUID.randomUUID(), userId = userId, title = "t1"),
            Video(id = UUID.randomUUID(), userId = userId, title = "t2")
        )

        every { repository.findByUserId(userId) } returns videos

        val result = service.findByUser(userId)

        assertEquals(videos, result)
        verify(exactly = 1) { repository.findByUserId(userId) }
    }

    @Test
    fun `update deve atualizar titulo e descricao e salvar`() {
        val id = UUID.randomUUID()
        val userId = "user-1"
        val existing = Video(
            id = id,
            userId = userId,
            title = "old",
            description = "old-desc",
            updatedAt = LocalDateTime.now().minusDays(1)
        )
        val request = UpdateVideoRequest(title = "new", description = "new-desc")

        every { repository.findById(id) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.update(id, userId, request)

        assertEquals("new", result.title)
        assertEquals("new-desc", result.description)
        assertTrue(result.updatedAt.isAfter(existing.updatedAt))

        val videoSlot = slot<Video>()
        verify(exactly = 1) { repository.save(capture(videoSlot)) }
        assertEquals(id, videoSlot.captured.id)
        assertEquals(userId, videoSlot.captured.userId)
        assertEquals("new", videoSlot.captured.title)
        assertEquals("new-desc", videoSlot.captured.description)
    }

    @Test
    fun `update deve lancar 403 quando userId nao for dono`() {
        val id = UUID.randomUUID()
        val existing = Video(id = id, userId = "owner", title = "t")

        every { repository.findById(id) } returns Optional.of(existing)

        val ex = assertThrows(ResponseStatusException::class.java) {
            service.update(id, "other", UpdateVideoRequest(title = "x", description = "y"))
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)

        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `delete deve remover video quando userId corresponder`() {
        val id = UUID.randomUUID()
        val userId = "user-1"
        val existing = Video(id = id, userId = userId, title = "t")

        every { repository.findById(id) } returns Optional.of(existing)
        every { repository.delete(existing) } just Runs

        service.delete(id, userId)

        verify(exactly = 1) { repository.delete(existing) }
    }

    @Test
    fun `delete deve lancar 403 quando userId nao for dono`() {
        val id = UUID.randomUUID()
        val existing = Video(id = id, userId = "owner", title = "t")

        every { repository.findById(id) } returns Optional.of(existing)

        val ex = assertThrows(ResponseStatusException::class.java) { service.delete(id, "other") }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)

        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `upload deve enviar arquivo para storage, atualizar video e enviar mensagem para sqs`() {
        val id = UUID.randomUUID()
        val userId = "user-1"
        val existing = Video(id = id, userId = userId, title = "t", status = VideoStatus.PENDING)

        val file: MultipartFile = mockk()
        every { file.originalFilename } returns "video.mp4"

        val expectedKey = "videos/$id/video.mp4"
        val url = "http://storage/$expectedKey"

        every { repository.findById(id) } returns Optional.of(existing)
        every { storageService.upload(expectedKey, file) } returns url

        val savedSlot = slot<Video>()
        every { repository.save(capture(savedSlot)) } answers {
            val v = savedSlot.captured
            v.copy(id = id)
        }

        every { sqsService.sendVideoProcessingMessage(any()) } just Runs

        val result = service.upload(id, userId, file)

        assertEquals(url, result.videoUrl)
        assertEquals(VideoStatus.PROCESSING, result.status)
        assertNotNull(result.updatedAt)

        verify(exactly = 1) { storageService.upload(expectedKey, file) }
        verify(exactly = 1) { repository.save(any()) }

        val msgSlot = slot<VideoProcessingMessage>()
        verify(exactly = 1) { sqsService.sendVideoProcessingMessage(capture(msgSlot)) }
        assertEquals(id, msgSlot.captured.videoId)
        assertEquals(userId, msgSlot.captured.userId)
        assertEquals(url, msgSlot.captured.videoUrl)
    }

    @Test
    fun `upload deve lancar 403 quando userId nao for dono`() {
        val id = UUID.randomUUID()
        val existing = Video(id = id, userId = "owner", title = "t")
        val file: MultipartFile = mockk(relaxed = true)

        every { repository.findById(id) } returns Optional.of(existing)

        val ex = assertThrows(ResponseStatusException::class.java) { service.upload(id, "other", file) }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)

        verify(exactly = 0) { storageService.upload(any(), any()) }
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { sqsService.sendVideoProcessingMessage(any()) }
    }

    @Test
    fun `updateStatus deve atualizar status e zipUrl`() {
        val id = UUID.randomUUID()
        val existing = Video(
            id = id,
            userId = "u1",
            title = "t",
            status = VideoStatus.PROCESSING,
            zipUrl = null,
            updatedAt = LocalDateTime.now().minusDays(1)
        )

        every { repository.findById(id) } returns Optional.of(existing)

        val savedSlot = slot<Video>()
        every { repository.save(capture(savedSlot)) } answers { savedSlot.captured }

        service.updateStatus(id, VideoStatus.READY, "http://zip")

        verify(exactly = 1) { repository.findById(id) }
        verify(exactly = 1) { repository.save(any()) }

        assertEquals(VideoStatus.READY, savedSlot.captured.status)
        assertEquals("http://zip", savedSlot.captured.zipUrl)
        assertTrue(savedSlot.captured.updatedAt.isAfter(existing.updatedAt))
    }
}
