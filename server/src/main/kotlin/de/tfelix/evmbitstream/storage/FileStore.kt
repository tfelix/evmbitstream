package de.tfelix.evmbitstream.storage

interface FileStore {
    class StoreFile(
        val fileId: String,
        val filename: String,
        val mime: String,
        val data: ByteArray
    )

    fun storeFile(file: StoreFile)
    fun retrieveFile(fileId: String): StoreFile
    fun getFileSize(fileId: String): Long
    fun deleteFile(fileId: String)
}