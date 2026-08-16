# CORE RULES — Non-Negotiable (ANDROIDDESIGN)

> These rules apply at ALL times during development of the ANDROIDDESIGN project.
> They supersede any prior ruleset (including the ANI-KUTA reference rules the
> project started from). If a rule here conflicts with anything else, **this file wins.**
>
> The reference ruleset (from the prior ANI-KUTA project) lives at
> `REFERENCES/core-rules-reference.md` and is kept ONLY as a structural inspiration.
> Anything ANI-KUTA-specific (MPV player, SQLDelight, `app.confused.anikuta`,
> ARM-only ABIs, DASHBOARD zone) is **NOT** carried over unless explicitly restated
> here. This project has its own stack, its own goals, its own constraints.

---

## 0. Project Identity (placeholders — confirm with user)

| Field | Working value | Status |
|-------|---------------|--------|
| Repo | `testplay-byte/ANDROIDDESIGN` | confirmed |
| Wrapper folder | `ANDROIDDESIGN/` (single folder at repo root) | proposed — awaiting confirmation |
| App module | `APP/ani-design/` | proposed |
| App package | `com.testplaybyte.anidesign` | proposed — awaiting confirmation |
| App display name | "AniDesign" | proposed — awaiting confirmation |
| Default branch | `main` | confirmed |
| ABIs | `arm64-v8a` + `armeabi-v7a` + `x86_64` | confirmed (user approved x86_64 for emulator) |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 | proposed (per R-4 research) |

> These placeholders are repeated in `memory/open-questions.md` (Q-001..Q-004).
> Do not treat them as final until the user confirms.

---

## 1. Development Flow

Every task follows this cognitive sequence — in order:

1. **Analyze** — Understand the user's request, intentions, and context. What do they want? How do they want it done? No blind guesses.
2. **Research** — Investigate the relevant topic/code before acting. Understand what already exists, what touches what. Look before you write.
3. **Comprehend** — Confirm the whole task is understood. If anything is unclear, ask directly — no hesitation.
4. **Confirm** — For non-trivial changes, confirm your understanding with the user before building. State what you'll do in one line.
5. **No Assumptions** — Never guess. If unclear: ask the user or verify in the codebase. Assumptions are bugs you ship early.
6. **Modular Complexity** — Long/complex task? Split it across multiple files and multiple workflow steps. Keep each piece manageable, documented, and independently understandable.

> The concrete step-by-step task loop lives in `memory/workflow.md` (to be written).
> This section is the **mindset**; `workflow.md` is the **procedure**.

---

## 2. Communication & Honesty

- **Ask as many questions as needed.** Clarify anything unclear directly with the user.
- **Proactively highlight** concerns, limitations, and future risks — before the user discovers them.
- **Guide the user** through problems and constraints plainly.
- **Never sugarcoat.** If a request has an issue, say so directly. Do not blindly agree. Do not follow requests that you can see are flawed without flagging the flaw first.
- **Be honest at all times.** A correct uncomfortable truth beats a polite wrong answer.
- Keep wording **short, simple, to the point**. Tell as much as needed — no more.

---

## 3. Summary After Completion

After completing a task, give the user a **short summary**:

- **Do not exaggerate.** Do not leave out key details.
- Use **proper formatting**: headings, highlights, emojis for emphasis and spacers.
- Use **multiple empty lines** for spacing where one line isn't enough.
- Lead with the **key outcome**. Then what changed. Then what's next.
- Reference file paths, not file contents (the user opens files if they want detail).
- **Test checklist**: After implementing improvements/fixes, ALWAYS provide a **test checklist** the user can follow to verify each fix on their device. Format: grouped by category (e.g. Login, Search, Episode Metadata), each item as a checkbox `[ ]` with a clear description of what to test + what the expected result is. The user tests, reports back (✅/❌/⚠️), and the agent fixes any remaining issues. This closes the feedback loop and ensures nothing is missed.

---

## 4. Project Structure

- Keep the project **easy to handle and manage**. Well-documented, well-understood.
- **AGENT-CONTEXT stays updated after every task** so any future AI agent can pick up immediately.
- Build so that **editing one part** only requires understanding that part + its immediate context — not the whole project.
- **All things link together.** Document the relations (comments in code, notes in knowledge files).

### Folder Layout (canonical)

**RULE (non-negotiable):** The repo root contains exactly ONE wrapper folder. All project zones live INSIDE that single folder — never directly at the repo root. The wrapper folder's name is the project name (`ANDROIDDESIGN/`).

```
repo-root/
└── ANDROIDDESIGN/                     ← the SINGLE wrapper folder
    ├── AGENT-CONTEXT/                 # agent memory + rules (versioned in repo)
    │   ├── memory/                    # progress, decisions, lessons, open-questions, session-log
    │   ├── rules/                     # CORE_RULES.md (this file) + workflow.md
    │   └── knowledge/                 # quick-reference summaries, link to detailed docs
    ├── APP/ani-design/                # Android app (Gradle + Kotlin + Compose)
    │   ├── app/                       # the :app shell module (composition root)
    │   ├── core/                      # :core:designsystem, :core:data, :core:network, :core:agent, ...
    │   ├── feature/                   # :feature:home, :feature:profile, :feature:search, ...
    │   ├── DOCUMENTATION/             # new-app architecture/research/design docs
    │   └── DESIGN-LANGUAGE.md         # the app's design language (canonical)
    ├── REFERENCES/                    # external reference material
    │   ├── core-rules-reference.md   # the ANI-KUTA ruleset we started from (read-only)
    │   ├── cline/                     # vendored Cline source (submodule or shallow clone)
    │   └── README.md
    ├── research/                      # detailed research reports (R-1..R-N)
    │   ├── R-1-cline-agent.md
    │   ├── R-2-anilist-api.md
    │   ├── R-3-kitsu-jikan.md
    │   └── R-4-android-design-system.md
    └── README.md                      # project overview
```

**Exception for `.github/`:** GitHub Actions ONLY detects workflows at `<repo-root>/.github/workflows/`. So `.github/` stays at the repo root (NOT inside the wrapper folder). This is a GitHub platform constraint.

```
repo-root/
├── ANDROIDDESIGN/                     ← wrapper folder (zones inside)
└── .github/workflows/                 # CI (repo-root level — GitHub constraint)
```

**Why a single wrapper folder:** The user explicitly requires this. It keeps the repo root clean, makes the project self-contained, and simplifies cloning (one folder to open, not many). If you see the project zones directly at the repo root, that's a violation — fix it.

**No DASHBOARD zone (yet):** The prior ANI-KUTA project had a `DASHBOARD/webpage/` Next.js site as a project-status dashboard. The user has NOT requested one for this project. If a dashboard becomes wanted, add a `DASHBOARD/` zone inside the wrapper folder at that time and add the corresponding rules back.

---

## 5. Code Rules

- **Split code into multiple files** for development, maintenance, and reuse. Fewest files that make sense — not one giant file, not a file per function.
- **Document with comments**: what lives where, what the relations are. Comments explain *why*, not *what*.
- **One module = one responsibility.**
- Reuse before you write. Look a few files over before implementing.
- No unrequested abstractions (no interface with one impl, no factory for one product). **Exception:** an interface with one impl is OK when a future swap is explicitly planned (e.g. `EpisodeMetadataSource` interface with `KitsuSource`/`JikanSource` impls — we know we'll merge them).
- Mark deliberate simplifications with a `ponytail:` comment naming the ceiling + upgrade path. Example: `// ponytail: single-user only — ceiling: multi-account, upgrade via AccountStore`.
- **No MPV / media-player carve-over in this project.** The old project had a player-screen exception to UI/backend separation because of MPV's single-instance constraint. This project has NO media player. Strict UI/backend separation applies everywhere — no exceptions.

---

## 6. Documentation Rules

- **Verify before writing.** Confirm the change is real, understood, and actually needed before documenting it.
- **If the project changes, the docs reflect it** — same session, not "later".
- Do not over-document file structure. Document what's non-obvious.
- No generic advice. Specific, actionable rules only.

---

## 7. Architecture

- **Highly modular.** Multiple things → multiple modules.
- **UI and backend logic are separate per screen.** A screen's UI and its data/logic live in different files/modules. The UI either calls for data or receives it pre-loaded.
- **Frontend (UI)** renders data, handles input only. No data-fetching logic baked in.
- **Backend (data)** fetches, processes, stores. Exposes clean interfaces to the UI.
- They communicate via **defined contracts** (interfaces/repositories), so UI can be customized by the AI agent without touching data logic.
- **The AI design-system agent is part of the architecture, not bolted on.** It lives in `:core:agent:*` modules and writes to design tokens (§29) via a controlled tool surface (§28). The UI layer observes the active token set reactively.

> Concept diagrams + module graph live in `knowledge/architecture.md`.
> This section is the **rule**; that file is the **design**.

---

## 8. GitHub Actions & Branching

- **Always use GitHub Actions** for builds (APK). Never build locally.
- **Create a branch** for each feature/fix: `feature/<name>`, `fix:<name>`, `docs/<name>`.
- **Merge to `main` only after** the feature is verified working and satisfactory. Not before.
- Commit messages: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`).
- Never force-push to `main`.

### Build Rules (APK)
- **NEVER** build the APK locally. GitHub Actions only.
- **NEVER** install the Android SDK, JDK (with javac), or any Android build tooling in the local sandbox environment. The local environment does NOT have these and MUST NOT acquire them. GitHub Actions provides everything needed.
- **NEVER** run `./gradlew compileDebugKotlin`, `./gradlew assembleDebug`, or ANY Gradle build task locally. Not even for "just checking compilation". Not even for "just finding the error".
- **NEVER** write `sdk.dir=...` to `local.properties`. Do not create `local.properties` at all.
- **How to find compile errors WITHOUT building locally:**
  1. Read the code carefully, line by line, checking every import, type, and API call.
  2. Use sub-agents (Explore type) to review the code for compile errors — they can read files and compare against reference code.
  3. Cross-reference against Cline source (in `REFERENCES/cline/`) when porting agent logic.
  4. Push to CI and read the failure annotations from the GitHub API.
  5. Iterate: fix → push → read CI annotations → fix again. This is the ONLY loop.
- **ABIs: `arm64-v8a` + `armeabi-v7a` + `x86_64`.** (User approved x86_64 for emulator — no ARM translation.)
  - Set in `build-logic/.../AndroidConfig.kt` (`abiFilters`), applied via the `anidesign.android.application` convention plugin.
  - Verified post-build in CI (the `build-apk.yml` "Verify ABIs" step inspects every APK's `lib/` folder and fails on any unexpected `lib/<abi>/`).
  - Ship via **App Bundle (AAB)** to Play + a **universal APK** for sideload/testing.
- **App ID:** `com.testplaybyte.anidesign` (placeholder — confirm with user).
- **compileSdk = 36, targetSdk = 36, minSdk = 26** (per R-4 research; ~98% device coverage, Play target requirement by Aug 2026).

---

## 9. Self-Learning

- **When the user corrects you, or you catch your own mistake** → log it immediately in `memory/lessons-learned.md`.
- Format: `- [TAG] lesson (source: <task-id or "self">, <date>)`
- Tags: `MISTAKE` (you did wrong), `CORRECTION` (user fixed you), `INSIGHT` (you realized), `PATTERN` (recurring).
- **Dedup**: grep existing entries for the keyword before adding. Don't log the same lesson twice.
- **Review**: at task start, grep `lessons-learned.md` for tags matching the current task type.
- **Stale**: mark `~~strikethrough~~` with `→ superseded by <ref>` when a newer lesson contradicts.
- If a lesson is a recurring pattern → also add a **one-line rule** to the relevant section of this file.

---

## 10. Patterns to Avoid

- ❌ **Dependencies between skills.** Each skill in `skills/` is standalone. One skill must not require another to run.
- ❌ **Complex build systems or test frameworks.** Maintain simplicity. One runnable self-check for non-trivial logic is enough. No frameworks unless explicitly requested.
- ❌ **Generic advice.** Every rule must be specific and actionable. "Write clean code" = useless. "Function ≤ 30 lines or split" = useful.
- ❌ **Over-documenting file structure.** Document what's non-obvious. Don't narrate every folder.
- ❌ **Boilerplate "for later".** Later can scaffold for itself.
- ❌ **Deletion disguised as addition.** Don't add prose that defends a simplification — delete the prose.
- ❌ **Material Design imports.** This project is explicitly NON-Material. Do not import `androidx.compose.material3.*` components (`Button`, `Card`, `TopAppBar`, `Scaffold`, etc.) in feature UI code. Reuse only low-level primitives (`Surface`, `Ripple`, `ModalBottomSheet`) under the hood of our own design-system components, with explicit justification. CI enforces this via a custom Detekt rule. (See §34.)

---

## 11. Task Notification (optional)

- **After completing every task**, send a notification via `ntfy.sh` (IF the user has confirmed the topic):
  ```bash
  curl -fsSL -H "Title: ANDROIDDESIGN Agent" -d "<short result, one line>" https://ntfy.sh/<topic>
  ```
- **Topic:** deferred — ask the user. The old project used `TASKISDONE` but that was public + guessable.
- ⚠️ **Note**: ntfy.sh topics are public. Don't put secrets in the message body. If adopted, switch to a long random topic stored in a GitHub secret.
- This rule is **optional** until the user picks a topic. Don't fail a task just because the notification didn't send.

---

## 12. Skill Management

- Skills live in `skills/` (inside AGENT-CONTEXT or a dedicated skills folder — TBD). Each is a standalone markdown file.
- **To add a skill**: (1) understand it fully, (2) verify it's reliable + useful, (3) sub-agent review if non-trivial, (4) write it with concrete examples (no generic philosophy), (5) add to `skills/README.md` index.
- **To create a new skill yourself**: must have a solid reason + solid backing. Use sub-agents to verify. If unsure, don't add it.
- Skills are **reference material**, not dependencies. The agent reads them on demand.

---

## 13. User Uses Speech-to-Text

- The user often dictates messages via speech-to-text. Transcription errors happen (misheard words, dropped words, odd phrasing).
- **If a request feels off or ambiguous**: try to correct obvious transcription errors from context. If still unclear → **stop and highlight it with the user** before proceeding. Do not move in the wrong direction on a misheard instruction.
- Common tells: homophones ("their/there"), numbers spelled out, slightly wrong technical terms. Use project context to disambiguate.
- When in doubt: ask. A 10-second clarification beats an hour of wrong work.

---

## 14. Sub-Agent Delegation Scope

- The main agent delegates work to **sub-agents** (research, code review, focused implementation).
- **Sub-agents work ONLY inside the zone they're assigned.** A research sub-agent writes to `research/`; a feature sub-agent works inside `APP/ani-design/feature/<x>/`; a documentation sub-agent works inside its assigned doc folder.
- **Sub-agents must NOT touch `AGENT-CONTEXT/`** — no progress.md edits, no rule edits, no memory updates. They report back to the main agent, who updates AGENT-CONTEXT after verifying their work.
- When launching a sub-agent: tell it explicitly which folder(s) it may write to and which are off-limits.
- **Sub-agent budget:** at most 5 sub-agents per task/session (user-imposed). Use the best model available for each delegation. Prefer parallel execution when tasks are independent.

---

## 15. Session-End Backup (Push to GitHub) + Sandbox Recovery

- ⚠️ **This sandbox environment can clear out randomly.** Work not pushed to GitHub can be lost.
- **Every session MUST end with all changes committed and pushed to GitHub.** No exceptions.
- Before declaring a session done: `git status` must be clean, `git push` must be done.
- If the environment was cleared and re-cloned at session start: read `AGENT-CONTEXT/memory/progress.md` first to know where things stand, then continue.
- This rule exists because the environment is ephemeral; GitHub is the source of truth.

### Sandbox Recovery (if the environment feels off)

- **If anything feels off** — missing files, broken imports that were previously fine, stale state, weird errors that shouldn't exist, or the working tree doesn't match what `progress.md` says — **STOP and re-clone the repo from GitHub.** Don't try to patch over a corrupted environment.
- **How to re-clone:**
  1. Move the current (suspect) working dir aside: `mv /home/z/my-project/android-project/repo /home/z/my-project/android-project/repo.suspect`
  2. Clone fresh: `git clone https://github.com/testplay-byte/ANDROIDDESIGN.git /home/z/my-project/android-project/repo`
  3. Verify `AGENT-CONTEXT/memory/progress.md` exists + matches the last known state.
  4. If the suspect dir had uncommitted work, diff it against the fresh clone + manually port any salvageable changes.
- **Why re-clone instead of patch:** The sandbox can silently delete or corrupt files. Debugging a corrupted environment wastes hours. A fresh clone takes 30 seconds and guarantees a known-good state. GitHub is the source of truth — trust it over the local filesystem.
- **Prevention:** Push frequently (not just at session end). If you complete a major artifact, push it immediately. Don't accumulate uncommitted work.
- **Credentials re-hydration:** The GitHub PAT is provided by the user each session (it lives in `/home/z/my-project/android-project/credentials/.git-credentials`, outside the repo). If the sandbox cleared, the agent re-creates this file from the user-provided token at session start. NEVER commit the credentials folder.

---

## 16. Naming Consistency

- Keep naming schemes **consistent** across the project so searching is fast and reliable.
- **Files**: `kebab-case` for markdown/data files (`lessons-learned.md`, `open-questions.md`). `PascalCase` for Kotlin/TS classes. `camelCase` for functions/variables.
- **Folders**: `kebab-case` (`ani-design`, `core-rules-reference`). Uppercase for top-level project zones (`APP/`, `AGENT-CONTEXT/`, `REFERENCES/`, `research/` — lowercase `research/` is fine since it's data, not a code zone).
- **Gradle modules**: `:lower:case:colon` (`:core:designsystem`, `:feature:home`). Package `com.testplaybyte.anidesign.<module>`.
- **Decisions**: `D-NNN` (zero-padded, sequential). **Questions**: `Q-NNN`. **Research**: `R-N`. **Tasks**: `Task NN`.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`).
- If you need to rename something: update **all** references. Grep before and after.
- When creating a new file/folder/module: check existing naming patterns first. Match them.

---

## 17. Take As Much Time As Needed

- **Quality over speed.** Take as much time as a task needs to be done properly.
- Do not rush through steps to "finish faster." A rushed job creates rework.
- If a task is taking longer than expected: that's OK. Communicate progress to the user.
- **Do not skip steps** in the workflow (Understand → Verify → Implement → Verify → Move On) to save time.
- Sub-agent reviews, verification, documentation — all take time. They are not optional.
- "Done fast but wrong" is worse than "done slow but right."
- The only hard deadline is: **push to GitHub at session end** (§15). Everything else is quality-bound.

---

## 18. Filtered Console Logging

- **Proper console logging for everything.** Every significant action, state change, error, and network call must be logged with enough context to understand what happened and where.
- **Filtered**: Use log levels (VERBOSE / DEBUG / INFO / WARN / ERROR). Logcat tags per module (`AniDesign:Core:Database`, `AniDesign:Feature:Details`, `AniDesign:Core:Agent`, etc.). The user/developer can filter by tag + level.
- **Toggleable**: Logging can be turned OFF for performance (release builds). Controlled by a build config flag (`BuildConfig.DEBUG` default) + a runtime toggle in Settings for beta/debug builds.
- **What to log**:
  - ✅ INFO: screen navigation, user actions (tap, search), feature start/end.
  - ✅ DEBUG: repository queries, cache hits/misses, state transitions, DI module init, agent tool calls.
  - ✅ WARN: recoverable errors (retry, fallback), deprecated API usage, source backoff triggered.
  - ✅ ERROR: exceptions, failed network calls, DB errors, agent errors, with stack traces.
  - ✅ VERBOSE: detailed flow tracing (only when debugging a specific issue).
- **What NOT to log**: user credentials, tokens, personal data, full request/response bodies (log URLs + status codes only).
- **Implementation**: Use a central `Logger` wrapper (in `:core:common`) that respects the level + tag + toggle. Never call `Log.d()` directly — always go through `Logger`.
- **Performance**: When logging is OFF, the Logger is a no-op (zero overhead). Use `if (Logger.isEnabled)` guards around expensive log message construction.
- **Logcat filter format for Android Studio**: When giving the user a logcat filter to diagnose an issue, ALWAYS use this format (directly pasteable into Android Studio's Logcat filter bar):
  ```
  tag:AniDesign:Feature:Details | tag:AniDesign:Core:Network:AniList message~:(?i)(keyword1|keyword2)
  ```
  Rules:
  - Use `tag:` prefix for each tag, separated by ` | ` (space-pipe-space).
  - Add `message~:(?i)(keywords)` to filter by message content (case-insensitive regex). List keywords separated by `|` inside the group.
  - NEVER use `adb logcat` commands — the user uses Android Studio's Logcat panel, not the command line.
  - Replace the tags + keywords with the ones relevant to the issue being diagnosed.

---

## 19. Documentation Folder Organization (STRICT)

> Where documentation lives. Read this before writing ANY doc.

### Documentation zones — NEVER mix them:

| Zone | Path | What goes here |
|------|------|----------------|
| **Agent memory + rules** | `AGENT-CONTEXT/` | `memory/` (progress, decisions, lessons, open-questions, session-log), `rules/` (this file + workflow.md), `knowledge/` (quick-reference summaries). Versioned in repo. |
| **New app docs** | `APP/ani-design/DOCUMENTATION/` | Architecture plans, design decisions, DB schema docs, design-language rationale. The app's technical documentation. |
| **App design language** | `APP/ani-design/DESIGN-LANGUAGE.md` | The app's canonical design language (colors, typography, motion, components). ONE file. |
| **Research reports** | `research/` | Deep research reports (`R-N-*.md`). Read-mostly; written during planning phases. |
| **External references** | `REFERENCES/` | Vendored external repos (Cline), reference rulesets, read-only material. |

### Rules
1. **Agent memory/rules go in `AGENT-CONTEXT/`.** This is the only zone sub-agents must not edit directly (main agent only).
2. **New app architecture/research/design goes in `APP/ani-design/DOCUMENTATION/`.**
3. **Agent-facing summaries go in `AGENT-CONTEXT/knowledge/`.** Short, cross-reference the detailed docs.
4. **The app's design language** lives at `APP/ani-design/DESIGN-LANGUAGE.md` (one file, canonical).
5. **Before writing a doc**: ask "is this agent memory, app technical docs, design language, research, or reference?" → put it in the right zone.
6. **When in doubt**: ask the user. Don't guess the location.

### Verification
- After writing a doc, verify its location matches the table above.
- If you find a doc in the wrong zone: move it + update all cross-references (grep for the old path).

---

## 20. UI / UX Quality — Buttery Smooth Animations

> The user is a great fan of rich, buttery-smooth animations and beautiful, minimalistic UI. This is a quality bar, not an afterthought.

### Animation Requirements
- **Scrolling**: smooth scroll effects (parallax, fade-in on scroll, collapsible headers that animate). Never janky.
- **Screen transitions**: animated screen switches (fade, slide, shared element transitions where appropriate). Never instant cuts.
- **Button clicks**: MUST give user feedback — ripple, scale-down on press, color change, haptic. Never a dead tap.
- **Loading states**: smooth skeletons / shimmer, not jarring spinners where possible.
- **State changes**: animate UI state changes (expand/collapse, appear/disappear) — never pop in/out.

### Design Aesthetic
- **Simple, minimalistic, good-looking.** Not cluttered. Every element earns its place.
- **Follow `APP/ani-design/DESIGN-LANGUAGE.md` strictly.** The starting design language is intentionally simple (NOT Material) so users can customize it via the AI agent.
- **The design language is a living document.** When the user (or the AI agent) changes the design, update `DESIGN-LANGUAGE.md` AND the active `theme.json` token set.

### Performance
- **60fps target.** Animations must not drop frames. Use Compose's animation APIs correctly (`AnimatedVisibility`, `animateContentSize`, `SharedTransitionLayout`, `Animatable`).
- **No heavy work on the main thread** during animation. Offload to IO/background.
- **Test on low-end devices** (not just emulators).

---

## 21. Live Data Verification

> When the user makes a change, it must be verified AND reflected live on screen. No "change + manual refresh."

### Rules
1. **Every user action has immediate visual feedback.** If the user taps "Add to Library," the UI updates instantly (optimistic update), then confirms with the backend.
2. **Data changes propagate live.** Use `Flow`/`StateFlow` throughout. The UI observes state and recomposes automatically. Never poll.
3. **Verify changes persisted.** After a write, verify it landed (read-back or DB callback). If it failed, roll back the optimistic update + show an error.
4. **No silent failures.** If a save fails, the user MUST know. Toast/snackbar with the error + retry option.
5. **Cross-screen consistency.** If the user adds an anime to their library on the Details screen, the Library screen must reflect it without a manual refresh (shared state via Flow).
6. **AI design changes propagate live.** When the AI agent commits a design-token change, every visible screen recomposes to the new theme without restart.

### Implementation
- Repositories return `Flow<T>` for reads (reactive).
- Writes return `Result<T>` (success/failure) — handle both in the ViewModel.
- ViewModels expose `StateFlow<UiState>` — UI collects and renders.
- Optimistic updates: update the UI state immediately, then confirm with the backend. Roll back on failure.
- Design tokens: the active token set is a `StateFlow<DesignTokens>`. UI reads via `CompositionLocal`. Agent commits write to this StateFlow.

---

## 22. Database Documentation — Always Up to Date

> The database is a crucial part of the app. Its structure must be documented and kept in sync with the code at all times.

### Rules
1. **Dedicated documentation**: All database schema documentation lives in `APP/ani-design/DOCUMENTATION/database/`. One file per table group, plus a README index.
2. **Update on every change**: Whenever a table is added, modified, or removed (including columns, indexes, constraints), the corresponding documentation file MUST be updated in the SAME commit. No "document it later."
3. **Document what + why**: Each table documents its columns (name, type, constraints, description) AND why it exists (what problem it solves, what queries it supports).
4. **Migration log**: Every schema change must have a corresponding entry in `APP/ani-design/DOCUMENTATION/database/changelog.md` — what changed, why, when.
5. **ER diagram**: Keep the entity relationship diagram in `APP/ani-design/DOCUMENTATION/database/er-diagram.md` updated when relationships change.
6. **Verify before commit**: Before committing a DB change, verify the docs match the Room entities/DAOs. If they don't match, the commit is incomplete.

### File Structure
```
APP/ani-design/DOCUMENTATION/database/
├── README.md              — index of all tables + groups
├── er-diagram.md          — entity relationship diagram
├── changelog.md           — migration history (version, date, what changed)
├── media.md               — Media (anime/manga) cache
├── episode.md             — Episode metadata cache (multi-source merged)
├── media-list-entry.md    — user's list entries (per status)
├── airing-schedule.md     — airing schedule cache
├── user.md                — user profile cache
├── metadata-source-state.md — per-source fetch state (lastFetched, backoff, etc.)
├── design-snapshots.md    — AI agent's design-token snapshots (for rollback)
├── sorting-rules.md      — sorting_rules table (custom sort DSL per list)
├── screen-layouts.md     — screenLayouts section of theme.json (layout variant selections)
└── app.md                 — app_metadata (version, last backup, etc.)
```

---

## 23. Documentation Verification (Continuous)

> Docs drift is silent and corrosive. A stale doc is worse than no doc — it actively misleads the next session and erodes user trust.

### Rules
1. **Same-session updates (reinforces §6):** When code, state, or decisions change, update the relevant docs in the SAME commit/session — not "later."
2. **Verify at task end (the verification gate):** Before declaring a task done, do a **drift check**:
   - Re-read the docs you touched this session. Do they match what you actually built?
   - `grep` for stale references: old phase numbers, old module counts, removed decision IDs, deleted file paths, renamed modules. Fix every hit.
   - If a doc says "X modules built" or "Phase N complete" — verify by counting/inspecting the actual code. Don't trust the doc's prior claim; re-derive it.
3. **Cross-check claims against reality:** If progress.md says "Phase 2 done" but the code shows Phase 2 work is incomplete, fix the doc (not the code) — OR fix the code and update the doc. Never leave them disagreeing.
4. **Lessons audit:** When you catch a doc-drift mistake (yours or a prior session's), log it in `lessons-learned.md` with the `[PATTERN]` tag. If drift recurs, promote a stricter rule here.
5. **Honesty about drift:** If you discover drift you can't fully fix in this session (e.g. a large doc rewrite needed), flag it explicitly to the user + note it in `progress.md` under a "Known doc debt" section. Don't silently leave it.

---

## 24. Tool Failure Recovery (Stop After 5 Tries)

> When a tool (Bash, Read, Edit, etc.) fails repeatedly, hammering it wastes context and time. The environment often self-recovers if you pause.

### Rules
1. **Stop after 5 consecutive failures** of the same tool with the same/similar error. Do NOT keep retrying — it won't help and burns context.
2. **Acknowledge the failure to the user** — tell them the tool is erroring and you're pausing. The user may need to reset the session or wait.
3. **Do NOT retry in a tight loop.** After the 5th failure, stop calling that tool entirely for the rest of the turn. Move to a different tool or describe what you would have done.
4. **The environment often self-recovers.** If the user says "continue" or sends a new message, try the tool again — it may work now.
5. **Log the failure** in `lessons-learned.md` with the `[PATTERN]` tag if it recurs across sessions.
6. **If a critical action is blocked** (e.g. can't `git push`), tell the user explicitly: "I can't push to GitHub right now because Bash is failing. The changes are saved locally. Please retry in a new message or run `git push` manually."

---

## 25. Log Comparison Debugging (When Stuck in Circles)

> When debugging a feature that "should work but doesn't" — and you've been going in circles for multiple sessions — **stop guessing and compare logs line-by-line** between the working reference and the broken implementation.

### Rules
1. **Ask the user for logs from BOTH projects.** One log from the working (reference) project, one from the broken (new) project. Same scenario.
2. **Compare the exact sequence of events.** Don't skim — list every event/error/warning in order for both projects, side by side. The difference is the bug.
3. **Look for MISSING events, not just extra ones.** If the reference fires `FILE_LOADED` but the new project fires `FILE_ERROR` + `PLAYBACK_RESTART` — the missing `FILE_LOADED` is the root cause.
4. **Don't assume your code is correct.** Even if sub-agents confirmed "the code is correct," the code may be correct BUT the prerequisite never fires.
5. **Don't go in circles.** If you've tried the same fix approach 3+ times and it doesn't work, you're fixing the wrong thing. Step back, get logs from both, and compare. The answer is in the diff.
6. **Document the root cause.** Once found, log it in `lessons-learned.md` with the `[PATTERN]` tag. Future agents should not repeat the same circle.

---

## 26. Crash Handling (Global Safety Net)

> The app MUST NOT silently crash to the home screen. Every uncaught exception shows a user-facing error screen with copyable logs.

### Rules
1. **Global crash handler installed in `Application.onCreate()`** — `Thread.setDefaultUncaughtExceptionHandler(AniDesignCrashHandler(this))`. Installed FIRST, before any other initialization (Logger, Koin).
2. **Crash report persisted to `filesDir/last_crash.txt`** — survives the process restart so `ErrorActivity` can read it.
3. **`ErrorActivity` launched with `NEW_TASK | CLEAR_TASK`** — replaces the crashed process with a fresh one showing the error screen.
4. **Error screen shows:** error icon + "Something went wrong" heading + explanation + scrollable monospace crash log + Copy button + Restart button + Close button.
5. **Copy button** copies the full crash report (timestamp, thread, device info, exception, stack trace) to the clipboard.
6. **Restart button** clears the crash report + launches `MainActivity` fresh.
7. **Close button** clears the crash report + finishes the activity.
8. **`ErrorActivity` registered in AndroidManifest** — `android:exported="false"`, same theme as `MainActivity`, `configChanges` to prevent recreation.

---

## 27. Debug-Build Schema Freedom

> The project is currently in **debug builds only**. There are no production users. There is no published APK. This gives us freedom to make complete schema changes without migration concerns.

### Rules
1. **Debug builds can rebuild the schema freely.** You CAN drop tables, add columns with NOT NULL, add CHECK constraints, change column types, restructure relationships — without writing migration scripts. The simplest approach (if a schema change is complex) is to bump the DB version + let Room's `fallbackToDestructiveMigration` recreate the tables. Existing dev-install data will be wiped — that's acceptable for debug builds.
2. **No migration scripts needed for debug.** When publishing approaches, the user will explicitly tell you. At that point, you MUST write proper migrations for every schema change that affects existing data. Do NOT assume publishing is imminent — wait for the user's signal.
3. **This rule supersedes any "preserve existing data" guidance** when the project is still in debug. Once the user signals production approach, this rule is suspended + migration discipline returns.
4. **The testing workflow is**: delete the app → reinstall → fresh DB → test. If a dev install has stale data, the user clears app data. That's the debug workflow.

---

## 28. AI Agent — The Design-System Agent

> The app embeds a full-fledged AI agent (architectural inspiration: Cline, ported to idiomatic Kotlin — see `research/R-1-cline-agent.md`). The agent's PRIMARY job is design-system customization: it takes natural-language requests ("make the home screen darker and use a palette from this image"), reasons about the current design tokens, and applies changes via a controlled tool surface. It is NOT a general-purpose coding agent.

### Rules
1. **Module shape**: `:core:agent:core` (agent loop, context manager), `:core:agent:llm` (provider abstraction — Anthropic, OpenAI, OpenRouter, Gemini, + OpenAI-compatible generic), `:core:agent:tools` (tool registry), `:core:agent:permissions` (approval gateway). Pure Kotlin where possible; Android-only at the edges.
2. **Agent loop**: iterative `while(isActive)` Kotlin coroutine (NOT recursive like Cline's `recursivelyMakeClineRequests`). Read context → decide action → call tool → observe → repeat until `attempt_completion`.
3. **Context management**: quarter-truncation strategy — start truncating at **50% remaining context** (NOT 70% — mobile constraints: smaller context windows + cost sensitivity). Drop ~25% oldest undeletable messages, preserve the initial exchange + the most recent tool results. File-read dedup with `[DUPLICATE FILE READ]` notices. (Per R-1 §11.4 mobile-specific recommendation.)
4. **Tool surface** (the ONLY way the agent touches the app state):
   - `read_design_tokens` — read the current `theme.json`.
   - `apply_token_patch` — apply a JSON-Patch (RFC 6902) to the design tokens.
   - `apply_text_patch` — SEARCH/REPLACE for free-form text (rules, notes).
   - `set_color_role(role, value)` / `set_typography(...)` / `set_shape(...)` / `set_motion(...)` — convenience wrappers over `apply_token_patch`.
   - `apply_image_palette(image_uri, mapping)` — extract palette from an image and map to roles.
   - `swap_layout(screen, layout_id)` — swap a screen's layout variant.
   - `set_component_variant(component, variant)` — swap a component variant.
   - `set_sorting_rule(list, rule)` — write a logic-based sorting rule (user can ask the AI to build a custom sort).
   - `preview` / `commit` / `rollback` — stage changes in a preview StateFlow; only `commit` writes to the active token set; `rollback` undoes the last commit.
   - `ask_user(question)` — when the agent needs clarification.
   - `attempt_completion(summary)` — end the task.
5. **Approval flow**: destructive tools (`apply_token_patch`, `swap_layout`, `set_component_variant`) require user approval by default. The user can toggle auto-approve per tool in Settings. Approval is a `suspend` callback — no polling.
6. **Snapshots / rollback**: before each `commit`, snapshot the current token set into `design_snapshots` table (Room). Cap at 50 snapshots (oldest evicted). The user can browse + restore snapshots from Settings → Design History. (This replaces Cline's shadow-git checkpoints, which are unviable on Android.)
7. **LLM streaming**: stream responses via SSE (Server-Sent Events) for token-by-token rendering in the agent chat UI.
8. **No terminal, no filesystem browsing, no MCP-stdio**: drop Cline's `bash`/`execute_command`, browser automation, and stdio MCP. The agent operates ONLY on design tokens + layout/component variants + sorting rules. HTTP-based MCP servers can be added post-MVP if a use case emerges.
9. **Provider key storage**: the user's LLM API key is stored in Android Keystore (encrypted). Never logged, never in backups unless the user explicitly opts in (§31).
10. **The agent is OPTIONAL.** The app must be fully usable without ever invoking the agent. The agent is a power-user feature for design customization. The design-token system + preset picker + image-palette flow MUST be usable by hand (no agent) — see §29.1.
11. **Iteration cap**: cap agent iterations at **25 per task** (mobile battery + LLM cost protection). If the agent hits the cap without `attempt_completion`, it stops + asks the user how to proceed (continue / abort / simplify the goal). Configurable in Settings (default 25). (Per R-1 §14.1 mobile recommendation.)
12. **Per-run cost guard**: the agent shows an estimated token-cost summary before each run (input + output tokens × user's provider rate, if the user set their rate in Settings). The user can set a soft per-run token budget; the agent warns (not blocks) when exceeded.

---

## 29. Design Tokens — Source of Truth

> The app's visual design is defined by a **single, versioned, serializable token set** (`theme.json`). This is the AI agent's primary edit surface and the user's backup/restore unit.

### Rules
1. **One canonical file**: `theme.json` (schema version `"1.0"`). Sections: `colors` (per role), `typography` (font family refs + sizes + weights), `shapes` (corner radii), `motion` (durations + easings), `elevation`, `spacing`, `componentVariants`.
2. **Loaded at runtime** into a `StateFlow<DesignTokens>`. UI reads via `CompositionLocal` (`LocalColors`, `LocalTypography`, `LocalShapes`, `LocalMotion`, `LocalSpacing`, `LocalElevation`).
3. **NOT Material**: do NOT use `MaterialTheme`'s 29 fixed color roles. Define our own role set (background, surface, surfaceVariant, primary, accent, accentVariant, outline, error, success, warning, info, etc.).
4. **Multiple named themes**: the user can save multiple `theme.json` sets ("Midnight", "Paper", "Sunset", custom) and switch between them instantly.
5. **Font families** are bundled (res/font) and referenced by key in the token set. Swapping the font family is a token edit, not a code change. (See §34 — bundle Inter + Sora + JetBrains Mono, all OFL.)
6. **The AI agent edits tokens, not code.** The agent never writes Kotlin/Compose files. It only writes to `theme.json` (+ sorting rules + layout/component variant selections).
7. **Schema evolution**: if the token schema changes, bump `$schema` version + write a migrator. Old backups must still restore (migrate on import).
8. **Layout selections** live IN `theme.json` under a `screenLayouts` section (a map of `screenId → layoutId`). Swapping a screen's layout is a token edit (the `swap_layout` agent tool writes here). The UI reads the active layout id per screen + renders the corresponding composable variant.
9. **Component variant selections** live IN `theme.json` under `componentVariants` (a map of `componentKey → variantId`). The `set_component_variant` agent tool writes here.
10. **Sorting rules** live in a **separate Room table `sorting_rules`** (NOT in theme.json — sorting is data behavior, not visual design). Schema: `id, list_type (ANIME_LIST|MANGA_LIST|...), rule_id, dsl_text, is_active, created_at, updated_at`. The DSL is a simple expression: `sort by: score desc, title asc, status asc` (parseable by a small parser in `:core:data`). The `set_sorting_rule` agent tool writes here. The user can also edit sorting rules by hand in Settings → Sorting.

### 29.1 User-Facing Theme Editing (No Agent Required)

> Reinforces §28 rule 10. The app MUST be fully customizable WITHOUT the AI agent. The agent is a convenience, not a dependency. If the only way to change the theme were via the agent, a user without an LLM API key (or who doesn't want AI) would be locked into the starter theme — unacceptable.

1. **Settings → Theme Editor**: a JSON editor screen where the user can view + edit the raw `theme.json` (with syntax highlighting + validation). Power users can hand-edit tokens.
2. **Settings → Preset Themes**: a picker for bundled + saved themes ("Midnight", "Paper", "Sunset", custom). Switching is instant (writes to active `StateFlow<DesignTokens>`).
3. **Settings → Image → Palette**: the user picks an image, the app extracts a palette (§33), previews it, and commits — all WITHOUT the agent. The agent's `apply_image_palette` tool calls the same `ThemeRepository.commitFromPalette(image, mapping)` method.
4. **Settings → Design History**: the user can browse + restore design snapshots (§28 rule 6) — also without the agent.
5. **The agent's tools call the same repository methods as the manual UI.** There is ONE `ThemeRepository` with methods like `commit(tokens)`, `commitFromPalette(image, mapping)`, `rollbackTo(snapshotId)`, `savePreset(name, tokens)`. The agent's `commit` tool + the manual "Save" button both call `ThemeRepository.commit()`. No duplication — the agent is just another caller of the same repository.

---

## 30. Multi-Source Data Merge (AniList + Kitsu + Jikan)

> Episode metadata (thumbnails, titles, descriptions, air dates) comes from multiple sources because no single source is complete. (See `research/R-3-kitsu-jikan.md`.)

### Rules
1. **AniList is primary** for: media metadata (title, cover, banner, episodes count, status, season, genres, tags, score, relations, characters, studios, nextAiringEpisode), user lists, airing schedule, notifications.
2. **Kitsu is primary** for: per-episode thumbnails, per-episode synopsis, per-episode canonical/en/jp titles, per-episode duration (minutes).
3. **Jikan is primary** for: per-episode air date (timezone-aware ISO8601), filler/recap flags, per-episode score (1-5). (Jikan's per-episode synopsis endpoint is fragile — 504s frequently — use only as lazy fallback when Kitsu synopsis is missing.)
4. **ID mapping**: AniList `Media.idMal` → Jikan (direct). AniList → Kitsu via Kitsu `mappings?filter[externalSite]=anilist/anime` (direct, cache forever) OR fallback via `myanimelist/anime`. Cache the kitsuId per mediaId forever (it doesn't change).
5. **Merge order** (per episode field):
   - Thumbnail: Kitsu → Jikan (sparse) → none
   - Title (en): Kitsu `canonicalTitle` → Jikan `title` → fallback "Episode N"
   - Title (jp): Kitsu `titles.ja_jp` → Jikan `title_japanese`
   - Air date: Jikan `aired` (TZ-aware) → Kitsu `airdate`
   - Synopsis: Kitsu `synopsis` → Jikan `synopsis` (lazy, on-expand)
   - Duration: Kitsu `length` (min) → Jikan `duration` (sec, convert)
   - Filler/recap: Jikan only
6. **Never overwrite existing episode metadata unless the new data is richer.** New episodes append. Existing episodes update only fields that were missing or null.
7. **Source state tracking**: `metadata_source_state` table per (mediaId, source) with `lastFetchedAt`, `lastSuccessAt`, `failureCount`, `backoffUntil`, `etag`. On 404/empty/malformed: increment failureCount, set backoffUntil = exponential (1m → 5m → 15m → 1h → 6h → 24h cap), keep serving last-good cache.
8. **Refresh cadence**: airing anime — re-fetch episode metadata once per day per anime (or on user pull-to-refresh). Completed anime — fetch once, cache forever (refresh only on explicit user request).
9. **All sources must be behind interfaces**: `EpisodeMetadataSource` interface with `KitsuSource`, `JikanSource` impls. The merge logic lives in `EpisodeMetadataRepository`. This makes adding a future source (e.g. AnimeSchedule.net) a one-file change.

---

## 31. Offline-First Data Layer

> The app must work fully offline. Network is a refresh opportunity, not a prerequisite for rendering.

### Rules
1. **Every screen renders from the local DB first.** Network fetches update the DB; the UI observes the DB via Flow and recomposes. No screen shows a loading spinner waiting on network if local data exists — it shows local data, then updates when network resolves.
2. **Cache schema** (Room, per R-4): `media`, `episode`, `media_list_entry`, `airing_schedule`, `character`, `studio`, `metadata_source_state`, `user`, plus FTS4 search index on media titles/synonyms.
3. **Reconciliation** (on refresh):
   - Media: field-level COALESCE (keep non-null from either side; prefer remote for fields the source is authoritative on).
   - List entries: last-write-wins per `source_updated_at` (AniList `MediaList.updatedAt`).
   - Episodes: append-never-overwrite (§30 rule 6).
   - 404/stale: keep local, mark stale, backoff (§30 rule 7).
4. **Image caching**: Coil 3, 250MB disk cache default, placeholder color from `MediaCoverImage.color` (AniList provides a hex tint per media — use it for the placeholder while the image loads).
5. **AniList auth token**: persisted in Android Keystore (encrypted). Never in plain SharedPreferences, never logged, never in backups unless user opts in (§32).
6. **Rate-limit handling**: single-flight queue per source; sliding 60s window; respect `Retry-After` header (AniList returns it). Dedupe in-flight requests. (AniList currently degraded to 30 req/min — design for that, not the documented 90.)
7. **GraphQL cache**: Apollo Kotlin normalized cache (in-memory + persistent SQLite). Query results populate the normalized cache; the DB holds the domain models.
8. **Pull-to-refresh**: always re-fetches from network (bypassing cache TTL) but does NOT block the UI — local data stays visible until new data lands.
9. **AniList token expiry handling**: the auth token is a 1-year JWT (D-017). On app launch, decode the JWT `exp` claim client-side. If expiry is within 7 days, show a non-intrusive banner ("AniList session expires in N days — re-link to refresh"). If expired, block all authenticated writes (mutations) + show a full-screen re-auth prompt. Read-only access (public AniList queries + all local cache) continues to work. The user re-links via OAuth to get a fresh 1-year token.

---

## 32. Backup & Restore

> The user can back up ALL app data and restore it. This includes the AniList token (encrypted) AND the user's design (theme.json + saved themes + design snapshots).

### Rules
1. **Backup contents** (single zip):
   - Room DB file (`anidesign.db`)
   - DataStore preferences
   - `theme.json` (active theme) + all saved themes
   - `design_snapshots` (agent history — optional, user-toggleable)
   - AniList auth token (encrypted with a user-set passphrase OR Android Keystore key wrapped by a user passphrase via PBKDF2)
   - Sorting rules / layout selections / component variant selections
   - `backup_manifest.json` (schema version, app version, timestamp, contents list)
2. **Weekly auto-backup**: WorkManager `PeriodicWorkRequest` (7 days), constrained to `NETWORK_TYPE_UNMETERED` + `DEVICE_IDLE` + `BATTERY_NOT_LOW` + `CHARGING` (user can relax constraints in Settings). **One rolling copy** — new backup replaces the previous auto-backup. Manual backups (user-initiated) are NOT overwritten by auto-backup.
3. **Backup location**: app-specific storage (`filesDir/backups/`) by default. User can export a backup to `MediaStore.Downloads` for cross-device transfer. Import reads from either location.
4. **Restore**: validates `backup_manifest.json` schema version. If older than current, runs migrators. If newer than current app, refuses with a clear error ("backup is from a newer app version"). Restoring OVERWRITES current data (with a confirmation dialog + the option to auto-backup current state first).
5. **Design restore**: when restoring, the active `theme.json` + saved themes + snapshots are restored exactly. The user does NOT lose their design customizations. (User explicitly required: "If the user restores the backup, then all of his designs and every single thing will be properly backed up and saved. When he restores, all the things will be restored exactly like that, and the user will not lose anything at all.")
6. **AniList token in backup**: encrypted. The user sets a passphrase at backup time (or opts to use device Keystore — but then the backup is non-portable across devices). On restore, prompt for the passphrase. If the user opts to NOT include the token, the backup works but the user must re-link AniList after restore.
7. **Never auto-back up the LLM API key.** The agent's provider key is device-bound (Keystore) and is NEVER included in backups. The user re-enters it after restore.
8. **Backup verification**: after writing a backup, the app reads it back + verifies the zip integrity + manifest checksums. If verification fails, delete the corrupt backup + notify the user.

---

## 33. Dynamic Theming (Palette from Image)

> The user can pick an image (e.g. an anime cover) and the app builds a theme from it. The AI agent can also do this via the `apply_image_palette` tool.

### Rules
1. **Palette extraction**: `androidx.palette` API. Extract up to 16 swatches. Map: dominant → surface/background, vibrant → primary/accent, muted → surfaceVariant, darkVibrant → outline/elevation. (Per R-4: simple HSL-shift tonal generation, NOT Material You HCT — too much color science for v1.)
2. **Role mapping is customizable**: the default mapping above can be overridden by the user (or the AI agent) per-image. The user can say "use the blue from the sky as the accent" and the agent picks the right swatch.
3. **Contrast check**: after mapping, verify WCAG AA contrast between text roles and their backgrounds. If contrast fails, auto-darken/lighten the text role until it passes. Never ship an unreadable theme.
4. **Preview before commit**: the generated theme goes into the preview StateFlow (§28 rule 4). The user sees the theme applied live and can commit or discard.
5. **The image itself is NOT stored** in the theme — only the extracted colors. (The user can re-pick a different image later.)

---

## 34. Non-Material Design Enforcement

> The user explicitly does NOT want Material Design. This is enforced in code, not just in intent.

### Rules
1. **Custom `AppTheme`**: a single composable that provides 6 `CompositionLocal`s (`LocalColors`, `LocalTypography`, `LocalShapes`, `LocalMotion`, `LocalSpacing`, `LocalElevation`). NOT `MaterialTheme`.
2. **Forbidden imports** in feature UI code (enforced by Detekt CI rule):
   - `androidx.compose.material3.Button`
   - `androidx.compose.material3.Card`
   - `androidx.compose.material3.TopAppBar`
   - `androidx.compose.material3.Scaffold` (use our own `AppScaffold`)
   - `androidx.compose.material3.NavigationBar` / `NavigationRail` (use our own `AppNavBar`)
   - `androidx.compose.material3.*` (any high-level component)
3. **Allowed primitives** (reused under the hood, with justification):
   - `androidx.compose.foundation.Surface` — basic surface behavior.
   - `androidx.compose.material3.Ripple` — ripple effect (or our own if we want non-Material ripple).
   - `androidx.compose.material3.ModalBottomSheet` — bottom sheet mechanics (or a third-party like `Sheet` — decide in implementation).
   - These are allowed because they're low-level mechanics, not visual design decisions.
4. **Custom components** live in `:core:designsystem`. Feature modules depend on `:core:designsystem`, never on `material3` directly.
5. **Design language doc**: `APP/ani-design/DESIGN-LANGUAGE.md` defines the starter design language (intentionally simple, customizable). The AI agent + user edit `theme.json` to customize; the doc is the human-readable spec.
6. **No Material icons mandate**: use Lucide-for-Compose or our own icon set. Material icons are allowed if their visual style fits, but the design system is not Material.

---

## 35. Bundled Fonts

> Fonts MUST be bundled (per user). Do not rely on system fonts for the design language — bundling avoids "font not found at runtime" issues and ensures the design renders identically across devices.

### Rules
1. **Starter set** (all OFL-licensed): Inter (body), Sora (display), JetBrains Mono (monospace/numbers). Bundled as `.ttf` in `res/font/`.
2. **Font registry**: `FontRegistry` maps token keys (`"body"`, `"display"`, `"mono"`) → `FontFamily`. The AI agent can swap a family by editing the token set + adding the new font file to `res/font/` (post-MVP; for v1, the agent picks from the bundled set).
3. **APK size**: ~1.8MB for the three families. Acceptable.
4. **Variable fonts**: prefer variable fonts where available (smaller + more flexible). Inter and Sora both ship variable versions.
5. **License attribution**: OFL fonts require attribution. Add a "Open Source Licenses" screen in Settings listing the fonts + their licenses.

---

## 36. Session-End Checklist

Before declaring a session done:

- [ ] All code changes committed + pushed to GitHub.
- [ ] `AGENT-CONTEXT/memory/progress.md` matches reality (phase, what's done, what's next).
- [ ] `AGENT-CONTEXT/memory/decisions.md` has any new `D-NNN` from this session.
- [ ] `AGENT-CONTEXT/memory/lessons-learned.md` has any new lesson from this session.
- [ ] `AGENT-CONTEXT/memory/open-questions.md` is up to date (answered questions removed or marked answered; new questions added).
- [ ] No stale phase references in any doc.
- [ ] Credentials file (`/home/z/my-project/android-project/credentials/`) is NOT committed. Verify `.gitignore` covers it.
- [ ] If the sandbox might clear: note the next session's first step in `progress.md` ("Next session: read this file, then do X").

---

## Appendix A: What changed from the reference (ANI-KUTA) rules

| Reference section | Status | Reason |
|-------------------|--------|--------|
| §1–3 (Dev Flow, Comm, Summary) | Kept | Timeless mindset. |
| §4 (Project Structure) | Adapted | Wrapper folder `ANI-KUTA` → `ANDROIDDESIGN`; zones reduced (no DASHBOARD zone unless user requests). |
| §5 (Code Rules) | Adapted | Dropped the MPV player-lifecycle carve-out (no media player in this project). |
| §6 (Documentation) | Kept | — |
| §7 (Architecture) | Adapted | Dropped the MPV player-screen carve-out. Added AI-agent-as-architecture rule. |
| §8 (GitHub Actions) | Adapted | Added `x86_64` ABI; new app id; new SDK targets; removed Nav3 reference. |
| §9–10 (Self-Learning, Patterns) | Kept | — |
| §11 (ntfy.sh) | Made optional | Topic deferred — ask user. |
| §12–13 (Skills, Speech-to-Text) | Kept | — |
| §14 (Sub-Agent Scope) | Adapted | Generalized beyond "webpage sub-agents" to any zone. Added 5-sub-agent budget. |
| §15 (Session-End Backup) | Kept + strengthened | Added credentials re-hydration note. |
| §16 (Dashboard Design Language) | Dropped | No dashboard this project. Re-add if user requests. |
| §17 (Naming) | Adapted | New package, new module conventions. |
| §18 (Take Time) | Kept | — |
| §19 (Webpage Full-Stack-Dev Agent) | Dropped | No dashboard. |
| §20 (Filtered Logging) | Kept | Tags renamed `Anikuta:` → `AniDesign:`. Added agent tag. |
| §21 (Doc Folder Org) | Adapted | New zones (no old-kuta, no DASHBOARD). |
| §22 (UI/UX Animations) | Kept | — |
| §23 (Live Data) | Kept + extended | Added design-token live propagation. |
| §24 (Database Docs) | Kept | Path adjusted; table list updated. |
| §25 (Dashboard Keep Up to Date) | Dropped | No dashboard. |
| §26 (Doc Verification) | Kept | — |
| §27 (Tool Failure) | Kept | — |
| §28 (Log Comparison) | Kept | — |
| §29 (Crash Handling) | Kept | Class renamed. |
| §30 (Debug Schema Freedom) | Kept | SQLDelight → Room wording. |
| **NEW §28–§35** | Added | AI agent, design tokens, multi-source merge, offline-first, backup/restore, dynamic theming, non-Material enforcement, bundled fonts. |
