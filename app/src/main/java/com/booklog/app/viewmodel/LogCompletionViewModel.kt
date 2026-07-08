package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.local.Book
import com.booklog.app.data.local.CompletedBook

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

data class LogCompletionUiState(
    val book: Book,
    val kidId: Long? = null,
    val priorHistory: List<CompletedBook> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed class LogCompletionEvent {
    data class Success(val title: String, val rewardCents: Int) : LogCompletionEvent()
}

class LogCompletionViewModel(
    initialBook: Book,
    private val kidId: Long?,
    private val bookRepository: BookRepository,
    private val rewardRepository: RewardRepository,
    private val onBookSynced: ((Book) -> Unit)? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogCompletionUiState(book = initialBook, kidId = kidId))
    val uiState: StateFlow<LogCompletionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogCompletionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LogCompletionEvent> = _events.asSharedFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val isbn = _uiState.value.book.isbn ?: return
        viewModelScope.launch {
            val history = rewardRepository.getCompletionHistory(isbn, kidId)
            _uiState.update { it.copy(priorHistory = history) }
        }
    }

    fun logCompletion(minutesRead: Int) {
        if (minutesRead <= 0) {
            _uiState.update { it.copy(error = "Enter how many minutes you read") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                val book = _uiState.value.book.copy(kidProfileId = kidId)
                val completion = rewardRepository
                    .logBookCompletion(kidId, book, minutesRead)
                    .getOrThrow()
                val savedBook = bookRepository.getBook(completion.bookId)
                savedBook?.let { onBookSynced?.invoke(it) }
                completion
            }.onSuccess { completion ->
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(
                    LogCompletionEvent.Success(
                        title = _uiState.value.book.title,
                        rewardCents = completion.totalRewardCents,
                    ),
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = error.message ?: "Couldn't log completion",
                    )
                }
            }
        }
    }

    class Factory(
        private val initialBook: Book,
        private val kidId: Long?,
        private val bookRepository: BookRepository,
        private val rewardRepository: RewardRepository,
        private val onBookSynced: ((Book) -> Unit)? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LogCompletionViewModel(
                initialBook,
                kidId,
                bookRepository,
                rewardRepository,
                onBookSynced,
            ) as T
    }
}