package org.skepsun.kototoro.notes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.notes.domain.BookNoteItem
import org.skepsun.kototoro.notes.domain.BookNotesSummary
import org.skepsun.kototoro.notes.domain.NoteType
import org.skepsun.kototoro.notes.ui.NotesUiState
import org.skepsun.kototoro.parsers.model.ContentType

class BookNotesAggregationTest {

    @Test
    fun `novel highlight noteType determines highlight or thought correctly`() {
        val highlightOnly = BookNoteItem.NovelHighlight(
            id = 1L,
            mangaId = 100L,
            chapterId = 10L,
            chapterIndex = 0,
            chapterTitle = "第1章",
            text = "从安稳富足的环境里顺理成章走向哲学",
            note = null,
            startOffset = 0,
            endOffset = 18,
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        assertEquals(NoteType.HIGHLIGHT, highlightOnly.noteType)

        val thought = highlightOnly.copy(
            id = 2L,
            note = "作者在第1章就给出了重要伏笔",
        )
        assertEquals(NoteType.THOUGHT, thought.noteType)
    }

    @Test
    fun `bookmark item has BOOKMARK noteType`() {
        val bookmark = BookNoteItem.BookmarkEntry(
            id = 1L,
            mangaId = 100L,
            chapterId = 10L,
            chapterIndex = 0,
            chapterTitle = "第1话",
            page = 5,
            imageUrl = "https://example.com/page5.jpg",
            percent = 0.5f,
            createdAt = 2000L,
        )
        assertEquals(NoteType.BOOKMARK, bookmark.noteType)
    }

    @Test
    fun `notes ui state computes counts and filters correctly`() {
        val summary1 = BookNotesSummary(
            mangaId = 1L,
            title = "康德：被限制，但不仅被定义的人",
            coverUrl = null,
            contentType = ContentType.NOVEL,
            totalCount = 3,
            highlightCount = 2,
            thoughtCount = 0,
            bookmarkCount = 1,
            readingProgressPercent = 0.95f,
            readingProgressText = "在读 · 95%",
            lastUpdatedAt = 5000L,
        )
        val summary2 = BookNotesSummary(
            mangaId = 2L,
            title = "明朝那些事儿",
            coverUrl = null,
            contentType = ContentType.NOVEL,
            totalCount = 1,
            highlightCount = 1,
            thoughtCount = 0,
            bookmarkCount = 0,
            readingProgressPercent = 0.01f,
            readingProgressText = "在读 · 1%",
            lastUpdatedAt = 4000L,
        )

        val state = NotesUiState(
            summaries = listOf(summary1, summary2),
            searchQuery = "",
        )

        assertEquals(4, state.totalNotesCount)
        assertEquals(2, state.totalBooksCount)
        assertEquals(2, state.filteredSummaries.size)

        val filteredState = state.copy(searchQuery = "康德")
        assertEquals(1, filteredState.filteredSummaries.size)
        assertEquals(1L, filteredState.filteredSummaries.first().mangaId)
    }

    @Test
    fun `notes ui state filters and groups book notes by chapter`() {
        val h1 = BookNoteItem.NovelHighlight(
            id = 1L,
            mangaId = 1L,
            chapterId = 10L,
            chapterIndex = 0,
            chapterTitle = "1 贫寒出身",
            text = "文本A",
            note = null,
            startOffset = 0,
            endOffset = 3,
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        val t1 = BookNoteItem.NovelHighlight(
            id = 2L,
            mangaId = 1L,
            chapterId = 20L,
            chapterIndex = 1,
            chapterTitle = "2 大学生涯",
            text = "文本B",
            note = "我的思考",
            startOffset = 10,
            endOffset = 13,
            createdAt = 2000L,
            updatedAt = 2000L,
        )
        val b1 = BookNoteItem.BookmarkEntry(
            id = 3L,
            mangaId = 1L,
            chapterId = 20L,
            chapterIndex = 1,
            chapterTitle = "2 大学生涯",
            page = 1,
            imageUrl = null,
            percent = 0.5f,
            createdAt = 3000L,
        )

        val state = NotesUiState(
            selectedMangaId = 1L,
            selectedBookNotes = listOf(h1, t1, b1),
            selectedFilter = NoteType.ALL,
        )

        val grouped = state.notesGroupedByChapter
        assertEquals(2, grouped.size)
        assertEquals(1, grouped["1 贫寒出身"]?.size)
        assertEquals(2, grouped["2 大学生涯"]?.size)

        val highlightOnlyState = state.copy(selectedFilter = NoteType.HIGHLIGHT)
        assertEquals(1, highlightOnlyState.filteredBookNotes.size)
        assertEquals(1L, highlightOnlyState.filteredBookNotes.first().id)

        val thoughtOnlyState = state.copy(selectedFilter = NoteType.THOUGHT)
        assertEquals(1, thoughtOnlyState.filteredBookNotes.size)
        assertEquals(2L, thoughtOnlyState.filteredBookNotes.first().id)

        val bookmarkOnlyState = state.copy(selectedFilter = NoteType.BOOKMARK)
        assertEquals(1, bookmarkOnlyState.filteredBookNotes.size)
        assertEquals(3L, bookmarkOnlyState.filteredBookNotes.first().id)
    }
}
