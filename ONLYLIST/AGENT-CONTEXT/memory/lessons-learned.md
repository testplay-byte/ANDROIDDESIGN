# Lessons Learned — ONLYLIST

> Self-learning log. Format: `- [TAG] lesson (source: <task-id or "self">, <date>)`
> Tags: MISTAKE (you did wrong), CORRECTION (user fixed you), INSIGHT (you realized), PATTERN (recurring).
> Dedup before adding: grep for the keyword. Don't log the same lesson twice.

---

## Environment / Sandbox

- [PATTERN] The sandbox environment clears out randomly. Push to GitHub frequently — not just at session end. (source: user warning, session 1)
- [PATTERN] Credentials must live OUTSIDE the git repo tree so they can never be accidentally committed. Use `/home/z/my-project/android-project/credentials/` with 600 perms. (source: self, session 1)
- [INSIGHT] The sandbox is a Next.js web environment, but we're building an Android app. The sandbox is for planning/coordination/research ONLY — the actual Android build happens on the user's machine via GitHub Actions. Never attempt to install Android SDK/JDK in the sandbox. (source: user + CORE_RULES reference §8, session 1)

## AniList API

- [INSIGHT] AniList's documented rate limit is 90 req/min but it is CURRENTLY DEGRADED to 30 req/min (verified live: `X-RateLimit-Limit: 30`). Design the client for 30, not 90. (source: R-2 research, session 1)
- [INSIGHT] AniList GraphQL is POST-only — GET returns 404. HTTP-layer caching is impossible; the app MUST cache in SQLite/Room. (source: R-2 research, session 1)
- [INSIGHT] AniList auth tokens are 1-year JWTs with NO refresh tokens and NO PKCE. Use Implicit Grant + custom-scheme redirect to avoid shipping client_secret in the APK. (source: R-2 research, session 1)
- [INSIGHT] AniList has NO GraphQL subscriptions/WebSocket — `__schema.subscriptionType` is null. Must poll. Use `Viewer.unreadNotificationCount` as a cheap pre-check. (source: R-2 research, session 1)
- [INSIGHT] AniList `Page(page, perPage)` silently caps perPage at 50. `PageInfo.total`/`lastPage` are inaccurate — only trust `hasNextPage`. (source: R-2 research, session 1)

## Kitsu + Jikan

- [INSIGHT] Kitsu's image CDN is `media.kitsu.app` NOT `media.kitsu.io` (the .io URL 404s as of this research). (source: R-3 research, session 1)
- [INSIGHT] Jikan's per-episode synopsis endpoint (`/v4/anime/{id}/episodes/{episode}`) is fragile — 504s frequently. Use only as lazy fallback when Kitsu synopsis is missing; never block the UI on it. (source: R-3 research, session 1)
- [INSIGHT] AniList↔Kitsu ID mapping is DIRECT via `mappings?filter[externalSite]=anilist/anime` — no need to go through MAL id. Cache the kitsuId forever. (source: R-3 research, session 1)
- [INSIGHT] Jikan's `/episodes` (list) endpoint returns ALL episodes in one page (no pagination needed) but has NO synopsis/thumbnail/duration. The single-episode endpoint has synopsis+duration but is fragile. (source: R-3 research, session 1)

## Cline / AI agent porting

- [INSIGHT] Cline is Apache 2.0 — we can port ideas/architecture freely; ship a NOTICES screen. Do NOT translate source files verbatim — write idiomatic Kotlin. (source: R-1 research, session 1)
- [INSIGHT] Cline's `recursivelyMakeClineRequests` is a recursive ReAct loop with a teardown note recommending conversion to iterative. We use `while(isActive)` Kotlin coroutines from day one. (source: R-1 research, session 1)
- [INSIGHT] Cline's shadow-git checkpoint system is unviable on Android (no subprocess, sandboxed FS). Use a Room-backed `design_snapshots` table capped at 50 entries. (source: R-1 research, session 1)
- [INSIGHT] Cline's stdio MCP transport is impossible on Android (no subprocess spawning). Only HTTP-based MCP servers are portable — defer MCP entirely to post-MVP. (source: R-1 research, session 1)

## Architecture / Design

- [INSIGHT] Material 3 locks us into 29 fixed color roles. A custom `AppTheme` with 6 CompositionLocals gives the AI agent a free-form token surface. Enforce with a Detekt rule forbidding `material3.*` imports in feature code. (source: R-4 research, session 1)
- [INSIGHT] Room over SQLDelight for this project — Room has more LLM training data (AI-agent-friendly) and better Compose/Flow ergonomics. The prior ANI-KUTA project used SQLDelight; we diverge. (source: R-4 research, session 1)
- [INSIGHT] Koin 4 over Hilt — runtime DI is more LLM-friendly than Hilt's KSP graph. Recover safety with a CI resolvability test. (source: R-4 research, session 1)
- [INSIGHT] Navigation Compose (NOT Nav3) — Nav3 is still alpha; the prior project tried it and removed it. (source: R-4 + CORE_RULES reference §8, session 1)
- [PATTERN] When the user says "the design language which I am interested in going with is what I require: a simple starting foundation" — they want the starter design to be intentionally simple so users customize it via AI. Do NOT over-design the starter; do NOT use Material. (source: user, session 1)

## User communication

- [PATTERN] The user dictates via speech-to-text. "rapper folder" = "wrapper folder". Interpret obvious homophones from context; ask if unclear. (source: CORE_RULES reference §13 + user message, session 1)
- [INSIGHT] The user values quality over speed explicitly. Do not rush planning. A thorough plan + question list now saves rework later. (source: user, session 1)
- [INSIGHT] The user wants the AI agent to be a "full-fledged agent tech system" not an "off-the-shelf AI system." This means a real agent loop (ReAct), tool surface, context management, approval flow — not just an LLM call. (source: user, session 1)
