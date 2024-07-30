package de.tfelix.evmbitstream.download

import de.tfelix.evmbitstream.bitstream.EncryptedFile
import java.math.BigInteger
import java.time.Instant

data class PreparedDownload(
    val preImageHash: String,
    val tokenAddress: String,
    val expectedPaymentAmount: BigInteger,
    val validUntil: Instant,
    val download: EncryptedFile,
    val mime: String,
    val filename: String
)