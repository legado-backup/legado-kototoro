package org.skepsun.kototoro.reader.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NovelReaderSettingsTest {

	@Test
	fun `paragraph spacing snaps to discrete line counts`() {
		assertEquals(0, NovelReaderSettings(paragraphSpacing = 0.2f).normalized().paragraphSpacingLines)
		assertEquals(1, NovelReaderSettings(paragraphSpacing = 0.8f).normalized().paragraphSpacingLines)
		assertEquals(2, NovelReaderSettings(paragraphSpacing = 1.7f).normalized().paragraphSpacingLines)
		assertEquals(3, NovelReaderSettings(paragraphSpacing = 9f).normalized().paragraphSpacingLines)
	}

	@Test
	fun `font selection is preserved when settings are normalized`() {
		assertEquals(
			NovelReaderFont.LXGW_WENKAI,
			NovelReaderSettings(font = NovelReaderFont.LXGW_WENKAI).normalized().font,
		)
	}

	@Test
	fun `screen brightness stays within the supported range`() {
		assertEquals(
			NovelReaderSettings.SCREEN_BRIGHTNESS_RANGE.start,
			NovelReaderSettings(screenBrightness = 0f).normalized().screenBrightness,
		)
		assertEquals(
			NovelReaderSettings.SCREEN_BRIGHTNESS_RANGE.endInclusive,
			NovelReaderSettings(screenBrightness = 1.4f).normalized().screenBrightness,
		)
		assertEquals(null, NovelReaderSettings().normalized().screenBrightness)
	}
}
