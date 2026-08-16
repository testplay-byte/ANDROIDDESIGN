# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 2 — Answers applied + Phase 1 build (COMPLETE)

**Date:** (session 2)
**Phase:** 1 — Project scaffolding ✅ COMPLETE (CI green)

### Done this session
- ✅ Launched 3 parallel research sub-agents (R-5 ANI-KUTA design, R-6 Kilo Code agent, R-7 modern Android design).
- ✅ Renamed wrapper folder `ANDROIDDESIGN` → `ONLYLIST` (git mv, history preserved).
- ✅ Rewrote `CORE_RULES.md` concise (704 → 242 lines, 66% reduction).
- ✅ Applied all user answers to `decisions.md` (D-001..D-071).
- ✅ Wrote `DESIGN-LANGUAGE.md` (Midnight Coral — dark #14110F + coral #FF6B5C).
- ✅ Scaffolded the Gradle project: 3 modules (`:app` + `:core:designsystem` + `:core:common`).
- ✅ Built the design system: AppTheme (5 CompositionLocals), Color, Typography, Shape, Motion, Spacing, FontRegistry, BottomBar (floating pill + animated label reveal), CollapsibleHeader (gradient-scrim scroll-blur), SegmentedControl (3-way toggle), SkeletonBox (shimmer), pressScale modifier.
- ✅ Built 6 placeholder screens: Home, Library, Search, Airing, Details, Settings.
- ✅ Built the app shell: OnlyListApplication, MainActivity, AppNavHost (Navigation Compose).
- ✅ Custom vector icons (5 tabs, non-Material), adaptive launcher icon (coral bg + OL monogram).
- ✅ GitHub Actions `build-apk.yml` workflow.
- ✅ V-2 code review (sub-agent): GREEN — no blocking compile errors.
- ✅ **CI BUILD GREEN** (commit `80f3f09`, Run #29). Debug APK (7.6MB) uploaded as artifact.

### The build-debugging journey (9 CI iterations)
The build failed 8 times before going green. Root cause: **wrong import packages**.
- `BasicText` is in `androidx.compose.foundation.text` (NOT `androidx.compose.foundation`).
- `graphicsLayer` is in `androidx.compose.ui.graphics` (NOT `androidx.compose.ui`).
- `Image` + `Modifier` resolved fine because they ARE in the root packages — which made the issue look like a dependency resolution problem when it was actually just wrong imports.
- Found via R-8 sub-agent research (verified against the actual AAR's classes.jar contents).
- Lesson logged in `lessons-learned.md`.

### What's built (Phase 1 deliverable)
A launching Android app that demonstrates the full Midnight Coral design system:
- App launches with edge-to-edge warm dark background (#14110F).
- Floating pill bottom nav with 5 tabs (Home/Search/Airing/Library/Settings) + animated label reveal.
- Collapsible header with gradient-scrim scroll-blur on each screen.
- 6 placeholder screens with themed content (skeletons, segmented controls, cards).
- Custom non-Material components throughout (BasicText, no material3 imports).
- Logger wrapper.
- CI builds a debug APK on every push to main.

### Next (Phase 2)
1. Data layer: Room schema (media, episode, media_list_entry, airing_schedule, etc.).
2. Network layer: AniList (Apollo Kotlin GraphQL), Kitsu + Jikan (Ktor REST).
3. Offline-first repositories.
4. Real fonts bundled (Inter + Sora + JetBrains Mono — currently using system fonts as placeholder).

### Phase map
- **Phase 0** ✅: Planning / Setup / Research.
- **Phase 1** ✅: Project scaffolding + design system + 6 placeholder screens + CI green.
- **Phase 2** (next): Data layer (Room, AniList/Kitsu/Jikan, repositories).
- **Phase 3**: Core screens with real data.
- **Phase 4**: AI agent port + Design Studio.
- **Phase 5**: Backup/restore + dynamic theming.
- **Phase 6**: Polish (animations, charts, notifications, edge cases).

---

## How to read this file at session start

1. Read the top section (most recent session).
2. Read `decisions.md` (✅ confirmed vs 🟡 proposed vs 🔵 deferred).
3. Read `open-questions.md` (any new blocking questions?).
4. Grep `lessons-learned.md` for tags matching your current task type.
5. If the sandbox feels off, follow CORE_RULES §10 (re-clone from GitHub).
