package de.tfelix.evmbitstream.blockchain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MockWalletTest {

    private val sut = MockWallet(WalletConfig("503f38a9c967ed597e47fe25643985f032b072db8075426a92110f82df48dfcb"))

    @Test
    fun `sign is producing valid signatures`() {
        val message = "Hello World-Test 123"
        val signature = sut.sign(message)

        assertTrue(sut.isValidSignature(signature, message))
    }
}