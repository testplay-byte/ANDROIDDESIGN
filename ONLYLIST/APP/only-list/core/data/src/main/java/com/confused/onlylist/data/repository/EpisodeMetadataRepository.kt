package com.confused.onlylist.data.repository

import com.confused.onlylist.common.Logger
import com.confused.onlylist.database.dao.EpisodeDao
import com.confused.onlylist.database.entity.EpisodeEntity
import com.confused.onlylist.network.jikan.JikanClient
import com.confused.onlylist.network.kitsu.KitsuClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Episode metadata repository — merges Kitsu (primary) + Jikan (filler/dates).
 * Per CORE_RULES §15 + R-3 research:
 * - Kitsu: thumbnails, synopses, titles (en/jp), duration (primary).
 * - Jikan: air dates (TZ-aware), filler/recap flags (primary).
 * - Append-never-overwrite: existing episodes only update missing fields.
 *
 * ID mapping: AniList.idMal → Jikan (direct). AniList.id → Kitsu via mappings.
 */
class EpisodeMetadataRepository(
    private val episodeDao: EpisodeDao,
    private val kitsuClient: KitsuClient,
    private val jikanClient: JikanClient,
) {

    /**
     * Fetches + merges episode metadata for a media.
     * @param anilistId the AniList media ID
     * @param malId the MyAnimeList ID (from AniList Media.idMal) — needed for Jikan
     * @param episodeCount total episode count (from AniList) — generates episode numbers
     */
    suspend fun refreshEpisodes(anilistId: Int, malId: Int?, episodeCount: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Logger.d("EpisodeRepo", "Refreshing episodes for media $anilistId (malId=$malId, eps=$episodeCount)")

                // Fetch from both sources in parallel
                val kitsuDeferred = kotlinx.coroutines.async { fetchKitsuEpisodes(anilistId) }
                val jikanDeferred = kotlinx.coroutines.async { fetchJikanEpisodes(malId) }

                val kitsuEpisodes = kitsuDeferred.await()
                val jikanEpisodes = jikanDeferred.await()

                // Merge per episode number
                val mergedEpisodes = mutableMapOf<Int, EpisodeEntity>()
                for (epNum in 1..episodeCount.coerceAtMost(24)) {
                    val kitsu = kitsuEpisodes[epNum]
                    val jikan = jikanEpisodes[epNum]
                    val merged = mergeEpisode(anilistId, epNum, kitsu, jikan)
                    mergedEpisodes[epNum] = merged
                }

                episodeDao.upsertAll(mergedEpisodes.values.toList())
                Logger.i("EpisodeRepo", "Merged ${mergedEpisodes.size} episodes for media $anilistId")
                Result.success(Unit)
            } catch (e: Exception) {
                Logger.w("EpisodeRepo", "Episode refresh failed for $anilistId: ${e.message}")
                Result.failure(e)
            }
        }

    private suspend fun fetchKitsuEpisodes(anilistId: Int): Map<Int, KitsuEpisode> {
        // First: resolve the Kitsu ID via mappings
        val kitsuIdResult = kitsuClient.getKitsuIdForAnilistId(anilistId)
        val kitsuId = kitsuIdResult.getOrNull() ?: return emptyMap()
        if (kitsuId == 0) return emptyMap()

        val episodesResult = kitsuClient.getEpisodes(kitsuId)
        val episodesArray = episodesResult.getOrNull() ?: return emptyMap()

        return episodesArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val attrs = obj["attributes"]?.jsonObject ?: return@mapNotNull null
            val number = attrs["number"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
            val canonicalTitle = attrs["canonicalTitle"]?.jsonPrimitive?.contentOrNull()
            val synopsis = attrs["synopsis"]?.jsonPrimitive?.contentOrNull()
            val airdate = attrs["airdate"]?.jsonPrimitive?.contentOrNull()
            val length = attrs["length"]?.jsonPrimitive?.contentOrNull()?.toIntOrNull()
            val thumbnailObj = attrs["thumbnail"]?.jsonObject
            val thumbnailUrl = thumbnailObj?.get("original")?.jsonPrimitive?.contentOrNull()
            val titlesObj = attrs["titles"]?.jsonObject
            val titleJp = titlesObj?.get("ja_jp")?.jsonPrimitive?.contentOrNull()
            KitsuEpisode(
                number = number,
                canonicalTitle = canonicalTitle,
                synopsis = synopsis,
                airdate = airdate,
                length = length,
                thumbnailUrl = thumbnailUrl,
                titleJp = titleJp,
            )
        }.associateBy { it.number }
    }

    private suspend fun fetchJikanEpisodes(malId: Int?): Map<Int, JikanEpisode> {
        if (malId == null || malId == 0) return emptyMap()
        val episodesResult = jikanClient.getEpisodes(malId)
        val episodesArray = episodesResult.getOrNull() ?: return emptyMap()

        return episodesArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val malId = obj["mal_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.contentOrNull()
            val titleJp = obj["title_japanese"]?.jsonPrimitive?.contentOrNull()
            val aired = obj["aired"]?.jsonPrimitive?.contentOrNull()
            val filler = obj["filler"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            val recap = obj["recap"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            JikanEpisode(
                number = malId,
                title = title,
                titleJp = titleJp,
                aired = aired,
                filler = filler,
                recap = recap,
            )
        }.associateBy { it.number }
    }

    /**
     * Merge a single episode per the R-3 strategy.
     * Kitsu primary for: thumbnail, synopsis, titles, duration.
     * Jikan primary for: air date, filler, recap.
     */
    private fun mergeEpisode(
        anilistId: Int,
        episodeNumber: Int,
        kitsu: KitsuEpisode?,
        jikan: JikanEpisode?,
    ): EpisodeEntity = EpisodeEntity(
        mediaId = anilistId,
        episodeNumber = episodeNumber,
        titleEn = kitsu?.canonicalTitle ?: jikan?.title ?: "Episode $episodeNumber",
        titleJp = kitsu?.titleJp ?: jikan?.titleJp,
        synopsis = kitsu?.synopsis,
        airDate = jikan?.aired ?: kitsu?.airdate,
        thumbnailUrl = kitsu?.thumbnailUrl,
        durationMinutes = kitsu?.length,
        filler = jikan?.filler ?: false,
        recap = jikan?.recap ?: false,
        lastFetchedAt = System.currentTimeMillis(),
    )

    // ── Internal data classes for parsed source responses ──

    private data class KitsuEpisode(
        val number: Int,
        val canonicalTitle: String?,
        val synopsis: String?,
        val airdate: String?,
        val length: Int?,
        val thumbnailUrl: String?,
        val titleJp: String?,
    )

    private data class JikanEpisode(
        val number: Int,
        val title: String?,
        val titleJp: String?,
        val aired: String?,
        val filler: Boolean,
        val recap: Boolean,
    )

    // ── JsonPrimitive helper: contentOrNull returns null for JsonNull ──

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        if (this == kotlinx.serialization.json.JsonNull) null else content
}
