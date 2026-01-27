package br.com.felixgilioli.videoservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class VideoServiceApplication

fun main(args: Array<String>) {
	runApplication<VideoServiceApplication>(*args)
}
