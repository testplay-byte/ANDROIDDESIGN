# Decisions — Only-List

> Architectural decisions. Status: ✅ CONFIRMED (user answered) / 🟡 PROPOSED (awaiting) / 🔵 DEFERRED.
> New decisions get the next free D-NNN.

---

## Identity

## D-001 — App display name: **Only-List**
**Status:** ✅ CONFIRMED (user, session 2)

## D-002 — Package: **`com.confused.onlylist`**
**Status:** ✅ CONFIRMED (user, session 2)

## D-003 — Wrapper folder: **`ONLYLIST`** (at repo root)
**Status:** ✅ CONFIRMED (user, session 2)

## D-004 — ABIs: arm64-v8a + armeabi-v7a + x86_64
**Status:** ✅ CONFIRMED (user, session 1)

## D-005 — SDK: compileSdk=36, targetSdk=36, minSdk=26
**Status:** 🟡 PROPOSED (per R-4)

---

## Tech Stack

## D-010 — Language: Kotlin 2.x + Jetpack Compose
**Status:** ✅ CONFIRMED (only modern choice)

## D-011 — Design system: Custom AppTheme (NON-Material)
**Status:** ✅ CONFIRMED (user explicit). Detekt CI rule forbids `material3.*` imports in feature code.

## D-012 — Design language: **Midnight Coral** (dark + coral accent)
**Status:** ✅ CONFIRMED (user: "darker theme + coral theme"). See `APP/only-list/DESIGN-LANGUAGE.md`.

## D-013 — Local DB: Room
**Status:** 🟡 PROPOSED (per R-4 — KMP-stable, AI-friendly). Well-built, offline-compatible, each part editable.

## D-014 — Networking: Apollo Kotlin (AniList GraphQL) + Ktor 3 (Kitsu/Jikan REST)
**Status:** 🟡 PROPOSED (per R-4)

## D-015 — DI: Koin 4
**Status:** 🟡 PROPOSED (per R-4 — LLM-friendly)

## D-016 — Navigation: Navigation Compose
**Status:** 🟡 PROPOSED (per R-4 — Nav3 still alpha)

## D-017 — Images: Coil 3
**Status:** 🟡 PROPOSED (per R-4)

## D-018 — Charts: Vico (line/bar) + custom Canvas (radar/spider)
**Status:** 🟡 PROPOSED (per R-4)

## D-019 — Background work: WorkManager (backup + on-demand only — NO auto-polling)
**Status:** ✅ CONFIRMED (user: NO automatic polling; fetch only when user uses the app). WorkManager used ONLY for weekly backup.

## D-020 — Preferences: DataStore
**Status:** 🟡 PROPOSED

## D-021 — Fonts: bundle Inter + Sora + JetBrains Mono (all OFL, all weights, variable)
**Status:** ✅ CONFIRMED (user: "implement all the starter fonts properly + bold fonts"). Past bold-rendering issues → bundle ALL weights + use variable fonts. FontRegistry maps token+weight → FontFamily.

---

## AniList / Auth

## D-030 — AniList OAuth: Implicit Grant + custom-scheme redirect
**Status:** ✅ CONFIRMED (user: custom URI scheme, easier). Redirect: `com.confused.onlylist://anilist-auth`. Tokens are 1-year JWTs, no refresh, no PKCE.

## D-031 — AniList API client registration
**Status:** 🔵 DEFERRED (user: "not registered yet, will register later"). AniList auth flows can't be tested end-to-end until user registers a client + provides the Client ID. Public API works without it.

## D-032 — AniList rate limit: target 60 req/min (middle ground, editable)
**Status:** ✅ CONFIRMED (user: "work with a middle ground, maybe 60 req/min, keep easily editable later"). Currently degraded to 30/min; design for 60 with a single config constant.

## D-033 — AniList polling: NO automatic background polling
**Status:** ✅ CONFIRMED (user: "does not necessarily require automatically searching or pulling things even though the user is not using it"). Fetch ONLY when: user opens a screen needing data, user pull-to-refreshes, user triggers an action. No daily WorkManager fetch.

---

## AI Agent

## D-040 — Agent architecture: Cline backbone + Kilo Code advanced patterns
**Status:** ✅ CONFIRMED (user: "combination of both"). Per R-1 + R-6: Cline as structural backbone (layered modules, iterative coroutine loop, Room-backed checkpoint store) + Kilo Code's LLM-based auto-compaction, glob-based per-agent permissions, persistent plan files. Neither alone is sufficient.

## D-041 — Agent modules must be extractable into a separate app later
**Status:** ✅ CONFIRMED (user: "goal is to build a separate LLM agent application too"). Keep `:core:agent:core`, `:core:agent:llm`, `:core:agent:tools` as pure Kotlin (JVM-targetable). Android deps only at the edges. Future goal: dedicated agent app.

## D-042 — LLM providers: all Cline-compatible, OpenAI-compatible by default
**Status:** ✅ CONFIRMED (user: "all the providers which are available on cline... OpenAI-compatible ones by default"). User pastes their API key (Android Keystore, never logged, never in backups).

## D-043 — Approval mode: AUTO-APPROVE by default + undo + reset
**Status:** ✅ CONFIRMED (user: "auto-approved + undo button + reset button. Reset reverts everything to original state — UI, agent context, everything deleted"). Every `commit` snapshots to `design_snapshots` (cap 50) for one-tap undo. Settings → "Reset to defaults" wipes ALL agent state + design customizations + restores starter theme.

## D-044 — Cost guardrails: user-set cap (tokens or price), if off runs free
**Status:** ✅ CONFIRMED (user: "user given a cap option... when user selects cap, also given option to select capping level. If not turned on, continues"). Plus iteration cap (25/task) from CORE_RULES §11. Daily usage shown in Settings.

---

## Screens / Features

## D-050 — v1 screen set: Home, Library, Search, Airing, Details, Settings
**Status:** ✅ CONFIRMED (user). 6 screens. Bottom nav tabs: Home, Search, Airing, Library, Settings (5 tabs — Details is navigated). Profile is deferred (not in v1 list — user didn't mention it).

## D-051 — Design Studio: NOT in v1
**Status:** ✅ CONFIRMED (user: "doesn't need to be in the very first one... will be part of the agent system, implement later"). Manual design editing (§29.1 of old rules) is deferred. The agent is the only customization mechanism for now. v1 ships the starter Midnight Coral theme; customization comes with the agent (Phase 2-3).

## D-052 — Manga scope: tracker only, no reader
**Status:** ✅ CONFIRMED (user: "tracker only, won't be a reader system"). Same as anime — list/progress tracking, no in-app reading.

## D-053 — 3-way segmented controls
**Status:** ✅ CONFIRMED (user mentioned). Used for Anime/Manga toggle (Search, Library), status filters, sort options. Per DESIGN-LANGUAGE.md §7.5.

---

## Backup / Security

## D-060 — Backup: NOT password-protected by default; opt-in
**Status:** ✅ CONFIRMED (user: "won't be password protecting by default. If user decides to password protect, then... password"). If opted in, password required on restore (no recovery path). If not, backup is plaintext zip (convenient).

## D-061 — Crash reporting: YES ("Something went wrong" screen)
**Status:** ✅ CONFIRMED (user: "if app crashes... show 'Something went wrong' screen rather than just crashing"). Per CORE_RULES §19: global crash handler + ErrorActivity + copyable logs. No third-party crash reporting service (no Firebase/Sentry) for v1 — local crash handler only.

## D-062 — Analytics: none for v1
**Status:** ✅ CONFIRMED (implicit — user wants crash handling, not analytics). No analytics SDK. Revisit before publishing.

---

## Build / CI

## D-070 — CI: GitHub Actions only, never build locally
**Status:** ✅ CONFIRMED (CORE_RULES §7)

## D-071 — Convention plugins: deferred (inline build config for v1)
**Status:** 🟡 PROPOSED. Inline each module's `build.gradle.kts` for v1 (simpler, more likely to build on first CI run). Refactor to convention plugins in `build-logic/` in Phase 2 once CI is green.

---

## Research-confirmed findings (carried from R-1..R-7)

- **R-1 Cline:** Apache 2.0. Iterative `while(isActive)` coroutine. Drop bash/browser/stdio-MCP/shadow-git. Room-backed snapshots (cap 50).
- **R-2 AniList:** POST-only GraphQL. Implicit Grant. 30/min degraded (design for 60 per D-032). No subscriptions. `Media.idMal` → Jikan/Kitsu.
- **R-3 Kitsu+Jikan:** Kitsu = best per-episode thumbnails/synopses (CDN `media.kitsu.app` not `.io`). Jikan = filler flags + TZ-aware dates (single-ep endpoint fragile). Direct AniList↔Kitsu mapping via `mappings`.
- **R-4 Android:** Custom AppTheme (6 CompositionLocals), Room, Koin 4, Navigation Compose, Coil 3, Vico + Canvas.
- **R-5 ANI-KUTA design:** Floating pill bottom nav (label reveal via AnimatedVisibility), gradient-scrim scroll-blur (NOT RenderEffect, draw-phase, 60fps), pressScale modifier (graphicsLayer deferred, no ripple).
- **R-6 Kilo Code:** MIT fork of OpenCode. Auto-compaction (dedicated compaction model), glob permissions, plan files, native subagents. Combine with Cline backbone.
- **R-7 Modern design:** Midnight Coral theme — warm dark `#14110F`, coral `#FF6B5C`. Inter+Sora+JetBrains Mono. Skeleton+shimmer cache-first. Spring press feedback. Optimistic updates + undo toast.
