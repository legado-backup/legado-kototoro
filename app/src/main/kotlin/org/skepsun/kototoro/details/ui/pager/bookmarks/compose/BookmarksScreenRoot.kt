package org.skepsun.kototoro.details.ui.pager.bookmarks.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.skepsun.kototoro.R
import org.skepsun.kototoro.bookmarks.domain.Bookmark
import org.skepsun.kototoro.core.model.getContentType
import org.skepsun.kototoro.core.nav.AppRouter
import org.skepsun.kototoro.core.nav.ReaderIntent
import org.skepsun.kototoro.core.util.ext.findActivity
import org.skepsun.kototoro.core.util.ext.toFileNameSafe
import org.skepsun.kototoro.details.ui.DetailsViewModel
import org.skepsun.kototoro.details.ui.compose.state.DetailsPaneState
import org.skepsun.kototoro.details.ui.compose.state.rememberDetailsPaneNestedScrollConnection
import org.skepsun.kototoro.details.ui.pager.ChaptersPagesViewModel
import org.skepsun.kototoro.details.ui.pager.bookmarks.BookmarksViewModel
import org.skepsun.kototoro.notes.domain.BookNoteItem
import org.skepsun.kototoro.notes.domain.NoteType
import org.skepsun.kototoro.notes.ui.BookNotesExportHelper
import org.skepsun.kototoro.notes.ui.BookNoteCard
import org.skepsun.kototoro.notes.ui.BookNotesExportBottomSheet
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentType
import org.skepsun.kototoro.reader.novel.compose.NovelExcerptData
import org.skepsun.kototoro.reader.novel.compose.NovelExcerptSheet
import org.skepsun.kototoro.reader.ui.ReaderNavigationCallback
import org.skepsun.kototoro.reader.ui.ReaderState

@Composable
fun BookmarksScreenRoot(
    activityViewModel: ChaptersPagesViewModel,
    router: AppRouter,
    viewModel: BookmarksViewModel,
    detailsPaneState: DetailsPaneState? = null,
) {
    val context = LocalContext.current
    val mangaDetails by activityViewModel.mangaDetails.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(mangaDetails) {
        viewModel.emit(mangaDetails)
    }

    val content = mangaDetails?.toContent()
    val contentType = content?.source?.getContentType()
    val isNovel = contentType == ContentType.NOVEL || contentType == ContentType.HENTAI_NOVEL

    if (isNovel && content != null) {
        val bookNotes by viewModel.bookNotes.collectAsStateWithLifecycle(initialValue = emptyList())
        NovelNotesTabContent(
            manga = content,
            notes = bookNotes,
            router = router,
            detailsPaneState = detailsPaneState,
            onDeleteNote = { note -> viewModel.deleteBookNote(note) },
            activityViewModel = activityViewModel,
        )
    } else {
        val contentItems by viewModel.content.collectAsStateWithLifecycle(initialValue = emptyList())
        val gridScale by viewModel.gridScale.collectAsStateWithLifecycle(initialValue = 1f)
        val selectedItemIds = remember { mutableStateListOf<Long>() }
        val selectedIds = remember(selectedItemIds.toList()) {
            selectedItemIds.toSet()
        }

        BookmarksScreen(
            items = contentItems,
            gridMinSize = (120.dp / gridScale.coerceIn(0.5f, 1.5f)),
            selectedItemIds = selectedIds,
            detailsPaneState = detailsPaneState,
            onItemClick = { item ->
                val bookmark = item as Bookmark
                if (selectedItemIds.isNotEmpty()) {
                    if (selectedItemIds.contains(bookmark.pageId)) {
                        selectedItemIds.remove(bookmark.pageId)
                    } else {
                        selectedItemIds.add(bookmark.pageId)
                    }
                } else {
                    val navigationCallback = (context as? ReaderNavigationCallback)
                        ?: (context.findActivity() as? ReaderNavigationCallback)
                    if (navigationCallback?.onBookmarkSelected(bookmark) == true) {
                        return@BookmarksScreen
                    }
                    val targetState = ReaderState(bookmark.chapterId, bookmark.page, bookmark.scroll)
                    (activityViewModel as? DetailsViewModel)?.recordDetailsJump(targetState, "detail_bookmark")
                    router.openReader(
                        ReaderIntent.Builder(context)
                            .manga(bookmark.manga)
                            .state(targetState)
                            .build(),
                    )
                }
            },
            onItemLongClick = { item ->
                val bookmark = item as Bookmark
                if (selectedItemIds.contains(bookmark.pageId)) {
                    selectedItemIds.remove(bookmark.pageId)
                } else {
                    selectedItemIds.add(bookmark.pageId)
                }
            },
            onSelectionActionClick = { actionId ->
                if (actionId == R.id.action_delete) {
                    viewModel.removeBookmarks(selectedIds)
                }
                selectedItemIds.clear()
            },
            onClearSelection = { selectedItemIds.clear() },
        )
    }
}

@Composable
private fun NovelNotesTabContent(
    manga: Content,
    notes: List<BookNoteItem>,
    router: AppRouter,
    detailsPaneState: DetailsPaneState?,
    onDeleteNote: (BookNoteItem) -> Unit,
    activityViewModel: ChaptersPagesViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(NoteType.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var activeExcerptData by remember { mutableStateOf<NovelExcerptData?>(null) }
    var exportSheetVisible by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        if (uri != null) {
            BookNotesExportHelper.writeMarkdownToUri(context, uri, manga, notes)
        }
    }

    val highlightCount = remember(notes) { notes.count { it is BookNoteItem.NovelHighlight && it.note.isNullOrBlank() } }
    val thoughtCount = remember(notes) { notes.count { it is BookNoteItem.NovelHighlight && !it.note.isNullOrBlank() } }
    val bookmarkCount = remember(notes) { notes.count { it is BookNoteItem.BookmarkEntry } }

    val filteredNotes = remember(notes, selectedFilter, searchQuery) {
        val base = when (selectedFilter) {
            NoteType.ALL -> notes
            NoteType.HIGHLIGHT -> notes.filter { it is BookNoteItem.NovelHighlight && it.note.isNullOrBlank() }
            NoteType.THOUGHT -> notes.filter { it is BookNoteItem.NovelHighlight && !it.note.isNullOrBlank() }
            NoteType.BOOKMARK -> notes.filterIsInstance<BookNoteItem.BookmarkEntry>()
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter { item ->
                when (item) {
                    is BookNoteItem.NovelHighlight -> {
                        item.text.contains(searchQuery, ignoreCase = true) ||
                            item.note?.contains(searchQuery, ignoreCase = true) == true ||
                            item.chapterTitle.contains(searchQuery, ignoreCase = true)
                    }
                    is BookNoteItem.BookmarkEntry -> {
                        item.chapterTitle.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
        }
    }
    val groupedNotes = remember(filteredNotes) {
        filteredNotes.groupBy { it.chapterTitle }
    }

    val listState = rememberLazyListState()
    val paneNestedScrollConnection = rememberDetailsPaneNestedScrollConnection(
        state = detailsPaneState,
        canChildScrollBackward = { listState.canScrollBackward },
    )
    val paneNestedScrollModifier = remember(paneNestedScrollConnection) {
        if (paneNestedScrollConnection != null) {
            Modifier.nestedScroll(paneNestedScrollConnection)
        } else {
            Modifier
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter bar with chips and search / export actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedFilter == NoteType.ALL,
                    onClick = { selectedFilter = NoteType.ALL },
                    label = { Text("全部 (${notes.size})", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedFilter == NoteType.HIGHLIGHT,
                    onClick = { selectedFilter = NoteType.HIGHLIGHT },
                    label = { Text("划线 ($highlightCount)", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedFilter == NoteType.THOUGHT,
                    onClick = { selectedFilter = NoteType.THOUGHT },
                    label = { Text("想法 ($thoughtCount)", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = selectedFilter == NoteType.BOOKMARK,
                    onClick = { selectedFilter = NoteType.BOOKMARK },
                    label = { Text("书签 ($bookmarkCount)", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }

            IconButton(
                onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) searchQuery = ""
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    modifier = Modifier.size(18.dp),
                    tint = if (searchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (notes.isNotEmpty()) {
                IconButton(
                    onClick = { exportSheetVisible = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "导出笔记",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("搜索划线或想法内容...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(16.dp))
                        }
                    }
                },
            )
        }

        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bookmark),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (notes.isEmpty()) "暂无笔记与书签" else "未找到匹配的笔记",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (notes.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "阅读小说时长按划线、发表想法或添加书签，即可在此处查看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(paneNestedScrollModifier),
                contentPadding = PaddingValues(bottom = 32.dp, top = 4.dp),
            ) {
                groupedNotes.forEach { (chapterTitle, itemsInChapter) ->
                    item(key = "header_$chapterTitle") {
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }

                    items(itemsInChapter.size, key = { index -> itemsInChapter[index].id }) { index ->
                        val item = itemsInChapter[index]
                        BookNoteCard(
                            item = item,
                            manga = manga,
                            onClick = {
                                when (item) {
                                    is BookNoteItem.NovelHighlight -> {
                                        val targetState = ReaderState(
                                            chapterId = item.chapterId,
                                            page = 0,
                                            scroll = 0,
                                        )
                                        (activityViewModel as? DetailsViewModel)?.recordDetailsJump(targetState, "detail_note")
                                        router.openReader(
                                            manga = manga,
                                            state = targetState,
                                            markingId = item.id,
                                            startOffset = item.startOffset,
                                            endOffset = item.endOffset,
                                            selectedText = item.text,
                                        )
                                    }
                                    is BookNoteItem.BookmarkEntry -> {
                                        val targetState = ReaderState(
                                            chapterId = item.chapterId,
                                            page = item.page,
                                            scroll = 0,
                                        )
                                        (activityViewModel as? DetailsViewModel)?.recordDetailsJump(targetState, "detail_bookmark")
                                        router.openReader(
                                            manga = manga,
                                            state = targetState,
                                        )
                                    }
                                }
                            },
                            onMakeExcerpt = {
                                if (item is BookNoteItem.NovelHighlight) {
                                    activeExcerptData = NovelExcerptData(
                                        selectedText = item.text,
                                        bookTitle = manga.title,
                                        chapterTitle = item.chapterTitle,
                                        author = manga.authors.firstOrNull().orEmpty(),
                                    )
                                }
                            },
                            onCopy = {
                                val copyText = when (item) {
                                    is BookNoteItem.NovelHighlight -> item.note?.let { "$it\n引用：${item.text}" } ?: item.text
                                    is BookNoteItem.BookmarkEntry -> "${item.chapterTitle} (第${item.page + 1}页)"
                                }
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("note", copyText))
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                onDeleteNote(item)
                            },
                        )
                    }
                }
            }
        }
    }

    activeExcerptData?.let { data ->
        NovelExcerptSheet(
            data = data,
            onDismiss = { activeExcerptData = null },
        )
    }

    if (exportSheetVisible) {
        BookNotesExportBottomSheet(
            manga = manga,
            notes = notes,
            onDismiss = { exportSheetVisible = false },
            onCopyMarkdown = {
                BookNotesExportHelper.copyToClipboard(context, manga, notes)
            },
            onShareMarkdown = {
                BookNotesExportHelper.shareMarkdownFile(context, manga, notes)
            },
            onSaveMarkdown = {
                val safeTitle = manga.title.toFileNameSafe().ifBlank { "book" }.take(50)
                createDocumentLauncher.launch("《${safeTitle}》读书笔记.md")
            },
        )
    }
}

