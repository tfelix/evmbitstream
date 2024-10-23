package de.tfelix.evmbitstream.bitstream.signature

import org.web3j.crypto.Sign.SignatureData
import org.web3j.utils.Numeric


data class Signature(
    val r: ByteArray,
    val s: ByteArray,
    val v: ByteArray
) {

    fun toByteArray(): ByteArray {
        val data = ByteArray(65)

        System.arraycopy(r, 0, data, 0, 32)
        System.arraycopy(s, 0, data, 32, 32)
        System.arraycopy(v, 0, data, 64, 1)

        return data
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

        if (!r.contentEquals(other.r)) return false
        if (!s.contentEquals(other.s)) return false
        return v.contentEquals(other.v)
    }

    override fun hashCode(): Int {
        var result = r.contentHashCode()
        result = 31 * result + s.contentHashCode()
        result = 31 * result + v.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "Signature[r=${Numeric.toHexString(r)}, s=${Numeric.toHexString(s)}, v=${Numeric.toHexString(v)}]"
    }

    companion object {
        fun fromHexString(signature: String): Signature {
            val cleanedSignature = if (signature.startsWith("0x")) {
                signature.substring(2)
            } else {
                signature
            }

            // No need to prepend these strings with 0x because
            // Numeric.hexStringToByteArray() accepts both formats
            val r = cleanedSignature.substring(0, 64)
            val s = cleanedSignature.substring(64, 128)
            val v = cleanedSignature.substring(128, 130)

            return Signature(
                Numeric.hexStringToByteArray(r),
                Numeric.hexStringToByteArray(s),
                Numeric.hexStringToByteArray(v),
            )
        }

        fun fromSignatureData(signatureData: SignatureData): Signature {
            return Signature(
                r = signatureData.r,
                s = signatureData.s,
                v = signatureData.v
            )
        }
    }
}