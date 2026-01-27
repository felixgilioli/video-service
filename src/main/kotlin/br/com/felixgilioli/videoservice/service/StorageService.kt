package br.com.felixgilioli.videoservice.service

import br.com.felixgilioli.videoservice.config.StorageProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Service
class StorageService(
    private val s3Client: S3Client,
    private val properties: StorageProperties
) {

    fun upload(key: String, file: MultipartFile): String {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(properties.bucket)
                .key(key)
                .contentType(file.contentType)
                .build(),
            RequestBody.fromInputStream(file.inputStream, file.size)
        )
        return "${properties.endpoint}/$key"
    }
}