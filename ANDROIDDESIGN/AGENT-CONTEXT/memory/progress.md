# Progress — ANDROIDDESIGN

> Living document. Update after every task. Read this FIRST at session start.
> Format: most recent session at the top.

---

## Session 1 — Planning / Setup / Research (IN PROGRESS)

**Date:** (session start)
**Phase:** 0 — Planning / Setup / Research
**Goal:** Set up the environment, understand requirements, research the key technologies, produce a refined ruleset + architecture + open-questions list. NOT building the app yet.

### Done this session
- ✅ Read the reference `CORE_RULES .md` (from the prior ANI-KUTA project) — treated as reference only.
- ✅ Set up local workspace at `/home/z/my-project/android-project/` (AGENT-CONTEXT, credentials, research, references, repo).
- ✅ Secured GitHub credentials at `/home/z/my-project/android-project/credentials/.git-credentials` (600 perms, outside the repo tree, never committed).
- ✅ Cloned `testplay-byte/ANDROIDDESIGN` repo (empty initial commit `267d2ec`).
- ✅ Configured git credential helper (no PAT in remote URLs or `.git/config`).
- ✅ Launched 4 parallel research sub-agents (R-1 Cline, R-2 AniList, R-3 Kitsu+Jikan, R-4 Android design system). All completed with verified findings.
- ✅ Wrote refined `CORE_RULES.md` (adapted from reference: dropped ANI-KUTA-specifics, added AI-agent/design-token/offline-first/backup/non-Material rules).
- ✅ Wrote `knowledge/architecture.md` (tech stack, module graph, data flow, agent integration, backup flow).
- ✅ Initiated shallow clone of Cline into `references/cline/` (background).

### In progress
- 🔄 Writing the open-questions list for user clarification.
- 🔄 Preparing the first GitHub push (planning artifacts).

### Next session (after user answers questions)
1. Read this file + `open-questions.md` + `decisions.md`.
2. Apply user's answers (update D-NNN statuses from PROPOSED → CONFIRMED, update placeholders in CORE_RULES §0).
3. If user confirmed app name + package: scaffold the Gradle project (`APP/ani-design/`).
4. Set up the GitHub Actions build workflow (`.github/workflows/build-apk.yml`).
5. Begin Phase 1: project skeleton + `:core:designsystem` starter (AppTheme + CompositionLocals + bundled fonts) + `:app` shell.

### Known doc debt
- (none yet)

### Phase map
- **Phase 0** (current): Planning / Setup / Research.
- **Phase 1** (next): Project scaffolding (Gradle, modules, CI, app shell, design-system starter).
- **Phase 2**: Data layer (Room schema, AniList client, repositories, offline-first wiring).
- **Phase 3**: Core screens (Home, Profile, Library, Search, Airing, Details).
- **Phase 4**: AI agent port + Design Studio UI + design-token tools.
- **Phase 5**: Backup/restore + dynamic theming (palette extraction).
- **Phase 6**: Polish (animations, charts, notifications, edge cases).
- (Phases are indicative, not committed — refine after user Q&A.)

---

## How to read this file at session start

1. Read the top section (most recent session) — that tells you where you are.
2. Read `decisions.md` for the architectural decisions (status: PROPOSED vs CONFIRMED).
3. Read `open-questions.md` for unresolved questions to the user.
4. Read `lessons-learned.md` (grep for tags matching your current task type).
5. If the sandbox feels off (missing files, weird errors), follow CORE_RULES §15 (re-clone).
