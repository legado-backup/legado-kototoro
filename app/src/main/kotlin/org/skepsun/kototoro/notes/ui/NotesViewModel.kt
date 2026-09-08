package org.skepsun.kototoro.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.skepsun.kototoro.core.db.MangaDatabase
import org.skepsun.kototoro.core.db.entity.toContent
import org.skepsun.kototoro.notes.domain.BookNoteItem
import org.skepsun.kototoro.notes.domain.BookNotesRepository
import org.skepsun.kototoro.notes.domain.BookNotesSummary
import org.skepsun.kototoro.notes.domain.NoteType
import org.skepsun.kototoro.parsers.model.Content
import javax.inject.Inject

data class NotesUiState(
    val summaries: List<BookNotesSummary> = emptyList(),
    val searchQuery: String = "",
    val selectedMangaId: Long? = null,
    val selectedManga: Content? = null,
    val selectedBookNotes: List<BookNoteItem> = emptyList(),
    val selectedFilter: NoteType = NoteType.ALL,
    val bookSearchQuery: String = "",
    val isLoading: Boolean = true,
) {
    val totalNotesCount: Int get() = summaries.sumOf { it.totalCount }
    val totalBooksCount: Int get() = summaries.size

    val filteredSummaries: List<BookNotesSummary>
        get() = if (searchQuery.isBlank()) {
            summaries
        } else {
            summaries.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

    val filteredBookNotes: List<BookNoteItem>
        get() {
            var items = selectedBookNotes
            if (selectedFilter != NoteType.ALL) {
                items = items.filter { it.noteType == selectedFilter }
            }
            if (bookSearchQuery.isNotBlank()) {
                items = items.filter { item ->
                    when (item) {
                        is BookNoteItem.NovelHighlight -> {
                            item.text.contains(bookSearchQuery, ignoreCase = true) ||
                                (item.note?.contains(bookSearchQuery, ignoreCase = true) == true)
                        }
                        is BookNoteItem.BookmarkEntry -> {
                            item.chapterTitle.contains(bookSearchQuery, ignoreCase = true)
                        }
                    }
                }
            }
            return items
        }

    val notesGroupedByChapter: Map<String, List<BookNoteItem>>
        get() = filteredBookNotes.groupBy { it.chapterTitle }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: BookNotesRepository,
    private val database: MangaDatabase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedMangaId = MutableStateFlow<Long?>(null)
    private val selectedFilter = MutableStateFlow(NoteType.ALL)
    private val bookSearchQuery = MutableStateFlow("")

    private val selectedBookData = selectedMangaId.flatMapLatest { mangaId ->
        if (mangaId == null) {
            flowOf(Pair<Content?, List<BookNoteItem>>(null, emptyList()))
        } else {
            combine(
                repository.observeBookNotes(mangaId),
                flowOf(database.getMangaDao().find(mangaId)?.toContent()),
            ) { notes, manga ->
                Pair(manga, notes)
            }
        }
    }

    private val summariesAndSearch = combine(repository.observeSummaries(), searchQuery) { s, q -> Pair(s, q) }
    private val filterAndQuery = combine(selectedFilter, bookSearchQuery) { f, q -> Pair(f, q) }

    val uiState: StateFlow<NotesUiState> = combine(
        summariesAndSearch,
        selectedMangaId,
        selectedBookData,
        filterAndQuery,
    ) { (summaries, query), mangaId, (manga, notes), (filter, bookQuery) ->
        NotesUiState(
            summaries = summaries,
            searchQuery = query,
            selectedMangaId = mangaId,
            selectedManga = manga,
            selectedBookNotes = notes,
            selectedFilter = filter,
            bookSearchQuery = bookQuery,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState(),
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectBook(mangaId: Long) {
        selectedMangaId.value = mangaId
        selectedFilter.value = NoteType.ALL
        bookSearchQuery.value = ""
    }

    fun clearSelectedBook() {
        selectedMangaId.value = null
        bookSearchQuery.value = ""
    }

    fun setFilter(filter: NoteType) {
        selectedFilter.value = filter
    }

    fun setBookSearchQuery(query: String) {
        bookSearchQuery.value = query
    }

    fun deleteNote(item: BookNoteItem) {
        viewModelScope.launch {
            repository.deleteNote(item)
        }
    }

    fun updateThought(id: Long, note: String?) {
        viewModelScope.launch {
            repository.updateThoughtNote(id, note)
        }
    }
}
