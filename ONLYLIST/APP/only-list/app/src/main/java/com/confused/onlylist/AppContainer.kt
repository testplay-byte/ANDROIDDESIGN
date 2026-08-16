package com.confused.onlylist

import android.content.Context
import com.confused.onlylist.data.repository.EpisodeMetadataRepository
import com.confused.onlylist.data.repository.MediaRepository
import com.confused.onlylist.database.DatabaseProvider
import com.confused.onlylist.network.anilist.AniListAuthManager
import com.confused.onlylist.network.anilist.AniListGraphQLClient
import com.confused.onlylist.network.jikan.JikanClient
import com.confused.onlylist.network.kitsu.KitsuClient

/**
 * Simple dependency container.
 * Phase 3.5 will replace this with Koin DI; for now it's a lazy-initialized object.
 * Initialized in OnlyListApplication.onCreate().
 */
object AppContainer {

    private lateinit var appContext: Context

    val database by lazy { DatabaseProvider.get(appContext) }
    val authManager by lazy { AniListAuthManager(appContext) }

    val anilistClient by lazy {
        AniListGraphQLClient(
            tokenProvider = { authManager.getToken() },
        )
    }

    val kitsuClient by lazy { KitsuClient() }
    val jikanClient by lazy { JikanClient() }

    val mediaRepository by lazy {
        MediaRepository(database.mediaDao(), anilistClient)
    }

    val episodeMetadataRepository by lazy {
        EpisodeMetadataRepository(
            episodeDao = database.episodeDao(),
            kitsuClient = kitsuClient,
            jikanClient = jikanClient,
        )
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

