package de.tfelix.evmbitstream.payment

import com.fasterxml.jackson.databind.ObjectMapper
import de.tfelix.evmbitstream.bitstream.signature.Signature
import de.tfelix.evmbitstream.bitstream.signature.Verifier
import de.tfelix.evmbitstream.blockchain.PaymentChannel
import org.springframework.stereotype.Service
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.*


@Service
class PaymentProcessorService(
    private val paymentChannel: PaymentChannel,
    private val preimageRepository: PreImagePaymentRepository,
    private val channelStateRepository: ChannelStateRepository,
    private val paymentConfig: PaymentConfig,
    private val verifier: Verifier,
    private val mapper: ObjectMapper
) {

    fun collectPayment(signedUpdateRequest: PaymentController.ChannelStateUpdateSignedRequest) {
        requireValidAmounts(signedUpdateRequest.update)

        // check if we have a preimage payment open for the provided secret.
        val htlc = signedUpdateRequest.update.htlc
            ?: throw InvalidPaymentData("No HTLC provided")

        val hashedPreimage = htlc.secret
        val existingPreimage = preimageRepository.findByHash(hashedPreimage)
            ?: throw InvalidPaymentData("No payment info found for this preimage")

        // check if the server balance has increased for the expected amount
        requireInflightToMatchPayment(existingPreimage, htlc)

        // check if locked until is acceptable
        requireEnoughLocktime(htlc)

        // check if sequence number is higher
        val channelState = channelStateRepository.findByChannelId(signedUpdateRequest.update.channelId)
            ?.let { existingChannelState ->
                if (existingChannelState.sequenceNumber <= signedUpdateRequest.update.sequence) {
                    throw InvalidPaymentData("Sequence number was not incremented")
                }
            } ?: ChannelState(
            sequenceNumber = signedUpdateRequest.update.sequence,
            channelId = signedUpdateRequest.update.channelId,
            lockedUntil = Instant.ofEpochMilli(htlc.lockedUntil)
        ).also { channelStateRepository.save(it) }

        // check the client signature
        // wallet.isValidSignature(serverSignature, claim.toHex())
        val signature = Signature.fromHexString(signedUpdateRequest.signature)

        val eip712DataDomain = mapOf(
            "name" to "EVMBitstream",
            "version" to "1",
            "chainId" to 1,
            "verifyingContract" to "0x0"
        )

        val eip712Types = mapOf(
            "EIP712Domain" to listOf(
                mapOf("name" to "name", "type" to "string"),
                mapOf("name" to "version", "type" to "string"),
                mapOf("name" to "chainId", "type" to "uint256"),
                mapOf("name" to "verifyingContract", "type" to "address"),
            ),
            "ChannelClose" to listOf(
                mapOf("name" to "channelId", "type" to "bytes32"),
                mapOf("name" to "serverAmount", "type" to "uint256"),
                mapOf("name" to "clientAmount", "type" to "uint256"),
                mapOf("name" to "htlc", "type" to "Htlc"),
                mapOf("name" to "sequence", "type" to "uint256"),
            ),
            "Htlc" to listOf(
                mapOf("name" to "inflightAmount", "type" to "uint256"),
                mapOf("name" to "lockedUntil", "type" to "uint256"),
                mapOf("name" to "secret", "type" to "bytes32"),
            ),
        )

        val eip712TypedData = mapOf(
            "domain" to eip712DataDomain,
            "types" to eip712Types,
            "primaryType" to "ChannelClose",
            "message" to signedUpdateRequest.update
        )

        val eip712TypedDataString = mapper.writeValueAsString(eip712TypedData)

        val dataEncoder = StructuredDataEncoder(eip712TypedDataString)
        val hashStructuredData: ByteArray = dataEncoder.hashStructuredData()

        // Step 1: Hash the EIP-712 typed data
        val message = (eip712TypedDataString.toByteArray())
        val messageHash = Sign.getEthereumMessageHash(message)
        val signatureBytes = Numeric.hexStringToByteArray(signedUpdateRequest.signature)
        val signatureData = SignatureData(
            signatureBytes[64],
            Arrays.copyOfRange(signatureBytes, 0, 32),
            Arrays.copyOfRange(signatureBytes, 32, 64)
        )
        // Step 3: Recover the public key from the message hash and signature
        val publicKey = Sign.signedMessageHashToKey(hashStructuredData, signatureData)

        // Step 4: Derive the Ethereum address from the public key
        val recoveredAddress = "0x" + Keys.getAddress(publicKey)

        println(recoveredAddress)

        val signerAddress = verifier.getSigningAddress(signature, eip712TypedDataString.toByteArray())
        val paymentChannelClientAddress = paymentChannel.getClientAddress(signedUpdateRequest.update.channelId)
        require(signerAddress.equals(paymentChannelClientAddress, false)) {
            "Signer address $signerAddress does not match client address $paymentChannelClientAddress of " +
                    "payment channel ${signedUpdateRequest.update.channelId}"
        }

        println(signerAddress)

        // save the latest channel close message, so we are safe if client publishes an older state and we
        // can punish him

        // publishes the preimage to the given secret, so client can decrypt

        // now we expect the client to consolidate the htlc at some point before the timeout runs out,
        // if he does not do so, we need to publish the preimage on chain to collect the payment before the
        // locktime runs out. But another services handles this.
    }

    private fun requireEnoughLocktime(htlc: PaymentController.ChannelStateUpdate.Htlc) {
        val lockedUntil = Instant.ofEpochMilli(htlc.lockedUntil)
        if (Duration.between(Instant.now(), lockedUntil) < paymentConfig.minHtlcTimeoutAsDuration) {
            throw InvalidPaymentData("Lock until is too small")
        }
    }

    /**
     * Verify if the channel looks like with the expected value
     */
    private fun requireValidAmounts(
        channelStateUpdateRequest: PaymentController.ChannelStateUpdate
    ) {
        val clientAmount = BigInteger(channelStateUpdateRequest.clientAmount, 10)
        val serverAmount = BigInteger(channelStateUpdateRequest.serverAmount, 10)
        val inflightAmount = BigInteger(channelStateUpdateRequest.htlc?.inflightAmount ?: "0", 10)

        val totalChannelAmount = paymentChannel.getTotalBalance(channelStateUpdateRequest.channelId)
        val sum = clientAmount + serverAmount + inflightAmount

        require(totalChannelAmount == sum)
    }

    private fun requireInflightToMatchPayment(
        existingPreimage: PreImagePayment,
        htlc: PaymentController.ChannelStateUpdate.Htlc
    ) {
        val givenPaymentAmount = BigInteger(htlc.inflightAmount)
        val expectedPaymentAmount = BigInteger(existingPreimage.paymentAmount)

        require(givenPaymentAmount == expectedPaymentAmount) {
            "Payment amount did not match $expectedPaymentAmount"
        }
    }
}