package de.tfelix.evmbitstream.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.IOException

private val log = KotlinLogging.logger { }

@Component
class InMemoryFileStore : FileStore {

    private val fileStorage = mutableMapOf<String, FileStore.StoreFile>()

    override fun storeFile(file: FileStore.StoreFile) {
        fileStorage[file.fileId] = file
    }

    override fun retrieveFile(fileId: String): FileStore.StoreFile {
        return fileStorage[fileId]
            ?: throwNotFound(fileId)
    }

    override fun getFileSize(fileId: String): Long {
        return fileStorage[fileId]?.data?.size?.toLong()
            ?: throwNotFound(fileId)
    }

    override fun deleteFile(fileId: String) {
        fileStorage.remove(fileId)
    }

    private fun throwNotFound(fileId: String): Nothing {
        throw IOException("File with ID $fileId not found")
    }
}