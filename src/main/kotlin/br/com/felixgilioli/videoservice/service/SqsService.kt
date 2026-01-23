package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.config.SqsProperties
import br.com.felixgilioli.videoservice.dto.VideoProcessingMessage
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import tools.jackson.databind.ObjectMapper

@Service
class SqsService(
    private val sqsClient: SqsClient,
    private val properties: SqsProperties,
    private val objectMapper: ObjectMapper
) {

    fun sendVideoProcessingMessage(message: VideoProcessingMessage) {
        sqsClient.sendMessage {
            it.queueUrl(getQueueUrl())
                .messageBody(objectMapper.writeValueAsString(message))
        }
    }

    private fun getQueueUrl(): String =
        sqsClient.getQueueUrl { it.queueName(properties.videoProcessingQueue) }.queueUrl()
}