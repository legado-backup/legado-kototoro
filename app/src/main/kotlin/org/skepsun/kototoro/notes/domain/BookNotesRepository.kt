package org.skepsun.kototoro.notes.domain

import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.skepsun.kototoro.core.db.MangaDatabase
import org.skepsun.kototoro.core.db.entity.toContent
import javax.inject.Inject

@Reusable
class BookNotesRepository @Inject constructor(
    private val database: MangaDatabase,
) {

    fun observeSummaries(): Flow<List<BookNotesSummary>> = combine(
        database.getNovelMarkingDao().observeAll(),
        database.getBookmarksDao().observe(),
    ) { markings, bookmarksMap ->
        val markingsByManga = markings.groupBy { it.mangaId }
        val allMangaIds = (markingsByManga.keys + bookmarksMap.keys.map { it.manga.id }).distinct()
        if (allMangaIds.isEmpty()) return@combine emptyList<BookNotesSummary>()

        val historyMap = database.getHistoryDao().findAllByMangaIds(allMangaIds).associateBy { it.mangaId }

        val missingMangaIds = allMangaIds.filter { id -> bookmarksMap.none { it.key.manga.id == id } }
        val fetchedMangaMap = if (missingMangaIds.isNotEmpty()) {
            missingMangaIds.mapNotNull { id -> database.getMangaDao().find(id) }.associateBy { it.manga.id }
        } else {
            emptyMap()
        }

        allMangaIds.mapNotNull { mangaId ->
            val mangaWithTags = bookmarksMap.keys.find { it.manga.id == mangaId } ?: fetchedMangaMap[mangaId]
            val manga = mangaWithTags?.toContent() ?: return@mapNotNull null

            val bookMarkings = markingsByManga[mangaId].orEmpty()
            val bookBookmarks = bookmarksMap[mangaWithTags].orEmpty()

            val highlightCount = bookMarkings.count { it.note.isNullOrBlank() }
            val thoughtCount = bookMarkings.count { !it.note.isNullOrBlank() }
            val bookmarkCount = bookBookmarks.size
            val totalCount = highlightCount + thoughtCount + bookmarkCount

            if (totalCount == 0) return@mapNotNull null

            val history = historyMap[mangaId]
            val progressPercent = history?.percent
            val progressText = when {
                progressPercent != null && progressPercent > 0f -> {
                    val p = (progressPercent * 100).toInt().coerceIn(0, 100)
                    "在读 · $p%"
                }
                history != null -> "在读"
                else -> null
            }

            val lastMarkingTime = bookMarkings.maxOfOrNull { it.updatedAt } ?: 0L
            val lastBookmarkTime = bookBookmarks.maxOfOrNull { it.createdAt } ?: 0L
            val lastTime = maxOf(lastMarkingTime, lastBookmarkTime)

            BookNotesSummary(
                mangaId = mangaId,
                title = manga.title,
                coverUrl = manga.coverUrl,
                contentType = manga.source.contentType,
                totalCount = totalCount,
                highlightCount = highlightCount,
                thoughtCount = thoughtCount,
                bookmarkCount = bookmarkCount,
                readingProgressPercent = progressPercent,
                readingProgressText = progressText,
                lastUpdatedAt = lastTime,
            )
        }.sortedByDescending { it.lastUpdatedAt }
    }

    fun observeBookNotes(mangaId: Long): Flow<List<BookNoteItem>> = combine(
        database.getNovelMarkingDao().observe(mangaId),
        database.getBookmarksDao().observe(mangaId),
    ) { markings, bookmarks ->
        val chapters = database.getChaptersDao().findAll(mangaId).associateBy { it.chapterId }

        val highlightItems = markings.map { m ->
            val chapter = chapters[m.chapterId]
            val chapterTitle = chapter?.title ?: "第 ${m.chapterIndex + 1} 章"
            BookNoteItem.NovelHighlight(
                id = m.id,
                mangaId = m.mangaId,
                chapterId = m.chapterId,
                chapterIndex = m.chapterIndex,
                chapterTitle = chapterTitle,
                text = m.selectedText,
                note = m.note,
                startOffset = m.startOffset,
                endOffset = m.endOffset,
                createdAt = m.createdAt,
                updatedAt = m.updatedAt,
            )
        }

        val bookmarkItems = bookmarks.map { b ->
            val chapter = chapters[b.chapterId]
            val chapterIndex = chapter?.index ?: 0
            val chapterTitle = chapter?.title ?: "第 ${b.page + 1} 页"
            BookNoteItem.BookmarkEntry(
                id = b.pageId,
                mangaId = b.mangaId,
                chapterId = b.chapterId,
                chapterIndex = chapterIndex,
                chapterTitle = chapterTitle,
                page = b.page,
                imageUrl = b.imageUrl,
                percent = b.percent,
                createdAt = b.createdAt,
            )
        }

        (highlightItems + bookmarkItems).sortedWith(
            compareBy<BookNoteItem> { it.chapterIndex }
                .thenBy { item ->
                    when (item) {
                        is BookNoteItem.NovelHighlight -> item.startOffset
                        is BookNoteItem.BookmarkEntry -> item.page
                    }
                }
                .thenBy { it.createdAt },
        )
    }

    suspend fun deleteNote(item: BookNoteItem) {
        when (item) {
            is BookNoteItem.NovelHighlight -> database.getNovelMarkingDao().deleteById(item.id)
            is BookNoteItem.BookmarkEntry -> database.getBookmarksDao().delete(item.id)
        }
    }

    suspend fun updateThoughtNote(id: Long, note: String?) {
        database.getNovelMarkingDao().updateNote(
            id = id,
            note = note,
            updatedAt = System.currentTimeMillis(),
        )
    }
}
