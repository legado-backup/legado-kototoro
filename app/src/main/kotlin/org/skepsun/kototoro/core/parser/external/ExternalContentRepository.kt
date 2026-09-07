package org.skepsun.kototoro.core.parser.external

import android.content.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.skepsun.kototoro.core.cache.MemoryContentCache
import org.skepsun.kototoro.core.parser.CachingContentRepository
import org.skepsun.kototoro.core.util.ext.printStackTraceDebug
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.parsers.model.ContentListFilter
import org.skepsun.kototoro.parsers.model.ContentListFilterCapabilities
import org.skepsun.kototoro.parsers.model.ContentListFilterOptions
import org.skepsun.kototoro.parsers.model.ContentPage
import org.skepsun.kototoro.parsers.model.SortOrder
import java.util.EnumSet

class ExternalContentRepository(
    contentResolver: ContentResolver,
    override val source: ExternalContentSource,
    cache: MemoryContentCache,
) : CachingContentRepository(cache) {

    private val contentSource = ExternalPluginContentSource(contentResolver, source)

    private val capabilities by lazy {
        runCatching {
            contentSource.getCapabilities()
        }.onFailure {
            it.printStackTraceDebug()
        }.getOrNull()
    }

    @Volatile
    private var filterOptionsCache: ContentListFilterOptions? = null
    private val filterOptionsMutex = Mutex()

    override val sortOrders: Set<SortOrder>
        get() = capabilities?.availableSortOrders ?: EnumSet.of(SortOrder.POPULARITY)

    override val filterCapabilities: ContentListFilterCapabilities
        get() = capabilities?.listFilterCapabilities ?: ContentListFilterCapabilities()

    override var defaultSortOrder: SortOrder
        get() = capabilities?.availableSortOrders?.firstOrNull() ?: SortOrder.ALPHABETICAL
        set(value) = Unit

    override suspend fun getFilterOptions(): ContentListFilterOptions {
        filterOptionsCache?.let { return it }
        return filterOptionsMutex.withLock {
            filterOptionsCache?.let { return it }
            contentSource.getListFilterOptions().also { filterOptionsCache = it }
        }
    }

    override fun invalidateFilterOptions() {
        filterOptionsCache = null
    }

    override suspend fun getList(offset: Int, order: SortOrder?, filter: ContentListFilter?): List<Content> =
        runInterruptible(Dispatchers.IO) {
            contentSource.getList(offset, order ?: defaultSortOrder, filter ?: ContentListFilter.EMPTY)
        }

    override suspend fun getDetailsImpl(manga: Content): Content = runInterruptible(Dispatchers.IO) {
        contentSource.getDetails(manga)
    }

    override suspend fun getPagesImpl(chapter: ContentChapter, nextChapterUrl: String?): List<ContentPage> = runInterruptible(Dispatchers.IO) {
        contentSource.getPages(chapter)
    }

override suspend fun getPageUrl(page: ContentPage): String = runInterruptible(Dispatchers.IO) {
    contentSource.getPageUrl(page.url)
}

    override suspend fun getRelatedContentImpl(seed: Content): List<Content> = emptyList() // TODO
}
