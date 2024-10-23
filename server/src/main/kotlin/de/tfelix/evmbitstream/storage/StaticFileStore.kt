package de.tfelix.evmbitstream.storage

import de.tfelix.evmbitstream.bitstream.FileChunkSplitter
import de.tfelix.evmbitstream.bitstream.MerkleTree
import de.tfelix.evmbitstream.util.toHex
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.InputStream
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

private val log = KotlinLogging.logger { }

/**
 * This indexes and loads a bunch of files from the hard disc when the service starts up and
 * prepares to serve those files.
 */
class StaticFileStore(
    private val fileSplitter: FileChunkSplitter,
    private val merkleTree: MerkleTree,
    private val files: List<String>
) : FileStore {

    private data class FileInfo(
        val filename: String,
        val contentType: String,
        val size: Long,
        val filePath: String
    )

    private val hasher = MessageDigest.getInstance("SHA-256")
    private val fileInfoByFileId: Map<String, FileInfo>

    init {
        // Load and transform all the listed files into
        log.info { "Statically serving ${files.size} files, generating file IDs..." }

        fileInfoByFileId = files.associate { filePath ->
            val file = File(filePath)

            val fileId = file.inputStream().use {
                generateFileId(it)
            }

            fileId to FileInfo(
                size = file.length(),
                filePath = filePath,
                filename = file.name,
                contentType = Files.probeContentType(Paths.get(file.path))
            )
        }

        log.info { "Serving the following files:\n" }
        fileInfoByFileId.map {
            log.info { " - ${it.key}: ${it.value.filePath} (${it.value.size}) " }
        }
    }

    private fun generateFileId(fileInputStream: InputStream): String {
        val chunks = fileSplitter.splitFileIntoChunks(fileInputStream)
            .map { hasher.digest(it) }
            .toList()

        return merkleTree.getRoot(chunks).toHex()
    }

    override fun storeFile(file: FileStore.StoreFile) {
        throw IllegalStateException("Saving files it not supported with static file store")
    }

    override fun retrieveFile(fileId: String): FileStore.StoreFile {
        val info = fileInfoByFileId[fileId] ?: throwNotFound(fileId)

        val data = File(info.filePath).readBytes()

        return FileStore.StoreFile(
            fileId = fileId,
            filename = info.filename,
            mime = info.contentType,
            data = data
        )
    }

    override fun getFileSize(fileId: String): Long {
        return fileInfoByFileId[fileId]?.size ?: throwNotFound(fileId)
    }

    override fun deleteFile(fileId: String) {
        throw IllegalStateException("Deleting files it not supported with static file store")
    }

    private fun throwNotFound(fileId: String): Nothing {
        throw FileNotFoundException(fileId)
    }
}