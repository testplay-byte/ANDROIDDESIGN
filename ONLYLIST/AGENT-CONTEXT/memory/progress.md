# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 4 — Phase 1 frosted glass + header + font fixes (COMPLETE)

**Phase:** 1 (R-9 fixes) ✅ — CI green

### Done this session

#### R-9 Research (3-topic deep-dive sub-agent)
- **Topic 1 — True frosted glass**: `Modifier.blur()` blurs a composable's OWN content, not what's behind. No public `Modifier.blurBehind()` in AndroidX. **Recommendation: Chris Banes' Haze library** — de-facto standard for backdrop blur in Compose. API 21+ (RenderEffect on 31+, RenderScript fallback on ≤30).
- **Topic 2 — Collapsible header**: use `lerp(displayLarge, titleLarge, fraction)` for title + single `animateFloatAsState(spring)` driving title size + padding + scrim alpha. Read `LazyListState.firstVisibleItemScrollOffset` directly (no `nestedScroll` needed).
- **Topic 3 — Variable font weights (THE BUG)**: `FontFamily(Font(R.font.inter_variable))` registers ONLY Normal weight. `FontWeight.Bold` in TextStyle is SILENTLY IGNORED. **Fix: register a SEPARATE Font per weight with `FontVariation.Settings(FontVariation.weight(N))`.**

#### R-10 Research (Haze version compatibility)
- Haze 1.7.2 (recommended by R-9) was compiled with Kotlin 2.2.0 — metadata version mismatch with our Kotlin 2.0.21.
- Haze 1.1.1 is the latest compatible with Kotlin 2.0.21 (1.2.0+ requires Kotlin 2.1+). API surface (haze + hazeChild + HazeStyle + HazeTint) is identical.

#### Fixes applied (3 commits, 3 CI iterations → green)
- ✅ Added Haze 1.1.1 dependency to `:core:designsystem` (via `api()` so it's exposed to `:app`).
- ✅ Rewrote `BottomBar.kt`: removed `Modifier.shadow(12.dp)` (was the "line" artifact) + gradient seam; replaced with `hazeChild` (real backdrop blur of content behind the bar).
- ✅ Rewrote `CollapsibleHeader.kt`: title SHRINKS (displayLarge 30sp → titleLarge 18sp via `lerp()`), padding animates (8→2/4→0), scrim is Haze-backed (real frosted blur, 0→0.85 alpha over 200dp scroll), single spring animation drives everything.
- ✅ Rewrote `FontRegistry.kt`: each weight registered as a SEPARATE Font with explicit `FontVariation.Settings(FontVariation.weight(N))`. Inter (400/500/600/700), Sora (600/700/800), JetBrains Mono (400/500/600). `@OptIn(ExperimentalTextApi::class)` per Aug 2026 docs.
- ✅ Updated all 6 screens: accept `bottomBarHazeState`, create own `hazeState` for LazyColumn + CollapsibleHeader, apply `Modifier.haze(hazeState)` to LazyColumn (the blur source).
- ✅ Updated `AppNavHost`: shared `bottomBarHazeState` passed to each screen.
- ✅ Settings: "Link AniList Account" button — tapping "AniList Account" when not logged in opens Chrome Custom Tabs for OAuth (`MainActivity.startAniListAuth`).

### CI builds
- Run #36: FAILURE — Haze 1.7.2 compiled with Kotlin 2.2.0 (metadata mismatch)
- Run #37: FAILURE — Haze 1.1.1 used `implementation` (not exposed to :app)
- Run #38: ✅ SUCCESS — Haze via `api()` (commit `d5352e1`)
- APK artifact: 10.2MB (up from 7.9MB — Haze + real variable font files)

### What's fixed (user-reported bugs)
1. ✅ "Line on the bottom nav" — `Modifier.shadow` removed; Haze provides depth without a ring artifact.
2. ✅ "Frosted glass not achieved" — real backdrop blur via Haze (content behind is now blurred, not just dimmed).
3. ✅ "Home heading not bold" — variable font weights now render correctly via `FontVariation.Settings`.
4. ✅ "Home heading doesn't shrink + move up" — `lerp()` animation on title style + padding.
5. ✅ "No top background to home title" — Haze-backed scrim fades in on scroll.
6. ✅ "Gradient unnecessary" — gradient replaced with Haze frosted blur.
7. ✅ "Link AniList account" — Settings button opens OAuth in Chrome Custom Tabs.

### Next (Phase 3)
1. Wire real AniList data into Search, Library, Airing, Details screens
2. Add ViewModels for proper state management
3. Details screen — per-episode metadata (Kitsu + Jikan)
4. Profile screen with charts (radar/spider)
5. Improve logging with filtering

### Phase map
- **Phase 0** ✅: Planning / Setup / Research
- **Phase 1** ✅: Project scaffolding + design system + 6 screens + CI + frosted glass + header animation + real fonts
- **Phase 2** ✅: Data layer (Room + AniList + Kitsu/Jikan stubs + repositories)
- **Phase 3** (next): Wire real data into all screens + ViewModels + Kitsu/Jikan full impl + Profile + logging
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
