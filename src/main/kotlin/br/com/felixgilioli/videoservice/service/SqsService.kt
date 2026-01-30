package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.config.SqsProperties
import br.com.felixgilioli.videoservice.dto.VideoProcessingMessage
import io.opentelemetry.api.trace.Span
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import tools.jackson.databind.ObjectMapper

@Service
class SqsService(
    private val sqsClient: SqsClient,
    private val properties: SqsProperties,
    private val objectMapper: ObjectMapper
) {

    fun sendVideoProcessingMessage(message: VideoProcessingMessage) {
        val traceId = Span.current().spanContext.traceId

        sqsClient.sendMessage {
            it.queueUrl(getQueueUrl())
                .messageBody(objectMapper.writeValueAsString(message))
                .messageAttributes(
                    mapOf(
                        "traceId" to MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(traceId)
                            .build()
                    )
                )
        }
    }

    private fun getQueueUrl(): String =
        sqsClient.getQueueUrl { it.queueName(properties.videoProcessingQueue) }.queueUrl()
}