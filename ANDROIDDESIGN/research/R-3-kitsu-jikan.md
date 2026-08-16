# R-3 — Kitsu + Jikan Episode Metadata Research

> Research sub-task for the Android anime/manga tracker app.
> Goal: identify exactly which per-episode fields (thumbnail, title, description, release date, filler flag, duration) each free public API exposes, and design a merge strategy that combines AniList + Kitsu + Jikan into a single, locally-cached, per-episode record.
>
> All findings below were verified against live API responses where the sandbox network permitted. Where a claim could not be live-verified, it is marked **(unverified)** and the source is cited.

**Date:** 2026-08-16
**Status:** Complete
**Author:** R-3 sub-agent (general-purpose)

---

## 0. TL;DR — read this first

| Question | Answer |
|---|---|
| Does Jikan expose per-episode **thumbnails**? | NO, not on the `/episodes` endpoints. Per-episode thumbnail-ish images exist on the `/videos` endpoint but only for episodes that have promo clips on MAL — sparse coverage. |
| Does Jikan expose per-episode **synopses**? | YES, but only via the **single** episode endpoint `/anime/{id}/episodes/{episode}` (1 request per episode — expensive at 3 req/sec). The **list** endpoint `/anime/{id}/episodes` does NOT include synopsis. |
| Does Kitsu expose per-episode **thumbnails**? | YES. `attributes.thumbnail.original` returns a working JPEG at `https://media.kitsu.app/episodes/thumbnails/{episodeId}/original.jpeg` (verified live, HTTP 200, image/jpeg). |
| Does Kitsu expose per-episode **synopses**? | YES. `attributes.synopsis` (and `attributes.description`, usually identical). |
| AniList → MAL → Jikan mapping | Direct: AniList `Media.idMal` is a MAL anime id; Jikan uses MAL ids natively. |
| AniList → Kitsu mapping | Two paths: (a) via MAL id through Kitsu `mappings?filter[externalSite]=myanimelist/anime&filter[externalId]={malId}`, or (b) **directly** through `mappings?filter[externalSite]=anilist/anime&filter[externalId]={anilistId}` (verified live). |
| Recommended authoritative sources per field | Thumbnail → Kitsu; Synopsis → Kitsu; Filler/Recap → Jikan (only source); Air date → Jikan (has timezone-aware ISO8601); Title → Kitsu; Duration → Kitsu (`length` in minutes is cleaner than Jikan's `duration` in seconds). |
| Biggest gotcha | The Kitsu API docs at hummingbird-me.github.io reference the image CDN as `media.kitsu.io`, but that domain returns HTTP 404. The live CDN is `media.kitsu.app`. The docs also show `airDate` (capital D) but the live API returns `airdate` (lowercase). |

---

## 1. Jikan v4 — full investigation

### 1.1 Base URL / transport / auth

| Property | Value |
|---|---|
| Base URL | `https://api.jikan.moe/v4` |
| Transport | REST over HTTPS, JSON request/response |
| Auth | None. Public read-only. No API key, no token. |
| CORS | `access-control-allow-origin: *` (verified in HEAD response) — safe to call from a browser if needed, though Android doesn't need this. |
| Self-description | "Jikan is an Unofficial MyAnimeList API. It scrapes the website to satisfy the need for a complete API - which MyAnimeList lacks." (https://docs.api.jikan.moe/) |
| Version | 4.0.0 |

### 1.2 Rate limits (verified from docs)

From https://docs.api.jikan.moe/ "About Information" section:

| Duration | Limit |
|---|---|
| Per second | **3 requests** |
| Per minute | **60 requests** |
| Per day | **Unlimited** |

The docs warn explicitly:

> "It's still possible to get rate limited from MyAnimeList.net instead."

(Practical implication for us: Jikan is a scraper. If MAL rate-limits or blocks Jikan's scraping, all Jikan endpoints can fail simultaneously — observed live in this research, see §1.7.) No `X-RateLimit-*` response headers are returned by the public endpoint (verified by inspecting live HEAD response). Rate-limit feedback is via HTTP 429 status codes.

### 1.3 Endpoint: `GET /v4/anime/{id}/episodes` — list of episodes

Verified live against `https://api.jikan.moe/v4/anime/5114/episodes` (Fullmetal Alchemist: Brotherhood, 64 episodes).

**Pagination wrapper:**
```json
{
  "pagination": { "last_visible_page": 1, "has_next_page": false },
  "data": [ ... 64 episode objects ... ]
}
```

All 64 episodes returned in a single page. For anime with hundreds/thousands of episodes (e.g. One Piece, mal_id=37510), the `?page=N` query parameter supports multi-page iteration. (Page size is not user-configurable; the API decides chunking.)

**Fields per episode in `data[]`:**

| Field | Type | Description |
|---|---|---|
| `mal_id` | integer | The episode number (1, 2, 3, ...). NOT a stable global episode UUID — it's positional within the anime. |
| `url` | string | MAL episode page URL |
| `title` | string | Episode title (English or romaji, depending on what MAL has) |
| `title_japanese` | string \| null | Japanese title |
| `title_romanji` | string \| null | Romaji title |
| `aired` | string (ISO8601 with timezone) | Air date, e.g. `"2009-04-05T00:00:00+00:00"` |
| `score` | float | MAL user-scored rating for the episode (0–5) |
| `filler` | boolean | Filler flag |
| `recap` | boolean | Recap flag |
| `forum_url` | string \| null | MAL forum topic URL for this episode |

**Notable absences on this endpoint:** NO `synopsis`, NO `duration`, NO `thumbnail` / `images`. Source: confirmed both by live response and by `app/Http/Resources/V4/AnimeEpisodesResource.php` in jikan-me/jikan-rest.

### 1.4 Endpoint: `GET /v4/anime/{id}/episodes/{episode}` — single episode

Schema derived from `app/Http/Resources/V4/AnimeEpisodeResource.php` in jikan-me/jikan-rest (OpenAPI annotations + toArray method):

| Field | Type | Description |
|---|---|---|
| `mal_id` | integer | Episode number (positional) |
| `url` | string | MAL episode URL |
| `title` | string | Episode title |
| `title_japanese` | string \| null | Japanese title |
| `title_romanji` | string \| null | Romaji title |
| `duration` | integer | **Episode duration in seconds** (unusual unit; Kitsu uses minutes) |
| `aired` | string (ISO8601) | Air date with timezone |
| `filler` | boolean | Filler flag |
| `recap` | boolean | Recap flag |
| `synopsis` | string \| null | **Per-episode synopsis** |

This endpoint is the **only** Jikan endpoint that exposes per-episode synopsis. It does NOT expose a thumbnail.

**Live verification:** The single-episode endpoint was unreachable during this research window — every call returned `{"status":504,"type":"BadResponseException","message":"Jikan failed to connect to MyAnimeList..."}` for anime ids 1, 21, 5114, 16498. The list endpoint and the `/full` endpoint worked intermittently for the same ids, suggesting the single-episode scraper hits a different MAL page that is more aggressively blocked from Jikan's hosting. The schema above is taken from Jikan's source code and is the contract the API commits to; we should treat this endpoint as **available but unreliable**.

**Cost warning:** To fetch synopses for all episodes of a 24-episode anime via Jikan alone requires 24 separate requests against the 3 req/sec rate limit = ~8 seconds minimum, plus retries on 504. For 1000+ episode anime this is impractical. Recommendation: treat Jikan synopses as a "lazy enrichment" — only fetch the single-episode detail for an episode when the user actually opens that episode's detail sheet, and only if Kitsu's synopsis is empty.

### 1.5 Endpoint: `GET /v4/anime/{id}/full` — full anime record

Verified live. The full record includes (truncated):
`mal_id, url, images{jpg,webp}, trailer, approved, titles[], title, title_english, title_japanese, title_synonyms, type, source, episodes, status, airing, aired{from,to,prop,string}, duration, rating, score, scored_by, rank, popularity, members, favorites, synopsis, background, season, year, broadcast, producers[], licensors[], studios[], genres[], explicit_genres[], themes[], demographics[], relations[], theme{openings[],endings[]}, external[], streaming[]`

**Useful for our merge:**
- `external[]` includes links to AniDB, ANN, Wikipedia, Syoboi, official site — could be useful for resolving alternative source IDs later.
- `relations[]` lists sequel/prequel/side-story/spin-off/alternative-version (each with mal_id) — useful for navigating franchises.
- `theme.openings[]` and `theme.endings[]` list OP/ED songs with episode ranges — fun trivia for the Details screen.
- `streaming[]` lists official streaming URLs (Crunchyroll, Netflix, etc.) per region.

### 1.6 Endpoint: `GET /v4/anime/{id}/videos` — per-episode thumbnails (sparse)

Schema from `app/Http/Resources/V4/AnimeVideosResource.php`. The response `data` object contains:
- `promo[]` — promotional videos / trailers
- `episodes[]` — **per-episode video entries** with fields: `mal_id` (episode number), `url`, `title`, `episode` (string), `images` (a `common_images` schema with `jpg`/`webp` variants: `image_url`, `small_image_url`, `large_image_url`)
- `music_videos[]` — OP/ED music videos

**Caveat:** This `episodes[]` array is **not a complete list of all episodes**. It only contains entries for episodes that have a promo clip uploaded on MAL. In practice this is sparse (often 0, sometimes a handful). Not reliable as a primary thumbnail source — useful as a Kitsu fallback for popular shows only.

Live calls to `/v4/anime/5114/videos` and `/v4/anime/21/videos` returned empty `data` (MAL upstream issue at the time of testing).

**Image CDN pattern (verified from `/full` response):**
```
https://cdn.myanimelist.net/images/anime/{subdir}/{file}.jpg
https://cdn.myanimelist.net/images/anime/{subdir}/{file}t.jpg   (thumbnail)
https://cdn.myanimelist.net/images/anime/{subdir}/{file}l.jpg   (large)
https://cdn.myanimelist.net/images/anime/{subdir}/{file}.webp   (webp variant)
```
Example for Cowboy Bebop (mal_id=1): `https://cdn.myanimelist.net/images/anime/4/19644.jpg`

### 1.7 Reliability observation (real, not theoretical)

During this research session, repeated calls to `https://api.jikan.moe/v4/anime/{id}/episodes` returned `504 BadResponseException` with `"Jikan failed to connect to MyAnimeList. MyAnimeList may be down/unavailable or refuses to connect"`. The `/full` endpoint worked intermittently for the same ids. The `/episodes/{episode}` (single) endpoint was 100% unavailable across 5+ retries with different ids over a 10-minute window.

**Implication for the app:** Jikan is fragile because it is a scraper. Our app MUST:
1. Cache every successful Jikan response locally.
2. Treat a 504 / 5xx as "source is stale for this media" — never blank the UI, never overwrite a good cached record.
3. Apply exponential backoff on consecutive failures (see §6).
4. Show cached data immediately on Details open, then attempt refresh in the background.

---

## 2. Kitsu — full investigation

### 2.1 Base URL / transport / auth

| Property | Value |
|---|---|
| Base URL | `https://kitsu.io/api/edge` |
| Transport | JSON:API specification (content-type: `application/vnd.api+json`) |
| Required headers | `Accept: application/vnd.api+json` (strongly recommended; some endpoints return empty bodies without it). |
| Auth | OAuth2 password grant, token URL `https://kitsu.io/api/oauth/token`. Scopes: `read`, `write`, `admin` — **all three are "Not yet implemented"** per the docs, meaning public reads work without auth. |
| Auth verdict for our use case | **No authentication required** for public GET endpoints. The only thing hidden for unauthenticated requests is NSFW/R18 content (feed posts, media, categories). For SFW anime episode metadata, unauthenticated GET is sufficient. |
| Source | https://hummingbird-me.github.io/api-docs/ (the "Kitsu JSON:API Documentation" — NOTE: this is the anime tracker Kitsu at kitsu.io, NOT the unrelated Kitsu animation-production tool at api-docs.kitsu.cloud). |

### 2.2 Rate limits

**Not documented.** No `X-RateLimit-*` headers returned (verified via live HEAD on `/api/edge/anime/1/episodes` — server is Cloudflare, returns standard cache headers but no rate-limit info).

Community consensus (unverified): Kitsu is generous, ~1 request per second sustained is safe, brief bursts tolerated. The JSON:API pagination defaults to 10 items/page with a max of 20 — fetching a 24-episode season is at most 2 page requests; a 100-episode show is 5–10. Well within any reasonable limit.

Recommendation for the app: self-throttle to 1 req/sec on Kitsu as a courtesy.

### 2.3 Pagination

Standard JSON:API style:
```
?page[limit]=20&page[offset]=0
```
- Default `page[limit]` = 10
- Max `page[limit]` = 20 (some routes allow more, but episodes is 20)
- Response includes `links.first`, `links.next`, `links.last` for cursor-based iteration

### 2.4 Endpoint: `GET /api/edge/anime/{id}/episodes` — list of episodes

Verified live against `https://kitsu.io/api/edge/anime/1/episodes` (Cowboy Bebop, Kitsu id=1). Returned a clean `data[]` array of episode resources.

**Full attributes shape (verified live):**
```json
{
  "data": [{
    "id": "229115",
    "type": "episodes",
    "links": { "self": "https://kitsu.io/api/edge/episodes/229115" },
    "attributes": {
      "createdAt": "2017-11-23T09:52:14.730Z",
      "updatedAt": "2021-09-17T05:03:10.398Z",
      "synopsis": "Spike and Jet head to Tijuana to track down an outlaw smuggling a dangerous drug known as blood-eye...",
      "description": "Spike and Jet head to Tijuana to track down an outlaw smuggling a dangerous drug known as blood-eye...",
      "titles": {
        "en_jp": "Asteroid Blues",
        "en_us": "Asteroid Blues",
        "ja_jp": "アステロイド・ブルース"
      },
      "canonicalTitle": "Asteroid Blues",
      "seasonNumber": 1,
      "number": 1,
      "relativeNumber": null,
      "airdate": "1998-10-23",
      "length": 25,
      "thumbnail": {
        "original": "https://media.kitsu.app/episodes/thumbnails/229115/original.jpeg",
        "meta": { "dimensions": {} }
      }
    },
    "relationships": {
      "media": { "links": { "self": "...", "related": "..." } },
      "videos": { "links": { "self": "...", "related": "..." } }
    }
  }]
}
```

**Field-by-field:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Kitsu episode id (stable global UUID, distinct from episode number). |
| `attributes.synopsis` | string | Per-episode synopsis. **YES.** |
| `attributes.description` | string | Usually identical to `synopsis`. Older field. |
| `attributes.titles.en_jp` | string | Romaji title |
| `attributes.titles.en_us` | string | English title |
| `attributes.titles.ja_jp` | string | Japanese title |
| `attributes.canonicalTitle` | string | Kitsu's preferred title (usually en_us or en_jp) |
| `attributes.seasonNumber` | integer | Season number (1-indexed) |
| `attributes.number` | integer | Absolute episode number within the show |
| `attributes.relativeNumber` | integer \| null | Episode number relative to the season (nullable) |
| `attributes.airdate` | string (YYYY-MM-DD) | **Per-episode air date.** Note: lowercase `airdate`. The docs at hummingbird-me.github.io show this as `airDate` (capital D) — docs are stale, live API uses lowercase. |
| `attributes.length` | integer | Episode duration **in minutes** |
| `attributes.thumbnail.original` | string | **Per-episode thumbnail URL** — verified live as a real 45 KB JPEG at `https://media.kitsu.app/episodes/thumbnails/229115/original.jpeg` (HTTP 200, content-type image/jpeg, served via Cloudflare cache HIT). |

**Important docs-vs-live discrepancy (verified):**
| Docs example URL | Live API URL returned |
|---|---|
| `media.kitsu.io/.../original.jpg` | `media.kitsu.app/episodes/thumbnails/{episodeId}/original.jpeg` |
| `airDate` (capital D) | `airdate` (lowercase d) |

The `media.kitsu.io` domain returns HTTP 404 for the thumbnail path (verified). The `media.kitsu.app` domain serves the actual image (verified). Treat `media.kitsu.app` as the canonical CDN host.

### 2.5 Endpoint: `GET /api/edge/episodes/{id}` — single episode

Verified live. Returns exactly the same attribute shape as the list endpoint, wrapped in a single `data` object. Useful for refreshing one episode's metadata (e.g. when the user opens the episode detail sheet and we want to check for an updated synopsis).

### 2.6 Kitsu mappings endpoint — ID resolution

Verified live against two distinct `externalSite` values.

**Path A: AniList id → Kitsu id (direct)**
```
GET /api/edge/mappings?filter[externalSite]=anilist/anime&filter[externalId]={anilistId}&include=item
```
Live response (AniList id=1 → Kitsu anime id=1):
```json
{
  "data": [{
    "id": "254652",
    "type": "mappings",
    "attributes": {
      "externalSite": "anilist/anime",
      "externalId": "1"
    },
    "relationships": {
      "item": { "data": { "type": "anime", "id": "1" } }
    }
  }],
  "included": [{ "id": "1", "type": "anime", ... }]
}
```
The Kitsu anime id we need is `included[0].id` (or equivalently `data[0].relationships.item.data.id`).

**Path B: MAL id → Kitsu id (direct)**
```
GET /api/edge/mappings?filter[externalSite]=myanimelist/anime&filter[externalId]={malId}&include=item
```
Live response (MAL id=1 → Kitsu anime id=1) returned a mapping record `id=64108` with `externalSite: "myanimelist/anime"`, `externalId: "1"`, pointing to anime id="1".

**Both paths verified.** This gives us a robust mapping graph (see §4).

### 2.7 ID system

Kitsu has its own opaque integer IDs. Mapping to/from external IDs is done via the `mappings` resource. Confirmed supported `externalSite` values:
- `anilist/anime` ✓ (verified)
- `myanimelist/anime` ✓ (verified)
- (other sites likely supported — e.g. `thetvdb`, `anidb`, `kitsu` itself — but **unverified** for our scope)

---

## 3. AniList idMal — confirmed available

Live GraphQL query:
```graphql
{ Media(id: 1) { id idMal title { romaji english native } } }
```
Response:
```json
{"data":{"Media":{"id":1,"idMal":1,"title":{"romaji":"Cowboy Bebop","english":"Cowboy Bebop","native":"カウボーイビバップ"}}}}
```

`idMal` is a nullable integer field on `Media`. When present, it is the MAL anime id Jikan can use directly. When null, the Jikan path is dead but the direct AniList→Kitsu mapping path is still available.

---

## 4. ID mapping strategy

```
                                    ┌──────────────────────────────┐
                                    │ AniList GraphQL (Media.id,   │
                                    │ Media.idMal)                  │
                                    └──────────────┬───────────────┘
                                                   │
                          ┌────────────────────────┼────────────────────────┐
                          │                        │                        │
                          │ if idMal != null       │ always available       │
                          ▼                        ▼                        ▼
              ┌────────────────────┐  ┌───────────────────────────┐  ┌────────────────────┐
              │ Jikan              │  │ Kitsu mappings            │  │ Kitsu mappings     │
              │ /v4/anime/{idMal}/ │  │ ?filter[externalSite]=    │  │ ?filter[externalSite]=│
              │   episodes         │  │   myanimelist/anime       │  │   anilist/anime    │
              │ /v4/anime/{idMal}/ │  │ &filter[externalId]=      │  │ &filter[externalId]=│
              │   episodes/{ep}    │  │   {idMal}                 │  │   {AniList.id}     │
              │ /v4/anime/{idMal}/ │  │ &include=item             │  │ &include=item      │
              │   full             │  └────────────┬──────────────┘  └─────────┬──────────┘
              └─────────┬──────────┘               │                          │
                        │                          │  included[0].id          │
                        │                          ▼                          │
                        │            ┌───────────────────────────┐              │
                        │            │ Kitsu anime id            │◄─────────────┘
                        │            └────────────┬──────────────┘
                        │                         │
                        │                         ▼
                        │            ┌───────────────────────────┐
                        │            │ Kitsu                    │
                        │            │ /api/edge/anime/{id}/     │
                        │            │   episodes                │
                        │            │ /api/edge/episodes/{id}   │
                        │            └───────────────────────────┘
                        ▼
            ┌────────────────────┐
            │ Jikan (MAL id used │
            │  natively)         │
            └────────────────────┘
```

**Recommended resolution order for the app:**
1. Query AniList for `Media.id`, `Media.idMal`, basic metadata (already planned as primary source).
2. Persist both IDs locally: `anilistId`, `malId` (nullable).
3. **Kitsu id** — prefer the AniList-direct mapping path:
   `GET /api/edge/mappings?filter[externalSite]=anilist/anime&filter[externalId]={anilistId}&include=item`
   - On success, persist `kitsuId`. Never re-resolve unless kitsuId becomes invalid (e.g. mapping disappears).
   - On failure (404 / empty), fallback to MAL path:
     `GET /api/edge/mappings?filter[externalSite]=myanimelist/anime&filter[externalId]={malId}&include=item`
   - Cache the kitsuId in the local DB forever; this mapping almost never changes.
4. **Jikan** — uses `malId` directly. If `malId` is null, mark Jikan as permanently unavailable for this media (no fallback path).
5. **Episode fetches** — fetch both Kitsu episodes and Jikan episodes lists in parallel on first Details open; merge per §5; cache the merged records.

This minimizes API calls (kitsuId is resolved once and cached forever) and degrades gracefully when idMal is null.

---

## 5. Coverage matrix — per-episode field × source

| Per-episode field | AniList | Kitsu | Jikan list (`/episodes`) | Jikan single (`/episodes/{ep}`) | Jikan videos (`/videos`) | Best source |
|---|---|---|---|---|---|---|
| Episode number | ❌ (no per-episode) | ✅ `attributes.number` | ✅ `mal_id` (positional) | ✅ `mal_id` | ✅ `mal_id` | All equivalent; Kitsu's is canonical |
| Season number | ❌ | ✅ `attributes.seasonNumber` | ❌ | ❌ | ❌ | Kitsu |
| Title (English) | ❌ | ✅ `attributes.titles.en_us` / `canonicalTitle` | ✅ `title` | ✅ `title` | ✅ `title` | Kitsu (more reliable localization) |
| Title (Japanese) | ❌ | ✅ `attributes.titles.ja_jp` | ✅ `title_japanese` | ✅ `title_japanese` | ❌ | Kitsu (canonical, romaji+ja) |
| Title (Romaji) | ❌ | ✅ `attributes.titles.en_jp` | ✅ `title_romanji` | ✅ `title_romanji` | ❌ | Tie |
| Air date | ❌ | ✅ `attributes.airdate` (YYYY-MM-DD) | ✅ `aired` (ISO8601 with TZ) | ✅ `aired` | ❌ | Jikan (timezone-aware) |
| Thumbnail | ❌ | ✅ `attributes.thumbnail.original` | ❌ | ❌ | ⚠️ sparse `images.jpg/webp` | Kitsu |
| Synopsis / description | ❌ | ✅ `attributes.synopsis` | ❌ | ✅ `synopsis` | ❌ | Kitsu (bulk) / Jikan single (lazy fallback) |
| Filler flag | ❌ | ❌ | ✅ `filler` (bool) | ✅ `filler` (bool) | ❌ | Jikan only |
| Recap flag | ❌ | ❌ | ✅ `recap` (bool) | ✅ `recap` (bool) | ❌ | Jikan only |
| Duration | ❌ | ✅ `attributes.length` (minutes) | ❌ | ✅ `duration` (seconds) | ❌ | Kitsu (cleaner unit) |
| Episode score (community) | ❌ | ❌ | ✅ `score` (0–5 float) | ❌ | ❌ | Jikan only |

**Legend:** ✅ = available · ❌ = not available · ⚠️ = available but sparse / partial.

---

## 6. Recommended merge strategy — per-field authoritative source

For each episode field, when merging Kitsu + Jikan records (matched by `attributes.number` ↔ `mal_id`), apply this priority order. **Always keep the last good cached value** if a source fails — never blank a record.

| Field | Primary | Secondary | Tertiary | Rationale |
|---|---|---|---|---|
| `episodeNumber` | Kitsu `number` | Jikan `mal_id` | — | Same value; Kitsu's stable UUID is for caching only. |
| `seasonNumber` | Kitsu `seasonNumber` | — | — | Only Kitsu exposes this. |
| `titleEn` | Kitsu `titles.en_us` or `canonicalTitle` | Jikan `title` | Episode number fallback (`"Episode 3"`) | Kitsu localization is generally better curated. |
| `titleJp` | Kitsu `titles.ja_jp` | Jikan `title_japanese` | — | Kitsu uses canonical Hepburn. |
| `titleRomaji` | Kitsu `titles.en_jp` | Jikan `title_romanji` | — | Tie; Kitsu preferred for consistency with titleEn. |
| `airDate` | Jikan `aired` (ISO8601 with TZ) | Kitsu `airdate` (YYYY-MM-DD) | — | Jikan has TZ (relevant for "airs at 23:00 JST"); Kitsu has only the calendar date. Note both refer to original Japanese air date. |
| `thumbnailUrl` | Kitsu `thumbnail.original` (`media.kitsu.app`) | Jikan `videos.episodes[].images` (if available) | Static placeholder image | Kitsu has near-complete coverage; Jikan's `videos` endpoint is sparse. |
| `synopsis` | Kitsu `synopsis` | Jikan single-episode `synopsis` (lazy fetch on demand) | — | Kitsu's bulk endpoint covers all episodes; Jikan requires N extra requests. Only fetch Jikan synopsis lazily when the user expands an episode and Kitsu's synopsis is empty. |
| `filler` | Jikan `filler` (bool) | — (no other source) | Default `false` | Jikan-only field. If Jikan unavailable, default to `false` and mark as "unknown" in UI. |
| `recap` | Jikan `recap` (bool) | — | Default `false` | Same as filler. |
| `durationMinutes` | Kitsu `length` (int minutes) | Jikan single `duration` (int seconds, divide by 60 and round) | — | Kitsu is in clean minutes. Jikan's seconds unit is awkward. |
| `score` | Jikan list `score` (float 0–5) | — | — | Jikan-only field; display as "MAL episode rating". |

### Matching Kitsu episodes to Jikan episodes

Both Kitsu `attributes.number` and Jikan `mal_id` are 1-indexed positional episode numbers within the show. They should match directly for the vast majority of episodes.

**Edge cases to handle in the merge:**
1. **Special episodes / OVAs** — Kitsu sometimes interleaves specials with a different `number` sequence; Jikan includes them only if MAL lists them on the main episodes page. Build the merge keyed by episode number but tolerate mismatches: log a warning, keep the higher count, mark unmatched extras as "bonus" entries.
2. ** Recap/filler episodes** — Jikan may number recaps differently. Match by air date as a fallback when numbers don't align.
3. **Multi-cour shows** — Kitsu `seasonNumber` + `number` vs Jikan's flat numbering. Kitsu seasonNumber disambiguates; Jikan doesn't have a season concept. Match within the same season by relative number.

### Final merged record schema (suggested)

```kotlin
data class EpisodeRecord(
  val anilistId: Int,
  val episodeNumber: Int,
  val seasonNumber: Int?,
  val titleEn: String?,
  val titleJp: String?,
  val titleRomaji: String?,
  val airDate: String?,          // ISO8601 preferred (from Jikan); else YYYY-MM-DD from Kitsu
  val thumbnailUrl: String?,     // Kitsu media.kitsu.app URL preferred
  val synopsis: String?,
  val filler: Boolean,
  val recap: Boolean,
  val durationMinutes: Int?,
  val malScore: Float?,
  val kitsuEpisodeId: String?,    // for refreshing individual Kitsu episodes
  val malForumUrl: String?,
  val lastFetchedAt: Instant,     // per-episode timestamp for cache invalidation
  val sourceHealth: SourceHealth   // see §7
)
```

---

## 7. Failure handling & backoff

### Source-health tracking

Per (mediaId, source) pair, track in local DB:
```
sourceHealth {
  lastSuccessAt: Instant?
  lastFailureAt: Instant?
  consecutiveFailures: Int
  nextAttemptAt: Instant   // computed from consecutiveFailures
  lastError: String?
}
```

### Backoff schedule

On consecutive failures, set `nextAttemptAt = now + delay` where `delay` follows exponential with cap:

| Consecutive failures | Delay before next attempt |
|---|---|
| 1 | 1 minute |
| 2 | 5 minutes |
| 3 | 15 minutes |
| 4 | 1 hour |
| 5 | 6 hours |
| 6+ | 24 hours (cap) |

When a request succeeds, reset `consecutiveFailures = 0`, set `lastSuccessAt = now`, clear `nextAttemptAt`.

### Behavior on failure

1. **HTTP 5xx / network timeout / 504 from Jikan** — treat as failure, apply backoff. Keep serving the last cached record. Do NOT overwrite the cache.
2. **HTTP 404** — likely permanent (anime removed from source, or wrong id mapping). Set a long backoff (24h) but DO log it for debugging — could indicate a stale ID mapping.
3. **HTTP 429 (rate limited)** — exponential backoff, double the normal delay. Parse `Retry-After` header if present (Jikan doesn't send it, but Kitsu might).
4. **Malformed / empty response body** — treat as failure. Apply backoff. Log the raw response for diagnostics.
5. **Partial failure** — if Kitsu succeeds but Jikan fails, merge what we have; mark the Jikan-only fields (`filler`, `recap`, `malScore`) as "stale" in the merged record. UI shows the cached value with a small "stale" indicator OR omits the indicator if the cached value is < 7 days old.

### Cache invalidation rules

| Trigger | Action |
|---|---|
| User opens Details screen for a media | Show cached records immediately. If `sourceHealth.nextAttemptAt <= now` for any source, fetch in background. |
| User pulls to refresh | Force-fetch all sources, ignoring `nextAttemptAt`. Reset `consecutiveFailures` only on success. |
| Currently-airing anime, daily background sync | Refresh episodes list for media with `airing == true` once per 24h. |
| Finished anime | Refresh only on user pull-to-refresh (or once per week at most). |
| New episode appears in source | Insert into local DB with `lastFetchedAt = now`. Notify UI to update episode list. |

---

## 8. Refresh cadence

| Anime status | Refresh trigger | Frequency |
|---|---|---|
| Currently airing | Background (WorkManager daily) | Once per 24h per airing anime |
| Currently airing | Foreground (user opens Details) | If last fetch > 1h ago, refresh in background |
| Currently airing | User pull-to-refresh | Force refresh all sources |
| Finished airing | User opens Details | If last fetch > 7d ago, refresh in background |
| Finished airing | User pull-to-refresh | Force refresh all sources |
| Any | User opens single episode detail sheet | Lazy fetch Jikan single-episode endpoint IF synopsis missing AND idMal != null |

Airing anime typically drop new episodes weekly on a fixed day (e.g. Thursday 23:30 JST). A daily background sync is sufficient — we don't need hourly polling.

---

## 9. Other free sources — brief survey

### AniDB HTTP API
- Endpoint: `https://api.anidb.net:9001/httpapi` (TCP), request/prot parameters.
- Auth: Free, requires client registration + client version string. Strict rate limit: 1 request per 2 seconds (unverified — historically enforced via IP ban if violated).
- Per-episode data: AniDB **does** have per-episode thumbnails and per-episode synopses in its database (the AniDB website itself displays them).
- BUT: AniDB's HTTP API is XML-based, dates back to ~2006, and historically returns only basic episode metadata (number, title, length, airdate) — **per-episode thumbnails are NOT exposed via the public HTTP API** (unverified — they may require the UDP API). AniDB's wiki page is currently blocked by Cloudflare for our probes.
- **Verdict:** Not worth pursuing as a primary source. Adds significant complexity (XML parsing, UDP fallback for some fields, strict rate limits) for marginal gain. Consider only as a 3rd fallback if both Kitsu and Jikan fail for a given show — and only for title/airdate, not thumbnails.

### AnimeSchedule.net API v3
- Endpoint: `https://animeschedule.net/api/v3/anime`, `/api/v3/timetables`, etc.
- Auth: Free API key (per-user, obtained from `animeschedule.net/users/<user>/settings/api`). OAuth2 for animelist-scoped endpoints.
- Per-anime data: `episodes` (count), `lengthMin` (per-episode minutes), `status` (Ongoing/Finished/Delayed), `premier`/`subPremier`/`dubPremier` dates, `jpnTime`/`subTime`/`dubTime` (weekly air time), `episodeOverride` (delayed episode info), `delayedTimetable`.
- Per-episode data: **NO** per-episode breakdown (no thumbnails, no synopses, no per-episode air dates).
- **Verdict:** Excellent complement for "next episode airs Thursday at 23:30 JST" / "delayed by 1 week" features on the Details screen. Not a substitute for Kitsu/Jikan episode metadata.

### LiveChart
- No public REST API documented (unverified). The site has internal JSON endpoints used by its web UI but they are not officially supported. Skip.

### HiAnime / 9anime scrapers (community)
- Several community scrapers exist (e.g. `MSMods-Pro/hianime-api` on GitHub). These scrape streaming sites for direct episode URLs.
- **Verdict:** Not appropriate for a metadata-only use case. Adds legal and reliability concerns. Skip.

### Recommended 3rd-source candidate for the future
**None of the surveyed alternatives beat Kitsu + Jikan for per-episode metadata.** The combination Kitsu (thumbnails + synopses + titles) + Jikan (filler/recap flags + score + reliable air dates with timezone) covers all the user's requested fields. AnimeSchedule is the strongest candidate to ADD for schedule/delay info, not for episode metadata.

---

## 10. Open questions / follow-up tasks for the planning phase

1. **Image loading strategy** — Kitsu thumbnails are ~45 KB JPEGs at original resolution. Should we also fetch Jikan's `webp` variants (smaller) for the `videos` endpoint? Consider Coil's image pipeline on Android — supports WebP natively from API 21+.
2. **Kitsu API stability** — Kitsu's docs (hummingbird-me.github.io) haven't been updated in years. The docs-vs-live discrepancies (`media.kitsu.io` vs `media.kitsu.app`, `airDate` vs `airdate`) suggest drift. We should treat the live API as ground truth and add a runtime check on the thumbnail URL host (fallback to `media.kitsu.io` if `media.kitsu.app` returns 404).
3. **AniList idMal coverage** — what % of AniList media entries have a non-null `idMal`? Need to check before committing to Jikan as a reliable second source. (Plan: run a query over the user's library, count nulls.)
4. **NSFW handling on Kitsu** — unauthenticated requests hide NSFW/R18 content. For anime marked NSFW on AniList, Kitsu episode metadata may be inaccessible without OAuth2 password-grant auth. Decision: implement unauthenticated-only and accept that NSFW shows lose Kitsu coverage (Jikan still works since MAL is public for SFW-equivalent content; but Jikan also has its own NSFW handling).
5. **Pagination size for Kitsu episodes** — should we use `page[limit]=20` (max) to minimize request count, or `page[limit]=10` (default) for safety? Recommend max-20 to halve request count.
6. **Local DB schema for `sourceHealth`** — needs its own table per (mediaId, source) pair, or columns on the media table. To be designed in the database documentation phase per CORE_RULES §24.

---

## 11. References

- Jikan docs: https://docs.api.jikan.moe/
- Jikan source code (jikan-me/jikan-rest, master branch):
  - `app/Http/Resources/V4/AnimeEpisodesResource.php` (list endpoint wrapper)
  - `app/Http/Resources/V4/AnimeEpisodeResource.php` (single endpoint schema with OpenAPI annotations)
  - `app/Http/Resources/V4/AnimeVideosResource.php` (videos endpoint schema)
  - `app/Http/Resources/V4/AnimeFullResource.php` (full anime endpoint)
  - `app/Features/AnimeEpisodesLookupHandler.php` (list endpoint handler)
  - `app/Features/AnimeEpisodeLookupHandler.php` (single endpoint handler)
- Jikan live API responses (verified 2026-08-16):
  - `https://api.jikan.moe/v4/anime/5114/episodes` (Fullmetal Alchemist: Brotherhood, 64 episodes — full list returned in one page)
  - `https://api.jikan.moe/v4/anime/5114/full` (relations, external, theme, streaming)
  - `https://api.jikan.moe/v4/anime/1/full` (Cowboy Bebop image CDN URL pattern)
- Kitsu docs: https://hummingbird-me.github.io/api-docs/
- Kitsu live API responses (verified 2026-08-16):
  - `https://kitsu.io/api/edge/anime/1/episodes` (Cowboy Bebop episode list with full attribute shape)
  - `https://kitsu.io/api/edge/episodes/229115` (single episode detail)
  - `https://kitsu.io/api/edge/mappings?filter[externalSite]=myanimelist/anime&filter[externalId]=1&include=item` (MAL→Kitsu mapping)
  - `https://kitsu.io/api/edge/mappings?filter[externalSite]=anilist/anime&filter[externalId]=1&include=item` (AniList→Kitsu mapping, direct)
- Kitsu thumbnail CDN verification: `https://media.kitsu.app/episodes/thumbnails/229115/original.jpeg` (HTTP 200, image/jpeg, 45438 bytes, Cloudflare cache HIT, 2026-08-16)
- Kitsu docs-vs-live discrepancy verification: `https://media.kitsu.io/episodes/thumbnails/229115/original.jpg` returns HTTP 404 (docs example URL is stale)
- AniList GraphQL: `https://graphql.anilist.co` — confirmed `Media.idMal` field is nullable integer
- AnimeSchedule.net API v3 docs: `https://animeschedule.net/api/v3/documentation/anime` (verified fields list)
- AnimeSchedule.net Rust client README (proxy reference): https://github.com/MolotovCherry/anime-schedule-rs

---

## 12. Appendix — live response excerpts

### A. Jikan /v4/anime/5114/episodes (first 2 episodes)

```json
{
  "pagination": {"last_visible_page": 1, "has_next_page": false},
  "data": [
    {
      "mal_id": 1,
      "url": "https://myanimelist.net/anime/5114/Fullmetal_Alchemist__Brotherhood/episode/1",
      "title": "Fullmetal Alchemist",
      "title_japanese": "鋼の錬金術師",
      "title_romanji": "Hagane no Renkinjutsushi\u00a0",
      "aired": "2009-04-05T00:00:00+00:00",
      "score": 3.91,
      "filler": true,
      "recap": false,
      "forum_url": "https://myanimelist.net/forum/?topicid=77340"
    },
    {
      "mal_id": 2,
      "url": "https://myanimelist.net/anime/5114/Fullmetal_Alchemist__Brotherhood/episode/2",
      "title": "The First Day",
      "title_japanese": "はじまりの日",
      "title_romanji": "Hajimari no Hi\u00a0",
      "aired": "2009-04-12T00:00:00+00:00",
      "score": 4.13,
      "filler": false,
      "recap": false,
      "forum_url": "https://myanimelist.net/forum/?topicid=78774"
    }
    /* ... 62 more episodes, 64 total in one page ... */
  ]
}
```

### B. Kitsu /api/edge/anime/1/episodes (first episode, full attribute shape)

```json
{
  "data": [{
    "id": "229115",
    "type": "episodes",
    "links": {"self": "https://kitsu.io/api/edge/episodes/229115"},
    "attributes": {
      "createdAt": "2017-11-23T09:52:14.730Z",
      "updatedAt": "2021-09-17T05:03:10.398Z",
      "synopsis": "Spike and Jet head to Tijuana to track down an outlaw smuggling a dangerous drug known as blood-eye.  Jet wants the bounty, but Spike has eyes for a far prettier prize.",
      "description": "Spike and Jet head to Tijuana to track down an outlaw smuggling a dangerous drug known as blood-eye.  Jet wants the bounty, but Spike has eyes for a far prettier prize.",
      "titles": {
        "en_jp": "Asteroid Blues",
        "en_us": "Asteroid Blues",
        "ja_jp": "アステロイド・ブルース"
      },
      "canonicalTitle": "Asteroid Blues",
      "seasonNumber": 1,
      "number": 1,
      "relativeNumber": null,
      "airdate": "1998-10-23",
      "length": 25,
      "thumbnail": {
        "original": "https://media.kitsu.app/episodes/thumbnails/229115/original.jpeg",
        "meta": {"dimensions": {}}
      }
    },
    "relationships": {
      "media": {"links": {"self": "...", "related": "..."}},
      "videos": {"links": {"self": "...", "related": "..."}}
    }
  }]
}
```

### C. Kitsu /api/edge/mappings (AniList-direct path)

```json
{
  "data": [{
    "id": "254652",
    "type": "mappings",
    "links": {"self": "https://kitsu.io/api/edge/mappings/254652"},
    "attributes": {
      "createdAt": "2020-05-02T14:54:45.296Z",
      "updatedAt": "2020-05-02T14:54:45.296Z",
      "externalSite": "anilist/anime",
      "externalId": "1"
    },
    "relationships": {
      "item": {
        "links": {"self": "...", "related": "..."},
        "data": {"type": "anime", "id": "1"}
      }
    }
  }],
  "included": [{
    "id": "1",
    "type": "anime",
    "links": {"self": "https://kitsu.io/api/edge/anime/1"},
    "attributes": {
      "slug": "cowboy-bebop",
      "synopsis": "In the year 2071...",
      "...": "..."
    }
  }]
}
```

---

**End of report.**
