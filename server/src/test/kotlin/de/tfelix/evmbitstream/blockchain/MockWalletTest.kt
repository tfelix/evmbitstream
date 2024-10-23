package de.tfelix.evmbitstream.blockchain

import de.tfelix.evmbitstream.util.toHex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MockWalletTest {

    private val sut = MockWallet(WalletConfig("503f38a9c967ed597e47fe25643985f032b072db8075426a92110f82df48dfcb"))

    @Test
    fun `sign is producing valid signatures`() {
        val message = "Hello World-Test 123"
        val signature = sut.sign(message).toByteArray().toHex()

        assertEquals(
            "0x22b20d22239d49d34c3e1e19829ff6f92dcfd67310d4b5bdd6d4c7556584ee8732c06e21010e91386c1ca49c7662fe968a8ebbed8af3458928a361890d1bea281b",
            signature
        )
    }
}