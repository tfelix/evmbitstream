package de.tfelix.evmbitstream.payment

import org.springframework.stereotype.Component

@Component
class PaymentHeaderExtractor {

    fun extractPaymentInfo(headers: Map<String, String>): String {

        return headers["X-Bitstream-Use-Channel"]
            ?: throw PaymentHeaderMissingException("X-Bitstream-Use-Channel")
    }
}
