package org.skepsun.kototoro.details.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DetailsHeaderTitlePolicyTest {

    @Test
    fun `expanded title stays within the compact header line budget`() {
        assertEquals(3, detailsTitleMaxLines(isExpanded = false))
        assertEquals(6, detailsTitleMaxLines(isExpanded = true))
    }
}
