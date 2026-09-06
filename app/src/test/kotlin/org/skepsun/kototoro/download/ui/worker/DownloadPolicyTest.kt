package org.skepsun.kototoro.download.ui.worker

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DownloadPolicyTest {

    @Test
    fun sourceDelayCannotBeDisabledByLegacyZero() {
        DownloadPolicy.sourceDelayMs(0) shouldBe DownloadPolicy.MIN_SOURCE_DELAY_MS
    }

    @Test
    fun retryDelayGrowsButRemainsBounded() {
        DownloadPolicy.retryDelayMs(0, -1L) shouldBe 2_000L
        DownloadPolicy.retryDelayMs(1, -1L) shouldBe 4_000L
        DownloadPolicy.retryDelayMs(2, -1L) shouldBe 8_000L
        DownloadPolicy.retryDelayMs(20, -1L) shouldBe 8_000L
    }

    @Test
    fun serverRetryDelayTakesPrecedence() {
        DownloadPolicy.retryDelayMs(0, 15_000L) shouldBe 15_000L
    }

    @Test
    fun duplicateDownloadRequestsHaveTheSameIdentity() {
        val first = DownloadTask.createExecutionTask(
            executionMangaId = 42L,
            isPaused = false,
            isSilent = false,
            executionChapterIds = longArrayOf(1L, 2L),
            destination = null,
            format = null,
            allowMeteredNetwork = false,
        )
        val duplicate = DownloadTask.createExecutionTask(
            executionMangaId = 42L,
            isPaused = true,
            isSilent = true,
            executionChapterIds = longArrayOf(1L, 2L),
            destination = null,
            format = null,
            allowMeteredNetwork = false,
        )
        val differentChapters = DownloadTask.createExecutionTask(
            executionMangaId = 42L,
            isPaused = false,
            isSilent = false,
            executionChapterIds = longArrayOf(1L, 3L),
            destination = null,
            format = null,
            allowMeteredNetwork = false,
        )

        first.hasSameRequestAs(duplicate) shouldBe true
        first.hasSameRequestAs(differentChapters) shouldBe false
    }
}
