package org.skepsun.kototoro.core.replace

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplaceRuleRepository @Inject constructor(
    private val dao: ReplaceRuleDao,
    @ApplicationContext context: Context,
) : ReplaceRuleSource {

    private val prefs =
        context.getSharedPreferences("replace_rules", Context.MODE_PRIVATE)

    private val migrationMutex = Mutex()

    suspend fun getAll(): List<ReplaceRule> = withContext(Dispatchers.IO) {
        ensureLegacyRulesMigrated()
        dao.getAll().sortedBy { it.order }
    }

    override suspend fun getContentRules(scopeName: String, origin: String): List<ReplaceRule> =
        withContext(Dispatchers.IO) {
            ensureLegacyRulesMigrated()
            val scopeValues = listOf(scopeName, origin)
            dao.getAll()
                .filter { it.isEnabled && it.isValid() && it.appliesTo(ReplaceRule.Scope.CONTENT, scopeValues) }
                .sortedBy { it.order }
        }

    override suspend fun getTitleRules(scopeName: String, origin: String): List<ReplaceRule> =
        withContext(Dispatchers.IO) {
            ensureLegacyRulesMigrated()
            val scopeValues = listOf(scopeName, origin)
            dao.getAll()
                .filter { it.isEnabled && it.isValid() && it.appliesTo(ReplaceRule.Scope.TITLE, scopeValues) }
                .sortedBy { it.order }
        }

    suspend fun getRulesForBook(scopeName: String, origin: String): List<ReplaceRule> =
        withContext(Dispatchers.IO) {
            ensureLegacyRulesMigrated()
            filterRulesForBook(dao.getAll(), scopeName, origin)
        }

    fun filterRulesForBook(
        rules: List<ReplaceRule>,
        scopeName: String,
        origin: String,
    ): List<ReplaceRule> {
        val scopeValues = listOf(scopeName, origin)
        return rules
            .filter { rule ->
                rule.isValid() && (
                    rule.appliesTo(ReplaceRule.Scope.CONTENT, scopeValues) ||
                        rule.appliesTo(ReplaceRule.Scope.TITLE, scopeValues)
                    )
            }
            .sortedBy { it.order }
    }

    fun observeAll(): Flow<List<ReplaceRule>> = flow {
        withContext(Dispatchers.IO) { ensureLegacyRulesMigrated() }
        emitAll(dao.observeAll().map { rules -> rules.sortedBy { it.order } })
    }

    suspend fun saveAll(rules: List<ReplaceRule>) = withContext(Dispatchers.IO) {
        ensureLegacyRulesMigrated()
        dao.replaceAll(normalizeIds(rules))
    }

    suspend fun importFromJson(importJson: String): Int = withContext(Dispatchers.IO) {
        ensureLegacyRulesMigrated()
        val imported = runCatching { ReplaceRuleJsonCodec.decode(importJson) }.getOrNull()
            ?: return@withContext 0
        val existing = dao.getAll().toMutableList()
        val existingIndexById = existing.mapIndexed { index, rule -> rule.id to index }.toMap()
        var importedCount = 0
        normalizeIds(imported).filter { it.pattern.isNotBlank() }.forEach { importedRule ->
            val existingIndex = existingIndexById[importedRule.id]
            if (existingIndex != null) {
                existing[existingIndex] = importedRule
            } else {
                existing += importedRule
            }
            importedCount++
        }
        dao.replaceAll(existing)
        importedCount
    }

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        ReplaceRuleJsonCodec.encode(getAll())
    }

    private suspend fun ensureLegacyRulesMigrated() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        migrationMutex.withLock {
            if (prefs.getBoolean(KEY_MIGRATED, false)) return
            val raw = prefs.getString(KEY_RULES, null)
            if (!raw.isNullOrBlank()) {
                val legacyRules = runCatching { ReplaceRuleJsonCodec.decode(raw) }
                    .getOrDefault(emptyList())
                    .filter { it.pattern.isNotBlank() }
                if (legacyRules.isNotEmpty()) {
                    val current = dao.getAll()
                    dao.replaceAll(normalizeIds(current + legacyRules))
                }
            }
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    private fun normalizeIds(rules: List<ReplaceRule>): List<ReplaceRule> {
        val usedIds = mutableSetOf<Long>()
        var nextId = maxOf(System.currentTimeMillis(), rules.maxOfOrNull { it.id } ?: 0L) + 1L
        return rules.map { rule ->
            val id = if (rule.id > 0L && usedIds.add(rule.id)) {
                rule.id
            } else {
                while (!usedIds.add(nextId)) nextId++
                nextId++
                nextId - 1L
            }
            rule.copy(id = id)
        }
    }

    companion object {
        private const val KEY_RULES = "rules"
        private const val KEY_MIGRATED = "room_migrated"
    }
}
