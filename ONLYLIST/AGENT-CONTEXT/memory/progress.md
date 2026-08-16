# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 6 — R-12 whole-page blur fix + Phase 3.5 (COMPLETE)

**Phase:** 1 (R-12 fix) ✅ + 3.5 (episode metadata + Viewer stats + logging) ✅

### Done this session

#### R-12 — Whole-page blur fix (user-reported)
- **R-12 sub-agent research**: diagnosed the root cause. `Modifier.haze()` does NOT blur the source's own content (verified in Haze 1.1.1 source — `HazeNode.kt` lines 77-102). The bug was in `CollapsibleHeader.kt`: the inner scrim Box used `Modifier.fillMaxSize()` inside a wrap-content parent Box. Per Compose docs, `fillMaxSize()` on a child of a wrap-content parent causes the parent to EXPAND to full screen height → the opaque `.background()` + `hazeChild` covered the whole screen.
- **Fix (Option A per R-12)**: applied `hazeChild` DIRECTLY to the outer Box. Removed the inner scrim Box + the opaque `.background()`. `HazeStyle.backgroundColor = colors.background` provides the visual backing. The outer Box now wraps to the title's measured height → blur only covers the header area.
- Reduced scrim alpha 0.85 → 0.7 (was too dark per user feedback).
- **Media tap navigation**: Home/Search/Airing/Library screens now accept `onMediaClick` callback → navigate to Details. Fixed `it.id` → `trendingMedia[index].id` / `media.id`.
- **Bottom nav on Details**: already hidden (checks `bottomNavRoutes` which excludes Details).

#### Phase 3.5 — Episode metadata + Viewer stats + logging

1. **EpisodeMetadataRepository** (`:core:data`)
   - Merges Kitsu (primary) + Jikan (filler/dates) per R-3 research.
   - Kitsu: thumbnails, synopses, titles (en/jp), duration (primary).
   - Jikan: air dates (TZ-aware), filler/recap flags (primary).
   - Append-never-overwrite: existing episodes only update missing fields.
   - ID mapping: AniList.id → Kitsu via mappings; AniList.idMal → Jikan.
   - Writes merged EpisodeEntity list to Room.

2. **DetailsViewModel**: uses EpisodeMetadataRepository
   - After media loads, fetches episodes via `episodeRepo.refreshEpisodes()`.
   - Reads merged episodes from Room (Flow.first()).
   - Falls back to placeholder episodes if all sources fail.

3. **Real AniList Viewer stats on Profile** (when authenticated)
   - ProfileViewModel: fetches Viewer query (name, avatar, anime/manga statistics).
   - AniListQueries.viewer: now includes `statistics { anime { count, episodesWatched, minutesWatched, meanScore }, manga { count, chaptersRead, volumesRead } }`.
   - ProfileUiState: Loading / Mock / Loaded sealed interface.
   - ProfileScreen: shows real avatar (Coil), name, anime count, episodes watched when authenticated; mock stats otherwise.

4. **Logs screen with filtering** (per CORE_RULES §20)
   - Logger enhanced: in-memory ring buffer (500 entries) of LogEntry.
   - LogEntry: timestamp, level (V/D/I/W/E), tag, message, stackTrace.
   - Logger.logBuffer StateFlow<List<LogEntry>> (observable).
   - Logger.clear() to wipe the buffer.
   - LogsScreen: shows recent log entries with level filter (All/Info/Warn/Error).
   - SegmentedControl for level filtering.
   - Clear button.
   - Monospace font for log entries.
   - Color-coded by level.
   - Settings → Logs item navigates to Logs screen.

5. **AniListGraphQLClient**: added Logger.d/w logging for query + errors.

### CI builds
- Run #49: ✅ (R-12 whole-page blur fix + navigation)
- Run #50: FAILURE (`:core:data` missing `:core:common` dep + async issues)
- Run #51: FAILURE (Pair destructuring ambiguity)
- Run #52: ✅ (sequential fetch)
- Run #53: FAILURE (`:core:network` missing `:core:common` dep)
- Run #54: FAILURE (`:app` missing serialization dep + LogsScreen smart cast)
- Run #55: FAILURE (LogsScreen smart cast still broken)
- Run #56: ✅ (Phase 3.5 complete — commit `aa01d8f`)
- APK artifact: 10.7MB

### What's built (Phase 3.5 deliverable)
- Real episode metadata from Kitsu + Jikan on Details screen (merged per R-3 strategy).
- Real AniList Viewer stats on Profile when authenticated (name, avatar, anime count, episodes watched).
- Logs screen with level filtering + clear button + color-coded entries.
- Whole-page blur fixed (only the header + bottom nav are frosted now).
- Media tap navigation works (Home/Search/Airing/Library → Details).
- Bottom nav hidden on Details screen.

### Phase map
- **Phase 0** ✅: Planning / Setup / Research
- **Phase 1** ✅: Project scaffolding + design system + frosted glass + header animation + fonts
- **Phase 2** ✅: Data layer (Room + AniList + Kitsu/Jikan stubs + repositories)
- **Phase 3** ✅: Real data on all screens + ViewModels + Coil images + Profile with charts
- **Phase 3.5** ✅: Kitsu/Jikan episode metadata + real Viewer stats + logging screen
- **Phase 4** (next): AI agent port + Design Studio
- **Phase 5**: Backup/restore + dynamic theming
- **Phase 6**: Polish (animations, charts, notifications, edge cases)

---

## How to read this file at session start

1. Read the top section (most recent session).
2. Read `decisions.md` (✅ confirmed vs 🟡 proposed vs 🔵 deferred).
3. Read `open-questions.md` (any new blocking questions?).
4. Grep `lessons-learned.md` for tags matching your current task type.
5. If the sandbox feels off, follow CORE_RULES §10 (re-clone from GitHub).
