package org.skepsun.kototoro.reader.novel.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
}
