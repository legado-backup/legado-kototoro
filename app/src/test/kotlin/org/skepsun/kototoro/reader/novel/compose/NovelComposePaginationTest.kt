package org.skepsun.kototoro.reader.novel.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NovelComposePaginationTest {

	@Test
	fun `line ranges cover every line once`() {
		val tops = listOf(0f, 20f, 40f, 60f, 80f)
		val bottoms = listOf(20f, 40f, 60f, 80f, 100f)

		val ranges = splitNovelPageLineRanges(
			lineCount = tops.size,
			pageHeightPx = 45,
			lineTop = tops::get,
			lineBottom = bottoms::get,
		)

		assertEquals(listOf(0..1, 2..3, 4..4), ranges)
		assertEquals(tops.indices.toList(), ranges.flatMap { it.toList() })
	}

	@Test
	fun `line taller than a page still advances`() {
		val ranges = splitNovelPageLineRanges(
			lineCount = 2,
			pageHeightPx = 40,
			lineTop = listOf(0f, 80f)::get,
			lineBottom = listOf(80f, 100f)::get,
		)

		assertEquals(listOf(0..0, 1..1), ranges)
	}

	@Test
	fun `invalid dimensions produce no pages`() {
		assertEquals(
			emptyList<IntRange>(),
			splitNovelPageLineRanges(3, 0, { 0f }, { 0f }),
		)
	}

	@Test
	fun `page request is clamped inside its target chapter`() {
		val pages = listOf(
			NovelPageIdentity(10L, 0),
			NovelPageIdentity(10L, 0),
			NovelPageIdentity(11L, 1),
			NovelPageIdentity(11L, 1),
		)

		val target = resolveNovelPageRequest(
			request = NovelPageRequest(1L, 10L, 0, Int.MAX_VALUE),
			pages = pages,
		)

		assertEquals(1, target)
	}

	@Test
	fun `page request resolves local page after prepended chapter`() {
		val pages = listOf(
			NovelPageIdentity(9L, 0),
			NovelPageIdentity(10L, 1),
			NovelPageIdentity(10L, 1),
			NovelPageIdentity(10L, 1),
		)

		val target = resolveNovelPageRequest(
			request = NovelPageRequest(2L, 10L, 1, 1),
			pages = pages,
		)

		assertEquals(2, target)
	}

	@Test
	fun `page request waits until its chapter exists`() {
		val target = resolveNovelPageRequest(
			request = NovelPageRequest(3L, 10L, 1, 0),
			pages = listOf(NovelPageIdentity(9L, 0)),
		)

		assertEquals(null, target)
	}

	@Test
	fun `repeated image urls retain unique pager keys`() {
		val chapterId = 10L

		val first = novelComposeImagePageKey(chapterId, "image-0")
		val second = novelComposeImagePageKey(chapterId, "image-1")

		assertEquals("image:10:image-0", first)
		assertEquals("image:10:image-1", second)
	}

	@Test
	fun `dual page forward curl anchors turning and revealed pages`() {
		assertEquals(
			null,
			novelDualPageCurlOffset(pageOffset = 0f, isScrollInProgress = false, curlOnEnd = true),
		)
		assertEquals(
			-0.25f,
			novelDualPageCurlOffset(pageOffset = 0.75f, isScrollInProgress = true, curlOnEnd = true),
		)
		assertEquals(
			0.75f,
			novelDualPageCurlOffset(pageOffset = 1.75f, isScrollInProgress = true, curlOnEnd = true),
		)
	}

	@Test
	fun `dual page backward curl anchors turning and revealed pages`() {
		assertEquals(
			0.25f,
			novelDualPageCurlOffset(pageOffset = 0.25f, isScrollInProgress = true, curlOnEnd = false),
		)
		assertEquals(
			-0.75f,
			novelDualPageCurlOffset(pageOffset = -0.75f, isScrollInProgress = true, curlOnEnd = false),
		)
	}

	@Test
	fun `dual page curl uses the physical outer edge`() {
		assertEquals(true, novelDualPageCurlOnEnd(horizontalDragFraction = -0.1f, isReversed = false))
		assertEquals(false, novelDualPageCurlOnEnd(horizontalDragFraction = 0.1f, isReversed = false))
		assertEquals(false, novelDualPageCurlOnEnd(horizontalDragFraction = -0.1f, isReversed = true))
	}

	@Test
	fun `findNovelPageForMarking locates page by exact text match`() {
		val pages = listOf(
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("赵客缦胡缨，吴钩霜雪明。银鞍照白马，飒沓如流星。"),
				chapterId = 100L,
				chapterIndex = 0,
				charStart = 0,
				charEnd = 50,
			),
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("十步杀一人，千里不留行。事了拂衣去，深藏身与名。"),
				chapterId = 100L,
				chapterIndex = 0,
				charStart = 51,
				charEnd = 100,
			),
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("闲过临淄市，脱剑膝前横。将炙啖朱亥，持觞劝侯嬴。"),
				chapterId = 100L,
				chapterIndex = 0,
				charStart = 101,
				charEnd = 150,
			),
		)

		val target = NovelMarkingTarget(
			markingId = 1L,
			chapterId = 100L,
			chapterIndex = 0,
			startOffset = 60,
			endOffset = 70,
			selectedText = "十步杀一人",
		)

		val pageIndex = findNovelPageForMarking(pages, target)
		assertEquals(1, pageIndex, "Should jump directly to page 1 which contains '十步杀一人'")
	}

	@Test
	fun `findNovelPageForMarking disambiguates duplicate text by proximity to startOffset`() {
		val pages = listOf(
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("天地玄黄，宇宙洪荒。他说了一声你好。"),
				chapterId = 100L,
				chapterIndex = 0,
				charStart = 0,
				charEnd = 100,
			),
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("日月盈昃，辰宿列张。寒来暑往，秋收冬藏。"),
				chapterId = 100L,
				chapterIndex = 0,
				charStart = 101,
				charEnd = 200,
			),
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("闰余成岁，律吕调阳。他又说了一声你好。"),
				chapterId = 100L,
				chapterIndex = 0,
				charStart = 201,
				charEnd = 300,
			),
		)

		val target = NovelMarkingTarget(
			markingId = 2L,
			chapterId = 100L,
			chapterIndex = 0,
			startOffset = 250,
			endOffset = 252,
			selectedText = "你好",
		)

		val pageIndex = findNovelPageForMarking(pages, target)
		assertEquals(2, pageIndex, "Should disambiguate and select page 2 closest to offset 250")
	}

	@Test
	fun `findNovelPageForMarking ignores pages from other chapters`() {
		val pages = listOf(
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("第一章的文本，包含关键字天地玄黄"),
				chapterId = 99L,
				chapterIndex = 0,
				charStart = 0,
				charEnd = 50,
			),
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("第二章的开头"),
				chapterId = 100L,
				chapterIndex = 1,
				charStart = 0,
				charEnd = 50,
			),
			NovelComposePage.Text(
				value = androidx.compose.ui.text.AnnotatedString("第二章的文本，也包含关键字天地玄黄"),
				chapterId = 100L,
				chapterIndex = 1,
				charStart = 51,
				charEnd = 100,
			),
		)

		val target = NovelMarkingTarget(
			markingId = 3L,
			chapterId = 100L,
			chapterIndex = 1,
			startOffset = 70,
			endOffset = 74,
			selectedText = "天地玄黄",
		)

		val pageIndex = findNovelPageForMarking(pages, target)
		assertEquals(2, pageIndex, "Should match page 2 in chapter 100 and ignore chapter 99")
	}
}
