package org.skepsun.kototoro.core.image

import android.graphics.Bitmap
import android.graphics.Rect
import tachiyomi.decoder.ImageDecoder
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Small adapter around Mihon's native decoder for formats that Android cannot
 * decode consistently on every supported API level.
 */
internal object MihonImageDecoderCompat {

    const val FORMAT_JXL = "jxl"

    fun isJxl(bytes: ByteArray): Boolean {
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0x0A.toByte()) {
            return true
        }
        // JPEG XL container signature: a 12-byte JXL box signature.
        return bytes.size >= JXL_CONTAINER_SIGNATURE.size &&
            JXL_CONTAINER_SIGNATURE.indices.all { bytes[it] == JXL_CONTAINER_SIGNATURE[it] }
    }

    fun isJxl(bytes: ByteBuffer): Boolean {
        val headerSize = minOf(bytes.remaining(), JXL_HEADER_SIZE)
        if (headerSize < 2) return false
        val header = ByteArray(headerSize)
        bytes.duplicate().get(header)
        return isJxl(header)
    }

    fun isJxl(file: File): Boolean {
        if (!file.isFile) return false
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(JXL_HEADER_SIZE)
                val count = input.readAtMost(header)
                count >= 2 && isJxl(header.copyOf(count))
            }
        }.getOrDefault(false)
    }

    fun newInstance(input: InputStream, uri: String? = null): ImageDecoder {
        return try {
            ImageDecoder.newInstance(input)
        } catch (error: Exception) {
            throw ImageDecodeException(uri, FORMAT_JXL, cause = error)
        } ?: throw ImageDecodeException(
            uri = uri,
            format = FORMAT_JXL,
            message = "Mihon image decoder cannot open JPEG XL data",
        )
    }

    fun decode(input: InputStream, uri: String? = null): Bitmap {
        val decoder = newInstance(input, uri)
        return try {
            decoder.decode() ?: throw ImageDecodeException(
                uri = uri,
                format = FORMAT_JXL,
                message = "Mihon image decoder returned no bitmap",
            )
        } finally {
            decoder.recycle()
        }
    }

    fun decode(bytes: ByteBuffer, uri: String? = null): Bitmap =
        decode(ByteBufferInputStream(bytes.duplicate()), uri)

    fun decode(input: InputStream, region: Rect, sampleSize: Int, uri: String? = null): Bitmap {
        val decoder = newInstance(input, uri)
        return try {
            decoder.decode(region, sampleSize) ?: throw ImageDecodeException(
                uri = uri,
                format = FORMAT_JXL,
                message = "Mihon image decoder returned no bitmap",
            )
        } finally {
            decoder.recycle()
        }
    }

    private fun InputStream.readAtMost(buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val count = read(buffer, offset, buffer.size - offset)
            if (count <= 0) break
            offset += count
        }
        return offset
    }

    private class ByteBufferInputStream(buffer: ByteBuffer) : InputStream() {

        private val source = buffer.slice()

        override fun read(): Int = if (source.hasRemaining()) {
            source.get().toInt() and 0xFF
        } else {
            -1
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (offset < 0 || length < 0 || offset > buffer.size - length) {
                throw IndexOutOfBoundsException("offset=$offset length=$length size=${buffer.size}")
            }
            if (length == 0) return 0
            if (!source.hasRemaining()) return -1
            val count = length.coerceAtMost(source.remaining())
            source.get(buffer, offset, count)
            return count
        }

        override fun available(): Int = source.remaining()
    }

    private const val JXL_HEADER_SIZE = 32
    private val JXL_CONTAINER_SIGNATURE = byteArrayOf(
        0x00,
        0x00,
        0x00,
        0x0C,
        0x4A,
        0x58,
        0x4C,
        0x20,
        0x0D,
        0x0A,
        0x87.toByte(),
        0x0A,
    )
}
