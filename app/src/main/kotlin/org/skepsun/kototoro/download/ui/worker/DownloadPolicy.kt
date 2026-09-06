package org.skepsun.kototoro.download.ui.worker

import java.io.IOException
import kotlin.math.min

/**
 * Download behaviour that should remain safe for ordinary users.
 *
 * These limits are deliberately kept out of the regular settings screen. A source can
 * still request a slower mode through [DownloadSlowdownDispatcher], while the global
 * scheduler prevents several tasks from multiplying their own concurrency.
 */
internal object DownloadPolicy {
    const val MAX_ACTIVE_SERIES = 2
    const val IMAGE_CONCURRENCY = 3
    const val HLS_SEGMENT_CONCURRENCY = 3
    const val MAX_ATTEMPTS = 3
    const val MAX_WORK_RETRIES = 2
    const val BASE_RETRY_DELAY_MS = 2_000L
    const val MIN_SOURCE_DELAY_MS = 1_000L

    fun sourceDelayMs(legacyDelayMs: Int): Long {
        return legacyDelayMs.toLong().coerceAtLeast(MIN_SOURCE_DELAY_MS)
    }

    fun retryDelayMs(attempt: Int, serverDelayMs: Long): Long {
        if (serverDelayMs > 0L) {
            return serverDelayMs
        }
        val exponent = attempt.coerceIn(0, 2)
        return min(BASE_RETRY_DELAY_MS shl exponent, 10_000L)
    }

    fun shouldRetry(error: Throwable): Boolean = error is IOException
}
