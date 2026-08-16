# Architecture — ONLYLIST

> Quick-reference summary. Deep rationale lives in `research/R-4-android-design-system.md`.
> Module graph + data flow live here. Decisions referenced as `D-NNN` live in `memory/decisions.md`.

## 1. What we are building

An **Android app** whose primary purpose is an **AI-powered, infinitely-customizable design system**, with an **anime/manga tracker** (AniList + Kitsu + Jikan) as the test-bed application. The app must be:

- **Offline-first** — works without network; reconciles on refresh.
- **Non-Material** — custom design system; the AI agent + user customize it.
- **Agent-embedded** — a full-fledged AI agent (ported from Cline's architecture) drives design customization.
- **Backup-able** — full data export/import including design + AniList token (encrypted).

## 2. Tech stack (confirmed by research)

| Concern | Choice | Decision |
|---------|--------|----------|
| Language | Kotlin 2.x | D-001 |
| UI | Jetpack Compose | D-001 |
| Design system | Custom `AppTheme` (NOT Material) | D-002 |
| Local DB | Room | D-003 |
| Networking (GraphQL) | Apollo Kotlin + normalized cache | D-004 |
| Networking (REST) | Ktor 3 | D-004 |
| DI | Koin 4 | D-005 |
| Navigation | Navigation Compose | D-006 |
| Images | Coil 3 | D-007 |
| Charts | Vico (line/bar) + custom Canvas (radar/spider) | D-008 |
| Background work | WorkManager | D-009 |
| Preferences | DataStore | D-010 |
| Fonts | Inter + Sora + JetBrains Mono (OFL, bundled) | D-011 |
| AI agent | Ported Cline architecture, Kotlin | D-012 |
| LLM providers | Anthropic, OpenAI, OpenRouter, Gemini, + OpenAI-compatible | D-012 |
| Build CI | GitHub Actions only | D-013 |
| ABIs | arm64-v8a + armeabi-v7a + x86_64 | D-014 |
| SDK | compileSdk=36, targetSdk=36, minSdk=26 | D-015 |
| Backup crypto | Android Keystore + optional user passphrase (PBKDF2) | D-016 |

## 3. Module graph

```
:app                                    ← composition root (Application, MainActivity, nav host)
│
├── :core:designsystem                   ← AppTheme, CompositionLocals, custom components, FontRegistry
├── :core:common                         ← Logger, utils, Result wrappers, coroutines dispatchers
├── :core:database                       ← Room DB, entities, DAOs, type converters
├── :core:datastore                      ← DataStore prefs wrappers
├── :core:network:anilist                ← Apollo Kotlin client, queries, auth
├── :core:network:kitsu                  ← Ktor client, endpoints
├── :core:network:jikan                  ← Ktor client, endpoints
├── :core:network:common                 ← shared HTTP config, rate limiter, retry, etag
├── :core:data                           ← repositories (MediaRepository, EpisodeMetadataRepository, ListRepository, AiringRepository, UserRepository)
├── :core:design-tokens                  ← DesignTokens model, theme.json (de)serialization, migrators
├── :core:agent:core                     ← agent loop, context manager, checkpoint store
├── :core:agent:llm                      ← LLM provider abstraction (4 providers + generic)
├── :core:agent:tools                    ← tool registry, design-token tools, layout tools, sorting tools
├── :core:agent:permissions              ← approval gateway
├── :core:backup                         ← backup/restore, zip, encryption
├── :core:palette                        ← image palette extraction (androidx.palette wrapper)
│
├── :feature:home                        ← home screen (user info, new content, quick stats)
├── :feature:profile                      ← profile screen (charts: radar, bar, etc.)
├── :feature:library                      ← anime + manga lists (sorting by status)
├── :feature:search                       ← search (anime/manga toggle, filters, sort)
├── :feature:airing                       ← airing schedule (calendar view, customizable layout)
├── :feature:details                      ← details screen (multi-source episode metadata)
├── :feature:settings                     ← settings (auth, backup, agent config, design history)
└── :feature:design-studio                ← the AI agent chat UI + design customization surface
```

**Dependency direction**: features → core; core → core (lower-level). No feature-to-feature dependencies. No upward dependencies. `:app` wires everything via Koin modules.

## 4. Data flow (offline-first)

```
[User opens Home screen]
        │
        ▼
[HomeViewModel] ── observes ──▶ [HomeRepository] ── observes ──▶ [Room: media, media_list_entry, airing_schedule]
        │                                │
        │                                └── triggers (if stale) ──▶ [AniListGraphQLClient] ──▶ [AniList API]
        │                                                                                      │
        │                                                                                      ▼
        │                                                                              [normalize + merge]
        │                                                                                      │
        │                                                                                      ▼
        │                                                                              [write to Room]
        │                                                                                      │
        ▼                                                                                      │
[UI renders local data immediately] ◀── Flow recompose ◀──────────────────────────────────────┘
```

**Key invariants**:
- UI ALWAYS renders local data first. Network is a refresh, not a prerequisite.
- Repositories return `Flow<T>` (reactive). ViewModels expose `StateFlow<UiState>`.
- Writes are `Result<T>` (success/failure) with optimistic updates + rollback on failure.
- Rate-limiter + single-flight queue per source (AniList currently 30 req/min).

## 5. Design-token flow

```
[theme.json] ──load──▶ [DesignTokens data class] ──▶ [StateFlow<DesignTokens>] ──▶ [CompositionLocals]
                                                                               ▲
                                                                               │
[AiDesignAgent] ──tool: apply_token_patch──▶ [preview StateFlow] ──commit──▶ [active StateFlow]
                                                       │
                                                       ▼
                                                  [preview UI renders]
                                                       │
                                               user approves / discards
                                                       │
                                                       ▼
                                                 [snapshot to Room: design_snapshots] (capped 50)
```

**Key invariants**:
- The AI agent NEVER writes Kotlin/Compose. It only writes to `theme.json` (+ sorting rules + variant selections).
- Changes go to a preview StateFlow first; only `commit` writes to the active StateFlow + Room.
- Every commit snapshots the prior state for rollback.
- UI recomposes live when the active StateFlow emits.

## 6. AI agent integration

```
[User chats in Design Studio] ──▶ [AgentRuntime]
        │                                │
        │                                ├──▶ [LLM provider (streaming SSE)]
        │                                ├──▶ [ContextManager (quarter-truncation)]
        │                                ├──▶ [ToolExecutor]
        │                                │       ├── read_design_tokens
        │                                │       ├── apply_token_patch (JSON-Patch)
        │                                │       ├── apply_image_palette
        │                                │       ├── swap_layout / set_component_variant
        │                                │       ├── set_sorting_rule
        │                                │       ├── preview / commit / rollback
        │                                │       ├── ask_user
        │                                │       └── attempt_completion
        │                                ├──▶ [ApprovalGateway (suspend)] ── user approves destructive tools
        │                                └──▶ [CheckpointStore (Room: design_snapshots)]
        │
        ▼
[Design tokens updated] ──▶ [UI recomposes live]
```

**Loop**: `while(isActive) { buildContext → llm.stream → parse tool calls → for each: (if destructive: await approval) → execute → observe → repeat until attempt_completion }`

## 7. Multi-source episode metadata flow

```
[Details screen for Media X]
        │
        ▼
[EpisodeMetadataRepository.getEpisodes(mediaId=X)]
        │
        ├──▶ Room: episode (local cache) ──▶ if fresh enough, return Flow
        │
        └──▶ if stale / missing:
                ├──▶ AniList: Media(id=X).streamingEpisodes  (only ~6 recent, no descriptions)
                ├──▶ Kitsu: mappings?filter[externalSite]=anilist/anime → kitsuId → /anime/{kitsuId}/episodes
                └──▶ Jikan: /v4/anime/{idMal}/episodes  (list) + /episodes/{id} (lazy single, fragile)
                        │
                        ▼
                [merge per field] ──▶ [write to Room: episode] ──▶ [Flow emits] ──▶ UI recomposes
```

**Merge priority** (per field):
- Thumbnail: Kitsu → Jikan (sparse) → none
- Title (en): Kitsu canonicalTitle → Jikan → "Episode N"
- Title (jp): Kitsu titles.ja_jp → Jikan title_japanese
- Air date: Jikan aired (TZ) → Kitsu airdate
- Synopsis: Kitsu → Jikan (lazy)
- Duration: Kitsu length (min) → Jikan duration (sec→min)
- Filler/recap: Jikan only

## 8. Backup/restore flow

```
[Weekly WorkManager triggers] OR [User taps "Back up now"]
        │
        ▼
[BackupManager]
        ├── collect: Room DB file + DataStore + theme.json + saved themes + snapshots + (optional) AniList token
        ├── encrypt: AniList token with user passphrase (PBKDF2) OR Keystore
        ├── zip: single backup.zip + backup_manifest.json (checksums)
        ├── write: filesDir/backups/auto-backup.zip (rolling — overwrites prior auto-backup)
        └── verify: read back + validate manifest

[User taps "Restore"]
        ├── validate manifest + schema version
        ├── if older: run migrators
        ├── if newer: refuse with clear error
        ├── confirm overwrite (offer to auto-backup current state first)
        ├── decrypt token (prompt passphrase)
        ├── unzip → overwrite Room + DataStore + theme.json + themes + snapshots
        └── restart app (design restores exactly, user loses nothing)
```

## 9. Build & CI

- **GitHub Actions workflow** (`.github/workflows/build-apk.yml`):
  - Triggers: push to `main`, PRs, manual dispatch, tags (for releases).
  - Steps: checkout → setup-java 17 → setup-android SDK → `gradle/actions/setup-gradle@v4` (caching) → `./gradlew assembleDebug` (debug) + `./gradlew bundleRelease` (release AAB) → Detekt (with custom rule forbidding material3 imports) → "Verify ABIs" step → upload APK + AAB artifacts.
- **NEVER build locally** (CORE_RULES §8). Find compile errors by: reading code carefully → sub-agent review → push to CI → read annotations → iterate.
- **ABIs**: `arm64-v8a` + `armeabi-v7a` + `x86_64`. Verified post-build.
- **Emulator support**: x86_64 ABI included so the user can run the Android emulator (Hyper-V now enabled on their PC per their confirmation). Physical device is the primary test target; emulator is secondary.

## 10. Open architectural questions (deferred to user)

See `memory/open-questions.md` for the full list. The architectural ones:
- Q-005: Should we use Apollo Kotlin's normalized cache in addition to Room, or just Room?
- Q-006: Should the AI agent's design snapshots be included in backups by default?
- Q-007: Should we add a project web dashboard (like the old ANI-KUTA project had)?
- Q-008: Confirm app name + package name.
