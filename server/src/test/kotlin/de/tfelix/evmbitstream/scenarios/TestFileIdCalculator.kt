package de.tfelix.evmbitstream.scenarios

import de.tfelix.evmbitstream.bitstream.FileChunkSplitter
import de.tfelix.evmbitstream.bitstream.Sha256MerkleTree
import de.tfelix.evmbitstream.util.toHex
import java.io.ByteArrayInputStream

object TestFileIdCalculator {

    private val chunkSplitter = FileChunkSplitter()
    private val merkleTree = Sha256MerkleTree()

    fun getChunks(fileContent: ByteArray): List<ByteArray> {
        return chunkSplitter.splitFileIntoChunks(ByteArrayInputStream(fileContent)).toList()
    }

    fun calculateFileId(fileContent: ByteArray): String {
        val chunks = getChunks(fileContent)
        val chunkHashes = chunks.filterIndexed { index, _ -> index % 2 == 0 }

        return merkleTree.getRoot(chunkHashes).toHex()
    }
}