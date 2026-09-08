package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelTextSelectionTest {

    @Test
    fun `finds exact selection from preferred position`() {
        assertEquals(3 until 7, findNovelTextRange("前文\n选中文本\n后文", "选中文本", preferredStart = 2))
    }

    @Test
    fun `finds selection when rendered whitespace differs`() {
        assertEquals(2 until 6, findNovelTextRange("甲\n乙  丙\n丁", "乙 丙"))
    }

    @Test
    fun `returns null for missing selection`() {
        assertNull(findNovelTextRange("只有正文", "不存在"))
    }

    @Test
    fun `maps a marking fragment that starts before the rendered range`() {
        assertEquals(
            0 until 20,
            resolveNovelMarkingLocalRange(
                sourceRange = 100..149,
                markingStart = 80,
                markingEnd = 120,
                textLength = 50,
            ),
        )
    }

    @Test
    fun `maps a marking fragment that ends after the rendered range`() {
        assertEquals(
            20 until 50,
            resolveNovelMarkingLocalRange(
                sourceRange = 100..149,
                markingStart = 120,
                markingEnd = 180,
                textLength = 50,
            ),
        )
    }

    @Test
    fun `ignores a marking without range intersection`() {
        assertNull(
            resolveNovelMarkingLocalRange(
                sourceRange = 100..149,
                markingStart = 150,
                markingEnd = 180,
                textLength = 50,
            ),
        )
    }

    @Test
    fun `places toolbar above when sufficient space exists`() {
        val placement = calculateNovelSelectionToolbarPlacement(
            anchorRect = Rect(left = 100f, top = 500f, right = 300f, bottom = 540f),
            fallbackAnchor = null,
            toolbarSize = IntSize(width = 200, height = 80),
            rootSize = IntSize(width = 1080, height = 1920),
            horizontalMargin = 16,
            spacing = 6,
            topSafeInset = 48,
        )

        requireNotNull(placement)
        assertTrue(placement.isAbove)
        assertEquals(414, placement.offset.y)
        assertEquals(100, placement.offset.x)
    }

    @Test
    fun `places toolbar below when near top edge`() {
        val placement = calculateNovelSelectionToolbarPlacement(
            anchorRect = Rect(left = 100f, top = 60f, right = 300f, bottom = 90f),
            fallbackAnchor = null,
            toolbarSize = IntSize(width = 200, height = 80),
            rootSize = IntSize(width = 1080, height = 1920),
            horizontalMargin = 16,
            spacing = 6,
            topSafeInset = 48,
        )

        requireNotNull(placement)
        assertFalse(placement.isAbove)
        assertEquals(96, placement.offset.y)
    }

    @Test
    fun `keeps toolbar above the bottom safe inset`() {
        val placement = calculateNovelSelectionToolbarPlacement(
            anchorRect = Rect(left = 100f, top = 1800f, right = 300f, bottom = 1840f),
            fallbackAnchor = null,
            toolbarSize = IntSize(width = 200, height = 300),
            rootSize = IntSize(width = 1080, height = 1920),
            horizontalMargin = 16,
            spacing = 6,
            topSafeInset = 48,
            bottomSafeInset = 120,
        )

        requireNotNull(placement)
        assertTrue(placement.isAbove)
        assertEquals(1484, placement.offset.y)
        assertTrue(placement.offset.y + 300 <= 1920 - 120 - 16)
    }

    @Test
    fun `clamps toolbar horizontally inside screen margins`() {
        val placement = calculateNovelSelectionToolbarPlacement(
            anchorRect = Rect(left = 440f, top = 300f, right = 490f, bottom = 340f),
            fallbackAnchor = null,
            toolbarSize = IntSize(width = 400, height = 120),
            rootSize = IntSize(width = 500, height = 800),
            horizontalMargin = 16,
        )

        requireNotNull(placement)
        assertEquals(84, placement.offset.x)
        assertTrue(placement.offset.x + 400 <= 500 - 16)
    }
}
