package de.tfelix.evmbitstream.blockchain

import de.tfelix.evmbitstream.bitstream.signature.ECDSASigner
import de.tfelix.evmbitstream.bitstream.signature.ECDSAVerifier
import de.tfelix.evmbitstream.bitstream.signature.Signer
import de.tfelix.evmbitstream.util.toHex
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.utils.Numeric

@Component
class MockWallet(
    config: WalletConfig
) : Wallet, Signer {

    private val credentials: Credentials
    private val signer: ECDSASigner
    private val verifier = ECDSAVerifier()

    init {
        try {
            credentials = Credentials.create(config.privateKey)
            signer = ECDSASigner(credentials)
        } catch (e: Exception) {
            throw BlockchainException("Invalid private key", e)
        }
    }

    override fun sign(messageBytes: ByteArray): ByteArray {
        return signer.sign(messageBytes)
    }

    override fun address(): String {
        return credentials.address
    }

    override fun sign(message: String): String {
        val signature = signer.sign(message.toByteArray())

        return signature.toHex()
    }

    override fun isValidSignature(signature: String, originalMessage: String): Boolean {
        val messageBytes = if (originalMessage.startsWith("0x", ignoreCase = true)) {
            Numeric.hexStringToByteArray(originalMessage)
        } else {
            originalMessage.toByteArray()
        }

        return verifier.isValidSignature(
            Numeric.hexStringToByteArray(signature),
            messageBytes,
            credentials.address
        )
    }
}