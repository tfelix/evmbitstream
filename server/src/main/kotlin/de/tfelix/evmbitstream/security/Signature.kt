package de.tfelix.evmbitstream.security

import java.math.BigDecimal

data class Signature(
    val allowance: Allowance,
    val validUntilBlock: Long,
    val nonce: Long,
    val signature: String
)

data class Allowance(
    val allowBytes: BigDecimal,
    val allowPayPerByte: BigDecimal
)

data class ClientDataRequest(
    val address: String,
    val signature: Signature
)