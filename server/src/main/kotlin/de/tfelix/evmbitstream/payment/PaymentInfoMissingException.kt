package de.tfelix.evmbitstream.payment

import de.tfelix.evmbitstream.bitstream.BitstreamException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.math.BigInteger

@ResponseStatus(value = HttpStatus.PAYMENT_REQUIRED)
class PaymentInfoMissingException(
    override val paymentRecipientAddress: String,
    override val amount: BigInteger,
    override val tokenAddress: String,
    cause: Throwable? = null
) : PaymentException(cause)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class InvalidPaymentData(
    message: String,
    cause: Throwable? = null
) : BitstreamException(message, cause)