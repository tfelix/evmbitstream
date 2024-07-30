package de.tfelix.evmbitstream.bitstream

import de.tfelix.evmbitstream.util.concatByteArrays
import de.tfelix.evmbitstream.util.toHex
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.InputStream
import java.security.MessageDigest

private val log = KotlinLogging.logger { }

@Component
class BitstreamDecrypt(
    private val fileSplitter: FileChunkSplitter,
) {
    private val hasher = MessageDigest.getInstance("SHA-256")
    private val xorEncryptor = XOREncryptor()

    fun decrypt(
        fileInputStream: InputStream,
        preimage: ByteArray
    ): ByteArray {
        val chunks = fileSplitter.splitFileIntoChunks(fileInputStream)
            .toList()

        val hashedChunks = chunks.filterIndexed { index, _ -> index % 2 == 0 }
        val encryptedChunks = chunks.filterIndexed { index, _ -> (index + 1) % 2 == 0 }

        log.debug { "Hashed Chunks:\n${hashedChunks.joinToString("\n") { it.toHex() }}" }
        log.debug { "Encrypted Chunks:\n${encryptedChunks.joinToString("\n") { it.toHex() }}" }

        require(hashedChunks.size == encryptedChunks.size)

        val decryptedChunks = encryptedChunks.mapIndexed { index, encChunk ->
            val decChunk = xorEncryptor.processChunk(encChunk, preimage, index.toLong())
            requireChunkMatch(decChunk, hashedChunks[index])

            decChunk
        }

        log.debug { "Decrypted Chunks:\n${decryptedChunks.joinToString("\n") { it.toHex() }}" }

        return concatByteArrays(decryptedChunks)
    }

    private fun requireChunkMatch(decChunk: ByteArray, hash: ByteArray) {
        val hashDecChunk = hasher.digest(decChunk)
        if (!hashDecChunk.contentEquals(hash)) {
            throw ChunkMismatchException()
        }

        // TODO generate proof and add to exception
    }
}