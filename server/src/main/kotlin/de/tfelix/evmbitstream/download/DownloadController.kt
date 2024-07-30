package de.tfelix.evmbitstream.download

import de.tfelix.evmbitstream.util.toHex
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.io.IOException

@RestController
@RequestMapping("v1/download")
class DownloadController(
    private val downloadOperation: DownloadOperation,
    private val downloadService: DownloadService,
) {

    @GetMapping("/{fileId}")
    fun downloadFile(
        @PathVariable("fileId") fileId: String,
        @RequestHeader headers: Map<String, String>
    ): ResponseEntity<InputStreamResource> {
        try {
            val preparedDownload = downloadOperation.prepareDownload(fileId, headers)

            return convertToResponseEntity(preparedDownload)
        } catch (ex: IOException) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "FileID $fileId was not found",
                ex
            )
        }
    }

    private fun convertToResponseEntity(
        preparedDownload: PreparedDownload
    ): ResponseEntity<InputStreamResource> {
        val hexSig = preparedDownload.download.signature.toHex()

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("X-Bitstream-Sig", hexSig)
            .header("X-Bitstream-Pay-Hash", preparedDownload.preImageHash)
            .header("X-Bitstream-Amount", preparedDownload.expectedPaymentAmount.toString(10))
            .header("X-Bitstream-File-Mime", preparedDownload.mime)
            .header("X-Bitstream-File-Name", preparedDownload.filename)
            .body(InputStreamResource(ByteArrayInputStream(preparedDownload.download.encryptedFile)))
    }
}