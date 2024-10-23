package de.tfelix.evmbitstream.bitstream.signature

import io.github.oshai.kotlinlogging.KotlinLogging
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

private val log = KotlinLogging.logger { }

class ECDSASigner(
    private val credentials: Credentials
) : Signer {

    constructor(privateKey: String) : this(Credentials.create(privateKey))

    override fun sign(message: ByteArray): Signature {
        val signatureValue = Sign.signPrefixedMessage(message, credentials.ecKeyPair)
        val signature = Signature.fromSignatureData(signatureValue)

        log.debug {
            "Signed Message: ${Numeric.toHexString(message)} with signature: $signature"
        }

        return signature
    }

    override fun address(): String {
        return credentials.address
    }
}