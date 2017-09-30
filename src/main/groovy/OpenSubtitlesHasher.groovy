import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import java.nio.channels.FileChannel

/**
 * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
 * checksum of the first and last 64k (even if they overlap because the file is smaller than
 * 128k).
 */
class OpenSubtitlesHasher {

    /**
     * Size of the chunks that will be hashed in bytes (64 KB)
     */
    private static final int HASH_CHUNK_SIZE = 64 * 1024


    static String computeHash(final String moviePath) {
        File movieFile = new File(moviePath)
        long size = movieFile.length()
        long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size)

        FileChannel fileChannel = new FileInputStream(movieFile).getChannel()
        long head = computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, chunkSizeForFile))
        long tail = computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile))

        return String.format("%016x", size + head + tail)
    }

    static long computeHashForChunk(ByteBuffer buffer) {

        LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer()
        long hash = 0

        while (longBuffer.hasRemaining()) {
            hash += longBuffer.get()
        }

        return hash
    }
}
