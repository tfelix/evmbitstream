package de.tfelix.evmbitstream.download

import de.tfelix.evmbitstream.payment.InvalidPaymentException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class DownloadOperation(
    private val downloadService: DownloadService,
) {

    fun prepareDownload(
        fileId: String,
        headers: Map<String, String>
    ): PreparedDownload {
        LOG.debug { "Requested FileId: '$fileId'" }

        requireValidPaymentHeaders(headers)

        // prepare requried payment steps, SC interaction.

        return downloadService.prepareDownload(fileId)
    }

    private fun requireValidPaymentHeaders(headers: Map<String, String>) {
        if (headers["X-Bitstream-Payment"] == null) {
            throw InvalidPaymentException("X-Bitstream-Payment header is missing")
        }
    }

    companion object {
        private val LOG = KotlinLogging.logger { }
    }
}