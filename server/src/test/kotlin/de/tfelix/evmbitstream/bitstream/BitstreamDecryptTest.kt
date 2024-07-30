package de.tfelix.evmbitstream.bitstream

import de.tfelix.evmbitstream.bitstream.signature.ECDSASigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import java.io.ByteArrayInputStream
import java.util.*

class BitstreamDecryptTest {

    private val chunkSplitter = FileChunkSplitter()
    private val privateKey = "503f38a9c967ed597e47fe25643985f032b072db8075426a92110f82df48dfcb"

    private val bitstreamEncrypt = BitstreamEncrypt(
        signer = ECDSASigner(Credentials.create(privateKey)),
        fileSplitter = chunkSplitter,
        merkleTree = Sha256MerkleTree()
    )

    private val sut = BitstreamDecrypt(chunkSplitter)

    @Test
    fun `decrypt can decrypt a small valid bitstream file`() {
        val fileContent = "Hello Bitstream!"
        val preImageData = BitstreamEncrypt.PreImageData.test()
        val encrypt = bitstreamEncrypt.encrypt(ByteArrayInputStream(fileContent.toByteArray()), preImageData)

        val base64EncFile = Base64.getEncoder().encodeToString(encrypt.encryptedFile)
        println(base64EncFile)

        val decrypt = sut.decrypt(ByteArrayInputStream(encrypt.encryptedFile), preImageData.preImage)
        val result = String(decrypt)

        assertEquals(fileContent, result)
    }

    @Test
    fun `decrypt can decrypt a big valid bitstream file`() {
        val fileData = this.javaClass.getResource("/lorem.txt")!!
        val fileStream = fileData.openStream()
        val preImageData = BitstreamEncrypt.PreImageData.test()
        val encrypt = bitstreamEncrypt.encrypt(fileStream, preImageData)

        val decrypt = sut.decrypt(ByteArrayInputStream(encrypt.encryptedFile), preImageData.preImage)
        val result = String(decrypt)

        val fileContent = String(fileData.readBytes())
        assertEquals(fileContent, result)
    }

    // @Test
    fun `decrypt can generate valid proof when decryption fails`() {
        val fileData = this.javaClass.getResource("/lorem.txt")!!
        val fileStream = fileData.openStream()
        val preImageData = BitstreamEncrypt.PreImageData.test()
        val encrypt = bitstreamEncrypt.encrypt(fileStream, preImageData)

        // TODO Manipulate byte stream to be invalid somehow?
    }
}