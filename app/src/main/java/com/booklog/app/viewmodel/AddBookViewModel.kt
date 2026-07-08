package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.local.Book
import com.booklog.app.data.remote.BookSearchResult
import com.booklog.app.data.remote.CoverUrlResolver
import com.booklog.app.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookUiState(
    val isbn: String = "",
    val title: String = "",
    val author: String = "",
    val pageCount: String = "",
    val publishedYear: String = "",
    val coverUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val saveSuccess: Boolean = false,
    val searchResults: List<BookSearchResult> = emptyList(),
    val selectedKidId: Long? = null,
)

sealed class AddBookEvent {
    data class BookSaved(val id: Long, val title: String) : AddBookEvent()
}

class AddBookViewModel(
    private val repository: BookRepository,
    private val activeKidIdProvider: () -> Long? = { null },
    private val onBookSaved: ((Book) -> Unit)? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddBookEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddBookEvent> = _events.asSharedFlow()

    init {
        _uiState.update { it.copy(selectedKidId = activeKidIdProvider()) }
    }

    fun refreshSelectedKid() {
        _uiState.update { it.copy(selectedKidId = activeKidIdProvider()) }
    }

    fun selectKid(kidId: Long?) {
        _uiState.update { it.copy(selectedKidId = kidId) }
    }

    fun updateIsbn(value: String) = _uiState.update { it.copy(isbn = value, error = null, info = null) }
    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, error = null) }
    fun updateAuthor(value: String) = _uiState.update { it.copy(author = value, error = null) }
    fun updatePageCount(value: String) = _uiState.update { it.copy(pageCount = value, error = null) }
    fun updatePublishedYear(value: String) = _uiState.update { it.copy(publishedYear = value, error = null) }

    fun lookupIsbn() {
        val isbn = _uiState.value.isbn.trim()
        if (isbn.isBlank()) {
            _uiState.update { it.copy(error = "Enter an ISBN to look up") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null, searchResults = emptyList()) }
            repository.lookupByIsbn(isbn)
                .onSuccess { book ->
                    val cover = CoverUrlResolver.bestAvailable(book.isbn ?: isbn, book.coverUrl)
                    val needsManual = book.title.isBlank()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isbn = book.isbn ?: isbn,
                            title = book.title,
                            author = book.author,
                            pageCount = book.pageCount?.toString() ?: "",
                            publishedYear = book.publishedYear ?: "",
                            coverUrl = cover,
                            info = if (needsManual) {
                                "We couldn't find full details, but here's the cover! Fill in title & author."
                            } else {
                                "Found it! Check the details below."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    val cover = CoverUrlResolver.fromIsbn(isbn)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            coverUrl = cover,
                            error = error.message,
                        )
                    }
                }
        }
    }

    fun searchByTitleAndAuthor() {
        val state = _uiState.value
        if (state.title.isBlank() && state.author.isBlank()) {
            _uiState.update { it.copy(error = "Enter a title and/or author to search") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null, searchResults = emptyList()) }
            repository.searchByTitleAndAuthor(state.title, state.author)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            searchResults = results,
                            info = "Found ${results.size} match${if (results.size == 1) "" else "es"}! Tap one to fill in details.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun applySearchResult(result: BookSearchResult) {
        _uiState.update {
            it.copy(
                title = result.title,
                author = result.author,
                isbn = result.isbn ?: it.isbn,
                pageCount = result.pageCount?.toString() ?: it.pageCount,
                publishedYear = result.publishedYear ?: it.publishedYear,
                coverUrl = CoverUrlResolver.bestAvailable(result.isbn, result.coverUrl),
                searchResults = emptyList(),
                info = "Loaded \"${result.title}\" — review and save!",
                error = null,
            )
        }
    }

    fun applyScannedIsbn(isbn: String) {
        _uiState.update { it.copy(isbn = isbn, error = null, info = null) }
        lookupIsbn()
    }

    fun saveBook() {
        val state = _uiState.value
        if (state.title.isBlank() || state.author.isBlank()) {
            _uiState.update { it.copy(error = "Title and author are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveSuccess = false) }
            val book = Book(
                isbn = state.isbn.takeIf { it.isNotBlank() },
                title = state.title.trim(),
                author = state.author.trim(),
                coverUrl = CoverUrlResolver.bestAvailable(state.isbn, state.coverUrl),
                pageCount = state.pageCount.toIntOrNull(),
                publishedYear = state.publishedYear.takeIf { it.isNotBlank() },
                kidProfileId = state.selectedKidId,
            )
            runCatching { repository.saveBook(book) }
                .onSuccess { id ->
                    val saved = book.copy(id = id)
                    onBookSaved?.invoke(saved)
                    _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
                    _events.emit(AddBookEvent.BookSaved(id, book.title))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to save book")
                    }
                }
        }
    }

    class Factory(
        private val repository: BookRepository,
        private val activeKidIdProvider: () -> Long? = { null },
        private val onBookSaved: ((Book) -> Unit)? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddBookViewModel(repository, activeKidIdProvider, onBookSaved) as T
    }
}