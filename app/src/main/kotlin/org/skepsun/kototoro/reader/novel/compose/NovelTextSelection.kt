package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString

enum class NovelTextSelectionAction {
    COPY,
    SHARE,
    DICTIONARY,
    HIGHLIGHT,
    NOTE,
}

data class NovelTextSelection(
    val text: String,
    val chapterId: Long,
    val chapterIndex: Int,
    val chapterText: String,
    val renderedStart: Int? = null,
    val renderedText: String? = null,
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

/** Adds selection reporting while retaining Compose's native handles and copy menu. */
@Composable
internal fun NovelSelectionContainer(
    chapterId: Long,
    chapterIndex: Int,
    chapterText: String,
    onSelectionChanged: (NovelTextSelection?) -> Unit,
    renderedStart: Int? = null,
    renderedText: String? = null,
    content: @Composable () -> Unit,
) {
    val selectionState = rememberSelectionState()
    val selectedText = remember(selectionState.selectedTexts) {
        selectionState.selectedTexts
            .joinToString(separator = "\n", transform = AnnotatedString::text)
            .trim()
    }
    LaunchedEffect(selectedText, chapterId, chapterIndex, chapterText) {
        onSelectionChanged(
            selectedText.takeIf(String::isNotBlank)?.let {
                NovelTextSelection(
                    text = it,
                    chapterId = chapterId,
                    chapterIndex = chapterIndex,
                    chapterText = chapterText,
                    renderedStart = renderedStart,
                    renderedText = renderedText,
                    clear = selectionState::clear,
                )
            },
        )
    }
    SelectionContainer(state = selectionState, content = content)
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
