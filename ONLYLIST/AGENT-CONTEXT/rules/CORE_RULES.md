# CORE RULES — Only-List

> Non-negotiable rules for the Only-List project. Supersedes all prior rulesets.
> If a rule here conflicts with anything else, this file wins.
> Reference ruleset (from prior ANI-KUTA project) lives at `REFERENCES/core-rules-reference.md` — structural inspiration only.

---

## 1. Dev Flow

1. **Analyze → Research → Comprehend → Confirm → Implement → Verify.** No blind guesses.
2. **No assumptions.** If unclear: ask the user or verify in the codebase.
3. **Modular complexity.** Split long tasks across files + steps. Each piece independently understandable.

## 2. Honesty & Communication

1. **Ask as many questions as needed.** No hesitation.
2. **Proactively highlight** concerns, risks, limitations — before the user discovers them.
3. **Never sugarcoat.** Flag flawed requests directly. A correct uncomfortable truth beats a polite wrong answer.
4. **Short + to the point.** Tell as much as needed — no more.

## 3. Summary After Completion

- Lead with key outcome → what changed → what's next.
- Reference file paths, not contents.
- Provide a **test checklist** (grouped by category, `[ ]` checkboxes, expected result) for any fix/improvement. User tests + reports ✅/❌/⚠️.

## 4. Project Structure

**One wrapper folder at repo root.** All zones live inside `ONLYLIST/`. `.github/` stays at repo root (GitHub Actions constraint).

```
repo-root/
├── ONLYLIST/
│   ├── AGENT-CONTEXT/          # agent memory + rules (versioned)
│   ├── APP/only-list/          # Android app (Gradle + Kotlin + Compose)
│   ├── REFERENCES/             # external reference material (read-only)
│   ├── research/               # deep research reports (R-N-*.md)
│   └── README.md
└── .github/workflows/          # CI (repo-root level)
```

**Identity:**
- App display name: **Only-List**
- Package: **`com.confused.onlylist`**
- ABIs: `arm64-v8a` + `armeabi-v7a` + `x86_64`
- compileSdk=36, targetSdk=36, minSdk=26

## 5. Code Rules

1. **Split code into multiple files** — fewest that make sense. One module = one responsibility.
2. **Comments explain *why*, not *what*.**
3. **Reuse before you write.** Look at existing files first.
4. **No unrequested abstractions.** Exception: an interface with one impl is OK when a future swap is explicitly planned.
5. **Mark deliberate simplifications** with `// ponytail: <ceiling> — upgrade via <path>`.
6. **No media player in this project.** Strict UI/backend separation everywhere — no carve-outs.

## 6. Architecture

1. **Highly modular.** Multiple things → multiple modules. (See `knowledge/architecture.md` for module graph.)
2. **UI and backend separate per screen.** UI renders data + handles input only. Data layer fetches/processes/stores. They communicate via defined contracts (interfaces/repositories).
3. **The AI design-system agent is part of the architecture.** It lives in `:core:agent:*` modules, writes to design tokens via a controlled tool surface (§11), and is OPTIONAL — the app must be fully usable without it (§12).
4. **Agent modules must be extractable into a separate app later.** Keep `:core:agent:core`, `:core:agent:llm`, `:core:agent:tools` as pure Kotlin (JVM-targetable) where possible. Android deps only at the edges. (Future goal: a dedicated LLM agent app.)

## 7. GitHub Actions & Branching

1. **Always use GitHub Actions for builds.** Never build locally.
2. **NEVER install Android SDK / JDK / any Android build tooling in the local sandbox.** GitHub Actions provides everything.
3. **NEVER run `./gradlew` locally** — not even to "check compilation." Find compile errors by: careful reading → sub-agent review → push to CI → read annotations → iterate.
4. **NEVER create `local.properties`** or write `sdk.dir=...` to it.
5. **Branch per feature/fix:** `feature/<name>`, `fix:<name>`, `docs/<name>`. Merge to `main` only after CI green + user review.
6. **Conventional Commits:** `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`. Never force-push to `main`.

## 8. Self-Learning

1. **Log mistakes immediately** in `memory/lessons-learned.md`. Format: `- [TAG] lesson (source: <task-id>, <date>)`. Tags: `MISTAKE` / `CORRECTION` / `INSIGHT` / `PATTERN`.
2. **Dedup before adding.** Grep for the keyword first.
3. **At task start:** grep `lessons-learned.md` for tags matching your current task type.

## 9. Naming

- **Files:** `kebab-case` (markdown/data). `PascalCase` (Kotlin classes). `camelCase` (functions/vars).
- **Folders:** `kebab-case` (code). Uppercase for top-level zones (`APP/`, `AGENT-CONTEXT/`, `REFERENCES/`, `research/`).
- **Gradle modules:** `:lower:case:colon` (`:core:designsystem`). Package `com.confused.onlylist.<module>`.
- **Decisions:** `D-NNN`. **Questions:** `Q-NNN`. **Research:** `R-N`. **Tasks:** `Task NN`.

## 10. Session-End + Sandbox Recovery

1. **Every session ends with all changes committed + pushed to GitHub.** The sandbox clears randomly — unpushed work is lost.
2. **Push frequently**, not just at session end.
3. **Credentials live OUTSIDE the repo tree** at `/home/z/my-project/android-project/credentials/` (600 perms, never committed).
4. **If the environment feels off** (missing files, weird errors, working tree doesn't match `progress.md`): STOP + re-clone from GitHub. Don't patch a corrupted environment.
5. **At session start:** read `memory/progress.md` first, then `memory/open-questions.md`.

## 11. AI Design-System Agent

1. **Module shape:** `:core:agent:core` (loop + context), `:core:agent:llm` (4+ providers, OpenAI-compatible default), `:core:agent:tools` (tool registry), `:core:agent:permissions` (approval gateway).
2. **Agent loop:** iterative `while(isActive)` Kotlin coroutine. NOT recursive.
3. **Context:** quarter-truncation at 50% remaining context + LLM-based auto-compaction (port from Kilo Code) with optional dedicated compaction model.
4. **Tool surface** (the ONLY way the agent touches app state):
   - `read_design_tokens`, `apply_token_patch` (JSON-Patch RFC 6902), `apply_text_patch` (SEARCH/REPLACE)
   - `set_color_role`, `set_typography`, `set_shape`, `set_motion` (convenience wrappers)
   - `apply_image_palette(image_uri, mapping)`
   - `swap_layout(screen, layout_id)`, `set_component_variant(component, variant)`
   - `set_sorting_rule(list, rule)` — writes to Room `sorting_rules` table (DSL: `sort by: score desc, title asc`)
   - `preview`, `commit`, `rollback`, `ask_user`, `attempt_completion`
5. **Approval:** AUTO-APPROVE by default (per user). Every `commit` snapshots to `design_snapshots` (Room, cap 50) for one-tap undo. Settings → "Reset to defaults" wipes ALL agent state + design customizations + restores the starter theme.
6. **Iteration cap:** 25 per task (mobile battery + LLM cost). Configurable in Settings.
7. **Cost guard:** user sets a per-run token/price cap; agent aborts when exceeded. If cap is off, agent runs free. User sees daily usage in Settings.
8. **LLM providers:** all Cline-compatible providers, OpenAI-compatible by default. User pastes their API key (stored in Android Keystore, never logged, never in backups).
9. **No terminal, no filesystem browsing, no MCP-stdio.** Agent edits ONLY design tokens + layout/component/sorting selections. HTTP MCP post-MVP.
10. **Architecture inspiration:** Cline (structural backbone) + Kilo Code (auto-compaction, glob permissions, plan files). See `research/R-1-cline-agent.md` + `research/R-6-kilocode-agent.md`.

## 12. Design Tokens — Source of Truth

1. **One canonical file:** `theme.json` (schema `"1.0"`). Sections: `colors`, `typography`, `shapes`, `motion`, `elevation`, `spacing`, `screenLayouts`, `componentVariants`.
2. **Loaded at runtime** into `StateFlow<DesignTokens>`. UI reads via 6 CompositionLocals (`LocalColors`, `LocalTypography`, `LocalShapes`, `LocalMotion`, `LocalSpacing`, `LocalElevation`).
3. **NOT Material.** Custom role set. (See §13.)
4. **Multiple named themes** — user can save + switch instantly.
5. **Font families bundled** (res/font), referenced by key in tokens. Swapping a family is a token edit, not code.
6. **Schema evolution:** bump `$schema` version + write a migrator. Old backups must restore (migrate on import).

## 13. Non-Material Enforcement

1. **Custom `AppTheme`** with 6 CompositionLocals. NOT `MaterialTheme`.
2. **Forbidden imports in feature UI** (enforced by Detekt CI rule): `androidx.compose.material3.{Button,Card,TopAppBar,Scaffold,NavigationBar,NavigationRail,...}`.
3. **Allowed primitives** (reused under the hood, with justification): `Surface`, `Ripple`, `ModalBottomSheet`.
4. **Custom components** live in `:core:designsystem`. Features depend on `:core:designsystem`, never `material3` directly.
5. **Design language** lives at `APP/only-list/DESIGN-LANGUAGE.md` (canonical). The active `theme.json` is its runtime representation.

## 14. Offline-First Data Layer

1. **Every screen renders from local DB first.** Network is a refresh, not a prerequisite. No spinner-while-waiting if local data exists.
2. **Cache** (Room, entities): `media`, `episode`, `media_list_entry`, `airing_schedule`, `character`, `studio`, `metadata_source_state`, `user`, `sorting_rules` + FTS4 search index.
3. **Reconciliation:** field-level COALESCE for media; LWW per `source_updated_at` for list entries; append-never-overwrite for episodes; stale-flag + backoff for 404s.
4. **NO automatic background polling.** (User clarification: the app does NOT poll daily or fetch when the user isn't using it.) Fetch ONLY when: user opens a screen that needs the data, user pull-to-refreshes, or user explicitly triggers an action. WorkManager is used ONLY for weekly backup + (optional) airing-episode refresh of the currently-visible screen.
5. **Rate-limit:** single-flight queue per source; sliding 60s window; respect `Retry-After`. Target 60 req/min for AniList (middle ground, easily editable later — currently degraded to 30).
6. **AniList token:** Android Keystore (encrypted). Decode JWT `exp` on launch; banner at 7 days; block writes when expired; read-only continues.
7. **Images:** Coil 3, 250MB disk cache, placeholder from `MediaCoverImage.color`.

## 15. Multi-Source Episode Metadata

1. **AniList** = primary for media metadata, lists, schedule, count, nextAiring.
2. **Kitsu** = primary for per-episode thumbnails, synopses, titles (en/jp), duration.
3. **Jikan** = primary for per-episode air dates (TZ-aware), filler/recap flags, score. Single-episode synopsis endpoint is fragile (504s) — lazy fallback only, never block UI. Use smarter retry/backoff.
4. **ID mapping:** AniList `idMal` → Jikan (direct). AniList → Kitsu via `mappings?filter[externalSite]=anilist/anime` (cache forever).
5. **Merge per field:** thumbnail Kitsu→Jikan; title Kitsu→Jikan; airdate Jikan→Kitsu; synopsis Kitsu→Jikan(lazy); duration Kitsu→Jikan; filler Jikan-only.
6. **Sources behind interfaces:** `EpisodeMetadataSource` with `KitsuSource`/`JikanSource` impls. Merge in `EpisodeMetadataRepository`. Adding a source = one file.

## 16. Backup & Restore

1. **Backup contents** (single zip): Room DB + DataStore + `theme.json` + saved themes + (optional) snapshots + (optional) AniList token.
2. **NOT password-protected by default.** User can opt in to password protection per backup. If opted in, password required on restore (no recovery path — lose password = lose token).
3. **Weekly auto-backup** via WorkManager (charging + battery-not-low + unmetered + idle). One rolling copy. User can relax constraints in Settings.
4. **Design restores exactly.** Active `theme.json` + saved themes + snapshots + sorting rules all restore. User loses nothing.
5. **LLM API key NEVER in backups** (device-bound Keystore).
6. **Restore validates manifest + schema version.** Older → migrate. Newer → refuse with clear error. Confirm overwrite (offer auto-backup current state first).
7. **Verify after write:** read-back + zip integrity + manifest checksums. Delete corrupt backups + notify.

## 17. Dynamic Theming (Palette from Image)

1. `androidx.palette` + HSL-shift tonal generation (NOT Material You HCT — too much color science for v1).
2. **Coral HSL bounds:** hue 5–20°, sat 70–100%, light 55–72% — prevents drift to red/orange/pink.
3. **Role mapping** (default, customizable): dominant → surface, vibrant → accent, muted → surfaceVariant.
4. **WCAG AA contrast check** on text roles; auto-darken/lighten until pass.
5. **Preview before commit** — goes to preview StateFlow, not active.

## 18. UI / UX Quality

1. **Buttery-smooth animations.** 60fps target. Use Compose animation APIs correctly (`AnimatedVisibility`, `AnimatedContent`, `animateContentSize`, `SharedTransitionLayout`, `Animatable`, spring physics).
2. **No heavy work on main thread** during animation.
3. **Every tap gives feedback** — spring scale (1.0→0.96→1.02→1.0) + haptic + color change. Never a dead tap.
4. **Skeletons + shimmer** for data loads (never spinners if local data exists).
5. **Optimistic updates + undo toast** — never confirmation dialogs.
6. **Live data verification:** every user action has immediate visual feedback; data changes propagate via `Flow`/`StateFlow`; no silent failures.
7. Follow `APP/only-list/DESIGN-LANGUAGE.md` strictly.

## 19. Crash Handling

1. **Global crash handler** in `Application.onCreate()` FIRST, before any other init. `Thread.setDefaultUncaughtExceptionHandler(OnlyListCrashHandler(this))`.
2. **Crash report** persisted to `filesDir/last_crash.txt`.
3. **`ErrorActivity`** launched with `NEW_TASK | CLEAR_TASK` — shows "Something went wrong" + scrollable crash log + Copy + Restart + Close buttons.
4. **Never silently crash to home screen.**

## 20. Logging

1. **Central `Logger` wrapper** in `:core:common`. Never call `Log.d()` directly.
2. **Levels:** VERBOSE / DEBUG / INFO / WARN / ERROR. Tags per module (`OnlyList:Core:Database`, `OnlyList:Feature:Details`, `OnlyList:Core:Agent`, etc.).
3. **Toggleable** off in release; runtime toggle in Settings for debug builds.
4. **Never log:** credentials, tokens, personal data, full request/response bodies. Log URLs + status codes only.
5. **Zero overhead when off** — guard expensive construction with `if (Logger.isEnabled)`.
6. **Logcat filter format** (for Android Studio): `tag:OnlyList:Feature:Details | tag:OnlyList:Core:Network message~:(?i)(keyword1|keyword2)`. Never use `adb logcat` commands.

## 21. Documentation

1. **Agent memory/rules** → `AGENT-CONTEXT/`. Main agent only updates.
2. **App technical docs** → `APP/only-list/DOCUMENTATION/`.
3. **App design language** → `APP/only-list/DESIGN-LANGUAGE.md` (one canonical file).
4. **Research** → `research/`. **References** → `REFERENCES/`.
5. **Same-session updates.** If code/state/decisions change, update docs in the SAME commit. "Later" never comes.
6. **DB docs stay in sync with code** — `APP/only-list/DOCUMENTATION/database/`. One file per table group + README + changelog + ER diagram. Update on every schema change.
7. **Doc drift check at task end:** re-read docs you touched; grep for stale references; re-derive claims ("X modules built") by inspecting code.

## 22. Sub-Agent Delegation

1. **Sub-agents work ONLY in their assigned zone.** Research → `research/`; feature → `APP/only-list/feature/<x>/`; etc.
2. **Sub-agents must NOT touch `AGENT-CONTEXT/`.** They report to the main agent, who updates memory after verifying.
3. **Budget:** 5 sub-agents concurrent max. Use the best model available. Prefer parallel execution when tasks are independent.

## 23. Patterns to Avoid

- ❌ Dependencies between skills (each standalone).
- ❌ Complex build systems / test frameworks (one self-check for non-trivial logic is enough).
- ❌ Generic advice ("write clean code" = useless; "function ≤ 30 lines or split" = useful).
- ❌ Over-documenting file structure (document what's non-obvious).
- ❌ Boilerplate "for later."
- ❌ Material Design imports (§13).
- ❌ Automatic background polling when the user isn't using the app (§14 rule 4).

## 24. Tool Failure Recovery

1. **Stop after 5 consecutive failures** of the same tool. Don't hammer.
2. **Acknowledge to user.** Move to a different tool or describe what you would have done.
3. **Environment often self-recovers** on the next user message.
4. **Log** in `lessons-learned.md` with `[PATTERN]` if it recurs across sessions.

## 25. Debug-Build Schema Freedom

1. **Debug builds can rebuild the schema freely.** Drop tables, add NOT NULL columns, restructure — no migration scripts needed. `fallbackToDestructiveMigration` is fine.
2. **Testing workflow:** delete app → reinstall → fresh DB → test.
3. **When publishing approaches** (user will signal): migration discipline returns. Until then, don't let migration fear block schema quality.

## 26. User Uses Speech-to-Text

- Transcription errors happen. If a request feels off/ambiguous: correct obvious homophones from context; if still unclear → STOP + highlight before proceeding.
- When in doubt: ask. 10 seconds of clarification beats an hour of wrong work.

## 27. Take As Much Time As Needed

- **Quality over speed.** Don't rush steps to "finish faster."
- Sub-agent reviews + verification + documentation take time — they are not optional.
- The only hard deadline: **push to GitHub at session end** (§10). Everything else is quality-bound.
