package de.tfelix.evmbitstream.blockchain

interface Wallet {
    fun address(): String
    fun sign(message: String): String
    fun isValidSignature(signature: String, originalMessage: String): Boolean
}