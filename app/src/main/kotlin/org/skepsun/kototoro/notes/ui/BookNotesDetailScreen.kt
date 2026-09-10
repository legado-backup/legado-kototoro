package org.skepsun.kototoro.notes.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.prefs.BackgroundStyle
import org.skepsun.kototoro.core.ui.theme.LocalBackgroundStyle
import org.skepsun.kototoro.core.ui.theme.artworkAwareContainerColor
import org.skepsun.kototoro.core.util.ext.toFileNameSafe
import org.skepsun.kototoro.main.ui.compose.GlassDropdownMenu
import org.skepsun.kototoro.notes.domain.BookNoteItem
import org.skepsun.kototoro.notes.domain.NoteType
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentPage
import org.skepsun.kototoro.reader.novel.compose.NovelExcerptData
import org.skepsun.kototoro.reader.novel.compose.NovelExcerptSheet
import org.skepsun.kototoro.reader.novel.annotation.NovelMarkingColor

@Composable
fun BookNotesDetailScreen(
    uiState: NotesUiState,
    onBack: () -> Unit,
    onFilterChange: (NoteType) -> Unit,
    onBookSearchQueryChange: (String) -> Unit,
    onJumpToReader: (Content, BookNoteItem) -> Unit,
    onDeleteNote: (BookNoteItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val manga = uiState.selectedManga
    val notes = uiState.selectedBookNotes
    val groupedNotes = uiState.notesGroupedByChapter

    var searchBarVisible by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var activeExcerptData by remember { mutableStateOf<NovelExcerptData?>(null) }
    var exportSheetVisible by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        if (uri != null && manga != null) {
            BookNotesExportHelper.writeMarkdownToUri(context, uri, manga, notes)
        }
    }

    val highlightCount = notes.count { it is BookNoteItem.NovelHighlight && it.note.isNullOrBlank() }
    val thoughtCount = notes.count { it is BookNoteItem.NovelHighlight && !it.note.isNullOrBlank() }
    val bookmarkCount = notes.count { it is BookNoteItem.BookmarkEntry }

    val subtitleText = buildList {
        if (highlightCount > 0) {
            add(stringResource(R.string.book_notes_count_highlights, highlightCount))
        }
        if (thoughtCount > 0) {
            add(stringResource(R.string.book_notes_count_thoughts, thoughtCount))
        }
        if (bookmarkCount > 0) {
            add(stringResource(R.string.book_notes_count_bookmarks, bookmarkCount))
        }
    }.joinToString(" · ").ifBlank { stringResource(R.string.book_notes_total_count, notes.size) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding),
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.notes),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (searchBarVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.bookSearchQuery,
                        onValueChange = onBookSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        placeholder = { Text(stringResource(R.string.book_notes_book_search_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                searchBarVisible = false
                                onBookSearchQueryChange("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Book Header Card
            item {
                manga?.let { book ->
                    BookDetailHeaderCard(
                        manga = book,
                        totalNotesCount = notes.size,
                        onExcerptClick = {
                            val firstHighlight = notes.filterIsInstance<BookNoteItem.NovelHighlight>().firstOrNull()
                            if (firstHighlight != null) {
                                activeExcerptData = NovelExcerptData(
                                    selectedText = firstHighlight.text,
                                    bookTitle = book.title,
                                    chapterTitle = firstHighlight.chapterTitle,
                                    author = book.authors.firstOrNull().orEmpty(),
                                )
                            } else {
                                Toast.makeText(context, R.string.book_notes_no_highlight_excerpt, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSearchClick = { searchBarVisible = !searchBarVisible },
                        onExportClick = {
                            exportSheetVisible = true
                        },
                        onReadClick = {
                            val firstNote = notes.firstOrNull()
                            if (firstNote != null) {
                                onJumpToReader(book, firstNote)
                            }
                        },
                    )
                }
            }

            // Filter row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (uiState.selectedFilter) {
                            NoteType.ALL -> stringResource(R.string.book_notes_selected_all, uiState.filteredBookNotes.size)
                            NoteType.HIGHLIGHT -> stringResource(
                                R.string.book_notes_selected_highlights,
                                uiState.filteredBookNotes.size,
                            )
                            NoteType.THOUGHT -> stringResource(
                                R.string.book_notes_selected_thoughts,
                                uiState.filteredBookNotes.size,
                            )
                            NoteType.BOOKMARK -> stringResource(
                                R.string.book_notes_selected_bookmarks,
                                uiState.filteredBookNotes.size,
                            )
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Box {
                        TextButton(onClick = { filterMenuExpanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_filter_menu),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.filter))
                        }

                        GlassDropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.book_notes_filter_all, notes.size)) },
                                onClick = {
                                    filterMenuExpanded = false
                                    onFilterChange(NoteType.ALL)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.book_notes_filter_highlights, highlightCount)) },
                                onClick = {
                                    filterMenuExpanded = false
                                    onFilterChange(NoteType.HIGHLIGHT)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.book_notes_filter_thoughts, thoughtCount)) },
                                onClick = {
                                    filterMenuExpanded = false
                                    onFilterChange(NoteType.THOUGHT)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.book_notes_filter_bookmarks, bookmarkCount)) },
                                onClick = {
                                    filterMenuExpanded = false
                                    onFilterChange(NoteType.BOOKMARK)
                                },
                            )
                        }
                    }
                }
            }

            // Grouped Notes List
            if (groupedNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.book_notes_filtered_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                groupedNotes.forEach { (chapterTitle, itemsInChapter) ->
                    item(key = "header_$chapterTitle") {
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                        )
                    }

                    items(itemsInChapter, key = { it.id }) { item ->
                        BookNoteCard(
                            item = item,
                            manga = manga,
                            onClick = {
                                manga?.let { onJumpToReader(it, item) }
                            },
                            onMakeExcerpt = {
                                if (item is BookNoteItem.NovelHighlight) {
                                    activeExcerptData = NovelExcerptData(
                                        selectedText = item.text,
                                        bookTitle = manga?.title.orEmpty(),
                                        chapterTitle = item.chapterTitle,
                                        author = manga?.authors?.firstOrNull().orEmpty(),
                                    )
                                }
                            },
                            onCopy = {
                                val copyText = when (item) {
                                    is BookNoteItem.NovelHighlight -> item.note?.let {
                                        "$it\n${context.getString(R.string.book_notes_quote_prefix, item.text)}"
                                    } ?: item.text
                                    is BookNoteItem.BookmarkEntry -> context.getString(
                                        R.string.book_notes_bookmark_position,
                                        item.chapterTitle,
                                        item.page + 1,
                                    )
                                }
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("note", copyText))
                                Toast.makeText(context, R.string.book_notes_copied, Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { onDeleteNote(item) },
                        )
                    }
                }
            }
        }
    }

    // Excerpt Sheet
    activeExcerptData?.let { data ->
        NovelExcerptSheet(
            data = data,
            onDismiss = { activeExcerptData = null },
        )
    }

    // Export Bottom Sheet
    if (exportSheetVisible && manga != null) {
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
                createDocumentLauncher.launch(
                    context.getString(R.string.book_notes_export_filename, safeTitle),
                )
            },
        )
    }
}

@Composable
private fun BookDetailHeaderCard(
    manga: Content,
    totalNotesCount: Int,
    onExcerptClick: () -> Unit,
    onSearchClick: () -> Unit,
    onExportClick: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArtworkBackground = LocalBackgroundStyle.current == BackgroundStyle.DYNAMIC_ARTWORK_BLUR

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.artworkAwareContainerColor(),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isArtworkBackground) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                ) {
                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.book_notes_total_count, totalNotesCount),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (manga.authors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = manga.authors.joinToString(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 54.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4 quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickActionButton(
                    iconRes = R.drawable.ic_images,
                    label = stringResource(R.string.book_notes_make_excerpt),
                    onClick = onExcerptClick,
                )
                QuickActionButton(
                    iconRes = R.drawable.ic_search,
                    label = stringResource(R.string.book_notes_search_notes),
                    onClick = onSearchClick,
                )
                QuickActionButton(
                    iconRes = R.drawable.ic_share,
                    label = stringResource(R.string.book_notes_export),
                    onClick = onExportClick,
                )
                QuickActionButton(
                    iconRes = R.drawable.ic_book_page,
                    label = stringResource(R.string.continue_reading),
                    onClick = onReadClick,
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun BookNoteCard(
    item: BookNoteItem,
    onClick: () -> Unit,
    onMakeExcerpt: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    manga: Content? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isArtworkBackground = LocalBackgroundStyle.current == BackgroundStyle.DYNAMIC_ARTWORK_BLUR

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.artworkAwareContainerColor(),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isArtworkBackground) 0.dp else 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Indicator Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (item) {
                            is BookNoteItem.NovelHighlight -> {
                                val markingColor = NovelMarkingColor.fromId(item.color)
                                markingColor.lineColor.copy(alpha = 0.18f)
                            }
                            is BookNoteItem.BookmarkEntry -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when (item) {
                    is BookNoteItem.NovelHighlight -> {
                        val markingColor = NovelMarkingColor.fromId(item.color)
                        if (item.note.isNullOrBlank()) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = markingColor.lineColor,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_comment),
                                contentDescription = stringResource(R.string.notes),
                                modifier = Modifier.size(13.dp),
                                tint = markingColor.lineColor,
                            )
                        }
                    }
                    is BookNoteItem.BookmarkEntry -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark),
                            contentDescription = stringResource(R.string.bookmarks),
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body content
            Column(modifier = Modifier.weight(1f)) {
                when (item) {
                    is BookNoteItem.NovelHighlight -> {
                        if (!item.note.isNullOrBlank()) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.book_notes_quote_prefix, item.text),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    is BookNoteItem.BookmarkEntry -> {
                        Text(
                            text = stringResource(
                                R.string.book_notes_bookmark_position,
                                item.chapterTitle,
                                item.page + 1,
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val imageModel = remember(item, manga) {
                            when {
                                !item.localSnapshotUri.isNullOrBlank() -> item.localSnapshotUri
                                item.imageUrl?.startsWith("file://") == true -> item.imageUrl
                                else -> item.toContentPage() ?: manga?.let {
                                    ContentPage(
                                        id = item.id,
                                        url = item.imageUrl.orEmpty(),
                                        preview = null,
                                        source = it.source,
                                    )
                                } ?: item.imageUrl
                            }
                        }
                        if (imageModel != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }

            // More actions
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                GlassDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (item is BookNoteItem.NovelHighlight) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.book_notes_make_excerpt)) },
                            onClick = {
                                menuExpanded = false
                                onMakeExcerpt()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.book_notes_copy_content)) },
                        onClick = {
                            menuExpanded = false
                            onCopy()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookNotesExportBottomSheet(
    manga: Content,
    notes: List<BookNoteItem>,
    onDismiss: () -> Unit,
    onCopyMarkdown: () -> Unit,
    onShareMarkdown: () -> Unit,
    onSaveMarkdown: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.book_notes_export_title, manga.title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.book_notes_export_summary, notes.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.book_notes_copy_markdown)) },
                supportingContent = { Text(stringResource(R.string.book_notes_copy_markdown_summary)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onCopyMarkdown()
                    },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.book_notes_share_markdown)) },
                supportingContent = { Text(stringResource(R.string.book_notes_share_markdown_summary)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onShareMarkdown()
                    },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.book_notes_save_markdown)) },
                supportingContent = { Text(stringResource(R.string.book_notes_save_markdown_summary)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onSaveMarkdown()
                    },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
