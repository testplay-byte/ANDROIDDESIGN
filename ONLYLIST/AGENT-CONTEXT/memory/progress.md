# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 3 — Phase 1 fixes + Phase 2 data layer (COMPLETE)

**Phase:** 1 (fixes) ✅ + 2 (data layer) ✅

### Done this session

#### Phase 1 fixes (user-reported bugs)
- ✅ FIX: Bottom nav at bottom (was at top) — `Modifier.align(Alignment.BottomCenter)`
- ✅ FIX: Selection reactive — `currentBackStackEntryAsState()` instead of one-shot read
- ✅ ADD: Mock data on all 6 screens (8 sample anime, GlassCards, grid/card layouts)
- ✅ ADD: Real bundled fonts (Inter + Sora + JetBrains Mono — variable, all weights)
- ✅ ADD: Frosted glass aesthetic (translucent gradient + shadow + border on BottomBar + GlassCard)
- ✅ ADD: Crash handler (OnlyListCrashHandler + ErrorActivity + "Something went wrong" screen)
- ✅ ADD: MediaCard + MediaListItem components for grid/list layouts

#### Phase 2 (data layer + network + offline-first)
- ✅ `:core:database` — Room with 5 entities (Media, Episode, MediaListEntry, AiringSchedule, MetadataSourceState) + 5 DAOs + OnlyListDatabase + DatabaseProvider
- ✅ `:core:network` — AniListGraphQLClient (Ktor POST), AniListAuthManager (OAuth Implicit Grant), AniListConfig (Client ID 48704), AniListQueries (trending/search/mediaById/viewer), KitsuClient (stub), JikanClient (stub)
- ✅ `:core:data` — MediaRepository (offline-first: Room Flow + AniList refresh + JSON parsing)
- ✅ AppContainer — simple DI (database + authManager + anilistClient + mediaRepository)
- ✅ AndroidManifest — deep link `olink://anilist-auth` + `launchMode=singleTask`
- ✅ MainActivity — handles OAuth redirect (parses token from URL fragment) + `startAniListAuth()` (Chrome Custom Tabs)
- ✅ HomeScreen — uses REAL AniList trending data via MediaRepository (offline-first: Room Flow + network refresh + mock fallback)

### CI builds
- Phase 1 fixes: 2 builds (1 failure — BasicText import, 1 success)
- Phase 2: 2 builds (1 failure — Room api() + title ref, 1 success)
- **Final: GREEN** (commit `b5053e7`, Run #33)

### What's built
A launching Android app with:
- Midnight Coral design system (dark + coral, frosted glass, real variable fonts)
- 6 screens with mock data + real AniList trending on Home
- Bottom nav at bottom (fixed) with reactive selection (fixed)
- Crash handler with error screen
- Room database (5 entities, offline-ready)
- AniList GraphQL client (real trending data fetch)
- AniList OAuth auth (deep link `olink://anilist-auth`)
- Offline-first MediaRepository

### Next (Phase 3)
1. Wire real AniList data into Search, Library, Airing, Details screens
2. Add "Link AniList Account" button in Settings (opens Chrome Custom Tabs)
3. Add ViewModels for proper state management
4. Fill in Kitsu + Jikan episode metadata (Details screen)
5. Add Profile screen with charts

### Phase map
- **Phase 0** ✅: Planning / Setup / Research
- **Phase 1** ✅: Project scaffolding + design system + 6 screens + CI
- **Phase 2** ✅: Data layer (Room + AniList + Kitsu/Jikan stubs + repositories)
- **Phase 3** (next): Wire real data into all screens + ViewModels + Kitsu/Jikan full impl
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
