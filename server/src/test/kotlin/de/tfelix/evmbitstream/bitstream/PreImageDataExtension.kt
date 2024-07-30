package de.tfelix.evmbitstream.bitstream

import java.security.MessageDigest

fun BitstreamEncrypt.PreImageData.Companion.test(): BitstreamEncrypt.PreImageData {
    val hasher = MessageDigest.getInstance("SHA-256")
    val preImage = hasher.digest("MySecretImage".toByteArray())

    return BitstreamEncrypt.PreImageData(
        preImage = preImage,
        preImageHash = hasher.digest(preImage),
    )
}