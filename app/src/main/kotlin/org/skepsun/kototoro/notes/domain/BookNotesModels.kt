package org.skepsun.kototoro.notes.domain

import org.skepsun.kototoro.parsers.model.ContentType

enum class NoteType {
    ALL,
    HIGHLIGHT,
    THOUGHT,
    BOOKMARK,
}

data class BookNotesSummary(
    val mangaId: Long,
    val title: String,
    val coverUrl: String?,
    val contentType: ContentType,
    val totalCount: Int,
    val highlightCount: Int,
    val thoughtCount: Int,
    val bookmarkCount: Int,
    val readingProgressPercent: Float?,
    val readingProgressText: String?,
    val lastUpdatedAt: Long,
)

sealed interface BookNoteItem {
    val id: Long
    val mangaId: Long
    val chapterId: Long
    val chapterIndex: Int
    val chapterTitle: String
    val createdAt: Long
    val noteType: NoteType

    data class NovelHighlight(
        override val id: Long,
        override val mangaId: Long,
        override val chapterId: Long,
        override val chapterIndex: Int,
        override val chapterTitle: String,
        val text: String,
        val note: String?,
        val startOffset: Int,
        val endOffset: Int,
        override val createdAt: Long,
        val updatedAt: Long,
    ) : BookNoteItem {
        override val noteType: NoteType
            get() = if (note.isNullOrBlank()) NoteType.HIGHLIGHT else NoteType.THOUGHT
    }

    data class BookmarkEntry(
        override val id: Long,
        override val mangaId: Long,
        override val chapterId: Long,
        override val chapterIndex: Int,
        override val chapterTitle: String,
        val page: Int,
        val imageUrl: String?,
        val percent: Float,
        override val createdAt: Long,
    ) : BookNoteItem {
        override val noteType: NoteType = NoteType.BOOKMARK
    }
}
