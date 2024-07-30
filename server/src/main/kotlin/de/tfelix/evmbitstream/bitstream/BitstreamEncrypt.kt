package de.tfelix.evmbitstream.bitstream

import de.tfelix.evmbitstream.bitstream.signature.Signer
import de.tfelix.evmbitstream.util.concatByteArrays
import de.tfelix.evmbitstream.util.toHex
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.InputStream
import java.security.MessageDigest

private val log = KotlinLogging.logger { }

@Component
class BitstreamEncrypt(
    private val signer: Signer,
    private val fileSplitter: FileChunkSplitter,
    private val merkleTree: MerkleTree,
) {

    class PreImageData(
        val preImage: ByteArray,
        val preImageHash: ByteArray
    ) {
        init {
            require(preImage.size == 32)
        }

        // To extend it in tests
        companion object
    }

    private val hasher = MessageDigest.getInstance("SHA-256")
    private val xorEncryptor = XOREncryptor()

    fun encrypt(
        fileInputStream: InputStream,
        preImageData: PreImageData
    ): EncryptedFile {
        log.debug { "Secret Preimage: ${preImageData.preImage.toHex()}" }
        log.debug { "  Preimage Hash: ${preImageData.preImageHash.toHex()}" }

        // Split the file into chunks.
        val chunks = fileSplitter.splitFileIntoChunks(fileInputStream)

        val encryptedFile = mutableListOf<ByteArray>()
        chunks.forEachIndexed { index, chunk ->
            log.debug { "File Chunk $index: ${chunk.toHex()}" }
            val encChunk = xorEncryptor.processChunk(chunk, preImageData.preImage, index.toLong())

            encryptedFile.add(hasher.digest(chunk))
            encryptedFile.add(encChunk)
        }

        val encryptedId = merkleTree.getRoot(encryptedFile)

        log.debug { "Encrypted FileId: ${encryptedId.toHex()}" }

        // Compute the message
        val claim = encryptedId + preImageData.preImageHash
        val signature = signer.sign(claim)

        log.debug { "Signature: ${signature.toHex()}" }

        return EncryptedFile(
            signature = signature,
            encryptedId = encryptedId,
            encryptedFile = concatByteArrays(encryptedFile)
        )
    }
}

