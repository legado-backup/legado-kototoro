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
}
