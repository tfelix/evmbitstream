package de.tfelix.evmbitstream.bitstream.signature

import io.github.oshai.kotlinlogging.KotlinLogging
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

private val log = KotlinLogging.logger { }

class ECDSASigner(
    private val credentials: Credentials
) : Signer {
    override fun sign(messageBytes: ByteArray): ByteArray {
        val signature = Sign.signPrefixedMessage(messageBytes, credentials.ecKeyPair)

        val value = ByteArray(65)
        System.arraycopy(signature.r, 0, value, 0, 32)
        System.arraycopy(signature.s, 0, value, 32, 32)
        System.arraycopy(signature.v, 0, value, 64, 1)

        log.debug {
            "Signed Message: ${Numeric.toHexString(messageBytes)} with signature: ${
                Numeric.toHexString(
                    value
                )
            }"
        }

        return value
    }

    override fun address(): String {
        return credentials.address
    }
}