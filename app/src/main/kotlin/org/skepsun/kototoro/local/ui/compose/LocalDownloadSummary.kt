package org.skepsun.kototoro.local.ui.compose

import androidx.work.WorkInfo
import org.skepsun.kototoro.download.ui.list.DownloadItemModel

internal data class LocalDownloadSummary(
    val running: Int,
    val queued: Int,
    val paused: Int,
    val failed: Int,
    /** Pending tasks (running / paused / queued / failed), most actionable first. */
    val active: List<DownloadItemModel>,
    /** Recently finished downloads, newest first, for the idle-state history. */
    val recent: List<DownloadItemModel>,
    val canPause: Boolean,
    val canResume: Boolean,
) {
    val pendingCount: Int get() = running + queued + paused + failed
    val featured: DownloadItemModel? get() = active.firstOrNull()
}

internal fun summarizeLocalDownloads(items: List<DownloadItemModel>): LocalDownloadSummary {
    val pending = items.filter { !it.workState.isFinished || it.workState == WorkInfo.State.FAILED }
    return LocalDownloadSummary(
        running = pending.count { it.workState == WorkInfo.State.RUNNING && !it.isPaused },
        queued = pending.count { it.workState == WorkInfo.State.ENQUEUED || it.workState == WorkInfo.State.BLOCKED },
        paused = pending.count { it.workState == WorkInfo.State.RUNNING && it.isPaused },
        failed = pending.count { it.workState == WorkInfo.State.FAILED },
        active = pending.sortedWith(activeOrder),
        recent = items
            .filter { it.workState == WorkInfo.State.SUCCEEDED }
            .sortedWith(
                compareByDescending<DownloadItemModel> { it.timestamp }
                    .thenByDescending { it.id },
            ),
        canPause = pending.any { it.canPause },
        canResume = pending.any { it.canResume },
    )
}

// Keep the representative task stable; progress updates must not rotate the cover.
private val activeOrder = compareBy<DownloadItemModel> {
    when {
        it.workState == WorkInfo.State.RUNNING && !it.isPaused -> 0
        it.workState == WorkInfo.State.FAILED -> 1
        it.isPaused -> 2
        else -> 3
    }
}.thenBy { it.timestamp }.thenBy { it.id }
