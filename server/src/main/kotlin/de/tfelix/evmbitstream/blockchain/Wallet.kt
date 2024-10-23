package de.tfelix.evmbitstream.blockchain

import de.tfelix.evmbitstream.bitstream.signature.Signature

interface Wallet {
    fun address(): String
    fun sign(message: String): Signature

    fun isValidSignature(signatureHex: String, messageHex: String): Boolean
}