package org.skepsun.kototoro.core.image

import android.graphics.Rect
import androidx.core.graphics.scale
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.util.component1
import coil3.util.component2
import kotlinx.coroutines.runInterruptible

/** Coil decoder for static JPEG XL images. */
class JxlImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult = runInterruptible {
        val uri = source.fileOrNull()?.toString()
        val decoder = MihonImageDecoderCompat.newInstance(source.source().inputStream(), uri)
        try {
            val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
                srcWidth = decoder.width,
                srcHeight = decoder.height,
                targetSize = options.size,
                scale = options.scale,
                maxSize = options.maxBitmapSize,
            )
            val sampleSize = DecodeUtils.calculateInSampleSize(
                srcWidth = decoder.width,
                srcHeight = decoder.height,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale,
            )
            val bitmap = decoder.decode(
                region = Rect(0, 0, decoder.width, decoder.height),
                sampleSize = sampleSize,
            ) ?: throw ImageDecodeException(
                uri = uri,
                format = MihonImageDecoderCompat.FORMAT_JXL,
                message = "Mihon image decoder returned no bitmap",
            )
            val (sampledWidth, sampledHeight) = bitmap.width to bitmap.height
            if (sampledWidth != dstWidth || sampledHeight != dstHeight) {
                val scaled = try {
                    bitmap.scale(dstWidth, dstHeight)
                } finally {
                    bitmap.recycle()
                }
                DecodeResult(
                    image = scaled.asImage(),
                    isSampled = true,
                )
            } else {
                DecodeResult(
                    image = bitmap.asImage(),
                    isSampled = sampleSize > 1,
                )
            }
        } finally {
            decoder.recycle()
        }
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? = if (isApplicable(result)) {
            JxlImageDecoder(result.source, options)
        } else {
            null
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()

        private fun isApplicable(result: SourceFetchResult): Boolean {
            if (result.mimeType.equals("image/jxl", ignoreCase = true)) return true
            return runCatching {
                result.source.source().peek().use { peek ->
                    if (!peek.request(12L)) return@use false
                    val header = peek.readByteArray(minOf(32L, peek.buffer.size))
                    MihonImageDecoderCompat.isJxl(header)
                }
            }.getOrDefault(false)
        }
    }
}
