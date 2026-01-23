package br.com.felixgilioli.videoservice.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import java.net.URI

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig(private val properties: StorageProperties) {

    @Bean
    fun s3Client(): S3Client {
        val client = S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey, properties.secretKey)
            ))
            .forcePathStyle(true)
            .build()

        createBucketIfNotExists(client)
        return client
    }

    private fun createBucketIfNotExists(client: S3Client) {
        try {
            client.headBucket { it.bucket(properties.bucket) }
        } catch (e: NoSuchBucketException) {
            client.createBucket { it.bucket(properties.bucket) }
        }
    }
}