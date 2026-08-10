package com.brk.booklogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brk.booklogger.data.cloud.CloudRepository
import com.brk.booklogger.data.cloud.HouseholdInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdUiState(
    val household: HouseholdInfo = HouseholdInfo(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val joinCodeInput: String = "",
)

class HouseholdViewModel(
    private val cloudRepository: CloudRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (cloudRepository.currentUser == null) {
            _uiState.update {
                it.copy(household = HouseholdInfo(isLinked = false), isLoading = false)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            cloudRepository.fetchHousehold()
                .onSuccess { info ->
                    _uiState.update { it.copy(household = info, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Couldn't load household",
                        )
                    }
                }
        }
    }

    fun onJoinCodeChange(value: String) {
        _uiState.update { it.copy(joinCodeInput = value.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(8)) }
    }

    fun createHousehold() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.createHousehold()
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            household = info,
                            isLoading = false,
                            successMessage = "Household created! Share code ${info.inviteCode} with your partner.",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't create household")
                    }
                }
        }
    }

    fun joinHousehold() {
        val code = _uiState.value.joinCodeInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.joinHousehold(code)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            household = info,
                            isLoading = false,
                            joinCodeInput = "",
                            successMessage = "Joined household! Shared library is syncing.",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't join household")
                    }
                }
        }
    }

    fun leaveHousehold() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.leaveHousehold()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            household = HouseholdInfo(isLinked = false),
                            isLoading = false,
                            successMessage = "Left household. Your local books stay on this device.",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't leave household")
                    }
                }
        }
    }

    fun regenerateCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.regenerateInviteCode()
                .onSuccess { code ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            household = it.household.copy(inviteCode = code),
                            successMessage = "New invite code: $code",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't regenerate code")
                    }
                }
        }
    }

    fun pullLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.pullLibraryFromCloud()
                .onSuccess {
                    cloudRepository.syncLocalBooksToCloud()
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "Library synced (covers loaded from Open Library)")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Sync failed")
                    }
                }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    class Factory(
        private val cloudRepository: CloudRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HouseholdViewModel(cloudRepository) as T
    }
}
