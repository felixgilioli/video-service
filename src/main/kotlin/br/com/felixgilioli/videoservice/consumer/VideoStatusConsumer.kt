package br.com.felixgilioli.videoservice.consumer

import br.com.felixgilioli.videoservice.config.SqsProperties
import br.com.felixgilioli.videoservice.dto.request.SnsMessage
import br.com.felixgilioli.videoservice.dto.request.VideoCompletedEvent
import br.com.felixgilioli.videoservice.enumeration.VideoStatus
import br.com.felixgilioli.videoservice.service.VideoService
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
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
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer
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
        val snsMessage = try {
            objectMapper.readValue(message.body(), SnsMessage::class.java)
        } catch (e: Exception) {
            logger.error("Erro ao parsear SNS message", e)
            return
        }

        val traceparent = snsMessage.messageAttributes?.get("traceparent")?.value
        val context = extractContextFromTraceparent(traceparent)

        val span = tracer.spanBuilder("update-video-status")
            .setParent(context)
            .startSpan()

        try {
            span.makeCurrent().use {
                val event = objectMapper.readValue(snsMessage.message, VideoCompletedEvent::class.java)
                span.setAttribute("videoId", event.videoId.toString())
                span.setAttribute("status", event.status)

                logger.info("Atualizando status do vídeo: ${event.videoId} -> ${event.status}")

                videoService.updateStatus(
                    videoId = event.videoId,
                    status = VideoStatus.valueOf(event.status),
                    zipUrl = event.zipUrl
                )

                deleteMessage(message)
                logger.info("Status atualizado: ${event.videoId}")
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.message ?: "Erro desconhecido")
            logger.error("Erro ao processar mensagem", e)
        } finally {
            span.end()
        }
    }

    private fun extractContextFromTraceparent(traceparent: String?): Context {
        if (traceparent == null) return Context.current()

        val propagator = W3CTraceContextPropagator.getInstance()
        val carrier = mapOf("traceparent" to traceparent)

        val getter = object : TextMapGetter<Map<String, String>> {
            override fun keys(carrier: Map<String, String>): Iterable<String> = carrier.keys
            override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
        }

        return propagator.extract(Context.current(), carrier, getter)
    }

    private fun getQueueUrl(): String =
        sqsClient.getQueueUrl { it.queueName(sqsProperties.videoStatusQueue) }.queueUrl()

    private fun deleteMessage(message: Message) {
        sqsClient.deleteMessage { it.queueUrl(getQueueUrl()).receiptHandle(message.receiptHandle()) }
    }
}