package org.skepsun.kototoro.settings.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.replace.ReplaceRule

@Composable
fun ReplaceRulesSettingsScreen(
    rules: List<ReplaceRule>,
    onToggle: (ReplaceRule, Boolean) -> Unit,
    onSetAllEnabled: (Boolean) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onSave: (ReplaceRule) -> Unit,
    onDelete: (ReplaceRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingRule by remember { mutableStateOf<ReplaceRule?>(null) }
    var isEditorVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = settingsContentTopInset(),
                start = SettingsContentHorizontalPadding,
                end = SettingsContentHorizontalPadding,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsPreferenceGroup(title = stringResource(R.string.replace_rules_actions)) {
            item {
                SettingsActionPreference(
                    title = stringResource(R.string.replace_rule_add),
                    summary = stringResource(R.string.replace_rule_add_summary),
                    iconRes = R.drawable.ic_book_page,
                    onClick = {
                        editingRule = null
                        isEditorVisible = true
                    },
                )
            }
            item {
                SettingsActionPreference(
                    title = stringResource(R.string.replace_rule_enable_all),
                    summary = stringResource(R.string.replace_rule_enable_all_summary),
                    iconRes = R.drawable.ic_check,
                    enabled = rules.any { !it.isEnabled },
                    onClick = { onSetAllEnabled(true) },
                )
            }
            item {
                SettingsActionPreference(
                    title = stringResource(R.string.replace_rule_disable_all),
                    summary = stringResource(R.string.replace_rule_disable_all_summary),
                    iconRes = R.drawable.ic_disable,
                    enabled = rules.any(ReplaceRule::isEnabled),
                    onClick = { onSetAllEnabled(false) },
                )
            }
            item {
                SettingsActionPreference(
                    title = stringResource(R.string.replace_rule_import),
                    summary = stringResource(R.string.replace_rule_import_summary),
                    iconRes = R.drawable.ic_import,
                    onClick = onImport,
                )
            }
            item {
                SettingsActionPreference(
                    title = stringResource(R.string.replace_rule_export),
                    summary = stringResource(R.string.replace_rule_export_summary),
                    iconRes = R.drawable.ic_cloud_upload,
                    onClick = onExport,
                )
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.replace_rules)) {
            if (rules.isEmpty()) {
                item {
                    SettingsInfoPreference(
                        title = stringResource(R.string.replace_rule_empty),
                        summary = stringResource(R.string.replace_rule_empty_summary),
                    )
                }
            } else {
                val groups = rules.groupBy { rule ->
                    rule.group?.trim().orEmpty()
                }
                groups.forEach { (group, groupRules) ->
                    if (groups.size > 1 || group.isNotBlank()) {
                        item {
                            Text(
                                text = group.ifBlank { stringResource(R.string.replace_rule_group_default) },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    groupRules.forEach { rule ->
                        item {
                            SettingsSplitSwitchPreference(
                                title = rule.name.ifBlank { rule.pattern },
                                summary = ruleSummary(rule),
                                checked = rule.isEnabled,
                                iconRes = R.drawable.ic_reorder_handle,
                                onClick = {
                                    editingRule = rule
                                    isEditorVisible = true
                                },
                                onCheckedChange = { enabled -> onToggle(rule, enabled) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (isEditorVisible) {
        ReplaceRuleEditorDialog(
            initialRule = editingRule,
            onDismissRequest = { isEditorVisible = false },
            onSave = { rule ->
                onSave(rule)
                isEditorVisible = false
            },
            onDelete = editingRule?.let { rule ->
                {
                    onDelete(rule)
                    isEditorVisible = false
                }
            },
        )
    }
}

@Composable
private fun ReplaceRuleEditorDialog(
    initialRule: ReplaceRule?,
    onDismissRequest: () -> Unit,
    onSave: (ReplaceRule) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val defaultRule = remember(initialRule?.id) {
        initialRule ?: ReplaceRule(
            name = "",
            isRegex = true,
            scopeContent = true,
            order = 0,
        )
    }
    var name by remember(defaultRule.id) { mutableStateOf(defaultRule.name) }
    var group by remember(defaultRule.id) { mutableStateOf(defaultRule.group.orEmpty()) }
    var pattern by remember(defaultRule.id) { mutableStateOf(defaultRule.pattern) }
    var replacement by remember(defaultRule.id) { mutableStateOf(defaultRule.replacement) }
    var scopeFilter by remember(defaultRule.id) { mutableStateOf(defaultRule.scopeFilter.orEmpty()) }
    var excludeScope by remember(defaultRule.id) { mutableStateOf(defaultRule.excludeScope.orEmpty()) }
    var isRegex by remember(defaultRule.id) { mutableStateOf(defaultRule.isRegex) }
    var scopeTitle by remember(defaultRule.id) { mutableStateOf(defaultRule.scopeTitle) }
    var scopeContent by remember(defaultRule.id) { mutableStateOf(defaultRule.scopeContent) }
    var order by remember(defaultRule.id) { mutableStateOf(defaultRule.order.toString()) }
    var timeout by remember(defaultRule.id) { mutableStateOf(defaultRule.timeoutMillisecond.toString()) }
    var previewInput by remember(defaultRule.id) { mutableStateOf("") }

    val candidate = defaultRule.copy(
        name = name,
        group = group.ifBlank { null },
        pattern = pattern,
        replacement = replacement,
        scopeFilter = scopeFilter.ifBlank { null },
        excludeScope = excludeScope.ifBlank { null },
        isRegex = isRegex,
        scopeTitle = scopeTitle,
        scopeContent = scopeContent,
        order = order.toIntOrNull() ?: 0,
        timeoutMillisecond = timeout.toLongOrNull()?.coerceAtLeast(0L) ?: defaultRule.timeoutMillisecond,
    )
    val isValid = candidate.isValid() && (scopeTitle || scopeContent)

    SettingsAlertDialog(
        title = stringResource(
            if (initialRule == null) R.string.replace_rule_add else R.string.replace_rule_edit,
        ),
        onDismissRequest = onDismissRequest,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RuleTextField(
                    value = name,
                    label = stringResource(R.string.replace_rule_name),
                    onValueChange = { name = it },
                )
                RuleTextField(
                    value = group,
                    label = stringResource(R.string.replace_rule_group),
                    onValueChange = { group = it },
                )
                RuleTextField(
                    value = pattern,
                    label = stringResource(R.string.replace_rule_pattern),
                    onValueChange = { pattern = it },
                    isError = pattern.isNotBlank() && !candidate.isValid(),
                )
                RuleTextField(
                    value = replacement,
                    label = stringResource(R.string.replace_rule_replacement),
                    onValueChange = { replacement = it },
                )
                RuleTextField(
                    value = scopeFilter,
                    label = stringResource(R.string.replace_rule_scope),
                    onValueChange = { scopeFilter = it },
                    supportingText = stringResource(R.string.replace_rule_scope_hint),
                )
                RuleTextField(
                    value = excludeScope,
                    label = stringResource(R.string.replace_rule_exclude_scope),
                    onValueChange = { excludeScope = it },
                    supportingText = stringResource(R.string.replace_rule_scope_hint),
                )
                RuleTextField(
                    value = order,
                    label = stringResource(R.string.replace_rule_order),
                    onValueChange = { order = it.filter(Char::isDigit).take(8) },
                )
                RuleTextField(
                    value = timeout,
                    label = stringResource(R.string.replace_rule_timeout),
                    onValueChange = { timeout = it.filter(Char::isDigit).take(8) },
                )
                RuleCheckRow(
                    checked = isRegex,
                    text = stringResource(R.string.replace_rule_regex),
                    onCheckedChange = { isRegex = it },
                )
                RuleCheckRow(
                    checked = scopeTitle,
                    text = stringResource(R.string.replace_rule_scope_title),
                    onCheckedChange = { scopeTitle = it },
                )
                RuleCheckRow(
                    checked = scopeContent,
                    text = stringResource(R.string.replace_rule_scope_content),
                    onCheckedChange = { scopeContent = it },
                )
                if (pattern.isNotBlank() && !candidate.isValid()) {
                    Text(
                        text = stringResource(R.string.replace_rule_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (pattern.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.replace_rule_preview),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    RuleTextField(
                        value = previewInput,
                        label = stringResource(R.string.replace_rule_preview_input),
                        onValueChange = { previewInput = it },
                        supportingText = stringResource(R.string.replace_rule_preview_hint),
                        minLines = 2,
                        maxLines = 5,
                    )
                    if (previewInput.isNotEmpty()) {
                        RuleTextField(
                            value = candidate.apply(previewInput),
                            label = stringResource(R.string.replace_rule_preview_output),
                            onValueChange = {},
                            readOnly = true,
                            minLines = 2,
                            maxLines = 5,
                        )
                    }
                }
            }
        },
        confirmButton = {
            SettingsDialogActionButton(
                text = stringResource(R.string.save),
                enabled = isValid,
                onClick = { onSave(candidate) },
            )
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    SettingsDialogActionButton(
                        text = stringResource(R.string.delete),
                        onClick = onDelete,
                    )
                }
                SettingsDialogActionButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismissRequest,
                )
            }
        },
    )
}

@Composable
private fun RuleTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { text -> { Text(text) } },
        isError = isError,
        readOnly = readOnly,
        minLines = minLines,
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RuleCheckRow(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun ruleSummary(rule: ReplaceRule): String {
    val mode = if (rule.isRegex) "regex" else "text"
    val scope = when {
        rule.scopeTitle && rule.scopeContent -> "title/content"
        rule.scopeTitle -> "title"
        else -> "content"
    }
    return "$mode · $scope · ${rule.pattern} → ${rule.replacement}"
}
