package de.tfelix.evmbitstream.blockchain

import de.tfelix.evmbitstream.bitstream.signature.*
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.utils.Numeric

@Component
class MockWallet(
    config: WalletConfig
) : Wallet, Signer {

    private val credentials = Credentials.create(config.privateKey)
    private val signer = ECDSASigner(credentials)
    private val verifier = ECDSAVerifier()

    override fun sign(message: ByteArray): Signature {
        return signer.sign(message)
    }

    override fun address(): String {
        return credentials.address
    }

    override fun sign(message: String): Signature {
        return signer.sign(message.toByteArray())
    }

    override fun isValidSignature(signatureHex: String, messageHex: String): Boolean {
        val signerAddress = verifier.getSigningAddress(
            Signature.fromHexString(signatureHex),
            Numeric.hexStringToByteArray(messageHex)
        )

        return credentials.address.equals(signerAddress, true)
    }
}