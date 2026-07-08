package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.audio.MilestoneCelebrationCoordinator
import com.booklog.app.data.local.Book
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.data.repository.BookRepository
import com.booklog.app.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: Book? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed class BookDetailEvent {
    data class BookSaved(val title: String) : BookDetailEvent()
}

class BookDetailViewModel(
    private val repository: BookRepository,
    private val rewardRepository: RewardRepository,
    private val milestoneCelebrationCoordinator: MilestoneCelebrationCoordinator,
    private val bookId: Long,
    private val activeKidIdProvider: () -> Long? = { null },
    private val onBookUpdated: ((Book) -> Unit)? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BookDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BookDetailEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeBook(bookId).collect { book ->
                _uiState.update { it.copy(book = book) }
            }
        }
    }

    fun saveChanges(book: Book) {
        persistBook(book, emitSavedEvent = true)
    }

    fun updateStatus(status: ReadingStatus) {
        val current = _uiState.value.book ?: return
        if (current.status == status) return
        val now = System.currentTimeMillis()
        val updated = when (status) {
            ReadingStatus.READING -> current.copy(
                status = status,
                dateStarted = current.dateStarted ?: now,
            )
            ReadingStatus.FINISHED -> current.copy(
                status = status,
                dateStarted = current.dateStarted ?: now,
                dateFinished = now,
                currentPage = current.pageCount ?: current.currentPage,
            )
            ReadingStatus.WANT_TO_READ -> current.copy(status = status)
        }
        persistBook(updated, emitSavedEvent = false)
    }

    private fun persistBook(book: Book, emitSavedEvent: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val toSave = if (book.kidProfileId == null) {
                book.copy(kidProfileId = activeKidIdProvider())
            } else {
                book
            }
            runCatching {
                val previous = _uiState.value.book
                repository.updateBook(toSave)
                logPageProgress(previous, toSave)
            }
                .onSuccess {
                    onBookUpdated?.invoke(toSave)
                    milestoneCelebrationCoordinator.checkAndCelebrate(
                        toSave.kidProfileId ?: activeKidIdProvider(),
                    )
                    _uiState.update { it.copy(isSaving = false) }
                    if (emitSavedEvent) {
                        _events.emit(BookDetailEvent.BookSaved(toSave.title))
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = error.message ?: "Failed to save")
                    }
                }
        }
    }

    private suspend fun logPageProgress(previous: Book?, updated: Book) {
        if (previous == null) return
        val kidId = updated.kidProfileId ?: activeKidIdProvider()
        when (updated.status) {
            ReadingStatus.READING -> {
                val oldPages = previous.currentPage ?: 0
                val newPages = updated.currentPage ?: oldPages
                val delta = newPages - oldPages
                if (delta > 0) {
                    rewardRepository.recordPagesRead(kidId, updated.id, delta)
                }
            }
            ReadingStatus.FINISHED -> {
                if (previous.status != ReadingStatus.FINISHED) {
                    val pages = when (previous.status) {
                        ReadingStatus.READING -> {
                            val target = updated.pageCount ?: updated.currentPage ?: 0
                            val already = previous.currentPage ?: 0
                            (target - already).coerceAtLeast(0)
                        }
                        else -> updated.pageCount ?: updated.currentPage ?: 0
                    }
                    if (pages > 0) {
                        rewardRepository.recordPagesRead(kidId, updated.id, pages)
                    }
                }
            }
            else -> Unit
        }
    }

    fun deleteBook(onDeleted: () -> Unit) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            repository.deleteBook(book)
            onDeleted()
        }
    }

    class Factory(
        private val repository: BookRepository,
        private val rewardRepository: RewardRepository,
        private val milestoneCelebrationCoordinator: MilestoneCelebrationCoordinator,
        private val bookId: Long,
        private val activeKidIdProvider: () -> Long? = { null },
        private val onBookUpdated: ((Book) -> Unit)? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BookDetailViewModel(
                repository = repository,
                rewardRepository = rewardRepository,
                milestoneCelebrationCoordinator = milestoneCelebrationCoordinator,
                bookId = bookId,
                activeKidIdProvider = activeKidIdProvider,
                onBookUpdated = onBookUpdated,
            ) as T
    }
}