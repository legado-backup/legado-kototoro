package org.skepsun.kototoro.core.ui.compose

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScrollbarSectionEstimatorTest {

    private fun mockItem(index: Int, offset: Int, size: Int): LazyListItemInfo {
        val item = mockk<LazyListItemInfo>()
        every { item.index } returns index
        every { item.offset } returns offset
        every { item.size } returns size
        return item
    }

    private fun mockLayoutInfo(
        totalItems: Int,
        visibleItems: List<LazyListItemInfo>,
        viewportStartOffset: Int = 0,
        viewportEndOffset: Int = 1000,
    ): LazyListLayoutInfo {
        val layoutInfo = mockk<LazyListLayoutInfo>()
        every { layoutInfo.totalItemsCount } returns totalItems
        every { layoutInfo.visibleItemsInfo } returns visibleItems
        every { layoutInfo.viewportStartOffset } returns viewportStartOffset
        every { layoutInfo.viewportEndOffset } returns viewportEndOffset
        return layoutInfo
    }

    @Test
    fun `computeFraction returns 0 when at top of list`() {
        val estimator = ScrollbarSectionEstimator()
        val items = (0 until 10).map { mockItem(it, it * 100, 100) }
        val layout = mockLayoutInfo(totalItems = 50, visibleItems = items)

        val fraction = estimator.computeFraction(layout)
        assertEquals(0f, fraction)
    }

    @Test
    fun `computeFraction returns 0 when content fits in viewport`() {
        val estimator = ScrollbarSectionEstimator()
        val items = (0 until 5).map { mockItem(it, it * 100, 100) }
        val layout = mockLayoutInfo(totalItems = 5, visibleItems = items)

        val fraction = estimator.computeFraction(layout)
        assertEquals(0f, fraction)
    }

    @Test
    fun `computeFraction returns 1 when scrolled to bottom of list`() {
        val estimator = ScrollbarSectionEstimator()
        val initialItems = (0 until 10).map { mockItem(it, it * 100, 100) }
        estimator.computeFraction(mockLayoutInfo(totalItems = 50, visibleItems = initialItems))

        val bottomItems = (40 until 50).map { mockItem(it, (it - 40) * 100, 100) }
        val layout = mockLayoutInfo(totalItems = 50, visibleItems = bottomItems)

        val fraction = estimator.computeFraction(layout)
        assertEquals(1f, fraction, 0.001f)
    }

    @Test
    fun `computeFraction increases smoothly and monotonically during scroll`() {
        val estimator = ScrollbarSectionEstimator()
        var previousFraction = -1f

        for (offset in 0..100 step 10) {
            val items = mutableListOf<LazyListItemInfo>()
            items.add(mockItem(index = 0, offset = -offset, size = 100))
            for (i in 1..9) {
                items.add(mockItem(index = i, offset = i * 100 - offset, size = 100))
            }
            if (offset > 0) {
                items.add(mockItem(index = 10, offset = 1000 - offset, size = 100))
            }

            val layout = mockLayoutInfo(totalItems = 50, visibleItems = items)
            val fraction = estimator.computeFraction(layout)
            assertTrue(fraction >= previousFraction, "Fraction should be non-decreasing: $fraction vs $previousFraction")
            previousFraction = fraction
        }
    }

    @Test
    fun `computeFraction resets maxSections when totalItems changes`() {
        val estimator = ScrollbarSectionEstimator()
        val initialItems = (0 until 10).map { mockItem(it, it * 100, 100) }
        estimator.computeFraction(mockLayoutInfo(totalItems = 100, visibleItems = initialItems))
        assertEquals(90f, estimator.getMaxSections(), 0.1f)

        // Data source reloads with 50 items
        estimator.computeFraction(mockLayoutInfo(totalItems = 50, visibleItems = initialItems))
        assertEquals(40f, estimator.getMaxSections(), 0.1f)
    }

    @Test
    fun `computeFraction on list with date headers increases smoothly without backwards jitter`() {
        val estimator = ScrollbarSectionEstimator()
        var previousFraction = -1f

        // Item sizes: index 0, 5, 10 are 40px date headers; others are 140px cards
        fun itemHeight(index: Int): Int = if (index % 5 == 0) 40 else 140

        for (scrollOffset in 0..400 step 10) {
            val visible = mutableListOf<LazyListItemInfo>()
            var currentY = -scrollOffset
            var idx = 0
            while (currentY < 1000 && idx < 50) {
                val h = itemHeight(idx)
                if (currentY + h > 0) {
                    visible.add(mockItem(index = idx, offset = currentY, size = h))
                }
                currentY += h
                idx++
            }

            val layout = mockLayoutInfo(totalItems = 50, visibleItems = visible)
            val fraction = estimator.computeFraction(layout)
            assertTrue(
                fraction >= previousFraction - 0.0001f,
                "Fraction should not decrease: got $fraction, previous was $previousFraction at scrollOffset $scrollOffset",
            )
            previousFraction = fraction
        }
    }

    private fun mockGridItem(index: Int, row: Int, offsetY: Int, height: Int): androidx.compose.foundation.lazy.grid.LazyGridItemInfo {
        val item = mockk<androidx.compose.foundation.lazy.grid.LazyGridItemInfo>()
        every { item.index } returns index
        every { item.row } returns row
        every { item.offset } returns androidx.compose.ui.unit.IntOffset(0, offsetY)
        every { item.size } returns androidx.compose.ui.unit.IntSize(100, height)
        return item
    }

    private fun mockGridState(
        totalItems: Int,
        visibleItems: List<androidx.compose.foundation.lazy.grid.LazyGridItemInfo>,
        viewportStartOffset: Int = 0,
        viewportEndOffset: Int = 1000,
    ): androidx.compose.foundation.lazy.grid.LazyGridState {
        val state = mockk<androidx.compose.foundation.lazy.grid.LazyGridState>()
        val layoutInfo = mockk<androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo>()
        every { state.layoutInfo } returns layoutInfo
        every { layoutInfo.totalItemsCount } returns totalItems
        every { layoutInfo.visibleItemsInfo } returns visibleItems
        every { layoutInfo.viewportStartOffset } returns viewportStartOffset
        every { layoutInfo.viewportEndOffset } returns viewportEndOffset
        return state
    }

    @Test
    fun `GridScrollbarEstimator maintains true column count when date header is at top`() {
        val estimator = GridScrollbarEstimator()

        // Frame 1: Date Header (row 0, span 3 -> 1 item) + Cards (row 1, 3 items)
        val frame1Items = listOf(
            mockGridItem(index = 0, row = 0, offsetY = 0, height = 40),
            mockGridItem(index = 1, row = 1, offsetY = 40, height = 180),
            mockGridItem(index = 2, row = 1, offsetY = 40, height = 180),
            mockGridItem(index = 3, row = 1, offsetY = 40, height = 180),
        )
        estimator.computeFraction(mockGridState(totalItems = 30, visibleItems = frame1Items))
        assertEquals(3, estimator.getColumnCount())

        // Frame 2: Scrolled down so Row 3 is a Date Header alone at the top of the viewport
        val frame2Items = listOf(
            mockGridItem(index = 7, row = 3, offsetY = 0, height = 40),
            mockGridItem(index = 8, row = 4, offsetY = 40, height = 180),
            mockGridItem(index = 9, row = 4, offsetY = 40, height = 180),
            mockGridItem(index = 10, row = 4, offsetY = 40, height = 180),
        )
        estimator.computeFraction(mockGridState(totalItems = 30, visibleItems = frame2Items))
        // Column count must remain 3, not collapse to 1!
        assertEquals(3, estimator.getColumnCount())
    }

    @Test
    fun `GridScrollbarEstimator computes smooth monotonic fraction across date headers`() {
        val estimator = GridScrollbarEstimator()
        var previousFraction = -1f

        // Grid with multiple sections: 40px headers (1 item) and 180px card rows (3 items)
        data class RowDef(val height: Int, val itemCount: Int)
        val pattern = listOf(
            RowDef(40, 1), // date header
            RowDef(180, 3), // cards
            RowDef(180, 3), // cards
            RowDef(180, 3), // cards
            RowDef(180, 3), // cards
        )
        val rows = (0 until 5).flatMap { pattern }
        val totalItems = rows.sumOf { it.itemCount }

        for (scrollOffset in 0..1000 step 15) {
            val visible = mutableListOf<androidx.compose.foundation.lazy.grid.LazyGridItemInfo>()
            var currentY = -scrollOffset
            var itemIdx = 0
            for ((rowIndex, rowDef) in rows.withIndex()) {
                val rowTop = currentY
                val rowBottom = currentY + rowDef.height
                if (rowBottom > 0 && rowTop < 1000) {
                    for (c in 0 until rowDef.itemCount) {
                        visible.add(mockGridItem(index = itemIdx + c, row = rowIndex, offsetY = rowTop, height = rowDef.height))
                    }
                }
                itemIdx += rowDef.itemCount
                currentY += rowDef.height
            }

            val state = mockGridState(totalItems = totalItems, visibleItems = visible)
            val fraction = estimator.computeFraction(state)
            assertTrue(
                fraction >= previousFraction - 0.0001f,
                "Grid fraction should not decrease: got $fraction, previous was $previousFraction at scrollOffset $scrollOffset",
            )
            previousFraction = fraction
        }
    }

    @Test
    fun `ScrollbarSectionEstimator maintains linear velocity across mixed item sizes`() {
        val estimator = ScrollbarSectionEstimator()
        // Index 0 is a 50px header; indices 1..29 are 300px cards
        fun itemH(index: Int): Int = if (index == 0) 50 else 300

        fun layoutAt(scrollOffset: Int): LazyListLayoutInfo {
            val visible = mutableListOf<LazyListItemInfo>()
            var y = -scrollOffset
            var idx = 0
            while (y < 1000 && idx < 30) {
                val h = itemH(idx)
                if (y + h > 0) {
                    visible.add(mockItem(index = idx, offset = y, size = h))
                }
                y += h
                idx++
            }
            return mockLayoutInfo(totalItems = 30, visibleItems = visible)
        }

        // Prime the estimator
        estimator.computeFraction(layoutAt(0))

        // Scroll 50px through the 50px header (from 0 to 50px)
        val fractionAt0 = estimator.computeFraction(layoutAt(0))
        val fractionAt50 = estimator.computeFraction(layoutAt(50))
        val deltaHeader50px = fractionAt50 - fractionAt0

        // Scroll 50px through the 300px card (from 50px to 100px)
        val fractionAt100 = estimator.computeFraction(layoutAt(100))
        val deltaCard50px = fractionAt100 - fractionAt50

        // The displacement per 50px must be practically identical (within 10%), NOT 6x different!
        assertEquals(deltaHeader50px, deltaCard50px, deltaHeader50px * 0.1f)
    }

    @Test
    fun `ScrollbarSectionEstimator has smooth continuous transition when item leaves viewport`() {
        val estimator = ScrollbarSectionEstimator()
        // Item 0 is 100px.
        // Frame A: Item 0 is at offset -99 (1px left).
        val frameAItems = listOf(
            mockItem(index = 0, offset = -99, size = 100),
            mockItem(index = 1, offset = 1, size = 100),
            mockItem(index = 2, offset = 101, size = 100),
        )
        // Frame B: Item 0 has just scrolled off; Item 1 is at offset 0.
        val frameBItems = listOf(
            mockItem(index = 1, offset = 0, size = 100),
            mockItem(index = 2, offset = 100, size = 100),
        )

        estimator.computeFraction(mockLayoutInfo(totalItems = 50, visibleItems = frameAItems))
        val fractionA = estimator.computeFraction(mockLayoutInfo(totalItems = 50, visibleItems = frameAItems))
        val fractionB = estimator.computeFraction(mockLayoutInfo(totalItems = 50, visibleItems = frameBItems))

        // Difference must be negligible (smooth step of ~1px scroll)
        assertEquals(fractionA, fractionB, 0.002f)
    }

    @Test
    fun `GridScrollbarEstimator maintains linear velocity across date header and card rows`() {
        val estimator = GridScrollbarEstimator()
        // Row 0 is a 40px date header (1 item).
        // Row 1..10 are 240px card rows (3 items each).
        fun rowH(rowIndex: Int): Int = if (rowIndex == 0) 40 else 240

        fun gridAt(scrollOffset: Int): androidx.compose.foundation.lazy.grid.LazyGridState {
            val visible = mutableListOf<androidx.compose.foundation.lazy.grid.LazyGridItemInfo>()
            var y = -scrollOffset
            var itemIdx = 0
            for (r in 0..10) {
                val h = rowH(r)
                val count = if (r == 0) 1 else 3
                if (y + h > 0 && y < 1000) {
                    for (c in 0 until count) {
                        visible.add(mockGridItem(index = itemIdx + c, row = r, offsetY = y, height = h))
                    }
                }
                itemIdx += count
                y += h
            }
            return mockGridState(totalItems = 31, visibleItems = visible)
        }

        // Prime the estimator
        estimator.computeFraction(gridAt(0))

        // Scroll 40px through the 40px date header
        val fractionAt0 = estimator.computeFraction(gridAt(0))
        val fractionAt40 = estimator.computeFraction(gridAt(40))
        val deltaHeader40px = fractionAt40 - fractionAt0

        // Scroll 40px through the 240px card row (from 40px to 80px)
        val fractionAt80 = estimator.computeFraction(gridAt(80))
        val deltaCard40px = fractionAt80 - fractionAt40

        // Must be practically equal (linear velocity), NOT 6x different!
        assertEquals(deltaHeader40px, deltaCard40px, deltaHeader40px * 0.1f)
    }

    @Test
    fun `findItemAndOffsetForFraction accurately inverts computeFraction`() {
        val estimator = ScrollbarSectionEstimator()
        val items = (0 until 10).map { mockItem(it, it * 100, 100) }
        val layout = mockLayoutInfo(totalItems = 100, visibleItems = items)
        estimator.computeFraction(layout)

        val targetFraction = 0.5f
        val (targetIndex, targetOffset) = estimator.findItemAndOffsetForFraction(targetFraction, layout)

        // 100 items of 100px = 10,000px. Viewport = 1,000px. Scrollable = 9,000px.
        // 50% scroll = 4,500px.
        // At 100px per item: index 45, offset 0.
        assertEquals(45, targetIndex)
        assertEquals(0, targetOffset)
    }

    @Test
    fun `findRowAndOffsetForFraction accurately inverts computeFraction for grid`() {
        val estimator = GridScrollbarEstimator()
        val visibleItems = (0 until 4).flatMap { row ->
            (0 until 3).map { col ->
                mockGridItem(index = row * 3 + col, row = row, offsetY = row * 200, height = 200)
            }
        }
        val state = mockGridState(totalItems = 60, visibleItems = visibleItems)
        estimator.computeFraction(state)

        val targetFraction = 0.5f
        val (targetRow, targetOffset, targetIndex) = estimator.findRowAndOffsetForFraction(targetFraction, state.layoutInfo)

        // 60 items in 3 columns = 20 rows.
        // 20 rows * 200px = 4,000px. Viewport = 1,000px. Scrollable = 3,000px.
        // 50% scroll = 1,500px.
        // At 200px per row: row 7 (1,400px) + 100px offset = row 7, offset 100, targetIndex = 7 * 3 = 21.
        assertEquals(7, targetRow)
        assertEquals(100, targetOffset)
        assertEquals(21, targetIndex)
    }
}
