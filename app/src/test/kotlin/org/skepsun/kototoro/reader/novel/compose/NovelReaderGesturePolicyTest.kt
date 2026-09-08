package org.skepsun.kototoro.reader.novel.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingEntity
import org.skepsun.kototoro.reader.novel.resolveNovelChapterTitleAtBottom

class NovelReaderGesturePolicyTest {

    @Test
    fun `top chapter title is hidden when chapter title is configured at bottom`() {
        assertFalse(
            shouldShowNovelTopChapterTitle(
                controlsVisible = true,
                chapterTitleAtBottom = true,
            ),
        )
    }

    @Test
    fun `top chapter title follows controls when chapter title is not at bottom`() {
        assertTrue(
            shouldShowNovelTopChapterTitle(
                controlsVisible = true,
                chapterTitleAtBottom = false,
            ),
        )
    }

    @Test
    fun `common chapter title setting takes precedence over legacy novel setting`() {
        assertTrue(resolveNovelChapterTitleAtBottom(commonValue = true, legacyValue = false))
        assertFalse(resolveNovelChapterTitleAtBottom(commonValue = false, legacyValue = true))
        assertTrue(resolveNovelChapterTitleAtBottom(commonValue = null, legacyValue = true))
    }

    @Test
    fun `highlighted text resolves a marking from its local rendered range`() {
        val marking = NovelMarkingEntity(
            id = 8L,
            mangaId = 1L,
            chapterId = 2L,
            chapterIndex = 0,
            startOffset = 0,
            endOffset = 6,
            selectedText = "marked",
            note = null,
            createdAt = 0L,
            updatedAt = 0L,
        )

        assertEquals(
            marking,
            findNovelMarkingAtOffset(
                textOffset = 6,
                text = "\u3000\u3000marked text",
                sourceRange = 0..11,
                markings = listOf(marking),
            ),
        )
    }

    @Test
    fun `selection actions start with highlight and omit bookmark`() {
        val actions = novelSelectionActions(isMarking = false)

        assertEquals(NovelTextSelectionAction.HIGHLIGHT, actions.first())
        assertFalse(actions.contains(NovelTextSelectionAction.BOOKMARK))
    }

    @Test
    fun `marking actions add delete without repeating highlight`() {
        val actions = novelSelectionActions(isMarking = true)

        assertEquals(NovelTextSelectionAction.COPY, actions.first())
        assertTrue(actions.contains(NovelTextSelectionAction.DELETE))
        assertFalse(actions.contains(NovelTextSelectionAction.HIGHLIGHT))
    }

    @Test
    fun `selection cancellation does not become reader tap`() {
        assertFalse(
            shouldDispatchNovelReaderTap(
                textSelectionActive = true,
                moved = false,
                cancelled = false,
                longPressDispatched = false,
            ),
        )
    }

    @Test
    fun `long press release does not become reader tap`() {
        assertFalse(
            shouldDispatchNovelReaderTap(
                textSelectionActive = false,
                moved = false,
                cancelled = false,
                longPressDispatched = true,
            ),
        )
    }

    @Test
    fun `consumed downward gesture does not arm bookmark pull`() {
        assertFalse(
            shouldStartNovelBookmarkPull(
                textSelectionActive = false,
                pointerConsumed = true,
                totalX = 0f,
                totalY = 80f,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun `unconsumed downward gesture arms bookmark pull`() {
        assertTrue(
            shouldStartNovelBookmarkPull(
                textSelectionActive = false,
                pointerConsumed = false,
                totalX = 2f,
                totalY = 80f,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun `tap offset resolves the marking that owns the character`() {
        val marking = NovelMarkingEntity(
            id = 7L,
            mangaId = 1L,
            chapterId = 2L,
            chapterIndex = 0,
            startOffset = 10,
            endOffset = 16,
            selectedText = "marked",
            note = "my note",
            createdAt = 0L,
            updatedAt = 0L,
        )

        assertEquals(
            marking,
            findNovelMarkingAtOffset(
                textOffset = 2,
                text = "marked text",
                sourceRange = 8..18,
                markings = listOf(marking),
            ),
        )
    }
}
