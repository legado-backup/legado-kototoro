package org.skepsun.kototoro.reader.novel.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class NovelExcerptCardRendererTest {

    @Test
    fun `date helper formats chinese year month day correctly`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 7)
        }
        val formatted = NovelExcerptDateHelper.formatChineseDate(cal.timeInMillis)
        assertEquals("二〇二六年 · 九月 · 七日", formatted)
    }

    @Test
    fun `date helper calculates 24 solar terms accurately`() {
        // September 7 is White Dew (白露)
        val termSep7 = NovelExcerptDateHelper.getSolarTerm(Calendar.SEPTEMBER, 7)
        assertEquals("白露", termSep7)

        // September 23 is Autumn Equinox (秋分)
        val termSep23 = NovelExcerptDateHelper.getSolarTerm(Calendar.SEPTEMBER, 23)
        assertEquals("秋分", termSep23)
    }

    @Test
    fun `splitTitleForVertical splits long titles into max 6 character columns`() {
        val shortTitle = NovelExcerptDateHelper.splitTitleForVertical("三体")
        assertEquals(listOf("三体"), shortTitle)

        val sixCharTitle = NovelExcerptDateHelper.splitTitleForVertical("平凡的世界")
        assertEquals(listOf("平凡的世界"), sixCharTitle)

        val kantTitle = NovelExcerptDateHelper.splitTitleForVertical("康德三大批判合集")
        assertEquals(listOf("康德三大批判", "合集"), kantTitle)

        val titleWithBookMarks = NovelExcerptDateHelper.splitTitleForVertical("《百年孤独》")
        assertEquals(listOf("百年孤独"), titleWithBookMarks)
    }

    @Test
    fun `computeLayout computes valid layouts for all six templates`() {
        val data = NovelExcerptData(
            selectedText = "借助于外感官",
            bookTitle = "康德三大批判合集",
            chapterTitle = "第一章 论空间",
            author = "康德",
            userNickname = "书友",
            note = "时空的感性纯形式分析",
        )

        NovelExcerptTemplate.entries.forEach { template ->
            val config = NovelExcerptConfiguration(template = template)
            val layout = NovelExcerptCardRenderer.computeLayout(
                context = null,
                data = data,
                configuration = config,
            )

            assertEquals(1080, layout.width)
            assertTrue(layout.height >= 1280, "Height for $template should be >= 1280, was ${layout.height}")
            assertEquals("康德三大批判合集", layout.bookTitle)
            assertEquals("第一章 论空间", layout.chapterTitle)
            assertEquals("康德", layout.author)
            assertEquals("书友", layout.userNickname)
            assertTrue(layout.textLines.isNotEmpty())
            assertTrue(layout.noteLines.isNotEmpty())
        }
    }

    @Test
    fun `calendar template uses centered quote start and distinct height bounds`() {
        val data = NovelExcerptData(
            selectedText = "自由是道德律的存在的理由。",
            bookTitle = "实践理性批判",
            chapterTitle = "序言",
            author = "康德",
        )
        val config = NovelExcerptConfiguration(template = NovelExcerptTemplate.CALENDAR)
        val layout = NovelExcerptCardRenderer.computeLayout(null, data, config)

        assertEquals(450f, layout.quoteStartY)
        assertTrue(layout.height >= 1380)
    }
}
