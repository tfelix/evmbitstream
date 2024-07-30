package de.tfelix.evmbitstream.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationExtensionTest {

    @Test
    fun `toBlocks converts duration into expected block number`() {
        Assertions.assertEquals(0, Duration.ofSeconds(10).toBlocks())
        Assertions.assertEquals(1, Duration.ofSeconds(12).toBlocks())
        Assertions.assertEquals(14400, Duration.ofDays(2).toBlocks())
    }
}