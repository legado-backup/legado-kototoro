package org.skepsun.kototoro.local.epub

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File

class EpubFileReferenceTest {

	@Test
	fun `parses every persisted local EPUB chapter format`() {
		val references = listOf(
			"localepub:///storage/emulated/0/Books/old.epub#chapter/3",
			"file:///storage/emulated/0/Books/old.epub#chapter/3",
			"content://com.android.externalstorage.documents/document/primary%3ABooks%2Fold.epub#chapter/3",
		)

		references.forEach { url ->
			val result = requireNotNull(parseEpubChapterReference(url))
			assertEquals(url.substringBefore("#chapter/"), result.fileReference)
			assertEquals(3, result.chapterIndex)
		}
	}

	@Test
	fun `rejects unrelated and invalid chapter URLs`() {
		assertNull(parseEpubChapterReference("https://example.org/book.epub#chapter/1"))
		assertNull(parseEpubChapterReference("file:///books/book.epub#chapter/-1"))
		assertNull(parseEpubChapterReference("file:///books/book.epub#chapter/not-a-number"))
		assertNull(parseEpubChapterReference("file:///books/chapter.html"))
	}

	@Test
	fun `extracts chapter index from remote EPUB URL`() {
		val remoteUrl = "https://n.novelia.cc/api/wenku/book/file/volume.epub?model=fixture#chapter/2"

		assertEquals(2, parseEpubChapterIndex(remoteUrl))
	}

	@Test
	fun `builds internal URL from downloaded file reference`() {
		val remoteUrl = "https://n.novelia.cc/api/wenku/book/file/volume.epub?model=fixture"
		val localPath = "/data/user/0/org.skepsun.kototoro/files/epub/42/chapter_7.epub"

		val internalUrl = buildEpubChapterUrl(localPath, 2)

		assertFalse(internalUrl.startsWith(remoteUrl))
		assertEquals(File(localPath).toURI().toString(), internalUrl.substringBefore("#chapter/"))
		assertEquals(2, parseEpubChapterReference(internalUrl)?.chapterIndex)
	}
}
