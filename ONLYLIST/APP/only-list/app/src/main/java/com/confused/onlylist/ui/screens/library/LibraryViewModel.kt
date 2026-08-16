package com.confused.onlylist.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.confused.onlylist.AppContainer
import com.confused.onlylist.common.Logger
import com.confused.onlylist.data.mock.MockMedia
import com.confused.onlylist.ui.components.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * LibraryViewModel — shows trending AniList media as the "library" for now
 * (authenticated list entries require the user to link their AniList account).
 * Per CORE_RULES §14: offline-first.
 */
class LibraryViewModel : ViewModel() {

    private val repository = AppContainer.mediaRepository

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Use trending as the library for now (Phase 3.5: real MediaListCollection when authenticated)
    val library: StateFlow<List<MockMedia>> = repository.getTrending()
        .map { entities -> entities.map { it.toUiModel() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            Logger.d("Library", "Refreshing library from AniList...")
            val result = repository.refreshTrending()
            result.fold(
                onSuccess = { Logger.d("Library", "Library refresh OK") },
                onFailure = { Logger.w("Library", "Library refresh failed: ${it.message}") },
            )
            _isRefreshing.value = false
        }
    }
}
