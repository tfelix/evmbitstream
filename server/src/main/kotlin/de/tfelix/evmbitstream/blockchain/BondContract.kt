package de.tfelix.evmbitstream.blockchain

interface BondContract {
    fun isActive(address: String): Boolean
}
