package de.tfelix.evmbitstream.bitstream.signature

import org.springframework.stereotype.Component
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData

@Component
class ECDSAVerifier : Verifier {

    private fun extractPubKey(signature: Signature, originalMessage: ByteArray): String {
        // Using Sign.signedPrefixedMessageToKey for EIP-712 compliant signatures.
        return Sign.signedPrefixedMessageToKey(
            originalMessage,
            SignatureData(
                signature.v,
                signature.r,
                signature.s
            )
        ).toString(16)
    }

    override fun getSigningAddress(signature: Signature, message: ByteArray): String {
        val pubkey = extractPubKey(signature, message)

        return "0x" + Keys.getAddress(pubkey)
    }
}