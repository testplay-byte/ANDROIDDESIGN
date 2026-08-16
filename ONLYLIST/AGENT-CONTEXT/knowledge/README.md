# Knowledge — Quick-Reference Index

> Short summaries the agent reads on demand. NOT detailed research — that lives in `research/`.
> Each knowledge file links to the full research report or detailed doc.

## Index

| File | Topic | Deep doc |
|------|-------|----------|
| `architecture.md` | Tech stack, module graph, data flow, agent integration, backup flow | `research/R-4-android-design-system.md` |
| `cline-agent.md` | Cline porting notes: what to keep/drop/reimplement, module shape | `research/R-1-cline-agent.md` |
| `anilist-api.md` | AniList API: auth, rate limits, key queries, cache anchors | `research/R-2-anilist-api.md` |
| `kitsu-jikan-api.md` | Kitsu + Jikan: episode metadata, ID mapping, merge strategy | `research/R-3-kitsu-jikan.md` |
| `android-design-system.md` | Android patterns: Compose theming, Room, Koin, Coil, WorkManager | `research/R-4-android-design-system.md` |

## When to read what

- Starting any task → `architecture.md` (big picture) + `progress.md` (where we are).
- Working on the AI agent → `cline-agent.md`.
- Working on data layer / AniList → `anilist-api.md` + `android-design-system.md` (Room section).
- Working on episode metadata / Details screen → `kitsu-jikan-api.md`.
- Working on UI / design system → `android-design-system.md` + `APP/ani-design/DESIGN-LANGUAGE.md`.
