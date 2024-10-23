package de.tfelix.evmbitstream

import de.tfelix.evmbitstream.payment.PaymentConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@EnableConfigurationProperties(PaymentConfig::class)
class EthBitstream

fun main(args: Array<String>) {
    runApplication<EthBitstream>(*args)
}