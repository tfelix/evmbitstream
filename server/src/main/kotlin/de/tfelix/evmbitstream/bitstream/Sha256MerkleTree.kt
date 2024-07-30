package de.tfelix.evmbitstream.bitstream

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class Sha256MerkleTree : MerkleTree {

    private val hasher = MessageDigest.getInstance("SHA-256")

    override fun getRoot(leafs: List<ByteArray>): ByteArray {
        // Hash all the leafs first.
        val hashedLeafs = leafs.map { leaf -> hasher.digest(leaf) }

        var mutableLeafs = hashedLeafs.toMutableList()

        // Make sure to at least hash once even if you only start with one leaf.
        if (mutableLeafs.size % 2 != 0) {
            mutableLeafs.add(mutableLeafs.last())
        }

        while (mutableLeafs.size > 1) {
            if (mutableLeafs.size % 2 != 0) {
                mutableLeafs.add(mutableLeafs.last())
            }

            val newLeaves = (mutableLeafs.indices step 2).map { i ->
                val combinedData = mutableLeafs[i] + mutableLeafs[i + 1]
                hasher.digest(combinedData)
            }

            mutableLeafs = newLeaves.toMutableList()
        }

        return mutableLeafs[0]
    }
}