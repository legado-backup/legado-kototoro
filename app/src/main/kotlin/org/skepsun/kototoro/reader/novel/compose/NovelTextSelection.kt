package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingEntity
import kotlin.math.abs

enum class NovelTextSelectionAction {
    COPY,
    SHARE,
    DICTIONARY,
    BOOKMARK,
    HIGHLIGHT,
    NOTE,
    EXCERPT,
    ASK_AI,
    LISTEN,
    DELETE,
}

data class NovelTextSelection(
    val text: String,
    val chapterId: Long,
    val chapterIndex: Int,
    val chapterText: String,
    val renderedStart: Int? = null,
    val renderedText: String? = null,
    val selectionAnchor: Offset? = null,
    val anchorRect: androidx.compose.ui.geometry.Rect? = null,
    val sessionId: Long = 0L,
    val ownerId: String = "",
    val activeMarkingId: Long? = null,
    val clear: () -> Unit,
) {
    val renderedRange: IntRange?
        get() {
            val start = renderedStart ?: return null
            val rendered = renderedText ?: return null
            val localStart = rendered.indexOf(text)
            if (localStart < 0) return null
            return (start + localStart) until (start + localStart + text.length)
        }
}

internal class NovelSelectionTextToolbar(
    private val onShow: (androidx.compose.ui.geometry.Rect) -> Unit,
    private val onHide: () -> Unit,
) : TextToolbar {
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        status = TextToolbarStatus.Shown
        onShow(rect)
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        onHide()
    }
}

/** Adds selection reporting while retaining Compose's native handles without a second action bar. */
@Composable
internal fun NovelSelectionContainer(
    chapterId: Long,
    chapterIndex: Int,
    chapterText: String,
    onSelectionChanged: (NovelTextSelection?) -> Unit,
    ownerId: String = "novel-chapter-$chapterId-$chapterIndex",
    renderedStart: Int? = null,
    renderedText: String? = null,
    content: @Composable () -> Unit,
) {
    val selectionState = rememberSelectionState()
    var menuAnchorRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val textToolbar = remember {
        NovelSelectionTextToolbar(
            onShow = { rect ->
                menuAnchorRect = rect
            },
            onHide = {
                menuAnchorRect = null
            },
        )
    }
    val selectedText = remember(selectionState.selectedTexts) {
        selectionState.selectedTexts
            .joinToString(separator = "\n", transform = AnnotatedString::text)
            .trim()
    }
    LaunchedEffect(selectedText, chapterId, chapterIndex, chapterText, menuAnchorRect, ownerId) {
        if (selectedText.isBlank()) {
            menuAnchorRect = null
            onSelectionChanged(null)
        } else {
            val anchor = menuAnchorRect?.let { Offset(it.left + it.width / 2f, it.top) }
            onSelectionChanged(
                NovelTextSelection(
                    text = selectedText,
                    chapterId = chapterId,
                    chapterIndex = chapterIndex,
                    chapterText = chapterText,
                    renderedStart = renderedStart,
                    renderedText = renderedText,
                    selectionAnchor = anchor,
                    anchorRect = menuAnchorRect,
                    ownerId = ownerId,
                    clear = selectionState::clear,
                ),
            )
        }
    }
    CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
        SelectionContainer(
            state = selectionState,
            content = content,
        )
    }
}

@Composable
internal fun NovelMarkingText(
    text: AnnotatedString,
    style: TextStyle,
    textAlign: TextAlign,
    sourceRange: IntRange?,
    markings: List<NovelMarkingEntity>,
    onMarkingClick: (NovelMarkingEntity, androidx.compose.ui.geometry.Rect) -> Unit,
    textSelectionActive: Boolean,
    modifier: Modifier = Modifier,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentLayoutResult by rememberUpdatedState(layoutResult)
    val clickModifier = if (markings.isEmpty()) {
        Modifier
    } else {
        Modifier.pointerInput(text, sourceRange, markings, textSelectionActive, onMarkingClick) {
            detectNovelMarkingTap(
                layoutResult = { currentLayoutResult },
                resolveMarking = { position ->
                    currentLayoutResult?.let { layout ->
                        findNovelMarkingAtOffset(
                            textOffset = layout.getOffsetForPosition(position),
                            text = text.text,
                            sourceRange = sourceRange,
                            markings = markings,
                        )
                    }
                },
                selectionActive = textSelectionActive,
                onMarkingClick = { marking, releasePosition ->
                    val layout = currentLayoutResult
                    val coords = textCoordinates
                    val rootRect = if (layout != null && coords != null && coords.isAttached) {
                        val sourceStart = sourceRange?.first ?: 0
                        val localStart = (marking.startOffset - sourceStart).coerceIn(0, text.length)
                        val localEnd = (marking.endOffset - sourceStart).coerceIn(localStart, text.length)
                        val localBounds = if (localEnd > localStart) {
                            val tappedLine = layout.getLineForVerticalPosition(releasePosition.y)
                            val lineStart = layout.getLineStart(tappedLine)
                            val lineEnd = layout.getLineEnd(tappedLine)
                            val overlapStart = maxOf(localStart, lineStart)
                            val overlapEnd = minOf(localEnd, lineEnd)
                            if (overlapEnd > overlapStart) {
                                runCatching { layout.getPathForRange(overlapStart, overlapEnd).getBounds() }.getOrNull()
                                    ?: runCatching { layout.getPathForRange(localStart, localEnd).getBounds() }.getOrNull()
                                    ?: layout.getBoundingBox(overlapStart.coerceIn(0, (text.length - 1).coerceAtLeast(0)))
                            } else {
                                runCatching { layout.getPathForRange(localStart, localEnd).getBounds() }.getOrNull()
                                    ?: layout.getBoundingBox(localStart.coerceIn(0, (text.length - 1).coerceAtLeast(0)))
                            }
                        } else {
                            val charIndex = localStart.coerceIn(0, (text.length - 1).coerceAtLeast(0))
                            if (text.isNotEmpty()) layout.getBoundingBox(charIndex)
                            else androidx.compose.ui.geometry.Rect(releasePosition, androidx.compose.ui.geometry.Size(1f, 1f))
                        }
                        val rootTopLeft = coords.localToRoot(localBounds.topLeft)
                        val rootBottomRight = coords.localToRoot(localBounds.bottomRight)
                        androidx.compose.ui.geometry.Rect(rootTopLeft, rootBottomRight)
                    } else {
                        androidx.compose.ui.geometry.Rect(releasePosition, androidx.compose.ui.geometry.Size(1f, 1f))
                    }
                    onMarkingClick(marking, rootRect)
                },
            )
        }
    }
    androidx.compose.material3.Text(
        text = text,
        style = style,
        textAlign = textAlign,
        modifier = modifier
            .onGloballyPositioned { textCoordinates = it }
            .then(clickModifier)
            .fillMaxWidth(),
        onTextLayout = { layoutResult = it },
    )
}

internal fun findNovelMarkingAtOffset(
    textOffset: Int,
    text: String,
    sourceRange: IntRange?,
    markings: List<NovelMarkingEntity>,
): NovelMarkingEntity? {
    val sourceOffset = sourceRange?.first?.plus(textOffset)
    markings.firstOrNull { marking ->
        sourceOffset != null && marking.startOffset <= sourceOffset && sourceOffset < marking.endOffset
    }?.let { return it }
    return markings.firstOrNull { marking ->
        val localStart = text.indexOf(marking.selectedText)
        if (localStart >= 0) {
            textOffset in localStart until (localStart + marking.selectedText.length)
        } else {
            sourceOffset != null && marking.startOffset <= sourceOffset && sourceOffset < marking.endOffset
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectNovelMarkingTap(
    layoutResult: () -> TextLayoutResult?,
    resolveMarking: (Offset) -> NovelMarkingEntity?,
    selectionActive: Boolean,
    onMarkingClick: (NovelMarkingEntity, Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        var moved = false
        var up: Offset? = null
        var upChange: androidx.compose.ui.input.pointer.PointerInputChange? = null
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.pressed) {
                if (abs(change.position.x - down.position.x) > viewConfiguration.touchSlop ||
                    abs(change.position.y - down.position.y) > viewConfiguration.touchSlop
                ) {
                    moved = true
                }
            } else {
                up = change.position
                upChange = change
            }
        } while (change.pressed)
        val releasePosition = up ?: return@awaitEachGesture
        if (!moved && !selectionActive && upChange?.isConsumed != true) {
            val marking = resolveMarking(releasePosition)
            if (marking != null && layoutResult() != null) {
                upChange?.consume()
                onMarkingClick(marking, releasePosition)
            }
        }
    }
}

internal fun findNovelTextRange(
    source: String,
    selected: String,
    preferredStart: Int = 0,
): IntRange? {
    val cleanSelection = selected.trim()
    if (cleanSelection.isEmpty()) return null
    val exactStart = source.indexOf(cleanSelection, preferredStart.coerceAtLeast(0))
        .takeIf { it >= 0 }
        ?: source.indexOf(cleanSelection)
    if (exactStart >= 0) return exactStart until exactStart + cleanSelection.length

    val normalizedSource = normalizeNovelText(source)
    val normalizedSelection = normalizeNovelText(cleanSelection).first
    val normalizedStart = normalizedSource.first.indexOf(normalizedSelection)
    if (normalizedStart < 0) return null
    val offsets = normalizedSource.second
    val start = offsets[normalizedStart]
    val end = offsets[normalizedStart + normalizedSelection.length - 1] + 1
    return start until end
}

private fun normalizeNovelText(text: String): Pair<String, IntArray> {
    val normalized = StringBuilder(text.length)
    val offsets = ArrayList<Int>(text.length)
    var pendingWhitespace = false
    for (index in text.indices) {
        val char = text[index]
        if (char.isWhitespace()) {
            pendingWhitespace = normalized.isNotEmpty()
            continue
        }
        if (pendingWhitespace) {
            normalized.append(' ')
            offsets += index - 1
            pendingWhitespace = false
        }
        normalized.append(char)
        offsets += index
    }
    return normalized.toString() to offsets.toIntArray()
}
