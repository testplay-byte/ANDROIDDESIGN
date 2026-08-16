# Knowledge: Kitsu + Jikan (Episode Metadata)

> Quick-reference. Full research: `research/R-3-kitsu-jikan.md` (41KB, verified live).

## Why we need them
AniList is weak on per-episode metadata (only `streamingEpisodes` — ~6 recent, no descriptions). For the Details screen we need per-episode thumbnails/titles/synopses/airdates/durations, so we merge **Kitsu (primary)** + **Jikan (filler/dates)** + AniList (count/next).

## Kitsu
- **Transport**: JSON:API over HTTPS.
- **Auth**: NOT required for SFW public read endpoints.
- **CDN**: `media.kitsu.app` (NOT `media.kitsu.io` — the .io URL 404s as of research).
- **Episode list endpoint**: `GET /api/edge/anime/{id}/episodes` — returns: `number`, `seasonNumber`, `canonicalTitle`, `titles{en_us, en_jp, ja_jp}`, `synopsis`, `airdate`, `length` (min), `thumbnail{original, ...}`.
  - **Kitsu has the BEST per-episode data**: thumbnails + synopses + titles (en/jp) + duration.
- **Single episode**: `GET /api/edge/episodes/{id}`.
- **ID system**: Kitsu's own IDs. Map via `GET /api/edge/mappings?filter[externalSite]=anilist/anime&filter[externalId]={anilistId}` → gives Kitsu id. (DIRECT path — verified.) Fallback via `myanimelist/anime` mapping.
- **Rate limit**: historically generous; verify current state at implementation time.

## Jikan (MyAnimeList unofficial REST API, v4)
- **Base URL**: `https://api.jikan.moe/v4`
- **Auth**: none (public, read-only).
- **Rate limit**: 3 req/sec, 60 req/min (free tier). Headers `X-RateLimit-Remaining` etc.
- **Episode list endpoint**: `GET /v4/anime/{id}/episodes` — returns ALL episodes in one page (no pagination). Fields: `mal_id`, `title`, `title_japanese`, `title_romanji`, `aired` (ISO8601 with TZ), `score` (0-5), `filler`, `recap`, `forum_url`. **NO synopsis, NO thumbnail, NO duration** in this endpoint.
- **Single episode**: `GET /v4/anime/{id}/episodes/{episode}` — HAS `synopsis` + `duration` (sec). **FRAGILE — 504'd ~100% during research.** Use only as lazy fallback when user expands an episode with no Kitsu synopsis; never block UI.
- **Videos endpoint**: `GET /v4/anime/{id}/videos` — sparse per-episode thumbnails (only episodes with MAL promo clips). Not reliable as primary.
- **ID system**: MAL IDs. Map AniList `Media.idMal` → Jikan directly.

## ID mapping (verified live)
```
AniList Media.id  ──idMal──▶  MAL id  ──direct──▶  Jikan anime id
        │
        └──Kitsu mappings?filter[externalSite]=anilist/anime──▶  Kitsu id
                (DIRECT path — verified)
                Fallback: AniList.idMal → Kitsu mappings?filter[externalSite]=myanimelist/anime
```
Cache `kitsuId` per `anilistMediaId` forever (it doesn't change).

## Merge strategy (per episode field)

| Field | Primary | Fallback | Notes |
|-------|---------|----------|-------|
| Thumbnail | Kitsu `thumbnail.original` | Jikan (sparse, from `/videos`) | Kitsu has the best thumbnails |
| Title (en) | Kitsu `canonicalTitle` | Jikan `title` | Fallback "Episode N" if both null |
| Title (jp) | Kitsu `titles.ja_jp` | Jikan `title_japanese` | |
| Air date | Jikan `aired` (TZ-aware) | Kitsu `airdate` | Jikan is TZ-aware, prefer it |
| Synopsis | Kitsu `synopsis` | Jikan `synopsis` (lazy, on-expand) | Jikan single-ep is fragile |
| Duration | Kitsu `length` (min) | Jikan `duration` (sec → min) | Convert units |
| Filler flag | Jikan `filler` | — | Jikan only |
| Recap flag | Jikan `recap` | — | Jikan only |
| Score | Jikan `score` | — | Jikan only |

## Failure handling
- Per (mediaId, source): track `lastFetchedAt`, `lastSuccessAt`, `failureCount`, `backoffUntil` in `metadata_source_state` (Room).
- On 404/empty/malformed: increment `failureCount`, set `backoffUntil` = exponential (**1m → 5m → 15m → 1h → 6h → 24h cap**), keep serving last-good cache.
- Jikan's scraper fragility was directly observed — design for it.

## Refresh cadence
- **Airing anime**: re-fetch episode metadata once per day per anime (or on user pull-to-refresh).
- **Completed anime**: fetch once, cache forever (refresh only on explicit user request).

## Other sources considered (brief)
- **AniDB** HTTP API — XML, no per-episode thumbnails via public API. Skip.
- **AnimeSchedule.net v3** — good for schedule/next-episode info, no per-episode metadata. Useful COMPLEMENT for the Airing screen (consider adding later).
- **LiveChart / HiAnime** — skip.

## Implementation note
All sources behind interfaces: `EpisodeMetadataSource` with `KitsuSource`, `JikanSource` impls. Merge logic in `EpisodeMetadataRepository`. Adding a future source (e.g. AnimeSchedule.net) = one-file change.
