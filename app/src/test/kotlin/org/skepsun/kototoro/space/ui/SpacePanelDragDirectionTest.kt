package org.skepsun.kototoro.space.ui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SpacePanelDragDirectionTest {

    @Test
    fun `horizontal drag stays locked when later movement is vertical`() {
        val direction = SpacePanelDragDirection()

        direction.update(deltaX = 12f, deltaY = 2f, touchSlop = 8f) shouldBe
            SpacePanelDragDirection.Value.HORIZONTAL
        direction.update(deltaX = 1f, deltaY = 30f, touchSlop = 8f) shouldBe
            SpacePanelDragDirection.Value.HORIZONTAL
    }

    @Test
    fun `vertical movement remains available to the panel list`() {
        val direction = SpacePanelDragDirection()

        direction.update(deltaX = 2f, deltaY = 12f, touchSlop = 8f) shouldBe
            SpacePanelDragDirection.Value.VERTICAL
        direction.update(deltaX = 30f, deltaY = 1f, touchSlop = 8f) shouldBe
            SpacePanelDragDirection.Value.VERTICAL
    }
}
