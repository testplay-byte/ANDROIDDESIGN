package com.confused.onlylist.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.confused.onlylist.AppContainer
import com.confused.onlylist.common.Logger
import com.confused.onlylist.data.mock.MockMedia
import com.confused.onlylist.ui.components.toUiModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SearchViewModel — observes Room search results + triggers AniList refresh on query change.
 * Per CORE_RULES §14: offline-first — Room Flow is the source of truth, network refreshes.
 */
class SearchViewModel : ViewModel() {

    private val repository = AppContainer.mediaRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Debounced query → Room search Flow → mapped to UI models
    @OptIn(FlowPreview::class)
    val results: StateFlow<List<MockMedia>> = _query
        .debounce(400)  // wait 400ms after user stops typing
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                // Trigger network refresh (fire + forget)
                refreshSearch(query)
                repository.search(query).map { entities ->
                    entities.map { it.toUiModel() }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    private fun refreshSearch(query: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            Logger.d("Search", "Refreshing search for '$query' from AniList...")
            val result = repository.refreshSearch(query)
            result.fold(
                onSuccess = { Logger.d("Search", "Search refresh OK") },
                onFailure = { Logger.w("Search", "Search refresh failed: ${it.message}") },
            )
            _isRefreshing.value = false
        }
    }
}
