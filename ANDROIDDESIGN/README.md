# ANDROIDDESIGN — AI-Powered Design System for Android

> An Android application whose primary purpose is an **infinitely-customizable, AI-driven design
> system**, with an **anime/manga tracker** (AniList + Kitsu + Jikan) as the test-bed application.
>
> **Status:** Phase 0 — Planning / Setup / Research (see `AGENT-CONTEXT/memory/progress.md`).

---

## What this is

This project builds a reusable **design system foundation** for Android apps. The design system is:

- **Non-Material** — a custom `AppTheme` with design tokens the user (and an embedded AI agent) can edit.
- **AI-powered** — a full-fledged AI agent (architecture ported from [Cline](https://github.com/cline/cline), Apache 2.0) lives inside the app. The user describes what they want; the agent modifies the design tokens, layout, component variants, and sorting rules. Every change is previewable, committable, and rollback-able.
- **Dynamic theming** — extract a palette from any image and build a theme on top.
- **Offline-first** — all data cached locally; works without network; reconciles on refresh.
- **Backup-able** — full data export/import (DB + design + AniList token, encrypted) with weekly auto-backup (one rolling copy).

The test-bed application is an **anime/manga tracker** using **AniList** (primary), **Kitsu** + **Jikan** (episode metadata). It is a tracker, NOT a streaming app.

---

## Repository layout

```
repo-root/
├── ANDROIDDESIGN/                     ← the single wrapper folder
│   ├── AGENT-CONTEXT/                 ← agent memory + rules (versioned)
│   │   ├── rules/CORE_RULES.md        ← the non-negotiable rules (read FIRST)
│   │   ├── memory/                    ← progress, decisions, lessons, open-questions, session-log
│   │   └── knowledge/                 ← quick-reference summaries
│   ├── APP/ani-design/                ← Android app (Gradle + Kotlin + Compose) — Phase 1+
│   ├── REFERENCES/                    ← external reference material (Cline, prior ruleset)
│   ├── research/                      ← deep research reports (R-1..R-N)
│   └── README.md                      ← this file
└── .github/workflows/                 ← GitHub Actions CI (repo-root level — GitHub constraint)
```

**Start here:** `ANDROIDDESIGN/AGENT-CONTEXT/memory/progress.md` — tells you where the project is.

---

## Tech stack (proposed — see `AGENT-CONTEXT/memory/decisions.md`)

| Concern | Choice |
|---------|--------|
| Language / UI | Kotlin 2.x + Jetpack Compose |
| Design system | Custom `AppTheme` (NON-Material) |
| Local DB | Room |
| Networking | Apollo Kotlin (AniList GraphQL) + Ktor 3 (Kitsu/Jikan REST) |
| DI | Koin 4 |
| Navigation | Navigation Compose |
| Images | Coil 3 |
| Charts | Vico + custom Compose Canvas (radar/spider) |
| Background | WorkManager |
| Fonts | Inter + Sora + JetBrains Mono (OFL, bundled) |
| AI agent | Ported Cline architecture (Kotlin) — 4 LLM providers |
| ABIs | arm64-v8a + armeabi-v7a + x86_64 |
| SDK | compileSdk=36, targetSdk=36, minSdk=26 |
| CI | GitHub Actions only (never build locally) |

---

## Build

**We do NOT build locally.** All builds happen via GitHub Actions (see `.github/workflows/`). The sandbox environment has no Android SDK and must not acquire one (CORE_RULES §8).

When the CI workflow exists (Phase 1), pushing to `main` or opening a PR triggers a build. APK + AAB artifacts are uploaded to the Actions run.

To install a debug build: download the APK from the latest green Actions run + sideload on your device.

---

## Development workflow

1. **Branch per feature/fix**: `feature/<name>`, `fix:<name>`, `docs/<name>`.
2. **Conventional Commits**: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`.
3. **CI green + user review** before merging to `main`.
4. **Never force-push to `main`.**
5. **Session-end**: all changes committed + pushed (the sandbox can clear randomly — CORE_RULES §15).

---

## Open questions (blocking Phase 1)

See `AGENT-CONTEXT/memory/open-questions.md` for the full list. The blocking ones:
- App name + package name + wrapper folder name.
- Starter design aesthetic.
- AniList OAuth client registration (client ID).
- LLM provider preference.
- v1 screen set + manga scope confirmation.

---

## License

TBD — pending user decision. The project itself is currently private (repo `testplay-byte/ANDROIDDESIGN`). Third-party attributions (Cline Apache 2.0, OFL fonts, etc.) will be listed in `APP/ani-design/DOCUMENTATION/licenses/` once the app is built.

---

## For AI agents starting a new session

Read in this order:
1. `AGENT-CONTEXT/rules/CORE_RULES.md` — the rules.
2. `AGENT-CONTEXT/memory/progress.md` — where the project is.
3. `AGENT-CONTEXT/memory/open-questions.md` — what's blocking.
4. `AGENT-CONTEXT/memory/decisions.md` — architectural context.
5. `AGENT-CONTEXT/knowledge/architecture.md` — the big picture.

If the sandbox feels off (missing files, weird errors), follow CORE_RULES §15 (re-clone).
