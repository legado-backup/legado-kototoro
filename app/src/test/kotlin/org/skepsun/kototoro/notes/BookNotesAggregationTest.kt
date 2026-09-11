package org.skepsun.kototoro.notes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.notes.domain.BookNoteItem
import org.skepsun.kototoro.notes.domain.BookNotesSummary
import org.skepsun.kototoro.notes.domain.NoteType
import org.skepsun.kototoro.notes.ui.NotesUiState
import org.skepsun.kototoro.notes.ui.canMakeExcerpt
import org.skepsun.kototoro.notes.ui.createExcerptData
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

    @Test
    fun `manga crop note noteType determines highlight or thought correctly`() {
        val cropHighlight = BookNoteItem.MangaCropNote(
            id = 10L,
            mangaId = 50L,
            chapterId = 500L,
            chapterIndex = 0,
            chapterTitle = "第1话",
            page = 3,
            cropSnapshotUri = "file:///notes/crop1.jpg",
            note = null,
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        assertEquals(NoteType.HIGHLIGHT, cropHighlight.noteType)

        val cropThought = cropHighlight.copy(
            id = 11L,
            note = "精湛的作画与分镜",
        )
        assertEquals(NoteType.THOUGHT, cropThought.noteType)
    }

    @Test
    fun `video note noteType determines highlight or thought correctly`() {
        val videoHighlight = BookNoteItem.VideoNote(
            id = 20L,
            mangaId = 60L,
            chapterId = 600L,
            chapterIndex = 0,
            chapterTitle = "第1集",
            positionMs = 125000L,
            durationMs = 1440000L,
            snapshotUri = "file:///notes/video1.jpg",
            quoteText = "我一定会回来的！",
            note = null,
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        assertEquals(NoteType.HIGHLIGHT, videoHighlight.noteType)

        val videoThought = videoHighlight.copy(
            id = 21L,
            note = "经典台词名场面",
        )
        assertEquals(NoteType.THOUGHT, videoThought.noteType)
    }

    @Test
    fun `canMakeExcerpt returns true for all notes and bookmarks`() {
        val highlight = BookNoteItem.NovelHighlight(
            id = 1L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第1章", text = "有些句子值得被记住", note = null,
            startOffset = 0, endOffset = 9, createdAt = 1000L, updatedAt = 1000L,
        )
        val videoWithQuote = BookNoteItem.VideoNote(
            id = 2L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第1集", positionMs = 60000L, durationMs = 120000L,
            snapshotUri = "file:///notes/v1.jpg", quoteText = "人类的悲欢并不相通", note = null,
            createdAt = 1000L, updatedAt = 1000L,
        )
        val videoWithOnlyNote = videoWithQuote.copy(id = 3L, quoteText = null, note = "名场面")
        val videoEmpty = videoWithQuote.copy(id = 4L, quoteText = null, note = null)
        val cropWithNote = BookNoteItem.MangaCropNote(
            id = 5L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第1话", page = 3, cropSnapshotUri = "file:///notes/c1.jpg",
            note = "这个画格分镜很神", createdAt = 1000L, updatedAt = 1000L,
        )
        val cropWithoutNote = cropWithNote.copy(id = 6L, note = null)
        val bookmark = BookNoteItem.BookmarkEntry(
            id = 7L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第1话", page = 0, imageUrl = null, percent = 0f, createdAt = 1000L,
        )

        assertTrue(canMakeExcerpt(highlight))
        assertTrue(canMakeExcerpt(videoWithQuote))
        assertTrue(canMakeExcerpt(videoWithOnlyNote))
        assertTrue(canMakeExcerpt(videoEmpty))
        assertTrue(canMakeExcerpt(cropWithNote))
        assertTrue(canMakeExcerpt(cropWithoutNote))
        assertTrue(canMakeExcerpt(bookmark))
    }

    @Test
    fun `createExcerptData populates fields accurately for video and manga notes`() {
        val video = BookNoteItem.VideoNote(
            id = 2L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第3集", positionMs = 75000L, durationMs = 120000L,
            snapshotUri = "file:///notes/v1.jpg", quoteText = "这是台词字幕", note = "我的批注感想",
            createdAt = 1000L, updatedAt = 1000L,
        )
        val excerptData = createExcerptData(video, manga = null)
        org.junit.jupiter.api.Assertions.assertNotNull(excerptData)
        assertEquals("这是台词字幕", excerptData?.selectedText)
        assertEquals("我的批注感想", excerptData?.note)
        assertEquals("file:///notes/v1.jpg", excerptData?.imageUri)
        assertTrue(excerptData?.chapterTitle?.contains("第3集") == true)
        assertTrue(excerptData?.chapterTitle?.contains("1:15") == true)

        val cropWithNote = BookNoteItem.MangaCropNote(
            id = 5L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第10话", page = 4, cropSnapshotUri = "file:///notes/c1.jpg",
            note = "神级分镜", createdAt = 1000L, updatedAt = 1000L,
        )
        val cropExcerpt = createExcerptData(cropWithNote, manga = null)
        org.junit.jupiter.api.Assertions.assertNotNull(cropExcerpt)
        assertEquals("神级分镜", cropExcerpt?.selectedText)
        assertEquals("第10话 · P5", cropExcerpt?.chapterTitle)
        assertEquals("file:///notes/c1.jpg", cropExcerpt?.imageUri)

        val mangaBookmark = BookNoteItem.BookmarkEntry(
            id = 7L, mangaId = 100L, chapterId = 10L, chapterIndex = 0,
            chapterTitle = "第5话", page = 12, imageUrl = "https://example.com/p13.jpg",
            percent = 0.5f, createdAt = 1000L, localSnapshotUri = "file:///snapshots/p13.jpg",
        )
        val bookmarkExcerpt = createExcerptData(mangaBookmark, manga = null)
        org.junit.jupiter.api.Assertions.assertNotNull(bookmarkExcerpt)
        assertEquals("", bookmarkExcerpt?.selectedText)
        assertEquals("file:///snapshots/p13.jpg", bookmarkExcerpt?.imageUri)
        assertEquals("第5话 · P13", bookmarkExcerpt?.chapterTitle)
    }

    @Test
    fun `book notes summary retains isNsfw and source attributes`() {
        val summary = BookNotesSummary(
            mangaId = 10L,
            title = "成年人漫画",
            coverUrl = null,
            contentType = ContentType.HENTAI_MANGA,
            totalCount = 5,
            highlightCount = 0,
            thoughtCount = 2,
            bookmarkCount = 3,
            readingProgressPercent = 0.5f,
            readingProgressText = "50%",
            lastUpdatedAt = 10000L,
            isNsfw = true,
            source = "nhentai",
        )
        assertTrue(summary.isNsfw)
        assertEquals("nhentai", summary.source)
    }

    @Test
    fun `space and nsfw policy filters summaries correctly`() {
        val novelSummary = BookNotesSummary(
            mangaId = 1L, title = "小说道理", coverUrl = null,
            contentType = ContentType.NOVEL, totalCount = 2, highlightCount = 2,
            thoughtCount = 0, bookmarkCount = 0, readingProgressPercent = 0.5f,
            readingProgressText = "50%", lastUpdatedAt = 1000L, isNsfw = false, source = "sourceA",
        )
        val mangaSummary = BookNotesSummary(
            mangaId = 2L, title = "热血少年", coverUrl = null,
            contentType = ContentType.MANGA, totalCount = 4, highlightCount = 0,
            thoughtCount = 1, bookmarkCount = 3, readingProgressPercent = 0.2f,
            readingProgressText = "20%", lastUpdatedAt = 2000L, isNsfw = false, source = "sourceB",
        )
        val nsfwMangaSummary = BookNotesSummary(
            mangaId = 3L, title = "成人本子", coverUrl = null,
            contentType = ContentType.HENTAI_MANGA, totalCount = 1, highlightCount = 0,
            thoughtCount = 0, bookmarkCount = 1, readingProgressPercent = 0.1f,
            readingProgressText = "10%", lastUpdatedAt = 3000L, isNsfw = true, source = "sourceC",
        )

        val summaries = listOf(novelSummary, mangaSummary, nsfwMangaSummary)

        // Filter when NSFW is disabled (either globally or on-screen)
        val sfwOnly = summaries.filterNot { it.isNsfw }
        assertEquals(2, sfwOnly.size)
        assertTrue(sfwOnly.none { it.isNsfw })

        // Filter by Space allowed content types (e.g. Manga space)
        val mangaSpaceTypes = setOf(ContentType.MANGA, ContentType.MANHWA, ContentType.MANHUA)
        val mangaSpaceSummaries = sfwOnly.filter { it.contentType in mangaSpaceTypes }
        assertEquals(1, mangaSpaceSummaries.size)
        assertEquals(2L, mangaSpaceSummaries.first().mangaId)

        // Filter by Space allowed source names
        val allowedSources = setOf("sourceA")
        val sourceFilteredSummaries = summaries.filter { it.source in allowedSources }
        assertEquals(1, sourceFilteredSummaries.size)
        assertEquals(1L, sourceFilteredSummaries.first().mangaId)
    }
}
