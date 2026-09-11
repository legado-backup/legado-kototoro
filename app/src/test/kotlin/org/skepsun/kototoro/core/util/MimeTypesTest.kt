package org.skepsun.kototoro.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.util.ext.MimeType

class MimeTypesTest {

    @Test
    fun `recognizes jpeg xl extension case insensitively`() {
        assertEquals("image/jxl", MimeTypes.getMimeTypeFromExtension("chapter-01.JXL")?.toString())
    }

    @Test
    fun `recognizes jpeg xl urls when the server omits a mime type`() {
        assertEquals(
            "image/jxl",
            MimeTypes.getMimeTypeFromUrl("https://example.org/chapter-01.jxl?token=redacted")?.toString(),
        )
    }

    @Test
    fun `maps jpeg xl mime type back to extension`() {
        assertEquals("jxl", MimeTypes.getExtension(MimeType("image/jxl")))
    }
}
