package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.config.StorageProperties
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream

class StorageServiceTest {

    private val s3Client: S3Client = mockk()
    private val properties = StorageProperties(
        endpoint = "http://localhost:9000",
        accessKey = "minioadmin",
        secretKey = "minioadmin",
        bucket = "videos"
    )
    private val service = StorageService(s3Client, properties)

    @Test
    fun `upload deve chamar putObject no S3 e retornar URL publica`() {
        val key = "videos/uuid-123/video.mp4"
        val file: MultipartFile = mockk()
        every { file.contentType } returns "video/mp4"
        every { file.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { file.size } returns 0L
        every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()

        val result = service.upload(key, file)

        assertEquals("http://localhost:9000/videos/$key", result)
        verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
    }
}
