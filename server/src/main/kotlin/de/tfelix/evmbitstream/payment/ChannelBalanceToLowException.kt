package de.tfelix.evmbitstream.payment

import java.math.BigInteger

class ChannelBalanceToLowException(
    override val paymentRecipientAddress: String,
    override val amount: BigInteger,
    override val tokenAddress: String,
) : PaymentException()