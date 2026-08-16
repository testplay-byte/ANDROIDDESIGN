# Decisions — ANDROIDDESIGN

> Architectural decisions. Each has a status: PROPOSED (awaiting user confirmation) or CONFIRMED.
> New decisions get the next free D-NNN. Never reuse a number.

---

## D-001 — Language: Kotlin 2.x + Jetpack Compose
**Status:** PROPOSED
**Source:** user requirement + R-4 research
**Rationale:** Kotlin is the only modern choice for Android. Compose is the modern UI toolkit. No alternatives considered.
**Trade-offs:** none.

## D-002 — Design system: Custom AppTheme (NON-Material)
**Status:** PROPOSED
**Source:** user requirement (explicitly no Material) + R-4 Q1
**Rationale:** Material 3 locks us into 29 fixed color roles. A custom `AppTheme` with 6 CompositionLocals (colors/typography/shapes/motion/spacing/elevation) gives the AI agent a free-form token surface to edit. Reuse only low-level primitives (Surface, Ripple, ModalBottomSheet) under the hood.
**Trade-offs:** more upfront work (we build Button/Card/TopAppBar ourselves) but full customizability. Enforced by a Detekt CI rule forbidding `material3.*` imports in feature code.

## D-003 — Local DB: Room (not SQLDelight)
**Status:** PROPOSED
**Source:** R-4 Q8
**Rationale:** Room is KMP-stable as of 2.7+, has more LLM training data (AI-agent-friendly), better Compose/Flow ergonomics, official tooling. SQLDelight is fine but the AI agent will write more correct Room code on average. The prior ANI-KUTA project used SQLDelight; we diverge.
**Trade-offs:** Room's runtime annotation processing is slightly slower than SQLDelight's codegen, but for our scale (cache tables, not a hot path) irrelevant.

## D-004 — Networking: Apollo Kotlin (AniList GraphQL) + Ktor 3 (Kitsu/Jikan REST)
**Status:** PROPOSED
**Source:** R-4 Q15 + R-2 (AniList is GraphQL, POST-only) + R-3 (Kitsu JSON:API, Jikan REST)
**Rationale:** Apollo Kotlin gives codegen + normalized cache for AniList GraphQL. Ktor 3 is the modern multiplatform HTTP client for the REST sources. A shared `:core:network:common` module provides rate-limiter + retry + etag.
**Trade-offs:** two networking stacks (Apollo + Ktor) is more to learn, but each is best for its transport.

## D-005 — DI: Koin 4 (not Hilt)
**Status:** PROPOSED
**Source:** R-4 Q13
**Rationale:** Koin 4 is runtime DI but vastly more LLM-friendly (Hilt's annotation processing + KSP graphs are hard for AI agents to write correctly). Recover compile-time safety with a `KoinTest` resolvability check in CI.
**Trade-offs:** lose compile-time DI graph validation; mitigate with a CI test that resolves every module.

## D-006 — Navigation: Navigation Compose (not Nav3)
**Status:** PROPOSED
**Source:** R-4 Q14 + CORE_RULES reference §8 (Nav3 was tried + removed in the prior project)
**Rationale:** Navigation Compose is stable + well-documented. Nav3 is still alpha. Voyager is a contender but adds a third-party dependency for marginal benefit.
**Trade-offs:** Navigation Compose's type-safety story is improving but not perfect; acceptable.

## D-007 — Images: Coil 3
**Status:** PROPOSED
**Source:** R-4 Q11
**Rationale:** Coil 3 is Compose-native, KMP, actively maintained. 250MB disk cache default. Placeholder color from AniList's `MediaCoverImage.color` (hex tint).
**Trade-offs:** none significant.

## D-008 — Charts: Vico (line/bar) + custom Canvas (radar/spider)
**Status:** PROPOSED
**Source:** R-4 Q17 + user requirement (spider/radar charts on profile)
**Rationale:** Vico is the modern Compose charts library for line/bar. It doesn't ship radar — we build a custom Compose Canvas radar/spider chart (animated with `Animatable`).
**Trade-offs:** maintaining a custom radar chart (~150 lines) is acceptable for the visual control.

## D-009 — Background work: WorkManager
**Status:** PROPOSED
**Source:** R-4 Q19 + user requirement (weekly auto-backup, airing-schedule polling)
**Rationale:** WorkManager is the Android-standard for deferrable background work. Constraints: charging + battery-not-low + unmetered (for backup). Airing-schedule poll: daily.
**Trade-offs:** none.

## D-010 — Preferences: DataStore (Preferences DataStore)
**Status:** PROPOSED
**Source:** R-4 (implicit)
**Rationale:** DataStore is the modern replacement for SharedPreferences. Async, typed, no blocking reads.
**Trade-offs:** none.

## D-011 — Fonts: bundle Inter + Sora + JetBrains Mono (all OFL)
**Status:** PROPOSED
**Source:** user requirement (bundle fonts to avoid runtime issues) + R-4 Q5
**Rationale:** Inter (body), Sora (display), JetBrains Mono (numbers/mono). All OFL-licensed. Variable fonts where available (~1.8MB total APK impact). Bundled in `res/font/`. `FontRegistry` maps token keys → FontFamily so the AI agent can swap families via token edits.
**Trade-offs:** ~1.8MB APK size; acceptable.

## D-012 — AI agent: port Cline's architecture to Kotlin
**Status:** PROPOSED
**Source:** user requirement (named Cline explicitly) + R-1 research
**Rationale:** Cline is Apache 2.0 — port ideas, not source verbatim. Module shape: `:core:agent:core` (loop + context), `:core:agent:llm` (4 providers: Anthropic, OpenAI, OpenRouter, Gemini, + OpenAI-compatible generic), `:core:agent:tools` (design-token tools), `:core:agent:permissions` (approval gateway). Iterative `while(isActive)` coroutine (not recursive like Cline). Room-backed snapshot store (not shadow-git). JSON-Patch (RFC 6902) for token edits; SEARCH/REPLACE for free-form text. Drop: bash, browser, stdio MCP, shadow-git.
**Trade-offs:** significant engineering effort (~2-3 phases); the agent is the differentiating feature, worth it.

## D-013 — CI: GitHub Actions only, never build locally
**Status:** PROPOSED
**Source:** CORE_RULES reference §8 + user requirement ("utilize GitHub for most of the things")
**Rationale:** The sandbox has no Android SDK and must not acquire one. GitHub Actions builds the APK + AAB. Caching via `gradle/actions/setup-gradle@v4`.
**Trade-offs:** slower feedback loop (push → CI → read annotations); mitigate with careful pre-push code review + sub-agent review.

## D-014 — ABIs: arm64-v8a + armeabi-v7a + x86_64
**Status:** CONFIRMED
**Source:** user (this session) — approved x86_64 for emulator
**Rationale:** ARM for physical devices, x86_64 for the Android emulator (Hyper-V now enabled on user's PC). No ARM translation. Ship AAB to Play + universal APK for sideload.
**Trade-offs:** three ABIs = 3x native libs in universal APK; for a Compose app with minimal native code, impact is ~1MB. Acceptable.

## D-015 — SDK: compileSdk=36, targetSdk=36, minSdk=26
**Status:** PROPOSED
**Source:** R-4 Q25
**Rationale:** compileSdk=36 for latest Compose BOM + future-proofing. targetSdk=36 (Play requires by Aug 2026). minSdk=26 (~98% device coverage, gives us modern Java 8 time APIs + decent SQLite).
**Trade-offs:** minSdk 26 drops ~2% of oldest devices; acceptable for a new app.

## D-016 — Backup crypto: Android Keystore + optional user passphrase (PBKDF2)
**Status:** PROPOSED
**Source:** user requirement (backup must include AniList token + design) + R-4 Q18
**Rationale:** Default: AniList token encrypted with a Keystore-wrapped key (device-bound, not portable). Optional: user sets a passphrase → PBKDF2 derives a key → portable across devices. The user picks at backup time.
**Trade-offs:** Keystore-only backups are non-portable; passphrase backups require the user to remember the passphrase.

## D-017 — Auth: AniList OAuth2 Implicit Grant + custom-scheme redirect
**Status:** PROPOSED
**Source:** R-2 research
**Rationale:** AniList doesn't support PKCE and doesn't issue refresh tokens. Implicit Grant avoids shipping a `client_secret` in the APK. Custom scheme redirect (`<applicationId>://anilist-auth`) — AniList allows custom URI schemes. Tokens are 1-year JWTs.
**Trade-offs:** Implicit Grant is deprecated in OAuth 2.1, but AniList doesn't offer a better option for mobile. Acceptable.

## D-018 — Episode metadata: merge Kitsu (primary) + Jikan (filler/dates) + AniList (count/next)
**Status:** PROPOSED
**Source:** R-3 research
**Rationale:** Kitsu has the best per-episode thumbnails + synopses. Jikan has filler flags + TZ-aware air dates. AniList has episode count + nextAiringEpisode. Merge per field per CORE_RULES §30.
**Trade-offs:** three sources = more complexity; mitigated by the `EpisodeMetadataSource` interface pattern.

## D-019 — Notifications: local polling (no push)
**Status:** PROPOSED
**Source:** R-2 research (AniList has no GraphQL subscriptions/WebSocket)
**Rationale:** AniList doesn't push. We poll `Viewer.unreadNotificationCount` cheaply, then fetch `Page.notifications` only if count > 0. WorkManager daily poll + on-app-open check.
**Trade-offs:** not real-time; acceptable for an anime tracker (new episodes are weekly).
