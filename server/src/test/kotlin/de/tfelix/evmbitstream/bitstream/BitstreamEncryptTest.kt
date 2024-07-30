package de.tfelix.evmbitstream.bitstream

import de.tfelix.evmbitstream.bitstream.signature.ECDSASigner
import de.tfelix.evmbitstream.blockchain.MockWallet
import de.tfelix.evmbitstream.blockchain.WalletConfig
import de.tfelix.evmbitstream.util.toHex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import java.io.ByteArrayInputStream

class BitstreamEncryptTest {

    private lateinit var sut: BitstreamEncrypt

    private val expectedFileId = "0xe65a22a7f22c33a512ae9c6dcf949847679ee4c4f1eafa5b15d1389b77996c62"
    private val chunkSplitter = FileChunkSplitter()
    private val privateKey = "503f38a9c967ed597e47fe25643985f032b072db8075426a92110f82df48dfcb"
    private val merkleTree = Sha256MerkleTree()
    private val wallet = MockWallet(WalletConfig(privateKey))

    @BeforeEach
    fun setup() {
        sut = BitstreamEncrypt(
            signer = ECDSASigner(Credentials.create(privateKey)),
            fileSplitter = chunkSplitter,
            merkleTree = merkleTree,
        )
    }

    @Test
    fun `encrypt produces verifiable results for regular files`() {
        val fileData = this.javaClass.getResource("/lorem.txt")!!
        val fileStream = fileData.openStream()

        val preimageData = BitstreamEncrypt.PreImageData.test()
        val result = sut.encrypt(fileStream, preimageData)

        val chunks = chunkSplitter.splitFileIntoChunks(ByteArrayInputStream(result.encryptedFile)).toList()

        Assertions.assertEquals(38, chunks.size)

        val hashedChunks = chunks.filterIndexed { index, _ -> index % 2 == 0 }

        val encryptedId = merkleTree.getRoot(chunks)
        Assertions.assertEquals(encryptedId.toHex(), result.encryptedId.toHex())

        // Verify hash of fileId
        val resultFileId = merkleTree.getRoot(hashedChunks).toHex()
        Assertions.assertEquals(expectedFileId, resultFileId)

        // Verify signed claim of encId and fileId
        val claim = encryptedId + preimageData.preImageHash
        val isValidSignature = wallet.isValidSignature(result.signature.toHex(), claim.toHex())
        Assertions.assertTrue(isValidSignature)
    }

    @Test
    fun `encrypt produces verifiable results for small examples`() {
        val fileStream = ByteArrayInputStream("Hello".toByteArray())

        val preimageData = BitstreamEncrypt.PreImageData.test()
        val result = sut.encrypt(fileStream, preimageData)

        val chunks = chunkSplitter.splitFileIntoChunks(ByteArrayInputStream(result.encryptedFile)).toList()

        Assertions.assertEquals(2, chunks.size)

        val hashedChunks = chunks.filterIndexed { index, _ -> index % 2 == 0 }

        val encryptedId = merkleTree.getRoot(chunks)
        Assertions.assertEquals(encryptedId.toHex(), result.encryptedId.toHex())

        // Verify hash of fileId
        val resultFileId = merkleTree.getRoot(hashedChunks).toHex()
        Assertions.assertEquals(
            "0xbb8ece46d0814b21d57f0317624be680c335f6c6d344e06d3ea466526221e7e8",
            resultFileId
        )

        // Verify signed claim of encId and fileId
        val claim = encryptedId + preimageData.preImageHash
        val isValidSignature = wallet.isValidSignature(result.signature.toHex(), claim.toHex())
        Assertions.assertTrue(isValidSignature)
    }
}