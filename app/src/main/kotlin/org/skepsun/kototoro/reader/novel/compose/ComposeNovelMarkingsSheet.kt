package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import org.skepsun.kototoro.bookmarks.domain.Bookmark
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingColor
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class NovelMarkingsTab(val label: String) {
    ALL("全部"),
    HIGHLIGHTS("划线"),
    THOUGHTS("想法"),
    BOOKMARKS("书签"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeNovelMarkingsSheet(
    bookmarks: List<Bookmark> = emptyList(),
    markings: List<NovelMarkingEntity>,
    chapters: List<ContentChapter> = emptyList(),
    onDismiss: () -> Unit,
    onEditNote: (NovelMarkingEntity) -> Unit,
    onDelete: (NovelMarkingEntity) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit = {},
    onOpenBookmark: (Bookmark) -> Unit = {},
    onJumpToMarking: (NovelMarkingEntity) -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.92f),
    ) {
        ComposeNovelNotesContent(
            bookmarks = bookmarks,
            markings = markings,
            chapters = chapters,
            onDismiss = onDismiss,
            onJumpToMarking = onJumpToMarking,
            onOpenBookmark = onOpenBookmark,
            onEditNote = onEditNote,
            onDelete = onDelete,
            onDeleteBookmark = onDeleteBookmark,
            showTitle = true,
        )
    }
}

@Composable
internal fun ComposeNovelNotesContent(
    bookmarks: List<Bookmark> = emptyList(),
    markings: List<NovelMarkingEntity> = emptyList(),
    chapters: List<ContentChapter> = emptyList(),
    onDismiss: () -> Unit = {},
    onJumpToMarking: (NovelMarkingEntity) -> Unit = {},
    onOpenBookmark: (Bookmark) -> Unit = {},
    onEditNote: (NovelMarkingEntity) -> Unit = {},
    onDelete: (NovelMarkingEntity) -> Unit = {},
    onDeleteBookmark: (Bookmark) -> Unit = {},
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    var selectedTab by remember { mutableStateOf(NovelMarkingsTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val thoughtCount = remember(markings) { markings.count { !it.note.isNullOrBlank() } }
    val highlightCount = remember(markings) { markings.size }
    val bookmarkCount = remember(bookmarks) { bookmarks.size }

    val filteredMarkings = remember(markings, selectedTab, searchQuery) {
        val base = when (selectedTab) {
            NovelMarkingsTab.ALL, NovelMarkingsTab.HIGHLIGHTS -> markings
            NovelMarkingsTab.THOUGHTS -> markings.filter { !it.note.isNullOrBlank() }
            NovelMarkingsTab.BOOKMARKS -> emptyList()
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                it.selectedText.contains(searchQuery, ignoreCase = true) ||
                    it.note?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    val filteredBookmarks = remember(bookmarks, selectedTab, searchQuery) {
        if (selectedTab == NovelMarkingsTab.HIGHLIGHTS || selectedTab == NovelMarkingsTab.THOUGHTS) {
            emptyList()
        } else if (searchQuery.isBlank()) {
            bookmarks
        } else {
            bookmarks.filter { it.imageUrl.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (showTitle) 0.dp else 8.dp),
    ) {
        if (showTitle) {
            // Title & Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.novel_markings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "共 $highlightCount 条划线 · $thoughtCount 条想法 · $bookmarkCount 个书签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        } else {
            Text(
                text = "共 $highlightCount 条划线 · $thoughtCount 条想法 · $bookmarkCount 个书签",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索划线或想法内容...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NovelMarkingsTab.entries.forEach { tab ->
                val count = when (tab) {
                    NovelMarkingsTab.ALL -> highlightCount + bookmarkCount
                    NovelMarkingsTab.HIGHLIGHTS -> highlightCount
                    NovelMarkingsTab.THOUGHTS -> thoughtCount
                    NovelMarkingsTab.BOOKMARKS -> bookmarkCount
                }
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text("${tab.label} ($count)") },
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }

        // List of items
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 680.dp),
        ) {
            if (filteredBookmarks.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.bookmarks),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(filteredBookmarks, key = { "bookmark:${it.pageId}" }) { bookmark ->
                    val chapterName = chapters.firstOrNull { it.id == bookmark.chapterId }?.title
                    ListItem(
                        modifier = Modifier.clickable(onClick = {
                            onDismiss()
                            onOpenBookmark(bookmark)
                        }),
                        headlineContent = {
                            Text(
                                text = bookmark.imageUrl.ifBlank {
                                    stringResource(R.string.bookmark_position, bookmark.page + 1)
                                },
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            val positionText = stringResource(R.string.bookmark_position, bookmark.page + 1)
                            Text(
                                text = if (!chapterName.isNullOrBlank()) "$chapterName · $positionText" else positionText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDeleteBookmark(bookmark) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }

            if (filteredMarkings.isNotEmpty()) {
                // Group markings by chapterIndex
                val grouped = filteredMarkings.groupBy { it.chapterIndex }
                grouped.forEach { (chapIdx, list) ->
                    item(key = "chap_header_$chapIdx") {
                        val chapterTitle = chapters.getOrNull(chapIdx)?.title
                        val headerText = if (!chapterTitle.isNullOrBlank()) {
                            "第 ${chapIdx + 1} 章 · $chapterTitle"
                        } else {
                            "第 ${chapIdx + 1} 章"
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                    }
                    items(list, key = { "marking_${it.id}" }) { marking ->
                        ListItem(
                            modifier = Modifier.clickable {
                                onDismiss()
                                onJumpToMarking(marking)
                            },
                            headlineContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    val markingColor = NovelMarkingColor.fromId(marking.color)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(markingColor.lineColor),
                                    )
                                    Text(
                                        text = marking.selectedText,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            },
                            supportingContent = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    if (!marking.note.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(modifier = Modifier.padding(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(20.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = marking.note,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                    val dateStr = remember(marking.updatedAt) {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(marking.updatedAt))
                                    }
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onEditNote(marking) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = stringResource(R.string.novel_marking_edit_note),
                                        )
                                    }
                                    IconButton(onClick = { onDelete(marking) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = stringResource(R.string.novel_marking_delete),
                                        )
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }

            if (filteredBookmarks.isEmpty() && filteredMarkings.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "未找到匹配内容" else stringResource(R.string.novel_markings_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated BottomSheet for writing or editing thoughts on selected text or an existing marking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NovelNoteEditorSheet(
    quoteText: String,
    initialNote: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var noteDraft by remember { mutableStateOf(initialNote) }
    var quoteExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { onSave(noteDraft.trim()) },
                        enabled = noteDraft.trim().isNotBlank() || initialNote.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }

            // Quote Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { quoteExpanded = !quoteExpanded },
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quoteText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (quoteExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Note Input Field
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                placeholder = { Text(stringResource(R.string.novel_selection_note_hint)) },
                minLines = 5,
                maxLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Detailed sheet for viewing a full thought and jump to origin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NovelNoteDetailSheet(
    marking: NovelMarkingEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onExcerpt: () -> Unit,
    onJumpToText: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.75f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "想法详情",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            // Original Quote
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = marking.selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Thought text
            if (!marking.note.isNullOrBlank()) {
                Text(
                    text = marking.note,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Time & chapter
            val dateStr = remember(marking.updatedAt) {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(marking.updatedAt))
            }
            Text(
                text = "第 ${marking.chapterIndex + 1} 章 · 更新于 $dateStr",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            HorizontalDivider()

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onJumpToText()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("定位原文")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onExcerpt()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("做成书摘")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("编辑")
                }
            }

            TextButton(
                onClick = {
                    onDismiss()
                    onDelete()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

