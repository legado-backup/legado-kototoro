package org.skepsun.kototoro.local.ui.compose

import androidx.work.WorkInfo
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.download.ui.list.DownloadItemModel
import org.skepsun.kototoro.download.ui.worker.DownloadTaskKind
import java.time.Instant
import java.util.UUID

class LocalDownloadSummaryTest {
    @Test
    fun `finished history does not keep the task card expanded`() {
        val summary = summarizeLocalDownloads(listOf(
            task(1, WorkInfo.State.SUCCEEDED),
            task(2, WorkInfo.State.CANCELLED),
        ))

        summary.pendingCount shouldBe 0
        summary.featured shouldBe null
        summary.canPause shouldBe false
        summary.canResume shouldBe false
    }

    @Test
    fun `mixed queue counts each task once and shows the running task`() {
        val running = task(5, WorkInfo.State.RUNNING)
        val summary = summarizeLocalDownloads(listOf(
            task(1, WorkInfo.State.SUCCEEDED),
            task(2, WorkInfo.State.FAILED),
            task(3, WorkInfo.State.RUNNING, paused = true),
            task(4, WorkInfo.State.ENQUEUED),
            running,
            task(6, WorkInfo.State.BLOCKED),
        ))

        summary.running shouldBe 1
        summary.queued shouldBe 2
        summary.paused shouldBe 1
        summary.failed shouldBe 1
        summary.pendingCount shouldBe 5
        summary.featured shouldBe running
        summary.canPause shouldBe true
        summary.canResume shouldBe true
    }

    @Test
    fun `failure stays actionable when there is no running task`() {
        val failed = task(2, WorkInfo.State.FAILED)
        val summary = summarizeLocalDownloads(listOf(
            task(1, WorkInfo.State.RUNNING, paused = true),
            failed,
            task(3, WorkInfo.State.ENQUEUED),
        ))

        summary.featured shouldBe failed
        summary.canPause shouldBe false
        summary.canResume shouldBe true
    }

    @Test
    fun `queue only has no ineffective pause or resume actions`() {
        val summary = summarizeLocalDownloads(listOf(task(1, WorkInfo.State.ENQUEUED)))

        summary.pendingCount shouldBe 1
        summary.canPause shouldBe false
        summary.canResume shouldBe false
    }

    @Test
    fun `task order and progress updates do not rotate the featured artwork`() {
        val older = task(1, WorkInfo.State.RUNNING)
        val newer = task(2, WorkInfo.State.RUNNING)
        val before = summarizeLocalDownloads(listOf(newer, older))
        val after = summarizeLocalDownloads(listOf(older.copy(progress = 70), newer.copy(progress = 90)))

        before.featured?.id shouldBe older.id
        after.featured?.id shouldBe older.id
    }

    @Test
    fun `active list keeps running first and stays actionable-first`() {
        val running = task(5, WorkInfo.State.RUNNING)
        val failed = task(2, WorkInfo.State.FAILED)
        val paused = task(3, WorkInfo.State.RUNNING, paused = true)
        val queued = task(4, WorkInfo.State.ENQUEUED)
        val summary = summarizeLocalDownloads(listOf(queued, paused, failed, running))

        summary.active.map { it.id } shouldBe listOf(
            running.id, failed.id, paused.id, queued.id,
        )
        summary.featured shouldBe running
    }

    @Test
    fun `recent history lists only succeeded tasks newest first`() {
        val old = task(1, WorkInfo.State.SUCCEEDED)
        val new = task(7, WorkInfo.State.SUCCEEDED)
        val summary = summarizeLocalDownloads(
            listOf(
                old,
                task(3, WorkInfo.State.CANCELLED),
                new,
                task(4, WorkInfo.State.FAILED),
            ),
        )

        summary.recent.map { it.id } shouldBe listOf(new.id, old.id)
    }

    private fun task(id: Long, state: WorkInfo.State, paused: Boolean = false) = DownloadItemModel(
        id = UUID(id, id),
        workState = state,
        isIndeterminate = false,
        isPaused = paused,
        taskKind = DownloadTaskKind.DOWNLOAD,
        executionManga = null,
        displayManga = null,
        error = null,
        max = 100,
        progress = 30,
        eta = 0L,
        isStuck = false,
        timestamp = Instant.ofEpochSecond(id),
        chaptersDownloaded = 0,
        isExpanded = false,
        chapters = MutableStateFlow(null),
    )
}
