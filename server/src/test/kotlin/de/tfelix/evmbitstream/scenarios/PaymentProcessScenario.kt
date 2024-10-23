package de.tfelix.evmbitstream.scenarios

import com.fasterxml.jackson.databind.ObjectMapper
import de.tfelix.evmbitstream.bitstream.signature.Signature
import de.tfelix.evmbitstream.blockchain.MockPaymentChannel
import de.tfelix.evmbitstream.blockchain.Wallet
import de.tfelix.evmbitstream.payment.PaymentConfig
import de.tfelix.evmbitstream.payment.PaymentController
import de.tfelix.evmbitstream.storage.FileStore
import de.tfelix.evmbitstream.util.toHex
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.util.MimeTypeUtils
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import java.time.Instant

/**
 * Test of a full run of fetching a file and payment flow.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentProcessScenario : BaseMVCScenario() {

    private lateinit var expectedFileId: String
    private val wrongFileId = "0x0000000000000000000000000000000000000000000000000000000000000000"

    private val mapper = ObjectMapper()
    private val privateKey = "503f38a9c967ed597e47fe25643985f032b072db8075426a92110f82df48dfcb"

    @Autowired
    private lateinit var wallet: Wallet

    @Autowired
    private lateinit var fileStore: FileStore

    @Autowired
    private lateinit var mockPaymentChannel: MockPaymentChannel

    @Autowired
    private lateinit var paymentConfig: PaymentConfig

    private val correctChannelId = "0xfd5a7c0f62f2f5673ed6e7c7bf517f8d9cc4ff7db708f97abe318e242110afbb"

    @BeforeAll
    fun setup() {
        val payload = "Hello World!".toByteArray()
        expectedFileId = TestFileIdCalculator.calculateFileId(payload)

        val fileToStore = FileStore.StoreFile(
            fileId = expectedFileId,
            filename = "text.txt",
            mime = MimeTypeUtils.TEXT_PLAIN_VALUE,
            data = payload
        )
        fileStore.storeFile(fileToStore)

        expectedFileId = TestFileIdCalculator.calculateFileId(fileToStore.data)
    }

    @Test
    @Order(1)
    fun `GET with unknown file ID results in 404`() {
        mvc.perform(get("/v1/download/$wrongFileId"))
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(2)
    fun `GET with missing or wrong payment info responds with HTTP payment required with payment info`() {
        mvc.perform(get("/v1/download/$expectedFileId"))
            .andExpect(status().isPaymentRequired)
            .andExpect(header().string("X-Bitstream-Recipient", wallet.address()))
            .andExpect(header().string("X-Bitstream-Version", "1"))
            .andExpect(header().string("X-Bitstream-Token", paymentConfig.paymentTokenAddress))
            .andExpect(header().string("X-Bitstream-Amount", "12000001000000"))
    }

    @Test
    @Order(3)
    fun `GET with an unknown channel ID returns HTTP payment required with payment info`() {
        mockPaymentChannel.channelFundResponse = null

        mvc.perform(
            get("/v1/download/$expectedFileId")
                .header("X-Bitstream-Use-Channel", "unknown-channel-id")
        )
            .andExpect(status().isPaymentRequired)
            .andExpect(header().string("X-Bitstream-Recipient", wallet.address()))
            .andExpect(header().string("X-Bitstream-Version", "1"))
            .andExpect(header().string("X-Bitstream-Token", paymentConfig.paymentTokenAddress))
            .andExpect(header().string("X-Bitstream-Amount", "12000001000000"))
    }

    @Test
    @Order(4)
    fun `GET with an known channel ID and to little channel balance returns HTTP payment required with payment info`() {
        mockPaymentChannel.channelFundResponse = 10_000

        mvc.perform(
            get("/v1/download/$expectedFileId")
                .header("X-Bitstream-Use-Channel", correctChannelId)
        )
            .andExpect(status().isPaymentRequired)
            .andExpect(header().string("X-Bitstream-Recipient", wallet.address()))
            .andExpect(header().string("X-Bitstream-Version", "1"))
            .andExpect(header().string("X-Bitstream-Token", paymentConfig.paymentTokenAddress))
            .andExpect(header().string("X-Bitstream-Amount", "12000001000000"))
    }

    private lateinit var respondedSecret: String

    @Test
    @Order(5)
    fun `GET with a known channel ID returns starts payment flow`() {
        mockPaymentChannel.channelFundResponse = 50_000_000_000_000

        val response = mvc.perform(
            get("/v1/download/$expectedFileId")
                .header("X-Bitstream-Use-Channel", correctChannelId)
        )

        response.andExpect(status().isOk)
            .andExpect(header().exists("X-Bitstream-Secret"))
            .andExpect(header().exists("X-Bitstream-Sig"))
            .andExpect(header().string("X-Bitstream-Version", "1"))
            .andExpect(header().string("X-Bitstream-Token", paymentConfig.paymentTokenAddress))
            .andExpect(header().string("X-Bitstream-Amount", "12000001000000"))
            .andExpect(header().string("X-Bitstream-File-Mime", "text/plain"))
            .andExpect(header().string("X-Bitstream-File-Name", "text.txt"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))

        respondedSecret = response.andReturn().response.getHeader("X-Bitstream-Secret")?.toString()!!
    }

    @Test
    @Order(6)
    fun `when the requested amount is payed the pre-image is released`() {
        mockPaymentChannel.channelFundResponse = 50_000_000_000_000

        val requestPayload = PaymentController.ChannelStateUpdate(
            channelId = correctChannelId,
            clientAmount = "37999999000000",
            serverAmount = "0",
            sequence = 1,
            htlc = PaymentController.ChannelStateUpdate.Htlc(
                inflightAmount = "12000001000000",
                lockedUntil = Instant.now().plusSeconds(6000).toEpochMilli(),
                secret = respondedSecret
            ),
        )

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
            "message" to requestPayload
        )

        val eip712TypedDataString = mapper.writeValueAsString(eip712TypedData)

        val credentials = Credentials.create(privateKey)
        mockPaymentChannel.clientAddressResponse = credentials.address
        val signData = Sign.signTypedData(
            eip712TypedDataString,
            credentials.ecKeyPair
        )
        val signature = Signature.fromSignatureData(signData)

        val signedRequestPayload = PaymentController.ChannelStateUpdateSignedRequest(
            update = requestPayload,
            signature = signature.toByteArray().toHex()
        )

        // TODO perform all sorts of error checks: too low lock time, too low inflight payment amount
        //  another payment attempt while a HTLC is still in flight (possibly not supported for new and needs to
        //  consolidate first.

        val response = mvc.perform(
            post("/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(signedRequestPayload))
        )

        response.andExpect(status().isOk)
    }

    @Test
    @Order(7)
    fun `when the client consolidates the htlc in a wrong the server denies it`() {

    }

    @Test
    @Order(8)
    fun `when the client consolidates the htlc the server accepts it`() {

    }

    @Test
    @Order(9)
    fun `when client does not consolidate htlc the sever collects payment before locktime is over`() {

    }
}