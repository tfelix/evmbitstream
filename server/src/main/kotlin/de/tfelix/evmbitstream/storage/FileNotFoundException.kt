package de.tfelix.evmbitstream.storage

import de.tfelix.evmbitstream.bitstream.BitstreamException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such file")
class FileNotFoundException(fileId: String) : BitstreamException("File with ID $fileId was not found.")