package com.confused.onlylist.network.kitsu

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Kitsu API client — fetches per-episode metadata (thumbnails, synopses, titles).
 * Per R-3: Kitsu is primary for thumbnails/synopses. CDN is media.kitsu.app (NOT .io).
 * Per CORE_RULES §15: all sources behind interfaces, merge in repository.
 *
 * v1: stub implementation. Phase 2.5 will fill in the full endpoints.
 */
class KitsuClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@KitsuClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    /**
     * Gets the Kitsu anime ID for an AniList media ID.
     * Per R-3: `GET /api/edge/mappings?filter[externalSite]=anilist/anime&filter[externalId]={anilistId}`
     */
    suspend fun getKitsuIdForAnilistId(anilistId: Int): Result<Int?> = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.get("$BASE_URL/mappings") {
                url {
                    parameters.append("filter[externalSite]", "anilist/anime")
                    parameters.append("filter[externalId]", anilistId.toString())
                }
            }.body()
            val data = response["data"] as? JsonArray
            val firstItem = data?.firstOrNull() as? JsonObject
            val attributes = firstItem?.get("attributes") as? JsonObject
            val externalId = attributes?.get("externalId")?.toString()?.toIntOrNull()
            Result.success(externalId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets episodes for a Kitsu anime ID.
     * Per R-3: `GET /api/edge/anime/{id}/episodes`
     * Returns thumbnails, synopses, titles, durations.
     */
    suspend fun getEpisodes(kitsuId: Int): Result<JsonArray> = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.get("$BASE_URL/anime/$kitsuId/episodes").body()
            Result.success(response["data"] as? JsonArray ?: JsonArray(emptyList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() = client.close()

    companion object {
        // Per R-3: the CDN is media.kitsu.app (NOT media.kitsu.io)
        private const val BASE_URL = "https://kitsu.app/api/edge"
    }
}
