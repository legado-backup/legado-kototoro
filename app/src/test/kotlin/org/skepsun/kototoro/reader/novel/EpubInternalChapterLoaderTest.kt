package org.skepsun.kototoro.reader.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpubInternalChapterLoaderTest {

	@Test
	fun `persisted mapping index wins over remote internal chapter URL`() {
		val remoteUrl = "https://n.novelia.cc/api/wenku/book/file/volume.epub?model=fixture#chapter/2"

		assertEquals(7, resolveEpubChapterIndex(remoteUrl, mappedChapterIndex = 7))
	}

	@Test
	fun `chapter URL index is used when no persisted mapping exists`() {
		val localUrl = "file:///data/epub/volume.epub#chapter/2"

		assertEquals(2, resolveEpubChapterIndex(localUrl, mappedChapterIndex = null))
	}
}
