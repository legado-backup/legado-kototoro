package org.skepsun.kototoro.core.dictionary

data class ParsedAiTranslation(
    val translatedText: String,
    val discoveredPairs: List<DictPair>,
)

object AiTranslationOutputParser {

    fun parse(raw: String, existing: List<DictPair> = emptyList()): ParsedAiTranslation {
        var section: String? = null
        val dictionaryLines = mutableListOf<String>()
        val resultLines = mutableListOf<String>()
        raw.lineSequence().forEach { line ->
            when {
                line.trim().equals("[dictionary]", ignoreCase = true) -> section = "dictionary"
                line.trim().equals("[result]", ignoreCase = true) -> section = "result"
                section == "dictionary" -> dictionaryLines += line
                section == "result" -> resultLines += line
            }
        }
        val translatedText = resultLines.joinToString("\n").trim().ifBlank { raw.trim() }
        val existingOriginals = existing.map { normalize(it.original) }.toSet()
        val pairs = dictionaryLines.mapNotNull(::parsePair)
            .filter { normalize(it.original) !in existingOriginals }
            .distinctBy { normalize(it.original) }
            .take(MAX_DISCOVERED_PAIRS)
        return ParsedAiTranslation(translatedText, pairs)
    }

    private fun parsePair(line: String): DictPair? {
        val trimmed = line.trim()
        if (trimmed.isBlank() || trimmed.startsWith('[')) return null
        val separator = when {
            " -> " in trimmed -> " -> "
            "->" in trimmed -> "->"
            " : " in trimmed -> " : "
            ":" in trimmed -> ":"
            else -> return null
        }
        val parts = trimmed.split(separator, limit = 2)
        if (parts.size != 2) return null
        val original = parts[0].trim()
        val translation = parts[1].trim()
        if (original.isBlank() || translation.isBlank()) return null
        if (original.none { it.isLetter() }) return null
        return DictPair(original, translation)
    }

    private fun normalize(value: String): String = value.trim().lowercase()

    private const val MAX_DISCOVERED_PAIRS = 10
}
