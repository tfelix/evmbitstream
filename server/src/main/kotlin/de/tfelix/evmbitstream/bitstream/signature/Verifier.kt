package de.tfelix.evmbitstream.bitstream.signature

interface Verifier {
    fun isValidSignature(signature: ByteArray, originalMessage: ByteArray, address: String): Boolean
}