package org.skepsun.kototoro.reader.novel

import androidx.annotation.StringRes
import org.skepsun.kototoro.R

/** Immutable text window captured when the user opens the novel AI prompt. */
data class NovelAiContext(
    val before: String = "",
    val after: String = "",
) {
    val isNotEmpty: Boolean
        get() = before.isNotBlank() || after.isNotBlank()
}

internal data class NovelAiAskTarget(
    val bookTitle: String,
    val chapterTitle: String,
    val excerpt: String,
    val context: NovelAiContext,
)

internal enum class NovelAiQuestionPreset(
    @StringRes val questionRes: Int,
) {
    SUMMARY(R.string.novel_ai_question_summary),
    CHARACTER_MOTIVATION(R.string.novel_ai_question_character_motivation),
    CHAPTER_ROLE(R.string.novel_ai_question_chapter_role),
    EXPLAIN_DETAILS(R.string.novel_ai_question_explain_details),
}

internal val novelAiQuestionPresets: List<NovelAiQuestionPreset> = NovelAiQuestionPreset.entries

internal fun buildNovelAiContext(
    chapterText: String,
    excerptRange: IntRange?,
    maxCharsPerSide: Int = 720,
): NovelAiContext {
    if (chapterText.isBlank() || excerptRange == null || maxCharsPerSide <= 0) {
        return NovelAiContext()
    }

    val start = excerptRange.first.coerceIn(0, chapterText.length)
    val end = (excerptRange.last + 1).coerceIn(start, chapterText.length)
    if (end <= start) return NovelAiContext()

    return NovelAiContext(
        before = cropNovelAiContext(chapterText.substring(0, start), maxCharsPerSide, takeLast = true),
        after = cropNovelAiContext(chapterText.substring(end), maxCharsPerSide, takeLast = false),
    )
}

private fun cropNovelAiContext(text: String, maxChars: Int, takeLast: Boolean): String {
    val trimmed = text.trim()
    if (trimmed.length <= maxChars) return trimmed
    return if (takeLast) {
        "…" + trimmed.takeLast(maxChars)
    } else {
        trimmed.take(maxChars) + "…"
    }
}
