package org.skepsun.kototoro.reader.novel

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.replace.ReplaceRule
import org.skepsun.kototoro.core.replace.ReplaceRuleSource

class NovelTextProcessorTest {

    @Test
    fun `applies enabled content rules in order`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(pattern = "甲", replacement = "乙", isRegex = false, order = 1),
                    ReplaceRule(pattern = "乙", replacement = "丙", isRegex = false, order = 2),
                )
            },
        )

        val result = processor.process(
            text = "甲甲",
            context = NovelTextContext(sourceName = "source", scopeName = "book"),
        )

        assertEquals("丙丙", result.text)
    }

    @Test
    fun `skips rules outside their scope`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(
                        pattern = "广告",
                        replacement = "",
                        isRegex = false,
                        scopeFilter = "other",
                    ),
                )
            },
        )

        val result = processor.process(
            text = "正文广告",
            context = NovelTextContext(sourceName = "source", scopeName = "book"),
        )

        assertEquals("正文广告", result.text)
    }

    @Test
    fun `keeps original text when a rule fails`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(pattern = "[", replacement = "", isRegex = true),
                )
            },
        )

        val result = processor.process(
            text = "正文",
            context = NovelTextContext(sourceName = "source", scopeName = "book"),
        )

        assertEquals("正文", result.text)
    }

    @Test
    fun `applies title rules without applying content rules`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(pattern = "广告", replacement = "", isRegex = false),
                )

                override suspend fun getTitleRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(pattern = "广告", replacement = "", isRegex = false, scopeTitle = true, scopeContent = false),
                )
            },
        )

        val title = processor.processTitle(
            text = "第一章 广告",
            context = NovelTextContext(sourceName = "source", scopeName = "book"),
        )
        val content = processor.process(
            text = "正文广告",
            context = NovelTextContext(sourceName = "source", scopeName = "book"),
        )

        assertEquals("第一章 ", title.text)
        assertEquals("正文", content.text)
    }

    @Test
    fun `disabled replacement context keeps original text`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(
                        pattern = "marker",
                        replacement = "",
                        isRegex = false,
                    ),
                )
            },
        )

        val result = processor.process(
            text = "marker text",
            context = NovelTextContext(
                sourceName = "source",
                scopeName = "book",
                replaceRulesEnabled = false,
            ),
        )

        assertEquals("marker text", result.text)
        assertEquals(emptyList<String>(), result.failedRuleNames)
    }

    @Test
    fun `disabled rule ids override global rule state for the current book`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(id = 17L, pattern = "marker", replacement = "", isRegex = false),
                )
            },
        )

        val result = processor.process(
            text = "marker text",
            context = NovelTextContext(
                sourceName = "source",
                scopeName = "book",
                disabledReplaceRuleIds = setOf(17L),
            ),
        )

        assertEquals("marker text", result.text)
    }

    @Test
    fun `processes chapter titles as a batch`() = runTest {
        val processor = DefaultNovelTextProcessor(
            object : ReplaceRuleSource {
                override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> = emptyList()

                override suspend fun getTitleRules(scopeName: String, origin: String): List<ReplaceRule> = listOf(
                    ReplaceRule(
                        pattern = "广告",
                        replacement = "",
                        isRegex = false,
                        scopeTitle = true,
                        scopeContent = false,
                    ),
                )
            },
        )

        val results = processor.processTitles(
            listOf(
                NovelTextInput(
                    text = "第一章 广告",
                    context = NovelTextContext(sourceName = "source", scopeName = "book", chapterId = 1L),
                ),
                NovelTextInput(
                    text = "第二章 广告",
                    context = NovelTextContext(sourceName = "source", scopeName = "book", chapterId = 2L),
                ),
            ),
        )

        assertEquals(listOf("第一章 ", "第二章 "), results.map(ProcessedNovelText::text))
    }
}
