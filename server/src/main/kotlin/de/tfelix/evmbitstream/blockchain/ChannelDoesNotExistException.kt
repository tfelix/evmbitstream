package de.tfelix.evmbitstream.blockchain

import de.tfelix.evmbitstream.bitstream.BitstreamException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class ChannelDoesNotExistException(
    channelId: String
) : BitstreamException("Channel $channelId does not exist.")