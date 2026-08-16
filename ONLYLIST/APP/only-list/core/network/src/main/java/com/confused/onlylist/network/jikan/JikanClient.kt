package com.confused.onlylist.network.jikan

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
 * Jikan (MyAnimeList unofficial API v4) client.
 * Per R-3: Jikan is primary for per-episode air dates (TZ-aware) + filler/recap flags.
 * Per CORE_RULES §15: single-episode synopsis endpoint is fragile (504s) — lazy fallback only.
 *
 * v1: stub implementation. Phase 2.5 will fill in the full endpoints.
 */
class JikanClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@JikanClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    /**
     * Gets episodes for an anime by MAL ID.
     * Per R-3: `GET /v4/anime/{id}/episodes`
     * Returns filler, recap, aired (TZ-aware), title, title_japanese.
     * Returns ALL episodes in one page (no pagination needed).
     */
    suspend fun getEpisodes(malId: Int): Result<JsonArray> = withContext(Dispatchers.IO) {
        try {
            val response: JsonObject = client.get("$BASE_URL/anime/$malId/episodes").body()
            Result.success(response["data"] as? JsonArray ?: JsonArray(emptyList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() = client.close()

    companion object {
        private const val BASE_URL = "https://api.jikan.moe/v4"
    }
}
