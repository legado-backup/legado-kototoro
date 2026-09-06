package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.skepsun.kototoro.R

@Composable
internal fun NovelTextSelectionActions(
    selection: NovelTextSelection,
    onAction: (NovelTextSelection, NovelTextSelectionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SelectionActionButton(selection, NovelTextSelectionAction.COPY, stringResource(R.string.copy), onAction)
            SelectionActionButton(selection, NovelTextSelectionAction.SHARE, stringResource(R.string.share), onAction)
            SelectionActionButton(
                selection,
                NovelTextSelectionAction.DICTIONARY,
                stringResource(R.string.novel_selection_dictionary),
                onAction,
            )
            SelectionActionButton(
                selection,
                NovelTextSelectionAction.HIGHLIGHT,
                stringResource(R.string.novel_selection_highlight),
                onAction,
            )
            SelectionActionButton(
                selection,
                NovelTextSelectionAction.NOTE,
                stringResource(R.string.novel_selection_note),
                onAction,
            )
        }
    }
}

@Composable
private fun RowScope.SelectionActionButton(
    selection: NovelTextSelection,
    action: NovelTextSelectionAction,
    label: String,
    onAction: (NovelTextSelection, NovelTextSelectionAction) -> Unit,
) {
    TextButton(
        onClick = { onAction(selection, action) },
        modifier = Modifier.weight(1f),
    ) {
        Text(label, maxLines = 1)
    }
}
