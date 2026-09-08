package org.skepsun.kototoro.reader.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelAiAskContextTest {

	@Test
	fun `builds context from both sides of the selected range`() {
		val chapter = "前面的情节。\n\n选中的句子。\n\n后面的情节。"
		val selectedText = "选中的句子。"
		val start = chapter.indexOf(selectedText)

		val context = buildNovelAiContext(
			chapterText = chapter,
			excerptRange = start until start + selectedText.length,
		)

		assertEquals("前面的情节。", context.before)
		assertEquals("后面的情节。", context.after)
		assertFalse(context.before.contains(selectedText))
		assertFalse(context.after.contains(selectedText))
	}

	@Test
	fun `limits each context side without losing the selected boundary`() {
		val chapter = "甲".repeat(20) + "目标" + "乙".repeat(20)
		val start = chapter.indexOf("目标")

		val context = buildNovelAiContext(
			chapterText = chapter,
			excerptRange = start until start + 2,
			maxCharsPerSide = 5,
		)

		assertTrue(context.before.length <= 6)
		assertTrue(context.after.length <= 6)
		assertTrue(context.before.endsWith("甲甲甲甲甲"))
		assertTrue(context.after.startsWith("乙乙乙乙乙"))
	}
}
