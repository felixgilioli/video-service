package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.config.SqsProperties
import br.com.felixgilioli.videoservice.dto.VideoProcessingMessage
import io.mockk.*
import io.opentelemetry.api.trace.Span
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.function.Consumer

class SqsServiceTest {

    private val sqsClient: SqsClient = mockk()
    private val properties = SqsProperties(
        endpoint = "http://localhost:4566",
        region = "us-east-1",
        accessKey = "test",
        secretKey = "test",
        videoProcessingQueue = "video-processing-queue",
        videoStatusQueue = "video-status-queue"
    )
    private val objectMapper: ObjectMapper = mockk()
    private val service = SqsService(sqsClient, properties, objectMapper)

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `sendVideoProcessingMessage deve enviar mensagem para a fila SQS com traceparent`() {
        val message = VideoProcessingMessage(
            videoId = UUID.randomUUID(),
            userId = "user@test.com",
            videoUrl = "http://storage/video.mp4"
        )

        mockkStatic(Span::class)
        every { Span.current() } returns mockk(relaxed = true)
        every { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) } returns mockk<SendMessageResponse>()

        service.sendVideoProcessingMessage(message)

        verify(exactly = 1) { Span.current() }
        verify(exactly = 1) { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
    }
}
