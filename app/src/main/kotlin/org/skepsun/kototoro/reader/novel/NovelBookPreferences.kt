package org.skepsun.kototoro.reader.novel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.skepsun.kototoro.parsers.model.Content
import javax.inject.Inject
import javax.inject.Singleton

/** Per-book switches used by the novel reader. Defaults are intentionally enabled. */
@Singleton
class NovelBookPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun areReplaceRulesEnabled(book: Content): Boolean = preferences.getBoolean(keyFor(book), true)

    fun setReplaceRulesEnabled(book: Content, enabled: Boolean) {
        preferences.edit().putBoolean(keyFor(book), enabled).apply()
    }

    fun getDisabledReplaceRuleIds(book: Content): Set<Long> =
        getDisabledReplaceRuleIds(keyFor(book))

    fun getDisabledReplaceRuleIds(bookKey: String): Set<Long> =
        preferences.getStringSet(disabledRulesKey(bookKey), emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun setReplaceRuleEnabled(book: Content, ruleId: Long, enabled: Boolean) {
        val bookKey = keyFor(book)
        val disabled = getDisabledReplaceRuleIds(bookKey).toMutableSet()
        if (enabled) {
            disabled.remove(ruleId)
        } else {
            disabled.add(ruleId)
        }
        preferences.edit()
            .putStringSet(disabledRulesKey(bookKey), disabled.map(Long::toString).toSet())
            .apply()
    }

    private fun keyFor(book: Content): String {
        val stableId = book.id.takeIf { it != 0L }?.toString()
            ?: "${book.source.name}:${book.url}"
        return "$REPLACE_RULES_KEY_PREFIX$stableId"
    }

    private fun disabledRulesKey(bookKey: String): String = "$DISABLED_RULES_KEY_PREFIX$bookKey"

    private companion object {
        const val PREFS_NAME = "legado_book_store"
        const val REPLACE_RULES_KEY_PREFIX = "kototoro_replace_rules_enabled_"
        const val DISABLED_RULES_KEY_PREFIX = "kototoro_replace_rules_disabled_"
    }
}
