package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.data.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val totalBooks: Int = 0,
    val wantToRead: Int = 0,
    val reading: Int = 0,
    val finished: Int = 0,
    val pagesRead: Int = 0,
)

class StatsViewModel(repository: BookRepository) : ViewModel() {
    val stats = combine(
        repository.observeTotalCount(),
        repository.observeCountByStatus(ReadingStatus.WANT_TO_READ),
        repository.observeCountByStatus(ReadingStatus.READING),
        repository.observeCountByStatus(ReadingStatus.FINISHED),
        repository.observePagesRead(),
    ) { total, want, reading, finished, pages ->
        StatsUiState(total, want, reading, finished, pages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    class Factory(private val repository: BookRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(repository) as T
    }
}