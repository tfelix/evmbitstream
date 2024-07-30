package de.tfelix.evmbitstream.bitstream.signature

interface Signer {
    fun sign(messageBytes: ByteArray): ByteArray
    fun address(): String
}

