package org.skepsun.kototoro.settings.replace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.skepsun.kototoro.core.replace.ReplaceRule
import org.skepsun.kototoro.core.replace.ReplaceRuleRepository

@HiltViewModel
class ReplaceRuleSettingsViewModel @Inject constructor(
    private val repository: ReplaceRuleRepository,
) : ViewModel() {

    val rules: StateFlow<List<ReplaceRule>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun save(rule: ReplaceRule) {
        viewModelScope.launch {
            val current = rules.value.toMutableList()
            val index = current.indexOfFirst { it.id == rule.id && rule.id != 0L }
            if (index >= 0) {
                current[index] = rule
            } else {
                current += rule
            }
            repository.saveAll(current)
            _events.emit(Event.Saved)
        }
    }

    fun setEnabled(rule: ReplaceRule, enabled: Boolean) {
        save(rule.copy(isEnabled = enabled))
    }

    fun setAllEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAll(rules.value.map { rule -> rule.copy(isEnabled = enabled) })
            _events.emit(Event.BulkUpdated(enabled))
        }
    }

    fun delete(rule: ReplaceRule) {
        viewModelScope.launch {
            repository.saveAll(rules.value.filterNot { it.id == rule.id })
            _events.emit(Event.Deleted)
        }
    }

    fun import(raw: String) {
        viewModelScope.launch {
            val count = repository.importFromJson(raw)
            _events.emit(Event.Imported(count))
        }
    }

    fun export(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(repository.exportToJson())
        }
    }

    sealed interface Event {
        data object Saved : Event
        data object Deleted : Event
        data class Imported(val count: Int) : Event
        data class BulkUpdated(val enabled: Boolean) : Event
    }
}
