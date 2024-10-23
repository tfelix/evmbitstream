package de.tfelix.evmbitstream.blockchain

import de.tfelix.evmbitstream.payment.PaymentConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.lang.IllegalStateException
import java.math.BigInteger

private val log = KotlinLogging.logger { }

@Component
class MockPaymentChannel(
    private val config: PaymentConfig
) : PaymentChannel {

    private val collectedPayments = mutableListOf<String>()

    var channelFundResponse: Long? = null
    var clientAddressResponse: String? = null

    fun clear() {
        collectedPayments.clear()
    }

    fun getCollectedPayments(): List<String> {
        return collectedPayments.toList()
    }

    override fun getClientBalance(channelId: String): BigInteger {
        return channelFundResponse?.let { BigInteger.valueOf(it) }
            ?: throw ChannelDoesNotExistException(channelId)
    }

    override fun getTotalBalance(channelId: String): BigInteger {
        return channelFundResponse?.let { BigInteger.valueOf(it) }
            ?: throw ChannelDoesNotExistException(channelId)
    }

    override fun getClientAddress(channelId: String): String {
        return clientAddressResponse ?: throw IllegalStateException("Client address response not set")
    }
}