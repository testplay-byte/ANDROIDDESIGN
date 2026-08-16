# Knowledge: Android Design System + Architecture

> Quick-reference. Full research: `research/R-4-android-design-system.md` (84KB, 27 questions answered).

## Tech stack (see decisions.md D-001..D-016)
- Kotlin 2.x + Jetpack Compose (D-001)
- Custom `AppTheme` (NON-Material) (D-002)
- Room (D-003) — KMP-stable, AI-friendly
- Apollo Kotlin (AniList GraphQL) + Ktor 3 (Kitsu/Jikan REST) (D-004)
- Koin 4 (D-005) — runtime DI, more LLM-friendly than Hilt
- Navigation Compose (D-006) — NOT Nav3 (still alpha)
- Coil 3 (D-007) — Compose-native images
- Vico (line/bar) + custom Canvas (radar/spider) (D-008)
- WorkManager (D-009)
- DataStore (D-010)
- Bundled fonts: Inter + Sora + JetBrains Mono (OFL) (D-011)
- compileSdk=36, targetSdk=36, minSdk=26 (D-015)
- ABIs: arm64-v8a + armeabi-v7a + x86_64 (D-014)

## Design system (NON-Material)
- Build `AppTheme` with 6 `CompositionLocal`s: `LocalColors`, `LocalTypography`, `LocalShapes`, `LocalMotion`, `LocalSpacing`, `LocalElevation`.
- Do NOT use `MaterialTheme` as the public mechanism (locks into M3's 29 fixed color roles).
- Reuse only low-level primitives: `Surface`, `Ripple`, `ModalBottomSheet` (with justification).
- Detekt CI rule forbids `androidx.compose.material3.*` imports in feature code (D-002 + CORE_RULES §34).
- Custom components live in `:core:designsystem`. Features depend on `:core:designsystem`, never `material3` directly.

## Design tokens (source of truth — CORE_RULES §29)
- `theme.json` (schema version `"1.0"`). Sections: colors (per role), typography (font family refs + sizes + weights), shapes (corner radii), motion (durations + easings), elevation, spacing, componentVariants.
- Loaded at runtime into `StateFlow<DesignTokens>`. UI reads via CompositionLocals.
- Multiple named themes — user can save + switch instantly.
- Font families bundled in `res/font/`; `FontRegistry` maps token keys → `FontFamily`. AI agent swaps families via token edits.
- The agent edits tokens, NEVER code.

## Palette extraction (dynamic theming — CORE_RULES §33)
- `androidx.palette` API. Up to 16 swatches.
- Role mapping: dominant → surface/background, vibrant → primary/accent, muted → surfaceVariant, darkVibrant → outline.
- Simple HSL-shift tonal palette generation (NOT Material You HCT — too much color science for v1).
- WCAG AA contrast check on text roles; auto-darken/lighten until it passes.
- Preview before commit (goes to preview StateFlow, not active).

## Fonts (CORE_RULES §35)
- Starter set: Inter (body), Sora (display), JetBrains Mono (numbers/mono). All OFL.
- Bundled as `.ttf` in `res/font/` (~1.8MB APK impact).
- Prefer variable fonts where available (smaller + more flexible).
- OFL attribution in Settings → Open Source Licenses.

## Offline-first data layer (CORE_RULES §31)
- Every screen renders from local DB first. Network is a refresh, not a prerequisite.
- Cache schema (Room): `media`, `episode`, `media_list_entry`, `airing_schedule`, `character`, `studio`, `metadata_source_state`, `user` + FTS4 search index on media titles/synonyms.
- Reconciliation: field-level COALESCE for media; LWW per `source_updated_at` for list entries; append-never-overwrite for episodes; stale-flag for 404s.
- Images: Coil 3, 250MB disk cache default, placeholder color from `MediaCoverImage.color`.
- AniList token in Android Keystore (encrypted).
- Rate-limit: single-flight queue per source, sliding 60s window, respect `Retry-After`. Design for 30/min (current degraded limit).
- Apollo Kotlin normalized cache (in-memory + persistent SQLite) for GraphQL.
- Pull-to-refresh: bypasses cache TTL but doesn't block UI.

## DI (Koin 4)
- Runtime DI. More LLM-friendly than Hilt (no KSP graph).
- Recover compile-time safety with a `KoinTest` resolvability test in CI.
- Modules per feature + core module.

## Navigation
- Navigation Compose (stable).
- `NavigationSuiteScaffold` for adaptive (bottom bar on phone, rail on tablet, drawer on TV/desktop).
- Type-safe destinations (Compose Navigation 2.8+ supports type-safe args).

## Charts
- Vico for line/bar charts.
- Custom Compose Canvas for radar/spider (Vico doesn't ship radar). Animate with `Animatable`.

## Backup/restore (CORE_RULES §32)
- Single zip: Room DB + DataStore + theme.json + saved themes + snapshots + (optional) AniList token (encrypted).
- Weekly WorkManager (7 days, charging + battery-not-low + unmetered + idle). One rolling copy.
- Crypto: Android Keystore (device-bound, not portable) OR user passphrase (PBKDF2, portable). User picks at backup time.
- Restore: validate manifest + schema version → migrate if older → refuse if newer → confirm overwrite → decrypt → unzip → restart.
- LLM API key NEVER in backups.

## AI agent (CORE_RULES §28)
- Module shape: `:core:agent:{core, llm, tools, permissions}`.
- Iterative `while(isActive)` Kotlin coroutine (NOT recursive).
- Context: quarter-truncation.
- 4 LLM providers (Anthropic, OpenAI, OpenRouter, Gemini) + OpenAI-compatible generic.
- Tool surface: design-token tools only (read_design_tokens, apply_token_patch, apply_image_palette, swap_layout, set_component_variant, set_sorting_rule, preview, commit, rollback, ask_user, attempt_completion).
- Approval gateway (suspend callback).
- Room-backed `design_snapshots` (capped 50) for rollback.
- SSE streaming for token-by-token rendering.
- The agent is OPTIONAL — app is fully usable without it.

## Module graph (see architecture.md for full graph)
```
:app
├── :core:designsystem, :core:common, :core:database, :core:datastore
├── :core:network:{anilist, kitsu, jikan, common}
├── :core:data, :core:design-tokens, :core:palette, :core:backup
├── :core:agent:{core, llm, tools, permissions}
└── :feature:{home, profile, library, search, airing, details, settings, design-studio}
```

## CI (CORE_RULES §8)
- GitHub Actions only. NEVER build locally.
- `gradle/actions/setup-gradle@v4` for caching.
- Detekt (with custom rule forbidding material3 imports) + "Verify ABIs" step.
- Upload APK + AAB artifacts.
- Find compile errors: read carefully → sub-agent review → push to CI → read annotations → iterate.

## Performance
- 60fps target. Compose animation APIs: `AnimatedVisibility`, `AnimatedContent`, `animateContentSize`, `SharedTransitionLayout`, `Animatable`.
- No heavy work on main thread during animation. Offload to IO.
- Test on low-end devices (not just emulators).
