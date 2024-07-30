package de.tfelix.evmbitstream.bitstream.signature

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials

class ECDSASignerTest {

    private val privateKey = "503f38a9c967ed597e47fe25643985f032b072db8075426a92110f82df48dfcb"

    private val sut = ECDSASigner(Credentials.create(privateKey))

    @Test
    fun `created signatures are valid`() {
        val message = "Hello World".toByteArray()
        val signature = sut.sign(message)

        val verifier = ECDSAVerifier()
        val result = verifier.isValidSignature(signature, message, "0x5b38da6a701c568545dcfcb03fcb875f56beddc4")

        assertTrue(result)
    }
}