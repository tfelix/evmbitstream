package de.tfelix.evmbitstream.exception

class InvalidRequestResponse(
    message: String,
    val errors: Map<String, Set<String>>
): ErrorResponse(message)