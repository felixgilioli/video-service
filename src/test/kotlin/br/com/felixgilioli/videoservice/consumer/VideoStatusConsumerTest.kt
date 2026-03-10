package br.com.felixgilioli.videoservice.consumer

import br.com.felixgilioli.videoservice.config.SqsProperties
import br.com.felixgilioli.videoservice.dto.request.SnsMessage
import br.com.felixgilioli.videoservice.dto.request.VideoCompletedEvent
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.service.VideoService
import io.mockk.*
import io.opentelemetry.api.trace.Tracer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.function.Consumer

class VideoStatusConsumerTest {

    private val sqsClient: SqsClient = mockk()
    private val sqsProperties = SqsProperties(
        endpoint = "http://localhost:4566",
        region = "us-east-1",
        accessKey = "test",
        secretKey = "test",
        videoProcessingQueue = "video-processing-queue",
        videoStatusQueue = "video-status-queue"
    )
    private val videoService: VideoService = mockk()
    private val objectMapper: ObjectMapper = mockk()
    private val tracer: Tracer = mockk(relaxed = true)

    private val consumer = VideoStatusConsumer(
        sqsClient = sqsClient,
        sqsProperties = sqsProperties,
        videoService = videoService,
        objectMapper = objectMapper,
        tracer = tracer
    )

    @BeforeEach
    fun setup() {
        val receiveResponse: ReceiveMessageResponse = mockk()
        every { receiveResponse.messages() } returns emptyList()
        every { sqsClient.receiveMessage(any<Consumer<ReceiveMessageRequest.Builder>>()) } returns receiveResponse
    }

    @Test
    fun `poll sem mensagens nao deve chamar videoService nem deletar mensagens`() {
        consumer.poll()

        verify(exactly = 0) { videoService.updateStatus(any(), any(), any(), any()) }
        verify(exactly = 0) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
    }

    @Test
    fun `poll com mensagem valida deve atualizar status do video e deletar mensagem`() {
        val videoId = UUID.randomUUID()
        val message: Message = mockk()
        every { message.body() } returns "sns-body"

        val receiveResponse: ReceiveMessageResponse = mockk()
        every { receiveResponse.messages() } returns listOf(message)
        every { sqsClient.receiveMessage(any<Consumer<ReceiveMessageRequest.Builder>>()) } returns receiveResponse

        val snsMessage = SnsMessage(message = "event-json", messageAttributes = null)
        val event = VideoCompletedEvent(
            videoId = videoId,
            userId = "user@test.com",
            status = "READY",
            zipUrl = "http://zip",
            firstFrameUrl = "http://frame"
        )

        every { objectMapper.readValue(any<String>(), SnsMessage::class.java) } returns snsMessage
        every { objectMapper.readValue(any<String>(), VideoCompletedEvent::class.java) } returns event
        every { videoService.updateStatus(videoId, VideoStatus.READY, "http://zip", "http://frame") } just Runs
        every { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) } returns mockk<DeleteMessageResponse>()

        consumer.poll()

        verify(exactly = 1) { videoService.updateStatus(videoId, VideoStatus.READY, "http://zip", "http://frame") }
        verify(exactly = 1) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
    }

    @Test
    fun `poll com JSON invalido deve logar erro e nao deletar mensagem`() {
        val message: Message = mockk()
        every { message.body() } returns "invalid-json"

        val receiveResponse: ReceiveMessageResponse = mockk()
        every { receiveResponse.messages() } returns listOf(message)
        every { sqsClient.receiveMessage(any<Consumer<ReceiveMessageRequest.Builder>>()) } returns receiveResponse

        every { objectMapper.readValue(any<String>(), SnsMessage::class.java) } throws RuntimeException("parse error")

        consumer.poll()

        verify(exactly = 0) { videoService.updateStatus(any(), any(), any(), any()) }
        verify(exactly = 0) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
    }

    @Test
    fun `poll com erro no processamento deve registrar erro no span e ainda deletar mensagem`() {
        val message: Message = mockk()
        every { message.body() } returns "sns-body"

        val receiveResponse: ReceiveMessageResponse = mockk()
        every { receiveResponse.messages() } returns listOf(message)
        every { sqsClient.receiveMessage(any<Consumer<ReceiveMessageRequest.Builder>>()) } returns receiveResponse

        val snsMessage = SnsMessage(message = "event-json", messageAttributes = null)
        every { objectMapper.readValue(any<String>(), SnsMessage::class.java) } returns snsMessage
        every { objectMapper.readValue(any<String>(), VideoCompletedEvent::class.java) } throws RuntimeException("processing error")
        every { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) } returns mockk<DeleteMessageResponse>()

        consumer.poll()

        verify(exactly = 0) { videoService.updateStatus(any(), any(), any(), any()) }
        verify(exactly = 1) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
    }
}
