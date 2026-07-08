package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.cloud.CloudRepository
import com.booklog.app.data.cloud.LeaderboardEntry
import com.booklog.app.data.cloud.LeaderboardType
import com.booklog.app.data.leaderboard.LocalLeaderboardCalculator
import com.booklog.app.data.leaderboard.LocalLeaderboardEntry
import com.booklog.app.data.repository.BookRepository
import com.booklog.app.data.repository.KidProfileRepository
import com.booklog.app.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LeaderboardScope {
    LOCAL,
    GLOBAL,
}

data class LeaderboardUiState(
    val scope: LeaderboardScope = LeaderboardScope.LOCAL,
    val type: LeaderboardType = LeaderboardType.READERS,
    val entries: List<LeaderboardEntry> = emptyList(),
    val localEntries: List<LocalLeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LeaderboardViewModel(
    private val cloudRepository: CloudRepository,
    private val kidProfileRepository: KidProfileRepository,
    private val rewardRepository: RewardRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectScope(scope: LeaderboardScope) {
        if (_uiState.value.scope == scope) return
        _uiState.update { it.copy(scope = scope) }
        refresh()
    }

    fun selectType(type: LeaderboardType) {
        if (_uiState.value.type == type) return
        _uiState.update { it.copy(type = type) }
        if (_uiState.value.scope == LeaderboardScope.GLOBAL) {
            refresh()
        }
    }

    fun refresh() {
        when (_uiState.value.scope) {
            LeaderboardScope.LOCAL -> refreshLocal()
            LeaderboardScope.GLOBAL -> refreshGlobal()
        }
    }

    private fun refreshLocal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val kids = kidProfileRepository.getAll()
                val completionsByKid = kids.associate { kid ->
                    kid.id to rewardRepository.getCompletions(kid.id)
                }
                val booksByKid = kids.associate { kid ->
                    kid.id to bookRepository.getBooksForProfile(kid.id)
                }
                val logsByKid = kids.associate { kid ->
                    kid.id to rewardRepository.getReadingLogs(kid.id)
                }
                val balanceByKid = kids.associate { kid ->
                    kid.id to rewardRepository.getBalanceCents(kid.id)
                }
                LocalLeaderboardCalculator.compute(
                    kids = kids,
                    completionsByKid = completionsByKid,
                    booksByKid = booksByKid,
                    logsByKid = logsByKid,
                    balanceByKid = balanceByKid,
                )
            }.onSuccess { entries ->
                _uiState.update {
                    it.copy(isLoading = false, localEntries = entries, entries = emptyList())
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Couldn't load family leaderboard",
                        localEntries = emptyList(),
                    )
                }
            }
        }
    }

    private fun refreshGlobal() {
        val type = _uiState.value.type
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            cloudRepository.fetchLeaderboard(type)
                .onSuccess { entries ->
                    _uiState.update {
                        it.copy(isLoading = false, entries = entries, localEntries = emptyList())
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Couldn't load leaderboard",
                            entries = emptyList(),
                        )
                    }
                }
        }
    }

    class Factory(
        private val cloudRepository: CloudRepository,
        private val kidProfileRepository: KidProfileRepository,
        private val rewardRepository: RewardRepository,
        private val bookRepository: BookRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LeaderboardViewModel(
                cloudRepository,
                kidProfileRepository,
                rewardRepository,
                bookRepository,
            ) as T
    }
}