package de.tfelix.evmbitstream.blockchain

interface PaymentContract {
    fun collectPayment(preimage: String)
}

