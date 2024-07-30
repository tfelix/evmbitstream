package de.tfelix.evmbitstream.bitstream

import de.tfelix.evmbitstream.util.toHex
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class Sha256MerkleTreeTest {

    private val sut = Sha256MerkleTree()

    private val initialData = listOf(
        byteArrayOf(1),
        byteArrayOf(2),
        byteArrayOf(3)
    )

    @Test
    fun `getRoot returns the proper hashed merkle root`() {
        val root = sut.getRoot(initialData)

        Assertions.assertEquals("0x9faa2a58b06fa09e3df6f260fcd26040b798fd90bfb33a759f85ef29e95ae648", root.toHex())
    }

    @Test
    fun `getRoot does not touch the argument`() {
        sut.getRoot(initialData)

        val areEqual = listOf(
            byteArrayOf(1),
            byteArrayOf(2),
            byteArrayOf(3)
        ).zip(initialData).all { (a, b) -> a.contentEquals(b) }

        Assertions.assertTrue(areEqual)
    }

    @Test
    fun `getRoot returns good date even if only one leaf initially`() {
        val root = sut.getRoot(
            listOf(
                byteArrayOf(1),
            )
        )

        Assertions.assertEquals("0xe28c8b26b936e24632469d468079a29f00a3325a104a013a21dc744d2ec35129", root.toHex())
    }
}

