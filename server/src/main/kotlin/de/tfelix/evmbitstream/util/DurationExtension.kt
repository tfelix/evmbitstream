package de.tfelix.evmbitstream.util

import java.time.Duration

private val BLOCK_DURATION = Duration.ofSeconds(12)

fun Duration.toBlocks(): Long {
    return dividedBy(BLOCK_DURATION)
}