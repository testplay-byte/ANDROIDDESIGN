# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 5 — Phase 1 R-11 fixes + Phase 3 (COMPLETE)

**Phase:** 1 (frosted glass + header + OAuth fixes) ✅ + 3 (real data + ViewModels + Profile) ✅

### Done this session

#### Phase 1 fixes (user-reported issues)

1. **REAL Haze blur** (was just tint, no blur)
   - ROOT CAUSE: AppNavHost created a separate `bottomBarHazeState` with NO source composable marked `Modifier.haze()`. Each screen had its OWN hazeState. So the bottom bar's HazeState had nothing to sample.
   - FIX: Single shared `HazeState` per app. Each screen marks its LazyColumn with `Modifier.haze(sharedHazeState)`. The bottom bar + header consume via `hazeChild(sharedHazeState)`.

2. **AniList OAuth "unsupported_grant_type" fix**
   - ROOT CAUSE: We passed `redirect_uri=olink://anilist-auth` in the authorize URL. Per AniList's Implicit Grant docs, the URL should be ONLY `?client_id={id}&response_type=token`. The redirect_uri comes from the app's AniList developer settings.
   - FIX: `AniListConfig.authUrl()` no longer includes redirect_uri.

3. **Header improvements**
   - Title 1.5x bigger: `displayLarge` 30sp → 45sp.
   - Title STAYS BOLD always: removed the `lerp` on `fontWeight` (was causing the "bold → normal → bold again" flicker). Now lerps only `fontSize` + padding.
   - Full background behind header: the header Box always has `colors.background`.
   - Frosted glass scrim: Haze-backed, 0 → 0.85 alpha on scroll.

#### Phase 3 — Real data + ViewModels + Profile

1. **Coil image loading** (real cover images)
   - Added `coil-compose` 3.0.4 + `coil-network-okhttp` 3.0.4.
   - `MediaCard` + `MediaListItem`: `AsyncImage` loads real AniList cover URLs; falls back to color gradient.

2. **ViewModels** for Search, Library, Airing, Details
   - `SearchViewModel`: debounced query (400ms) → Room search Flow → AniList refresh.
   - `LibraryViewModel`: observes Room trending Flow + AniList refresh.
   - `AiringViewModel`: observes Room airing Flow (status=RELEASING) + AniList refresh.
   - `DetailsViewModel`: loads single media by ID + generates episode list (mock; Kitsu/Jikan in Phase 3.5).

3. **Real AniList data wired into all screens**
   - Home: real trending (was already done in Phase 2).
   - Search: real search results with "(live from AniList)" label.
   - Library: real trending data with "(live from AniList)" label.
   - Airing: real airing schedule (status=RELEASING media).
   - Details: real cover banner (Coil) + real metadata + episode list (mock).
   - Profile: mock stats (Phase 3.5 will use real Viewer data).

4. **Profile screen with radar chart**
   - Custom Canvas radar/spider chart (6 axes, 5 grid levels, data polygon + points).
   - Genre distribution visualization.
   - Top genres list with score bars.
   - Quick stats (Total, Episodes, Watching).
   - Profile header with avatar placeholder.
   - Navigation: Settings → Profile item navigates to Profile screen.

### CI builds
- Run #41: FAILURE (PaddingValues import)
- Run #42: ✅ (Phase 1 fixes)
- Run #43: ✅ (Coil + Search/Library VMs)
- Run #44: FAILURE (getAiring missing on repo + PaddingValues)
- Run #45: ✅ (Airing/Details VMs)
- Run #46: FAILURE (width import in Profile)
- Run #47: ✅ (Profile screen — commit `7b826f2`)
- APK artifact: 10.6MB

### What's built (Phase 3 deliverable)
- Real AniList trending data on Home + Library + Airing.
- Real AniList search results on Search (debounced).
- Real cover images via Coil on all cards + Details banner.
- Real AniList media metadata on Details (title, format, season, year, episodes, genres, description, score).
- Episode list on Details (mock — Kitsu/Jikan in Phase 3.5).
- Profile screen with radar chart + genre bars + quick stats.
- ViewModels for proper state management (Search, Library, Airing, Details).
- Settings → Profile navigation.
- AniList OAuth login flow (fixed — should work now).

### Deferred to Phase 3.5
1. Kitsu + Jikan episode metadata (real per-episode thumbnails, synopses, air dates).
2. Real AniList Viewer data on Profile (when authenticated) — real stats, real radar chart.
3. Improved logging screen with filtering.
4. AniList MediaListCollection (user's actual lists, not trending) when authenticated.

### Phase map
- **Phase 0** ✅: Planning / Setup / Research
- **Phase 1** ✅: Project scaffolding + design system + frosted glass + header animation + fonts
- **Phase 2** ✅: Data layer (Room + AniList + Kitsu/Jikan stubs + repositories)
- **Phase 3** ✅: Real data on all screens + ViewModels + Coil images + Profile with charts
- **Phase 3.5** (next): Kitsu/Jikan episode metadata + real Viewer stats + logging screen
- **Phase 4**: AI agent port + Design Studio
- **Phase 5**: Backup/restore + dynamic theming
- **Phase 6**: Polish (animations, charts, notifications, edge cases)

---

## How to read this file at session start

1. Read the top section (most recent session).
2. Read `decisions.md` (✅ confirmed vs 🟡 proposed vs 🔵 deferred).
3. Read `open-questions.md` (any new blocking questions?).
4. Grep `lessons-learned.md` for tags matching your current task type.
5. If the sandbox feels off, follow CORE_RULES §10 (re-clone from GitHub).
