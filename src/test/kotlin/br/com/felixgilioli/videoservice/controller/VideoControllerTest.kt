package br.com.felixgilioli.videoservice.controller

import br.com.felixgilioli.videoservice.dto.request.CreateVideoRequest
import br.com.felixgilioli.videoservice.dto.request.UpdateVideoRequest
import br.com.felixgilioli.videoservice.entity.Video
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.service.VideoService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

class VideoControllerTest {

    private val service: VideoService = mockk()
    private val controller = VideoController(service)

    private fun video(
        id: UUID = UUID.randomUUID(),
        userId: String = "user@test.com",
        title: String = "Test Video"
    ) = Video(id = id, userId = userId, title = title, status = VideoStatus.PENDING)

    @Test
    fun `create deve delegar para service e retornar VideoResponse`() {
        val userId = "user@test.com"
        val request = CreateVideoRequest(title = "My Video", description = "Desc")
        val video = video()
        every { service.create(userId, request) } returns video

        val result = controller.create(userId, request)

        assertEquals(video.id, result.id)
        assertEquals(video.title, result.title)
        assertEquals(video.userId, result.userId)
        verify(exactly = 1) { service.create(userId, request) }
    }

    @Test
    fun `findById deve delegar para service e retornar VideoResponse`() {
        val id = UUID.randomUUID()
        val video = video(id = id)
        every { service.findById(id) } returns video

        val result = controller.findById(id)

        assertEquals(id, result.id)
        assertEquals(video.title, result.title)
        verify(exactly = 1) { service.findById(id) }
    }

    @Test
    fun `findByUser deve retornar lista de VideoResponse do usuario`() {
        val userId = "user@test.com"
        val videos = listOf(video(), video())
        every { service.findByUser(userId) } returns videos

        val result = controller.findByUser(userId)

        assertEquals(2, result.size)
        verify(exactly = 1) { service.findByUser(userId) }
    }

    @Test
    fun `update deve delegar para service e retornar VideoResponse atualizado`() {
        val id = UUID.randomUUID()
        val userId = "user@test.com"
        val request = UpdateVideoRequest(title = "Updated", description = "New desc")
        val updated = video(id = id, title = "Updated")
        every { service.update(id, userId, request) } returns updated

        val result = controller.update(id, userId, request)

        assertEquals("Updated", result.title)
        verify(exactly = 1) { service.update(id, userId, request) }
    }

    @Test
    fun `delete deve delegar para service`() {
        val id = UUID.randomUUID()
        val userId = "user@test.com"
        every { service.delete(id, userId) } just Runs

        controller.delete(id, userId)

        verify(exactly = 1) { service.delete(id, userId) }
    }

    @Test
    fun `upload deve delegar para service e retornar VideoResponse com status PROCESSING`() {
        val id = UUID.randomUUID()
        val userId = "user@test.com"
        val file: MultipartFile = mockk()
        val video = video(id = id).copy(videoUrl = "http://storage/video.mp4", status = VideoStatus.PROCESSING)
        every { service.upload(id, userId, file) } returns video

        val result = controller.upload(id, userId, file)

        assertEquals(VideoStatus.PROCESSING, result.status)
        assertEquals("http://storage/video.mp4", result.videoUrl)
        verify(exactly = 1) { service.upload(id, userId, file) }
    }
}
