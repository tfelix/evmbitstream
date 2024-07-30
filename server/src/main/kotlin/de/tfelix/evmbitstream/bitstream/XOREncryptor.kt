package de.tfelix.evmbitstream.bitstream

import de.tfelix.evmbitstream.util.toHex
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.experimental.xor

class XOREncryptor {

    private val hasher = MessageDigest.getInstance("SHA-256")

    fun processChunk(chunk: ByteArray, preimage: ByteArray, index: Long): ByteArray {
        val secret = preimage + (index + 1).toByteArray()
        val hashBytes = hasher.digest(secret)

        println(hashBytes.toHex())

        require(chunk.size <= hashBytes.size)

        // XOR each byte of the chunk with the corresponding byte of the hash
        return ByteArray(chunk.size) { i -> chunk[i] xor hashBytes[i] }
    }

    private fun Long.toByteArray(): ByteArray = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()
}