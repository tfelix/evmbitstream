package de.tfelix.evmbitstream.payment

import de.tfelix.evmbitstream.blockchain.PaymentContract
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.math.BigInteger

private val log = KotlinLogging.logger { }

@Service
class PaymentCollectorService(
    private val preImagePaymentRepository: PreImagePaymentRepository,
    private val paymentContract: PaymentContract
) {

    @EventListener
    fun onPaymentDiscoveredEvent(event: PaymentDiscoveredEvent) {
        collectPayment(event.preImageHash, event.amount)
    }

    fun collectPayment(preImageHash: String, detectedPaymentAmount: String) {
        val payment = preImagePaymentRepository.findByHash(preImageHash)

        if (payment == null) {
            log.debug { "Ignoring unknown Payment: $preImageHash" }
            return
        }

        if (!isValidPayment(payment, detectedPaymentAmount)) {
            log.warn { "Detected invalid payment amount for $preImageHash, payed amount: $detectedPaymentAmount" }
            return
        }

        releasePreImage(payment)
    }

    private fun isValidPayment(payment: PreImagePayment, detectedPaymentAmount: String): Boolean {
        val expectedAmount = BigInteger(payment.paymentAmount)
        val detectedAmount = BigInteger(detectedPaymentAmount)

        return detectedAmount >= expectedAmount
    }

    private fun releasePreImage(preImageHash: PreImagePayment) {
        preImageHash.verifyPreImage()

        paymentContract.collectPayment(preImageHash.preImage)
    }
}