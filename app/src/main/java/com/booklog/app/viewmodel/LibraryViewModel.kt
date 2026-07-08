package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.local.Book
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.data.repository.BookRepository
import com.booklog.app.data.repository.RewardRepository
import com.booklog.app.data.streak.ReadingStreakCalculator
import com.booklog.app.data.streak.ReadingStreakInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: BookRepository,
    private val rewardRepository: RewardRepository,
    private val activeKidIdProvider: () -> Long?,
) : ViewModel() {
    private val filter = MutableStateFlow<ReadingStatus?>(null)
    private val activeKidId = MutableStateFlow<Long?>(activeKidIdProvider())

    val books: StateFlow<List<Book>> = combine(
        repository.observeAllBooks(),
        filter,
        activeKidId,
    ) { allBooks, status, kidId ->
        val profileBooks = when (kidId) {
            null -> allBooks.filter { it.kidProfileId == null }
            else -> allBooks.filter { it.kidProfileId == kidId }
        }
        if (status == null) profileBooks else profileBooks.filter { it.status == status }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val streakInfo: StateFlow<ReadingStreakInfo> = activeKidId
        .flatMapLatest { kidId -> rewardRepository.observeReadingLogs(kidId) }
        .combine(activeKidId) { logs, _ -> ReadingStreakCalculator.compute(logs) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ReadingStreakInfo(0, false, 0, 0),
        )

    fun refreshActiveKid() {
        activeKidId.value = activeKidIdProvider()
    }

    fun setFilter(status: ReadingStatus?) {
        filter.value = status
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { repository.deleteBook(book) }
    }

    class Factory(
        private val repository: BookRepository,
        private val rewardRepository: RewardRepository,
        private val activeKidIdProvider: () -> Long?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(repository, rewardRepository, activeKidIdProvider) as T
    }
}