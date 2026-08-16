package com.confused.onlylist.data.repository

import com.confused.onlylist.database.dao.MediaDao
import com.confused.onlylist.database.entity.MediaEntity
import com.confused.onlylist.network.anilist.AniListGraphQLClient
import com.confused.onlylist.network.anilist.AniListQueries
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Media repository — offline-first.
 * Per CORE_RULES §14: UI reads from Room first, network refreshes.
 * Returns Flow<List<MediaEntity>> that emits Room data + triggers a background refresh.
 */
class MediaRepository(
    private val mediaDao: MediaDao,
    private val anilistClient: AniListGraphQLClient,
) {

    // ── Trending ──

    fun getTrending(type: String = "ANIME", limit: Int = 20): Flow<List<MediaEntity>> {
        return mediaDao.getTrending(type, limit)
    }

    // ── Airing (status = RELEASING) ──

    fun getAiring(type: String = "ANIME", limit: Int = 20): Flow<List<MediaEntity>> {
        return mediaDao.getAiring(type, limit)
    }

    suspend fun refreshTrending(type: String = "ANIME", page: Int = 1, perPage: Int = 20): Result<Unit> {
        val result = anilistClient.query(
            query = AniListQueries.trending,
            variables = mapOf("page" to page, "perPage" to perPage, "type" to type),
        )
        return result.fold(
            onSuccess = { data ->
                val mediaList = parseMediaList(data)
                mediaDao.upsertAll(mediaList)
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    // ── Search ──

    fun search(query: String, type: String = "ANIME", limit: Int = 50): Flow<List<MediaEntity>> {
        return mediaDao.search(query, type, limit)
    }

    suspend fun refreshSearch(query: String, type: String = "ANIME", page: Int = 1, perPage: Int = 20): Result<Unit> {
        val result = anilistClient.query(
            query = AniListQueries.search,
            variables = mapOf("page" to page, "perPage" to perPage, "search" to query, "type" to type),
        )
        return result.fold(
            onSuccess = { data ->
                val mediaList = parseMediaList(data)
                if (mediaList.isNotEmpty()) mediaDao.upsertAll(mediaList)
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    // ── By ID ──

    fun getById(id: Int): Flow<MediaEntity?> = mediaDao.getById(id)

    suspend fun refreshById(id: Int): Result<Unit> {
        val result = anilistClient.query(
            query = AniListQueries.mediaById,
            variables = mapOf("id" to id),
        )
        return result.fold(
            onSuccess = { data ->
                val media = data["Media"]?.let { parseMedia(it.jsonObject) }
                if (media != null) mediaDao.upsert(media)
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    // ── JSON parsing: AniList response → MediaEntity ──

    private fun parseMediaList(data: JsonObject): List<MediaEntity> {
        val page = data["Page"]?.jsonObject ?: return emptyList()
        val mediaArray = page["media"]?.jsonArray ?: return emptyList()
        return mediaArray.mapNotNull { element -> parseMedia(element.jsonObject) }
    }

    private fun parseMedia(json: JsonObject): MediaEntity {
        val title = json.objOrNull("title")
        val coverImage = json.objOrNull("coverImage")
        val nextAiring = json.objOrNull("nextAiringEpisode")
        val genresArray = json.arrayOrNull("genres")

        return MediaEntity(
            id = json.int("id") ?: 0,
            idMal = json.int("idMal"),
            type = json.string("type") ?: "ANIME",
            titleRomaji = title?.string("romaji"),
            titleEnglish = title?.string("english"),
            titleNative = title?.string("native"),
            coverImageLarge = coverImage?.string("large"),
            coverImageColor = coverImage?.string("color"),
            bannerImage = json.string("bannerImage"),
            episodes = json.int("episodes"),
            chapters = json.int("chapters"),
            duration = json.int("duration"),
            status = json.string("status"),
            season = json.string("season"),
            seasonYear = json.int("seasonYear"),
            format = json.string("format"),
            source = json.string("source"),
            averageScore = json.int("averageScore"),
            meanScore = json.int("meanScore"),
            popularity = json.int("popularity"),
            favourites = json.int("favourites"),
            genres = genresArray?.mapNotNull { it.stringOrNull() }?.toString() ?: "[]",
            description = json.string("description"),
            nextAiringEpisode = nextAiring?.int("episode"),
            nextAiringAt = nextAiring?.long("airingAt"),
            updatedAt = json.long("updatedAt") ?: 0L,
            lastFetchedAt = System.currentTimeMillis(),
        )
    }

    // ── JSON helpers ──

    private fun JsonObject.string(key: String): String? = this[key].stringOrNull()
    private fun JsonObject.int(key: String): Int? = this[key].stringOrNull()?.toIntOrNull()
    private fun JsonObject.long(key: String): Long? = this[key].stringOrNull()?.toLongOrNull()
    private fun JsonObject.objOrNull(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.arrayOrNull(key: String): JsonArray? = this[key] as? JsonArray

    private fun JsonElement?.stringOrNull(): String? = when (this) {
        null, JsonNull -> null
        is JsonPrimitive -> if (isString) content else content
        else -> null
    }
}
