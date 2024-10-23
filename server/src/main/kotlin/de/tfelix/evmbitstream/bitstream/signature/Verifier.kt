package de.tfelix.evmbitstream.bitstream.signature

interface Verifier {
    fun getSigningAddress(signature: Signature, message: ByteArray): String
}