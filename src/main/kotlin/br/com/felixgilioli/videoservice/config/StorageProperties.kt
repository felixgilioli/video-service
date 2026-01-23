package br.com.felixgilioli.videoservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String
)