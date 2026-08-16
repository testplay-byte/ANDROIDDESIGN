package com.confused.onlylist.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.confused.onlylist.AppContainer
import com.confused.onlylist.common.Logger
import com.confused.onlylist.network.anilist.AniListQueries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * ProfileViewModel — fetches real AniList Viewer data when authenticated.
 * Falls back to mock stats when not logged in.
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            if (!AppContainer.authManager.isLoggedIn) {
                Logger.d("Profile", "Not logged in — showing mock stats")
                _uiState.value = ProfileUiState.Mock
                return@launch
            }

            Logger.d("Profile", "Fetching Viewer data from AniList...")
            val result = AppContainer.anilistClient.query(AniListQueries.viewer)
            result.fold(
                onSuccess = { data ->
                    val viewer = data["Viewer"]?.jsonObject
                    if (viewer != null) {
                        val name = viewer["name"]?.jsonPrimitive?.content ?: "Unknown"
                        val avatar = viewer["avatar"]?.jsonObject?.get("large")?.jsonPrimitive?.content
                        val stats = viewer["statistics"]?.jsonObject
                        val animeStats = stats?.get("anime")?.jsonObject
                        val mangaStats = stats?.get("manga")?.jsonObject

                        val animeCount = animeStats?.get("count")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val episodesWatched = animeStats?.get("episodesWatched")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val minutesWatched = animeStats?.get("minutesWatched")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val animeMeanScore = animeStats?.get("meanScore")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0

                        val mangaCount = mangaStats?.get("count")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val chaptersRead = mangaStats?.get("chaptersRead")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                        Logger.i("Profile", "Viewer loaded: $name ($animeCount anime, $episodesWatched eps)")

                        _uiState.value = ProfileUiState.Loaded(
                            name = name,
                            avatarUrl = avatar,
                            animeCount = animeCount,
                            episodesWatched = episodesWatched,
                            minutesWatched = minutesWatched,
                            animeMeanScore = animeMeanScore,
                            mangaCount = mangaCount,
                            chaptersRead = chaptersRead,
                        )
                    } else {
                        Logger.w("Profile", "Viewer data was null")
                        _uiState.value = ProfileUiState.Mock
                    }
                },
                onFailure = { e ->
                    Logger.w("Profile", "Viewer fetch failed: ${e.message}")
                    _uiState.value = ProfileUiState.Mock
                },
            )
        }
    }
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object Mock : ProfileUiState
    data class Loaded(
        val name: String,
        val avatarUrl: String?,
        val animeCount: Int,
        val episodesWatched: Int,
        val minutesWatched: Int,
        val animeMeanScore: Double,
        val mangaCount: Int,
        val chaptersRead: Int,
    ) : ProfileUiState
}
