package org.skepsun.kototoro.reader.ui.compose

internal data class ReaderBottomChromeVisibility(
    val visible: Boolean,
    val progressVisible: Boolean,
    val chapterTitleVisible: Boolean,
    val floatingControlsVisible: Boolean = false,
)

internal fun resolveReaderBottomChromeVisibility(
    controlsVisible: Boolean,
    progressAvailable: Boolean,
    chapterTitleAtBottom: Boolean,
    floatingControlsAvailable: Boolean = false,
    floatingControlsAllowed: Boolean = true,
): ReaderBottomChromeVisibility {
    val floatingControlsVisible = controlsVisible && floatingControlsAvailable && floatingControlsAllowed
    val visible = controlsVisible && (progressAvailable || chapterTitleAtBottom || floatingControlsVisible)
    return ReaderBottomChromeVisibility(
        visible = visible,
        progressVisible = visible && progressAvailable,
        chapterTitleVisible = visible && chapterTitleAtBottom,
        floatingControlsVisible = floatingControlsVisible,
    )
}
