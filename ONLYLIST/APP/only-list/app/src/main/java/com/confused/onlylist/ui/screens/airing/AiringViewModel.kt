package com.confused.onlylist.ui.screens.airing

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
 * AiringViewModel — shows currently-airing anime (status = RELEASING).
 * Per CORE_RULES §14: offline-first.
 */
class AiringViewModel : ViewModel() {

    private val repository = AppContainer.mediaRepository

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Use "airing" (status=RELEASING) media from Room
    val airing: StateFlow<List<MockMedia>> = repository.getAiring()
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
            Logger.d("Airing", "Refreshing airing schedule from AniList...")
            val result = repository.refreshTrending()  // refresh trending (which includes airing)
            result.fold(
                onSuccess = { Logger.d("Airing", "Airing refresh OK") },
                onFailure = { Logger.w("Airing", "Airing refresh failed: ${it.message}") },
            )
            _isRefreshing.value = false
        }
    }
}
