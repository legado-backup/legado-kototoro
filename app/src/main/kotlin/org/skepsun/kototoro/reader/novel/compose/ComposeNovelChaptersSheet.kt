package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import org.skepsun.kototoro.reader.novel.NovelReaderThemePreset
import org.skepsun.kototoro.reader.novel.novelReaderPalette
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingEntity
import org.skepsun.kototoro.reader.ui.compose.ReaderAnchoredBottomSheet

internal sealed interface NovelChapterListItem {
    val key: String
    data class Header(val title: String, val occurrence: Int) : NovelChapterListItem {
        override val key = "header:$title:$occurrence"
    }
    data class Chapter(val chapter: ContentChapter, val originalIndex: Int) : NovelChapterListItem {
        override val key = "chapter:${chapter.id}:$originalIndex"
    }
}

@Immutable
internal data class NovelChapterSearchResult(
    val chapterIndex: Int,
    val chapterTitle: String,
    val excerpt: String,
    val matchStart: Int,
    /** Keyword occurrence ranges (indices into [excerpt]) used for highlighting. */
    val matchRanges: List<IntRange> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeNovelChaptersSheet(
    chapters: List<ContentChapter>,
    currentIndex: Int,
    searchDocuments: List<NovelComposeChapterContent> = emptyList(),
    markings: List<NovelMarkingEntity> = emptyList(),
    bookmarks: List<Bookmark> = emptyList(),
    initialTab: NovelChaptersSheetTab = NovelChaptersSheetTab.CHAPTERS,
    themePreset: NovelReaderThemePreset = NovelReaderThemePreset.PAPER,
    onDismiss: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    onSearchResultSelected: (NovelMarkingTarget) -> Unit = { target -> onChapterSelected(target.chapterIndex) },
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

    val palette = novelReaderPalette(themePreset, isSystemInDarkTheme())
    val sheetColor = Color(palette.backgroundColor)
    val sheetContentColor = Color(palette.textColor)

    ReaderAnchoredBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetColor,
        contentColor = sheetContentColor,
        scrimColor = Color.Black.copy(alpha = if (palette.isDark) 0.58f else 0.42f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(palette.secondaryTextColor))
        },
    ) { sheetDragModifier ->
        val readerColors = MaterialTheme.colorScheme.copy(
            primary = Color(palette.chromeTextColor),
            onPrimary = sheetColor,
            primaryContainer = Color(palette.placeholderColor),
            onPrimaryContainer = Color(palette.textColor),
            secondaryContainer = Color(palette.highlightColor),
            onSecondaryContainer = Color(palette.textColor),
            background = sheetColor,
            surface = sheetColor,
            surfaceVariant = Color(palette.placeholderColor),
            onSurface = sheetContentColor,
            onSurfaceVariant = Color(palette.secondaryTextColor),
        )
        MaterialTheme(colorScheme = readerColors) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val contentWidth = if (maxWidth >= 720.dp) 680.dp else maxWidth
                Column(
                    modifier = Modifier
                        .widthIn(max = contentWidth)
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .align(Alignment.TopCenter),
                ) {
                    NovelChaptersTabRow(
                        currentPage = pagerState.currentPage,
                        chaptersCount = chapters.size,
                        notesCount = markings.size + bookmarks.size,
                        onPageSelected = { index ->
                            coroutineScope.launch { pagerState.scrollToPage(index) }
                        },
                        modifier = sheetDragModifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )

                    HorizontalPager(
                        state = pagerState,
                        overscrollEffect = null,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) { pageIndex ->
                        when (NovelChaptersSheetTab.entries[pageIndex]) {
                            NovelChaptersSheetTab.CHAPTERS -> {
                                ComposeNovelChaptersContent(
                                    chapters = chapters,
                                    currentIndex = currentIndex,
                                    onChapterSelected = onChapterSelected,
                                    dragModifier = sheetDragModifier,
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
                                    dragModifier = sheetDragModifier,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            NovelChaptersSheetTab.SEARCH -> {
                                ComposeNovelChapterSearchContent(
                                    chapters = chapters,
                                    documents = searchDocuments,
                                    onChapterSelected = onChapterSelected,
                                    onSearchResultSelected = onSearchResultSelected,
                                    dragModifier = sheetDragModifier,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelChaptersTabRow(
    currentPage: Int,
    chaptersCount: Int,
    notesCount: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            NovelChaptersSheetTab.entries.forEachIndexed { index, tab ->
                val selected = currentPage == index
                val label = when (tab) {
                    NovelChaptersSheetTab.CHAPTERS -> "${stringResource(R.string.chapters)}  $chaptersCount"
                    NovelChaptersSheetTab.NOTES -> "${stringResource(R.string.notes)}  $notesCount"
                    NovelChaptersSheetTab.SEARCH -> stringResource(R.string.novel_reader_chapter_search_tab)
                }
                val iconRes = when (tab) {
                    NovelChaptersSheetTab.CHAPTERS -> R.drawable.ic_list
                    NovelChaptersSheetTab.NOTES -> R.drawable.ic_bookmark
                    NovelChaptersSheetTab.SEARCH -> R.drawable.ic_search
                }
                Surface(
                    onClick = { onPageSelected(index) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var reversed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val items = remember(chapters, reversed, query, context) {
        buildChapterItems(chapters, reversed, query) { context.getString(R.string.volume_, it) }
    }
    val currentPosition = chapterListPositionForCurrent(items, currentIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPosition.coerceAtLeast(0))
    var locateRequest by remember { mutableStateOf(0) }
    LaunchedEffect(reversed, query, locateRequest) {
        if (query.isBlank() && currentPosition >= 0 && items.isNotEmpty()) {
            listState.scrollToItem(currentPosition)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = dragModifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    stringResource(R.string.novel_chapters_count, chapters.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        if (reversed) {
                            R.string.novel_chapters_order_descending
                        } else {
                            R.string.novel_chapters_order_ascending
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                )
            }
            Surface(
                onClick = { reversed = !reversed },
                shape = RoundedCornerShape(12.dp),
                color = if (reversed) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                contentColor = if (reversed) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort_desc),
                        contentDescription = stringResource(R.string.reverse_order),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(
                            if (reversed) {
                                R.string.novel_chapters_order_descending
                            } else {
                                R.string.novel_chapters_order_ascending
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        NovelReaderSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.search_chapters),
            clearContentDescription = stringResource(R.string.clear),
            modifier = dragModifier.fillMaxWidth(),
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
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
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_current_chapter),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
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
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onChapterSelected(item.originalIndex) },
                        )
                    }
                }
            }
        }
        FilledTonalButton(
            onClick = {
                if (chapters.isNotEmpty()) {
                    query = ""
                    locateRequest++
                }
            },
            enabled = chapters.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_current_chapter),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.novel_chapters_locate_current),
                modifier = Modifier.padding(start = 8.dp),
            )
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
    val currentPosition = chapterListPositionForCurrent(items, currentIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPosition.coerceAtLeast(0))
    LaunchedEffect(reversed, query) {
        if (query.isBlank() && currentPosition >= 0 && items.isNotEmpty()) {
            listState.scrollToItem(currentPosition)
        }
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
        NovelReaderSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.search_chapters),
            clearContentDescription = stringResource(R.string.clear),
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

@Composable
internal fun ComposeNovelChapterSearchContent(
    chapters: List<ContentChapter>,
    documents: List<NovelComposeChapterContent>,
    onChapterSelected: (Int) -> Unit,
    onSearchResultSelected: (NovelMarkingTarget) -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(chapters, documents, query) {
        searchNovelChapterContent(chapters, documents, query)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        NovelReaderSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.novel_reader_content_search_hint),
            clearContentDescription = stringResource(R.string.clear),
            modifier = dragModifier.fillMaxWidth(),
        )

        when {
            query.isBlank() -> {
                NovelChapterSearchEmptyState(
                    title = stringResource(R.string.novel_reader_content_search_empty_title),
                    message = stringResource(R.string.novel_reader_content_search_empty_hint),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            results.isEmpty() -> {
                NovelChapterSearchEmptyState(
                    title = stringResource(R.string.novel_reader_content_search_no_results),
                    message = stringResource(R.string.novel_reader_content_search_empty_hint),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            else -> {
                Text(
                    text = stringResource(R.string.novel_reader_content_search_result_count, results.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragModifier.padding(horizontal = 4.dp),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(
                        items = results,
                        key = { result -> "search:${result.chapterIndex}:${result.matchStart}" },
                    ) { result ->
                        NovelChapterSearchResultCard(
                            result = result,
                            query = query,
                            onClick = {
                                val needle = query.trim()
                                val chapter = chapters.getOrNull(result.chapterIndex)
                                if (chapter != null && needle.isNotEmpty()) {
                                    onSearchResultSelected(
                                        NovelMarkingTarget(
                                            markingId = 0L,
                                            chapterId = chapter.id,
                                            chapterIndex = result.chapterIndex,
                                            startOffset = result.matchStart,
                                            endOffset = result.matchStart + needle.length,
                                            selectedText = needle,
                                        ),
                                    )
                                } else {
                                    onChapterSelected(result.chapterIndex)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelChapterSearchResultCard(
    result: NovelChapterSearchResult,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.onSurface,
        background = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        fontWeight = FontWeight.SemiBold,
    )
    val needle = remember(query) { normalizeNovelSearchQuery(query) }
    val titleText = result.chapterTitle.ifBlank {
        stringResource(R.string.novel_reader_content_search_chapter_fallback, result.chapterIndex + 1)
    }
    val titleAnnotated = remember(titleText, needle, highlightStyle) {
        buildSearchHighlightedText(titleText, findSearchMatchRanges(titleText, needle), highlightStyle)
    }
    val excerptAnnotated = remember(result.excerpt, result.matchRanges, highlightStyle) {
        buildSearchHighlightedText(result.excerpt, result.matchRanges, highlightStyle)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_list),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = titleAnnotated,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = (result.chapterIndex + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = excerptAnnotated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NovelChapterSearchEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(horizontal = 28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun NovelReaderSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearContentDescription: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f),
        ),
        modifier = modifier.heightIn(min = 46.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .padding(start = 12.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = clearContentDescription,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

internal fun chapterListPositionForCurrent(
    items: List<NovelChapterListItem>,
    currentIndex: Int,
): Int = items.indexOfFirst {
    it is NovelChapterListItem.Chapter && it.originalIndex == currentIndex
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

internal fun searchNovelChapterContent(
    chapters: List<ContentChapter>,
    documents: List<NovelComposeChapterContent>,
    query: String,
    maxResults: Int = 80,
    maxResultsPerChapter: Int = 4,
): List<NovelChapterSearchResult> {
    val needle = query.trim()
    if (needle.isEmpty() || maxResults <= 0 || maxResultsPerChapter <= 0) return emptyList()
    // Excerpts collapse whitespace runs, so scan the display form of the query for highlight ranges.
    val displayNeedle = needle.replace(novelSearchWhitespaceRegex, " ")
    val results = mutableListOf<NovelChapterSearchResult>()
    documents
        .asSequence()
        .filter { it.content.isNotBlank() }
        .distinctBy { it.chapterIndex }
        .sortedBy { it.chapterIndex }
        .forEach { document ->
            if (results.size >= maxResults) return@forEach
            val title = chapters
                .getOrNull(document.chapterIndex)
                ?.title
                ?.takeIf(String::isNotBlank)
                ?: document.chapterTitle
            var matchStart = document.content.indexOf(needle, ignoreCase = true)
            var chapterMatchCount = 0
            while (matchStart >= 0 && chapterMatchCount < maxResultsPerChapter && results.size < maxResults) {
                val matchEnd = matchStart + needle.length
                val excerptStart = (matchStart - 48).coerceAtLeast(0)
                val excerptEnd = (matchEnd + 108).coerceAtMost(document.content.length)
                val excerptBody = document.content
                    .substring(excerptStart, excerptEnd)
                    .replace(novelSearchWhitespaceRegex, " ")
                    .trim()
                val excerpt = buildString {
                    if (excerptStart > 0) append('…')
                    append(excerptBody)
                    if (excerptEnd < document.content.length) append('…')
                }
                results += NovelChapterSearchResult(
                    chapterIndex = document.chapterIndex,
                    chapterTitle = title,
                    excerpt = excerpt,
                    matchStart = matchStart,
                    matchRanges = findSearchMatchRanges(excerpt, displayNeedle),
                )
                chapterMatchCount++
                matchStart = document.content.indexOf(needle, startIndex = matchEnd, ignoreCase = true)
            }
        }
    return results
}

internal fun normalizeNovelSearchQuery(query: String): String =
    query.trim().replace(novelSearchWhitespaceRegex, " ")

internal fun findSearchMatchRanges(text: String, needle: String): List<IntRange> {
    if (needle.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var index = text.indexOf(needle, ignoreCase = true)
    while (index >= 0) {
        ranges += index until index + needle.length
        index = text.indexOf(needle, startIndex = index + needle.length, ignoreCase = true)
    }
    return ranges
}

internal fun buildSearchHighlightedText(
    text: String,
    ranges: List<IntRange>,
    style: SpanStyle,
): AnnotatedString = buildAnnotatedString {
    append(text)
    ranges.forEach { range ->
        val start = range.first.coerceIn(0, text.length)
        val end = (range.last + 1).coerceIn(0, text.length)
        if (end > start) {
            addStyle(style, start, end)
        }
    }
}

private val novelSearchWhitespaceRegex = Regex("\\s+")
