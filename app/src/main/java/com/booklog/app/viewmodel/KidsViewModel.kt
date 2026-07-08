package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.cloud.CloudRepository
import com.booklog.app.data.local.KidProfile
import com.booklog.app.data.profiles.ActiveKidPreferences
import com.booklog.app.data.repository.KidProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KidsUiState(
    val kids: List<KidProfile> = emptyList(),
    val activeKidId: Long? = null,
    val activeKid: KidProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

class KidsViewModel(
    private val kidProfileRepository: KidProfileRepository,
    private val activeKidPreferences: ActiveKidPreferences,
    private val cloudRepository: CloudRepository,
) : ViewModel() {
    private val status = MutableStateFlow(KidsStatus())

    val uiState: StateFlow<KidsUiState> = combine(
        kidProfileRepository.observeAll(),
        status,
    ) { kids, s ->
        KidsUiState(
            kids = kids,
            activeKidId = s.activeKidId,
            activeKid = kids.find { it.id == s.activeKidId },
            isLoading = s.isLoading,
            error = s.error,
            successMessage = s.successMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KidsUiState())

    init {
        refreshActiveKid()
    }

    fun refreshActiveKid() {
        status.update { it.copy(activeKidId = activeKidPreferences.getActiveKidId()) }
    }

    fun ensureDefaultKidFor(kids: List<KidProfile>) {
        when {
            kids.isEmpty() -> refreshActiveKid()
            activeKidPreferences.isUnset() -> selectKid(kids.first().id)
            else -> refreshActiveKid()
        }
    }

    fun saveProfile(profile: KidProfile, onSuccess: () -> Unit = {}) {
        if (profile.name.isBlank()) {
            status.update { it.copy(error = "Name is required") }
            return
        }
        viewModelScope.launch {
            status.update { it.copy(isLoading = true, error = null, successMessage = null) }
            runCatching {
                val saved = kidProfileRepository.save(profile)
                if (cloudRepository.currentUser != null) {
                    cloudRepository.addKidProfile(saved)
                    cloudRepository.syncKidProfiles()
                }
                saved
            }.onSuccess { saved ->
                status.update {
                    it.copy(
                        isLoading = false,
                        successMessage = if (profile.id == 0L) {
                            "Added ${saved.name}!"
                        } else {
                            "Saved ${saved.name}'s profile"
                        },
                    )
                }
                onSuccess()
            }.onFailure { e ->
                status.update { it.copy(isLoading = false, error = e.message ?: "Couldn't save profile") }
            }
        }
    }

    fun removeKid(profile: KidProfile) {
        viewModelScope.launch {
            status.update { it.copy(isLoading = true, error = null) }
            runCatching {
                kidProfileRepository.delete(profile)
                if (activeKidPreferences.getActiveKidId() == profile.id) {
                    activeKidPreferences.setActiveKidId(null)
                    status.update { s -> s.copy(activeKidId = null) }
                }
                if (cloudRepository.currentUser != null) {
                    cloudRepository.removeKidProfile(profile)
                }
            }.onSuccess {
                status.update { it.copy(isLoading = false, successMessage = "Removed ${profile.name}") }
            }.onFailure { e ->
                status.update { it.copy(isLoading = false, error = e.message ?: "Couldn't remove profile") }
            }
        }
    }

    fun selectKid(id: Long?) {
        activeKidPreferences.setActiveKidId(id)
        status.update { it.copy(activeKidId = id) }
    }

    fun clearMessages() = status.update { it.copy(error = null, successMessage = null) }

    private data class KidsStatus(
        val activeKidId: Long? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
    )

    class Factory(
        private val kidProfileRepository: KidProfileRepository,
        private val activeKidPreferences: ActiveKidPreferences,
        private val cloudRepository: CloudRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            KidsViewModel(kidProfileRepository, activeKidPreferences, cloudRepository) as T
    }
}