package de.tfelix.evmbitstream.scenarios

import de.tfelix.evmbitstream.bitstream.BitstreamDecrypt
import de.tfelix.evmbitstream.bitstream.FileChunkSplitter
import de.tfelix.evmbitstream.bitstream.Sha256MerkleTree
import de.tfelix.evmbitstream.blockchain.MockPaymentContract
import de.tfelix.evmbitstream.util.toHex
import de.tfelix.evmbitstream.blockchain.Wallet
import de.tfelix.evmbitstream.payment.PaymentCollectorService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.web3j.utils.Numeric
import java.io.ByteArrayInputStream
import java.security.MessageDigest

/**
 *
 * User locked funds dBlocks
 * User sends pre-image to server + signature payment
 * Server encrypts file with the users preimage
 * Server collects user funds, must wait n blocks
 * User has time to raise fraud proof during n blocks
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploadAndDownloadScenario : BaseMVCScenario() {

    private val chunkSplitter = FileChunkSplitter()
    private val expectedFileId = "0x0566f866cc3dd9ec77dc59786f0d13e6be8e8e4865325e8865646ceebf567609"
    private val merkleTree = Sha256MerkleTree()
    private val expectedFileContent = "Hello, World!"
    private val hasher = MessageDigest.getInstance("SHA-256")

    @Autowired
    private lateinit var wallet: Wallet

    @Autowired
    private lateinit var paymentCollectorService: PaymentCollectorService

    @Autowired
    private lateinit var paymentContract: MockPaymentContract

    @Autowired
    private lateinit var bitstreamDecrypt: BitstreamDecrypt

    private var paymentHash: String? = null
    private var amount: String? = null
    private var fileContent: ByteArray? = null

    @Test
    @Order(1)
    fun `GET with missing or wrong payment info responds with payment info`() {
       mvc.perform(get("/v1/download/$expectedFileId"))
            .andExpect(status().isPaymentRequired)
        // Which token?
        // Which contract?
    }

    @Test
    @Order(2)
    fun `GET of unknown file returns 404`() {
        mvc.perform(
            get("/v1/download/$expectedFileId")
                .header("X-Bitstream-Payment", "abc")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(3)
    fun `file upload works and returns valid file hash`() {
        val file = MockMultipartFile(
            "file",
            "hello.txt",
            MediaType.TEXT_PLAIN_VALUE,
            expectedFileContent.toByteArray()
        )

        mvc.perform(multipart("/v1/upload").file(file))
            .andExpect(status().isCreated)
            .andExpect(header().string("location", "http://localhost/v1/download/$expectedFileId"))
    }

    @Test
    @Order(4)
    fun `file upload works with existing proof of deposit`() {
        val a = mvc.perform(
            get("/v1/download/$expectedFileId")
                .header("X-Bitstream-Payment", "abc")
        )
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Bitstream-Sig"))
            .andExpect(header().exists("X-Bitstream-Pay-Hash"))
            .andExpect(header().string("X-Bitstream-File-Mime", "text/plain"))
            // 13 bytes of data with the minimum amount is this value
            .andExpect(header().string("X-Bitstream-Amount", "300000000000000000"))
            .andExpect(header().string("X-Bitstream-File-Name", "hello.txt"))

        val response = a.andReturn().response
        fileContent = response.contentAsByteArray

        amount = response.getHeader("X-Bitstream-Amount")
        val serverSignature = response.getHeader("X-Bitstream-Sig")!!
        paymentHash = response.getHeader("X-Bitstream-Pay-Hash")

        // Verify file id
        val chunks = chunkSplitter.splitFileIntoChunks(ByteArrayInputStream(fileContent)).toList()
        val chunkHashes = chunks.filterIndexed { index, _ -> index % 2 == 0 }

        // Children are already hashed, so we don't need to hash them again.
        val responseFileId = merkleTree.getRoot(chunkHashes).toHex()

        Assertions.assertEquals(expectedFileId, responseFileId)

        // Verify server signature claim
        val encryptionId = merkleTree.getRoot(chunks).toHex()
        val claim = Numeric.hexStringToByteArray(encryptionId) + Numeric.hexStringToByteArray(paymentHash)
        val isSigValid = wallet.isValidSignature(serverSignature, claim.toHex())
        Assertions.assertTrue(isSigValid, "Signature is not valid")
    }

    @Test
    @Order(5)
    fun `when server detects client payment it releases the preimage of the file`() {
        paymentContract.clear()

        // We "perform" the payment by simulating the events.
        paymentCollectorService.collectPayment(paymentHash!!, amount!!)

        val preimage = paymentContract.getCollectedPayments().last()

        // Decrypt chunks of the file
        val decryptedFile = bitstreamDecrypt.decrypt(
            ByteArrayInputStream(fileContent!!),
            Numeric.hexStringToByteArray(preimage)
        )

        // Verify the decrypted file
        val chunks = chunkSplitter.splitFileIntoChunks(ByteArrayInputStream(decryptedFile))
            .map { hasher.digest(it) }
            .toList()
        val fileId = merkleTree.getRoot(chunks).toHex()

        val decryptedFileContent = String(decryptedFile)

        Assertions.assertEquals(expectedFileContent, decryptedFileContent)
        Assertions.assertEquals(expectedFileId, fileId)
    }
}