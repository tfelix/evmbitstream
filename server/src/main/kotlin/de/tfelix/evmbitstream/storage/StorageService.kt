package de.tfelix.evmbitstream.storage

import de.tfelix.evmbitstream.bitstream.FileChunkSplitter
import de.tfelix.evmbitstream.bitstream.MerkleTree
import de.tfelix.evmbitstream.util.toHex
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import org.springframework.web.multipart.MultipartFile
import java.security.MessageDigest

private val log = KotlinLogging.logger { }

@Service
class StorageService(
    private val fileSplitter: FileChunkSplitter,
    private val merkleTree: MerkleTree,
    private val fileStore: FileStore
) {

    private val hasher = MessageDigest.getInstance("SHA-256")

    fun delete(fileId: String) {
        fileStore.deleteFile(fileId)
    }

    fun store(file: MultipartFile): String {
        val fileId = generateFileId(file)

        log.debug { "Generated FileId '$fileId' for file to store" }

        val storeFile = FileStore.StoreFile(
            fileId = fileId,
            filename = file.originalFilename ?: "file",
            mime = file.contentType ?: MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE,
            data = file.bytes
        )

        fileStore.storeFile(storeFile)

        return fileId
    }

    private fun generateFileId(file: MultipartFile): String {
        val chunks = fileSplitter.splitFileIntoChunks(file.inputStream)
            .map { hasher.digest(it) }
            .toList()

        return merkleTree.getRoot(chunks).toHex()
    }
}