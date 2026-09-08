package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.skepsun.kototoro.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun NovelTextSelectionActions(
    selection: NovelTextSelection,
    onAction: (NovelTextSelection, NovelTextSelectionAction) -> Unit,
    modifier: Modifier = Modifier,
    isMarking: Boolean = false,
    note: String? = null,
    updatedAt: Long? = null,
    onExpandNote: (() -> Unit)? = null,
    isAbove: Boolean = true,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.widthIn(max = 440.dp),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val actionButtons = @Composable {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val primaryActions = novelPrimaryActions(isMarking)
                    primaryActions.forEach { action ->
                        SelectionActionButton(
                            selection = selection,
                            action = action,
                            isMarking = isMarking,
                            hasNote = !note.isNullOrBlank(),
                            onAction = onAction,
                        )
                    }

                    // More overflow button
                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.options),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false },
                        ) {
                            novelOverflowActions().forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(actionLabel(action)) },
                                    onClick = {
                                        moreMenuExpanded = false
                                        onAction(selection, action)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            val thoughtCard = @Composable {
                if (!note.isNullOrBlank()) {
                    NovelThoughtCard(
                        note = note,
                        updatedAt = updatedAt,
                        onEdit = { onAction(selection, NovelTextSelectionAction.NOTE) },
                        onExpand = onExpandNote,
                    )
                }
            }

            if (isAbove) {
                actionButtons()
                thoughtCard()
            } else {
                thoughtCard()
                actionButtons()
            }
        }
    }
}

@Composable
private fun NovelThoughtCard(
    note: String,
    updatedAt: Long?,
    onEdit: () -> Unit,
    onExpand: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(3.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "我的想法",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(22.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.novel_marking_edit_note),
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
                if (note.length > 50 || note.lines().size > 2) {
                    Text(
                        text = "展开完整想法",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onExpand?.invoke() ?: onEdit() }
                            .padding(vertical = 2.dp),
                    )
                }
                if (updatedAt != null && updatedAt > 0L) {
                    val dateString = remember(updatedAt) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(updatedAt))
                    }
                    Text(
                        text = "更新于 $dateString",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

internal fun novelPrimaryActions(isMarking: Boolean): List<NovelTextSelectionAction> = buildList {
    if (!isMarking) {
        add(NovelTextSelectionAction.HIGHLIGHT)
    }
    add(NovelTextSelectionAction.COPY)
    add(NovelTextSelectionAction.NOTE)
    add(NovelTextSelectionAction.EXCERPT)
    add(NovelTextSelectionAction.ASK_AI)
    add(NovelTextSelectionAction.LISTEN)
    if (isMarking) {
        add(NovelTextSelectionAction.DELETE)
    }
}

internal fun novelOverflowActions(): List<NovelTextSelectionAction> = listOf(
    NovelTextSelectionAction.DICTIONARY,
    NovelTextSelectionAction.SHARE,
)

internal fun novelSelectionActions(isMarking: Boolean): List<NovelTextSelectionAction> =
    novelPrimaryActions(isMarking) + novelOverflowActions()

@Composable
private fun actionLabel(
    action: NovelTextSelectionAction,
    isMarking: Boolean = false,
    hasNote: Boolean = false,
): String = when (action) {
    NovelTextSelectionAction.COPY -> stringResource(R.string.copy)
    NovelTextSelectionAction.NOTE -> if (hasNote) stringResource(R.string.novel_marking_edit_note) else stringResource(R.string.novel_selection_write_idea)
    NovelTextSelectionAction.EXCERPT -> stringResource(R.string.novel_selection_excerpt)
    NovelTextSelectionAction.ASK_AI -> stringResource(R.string.novel_selection_ask_ai)
    NovelTextSelectionAction.LISTEN -> stringResource(R.string.novel_selection_listen)
    NovelTextSelectionAction.SHARE -> stringResource(R.string.share)
    NovelTextSelectionAction.DICTIONARY -> stringResource(R.string.novel_selection_dictionary)
    NovelTextSelectionAction.HIGHLIGHT -> if (isMarking) "已划线" else stringResource(R.string.novel_selection_highlight)
    NovelTextSelectionAction.DELETE -> stringResource(R.string.delete)
    NovelTextSelectionAction.BOOKMARK -> error("Bookmark is intentionally not a selection toolbar action")
}

@Composable
private fun SelectionActionButton(
    selection: NovelTextSelection,
    action: NovelTextSelectionAction,
    isMarking: Boolean,
    hasNote: Boolean,
    onAction: (NovelTextSelection, NovelTextSelectionAction) -> Unit,
) {
    val label = actionLabel(action, isMarking, hasNote)
    val isMarkedBadge = isMarking && action == NovelTextSelectionAction.HIGHLIGHT
    TextButton(
        onClick = { onAction(selection, action) },
        modifier = Modifier.padding(horizontal = 1.dp),
    ) {
        if (isMarkedBadge) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(
                    text = label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
