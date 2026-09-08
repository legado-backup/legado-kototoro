package org.skepsun.kototoro.reader.translate.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelAiPromptTest {

	@Test
	fun `prompt labels nearby context separately from the selected excerpt`() {
		val prompt = buildNovelAiPrompt(
			bookTitle = "一本书",
			chapterTitle = "第一章",
			contextBefore = "选文之前发生的事",
			excerpt = "这是一段选文",
			contextAfter = "选文之后发生的事",
			question = "这段的作用是什么？",
		)

		assertTrue(prompt.contains("Context before excerpt:"))
		assertTrue(prompt.contains("Context after excerpt:"))
		assertTrue(prompt.contains("Selected excerpt:"))
		assertTrue(prompt.indexOf("选文之前发生的事") < prompt.indexOf("这是一段选文"))
		assertTrue(prompt.indexOf("这是一段选文") < prompt.indexOf("选文之后发生的事"))
	}

	@Test
	fun `prompt omits empty context sections`() {
		val prompt = buildNovelAiPrompt(
			bookTitle = "一本书",
			chapterTitle = "第一章",
			contextBefore = "",
			excerpt = "这是一段选文",
			contextAfter = "",
			question = "请解释。",
		)

		assertFalse(prompt.contains("Context before excerpt:"))
		assertFalse(prompt.contains("Context after excerpt:"))
	}
}
