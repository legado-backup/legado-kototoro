package org.skepsun.kototoro.reader.novel.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.model.UnknownContentSource
import org.skepsun.kototoro.parsers.model.ContentChapter

class ComposeNovelChaptersSheetTest {

	@Test
	fun `reversing preserves original chapter indices`() {
		val chapters = listOf(chapter(10, "First"), chapter(20, "Second"), chapter(30, "Third"))

		val indices = buildChapterItems(chapters, reversed = true, query = "")
			.filterIsInstance<NovelChapterListItem.Chapter>()
			.map(NovelChapterListItem.Chapter::originalIndex)

		assertEquals(listOf(2, 1, 0), indices)
	}

	@Test
	fun `search matches chapter title`() {
		val chapters = listOf(chapter(10, "Arrival"), chapter(20, "Departure"))

		val result = buildChapterItems(chapters, reversed = false, query = "part")
			.filterIsInstance<NovelChapterListItem.Chapter>()

		assertEquals(listOf(1), result.map(NovelChapterListItem.Chapter::originalIndex))
	}

	@Test
	fun `current chapter position includes inserted section headers`() {
		val chapters = listOf(
			chapter(10, "First", volume = 1, branch = "Original"),
			chapter(20, "Second", volume = 1, branch = "Original"),
			chapter(30, "Third", volume = 2, branch = "Original"),
		)
		val items = buildChapterItems(chapters, reversed = false, query = "")

		assertEquals(5, chapterListPositionForCurrent(items, currentIndex = 2))
	}

	@Test
	fun `missing current chapter does not resolve to the first row`() {
		val items = buildChapterItems(listOf(chapter(10, "First")), reversed = false, query = "")

		assertEquals(-1, chapterListPositionForCurrent(items, currentIndex = 4))
	}

	@Test
	fun `chapters are separated by volume within the same branch`() {
		val chapters = listOf(
			chapter(10, "First", volume = 1, branch = "Original"),
			chapter(20, "Second", volume = 1, branch = "Original"),
			chapter(30, "Third", volume = 2, branch = "Original"),
		)

		val headers = buildChapterItems(chapters, reversed = false, query = "")
			.filterIsInstance<NovelChapterListItem.Header>()
			.map(NovelChapterListItem.Header::title)

		assertEquals(listOf("Original", "Volume 1", "Volume 2"), headers)
	}

	@Test
	fun `reversing also reverses volume sections`() {
		val chapters = listOf(
			chapter(10, "First", volume = 1),
			chapter(20, "Second", volume = 2),
		)

		val headers = buildChapterItems(chapters, reversed = true, query = "")
			.filterIsInstance<NovelChapterListItem.Header>()
			.map(NovelChapterListItem.Header::title)

		assertEquals(listOf("Volume 2", "Volume 1"), headers)
	}

	private fun chapter(
		id: Long,
		title: String,
		volume: Int = 0,
		branch: String? = null,
	) = ContentChapter(
		id = id,
		title = title,
		volume = volume,
		number = 0f,
		url = "chapter/$id",
		scanlator = null,
		uploadDate = 0,
		branch = branch,
		source = UnknownContentSource,
	)

	@Test
	fun `novel chapters sheet tab enum has expected entries and order`() {
		val entries = NovelChaptersSheetTab.entries
		assertEquals(3, entries.size)
		assertEquals(NovelChaptersSheetTab.CHAPTERS, entries[0])
		assertEquals(NovelChaptersSheetTab.NOTES, entries[1])
		assertEquals(NovelChaptersSheetTab.SEARCH, entries[2])
	}

	@Test
	fun `content search returns chapter and matching excerpt`() {
		val chapters = listOf(chapter(10, "Arrival"), chapter(20, "Departure"))
		val documents = listOf(
			NovelComposeChapterContent(
				chapterId = 10,
				chapterIndex = 0,
				chapterTitle = "Arrival",
				content = "The quiet arrival changed everything.\nThe room fell silent.",
				translation = null,
			),
			NovelComposeChapterContent(
				chapterId = 20,
				chapterIndex = 1,
				chapterTitle = "Departure",
				content = "A final signal marked the departure.",
				translation = null,
			),
		)

		val results = searchNovelChapterContent(chapters, documents, "ARRIVAL")

		assertEquals(1, results.size)
		assertEquals(0, results.single().chapterIndex)
		assertEquals("Arrival", results.single().chapterTitle)
		assertTrue(results.single().excerpt.contains("arrival", ignoreCase = true))
	}

	@Test
	fun `content search limits repeated matches within one chapter`() {
		val chapters = listOf(chapter(10, "Arrival"))
		val documents = listOf(
			NovelComposeChapterContent(
				chapterId = 10,
				chapterIndex = 0,
				chapterTitle = "Arrival",
				content = "signal one; signal two; signal three",
				translation = null,
			),
		)

		val results = searchNovelChapterContent(
			chapters = chapters,
			documents = documents,
			query = "signal",
			maxResultsPerChapter = 2,
		)

		assertEquals(2, results.size)
	}

	@Test
	fun `content search reports keyword ranges inside the excerpt`() {
		val chapters = listOf(chapter(10, "Arrival"))
		val documents = listOf(
			NovelComposeChapterContent(
				chapterId = 10,
				chapterIndex = 0,
				chapterTitle = "Arrival",
				content = "The quiet arrival changed everything. Arrival was all that mattered.",
				translation = null,
			),
		)

		val result = searchNovelChapterContent(chapters, documents, "ARRIVAL").first()

		assertTrue(result.matchRanges.isNotEmpty())
		assertTrue(
			result.matchRanges.all { range ->
				result.excerpt.substring(range.first, range.last + 1).equals("arrival", ignoreCase = true)
			},
		)
	}

	@Test
	fun `content search reports keyword ranges after whitespace collapsing`() {
		val chapters = listOf(chapter(10, "Arrival"))
		val documents = listOf(
			NovelComposeChapterContent(
				chapterId = 10,
				chapterIndex = 0,
				chapterTitle = "Arrival",
				content = "They waited   for the arrival of dawn.",
				translation = null,
			),
		)

		val result = searchNovelChapterContent(chapters, documents, "for the arrival").first()

		assertEquals(1, result.matchRanges.size)
		assertEquals(
			"for the arrival",
			result.excerpt.substring(result.matchRanges.single().first, result.matchRanges.single().last + 1),
		)
	}

	@Test
	fun `findSearchMatchRanges finds all case-insensitive matches`() {
		val ranges = findSearchMatchRanges("aXa XA xab", "xa")

		assertEquals(listOf(1..2, 4..5, 7..8), ranges)
	}

	@Test
	fun `findSearchMatchRanges returns empty for blank needle`() {
		assertTrue(findSearchMatchRanges("anything", "").isEmpty())
	}
}
