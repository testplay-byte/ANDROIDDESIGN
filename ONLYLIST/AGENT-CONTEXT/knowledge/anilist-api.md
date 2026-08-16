# Knowledge: AniList API

> Quick-reference. Full research: `research/R-2-anilist-api.md` (60KB, verified live).

## Endpoint
- `https://graphql.anilist.co` — **POST-only** (GET 404s, PUT 405s).
- No HTTP-layer caching possible. MUST cache in Room.

## Auth (D-017)
- OAuth2 **Implicit Grant** + custom-scheme redirect (`<applicationId>://anilist-auth`).
- AniList does NOT support PKCE, does NOT issue refresh tokens.
- Tokens are 1-year JWTs. JWT `sub` claim = user ID (decodeable client-side, skips a `Viewer` round-trip).
- Auth URL: `https://anilist.co/api/v2/oauth/authorize`
- Token URL: `https://anilist.co/api/v2/oauth/token` (not needed for Implicit Grant)
- Header: `Authorization: Bearer <token>`
- **No client secret shipped in the APK** (that's why Implicit Grant, not Authorization Code).

## Rate limit (CRITICAL)
- Documented 90 req/min, **CURRENTLY DEGRADED to 30 req/min** (verified: `X-RateLimit-Limit: 30`).
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After` (sec), `X-RateLimit-Reset` (Unix).
- 429 returns `{"message":"Too Many Requests.","status":429}`.
- Strategy: single-flight queue per source, sliding 60s window, respect `Retry-After`, dedupe in-flight.

## Pagination
- `Page(page, perPage)` — offset-based, **max perPage = 50** (silently capped).
- `PageInfo.total`/`lastPage` are inaccurate — only trust `hasNextPage`.
- `MediaListCollection` uses chunked pagination (`chunk`, `perChunk`, `hasNextChunk`).

## No subscriptions / no push
- `__schema.subscriptionType` is `null`. No WebSocket.
- Must poll. Use `Viewer.unreadNotificationCount` as cheap pre-check before fetching `Page.notifications`.

## Core queries (give actual GraphQL operation + key fields)
- `Viewer` — id, name, avatar {medium large}, bannerImage, options {displayAdultContent, ...}, mediaListOptions {scoreFormat, ...}, statistics {anime {count, episodesWatched, minutesWatched, ...}, manga {...}}.
- `MediaListCollection(userId, type ANIME|MANGA, chunk, perChunk)` — lists grouped by status (CURRENT, COMPLETED, PAUSED, DROPPED, PLANNING, REPEATING). Entries: id, status, score, progress, progressVolumes, repeat, notes, priority, private, hiddenFromStatusLists, customLists, startedAt, completedAt, updatedAt, createdAt, media {nested Media}.
- `Media(id, type)` — id, idMal, title {romaji, english, native}, coverImage {large, extraLarge, color}, bannerImage, episodes, duration, status, season, seasonYear, format, source, genres, tags, averageScore, meanScore, popularity, favourites, nextAiringEpisode {airingAt, episode, timeUntilAiring}, airingSchedule {nodes {airingAt, episode}}, relations, characters, studios, description(asHtml), synonyms, trailer, externalLinks, streamingEpisodes, updatedAt.
- `Page(page, perPage, search, type, season, seasonYear, format, status, genre_in, sort)` — search + filter. Returns `media: [Media]` + `pageInfo {hasNextPage}`.
- `AiringSchedule(mediaId)` — airing schedule for a media.
- `MediaTrend(mediaId, sort)` — trending history (home screen "trending now").
- `Notification(resetNotificationCount, page, type)` — UNION type with 20 subtypes (AiringNotification, FollowingNotification, etc.). Requires auth.

## ID mapping
- `Media.idMal` — MAL ID (nullable int). → Jikan directly.
- **No Kitsu id field** — map via Kitsu `mappings?filter[externalSite]=anilist/anime` (see kitsu-jikan-api.md).

## Episodes (AniList is weak here — confirms need for Kitsu+Jikan)
- `Media.streamingEpisodes` — only ~6 most recent, Crunchyroll-sourced, NO descriptions.
- `Media.episodes` — count only.
- `Media.nextAiringEpisode` — next airing info.
- No per-episode thumbnails/titles/synopses.

## Mutations (verified)
- `SaveMediaListEntry` (id-less=create, with-id=update): mediaId, status, score, progress, progressVolumes, repeat, notes, priority, private, hiddenFromStatusLists, customLists, startedAt, completedAt.
- `DeleteMediaListEntry(id)`.
- `UpdateMediaListEntries(ids, status, score, ...)` — bulk.
- `UpdateUser` — toggles options (displayAdultContent, scoreFormat, ...).

## Image CDN
- `https://s4.anilist.co/file/anilistcdn/...` (Cloudflare-fronted, no documented rate limit).
- `MediaCoverImage.color` — hex tint per media. Use for placeholder color while image loads.

## Errors
- GraphQL `errors[]` array: `message`, `status` (HTTP-style int inside JSON), `locations`, optional `validation` map.
- Even HTTP 200 can carry errors — always check `errors[]` in the response.

## Cache anchors (for reconciliation)
- `Media.updatedAt` — remote changed → re-cache.
- `MediaList.updatedAt` / `createdAt` — LWW for list entries.
- `User.unreadNotificationCount` — cheap pre-check for polling.

## Recommendations (in the Android app)
- Auth: Implicit Grant + custom scheme. Store JWT in Android Keystore.
- Cache: Room entities for Media, Episode, MediaListEntry, AiringSchedule, User, Character, Studio + `metadata_source_state`.
- Polling cadence: airing schedule daily, notifications on-app-open + daily, trends weekly, lists on-pull-to-refresh.
- Rate-limit: single-flight queue, sliding 60s window, respect `Retry-After`, dedupe in-flight. Design for 30/min.
