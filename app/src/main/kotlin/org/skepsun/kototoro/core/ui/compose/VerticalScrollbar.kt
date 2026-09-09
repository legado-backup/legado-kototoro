package org.skepsun.kototoro.core.ui.compose

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.R as appcompatR
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.util.ext.getThemeColor
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val SCROLLBAR_HIDE_DELAY_MS = 1000L

private val FastScrollTouchWidth = 36.dp
private val FastScrollInsetEnd = 2.dp

/**
 * CompositionLocal indicating whether a scrollbar is actively visible or being dragged.
 * Used by parent chrome and overlay elements (e.g. Space switcher handle) to dynamically avoid overlapping.
 */
val LocalScrollbarActive = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

@Composable
fun BoxScope.VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    width: Dp = dimensionResource(R.dimen.fastscroll_handle_width),
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Transparent,
    draggable: Boolean = true,
    alwaysVisible: Boolean = false,
    endInset: Dp = FastScrollInsetEnd,
    labelProvider: ((Int) -> String)? = null,
) {
    val estimator = remember { ScrollbarSectionEstimator() }
    FastScrollbar(
        modifier = modifier,
        contentPadding = contentPadding,
        width = width,
        color = color,
        trackColor = trackColor,
        draggable = draggable,
        alwaysVisible = alwaysVisible,
        endInset = endInset,
        labelProvider = labelProvider,
        totalItemsCount = { state.layoutInfo.totalItemsCount },
        visibleItemsCount = { state.layoutInfo.visibleItemsInfo.size },
        scrollFraction = { estimator.computeFraction(state.layoutInfo) },
        isScrollInProgress = { state.isScrollInProgress },
        onFastScrollToFraction = { fraction ->
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 0) return@FastScrollbar
            if (fraction <= 0.001f) {
                state.requestScrollToItem(0, 0)
                return@FastScrollbar
            }
            if (fraction >= 0.999f) {
                state.requestScrollToItem(totalItems - 1, Int.MAX_VALUE)
                return@FastScrollbar
            }
            val maxSections = estimator.getMaxSections().coerceAtLeast(1f)
            val targetProgress = (fraction * maxSections).coerceIn(0f, maxSections)
            val targetIndex = targetProgress.toInt().coerceIn(0, totalItems - 1)
            val fractionalPart = targetProgress - targetIndex
            val visibleItems = layoutInfo.visibleItemsInfo
            val itemSize = visibleItems.find { it.index == targetIndex }?.size
                ?: visibleItems.map { it.size }.average().takeIf { it > 0 }?.toInt()
                ?: 100
            val targetOffset = (fractionalPart * itemSize).roundToInt()
            state.requestScrollToItem(targetIndex, targetOffset)
        },
    )
}

@Composable
fun BoxScope.VerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    width: Dp = dimensionResource(R.dimen.fastscroll_handle_width),
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Transparent,
    draggable: Boolean = true,
    alwaysVisible: Boolean = false,
    endInset: Dp = FastScrollInsetEnd,
    labelProvider: ((Int) -> String)? = null,
) {
    FastScrollbar(
        modifier = modifier,
        contentPadding = contentPadding,
        width = width,
        color = color,
        trackColor = trackColor,
        draggable = draggable,
        alwaysVisible = alwaysVisible,
        endInset = endInset,
        labelProvider = labelProvider,
        totalItemsCount = { state.layoutInfo.totalItemsCount },
        visibleItemsCount = { state.layoutInfo.visibleItemsInfo.size },
        scrollFraction = { state.smoothGridScrollFraction() },
        isScrollInProgress = { state.isScrollInProgress },
        onFastScrollToFraction = { fraction ->
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems <= 0) return@FastScrollbar
            if (fraction <= 0.001f) {
                state.requestScrollToItem(0, 0)
                return@FastScrollbar
            }
            if (fraction >= 0.999f) {
                state.requestScrollToItem(totalItems - 1, Int.MAX_VALUE)
                return@FastScrollbar
            }
            val columnCount = state.calculateColumnCount()
            val scrollRange = computeGridScrollRange(state, columnCount)
            val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
            val extraScrollRange = (scrollRange.toFloat() - viewportHeight).coerceAtLeast(1f)
            val scrollAmt = fraction * extraScrollRange

            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                state.requestScrollToItem((fraction * (totalItems - 1)).toInt(), 0)
                return@FastScrollbar
            }
            val laidOutArea = (visibleItems.last().offset.y + visibleItems.last().size.height) - visibleItems.first().offset.y
            val laidOutRows = (1 + abs(visibleItems.last().index - visibleItems.first().index) / columnCount).coerceAtLeast(1)
            val avgSizePerRow = (laidOutArea.toFloat() / laidOutRows).coerceAtLeast(1f)

            val rowNumber = (scrollAmt / avgSizePerRow).toInt()
            val rowOffset = (scrollAmt - rowNumber * avgSizePerRow).roundToInt()
            val targetIndex = (columnCount * rowNumber).coerceIn(0, totalItems - 1)
            state.requestScrollToItem(targetIndex, rowOffset)
        },
    )
}

@Composable
private fun BoxScope.FastScrollbar(
    modifier: Modifier,
    contentPadding: PaddingValues,
    width: Dp,
    color: Color,
    trackColor: Color,
    draggable: Boolean,
    alwaysVisible: Boolean,
    endInset: Dp,
    labelProvider: ((Int) -> String)?,
    totalItemsCount: () -> Int,
    visibleItemsCount: () -> Int,
    scrollFraction: () -> Float,
    isScrollInProgress: () -> Boolean,
    onFastScrollToFraction: (Float) -> Unit,
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val localScrollbarActive = LocalScrollbarActive.current
    val showScrollbar by remember {
        derivedStateOf {
            val totalItems = totalItemsCount()
            totalItems > 0 && totalItems > visibleItemsCount()
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var keepVisible by remember { mutableStateOf(false) }
    var lastBubbleIndex by remember { mutableIntStateOf(-1) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var bubbleText by remember { mutableStateOf<String?>(null) }
    val scrolling = isScrollInProgress()

    LaunchedEffect(scrolling, isDragging, showScrollbar) {
        if ((scrolling || isDragging) && showScrollbar) {
            keepVisible = true
        } else {
            delay(SCROLLBAR_HIDE_DELAY_MS)
            keepVisible = false
        }
    }

    val isActive = showScrollbar && (alwaysVisible || keepVisible || isDragging)
    DisposableEffect(isActive) {
        localScrollbarActive.value = isActive
        onDispose {
            localScrollbarActive.value = false
        }
    }

    val targetAlpha = if (isActive) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = if (targetAlpha > 0f) 150 else 300),
        label = "ScrollbarAlpha",
    )

    val density = LocalDensity.current
    val handleColor = remember(context, color) {
        if (color == Color.Unspecified) {
            Color(context.getThemeColor(appcompatR.attr.colorControlNormal, android.graphics.Color.DKGRAY))
        } else {
            color
        }
    }
    val handleHeight = dimensionResource(R.dimen.fastscroll_handle_height)
    val handleRadius = dimensionResource(R.dimen.fastscroll_handle_radius)
    val scrollbarMarginTop = dimensionResource(R.dimen.fastscroll_scrollbar_margin_top)
    val scrollbarMarginBottom = dimensionResource(R.dimen.fastscroll_scrollbar_margin_bottom)
    val layoutDirection = LocalLayoutDirection.current
    val effectiveTopPadding = contentPadding.calculateTopPadding() + scrollbarMarginTop
    val effectiveBottomPadding = contentPadding.calculateBottomPadding() + scrollbarMarginBottom
    val effectiveEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    val handleWidthPx = with(density) { width.toPx() }
    val handleHeightPx = with(density) { handleHeight.toPx() }
    val handleRadiusPx = with(density) { handleRadius.toPx() }
    val handleEndPaddingPx = with(density) { endInset.toPx() }

    Box(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(
                top = effectiveTopPadding,
                bottom = effectiveBottomPadding,
                end = effectiveEndPadding,
            )
            .width(FastScrollTouchWidth)
            .then(
                if (showScrollbar && (alwaysVisible || keepVisible)) {
                    Modifier.systemGestureExclusion()
                } else {
                    Modifier
                },
            )
            .then(
                if (draggable) {
                    Modifier.fastScrollbarPointerInput(
                        rootView = rootView,
                        showScrollbar = { showScrollbar },
                        handleHeightPx = handleHeightPx,
                        onDragStart = {
                            isDragging = true
                            keepVisible = true
                            lastBubbleIndex = -1
                        },
                        onDragStop = {
                            isDragging = false
                            bubbleText = null
                        },
                        onDragFraction = { fraction ->
                            dragFraction = fraction
                            onFastScrollToFraction(fraction)
                            if (labelProvider != null) {
                                val total = totalItemsCount()
                                if (total > 0) {
                                    val targetIndex = (fraction * (total - 1)).roundToInt().coerceIn(0, total - 1)
                                    if (targetIndex != lastBubbleIndex) {
                                        lastBubbleIndex = targetIndex
                                        val newLabel = labelProvider(targetIndex)
                                        if (newLabel != bubbleText) {
                                            bubbleText = newLabel
                                            rootView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                }
                            }
                        },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size -> trackHeightPx = size.height.toFloat() },
        ) {
            val totalItems = totalItemsCount()
            val visibleItems = visibleItemsCount()
            val currentScrollFraction = if (isDragging) {
                dragFraction
            } else {
                scrollFraction()
            }
            val barHeightPx = handleHeightPx.coerceAtMost(trackHeightPx)
            val barTopPx = (trackHeightPx - barHeightPx) * currentScrollFraction.coerceIn(0f, 1f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (alpha <= 0f || !showScrollbar || totalItems <= 0 || visibleItems <= 0 || totalItems <= visibleItems) {
                    return@Canvas
                }

                val barLeft = size.width - handleEndPaddingPx - handleWidthPx
                val trackWidthPx = (handleWidthPx / 3f).coerceAtLeast(1f)
                drawRoundRect(
                    color = trackColor.copy(alpha = trackColor.alpha * alpha),
                    topLeft = Offset(
                        x = barLeft + (handleWidthPx - trackWidthPx) / 2f,
                        y = 0f,
                    ),
                    size = Size(trackWidthPx, size.height),
                    cornerRadius = CornerRadius(trackWidthPx / 2f),
                )
                drawRoundRect(
                    color = handleColor.copy(alpha = handleColor.alpha * alpha),
                    topLeft = Offset(barLeft, barTopPx),
                    size = Size(handleWidthPx, barHeightPx),
                    cornerRadius = CornerRadius(handleRadiusPx),
                )
            }

            if (isDragging && !bubbleText.isNullOrEmpty() && alpha > 0f && showScrollbar) {
                FastScrollBubble(
                    text = bubbleText.orEmpty(),
                    barTopPx = barTopPx,
                    barHeightPx = barHeightPx,
                    trackHeightPx = trackHeightPx,
                    color = handleColor,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun FastScrollBubble(
    text: String,
    barTopPx: Float,
    barHeightPx: Float,
    trackHeightPx: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bubbleSize = dimensionResource(R.dimen.fastscroll_bubble_size_small)
    val bubblePadding = dimensionResource(R.dimen.fastscroll_bubble_padding_small)
    val bubbleSizePx = with(density) { bubbleSize.toPx() }
    val bubbleTopPx = (barTopPx + barHeightPx / 2f - bubbleSizePx / 2f)
        .coerceIn(0f, (trackHeightPx - bubbleSizePx).coerceAtLeast(0f))
    Box(
        modifier = modifier
            .wrapContentSize(unbounded = true, align = Alignment.TopEnd)
            .padding(end = FastScrollTouchWidth + 8.dp)
            .padding(top = with(density) { bubbleTopPx.toDp() })
            .requiredSizeIn(minWidth = bubbleSize, minHeight = bubbleSize)
            .heightIn(min = bubbleSize)
            .background(color = color, shape = MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = bubblePadding),
        )
    }
}

private fun Modifier.fastScrollbarPointerInput(
    rootView: View,
    showScrollbar: () -> Boolean,
    handleHeightPx: Float,
    onDragStart: () -> Unit,
    onDragStop: () -> Unit,
    onDragFraction: (fraction: Float) -> Unit,
): Modifier = pointerInput(rootView, handleHeightPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!showScrollbar()) {
            return@awaitEachGesture
        }

        rootView.parent?.requestDisallowInterceptTouchEvent(true)
        onDragStart()
        down.consume()

        fun updateFraction(y: Float) {
            val availableHeight = (size.height - handleHeightPx).coerceAtLeast(1f)
            val fraction = (y - handleHeightPx / 2f).coerceIn(0f, availableHeight) / availableHeight
            onDragFraction(fraction)
        }

        try {
            updateFraction(down.position.y)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    break
                }
                change.consume()
                updateFraction(change.position.y)
            }
        } finally {
            onDragStop()
            rootView.parent?.requestDisallowInterceptTouchEvent(false)
        }
    }
}

internal class ScrollbarSectionEstimator {
    private var maxSections = 1f
    private var lastTotalItems = 0

    fun computeFraction(layoutInfo: LazyListLayoutInfo): Float {
        val visibleItems = layoutInfo.visibleItemsInfo
        val totalItems = layoutInfo.totalItemsCount
        if (visibleItems.isEmpty() || totalItems <= visibleItems.size) {
            return 0f
        }
        if (totalItems != lastTotalItems) {
            lastTotalItems = totalItems
            maxSections = 1f
        }

        val topItem = visibleItems.first()
        val bottomItem = visibleItems.last()
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()

        val topHiddenProportion = (-topItem.offset.toFloat() / topItem.size.coerceAtLeast(1)).coerceAtLeast(0f)
        val bottomHiddenProportion = ((bottomItem.offset + bottomItem.size - viewportHeight) / bottomItem.size.coerceAtLeast(1)).coerceAtLeast(0f)

        val previousSections = topItem.index + topHiddenProportion
        val remainingSections = (totalItems - (bottomItem.index + 1)).coerceAtLeast(0) + bottomHiddenProportion
        val scrollableSections = (previousSections + remainingSections).coerceAtLeast(1f)

        if (scrollableSections > maxSections) {
            maxSections = scrollableSections
        }

        return (previousSections / maxSections).coerceIn(0f, 1f)
    }

    fun getMaxSections(): Float = maxSections
}

private fun LazyGridState.calculateColumnCount(): Int {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 1
    val firstLineTop = visibleItems.minOf { it.offset.y }
    return visibleItems.count { it.offset.y == firstLineTop }.coerceAtLeast(1)
}

private fun computeGridScrollOffset(state: LazyGridState, columnCount: Int): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val laidOutArea = (endChild.offset.y + endChild.size.height) - startChild.offset.y
    val laidOutRows = (1 + abs(endChild.index - startChild.index) / columnCount).coerceAtLeast(1)
    val avgSizePerRow = (laidOutArea.toFloat() / laidOutRows).coerceAtLeast(1f)

    val rowsBefore = min(startChild.index, endChild.index).coerceAtLeast(0) / columnCount
    return (rowsBefore * avgSizePerRow - startChild.offset.y).roundToInt()
}

private fun computeGridScrollRange(state: LazyGridState, columnCount: Int): Int {
    if (state.layoutInfo.totalItemsCount == 0) return 0
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0
    val startChild = visibleItems.first()
    val endChild = visibleItems.last()
    val laidOutArea = (endChild.offset.y + endChild.size.height) - startChild.offset.y
    val laidOutRows = (1 + abs(endChild.index - startChild.index) / columnCount).coerceAtLeast(1)
    val avgSizePerRow = (laidOutArea.toFloat() / laidOutRows).coerceAtLeast(1f)

    val totalRows = 1 + (state.layoutInfo.totalItemsCount - 1) / columnCount
    val endSpacing = avgSizePerRow - endChild.size.height
    return (endSpacing + (laidOutArea.toFloat() / laidOutRows) * totalRows).roundToInt()
}

private fun LazyGridState.smoothGridScrollFraction(): Float {
    val layoutInfo = layoutInfo
    if (layoutInfo.totalItemsCount == 0 || layoutInfo.visibleItemsInfo.isEmpty()) return 0f
    val columnCount = calculateColumnCount()
    val scrollOffset = computeGridScrollOffset(this, columnCount)
    val scrollRange = computeGridScrollRange(this, columnCount)
    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    val extraScrollRange = (scrollRange.toFloat() - viewportHeight).coerceAtLeast(1f)
    return (scrollOffset.toFloat() / extraScrollRange).coerceIn(0f, 1f)
}
