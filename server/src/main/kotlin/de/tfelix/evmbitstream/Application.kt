package de.tfelix.evmbitstream

import de.tfelix.evmbitstream.download.DownloadConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@EnableConfigurationProperties(DownloadConfig::class)
class EthBitstream

fun main(args: Array<String>) {
    runApplication<EthBitstream>(*args)
}