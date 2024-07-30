package de.tfelix.evmbitstream.bitstream

import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class FileChunkSplitter(
    private val chunkSizeBytes: Int = 32
) {
    fun splitFileIntoChunks(fileInputStream: InputStream): Sequence<ByteArray> {
        val bufferedStream = fileInputStream.buffered()
        val buffer = ByteArray(chunkSizeBytes)

        return generateSequence {
            val red = bufferedStream.read(buffer)
            if (red >= 0) buffer.copyOf(red)
            else {
                bufferedStream.close()
                null
            }
        }
    }
}