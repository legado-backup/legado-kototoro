package org.skepsun.kototoro.settings.compose

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReaderInfoBarSettingsStateTest {

    @Test
    fun `dependent options follow the latest information bar state`() {
        val disabled = resolveReaderInfoBarSettingsState(infoBarEnabled = false)
        assertFalse(disabled.layoutEnabled)
        assertFalse(disabled.cutoutAvoidanceEnabled)

        val enabled = resolveReaderInfoBarSettingsState(infoBarEnabled = true)
        assertTrue(enabled.layoutEnabled)
        assertTrue(enabled.cutoutAvoidanceEnabled)
    }
}
