package de.tfelix.evmbitstream.payment

import de.tfelix.evmbitstream.download.DownloadConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigInteger
import kotlin.math.pow

private val log = KotlinLogging.logger { }

@Component
class PaymentAmountCalculator(
    private val downloadConfig: DownloadConfig
) {

    init {
        log.info { "Payment Token: ${downloadConfig.paymentTokenAddress}" }

        val amountPerKbyte = downloadConfig.paymentPerByte * BigInteger.valueOf(1024)
        val amountPerMbyte = downloadConfig.paymentPerByte * BigInteger.valueOf(1024.0.pow(2.0).toLong())
        val amountPerGbyte = downloadConfig.paymentPerByte * BigInteger.valueOf(1024.0.pow(3.0).toLong())

        log.info { "  Payment Amount Fixed Fee: ${downloadConfig.paymentFixed}" }
        log.info { "Payment Amount for 1 KByte: $amountPerKbyte" }
        log.info { "Payment Amount for 1 MByte: $amountPerMbyte" }
        log.info { "Payment Amount for 1 GByte: $amountPerGbyte" }
    }

    fun getPaymentAmount(fileSizeBytes: Long): BigInteger {
        return BigInteger.valueOf(fileSizeBytes) * downloadConfig.paymentPerByte + downloadConfig.paymentFixed
    }
}