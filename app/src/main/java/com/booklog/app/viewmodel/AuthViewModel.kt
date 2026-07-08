package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.cloud.CloudRepository
import com.booklog.app.data.cloud.CloudUserProfile
import com.booklog.app.data.profiles.GuestPreferences
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val profile: CloudUserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isGuestMode: Boolean = false,
)

class AuthViewModel(
    private val cloudRepository: CloudRepository,
    private val guestPreferences: GuestPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isGuestMode = guestPreferences.isGuestMode()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cloudRepository.observeAuthState().collect { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        error = null,
                        isGuestMode = if (user != null) false else it.isGuestMode,
                    )
                }
                if (user != null) {
                    guestPreferences.clearGuestMode()
                    refreshProfile()
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.signIn(email, password)
                .onSuccess {
                    guestPreferences.clearGuestMode()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGuestMode = false,
                            successMessage = "Welcome back! Your books are syncing ☁️",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = friendlyAuthError(e.message))
                    }
                }
        }
    }

    fun signUp(displayName: String, email: String, password: String) {
        if (displayName.isBlank()) {
            _uiState.update { it.copy(error = "Pick a fun display name!") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.signUp(email, password, displayName)
                .onSuccess {
                    guestPreferences.clearGuestMode()
                    cloudRepository.syncLocalBooksToCloud()
                    cloudRepository.syncKidProfiles()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGuestMode = false,
                            successMessage = "Account created! You're on the leaderboard now 🏆",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = friendlyAuthError(e.message))
                    }
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            cloudRepository.signInWithGoogle(idToken)
                .onSuccess {
                    guestPreferences.clearGuestMode()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGuestMode = false,
                            successMessage = "Signed in with Google! Your books are syncing ☁️",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = friendlyAuthError(e.message))
                    }
                }
        }
    }

    fun continueAsGuest() {
        guestPreferences.setGuestMode(true)
        _uiState.update {
            it.copy(
                isGuestMode = true,
                error = null,
                successMessage = "Browsing as guest — your books stay on this device!",
            )
        }
    }

    fun exitGuestMode() {
        guestPreferences.clearGuestMode()
        _uiState.update {
            it.copy(isGuestMode = false, successMessage = null, error = null)
        }
    }

    fun signOut() {
        cloudRepository.signOut()
        guestPreferences.clearGuestMode()
        _uiState.update {
            it.copy(
                profile = null,
                isGuestMode = false,
                successMessage = "Signed out. Your local books are still here!",
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            cloudRepository.syncLocalBooksToCloud()
                .onSuccess {
                    refreshProfile()
                    _uiState.update { it.copy(isLoading = false, successMessage = "Library synced to the cloud!") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sync failed") }
                }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    private fun refreshProfile() {
        viewModelScope.launch {
            cloudRepository.fetchProfile()
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile) } }
        }
    }

    private fun friendlyAuthError(message: String?): String = when {
        message == null -> "Something went wrong. Please try again."
        message.contains("badly formatted", true) -> "That email doesn't look right."
        message.contains("password", true) && message.contains("least", true) ->
            "Password needs at least 6 characters."
        message.contains("already in use", true) -> "That email already has an account. Try signing in!"
        message.contains("no user record", true) || message.contains("invalid", true) ->
            "Wrong email or password. Try again!"
        message.contains("network", true) -> "No internet. Check your connection and try again."
        else -> message
    }

    class Factory(
        private val cloudRepository: CloudRepository,
        private val guestPreferences: GuestPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(cloudRepository, guestPreferences) as T
    }
}