# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 2 — Answers applied + Phase 1 build (IN PROGRESS)

**Date:** (session 2)
**Phase:** 0 → 1 transition (planning answers applied, now building the Gradle project + CI + core screens)

### Done this session
- ✅ Launched 3 parallel research sub-agents (R-5 ANI-KUTA design, R-6 Kilo Code agent, R-7 modern Android design). All completed with verified findings.
- ✅ Renamed wrapper folder `ANDROIDDESIGN` → `ONLYLIST` (via `git mv`, history preserved). Updated all references in 14 files.
- ✅ Rewrote `CORE_RULES.md` to be concise (704 → 242 lines, 66% reduction). Kept only what needs detail; cut verbose explanations.
- ✅ Applied all user answers to `decisions.md` (D-001..D-071, mostly ✅ CONFIRMED).
- ✅ Wrote `APP/only-list/DESIGN-LANGUAGE.md` (Midnight Coral theme — exact hex values, fonts, shapes, motion, component patterns: floating pill bottom nav, gradient-scrim scroll-blur, pressScale, 3-way segmented control, skeleton+shimmer).
- ✅ Updated `open-questions.md` — all session-1 blocking answered; 4 new non-blocking (Q-101..Q-104); 7 deferred.

### In progress
- 🔄 Building the Gradle project scaffold (settings, version catalog, build files, app shell, core modules, feature modules).
- 🔄 Writing the GitHub Actions `build-apk.yml` workflow.
- 🔄 Bundling fonts (Inter + Sora + JetBrains Mono).

### Next (after scaffold pushes green)
1. Iterate on CI until the debug APK builds green.
2. Phase 2: data layer (Room schema, AniList client, repositories, offline-first wiring).
3. Phase 3: fill in the 6 core screens (Home, Library, Search, Airing, Details, Settings) with real data.
4. Phase 4: AI agent port + Design Studio.
5. Phase 5: backup/restore + dynamic theming.

### Phase map
- **Phase 0** ✅ (done session 1): Planning / Setup / Research.
- **Phase 1** (current): Project scaffolding (Gradle, modules, CI, app shell, design-system starter, 6 placeholder screens).
- **Phase 2**: Data layer (Room, AniList/Kitsu/Jikan clients, repositories).
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

---

## Session 1 — Planning / Setup / Research (COMPLETE)

See `session-log.md` for the full session-1 log. Summary:
- Set up workspace + credentials.
- Researched Cline (R-1), AniList (R-2), Kitsu+Jikan (R-3), Android design (R-4).
- Wrote refined CORE_RULES + architecture + memory system.
- V-1 review sub-agent (APPROVE WITH MINOR FIXES) → incorporated 5 MAJOR fixes.
- Pushed to GitHub (commits `3106091`, `ba86f9a`, `017c05c`).
