package org.skepsun.kototoro.reader.ui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.parsers.model.ContentPage
import org.skepsun.kototoro.parsers.model.ContentSource
import org.skepsun.kototoro.parsers.model.ContentType

class PageSaveHelperTest {

    @Test
    fun `file name uses configured manga title length`() {
        val title = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val task = PageSaveHelper.Task(
            manga = Content(
                id = 1L,
                title = title,
                altTitles = emptySet(),
                url = "manga",
                publicUrl = "https://example.com/manga",
                rating = 0f,
                contentRating = null,
                coverUrl = null,
                tags = emptySet(),
                state = null,
                authors = emptySet(),
                chapters = listOf(
                    ContentChapter(
                        id = 2L,
                        title = null,
                        number = 1f,
                        volume = 0,
                        url = "chapter",
                        scanlator = null,
                        uploadDate = 0L,
                        branch = null,
                        source = TestSource,
                    ),
                ),
                source = TestSource,
            ),
            chapterId = 2L,
            pageNumber = 3,
            page = ContentPage(
                id = 4L,
                url = "page.jpg",
                preview = null,
                source = TestSource,
            ),
        )

        task.getFileBaseName(mangaTitleLength = 24).substringBefore("-") shouldBe "ABCDEFGHIJKLMNOPQRSTUVWX"
    }

    private data object TestSource : ContentSource {
        override val name: String = "TEST"
        override val locale: String = "en"
        override val contentType: ContentType = ContentType.MANGA
    }
}
