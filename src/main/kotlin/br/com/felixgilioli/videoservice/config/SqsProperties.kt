package br.com.felixgilioli.videoservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sqs")
data class SqsProperties(
    val endpoint: String,
    val region: String,
    val accessKey: String,
    val secretKey: String,
    val videoProcessingQueue: String
)