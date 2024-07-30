package de.tfelix.evmbitstream.bitstream

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class XOREncryptorTest {

    private val sut = XOREncryptor()

    @Test
    fun `Process enc and decrypts data smaller than 32 bytes`() {
        val dataStr = "Hello World"
        val dataBytes = dataStr.toByteArray()
        val preimage = "secret-password".toByteArray()

        val encrypted = sut.processChunk(dataBytes, preimage, 0)

        assertNotEquals(dataStr, String(encrypted))

        val decrypted = sut.processChunk(encrypted, preimage, 0)

        assertEquals(dataStr, String(decrypted))
    }
}