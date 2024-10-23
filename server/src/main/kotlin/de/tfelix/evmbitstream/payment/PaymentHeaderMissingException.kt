package de.tfelix.evmbitstream.payment

import de.tfelix.evmbitstream.bitstream.BitstreamException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class PaymentHeaderMissingException(
    header: String
) : BitstreamException("Bitstream request header '$header' missing")