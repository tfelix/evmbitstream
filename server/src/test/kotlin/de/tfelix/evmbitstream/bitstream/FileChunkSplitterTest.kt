package de.tfelix.evmbitstream.bitstream

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class FileChunkSplitterTest {

    private val sut = FileChunkSplitter(16)

    private val testData = ByteArray(24).also {
        it.fill(0, 0, 14)
        it.fill(1, 14, 24)
    }

    @Test
    fun `splitFileIntoChunks splits the given data`() {
        val input = ByteArrayInputStream(testData)
        val chunks = sut.splitFileIntoChunks(input).toList()

        assertEquals(2, chunks.size)
        assertTrue(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1).contentEquals(chunks[0]))
        assertTrue(byteArrayOf(1, 1, 1, 1, 1, 1, 1, 1).contentEquals(chunks[1]))
    }
}