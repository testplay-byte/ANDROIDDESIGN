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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * DetailsViewModel — loads a single media by ID (offline-first) + fetches
 * real episode metadata from Kitsu + Jikan (per CORE_RULES §15 + R-3 research).
 */
class DetailsViewModel(private val mediaId: Int) : ViewModel() {

    private val repository = AppContainer.mediaRepository
    private val episodeRepo = AppContainer.episodeMetadataRepository

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
                onSuccess = {
                    Logger.d("Details", "Media #$mediaId refresh OK")
                    fetchEpisodes()
                },
                onFailure = { Logger.w("Details", "Media #$mediaId refresh failed: ${it.message}") },
            )
        }
    }

    private fun fetchEpisodes() {
        viewModelScope.launch {
            // Read the media entity from Room (one-shot) to get idMal + episode count
            val mediaEntity = repository.getById(mediaId).first()
            val malId = mediaEntity?.idMal
            val episodeCount = mediaEntity?.episodes ?: 12

            Logger.d("Details", "Fetching episodes for media #$mediaId (malId=$malId, eps=$episodeCount)")
            val epResult = episodeRepo.refreshEpisodes(
                anilistId = mediaId,
                malId = malId,
                episodeCount = episodeCount,
            )
            epResult.fold(
                onSuccess = { Logger.d("Details", "Episodes fetched OK") },
                onFailure = { Logger.w("Details", "Episodes fetch failed: ${it.message}") },
            )

            // Load the merged episodes from Room
            val episodeEntities = AppContainer.database.episodeDao().getByMediaId(mediaId).first()
            _episodes.value = episodeEntities.map { entity ->
                EpisodeUi(
                    number = entity.episodeNumber,
                    title = entity.titleEn ?: "Episode ${entity.episodeNumber}",
                    synopsis = entity.synopsis ?: "No synopsis available.",
                    airDate = entity.airDate ?: "",
                    thumbnailUrl = entity.thumbnailUrl,
                    duration = entity.durationMinutes ?: 0,
                    filler = entity.filler,
                )
            }

            // If Room is empty (all sources failed), generate placeholder episodes
            if (_episodes.value.isEmpty()) {
                _episodes.value = (1..episodeCount.coerceAtMost(24)).map { ep ->
                    EpisodeUi(
                        number = ep,
                        title = "Episode $ep",
                        synopsis = "Episode $ep synopsis (metadata unavailable — sources may be offline).",
                        airDate = "",
                        thumbnailUrl = null,
                        duration = 0,
                        filler = false,
                    )
                }
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
