package br.com.felixgilioli.videoservice.consumer

import br.com.felixgilioli.videoservice.config.SqsProperties
import br.com.felixgilioli.videoservice.dto.request.SnsMessage
import br.com.felixgilioli.videoservice.dto.request.VideoCompletedEvent
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.service.VideoService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import tools.jackson.databind.ObjectMapper

@Component
class VideoStatusConsumer(
    private val sqsClient: SqsClient,
    private val sqsProperties: SqsProperties,
    private val videoService: VideoService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5000)
    fun poll() {
        val messages = sqsClient.receiveMessage {
            it.queueUrl(getQueueUrl()).maxNumberOfMessages(5).waitTimeSeconds(10)
        }.messages()

        messages.forEach { process(it) }
    }

    private fun process(message: Message) {
        try {
            val snsMessage = objectMapper.readValue(message.body(), SnsMessage::class.java)
            val event = objectMapper.readValue(snsMessage.message, VideoCompletedEvent::class.java)

            logger.info("Atualizando status do vídeo: ${event.videoId} -> ${event.status}")

            videoService.updateStatus(
                videoId = event.videoId,
                status = VideoStatus.valueOf(event.status),
                zipUrl = event.zipUrl
            )

            deleteMessage(message)
            logger.info("Status atualizado: ${event.videoId}")
        } catch (e: Exception) {
            logger.error("Erro ao processar mensagem", e)
        }
    }

    private fun getQueueUrl(): String =
        sqsClient.getQueueUrl { it.queueName(sqsProperties.videoStatusQueue) }.queueUrl()

    private fun deleteMessage(message: Message) {
        sqsClient.deleteMessage { it.queueUrl(getQueueUrl()).receiptHandle(message.receiptHandle()) }
    }
}