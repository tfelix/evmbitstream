package de.tfelix.evmbitstream.blockchain

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class MockPaymentContract : PaymentContract {

    private val collectedPayments = mutableListOf<String>()

    fun clear() {
        collectedPayments.clear()
    }

    fun getCollectedPayments(): List<String> {
        return collectedPayments.toList()
    }

    override fun collectPayment(preimage: String) {
        log.warn { "Mock Payment Collection for preImage: $preimage" }
        collectedPayments.add(preimage)
    }
}