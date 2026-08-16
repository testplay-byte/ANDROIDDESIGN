# Session Log — ONLYLIST

> Append-only log of sessions. One section per session. Most recent at top.

---

## Session 1 — Planning / Setup / Research

**Phase:** 0 (Planning)
**Started:** (session start)
**Ended:** (in progress)

### Goals
- Set up the environment (workspace, credentials, repo clone).
- Understand the user's requirements (AI-powered design system + anime tracker + Cline agent port + GitHub-centric workflow).
- Research the key technologies (Cline, AniList, Kitsu, Jikan, Android design system patterns).
- Produce a refined CORE_RULES (adapted from the ANI-KUTA reference).
- Produce an architecture document.
- Produce a comprehensive open-questions list for the user.
- Push the planning artifacts to GitHub.

### What was done
1. Read the reference `CORE_RULES .md` (499 lines, from prior ANI-KUTA project). Identified what to keep, adapt, drop.
2. Created local workspace `/home/z/my-project/android-project/` with subfolders: AGENT-CONTEXT/{memory,rules,knowledge}, credentials, research, references, repo.
3. Stored GitHub PAT securely at `credentials/.git-credentials` (600 perms, outside repo tree). Configured git credential helper.
4. Cloned `testplay-byte/ONLYLIST` repo (empty, single initial commit `267d2ec` on `main`).
5. Launched 4 parallel research sub-agents (general-purpose type):
   - R-1: Cline agent framework (architecture, portability) → `research/R-1-cline-agent.md` (64KB, 1310 lines).
   - R-2: AniList GraphQL API (auth, rate limits, queries) → `research/R-2-anilist-api.md` (60KB).
   - R-3: Kitsu + Jikan APIs (episode metadata, merge strategy) → `research/R-3-kitsu-jikan.md` (41KB).
   - R-4: Android design system + architecture patterns → `research/R-4-android-design-system.md` (84KB).
6. Wrote refined `CORE_RULES.md` (36 sections: kept timeless mindset rules, adapted project-specific rules, dropped ANI-KUTA-specifics like MPV/DASHBOARD/SQLDelight/ARM-only, added 8 new sections for AI agent / design tokens / multi-source merge / offline-first / backup / dynamic theming / non-Material enforcement / bundled fonts).
7. Wrote `knowledge/architecture.md` (tech stack table, module graph, data flow diagrams, agent integration, backup flow).
8. Wrote `memory/progress.md`, `memory/decisions.md` (D-001..D-019, mostly PROPOSED), `memory/lessons-learned.md` (seeded with insights from research).
9. Initiated shallow clone of Cline into `references/cline/` for local reference (background).
10. Prepared the open-questions list for the user.

### Artifacts produced
- `AGENT-CONTEXT/rules/CORE_RULES.md` — the refined ruleset.
- `AGENT-CONTEXT/knowledge/architecture.md` — architecture synthesis.
- `AGENT-CONTEXT/memory/progress.md` — current state.
- `AGENT-CONTEXT/memory/decisions.md` — D-001..D-019.
- `AGENT-CONTEXT/memory/lessons-learned.md` — seeded.
- `AGENT-CONTEXT/memory/open-questions.md` — questions for the user.
- `AGENT-CONTEXT/memory/session-log.md` — this file.
- `research/R-1-cline-agent.md` — Cline deep research.
- `research/R-2-anilist-api.md` — AniList API research.
- `research/R-3-kitsu-jikan.md` — Kitsu + Jikan research.
- `research/R-4-android-design-system.md` — Android design system research.
- `credentials/README.md` + `credentials/secrets.md` — credential docs (local only).
- `REFERENCES/` folder + Cline shallow clone (in progress).

### What's next (after user answers open-questions)
- Apply user's answers: update D-NNN statuses, fill CORE_RULES §0 placeholders.
- Begin Phase 1: scaffold the Gradle project + CI workflow + `:core:designsystem` starter.

### Lessons this session
(see `lessons-learned.md` — 19 entries seeded)

### Push status
- ✅ Committed as `3106091` ("docs(phase-0): planning, research, refined CORE_RULES, architecture, open-questions").
- ✅ Pushed to `main` at `testplay-byte/ONLYLIST`.
- ✅ Secret scan: 0 occurrences of the actual GitHub PAT in the commit. (The "client_secret"/"api_key" hits in the pre-commit scan were documentation mentions, not secret values.)
- ✅ Repo structure: 23 files, ~512KB, single wrapper folder `ONLYLIST/` at repo root, `.github/workflows/` at repo root (GitHub constraint).

### Cline full-source clone
- Attempted a shallow clone into `references/cline/` (local sandbox only). Stalled at 116KB (large repo + sandbox network limits).
- Cleaned up. Deferred to the porting phase (Phase 4) — at that point we'll either re-clone or add Cline as a git submodule in `REFERENCES/cline/`.
- The R-1 research report (64KB, 1310 lines) is the distilled analysis and is sufficient for planning.
