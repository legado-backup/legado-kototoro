package org.skepsun.kototoro.settings.sources

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import org.skepsun.kototoro.R
import org.skepsun.kototoro.settings.compose.SettingsChoiceOption
import org.skepsun.kototoro.settings.compose.SourceSettingsActionRowUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsChoiceRowUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsGroupLabelRowUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsMultiChoiceRowUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsRowUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsSectionUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsSwitchRowUiState
import org.skepsun.kototoro.settings.compose.SourceSettingsTextRowUiState

internal class ComposePreferenceAdapter(
    private val context: Context,
    private val sharedPreferencesName: String,
) {
    fun createScreen(): PreferenceScreen {
        val manager = PreferenceManager(context).apply {
            sharedPreferencesName = this@ComposePreferenceAdapter.sharedPreferencesName
        }
        return manager.createPreferenceScreen(context)
    }

    fun buildSections(
        screen: PreferenceScreen,
        onUnsupportedPreference: (Preference) -> Unit = {},
    ): List<SourceSettingsSectionUiState> {
        val sections = mutableListOf<SourceSettingsSectionUiState>()
        val defaultRows = mutableListOf<SourceSettingsRowUiState>()
        var defaultSectionIndex = 0

        fun flushDefaultRows() {
            if (defaultRows.isEmpty()) {
                return
            }
            sections += SourceSettingsSectionUiState(
                id = "default_$defaultSectionIndex",
                title = context.getString(R.string.settings),
                rows = defaultRows.toList(),
            )
            defaultRows.clear()
            defaultSectionIndex += 1
        }

        for (index in 0 until screen.preferenceCount) {
            val preference = screen.getPreference(index)
            when (preference) {
                is PreferenceCategory -> {
                    flushDefaultRows()
                    preference.toSection(onUnsupportedPreference)?.let(sections::add)
                }

                is PreferenceGroup -> {
                    val section = preference.toGroupSection(onUnsupportedPreference)
                    if (section != null) {
                        flushDefaultRows()
                        sections += section
                    } else {
                        flattenGroupRows(preference, onUnsupportedPreference).forEach(defaultRows::add)
                    }
                }

                else -> preference.toRow(onUnsupportedPreference)?.let(defaultRows::add)
            }
        }
        flushDefaultRows()
        return sections
    }

    private fun PreferenceCategory.toSection(
        onUnsupportedPreference: (Preference) -> Unit,
    ): SourceSettingsSectionUiState? {
        val rows = mutableListOf<SourceSettingsRowUiState>()
        for (index in 0 until preferenceCount) {
            val child = getPreference(index)
            when (child) {
                is PreferenceCategory -> {
                    child.title?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { title ->
                            rows += SourceSettingsGroupLabelRowUiState(
                                id = rowIdFor(child),
                                text = title,
                            )
                        }
                    for (nestedIndex in 0 until child.preferenceCount) {
                        child.getPreference(nestedIndex).toRow(onUnsupportedPreference)?.let(rows::add)
                    }
                }

                else -> child.toRow(onUnsupportedPreference)?.let(rows::add)
            }
        }
        if (rows.isEmpty()) {
            return null
        }
        return SourceSettingsSectionUiState(
            id = sectionIdFor(this),
            title = title?.toString().orEmpty().ifBlank { context.getString(R.string.settings) },
            rows = rows,
        )
    }

    private fun PreferenceGroup.toGroupSection(
        onUnsupportedPreference: (Preference) -> Unit,
    ): SourceSettingsSectionUiState? {
        val rows = flattenGroupRows(this, onUnsupportedPreference)
        if (rows.isEmpty()) {
            return null
        }
        val groupTitle = title?.toString().orEmpty().ifBlank { return null }
        return SourceSettingsSectionUiState(
            id = sectionIdFor(this),
            title = groupTitle,
            rows = rows,
        )
    }

    private fun flattenGroupRows(
        group: PreferenceGroup,
        onUnsupportedPreference: (Preference) -> Unit,
    ): List<SourceSettingsRowUiState> {
        val rows = mutableListOf<SourceSettingsRowUiState>()
        for (index in 0 until group.preferenceCount) {
            val child = group.getPreference(index)
            when (child) {
                is PreferenceCategory -> child.toSection(onUnsupportedPreference)?.rows?.let(rows::addAll)
                is PreferenceGroup -> {
                    child.title?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { title ->
                            rows += SourceSettingsGroupLabelRowUiState(
                                id = rowIdFor(child),
                                text = title,
                            )
                        }
                    rows += flattenGroupRows(child, onUnsupportedPreference)
                }

                else -> child.toRow(onUnsupportedPreference)?.let(rows::add)
            }
        }
        return rows
    }

    private fun Preference.toRow(
        onUnsupportedPreference: (Preference) -> Unit,
    ): SourceSettingsRowUiState? {
        if (!isVisible) {
            return null
        }
        return when (this) {
            is TwoStatePreference -> SourceSettingsSwitchRowUiState(
                id = rowIdFor(this),
                title = rowTitle(),
                checked = isChecked,
                summary = rowSummary(),
                enabled = isEnabled,
                onCheckedChange = { checked ->
                    if (callChangeListener(checked)) {
                        isChecked = checked
                    }
                },
            )

            is ListPreference -> SourceSettingsChoiceRowUiState(
                id = rowIdFor(this),
                title = rowTitle(),
                value = value.orEmpty(),
                options = buildList {
                    entries.orEmpty().forEachIndexed { index, entry ->
                        add(
                            SettingsChoiceOption(
                                value = entryValues?.getOrNull(index)?.toString().orEmpty(),
                                label = entry?.toString().orEmpty(),
                            ),
                        )
                    }
                },
                summary = rowSummary(),
                enabled = isEnabled,
                onValueChange = { newValue ->
                    if (callChangeListener(newValue)) {
                        value = newValue
                    }
                },
            )

            is MultiSelectListPreference -> SourceSettingsMultiChoiceRowUiState(
                id = rowIdFor(this),
                title = rowTitle(),
                values = values,
                options = buildList {
                    entries.orEmpty().forEachIndexed { index, entry ->
                        add(
                            SettingsChoiceOption(
                                value = entryValues?.getOrNull(index)?.toString().orEmpty(),
                                label = entry?.toString().orEmpty(),
                            ),
                        )
                    }
                },
                emptySelectionText = context.getString(R.string.not_specified),
                summary = rowSummary(),
                enabled = isEnabled,
                onValueChange = { newValues ->
                    if (callChangeListener(newValues)) {
                        values = newValues
                    }
                },
            )

            is EditTextPreference -> SourceSettingsTextRowUiState(
                id = rowIdFor(this),
                title = rowTitle(),
                value = text.orEmpty(),
                summary = rowSummary(),
                placeholder = dialogMessage?.toString()?.takeIf { it.isNotBlank() },
                isPassword = isLikelyPassword(),
                enabled = isEnabled,
                onValueChange = { newValue ->
                    if (callChangeListener(newValue)) {
                        text = newValue
                    }
                },
            )

            is PreferenceCategory -> null

            else -> {
                if (this is PreferenceGroup || fragment != null) {
                    onUnsupportedPreference(this)
                    null
                } else if (!isSelectable && intent == null && onPreferenceClickListener == null) {
                    SourceSettingsGroupLabelRowUiState(
                        id = rowIdFor(this),
                        text = rowTitle(),
                    )
                } else {
                    SourceSettingsActionRowUiState(
                        id = rowIdFor(this),
                        title = rowTitle(),
                        summary = rowSummary(),
                        enabled = isEnabled,
                        showChevron = isSelectable,
                        onClick = {
                            val handled = onPreferenceClickListener?.onPreferenceClick(this) == true
                            if (!handled) {
                                intent?.let(context::startActivity)
                            }
                        },
                    )
                }
            }
        }
    }

    private fun Preference.rowTitle(): String {
        return title?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: key
            ?: context.getString(R.string.settings)
    }

    private fun Preference.rowSummary(): String? {
        return summary?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun EditTextPreference.isLikelyPassword(): Boolean {
        val lowerKey = key?.lowercase().orEmpty()
        val lowerTitle = title?.toString()?.lowercase().orEmpty()
        return "password" in lowerKey || "password" in lowerTitle
    }

    private fun rowIdFor(preference: Preference): String {
        return preference.key ?: "row_${preference.javaClass.simpleName}_${System.identityHashCode(preference)}"
    }

    private fun sectionIdFor(preference: Preference): String {
        return preference.key ?: "section_${preference.javaClass.simpleName}_${System.identityHashCode(preference)}"
    }
}
