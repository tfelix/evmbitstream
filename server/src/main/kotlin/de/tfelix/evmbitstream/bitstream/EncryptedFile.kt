package de.tfelix.evmbitstream.bitstream

class EncryptedFile(
    val signature: ByteArray,
    val encryptedId: ByteArray,
    val encryptedFile: ByteArray
)