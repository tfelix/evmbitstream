package de.tfelix.evmbitstream.download

import de.tfelix.evmbitstream.payment.PaymentException
import de.tfelix.evmbitstream.util.toHex
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream

@RestController
@RequestMapping("v1/download")
class DownloadController(
    private val downloadOperation: DownloadOperation
) {

    @GetMapping("/{fileId}")
    fun downloadFile(
        @PathVariable("fileId") fileId: String,
        @RequestHeader headers: Map<String, String>
    ): ResponseEntity<InputStreamResource> {
        return try {
            val preparedDownload = downloadOperation.prepareDownload(fileId, headers)

            convertToResponseEntity(preparedDownload)
        } catch (ex: PaymentException) {
            paymentMissingResponseEntity(ex)
        }
    }

    private fun paymentMissingResponseEntity(
        ex: PaymentException
    ): ResponseEntity<InputStreamResource> {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .header("X-Bitstream-Recipient", ex.paymentRecipientAddress)
            .header("X-Bitstream-Version", "1")
            .header("X-Bitstream-Token", ex.tokenAddress)
            .header("X-Bitstream-Amount", ex.amount.toString(10))
            .body(null)
    }

    private fun convertToResponseEntity(
        preparedDownload: PreparedDownload
    ): ResponseEntity<InputStreamResource> {
        val hexSig = preparedDownload.download.signature.toHex()

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("X-Bitstream-Sig", hexSig)
            .header("X-Bitstream-Secret", preparedDownload.preImageHash)
            .header("X-Bitstream-Amount", preparedDownload.expectedPaymentAmount.toString(10))
            .header("X-Bitstream-Version", "1")
            .header("X-Bitstream-Token", preparedDownload.tokenAddress)
            .header("X-Bitstream-File-Mime", preparedDownload.mime)
            .header("X-Bitstream-File-Name", preparedDownload.filename)
            .body(InputStreamResource(ByteArrayInputStream(preparedDownload.download.encryptedFile)))
    }
}