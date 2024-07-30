package de.tfelix.evmbitstream.payment

import java.lang.RuntimeException

class InvalidPaymentException(message: String) : RuntimeException(message)