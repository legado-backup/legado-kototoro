package org.skepsun.kototoro.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.skepsun.kototoro.core.dictionary.DictionaryRule
import org.skepsun.kototoro.core.dictionary.DictionaryRuleRepository
import javax.inject.Inject

@HiltViewModel
class DictionaryRuleSettingsViewModel @Inject constructor(
    private val repository: DictionaryRuleRepository,
) : ViewModel() {

    val rules: StateFlow<List<DictionaryRule>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun save(rule: DictionaryRule) {
        viewModelScope.launch {
            val current = rules.value.toMutableList()
            val index = current.indexOfFirst { it.name == rule.name }
            if (index >= 0) current[index] = rule else current += rule
            repository.saveAll(current)
        }
    }

    fun setEnabled(rule: DictionaryRule, enabled: Boolean) = save(rule.copy(enabled = enabled))

    fun delete(rule: DictionaryRule) {
        viewModelScope.launch {
            repository.saveAll(rules.value.filterNot { it.name == rule.name })
        }
    }

    fun import(raw: String) {
        viewModelScope.launch { repository.importFromJson(raw) }
    }

    fun export(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(repository.exportToJson()) }
    }
}
