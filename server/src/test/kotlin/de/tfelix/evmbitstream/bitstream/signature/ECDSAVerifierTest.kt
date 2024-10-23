package de.tfelix.evmbitstream.bitstream.signature

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ECDSAVerifierTest {

    private val sut = ECDSAVerifier()

    @Test
    fun `getSigningAddress returns the signing address`() {
        val message = "Hello World".toByteArray()
        val usedAddress = "0xd36e44EFf4160F78E5088e02Fe8406D7638f73b4"
        val signature =
            "0x3d5d8ea3f8d8b61b67bc7bea3c0c18278444bc8a73f84746b8eab4906b6164983ac217f2a82ad5d8fa3cc089693e19d70fba48cb8caf074e93fb30759ac0f38f1c"

        val signerAddress = sut.getSigningAddress(Signature.fromHexString(signature), message)

        assertTrue(usedAddress.equals(signerAddress, true))
    }

    @Test
    fun `getSigningAddress does something for the wrong signature`() {
        val message = "Hello World 123".toByteArray()
        val usedAddress = "0xd36e44EFf4160F78E5088e02Fe8406D7638f73b4"
        val signature =
            "0x3d5d8ea3f8d8b61b67bc7bea3c0c18278444bc8a73f84746b8eab4906b6164983ac217f2a82ad5d8fa3cc089693e19d70fba48cb8caf074e93fb30759ac0f38f1c"

        val signerAddress = sut.getSigningAddress(Signature.fromHexString(signature), message)

        assertFalse(usedAddress.equals(signerAddress, true))
    }
}