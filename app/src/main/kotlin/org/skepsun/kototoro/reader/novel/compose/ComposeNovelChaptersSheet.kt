package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.skepsun.kototoro.R
import org.skepsun.kototoro.bookmarks.domain.Bookmark
import org.skepsun.kototoro.core.model.LocalNovelSource
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingEntity

internal sealed interface NovelChapterListItem {
    val key: String
    data class Header(val title: String, val occurrence: Int) : NovelChapterListItem {
        override val key = "header:$title:$occurrence"
    }
    data class Chapter(val chapter: ContentChapter, val originalIndex: Int) : NovelChapterListItem {
        override val key = "chapter:${chapter.id}:$originalIndex"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeNovelChaptersSheet(
    chapters: List<ContentChapter>,
    currentIndex: Int,
    markings: List<NovelMarkingEntity> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    initialTab: NovelChaptersSheetTab = NovelChaptersSheetTab.CHAPTERS,
    onDismiss: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    onJumpToMarking: (NovelMarkingEntity) -> Unit = {},
    onOpenBookmark: (Bookmark) -> Unit = {},
    onEditMarkingNote: (NovelMarkingEntity) -> Unit = {},
    onDeleteMarking: (NovelMarkingEntity) -> Unit = {},
    onDeleteBookmark: (Bookmark) -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal.coerceIn(0, NovelChaptersSheetTab.entries.lastIndex),
        pageCount = { NovelChaptersSheetTab.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(initialTab) {
        val target = initialTab.ordinal.coerceIn(0, NovelChaptersSheetTab.entries.lastIndex)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.92f),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val contentWidth = if (maxWidth >= 720.dp) 680.dp else maxWidth
            Column(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 2.dp, end = 12.dp, bottom = 2.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.novel_reader_chapter_index),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = if (chapters.isEmpty()) {
                                stringResource(R.string.chapters_empty)
                            } else {
                                stringResource(
                                    R.string.novel_reader_chapter_position,
                                    currentIndex.coerceIn(0, chapters.lastIndex) + 1,
                                    chapters.size,
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        NovelChaptersSheetTab.entries.forEachIndexed { index, tab ->
                            val selected = pagerState.currentPage == index
                            val label = when (tab) {
                                NovelChaptersSheetTab.CHAPTERS -> "${stringResource(R.string.chapters)} (${chapters.size})"
                                NovelChaptersSheetTab.NOTES -> {
                                    val totalNotes = markings.size + bookmarks.size
                                    "${stringResource(R.string.notes)} ($totalNotes)"
                                }
                            }
                            val iconRes = when (tab) {
                                NovelChaptersSheetTab.CHAPTERS -> R.drawable.ic_list
                                NovelChaptersSheetTab.NOTES -> R.drawable.ic_bookmark
                            }
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) { pageIndex ->
                    when (NovelChaptersSheetTab.entries[pageIndex]) {
                        NovelChaptersSheetTab.CHAPTERS -> {
                            ComposeNovelChaptersContent(
                                chapters = chapters,
                                currentIndex = currentIndex,
                                onChapterSelected = onChapterSelected,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        NovelChaptersSheetTab.NOTES -> {
                            ComposeNovelNotesContent(
                                bookmarks = bookmarks,
                                markings = markings,
                                chapters = chapters,
                                onDismiss = onDismiss,
                                onJumpToMarking = onJumpToMarking,
                                onOpenBookmark = onOpenBookmark,
                                onEditNote = onEditMarkingNote,
                                onDelete = onDeleteMarking,
                                onDeleteBookmark = onDeleteBookmark,
                                showTitle = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ComposeNovelChaptersContent(
    chapters: List<ContentChapter>,
    currentIndex: Int,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var reversed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val items = remember(chapters, reversed, query, context) {
        buildChapterItems(chapters, reversed, query) { context.getString(R.string.volume_, it) }
    }
    val currentPosition = items.indexOfFirst {
        it is NovelChapterListItem.Chapter && it.originalIndex == currentIndex
    }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPosition)
    LaunchedEffect(reversed, query) {
        if (query.isBlank()) listState.scrollToItem(currentPosition)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.novel_chapters_count, chapters.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { reversed = !reversed }) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort_desc),
                    contentDescription = stringResource(R.string.reverse_order),
                    tint = if (reversed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_chapters)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(items, key = NovelChapterListItem::key) { item ->
                when (item) {
                    is NovelChapterListItem.Header -> {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    is NovelChapterListItem.Chapter -> {
                        val selected = item.originalIndex == currentIndex
                        ListItem(
                            headlineContent = {
                                Text(
                                    item.chapter.title ?: stringResource(R.string.unnamed_chapter),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            supportingContent = {
                                val subtitle = if (selected) {
                                    stringResource(R.string.novel_reader_current_chapter)
                                } else {
                                    item.chapter.branch
                                }
                                subtitle?.takeIf { it.isNotBlank() }?.let { branch ->
                                    Text(
                                        text = branch,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            leadingContent = if (selected) {
                                { Icon(painterResource(R.drawable.ic_current_chapter), null) }
                            } else {
                                null
                            },
                            trailingContent = {
                                Text(
                                    text = (item.originalIndex + 1).toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                },
                            ),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onChapterSelected(item.originalIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ComposeNovelChaptersPanel(
    chapters: List<ContentChapter>,
    currentIndex: Int,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var reversed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val items = remember(chapters, reversed, query, context) {
        buildChapterItems(chapters, reversed, query) { context.getString(R.string.volume_, it) }
    }
    val currentPosition = items.indexOfFirst {
        it is NovelChapterListItem.Chapter && it.originalIndex == currentIndex
    }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPosition)
    LaunchedEffect(reversed, query) {
        if (query.isBlank()) listState.scrollToItem(currentPosition)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.chapters), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.novel_chapters_count, chapters.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { reversed = !reversed }) {
                Icon(painterResource(R.drawable.ic_sort_desc), stringResource(R.string.reverse_order))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_chapters)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp)) {
            items(items, key = NovelChapterListItem::key) { item ->
                when (item) {
                    is NovelChapterListItem.Header -> Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    is NovelChapterListItem.Chapter -> {
                        val selected = item.originalIndex == currentIndex
                        ListItem(
                            headlineContent = {
                                Text(
                                    item.chapter.title ?: stringResource(R.string.unnamed_chapter),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            leadingContent = if (selected) {
                                { Icon(painterResource(R.drawable.ic_current_chapter), null) }
                            } else {
                                null
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            ),
                            modifier = Modifier.clickable { onChapterSelected(item.originalIndex) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

internal fun buildChapterItems(
    chapters: List<ContentChapter>,
    reversed: Boolean,
    query: String,
    volumeTitle: (Int) -> String = { "Volume $it" },
): List<NovelChapterListItem> {
    val indexed = chapters.withIndex().let { if (reversed) it.reversed() else it }
    val filtered = indexed.filter { (_, chapter) ->
        query.isBlank() || listOf(chapter.title, chapter.branch, chapter.scanlator)
            .any { it?.contains(query, ignoreCase = true) == true }
    }
    val result = mutableListOf<NovelChapterListItem>()
    var previousGroup: String? = null
    var previousVolume: Int? = null
    var headerOccurrence = 0
    filtered.forEach { (index, chapter) ->
        val group = chapter.branch?.takeIf(String::isNotBlank)
            ?: chapter.scanlator?.takeIf { it.isNotBlank() && chapter.source == LocalNovelSource }
            .orEmpty()
        val groupChanged = group != previousGroup
        if (groupChanged) {
            if (group.isNotEmpty()) {
                result += NovelChapterListItem.Header(group, headerOccurrence++)
            }
            previousVolume = null
        }
        val volume = chapter.volume.takeIf { it > 0 }
        if (volume != null && volume != previousVolume) {
            result += NovelChapterListItem.Header(volumeTitle(volume), headerOccurrence++)
        }
        result += NovelChapterListItem.Chapter(chapter, index)
        previousGroup = group
        previousVolume = volume
    }
    return result
}
