package de.tfelix.evmbitstream.download

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.math.pow

@ConfigurationProperties(prefix = "demo.download")
class DownloadConfig(
    val paymentTokenAddress: String,
    val paymentPerByte: BigInteger,
    val paymentFixed: BigInteger,
    val preImageValidity: String
) {
    val preImageValidityAsDuration = Duration.parse(preImageValidity)!!
}