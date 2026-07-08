package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.cloud.CloudRepository
import com.booklog.app.data.cloud.LeaderboardEntry
import com.booklog.app.data.cloud.LeaderboardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val type: LeaderboardType = LeaderboardType.READERS,
    val entries: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LeaderboardViewModel(private val cloudRepository: CloudRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectType(type: LeaderboardType) {
        if (_uiState.value.type == type) return
        _uiState.update { it.copy(type = type) }
        refresh()
    }

    fun refresh() {
        val type = _uiState.value.type
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            cloudRepository.fetchLeaderboard(type)
                .onSuccess { entries ->
                    _uiState.update { it.copy(isLoading = false, entries = entries) }
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

    class Factory(private val cloudRepository: CloudRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LeaderboardViewModel(cloudRepository) as T
    }
}