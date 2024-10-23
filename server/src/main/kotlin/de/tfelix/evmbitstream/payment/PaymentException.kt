package de.tfelix.evmbitstream.payment

import de.tfelix.evmbitstream.bitstream.BitstreamException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.math.BigInteger

@ResponseStatus(value = HttpStatus.PAYMENT_REQUIRED)
abstract class PaymentException(
    cause: Throwable? = null
) : BitstreamException("Payment Exception", cause) {
    abstract val paymentRecipientAddress: String
    abstract val amount: BigInteger
    abstract val tokenAddress: String
}