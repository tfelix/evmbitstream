package de.tfelix.evmbitstream.bitstream

interface MerkleTree {
    fun getRoot(leafs: List<ByteArray>): ByteArray
}
