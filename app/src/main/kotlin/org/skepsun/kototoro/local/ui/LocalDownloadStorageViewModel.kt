package org.skepsun.kototoro.local.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.skepsun.kototoro.local.data.LocalStorageManager
import javax.inject.Inject

/** Aggregate disk footprint of the local (downloaded) content. */
data class LocalDownloadStorageStats(
    val usedBytes: Long,
    val availableBytes: Long?,
)

/**
 * Computes the local storage footprint for the local-tab download card.
 * Directory scans can be expensive on big libraries, so the caller only
 * triggers [refresh] when there is nothing downloading (and again whenever
 * the queue settles back to idle).
 */
@HiltViewModel
class LocalDownloadStorageViewModel @Inject constructor(
    private val storageManager: LocalStorageManager,
) : ViewModel() {

    private val _stats = MutableStateFlow<LocalDownloadStorageStats?>(null)
    val stats: StateFlow<LocalDownloadStorageStats?> = _stats.asStateFlow()

    private var job: Job? = null

    fun refresh() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val used = runCatching { storageManager.computeStorageSize() }.getOrNull() ?: return@launch
            val available = runCatching { storageManager.computeAvailableSize() }.getOrNull()
            _stats.value = LocalDownloadStorageStats(usedBytes = used, availableBytes = available)
        }
    }
}
