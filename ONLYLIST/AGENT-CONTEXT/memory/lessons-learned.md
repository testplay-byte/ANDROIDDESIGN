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

## Compose API / Imports

- [MISTAKE] `BasicText` is in `androidx.compose.foundation.TEXT` package, NOT `androidx.compose.foundation`. The correct import is `import androidx.compose.foundation.text.BasicText`. (source: R-8 research, session 2 — verified against the actual foundation-android-1.7.0.aar classes.jar)
- [MISTAKE] `graphicsLayer` (the Modifier extension) is in `androidx.compose.ui.GRAPHICS` package, NOT `androidx.compose.ui`. The correct import is `import androidx.compose.ui.graphics.graphicsLayer`. (source: R-8 research, session 2 — verified against ui-android-1.7.0.aar classes.jar)
- [INSIGHT] When a symbol from an artifact is unresolved but ANOTHER symbol from the SAME artifact resolves (e.g., `Image` resolves but `BasicText` doesn't, both in compose-foundation), the issue is the IMPORT PACKAGE, not dependency resolution. Check if the unresolved symbol is in a SUBPACKAGE. (source: self, session 2 — this took 8 CI iterations to diagnose because it looked like a dependency issue)
- [PATTERN] Before assuming a Compose dependency failed to resolve, verify the EXACT package path. Compose uses deep package hierarchies: `androidx.compose.foundation` (root: Image, Canvas) vs `androidx.compose.foundation.text` (BasicText, BasicTextField) vs `androidx.compose.foundation.layout` (Box, Row, Column, LazyColumn). Same for `androidx.compose.ui` (root: Modifier) vs `androidx.compose.ui.graphics` (Color, Brush, graphicsLayer) vs `androidx.compose.ui.unit` (Dp, sp). (source: R-8 research, session 2)
- [PATTERN] A sub-agent (R-8, haiku model, 77K tokens) resolved in ~30 seconds what took 8 CI iterations to diagnose manually. When stuck on an "unresolved reference" that looks like a dependency issue, delegate to a sub-agent to verify the EXACT package path against the published artifact. (source: self, session 2)

## Haze / Library compatibility

- [PATTERN] When adding a new library, check the Kotlin version it was compiled with. A library compiled with Kotlin 2.2.0 will fail with "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.2.0, expected version is 2.0.0" if the project uses Kotlin 2.0.x. Check the library's POM `kotlin-stdlib` version on Maven Central before adding. (source: R-10 research + CI Run #36 failure, session 4)
- [MISTAKE] Haze 1.7.2 was recommended by R-9 research but it requires Kotlin 2.2.0. Our project uses Kotlin 2.0.21. Had to downgrade to Haze 1.1.1 (latest 2.0-compatible). The R-9 research should have checked Kotlin compatibility — not just the API. (source: CI Run #36 failure, session 4)
- [PATTERN] Libraries used in the public API of a module (e.g. HazeState passed to composables) must be `api()` not `implementation()` in that module's build.gradle.kts. `implementation` doesn't expose transitively — :app couldn't see HazeState. Same pattern as the Room fix from session 3. (source: CI Run #37 failure, session 4)
