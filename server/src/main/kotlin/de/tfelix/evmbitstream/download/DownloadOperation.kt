package de.tfelix.evmbitstream.download

import de.tfelix.evmbitstream.blockchain.ChannelDoesNotExistException
import de.tfelix.evmbitstream.blockchain.PaymentChannel
import de.tfelix.evmbitstream.blockchain.Wallet
import de.tfelix.evmbitstream.payment.*
import de.tfelix.evmbitstream.storage.FileStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class DownloadOperation(
    private val downloadService: DownloadService,
    private val fileStore: FileStore,
    private val paymentHeaderExtractor: PaymentHeaderExtractor,
    private val paymentChannel: PaymentChannel,
    private val paymentConfig: PaymentConfig,
    private val wallet: Wallet,
    private val paymentAmountCalculator: PaymentAmountCalculator
) {

    fun prepareDownload(
        fileId: String,
        headers: Map<String, String>
    ): PreparedDownload {
        LOG.debug { "Requested FileId: '$fileId'" }

        // check if the file exists if not this throws a 404
        val fileSize = fileStore.getFileSize(fileId)
        val requiredPaymentAmount = paymentAmountCalculator.getPaymentAmount(fileSize)

        val paymentChannelToUse = try {
            paymentHeaderExtractor.extractPaymentInfo(headers)
        } catch (ex: PaymentHeaderMissingException) {
            throw PaymentInfoMissingException(
                paymentRecipientAddress = wallet.address(),
                amount = requiredPaymentAmount,
                tokenAddress = paymentConfig.paymentTokenAddress,
                cause = ex
            )
        }

        val availableBalance = try {
            paymentChannel.getClientBalance(paymentChannelToUse)
        } catch (ex: ChannelDoesNotExistException) {
            throw PaymentInfoMissingException(
                paymentRecipientAddress = wallet.address(),
                amount = requiredPaymentAmount,
                tokenAddress = paymentConfig.paymentTokenAddress,
                cause = ex
            )
        }

        if (availableBalance < requiredPaymentAmount) {
            throw ChannelBalanceToLowException(
                paymentRecipientAddress = wallet.address(),
                amount = requiredPaymentAmount,
                tokenAddress = paymentConfig.paymentTokenAddress
            )
        }

        return downloadService.prepareDownload(fileId)
    }

    companion object {
        private val LOG = KotlinLogging.logger { }
    }
}