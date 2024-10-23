package de.tfelix.evmbitstream.payment

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigInteger
import java.time.Duration

@ConfigurationProperties(prefix = "demo.payment")
class PaymentConfig(
    val paymentTokenAddress: String,
    val paymentPerByte: BigInteger,
    val paymentFixed: BigInteger,
    preImageValidity: String,
    minHtlcTimeout: String
) {
    val preImageValidityAsDuration = Duration.parse(preImageValidity)!!

    val minHtlcTimeoutAsDuration = Duration.parse(minHtlcTimeout)!!
}