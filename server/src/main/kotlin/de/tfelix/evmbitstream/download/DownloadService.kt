package de.tfelix.evmbitstream.download

import de.tfelix.evmbitstream.bitstream.BitstreamEncrypt
import de.tfelix.evmbitstream.bitstream.EncryptedFile
import de.tfelix.evmbitstream.payment.PaymentAmountCalculator
import de.tfelix.evmbitstream.payment.PreImagePayment
import de.tfelix.evmbitstream.payment.PreImagePaymentRepository
import de.tfelix.evmbitstream.storage.FileStore
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.time.Clock

@Service
class DownloadService(
    private val fileStore: FileStore,
    private val downloadConfig: DownloadConfig,
    private val paymentAmountCalculator: PaymentAmountCalculator,
    private val bitstreamEncrypt: BitstreamEncrypt,
    private val paymentRepository: PreImagePaymentRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val random = SecureRandom()

    fun prepareDownload(fileId: String): PreparedDownload {
        val fileSizeBytes = fileStore.getFileSize(fileId)
        val amount = paymentAmountCalculator.getPaymentAmount(fileSizeBytes)

        val preImage = generatePreimage()

        val preImagePayment = PreImagePayment.fromPreImage(preImage, amount)
        paymentRepository.save(preImagePayment)

        val file = fileStore.retrieveFile(fileId)
        val encryptedFile = getEncryptedFile(file, preImagePayment)

        val downloadValidUntil = clock.instant().plus(downloadConfig.preImageValidityAsDuration)

        return PreparedDownload(
            preImageHash = preImagePayment.hash,
            tokenAddress = downloadConfig.paymentTokenAddress,
            expectedPaymentAmount = amount,
            validUntil = downloadValidUntil,
            download = encryptedFile,
            mime = file.mime,
            filename = file.filename
        )
    }

    private fun getEncryptedFile(file: FileStore.StoreFile, preImagePayment: PreImagePayment): EncryptedFile {
        val preImageData = BitstreamEncrypt.PreImageData(
            preImage = preImagePayment.getPreImageAsByteArray(),
            preImageHash = preImagePayment.getHashAsByteArray()
        )

        return bitstreamEncrypt.encrypt(ByteArrayInputStream(file.data), preImageData)
    }

    private fun generatePreimage(): ByteArray {
        val preimage = ByteArray(32)
        random.nextBytes(preimage)

        return preimage
    }
}