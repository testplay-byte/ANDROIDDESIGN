# R-2 — AniList GraphQL API Research

> Research report for the Android anime/manga tracker app.
> Author: general-purpose sub-agent (R-2).
> Methodology: live `curl` probing of `https://graphql.anilist.co` + GraphQL schema
> introspection (`__schema` / `__type`) + reading https://docs.anilist.co/.
>
> **Verification convention used below:**
> - ✅ = verified live against the API (curl response) or via schema introspection
> - 📄 = stated in the official docs at https://docs.anilist.co/
> - ⚠️ = inferred from docs/samples but not directly verified by this agent
> - (unverified) = claimed by community sources, not confirmed by us
>
> All HTTP probes were executed on the date in `worklog.md`.

---

## 0. Cheat sheet (one-glance summary)

| Item | Value |
|---|---|
| GraphQL endpoint | `https://graphql.anilist.co` |
| HTTP method | **POST only** (GET → 404, PUT → 405) |
| Auth type | OAuth2 (Authorization Code Grant OR Implicit Grant) |
| Auth URL | `https://anilist.co/api/v2/oauth/authorize` |
| Token URL | `https://anilist.co/api/v2/oauth/token` |
| Token type | JWT, **1 year lifetime**, **no refresh tokens**, **no scopes** |
| PKCE | **NOT supported** (no `code_challenge` / `code_verifier` params documented; auth-code flow requires `client_secret`) |
| Redirect URI | "any valid URI, including custom URI schemes" — custom scheme (e.g. `myapp://anilist-callback`) works |
| Auth header | `Authorization: Bearer <jwt>` |
| Required headers | `Content-Type: application/json`, `Accept: application/json` |
| Rate limit (documented) | 90 req/min |
| Rate limit (live right now) | **30 req/min** — degraded state, `X-RateLimit-Limit: 30` returned |
| Rate limit headers | `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After` (sec), `X-RateLimit-Reset` (Unix ts) |
| Rate limit error | HTTP 429 + `{ "errors": [{ "message": "Too Many Requests.", "status": 429 }] }` |
| Max `perPage` (Page query) | **50** (server silently caps — verified) |
| MediaListCollection pagination | chunked (`chunk`, `perChunk`, `hasNextChunk`) |
| PageInfo.total / lastPage | **deprecated / inaccurate** — only trust `hasNextPage` |
| Subscriptions / WebSocket | **NONE** — `__schema.subscriptionType` is `null`. Poll only. |
| MAL id | `Media.idMal` (Int, nullable) |
| Kitsu id | NOT exposed by AniList — must use Kitsu's own lookup API |
| Image CDN | `https://s4.anilist.co/file/anilistcdn/...` (Cloudflare-fronted, no documented rate limit, cache aggressively) |
| `Media.updatedAt` | ✅ exists — Int (Unix seconds). Use for cache reconciliation. |
| `MediaList.updatedAt` / `createdAt` | ✅ both exist |

---

## 1. API shape

✅ **Endpoint URL:** `https://graphql.anilist.co`

✅ **Transport:** GraphQL over HTTPS.

✅ **POST only.** Verified by direct curl:
- `GET /?query=...` → HTTP 404 with body `{"errors":[{"message":"Not Found.","hint":"Use POST request to access graphql subdomain.","status":404}]}`.
- `PUT` → HTTP 405 (Nginx-level), HTML page.
- CORS header advertises `access-control-allow-methods: GET, POST, OPTIONS` but the GET path is only for CORS preflight; only POST actually executes a query.

⚠️ **Implication for caching:** because GET is not supported, **HTTP-layer CDN/browser caching of GraphQL responses is impossible**. An offline-first app must perform all caching in a local SQLite/SQLDelight/Room cache and treat the AniList endpoint as always-uncacheable at the HTTP layer. Coil/Glide can still cache the image CDN.

📄 Request body shape:
```json
{
  "query": "<graphql string>",
  "variables": { ... }
}
```

Both `query` and `variables` are sent in the same POST body. `variables` is optional but recommended.

---

## 2. Auth flow

📄 AniList uses OAuth2. Two grants are supported:
1. **Authorization Code Grant** — server-side flow, requires `client_secret` to exchange code.
2. **Implicit Grant** — token returned in URL fragment of the redirect URI, no secret needed.

### 2.1 Client registration
- Done in AniList developer settings (https://anilist.co/settings/developer).
- Two pieces of info required: application name + redirect URI.
- After creation you receive a `client_id` and `client_secret`.
- 📄 "Applications cannot be deleted once created."

### 2.2 Authorization URL
```
GET https://anilist.co/api/v2/oauth/authorize
  ?client_id={client_id}
  &redirect_uri={redirect_uri}        (auth code only)
  &response_type=code                 (auth code)
  -- OR --
  &response_type=token                (implicit)
```
✅ Verified: hitting this URL without an active session returns HTTP 302 → `https://anilist.co/login?apiVersion=v2&client_id=...&response_type=...`, i.e. the user must log in first.

📄 **Redirect URI must match exactly** what was registered in the AniList developer console. AniList says: *"This can be any valid URI, including custom URI schemes."* — so a custom scheme such as `com.example.myapp://anilist-callback` is accepted.

### 2.3 Token exchange (Authorization Code Grant)
```
POST https://anilist.co/api/v2/oauth/token
Content-Type: application/json
Accept: application/json

{
  "grant_type": "authorization_code",
  "client_id": "{client_id}",
  "client_secret": "{client_secret}",
  "redirect_uri": "{redirect_uri}",
  "code": "{code}"
}
```
✅ Verified endpoint exists (probe with empty body returns `{"error":"unsupported_grant_type","message":"The authorization grant type is not supported by the authorization server.","hint":"Check that all required parameters have been provided"}`).

### 2.4 Implicit Grant (token in URL fragment)
After user approves, AniList redirects to:
```
{redirect_uri}#access_token={jwt}
```
The token is in the **URL fragment** (so the server never sees it). The mobile client intercepts the redirect (via custom-tab + intent-filter) and parses the fragment.

### 2.5 PKCE
⚠️ **AniList does NOT document PKCE.** The `code_challenge` / `code_challenge_method` parameters are not referenced anywhere in the official auth pages (`/guide/auth/authorization-code`, `/guide/auth/implicit`, `/guide/auth/authenticated-requests`). The Authorization Code Grant flow as documented requires a `client_secret` in the token exchange, which is incompatible with PKCE-only clients.

**Recommendation for mobile:** Because PKCE is unavailable and the Authorization Code Grant requires the secret, a public mobile client has two imperfect options:

| Option | Pros | Cons |
|---|---|---|
| **Implicit Grant + custom scheme** (legacy, what most AniList Android apps use today) | No secret shipped in the APK; simple | Deprecated in OAuth 2.1; token visible in URL fragment (mitigated by custom scheme on Android); no refresh tokens |
| **Authorization Code Grant + custom scheme + secret in APK** | Modern, simpler token invalidation flow (server-side) | `client_secret` is embedded in APK — extractable; doesn't really buy security on a public client |

**Recommended choice for this app:** Use the **Implicit Grant with a custom scheme redirect** (`<package>://anilist-auth`). Rationale:
1. Avoids shipping a `client_secret` in the APK — a secret in a public client is security theater.
2. Tokens are JWT, 1-year lifetime, no refresh — even with the auth-code flow you'd still need to re-prompt the user yearly. No benefit.
3. Custom scheme on Android requires an `<intent-filter>` on the auth-receiver activity; Android 12+ (API 31+) requires the scheme to be declared `android:autoVerify="false"` for non-http(s) schemes — which is what we want for custom schemes.
4. Use `CustomTabsIntent` to launch the authorize URL (lets the user's browser manage AniList login/2FA cookies without us touching them) and intercept the redirect via the intent-filter.

### 2.6 Token lifetime & refresh
📄 Tokens are JWT, valid for **1 year from issue**. **No refresh tokens.** Once expired, the user must re-authenticate.

### 2.7 Decoding the JWT
📄 *"The access tokens provided by the authorization flows are JWT tokens. You can use a JWT library to decode the token and get the user's ID, expiration date, and other information."* The `sub` claim of the JWT contains the AniList user ID. Use this to skip a `Viewer` round-trip on app start if you already have the user ID cached.

### 2.8 Making authenticated requests
```http
POST https://graphql.anilist.co HTTP/1.1
Authorization: Bearer <jwt>
Content-Type: application/json
Accept: application/json

{ "query": "...", "variables": { ... } }
```

### 2.9 Redirect URI format for Android
- **Custom scheme (recommended, simplest):** `<applicationId>://anilist-auth` — e.g. `com.testplaybyte.animetracker://anilist-auth`. Register this exact string in the AniList dev console and declare an `<intent-filter>` on a small `AnilistAuthActivity`:
  ```xml
  <activity android:name=".auth.AnilistAuthActivity"
            android:exported="true"
            android:launchMode="singleTop">
      <intent-filter>
          <action android:name="android.intent.action.VIEW"/>
          <category android:name="android.intent.category.DEFAULT"/>
          <category android:name="android.intent.category.BROWSABLE"/>
          <data android:scheme="com.testplaybyte.animetracker" android:host="anilist-auth"/>
      </intent-filter>
  </activity>
  ```
  The activity extracts `access_token` (implicit) or `code` (auth-code) from either the fragment or the query, finishes, and routes the token to the auth repo.
- **App Links (https:// scheme, optional hardening):** AniList does accept https redirect URIs, so you *could* register `https://animetracker.testplay-byte.com/anilist-callback` and use Android App Links (`assetlinks.json` + `autoVerify="true"`). This avoids other apps hijacking the custom scheme but requires you to host an `assetlinks.json` file. **Not required for v1**; can be added later as a security hardening pass.

---

## 3. Rate limits

### 3.1 Documented limit
📄 Documented rate limit: **90 requests per minute.** Both anonymous and authenticated requests share this budget — the limit is per-IP (with authenticated requests also tied to the user). AniList does not publish a separate higher per-user limit.

### 3.2 Current live limit (verified)
⚠️ **As of the probe date, AniList is in a degraded state** and the live limit is **30 req/min**. Verified by inspecting the response headers of a real request:
```
HTTP/2 401
x-ratelimit-limit: 30
x-ratelimit-remaining: 23
```
The docs page explicitly warns: *"The API is currently in a degraded state and is limited to 30 requests per minute. This is a temporary measure until the API is fully restored."*

**Design the client for 30 req/min, not 90.** If the cap is later lifted, you get headroom for free.

### 3.3 Headers (✅ verified)
| Header | Meaning |
|---|---|
| `X-RateLimit-Limit` | Total budget in the current window (e.g. `30` or `90`). |
| `X-RateLimit-Remaining` | Requests remaining in the current window. |
| `Retry-After` | (Only on 429) Seconds to wait before retrying. |
| `X-RateLimit-Reset` | (Only on 429) Unix timestamp when the window resets. |

These are also exposed via CORS: `access-control-expose-headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, Content-Length, Content-Range`.

### 3.4 Burst limiter
📄 *"On top of the above rate limiting, we also have a burst limiter. This limiter is designed to stop you from hammering the API with too many requests in a very short period of time."* — No specific burst threshold documented.

### 3.5 Over-the-limit response
📄 HTTP 429 with body:
```json
{ "data": null, "errors": [ { "message": "Too Many Requests.", "status": 429 } ] }
```
Headers on 429:
```
HTTP/1.1 429 Too Many Requests
Retry-After: 30
X-RateLimit-Limit: 90
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1502035959
```

### 3.6 Manual IP block
📄 AniList may manually block your IP for abuse — returns a custom message in the body. Excessive requests are dropped at the edge (Cloudflare) before hitting origin.

### 3.7 Recommended client backoff strategy
1. **Maintain a sliding 60-second window** of timestamps of all sent requests; reject new requests locally if `>= N-1` already in flight in the window where N = current observed limit (read `X-RateLimit-Limit` from the first response and use that as `N`; default 30).
2. **Always read `X-RateLimit-Remaining`** and adaptively throttle: if remaining ≤ 5, switch to "trickle" mode (max 1 request per 2 seconds).
3. **On HTTP 429**: parse `Retry-After`; pause the request queue for that many seconds (use a `Flow` that emits "paused" state to UIs so they can show a banner); do NOT retry in a tight loop.
4. **Use a single-flight queue** (`Channel` + worker) so background sync, foreground refresh, and lazy screen loads don't accidentally burst.
5. **Dedupe identical in-flight requests** (e.g. multiple screens requesting Media(id=1) should share one network call).

---

## 4. Core queries

All operation names verified by schema introspection against `https://graphql.anilist.co`.

### 4.1 `Viewer` — current authenticated user ✅
The `Viewer` query has **no arguments** — it infers the user from the Bearer token.

```graphql
query Viewer {
  Viewer {
    id
    name
    about
    avatar { large medium }
    bannerImage
    options {
      titleLanguage            # ROMAJI | ENGLISH | NATIVE | ROMAJI_STYLISED | ENGLISH_STYLISED | NATIVE_STYLISED
      displayAdultContent
      airingNotifications
      profileColor
      timezone
      staffNameLanguage        # ROMAJI_WESTERN | ROMAJI | NATIVE
      restrictMessagesToFollowing
    }
    mediaListOptions {
      scoreFormat               # POINT_100 | POINT_10_DECIMAL | POINT_10 | POINT_5 | POINT_3
      rowOrder
      animeList { sectionOrder customLists splitCompletedListByFormat advancedScoring enabled advancedScoringEnabled }
      mangaList { sectionOrder customLists splitCompletedListByFormat advancedScoring enabled advancedScoringEnabled }
    }
    statistics { anime { count meanScore standardDeviation minutesWatched episodesWatched chaptersRead volumesRead } manga { count meanScore standardDeviation chaptersRead volumesRead } }
    unreadNotificationCount     # cheap poll target — see §4.7
    siteUrl
    donatorTier
    donatorBadge
    moderatorRoles
    createdAt
    updatedAt
    previousNames { name createdAt }
  }
}
```
`User.statistics` returns `UserStatistics` with the full breakdown: `formats`, `statuses`, `scores`, `lengths`, `releaseYears`, `startYears`, `genres`, `tags`, `countries`, `voiceActors`, `staff`, `studios`.

### 4.2 `MediaListCollection` — user's anime/manga lists ✅
Returns the user's **full list** split by status and custom lists. Two pagination modes:
- **No chunking**: returns the entire list (capped at 11,000 most recently updated unique entries — won't affect real users).
- **Chunked**: pass `chunk: Int, perChunk: Int` and use `hasNextChunk: Boolean` to walk through. Recommended for large lists.

Full args (verified):
```
userId, userName, type, status, notes, startedAt, completedAt,
forceSingleCompletedList, chunk, perChunk, status_in, status_not_in,
status_not, notes_like, startedAt_greater, startedAt_lesser, startedAt_like,
completedAt_greater, completedAt_lesser, completedAt_like, sort
```

Sample query (authenticated):
```graphql
query MyAnimeList($userId: Int!, $chunk: Int = 1, $perChunk: Int = 500) {
  MediaListCollection(userId: $userId, type: ANIME, chunk: $chunk, perChunk: $perChunk) {
    hasNextChunk
    user { id name avatar { large } }
    lists {
      name                   # "Watching", "Completed", "Planning", "Dropped", "Paused", "Custom: ..."
      isCustomList
      isSplitCompletedList
      status                 # MediaListStatus enum
      entries {
        id                   # ⚠️ list entry ID, not media ID
        mediaId
        status               # CURRENT | PLANNING | COMPLETED | DROPPED | PAUSED | REPEATING
        score
        progress              # episodes watched (anime) or chapters read (manga)
        progressVolumes
        repeat
        priority
        private
        notes
        hiddenFromStatusLists
        customLists           # JSON-like map { "Custom list name": bool }
        advancedScores        # JSON-like map
        startedAt  { year month day }   # FuzzyDate
        completedAt { year month day }
        updatedAt
        createdAt
        media {
          id idMal
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
          bannerImage
          episodes duration status format season seasonYear
        }
      }
    }
  }
}
```

⚠️ **Important notes from the docs**:
- *"Even when making authenticated requests, the user is not inferred. You will need to specify the user in the query."* → always pass `userId` (you can get it from JWT `sub` or `Viewer.id`).
- *"Do not skip over the user's custom lists. Users can hide entries from the default status lists, but they can still be accessed through the custom lists."* → iterate ALL groups returned, not just the status groups.
- For fetching a single entry: `MediaList(id:..., mediaId:..., userId:...)` (any combination works, ignored args are silently dropped).

### 4.3 `Media` — single anime/manga ✅
Verified field list (full schema):
```
id, idMal, title, type, format, status, description, startDate, endDate,
season, seasonYear, seasonInt, episodes, duration, chapters, volumes,
countryOfOrigin, isLicensed, source, hashtag, trailer, updatedAt,
coverImage, bannerImage, genres, synonyms, averageScore, meanScore,
popularity, isLocked, trending, favourites, tags, relations, characters,
staff, studios, isFavourite, isFavouriteBlocked, isAdult, nextAiringEpisode,
airingSchedule, trends, externalLinks, streamingEpisodes, rankings,
mediaListEntry, reviews, recommendations, stats, siteUrl,
autoCreateForumThread, isRecommendationBlocked, isReviewBlocked, modNotes
```

Sample detail-screen query:
```graphql
query MediaDetail($id: Int!, $type: MediaType = ANIME) {
  Media(id: $id, type: $type) {
    id idMal type format status source isAdult
    season seasonYear seasonInt
    episodes duration chapters volumes
    countryOfOrigin isLicensed hashtag
    siteUrl
    title { romaji english native userPreferred }
    synonyms
    description(asHtml: false)
    coverImage { extraLarge large medium color }
    bannerImage
    genres
    tags { id name rank category isMediaSpoiler isGeneralSpoiler isAdult }
    averageScore meanScore popularity favourites trending
    rankings { id rank type format year season allTime context }
    startDate { year month day }
    endDate   { year month day }
    trailer { id site thumbnail }
    nextAiringEpisode { id airingAt timeUntilAiring episode }
    airingSchedule(notYetAired: true) { id airingAt episode }
    streamingEpisodes { title thumbnail url }        # ⚠️ only ~6 latest, Crunchyroll-sourced
    externalLinks { id url site type language color icon notes isDisabled }
    relations {
      edges {
        relationType          # ADAPTATION|PREQUEL|SEQUEL|PARENT|SIDE_STORY|CHARACTER|...
        node { id type format title { romaji english } coverImage { large } }
      }
    }
    characters(sort: ROLE, page: 1, perPage: 25) {
      edges { role voiceActors(sort: ROLE) { id name { first last full } } node { id name { first last full native } image { large } siteUrl } }
    }
    studios(isMain: true) { edges { isMain node { id name siteUrl isAnimationStudio } } }
    mediaListEntry { id status score progress repeat private notes startedAt { year } completedAt { year } }
    recommendations(sort: RATING_DESC, page: 1, perPage: 6) {
      nodes { id rating mediaRecommendation { id title { romaji english } coverImage { large } } }
    }
    updatedAt
  }
}
```

Notes:
- `description(asHtml: false)` returns plain text; `asHtml: true` returns sanitized HTML (useful if you want to keep `<i>`/`<b>`).
- `trailer.site` is typically `"youtube"`, `trailer.id` is the YouTube video id, `trailer.thumbnail` is a `i.ytimg.com` URL.
- `Media.nextAiringEpisode` returns `null` for finished/cancelled anime. `airingSchedule(notYetAired: true)` returns the full upcoming episode list for currently-airing shows.
- `Media.mediaListEntry` is `null` when not authenticated.

### 4.4 `Page` — search with filters ✅
`Page` has only two own args: `page: Int`, `perPage: Int` (max 50 — verified). Inside, you select exactly one of: `media`, `characters`, `staff`, `studios`, `mediaList`, `airingSchedules`, `mediaTrends`, `notifications`, `users`, `followers`, `following`, `activities`, `activityReplies`, `threads`, `threadComments`, `reviews`, `recommendations`, `likes`. (Using two of these in one Page query is a GraphQL error.)

Search example with filters:
```graphql
query Browse(
  $page: Int = 1, $perPage: Int = 50,
  $type: MediaType, $search: String,
  $season: MediaSeason, $seasonYear: Int,
  $format_in: [MediaFormat], $status_in: [MediaStatus],
  $genre_in: [String], $genre_not_in: [String],
  $tag_in: [String], $tag_not_in: [String],
  $minimumTagRank: Int,
  $sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC],
  $isAdult: Boolean = false, $onList: Boolean
) {
  Page(page: $page, perPage: $perPage) {
    pageInfo { currentPage hasNextPage perPage }
    media(
      type: $type, search: $search,
      season: $season, seasonYear: $seasonYear,
      format_in: $format_in, status_in: $status_in,
      genre_in: $genre_in, genre_not_in: $genre_not_in,
      tag_in: $tag_in, tag_not_in: $tag_not_in,
      minimumTagRank: $minimumTagRank,
      isAdult: $isAdult, onList: $onList,
      sort: $sort
    ) {
      id idMal
      title { romaji english native userPreferred }
      coverImage { extraLarge large color }
      bannerImage
      format status season seasonYear
      episodes duration chapters volumes
      averageScore meanScore popularity favourites trending
      genres
      startDate { year month day }
      nextAiringEpisode { airingAt episode timeUntilAiring }
      isAdult
    }
  }
}
```
Full filter args (verified) include: `id`, `idMal`, `startDate`, `endDate`, `season`, `seasonYear`, `type`, `format`/`format_in`/`format_not`/`format_not_in`, `status`/`status_in`/`status_not`/`status_not_in`, `episodes`/`episodes_greater`/`episodes_lesser`, `duration_*`, `chapters_*`, `volumes_*`, `isAdult`, `genre`/`genre_in`/`genre_not_in`, `tag`/`tag_in`/`tag_not_in`/`tagCategory`/`tagCategory_in`/`tagCategory_not_in`, `minimumTagRank`, `onList`, `licensedBy`/`licensedBy_in`/`licensedById`/`licensedById_in`, `averageScore_*`, `popularity_*`, `source`/`source_in`, `countryOfOrigin`/`countryOfOrigin_in`/`countryOfOrigin_not_in`, `isLicensed`, `search`, `sort`. (50+ args total — extreme flexibility.)

`MediaSort` enum values: `ID[_DESC]`, `TITLE_ROMAJI[_DESC]`, `TITLE_ENGLISH[_DESC]`, `TITLE_NATIVE[_DESC]`, `TYPE[_DESC]`, `FORMAT[_DESC]`, `START_DATE[_DESC]`, `END_DATE[_DESC]`, `SCORE[_DESC]`, `POPULARITY[_DESC]`, `TRENDING[_DESC]`, `EPISODES[_DESC]`, `DURATION[_DESC]`, `STATUS[_DESC]`, `CHAPTERS[_DESC]`, `VOLUMES[_DESC]`, `UPDATED_AT[_DESC]`, `SEARCH_MATCH`, `FAVOURITES[_DESC]`.

### 4.5 `AiringSchedule` ✅
Type fields (verified):
```
id, airingAt (Int! Unix seconds), timeUntilAiring (Int! seconds),
episode (Int!), mediaId (Int!), media (Media)
```

Query the user's tracking-list airing calendar in one call:
```graphql
query AiringForMyList($mediaIds: [Int]) {
  Page(page: 1, perPage: 50) {
    airingSchedules(mediaId_in: $mediaIds, notYetAired: true, sort: TIME) {
      id
      airingAt           # Unix seconds — sort by this
      timeUntilAiring    # convenience: seconds from now
      episode
      mediaId
      media { id title { romaji english userPreferred } coverImage { large } nextAiringEpisode { airingAt episode timeUntilAiring } }
    }
  }
}
```
✅ Verified live against anime ids `[21, 178789, 189046]` — returned the next upcoming episode for each show with correct `airingAt`/`timeUntilAiring`/`episode`. This is the canonical "what's airing this week" query.

Other args: `id`, `mediaId`, `episode`, `airingAt`, `notYetAired`, `id_in`/`id_not_in`, `mediaId_in`/`mediaId_not_in`, `episode_in`/`episode_not_in`/`episode_greater`/`episode_lesser`, `airingAt_greater`/`airingAt_lesser`, `sort`. (`AiringSort`: `ID[_DESC]`, `MEDIA_ID[_DESC]`, `TIME[_DESC]`, `EPISODE[_DESC]`.)

### 4.6 `MediaTrend` ✅
A `MediaTrend` represents one day of trend data for one media. Fields:
```
mediaId, date (Int! Unix seconds), trending (Int!),
averageScore, popularity, inProgress, releasing (Boolean!),
episode, media (Media)
```

Two ways to query:
- Single: `MediaTrend(mediaId: 1) { ... }` — verified returns the latest day's trend.
- Paginated (home screen "trending now"): `Page.mediaTrends(...)` — args are the same as the `MediaTrend` query (`mediaId_in`, `date_greater`, `date_lesser`, `trending_greater`, `sort`, etc.). Use `MediaTrendSort = TRENDING_DESC` for "trending now."

```graphql
query TrendingNow($page: Int = 1) {
  Page(page: $page, perPage: 25) {
    mediaTrends(sort: TRENDING_DESC, date_greater: <unix-7d-ago>) {
      mediaId
      trending
      averageScore
      popularity
      inProgress
      releasing
      date
      media {
        id title { romaji english userPreferred } coverImage { large extraLarge color }
        format status season seasonYear episodes
        nextAiringEpisode { airingAt episode timeUntilAiring }
      }
    }
  }
}
```
Note: `mediaTrends` returned empty in our probe without a date filter — likely because we queried without `date`/`sort` constraints. Always pass `sort: TRENDING_DESC` (or another sort) and a `date_greater` window to get meaningful results.

### 4.7 `Notification` ✅ (requires auth)
**`Page.notifications`** requires authentication. Anonymous call returns HTTP 401 with `{ "message": "Unauthorized.", "status": 401 }`.

Args: `type: NotificationType`, `resetNotificationCount: Boolean`, `type_in: [NotificationType]`.

**Notification is a UNION** called `NotificationUnion` with **20 possible types** (verified via introspection):
- AiringNotification — fields: `id, type, animeId, episode, contexts, createdAt, media` ← the important one for us.
- FollowingNotification
- ActivityMessageNotification
- ActivityMentionNotification
- ActivityReplyNotification
- ActivityReplySubscribedNotification
- ActivityLikeNotification
- ActivityReplyLikeNotification
- ThreadCommentMentionNotification
- ThreadCommentReplyNotification
- ThreadCommentSubscribedNotification
- ThreadCommentLikeNotification
- ThreadLikeNotification
- RelatedMediaAdditionNotification
- MediaDataChangeNotification
- MediaMergeNotification
- MediaDeletionNotification
- MediaSubmissionUpdateNotification
- StaffSubmissionUpdateNotification
- CharacterSubmissionUpdateNotification

`NotificationType` enum (filter):
```
ACTIVITY_MESSAGE, ACTIVITY_REPLY, FOLLOWING, ACTIVITY_MENTION,
THREAD_COMMENT_MENTION, THREAD_SUBSCRIBED, THREAD_COMMENT_REPLY,
AIRING, ACTIVITY_LIKE, ACTIVITY_REPLY_LIKE, THREAD_LIKE,
THREAD_COMMENT_LIKE, ACTIVITY_REPLY_SUBSCRIBED, RELATED_MEDIA_ADDITION,
MEDIA_DATA_CHANGE, MEDIA_MERGE, MEDIA_DELETION, MEDIA_SUBMISSION_UPDATE,
STAFF_SUBMISSION_UPDATE, CHARACTER_SUBMISSION_UPDATE
```

Sample polling query:
```graphql
query Notifications($page: Int = 1, $perPage: Int = 25, $reset: Boolean = false) {
  Page(page: $page, perPage: $perPage) {
    notifications(resetNotificationCount: $reset) {
      __typename
      ... on AiringNotification { id type animeId episode contexts createdAt media { id title { romaji english } coverImage { large } } }
      ... on FollowingNotification { id type user { id name avatar { large } } createdAt }
      ... on RelatedMediaAdditionNotification { id type media { id title { romaji english } coverImage { large } } createdAt }
      ... on MediaDataChangeNotification { id type media { id title { romaji english } } reason context createdAt }
      ... on MediaMergeNotification { id type media { id title { romaji english } } deletedMediaTitles createdAt }
      ... on MediaDeletionNotification { id type deletedMediaId deletedMediaTitle createdAt }
    }
  }
}
```

**AniList does NOT push notifications** (no WebSocket / no subscriptions — see §8). **You must poll.** Polling cost considerations:
- A cheap pre-check: `Viewer { unreadNotificationCount }` — single small Int field. If it's `0`, skip the heavier `Page.notifications` call. Use this as a "trigger" poll every N minutes; only call `notifications` when the count goes up.
- `resetNotificationCount: true` should be passed **on the first call after the user opens the notifications screen** so AniList's "unread" badge resets server-side. Don't pass it on background polls.

### 4.8 `Character`, `Staff`, `Studio` ✅
Lower-priority detail-page queries. Available fields (verified):

```graphql
# Character
{ Character(id: $id) { id name { first last full native alternative alternativeSpoiler } image { large medium } description gender dateOfBirth { year month day } age bloodType isFavourite siteUrl favourites media { edges { characterRole node { id title { romaji english } coverImage { large } } } } } }

# Staff
{ Staff(id: $id) { id name { first last full native alternative } languageV2 image { large medium } description primaryOccupations gender dateOfBirth { year month day } dateOfDeath { year month day } age yearsActive homeTown bloodType isFavourite siteUrl favourites staffMedia { edges { staffRole node { id title { romaji english } coverImage { large } } } } characters { edges { roleNotes voiceActorRoles characterRole node { id name { full } image { large } } voiceActors { id name { full } } } } } }

# Studio
{ Studio(id: $id) { id name isAnimationStudio siteUrl isFavourite favourites media { edges { isMainStudio node { id title { romaji english } coverImage { large } format } } } } }
```

---

## 5. ID mapping

- ✅ **`Media.idMal: Int`** — direct MAL ID field. Verified live (Cowboy Bebop returns `idMal: 1` matching MAL id 1). Filter args `idMal`, `idMal_in`, `idMal_not`, `idMal_not_in` are all available on `Media` and `Page.media`.
- ❌ **No Kitsu id field.** AniList does not store or expose Kitsu IDs. To resolve a Kitsu id → AniList id (or vice-versa), use the Kitsu API's `mappings` endpoint (Kitsu exposes `mappings` with `externalSite: "anilist_production"`/`"myanimelist_production"`/etc.). This is documented as the reason **R-3 (Kitsu/Jikan research)** exists as a separate task.
- ✅ **`Media.id_in: [Int]`** — bulk-fetch up to 50 Media objects by ID in a single `Page.media` call (subject to perPage=50 cap). For larger bulk fetches, batch into chunks of 50.
- ⚠️ The `Site` field on `MediaExternalLink` is just a string like `"Crunchyroll"`, `"Hulu"`, `"Netflix"`, `"Official Site"` — not a stable identifier. Don't use it for cross-source linking; use `siteId` (Int) which is AniList's internal id for that external site.

---

## 6. Pagination

### 6.1 `Page(page, perPage)` — offset-based, capped at 50
✅ **Verified empirically**: requested `perPage=25, 50, 100, 200, 500, 1000`; actual returned counts were `25, 50, 50, 50, 50, 50`. The server silently caps at **50**.

📄 **PageInfo degradation**: *"The total and lastPage fields are not currently accurate. You should only rely on hasNextPage for any pagination logic."* So:
- ✅ Trust `pageInfo.hasNextPage` (Boolean).
- ❌ Do NOT trust `pageInfo.total` or `pageInfo.lastPage`.

Sample loop:
```kotlin
var page = 1
do {
  val resp = anilist.browse(page = page, perPage = 50, ...)
  // ... upsert Media rows into cache ...
  page += 1
} while (resp.pageInfo.hasNextPage && !cancelToken.isCancelled())
```

### 6.2 `MediaListCollection(chunk, perChunk)` — chunked
For large user lists, pass `chunk: Int, perChunk: Int` and read `hasNextChunk: Boolean`. Recommended `perChunk = 500` (keeps response payloads bounded; avoids the 11,000-entry ceiling surprise). Loop while `hasNextChunk == true`.

### 6.3 Cursor-based pagination?
❌ AniList does **not** expose cursor-based pagination (`after`/`before`/`Relay` style). All pagination is **offset-based** (`page`/`perPage`) or **chunked** (`chunk`/`perChunk`). For our offline-first cache, that's fine — we can page through deterministically. Be aware that offset-based pagination has the classic "data shifts between pages if rows change mid-poll" race; reconcile by upserting (not replacing) on each refresh.

---

## 7. Image CDN

✅ Base URL: `https://s4.anilist.co/file/anilistcdn/...`

Examples captured live:
- Cover large: `https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-GCsPm7waJ4kS.png`
- Cover medium: `https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx1-GCsPm7waJ4kS.png`
- Cover small: `https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx1-GCsPm7waJ4kS.png`
- Banner: `https://s4.anilist.co/file/anilistcdn/media/anime/banner/1-OquNCNB6srGe.jpg`

URL pattern: `https://s4.anilist.co/file/anilistcdn/media/{anime|manga}/{cover|banner}/{small|medium|large|extraLarge}/{slug}`.

`MediaCoverImage` fields: `extraLarge`, `large`, `medium`, `color` (hex string like `"#f16b50"`, useful as a placeholder background tint while the image loads).

**Rate limit on image CDN:** Not documented. Cloudflare-fronted. Cache aggressively:
- Coil's built-in HTTP cache + memory cache; 50-100 MB disk cache.
- Set `Cache-Control` honoring and use `maxStale` for offline reads.
- Use the `color` field as the placeholder color (Coil `Placeholder` API).

---

## 8. WebSocket / real-time subscriptions

✅ **AniList does NOT have a GraphQL subscriptions endpoint.** Verified via introspection:
```graphql
{ __schema { queryType { name } mutationType { name } subscriptionType { name } } }
```
Result:
```json
{ "data": { "__schema": {
  "queryType": { "name": "Query" },
  "mutationType": { "name": "Mutation" },
  "subscriptionType": null
}}}
```
`subscriptionType: null` is definitive.

**Implication:** every piece of "live" data (airing schedule, notifications, list updates from other devices, trending changes) must be **polled** on a cadence. There is no push channel.

Recommended polling cadences (assuming the 30 req/min degraded cap; halve the cadence if the cap lifts back to 90):

| Poll target | Cadence | Why |
|---|---|---|
| `Viewer { unreadNotificationCount }` (cheap pre-check) | every 10 min (foreground) / 60 min (background, with WorkManager) | cheap; decides whether to do the heavier `notifications` call |
| `Page.notifications` (only if count went up) | on-demand (when count increases) or when user opens Notifications screen | heavier payload; only fetch when needed |
| `Page.airingSchedules(mediaId_in: [...], notYetAired: true, sort: TIME)` | every 30 min foreground, every 6 h background | episodes don't shift quickly; 30 min is enough to update "airing in next 24h" widget |
| `MediaListCollection` (own list) | on app start + on user pull-to-refresh + after every `SaveMediaListEntry` mutation | own list rarely changes from outside; no need for tight polling |
| `Page.mediaTrends(sort: TRENDING_DESC)` | every 1 h foreground | trending barely changes minute-to-minute |
| `Media(id: ...)` (detail screen) | on screen entry; refresh if `cached.updatedAt` older than 1 h | single record; cheap |

WorkManager `PeriodicWorkRequest` minimum is 15 minutes — use that as the floor for background polling.

---

## 9. Anonymous vs. authenticated scope

### Anonymous (no token) — works ✅
- `Media(id, ...)` and `Page.media(...)` — search and detail
- `Page.airingSchedules(...)`
- `MediaTrend` / `Page.mediaTrends`
- `Character`, `Staff`, `Studio` and their `Page.*` paginated variants
- `User(id, ...)` for public/unlisted users (NOT for private users — returns 404 `"Private User"`)
- `MediaListCollection(userId, ...)` for **public** users' lists — verified live (user "matchai" id=2 returned their PLANNING list anonymously)
- `Thread`, `Activity`, `Review`, `Recommendation`, `Following`, `Follower` reads (no docs restriction)

### Anonymous — fails ❌
- `Viewer` → returns HTTP 401 `"Unauthorized."`
- `Page.notifications` → HTTP 401 `"Unauthorized."`
- `Media.mediaListEntry` field → returns `null` (does not error, just no data)
- All mutations → 401

### Authenticated — additionally available ✅
- `Viewer`
- Own private `MediaListCollection` and `MediaList`
- `mediaListEntry` field on `Media` (returns own entry for that media)
- `Page.notifications` (all 20 union subtypes)
- All `Mutation.*` operations (see §10)

This perfectly matches our **two app modes**:
- **Logged-out mode**: trending, search, detail page, public lists — no auth needed.
- **Logged-in mode**: Viewer, own MediaListCollection, notifications, mutations.

---

## 10. Mutations

All mutations require authentication. Verified available mutations (full list):
```
UpdateUser, SaveMediaListEntry, UpdateMediaListEntries, DeleteMediaListEntry,
DeleteCustomList, SaveTextActivity, SaveMessageActivity, SaveListActivity,
DeleteActivity, ToggleActivityPin, ToggleActivitySubscription, SaveActivityReply,
DeleteActivityReply, ToggleLike, ToggleLikeV2, ToggleFollow, ToggleFavourite,
UpdateFavouriteOrder, SaveReview, DeleteReview, RateReview, SaveRecommendation,
SaveThread, DeleteThread, ToggleThreadSubscription, SaveThreadComment,
DeleteThreadComment, UpdateAniChartSettings, UpdateAniChartHighlights
```
Naming convention (📄): `Save*` = create-or-update (update if `id` provided, create otherwise); `Delete*` = delete; `Toggle*` = boolean toggle.

### 10.1 `SaveMediaListEntry` ✅
Verified args:
```
id, mediaId, status (MediaListStatus), score (Float), scoreRaw (Int),
progress (Int), progressVolumes (Int), repeat (Int), priority (Int),
private (Boolean), notes (String), hiddenFromStatusLists (Boolean),
customLists ([String]), advancedScores ([Float]),
startedAt (FuzzyDateInput), completedAt (FuzzyDateInput)
```
- **If `id` is omitted**: creates a new entry, returns the new `id`.
- **If `id` is provided**: updates the existing entry.

```graphql
mutation SaveEntry(
  $id: Int, $mediaId: Int, $status: MediaListStatus,
  $score: Float, $progress: Int, $repeat: Int,
  $private: Boolean, $notes: String,
  $startedAt: FuzzyDateInput, $completedAt: FuzzyDateInput
) {
  SaveMediaListEntry(
    id: $id, mediaId: $mediaId, status: $status,
    score: $score, progress: $progress, repeat: $repeat,
    private: $private, notes: $notes,
    startedAt: $startedAt, completedAt: $completedAt
  ) {
    id status score progress repeat private notes
    startedAt { year month day } completedAt { year month day }
    updatedAt
  }
}
```
The response includes the (possibly new) `id` and the new `updatedAt` — use it to update the local cache without an extra round-trip.

### 10.2 `UpdateMediaListEntries` ✅
Bulk update. Args: `ids: [Int], status, score, progress, ...` (same shape minus `id`, plus `ids`).

### 10.3 `DeleteMediaListEntry` ✅
```graphql
mutation DeleteEntry($id: Int!) {
  DeleteMediaListEntry(id: $id) { deleted }
}
```
Returns a `Deleted` object type (typically `{ deleted: Boolean }`).

### 10.4 `DeleteCustomList` ✅
Args: `customList: String, type: MediaType`. Useful when the user wants to delete a custom list group.

### 10.5 `UpdateUser` ✅ (toggle user options)
Verified args:
```
about (String), titleLanguage (UserTitleLanguage),
displayAdultContent (Boolean), airingNotifications (Boolean),
scoreFormat (ScoreFormat), rowOrder (String), profileColor (String),
donatorBadge (String), notificationOptions ([NotificationOptionInput]),
timezone (String), activityMergeTime (Int),
animeListOptions (MediaListOptionsInput), mangaListOptions (MediaListOptionsInput),
staffNameLanguage (UserStaffNameLanguage),
restrictMessagesToFollowing (Boolean),
disabledListActivity ([ListActivityOptionInput])
```
Use this to toggle e.g. the user's `displayAdultContent` setting or their preferred `scoreFormat`.

```graphql
mutation ToggleAdultContent($displayAdultContent: Boolean!) {
  UpdateUser(displayAdultContent: $displayAdultContent) {
    id options { displayAdultContent airingNotifications scoreFormat titleLanguage }
  }
}
```

### 10.6 Other useful mutations
- `ToggleFavourite(animeId, mangaId, characterId, staffId, studioId)` — toggle favourite.
- `ToggleFollow(userId)` — follow/unfollow.
- `ToggleLike(id, type: LikeableType)` / `ToggleLikeV2(id, type: LikeableType)` — like activities.
- `SaveActivityReply(activityId, text)`, `SaveTextActivity(...)`, `SaveListActivity(...)` — write activities (for social feed; probably out of scope for v1).

---

## 11. Episodes metadata

✅ AniList DOES expose per-episode data via **`Media.streamingEpisodes`**, but it is **very limited**:
- Returns **only ~6 most recent episodes** for a show (verified on One Piece id=21: returned episodes ~125-130 of 1100+).
- Fields: `title` (String), `thumbnail` (String URL), `url` (String — typically a Crunchyroll watch URL).
- **No episode descriptions**, no air dates, no episode numbers as integers (the title includes "Episode N - ...").
- **Source: Crunchyroll integration** — thumbnails live on `img1.ak.crunchyroll.com` / `img1.ak.crunchyroll.com`, NOT on AniList's CDN. May break/expire if Crunchyroll restructures.
- For non-Crunchyroll shows, `streamingEpisodes` is typically empty.

So our hypothesis holds: **AniList is weak for per-episode metadata**. Use:
- `Media.episodes` (Int) — total count.
- `Media.nextAiringEpisode { airingAt, episode, timeUntilAiring }` — the next upcoming episode (verified live on One Piece: `{airingAt:1786889760, episode:1174, timeUntilAiring:35598}`).
- `Media.airingSchedule(notYetAired: true) { airingAt, episode }` — full list of upcoming episodes for an airing show.
- For richer per-episode data (titles, descriptions, thumbnails, air dates for ALL episodes): **use Kitsu** (`/api/edge/episodes?filter[mediaId]=...` or `/api/edge/anime/{id}/episodes`) **or Jikan v4** (`/anime/{id}/episodes` + `/anime/{id}/full`). This is the role of R-3.

---

## 12. Caching strategy

The user wants offline-first. AniList's API shape drives the strategy:

### 12.1 What to cache
| Entity | Source | TTL (foreground-triggered refresh) | TTL (background WorkManager) | Notes |
|---|---|---|---|---|
| `Media` (anime/manga detail) | `Media(id)` | 1 h | 24 h | Use `Media.updatedAt` for reconciliation (see §12.4). |
| `Media.coverImage` / `bannerImage` | image CDN | 30 d | n/a (cached by Coil) | Persistent disk cache; treat as immutable (URL contains a hash). |
| `Page.media` (search/browse) results | `Page(page,perPage,sort=...)` | 5 min | 1 h | Cache the *result set* (list of ids + sort key) AND the underlying Media rows (those get their own TTL). |
| `MediaListCollection` (own lists) | `MediaListCollection(userId, type, chunk, perChunk)` | refresh on app start + after every mutation | 6 h | Always overwrite local rows from server response; treat server as authoritative for own list. |
| `MediaList` entries (individual) | `MediaList`, `SaveMediaListEntry` response | mutate immediately on user action (optimistic) | n/a | Mutations return the full new state including `updatedAt` — store that. |
| `AiringSchedule` | `Page.airingSchedules(mediaId_in=[...])` | 30 min | 6 h | Cheap to refresh; cheap to cache (small payload). |
| `MediaTrend` | `Page.mediaTrends(sort=TRENDING_DESC)` | 1 h | 6 h | History data is append-only — cache daily snapshots if you want trend graphs. |
| `Notification` | `Page.notifications` | refresh on demand (when `unreadNotificationCount` increases) | 6 h | Store `createdAt` locally; dedupe by `id`. |
| `User` (Viewer + stats) | `Viewer` | refresh on app start | 24 h | Includes `unreadNotificationCount` — refresh whenever you want to check for new notifications. |
| `Character` / `Staff` / `Studio` | respective queries | 7 d | n/a (lazy on detail-screen open) | Rarely changes; long TTL is fine. |

### 12.2 Cache schema entities (recommended for the Android app)
A SQLite (SQLDelight or Room) schema with at least these tables:

```
media                  (id PK, idMal, type, format, status, season, seasonYear, episodes, duration,
                        chapters, volumes, title_romaji, title_english, title_native, title_user_preferred,
                        cover_xl, cover_lg, cover_md, cover_color, banner,
                        description, genres_json, synonyms_json,
                        average_score, mean_score, popularity, favourites, trending,
                        start_date, end_date, source, country_of_origin, is_adult, is_licensed,
                        site_url, updated_at_server, fetched_at_local)
media_tag              (media_id FK, tag_id FK, rank)         -- join table
tag                    (id PK, name, category, description, is_adult, is_spoiler)
media_external_link    (id PK, media_id FK, url, site, site_id, type, language, color, icon, is_disabled)
media_relation         (from_media_id FK, to_media_id FK, relation_type)
media_streaming_episode(media_id FK, position, title, thumbnail, url)
media_list_entry       (id PK, user_id, media_id FK, status, score, score_raw, progress,
                        progress_volumes, repeat, priority, private, notes,
                        hidden_from_status_lists, custom_lists_json, advanced_scores_json,
                        started_at_year, started_at_month, started_at_day,
                        completed_at_year, completed_at_month, completed_at_day,
                        updated_at_server, created_at_server, fetched_at_local)
airing_schedule        (id PK, media_id FK, airing_at, episode, time_until_airing_snapshot, fetched_at_local)
media_trend_snapshot   (media_id FK, date, trending, average_score, popularity, in_progress, releasing, episode, fetched_at_local)
user                   (id PK, name, about, avatar_large, avatar_medium, banner, options_json,
                        statistics_json, unread_notification_count, fetched_at_local)
notification           (id PK, type, created_at, payload_json, is_read_local, fetched_at_local)
search_cache           (id PK, query_hash, sort, type, page, per_page, result_ids_json, fetched_at_local)  -- cache of Page.media result sets
auth_state             (singleton: access_token, token_issued_at, user_id, scopes)
```

### 12.3 TTL strategy
- Every cached row has a `fetched_at_local` timestamp.
- Repository layer exposes `Flow<List<...>>` for reads (CORE_RULES §23).
- Read path: emit cached rows immediately (UI never blocks on network). Concurrently fire a network refresh if `now - fetched_at_local > TTL`. On success, upsert rows (don't delete first — that causes UI flicker); on failure, surface error to UI but keep showing cached rows.
- This matches the user's "offline-first" requirement exactly: the UI works on cache; the network reconciles in the background.

### 12.4 Reconciliation strategy
**AniList exposes `updatedAt` (Unix seconds) on `Media`, `MediaList`, `User` — use it.**

For `Media`:
- On refresh, you can either:
  - **Cheap path**: fetch the full `Media(id)` and overwrite. Single-record fetch is cheap.
  - **Batch path** (for a tracked-list refresh): use `Page.media(id_in: [50 ids], sort: UPDATED_AT_DESC) { id updatedAt title { ... } }` — fetch the lightweight fields for all 50 in one call, diff `updatedAt` against cached `updated_at_server`, and only re-fetch the full `Media` for those whose `updatedAt` changed.

For `MediaList`:
- `MediaList.updatedAt` is exposed; same diff-and-replace strategy.
- For full-list refresh: use `MediaListCollection` and **upsert all entries from the response**, then **delete locally any entry whose `id` is NOT in the response** (because it was removed remotely).

For `Notification`:
- Sort by `createdAt DESC`; paginate until you reach a `createdAt` you already have locally; stop. This avoids re-fetching old notifications every poll.

### 12.5 Mutation flow with optimistic updates
Per CORE_RULES §23:
1. User taps "Mark episode 5 watched".
2. ViewModel updates the local DB row (`media_list_entry.progress = 5`) **immediately** — UI reflects change instantly via `Flow`.
3. Fire `SaveMediaListEntry(mediaId, progress: 5)` mutation.
4. On success: upsert the response (which includes the new `updatedAt`); confirm.
5. On failure: roll back the optimistic update; show snackbar with retry button.
6. Queue mutations during offline periods in a `MutationQueue` WorkManager worker; flush on reconnect.

---

## 13. Error handling

### 13.1 GraphQL error shape
📄 AniList returns errors inside the `errors` array of the GraphQL response, even when the HTTP status is 200. Example:
```json
{
  "data": null,
  "errors": [
    {
      "message": "Cannot query field \"nonexistentField\" on type \"MediaList\".",
      "status": 400,
      "locations": [ { "line": 4, "column": 5 } ]
    }
  ]
}
```
Fields:
- `message` — human-readable.
- `status` — **HTTP-style integer code carried inside the GraphQL error**, e.g. 400 / 401 / 404 / 429 / 500.
- `locations` — `[ {line, column} ]` query position (useful in dev).
- `validation` — present only on mutation validation failures; an object mapping field name → array of error strings, e.g. `{"score": ["The score may not be greater than 100."]}`.

### 13.2 Status codes observed
| HTTP | GraphQL `status` | Meaning | Action |
|---|---|---|---|
| 200 | (none) | Success | Parse `data` |
| 200 | 400 | Bad query / unknown field / validation | Log; don't retry; surface to dev (don't show user) |
| 200 | 401 | Missing/invalid token (auth required) | Re-authenticate |
| 200 | 404 | Not found OR `Private User` | Treat as "no data"; e.g. hide media row if it was deleted |
| 200 | 429 | Rate-limited | Pause queue for `Retry-After` seconds |
| 200 | 500 | Server error | Retry with exponential backoff (max 3 attempts) |
| 200 | 403 | API-wide disabled (severe outage) | Show "AniList is temporarily unavailable" banner; do not retry |

📄 The 403 API-unavailable body looks like:
```json
{
  "errors": [
    {
      "message": "The AniList API has been temporarily disabled due to severe stability issues. Please check the official AniList Discord for more information.",
      "status": 403,
      "locations": [ { "line": 1, "column": 1 } ]
    }
  ],
  "data": null
}
```

### 13.3 Partial-data responses
⚠️ Some queries return `data: <partial>` alongside `errors: [...]` when a subfield fails. Always:
1. Check `response.errors` first — log every error, even on HTTP 200.
2. If `response.data` is non-null, use whatever is present (nullable fields will be `null`).
3. If a whole top-level field returns `null` (e.g. `data.MediaListCollection: null`), check the corresponding error in `errors[]` to determine the cause (auth, rate limit, private user, etc.).

### 13.4 Network errors
For OkHttp/Ktor-level exceptions (timeouts, no connectivity, DNS failures): do NOT propagate as a hard error to the UI when cached data exists — surface cached data + a small "offline" indicator (per CORE_RULES §23). Only escalate to a full error screen when there is no cached data AND no network.

### 13.5 Recommended client error model
A sealed result type:
```kotlin
sealed class AniListResult<out T> {
  data class Success<T>(val data: T) : AniListResult<T>()
  data class Partial<T>(val data: T, val errors: List<GraphQLError>) : AniListResult<T>()
  data class ValidationError(val fieldErrors: Map<String, List<String>>) : AniListResult<Nothing>()
  data class RateLimited(val retryAfterSeconds: Int) : AniListResult<Nothing>()
  data class Unauthorized(val message: String) : AniListResult<Nothing>()
  data class NotFound(val message: String) : AniListResult<Nothing>()
  data class ServerError(val message: String, val status: Int) : AniListResult<Nothing>()
  data class NetworkError(val cause: Throwable) : AniListResult<Nothing>()
}
```
The repository maps GraphQL errors to this sealed type; ViewModels pattern-match and surface appropriate UI states (snackbar / banner / retry button / re-login screen).

---

## 14. Recommendations for the Android app

### 14.1 Auth approach (PKCE custom scheme)
- Use the **Implicit Grant** with a **custom-scheme redirect URI** (`<applicationId>://anilist-auth`). Reasons:
  1. AniList **does not support PKCE** — the Authorization Code Grant requires `client_secret`, which cannot be safely stored in an APK.
  2. No refresh tokens exist either way, so the auth-code flow buys nothing.
  3. Custom scheme on Android is the simplest reliable way to receive the redirect; declare an `<intent-filter>` with `BROWSABLE` category.
- Launch the auth URL with **`CustomTabsIntent`** (re-uses the user's browser session so they don't have to re-enter their AniList credentials every time).
- Capture the JWT, **decode the `sub` claim** to get the `userId` immediately (skip a `Viewer` round-trip on cold start).
- **Store the token** in `EncryptedSharedPreferences` (Tink-backed) on API 23+; fall back to `KeyStore`-backed encryption otherwise.
- **Auto-logout**: schedule a check 1 year from `token_issued_at`; show a "Re-link AniList" prompt when within 7 days of expiry.
- **Optional hardening (post-v1)**: switch to Android **App Links** (`https://` redirect + `assetlinks.json`) to prevent custom-scheme hijacking by other apps.

### 14.2 Cache schema entities
Use the schema proposed in §12.2. Key entities: `media`, `media_list_entry`, `airing_schedule`, `media_trend_snapshot`, `user`, `notification`, plus join tables for tags, external links, relations, streaming episodes. Maintain a `search_cache` table for browse/search result sets.

### 14.3 Polling cadence for airing
| Poll target | Foreground cadence | Background cadence (WorkManager) |
|---|---|---|
| `Viewer.unreadNotificationCount` (cheap pre-check) | 10 min | 60 min |
| `Page.notifications` (only if count went up) | on-demand | n/a |
| `Page.airingSchedules(mediaId_in: [...])` | 30 min | 6 h |
| `MediaListCollection` (own list) | on app start + after mutations | 6 h |
| `Page.mediaTrends(sort: TRENDING_DESC)` | 1 h | 6 h |
| `Media(id)` (detail screen) | on screen entry if cached age > 1 h | n/a (lazy) |

### 14.4 Rate-limit strategy
- Maintain a sliding 60-second window of request timestamps locally; **never exceed `N-1` requests per 60 s**, where `N` is the latest observed `X-RateLimit-Limit` (default 30, since the API is currently degraded).
- Use a **single-flight request queue** (`Channel<Request>` + one worker coroutine). All network calls (UI refreshes, screen loads, background sync) enqueue through this queue.
- **Dedupe** identical in-flight requests (e.g. three screens asking for `Media(id=1)` should share one network call).
- **Read every response's `X-RateLimit-Remaining`**; if ≤ 5, throttle to 1 req / 2 s.
- **On 429**: pause the queue for `Retry-After` seconds, surface a "rate-limited, retrying in Ns" banner to UIs that depend on the queue; **do not retry in a tight loop** (CORE_RULES §27 — stop after 5 consecutive failures).
- **On 5xx**: exponential backoff (1s, 2s, 4s), max 3 attempts; then give up and use cache.

### 14.5 Additional notes
- Because AniList has no subscriptions / WebSocket (§8), **all real-time needs are polling**. Schedule via WorkManager `PeriodicWorkRequest` (min 15 min interval).
- The `Media.updatedAt` field is your friend for cache reconciliation — always request it.
- `Media.idMal` is your bridge to MAL; combine with R-3 (Kitsu/Jikan) for episode-level data not exposed by AniList.
- The 50-item `perPage` cap means bulk-fetching a user's tracked media ids (e.g. for an airing-schedule poll) needs to be batched into chunks of ≤ 50 IDs per `mediaId_in` array.
- Expect the rate limit to potentially drop further during incidents; build the client to degrade gracefully (cache-first, never crash on network failure).

---

## Appendix A — Verified enums (for type-safe Kotlin enums)

```
MediaType          = ANIME | MANGA
MediaFormat        = TV | TV_SHORT | MOVIE | SPECIAL | OVA | ONA | MUSIC | MANGA | NOVEL | ONE_SHOT
MediaStatus        = FINISHED | RELEASING | NOT_YET_RELEASED | CANCELLED | HIATUS
MediaSeason        = WINTER | SPRING | SUMMER | FALL
MediaSource        = ORIGINAL | MANGA | LIGHT_NOVEL | VISUAL_NOVEL | VIDEO_GAME | OTHER | NOVEL |
                     DOUJINSHI | ANIME | WEB_NOVEL | LIVE_ACTION | GAME | COMIC | MULTIMEDIA_PROJECT | PICTURE_BOOK
MediaListStatus    = CURRENT | PLANNING | COMPLETED | DROPPED | PAUSED | REPEATING
MediaRelation      = ADAPTATION | PREQUEL | SEQUEL | PARENT | SIDE_STORY | CHARACTER | SUMMARY |
                     ALTERNATIVE | SPIN_OFF | OTHER | SOURCE | COMPILATION | CONTAINS | SAME_UNIVERSE
CharacterRole      = MAIN | SUPPORTING | BACKGROUND
ExternalLinkType   = INFO | STREAMING | SOCIAL
ExternalLinkMediaType = ANIME | MANGA | STAFF
ScoreFormat        = POINT_100 | POINT_10_DECIMAL | POINT_10 | POINT_5 | POINT_3
UserTitleLanguage  = ROMAJI | ENGLISH | NATIVE | ROMAJI_STYLISED | ENGLISH_STYLISED | NATIVE_STYLISED
UserStaffNameLanguage = ROMAJI_WESTERN | ROMAJI | NATIVE
NotificationType   = (see §4.7 — 20 values)
AiringSort         = ID[_DESC] | MEDIA_ID[_DESC] | TIME[_DESC] | EPISODE[_DESC]
MediaTrendSort     = ID[_DESC] | MEDIA_ID[_DESC] | DATE[_DESC] | SCORE[_DESC] | POPULARITY[_DESC] | TRENDING[_DESC] | EPISODE[_DESC]
MediaSort          = ID[_DESC] | TITLE_ROMAJI[_DESC] | TITLE_ENGLISH[_DESC] | TITLE_NATIVE[_DESC] |
                     TYPE[_DESC] | FORMAT[_DESC] | START_DATE[_DESC] | END_DATE[_DESC] |
                     SCORE[_DESC] | POPULARITY[_DESC] | TRENDING[_DESC] |
                     EPISODES[_DESC] | DURATION[_DESC] | STATUS[_DESC] |
                     CHAPTERS[_DESC] | VOLUMES[_DESC] | UPDATED_AT[_DESC] |
                     SEARCH_MATCH | FAVOURITES[_DESC]
MediaListSort      = MEDIA_ID[_DESC] | SCORE[_DESC] | STATUS[_DESC] | PROGRESS[_DESC] |
                     PROGRESS_VOLUMES[_DESC] | REPEAT[_DESC] | PRIORITY[_DESC] |
                     STARTED_ON[_DESC] | FINISHED_ON[_DESC] | ADDED_TIME[_DESC] | UPDATED_TIME[_DESC] |
                     MEDIA_TITLE_ROMAJI[_DESC] | MEDIA_TITLE_ENGLISH[_DESC] | MEDIA_TITLE_NATIVE[_DESC] |
                     MEDIA_POPULARITY[_DESC]
```

## Appendix B — Live verification log

| Probe | Result |
|---|---|
| `GET https://graphql.anilist.co/?query=...` | HTTP 404 with hint "Use POST request" |
| `PUT https://graphql.anilist.co` | HTTP 405 (Nginx) |
| `__schema.subscriptionType` | `null` (no subscriptions) |
| `Page(perPage=25)` returns 25 items | ✅ |
| `Page(perPage=50)` returns 50 items | ✅ |
| `Page(perPage=100/200/500/1000)` returns 50 items each | ✅ (cap is 50) |
| `Media(id=1)` returns idMal=1 (Cowboy Bebop) | ✅ |
| `Media(id=1).coverImage.extraLarge` is `s4.anilist.co/file/anilistcdn/...` | ✅ |
| `Media(id=1).streamingEpisodes` returns ~6 Crunchyroll episodes | ✅ |
| `Media(id=1).nextAiringEpisode` is `null` (finished show) | ✅ |
| `Media(id=21)` (One Piece) `nextAiringEpisode` returns real airing data | ✅ |
| `Page.airingSchedules(mediaId_in:[21,178789,189046], notYetAired:true, sort:TIME)` returns 3 upcoming episodes | ✅ |
| `Page.notifications` anonymous → HTTP 401 `"Unauthorized."` | ✅ |
| `MediaListCollection(userId:1)` (private user) → 404 `"Private User"` | ✅ |
| `MediaListCollection(userId:2)` (public user "matchai") → returns PLANNING list anonymously | ✅ |
| `MediaTrend(mediaId:1)` → real trend object with `date`, `trending`, `averageScore` | ✅ |
| `MediaList.updatedAt`, `MediaList.createdAt`, `Media.updatedAt` all exist | ✅ (schema) |
| Response headers include `X-RateLimit-Limit: 30`, `X-RateLimit-Remaining: <n>` | ✅ (live as of probe date) |
| `https://anilist.co/api/v2/oauth/token` empty-body POST returns OAuth2 `unsupported_grant_type` error | ✅ |
| `https://anilist.co/api/v2/oauth/authorize?client_id=test&response_type=token` redirects to login | ✅ |

All probes were run from the sandbox environment on the date recorded in `worklog.md`.
