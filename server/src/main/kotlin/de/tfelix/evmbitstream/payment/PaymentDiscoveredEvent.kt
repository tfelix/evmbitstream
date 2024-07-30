package de.tfelix.evmbitstream.payment

data class PaymentDiscoveredEvent(
    val preImageHash: String,
    val amount: String
)