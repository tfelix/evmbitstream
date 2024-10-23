package de.tfelix.evmbitstream.payment

import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/payment")
class PaymentController(
    private val paymentProcessorService: PaymentProcessorService
) {

    data class ChannelStateUpdate(
        val channelId: String,
        val serverAmount: String,
        val clientAmount: String,
        val htlc: Htlc?,
        val sequence: Long,
    ) {
        data class Htlc(
            val inflightAmount: String,
            val lockedUntil: Long,
            val secret: String,
        )
    }

    data class ChannelStateUpdateSignedRequest(
        val update: ChannelStateUpdate,
        val signature: String
    )

    @PostMapping
    fun pay(
        @RequestBody channelStateUpdate: ChannelStateUpdateSignedRequest
    ): ResponseEntity<InputStreamResource> {
        paymentProcessorService.collectPayment(channelStateUpdate)

        return ResponseEntity.ok().build()
    }

}