package org.skepsun.kototoro.settings.replace

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.skepsun.kototoro.R
import org.skepsun.kototoro.settings.compose.ReplaceRulesSettingsScreen

@Composable
fun ReplaceRulesSettingsRoute(
    viewModel: ReplaceRuleSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var exportJson by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (raw != null) {
                withContext(Dispatchers.Main) { viewModel.import(raw) }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = exportJson ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            val message = when (event) {
                ReplaceRuleSettingsViewModel.Event.Saved -> R.string.replace_rule_saved
                ReplaceRuleSettingsViewModel.Event.Deleted -> R.string.replace_rule_deleted
                is ReplaceRuleSettingsViewModel.Event.Imported -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.replace_rule_imported, event.count),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@collectLatest
                }
                is ReplaceRuleSettingsViewModel.Event.BulkUpdated -> {
                    if (event.enabled) R.string.replace_rule_all_enabled else R.string.replace_rule_all_disabled
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    ReplaceRulesSettingsScreen(
        rules = rules,
        onToggle = viewModel::setEnabled,
        onSetAllEnabled = viewModel::setAllEnabled,
        onImport = {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onExport = {
            viewModel.export { json ->
                exportJson = json
                exportLauncher.launch("kototoro-replace-rules.json")
            }
        },
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}
