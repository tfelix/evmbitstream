package de.tfelix.evmbitstream.blockchain

import java.math.BigInteger

interface PaymentChannel {
    /**
     * Returns the current client side balance of the channel.
     *
     * @param channelId The channel ID which is queried.
     * @throws ChannelDoesNotExistException If the channel does not exist.
     */
    fun getClientBalance(channelId: String): BigInteger

    fun getTotalBalance(channelId: String): BigInteger

    fun getClientAddress(channelId: String): String
}

