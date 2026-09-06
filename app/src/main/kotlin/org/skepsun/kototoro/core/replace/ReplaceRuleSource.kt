package org.skepsun.kototoro.core.replace

/**
 * Small seam used by text consumers. The storage implementation may change without making the
 * reader, search, or annotation modules know about Room.
 */
interface ReplaceRuleSource {
    suspend fun getContentRules(scopeName: String = "", origin: String = ""): List<ReplaceRule>

    suspend fun getTitleRules(scopeName: String = "", origin: String = ""): List<ReplaceRule> = emptyList()
}
