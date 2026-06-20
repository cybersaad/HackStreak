package com.cybersaad.hackstreak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cybersaad.hackstreak.data.SyncResult
import com.cybersaad.hackstreak.data.ThmRepository
import com.cybersaad.hackstreak.data.UserProfileEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MainUiState(
    val username: String = "",
    val usernameInput: String = "",
    val profile: UserProfileEntity? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    /** Warning shown when displaying stale cache after a failed sync */
    val warningMessage: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val hasEverSynced: Boolean = false
)

class MainViewModel(private val repository: ThmRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Single profile observation job — prevents collector leaks */
    private var profileObserverJob: Job? = null

    init {
        viewModelScope.launch {
            // Load saved username
            val savedUsername = repository.getSavedUsername()
            if (savedUsername.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    username = savedUsername,
                    usernameInput = savedUsername,
                    isLoading = true
                )
                // Start observing cached profile (single collector)
                observeProfile(savedUsername)
                // Auto-sync on launch
                performSync(savedUsername)
            }
        }
    }

    /**
     * Starts observing the profile for the given username.
     * Cancels any previous observer to prevent collector leaks.
     */
    private fun observeProfile(username: String) {
        profileObserverJob?.cancel()
        profileObserverJob = viewModelScope.launch {
            repository.getProfile(username).collectLatest { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    isLoading = false,
                    hasEverSynced = profile != null,
                    lastSyncTimestamp = profile?.lastSyncedTimestamp ?: _uiState.value.lastSyncTimestamp
                )
            }
        }
    }

    fun onUsernameInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(usernameInput = input)
    }

    fun syncStreak() {
        val username = _uiState.value.usernameInput.trim()
        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a username")
            return
        }

        _uiState.value = _uiState.value.copy(
            isSyncing = true,
            errorMessage = null,
            warningMessage = null,
            username = username
        )

        // Save username & start observing (replaces any previous observer)
        viewModelScope.launch {
            repository.saveUsername(username)
        }
        observeProfile(username)

        // Perform sync
        viewModelScope.launch {
            performSync(username)
        }
    }

    /**
     * Performs the actual sync and updates UI state based on the result.
     */
    private suspend fun performSync(username: String) {
        _uiState.value = _uiState.value.copy(
            isSyncing = true,
            errorMessage = null,
            warningMessage = null
        )

        when (val result = repository.syncProfile(username)) {
            is SyncResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    profile = result.profile,
                    hasEverSynced = true,
                    lastSyncTimestamp = result.profile.lastSyncedTimestamp,
                    errorMessage = null,
                    warningMessage = null
                )
            }
            is SyncResult.StaleCache -> {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    profile = result.profile,
                    hasEverSynced = true,
                    lastSyncTimestamp = result.profile.lastSyncedTimestamp,
                    errorMessage = null,
                    warningMessage = "Sync failed — showing cached data. ${result.errorMessage}"
                )
            }
            is SyncResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = result.errorMessage
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissWarning() {
        _uiState.value = _uiState.value.copy(warningMessage = null)
    }

    /**
     * Returns formatted time-ago string for the last sync timestamp.
     */
    fun getTimeSinceLastSync(): String {
        val lastSync = _uiState.value.lastSyncTimestamp
        if (lastSync == 0L) return "Never"

        val diffMs = System.currentTimeMillis() - lastSync
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHr = diffMin / 60
        val diffDay = diffHr / 24

        return when {
            diffSec < 60 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHr < 24 -> "${diffHr}h ago"
            else -> "${diffDay}d ago"
        }
    }

    class Factory(private val repository: ThmRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
