package de.tfelix.evmbitstream.bitstream.signature

import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData

class ECDSAVerifier : Verifier {

    override fun isValidSignature(signature: ByteArray, originalMessage: ByteArray, address: String): Boolean {
        val pubkey = extractPubKey(signature, originalMessage)
        val signerAddress = Keys.getAddress(pubkey)

        val cleanedAddress = if (address.startsWith("0x")) {
            address.substring(2)
        } else {
            address
        }

        return signerAddress.equals(cleanedAddress, true)
    }

    private fun extractPubKey(signature: ByteArray, originalMessage: ByteArray): String {
        // No need to prepend these strings with 0x because
        // Numeric.hexStringToByteArray() accepts both formats
        val r = signature.copyOfRange(0, 32)
        val s = signature.copyOfRange(32, 64)
        val v = signature.copyOfRange(64, 65)

        // Using Sign.signedPrefixedMessageToKey for EIP-712 compliant signatures.
        return Sign.signedPrefixedMessageToKey(
            originalMessage,
            SignatureData(
                v[0],
                r,
                s
            )
        ).toString(16)
    }
}