package com.confused.onlylist.ui.screens.details

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
 * DetailsViewModel — loads a single media by ID (offline-first).
 * Phase 3.5 will add Kitsu/Jikan episode metadata fetch.
 */
class DetailsViewModel(private val mediaId: Int) : ViewModel() {

    private val repository = AppContainer.mediaRepository

    val media: StateFlow<MockMedia?> = repository.getById(mediaId)
        .map { entity -> entity?.toUiModel() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _episodes = MutableStateFlow<List<EpisodeUi>>(emptyList())
    val episodes: StateFlow<List<EpisodeUi>> = _episodes.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            Logger.d("Details", "Refreshing media #$mediaId from AniList...")
            val result = repository.refreshById(mediaId)
            result.fold(
                onSuccess = { Logger.d("Details", "Media #$mediaId refresh OK") },
                onFailure = { Logger.w("Details", "Media #$mediaId refresh failed: ${it.message}") },
            )

            // Generate episode list (Phase 3.5: fetch from Kitsu/Jikan)
            val currentMedia = media.value
            val episodeCount = currentMedia?.episodes ?: 0
            _episodes.value = (1..episodeCount.coerceAtMost(24)).map { ep ->
                EpisodeUi(
                    number = ep,
                    title = "Episode $ep",
                    synopsis = "Episode $ep synopsis — will be fetched from Kitsu/Jikan in Phase 3.5.",
                    airDate = "${currentMedia?.year ?: 2024}",
                    thumbnailUrl = null,
                    duration = 24,
                    filler = false,
                )
            }
        }
    }
}

data class EpisodeUi(
    val number: Int,
    val title: String,
    val synopsis: String,
    val airDate: String,
    val thumbnailUrl: String?,
    val duration: Int,
    val filler: Boolean,
)
