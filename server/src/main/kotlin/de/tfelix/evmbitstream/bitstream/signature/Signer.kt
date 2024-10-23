package de.tfelix.evmbitstream.bitstream.signature

interface Signer {
    fun sign(message: ByteArray): Signature
    fun address(): String
}

