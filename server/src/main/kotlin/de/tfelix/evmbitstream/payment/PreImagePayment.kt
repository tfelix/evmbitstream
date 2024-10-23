package de.tfelix.evmbitstream.payment

import de.tfelix.evmbitstream.blockchain.BlockchainException
import de.tfelix.evmbitstream.util.toHex
import jakarta.persistence.*
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.MessageDigest

@Entity
@Table(
    indexes = [
        Index(columnList = "preimage", unique = true),
        Index(columnList = "hash", unique = true)
    ]
)
class PreImagePayment(
    @Column(nullable = false)
    val preimage: String,

    @Column(nullable = false)
    val hash: String,

    @Column(nullable = false)
    val paymentAmount: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
) {

    fun getPreImageAsByteArray(): ByteArray {
        return Numeric.hexStringToByteArray(preimage)
    }

    fun getHashAsByteArray(): ByteArray {
        return Numeric.hexStringToByteArray(hash)
    }

    @PrePersist
    fun verifyPreImage() {
        val dataBytes = Numeric.hexStringToByteArray(preimage)
        val hashedPreImage = HASHER.digest(dataBytes).toHex()
        if (hashedPreImage != hash) {
            throw BlockchainException(
                "Hashed preImage ($hashedPreImage) does not match saved $preimage in PreImagePayment(id=$id)"
            )
        }
    }

    companion object {
        private val HASHER = MessageDigest.getInstance("SHA-256")

        fun fromPreImage(
            preimage: ByteArray,
            paymentAmount: BigInteger,
        ): PreImagePayment {
            require(preimage.size == 32)

            return PreImagePayment(
                preimage = preimage.toHex(),
                hash = HASHER.digest(preimage).toHex(),
                paymentAmount = paymentAmount.toString(10)
            )
        }
    }
}