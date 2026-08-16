# V-1 — Planning Artifacts Review

> Critical review of the Phase 0 planning package for the ANDROIDDESIGN project.
> Reviewer: V-1 sub-agent (general-purpose).
> Scope: `CORE_RULES.md`, `knowledge/architecture.md`, `memory/decisions.md`,
> `memory/open-questions.md`, `memory/progress.md`, `memory/lessons-learned.md`,
> `README.md`, `REFERENCES/README.md`, `REFERENCES/core-rules-reference.md`,
> plus cross-reference against `research/R-1..R-4.md`.
>
> Review rules: do NOT modify any planning artifact. Review only — the main
> agent incorporates the feedback.

---

## Verdict

**APPROVE WITH MINOR FIXES**

The planning package is internally coherent, well-researched, and traceable.
None of the issues block the user from being asked the open-questions list.
However, six issues rise to MAJOR severity and should be addressed before the
user answers — because they shape how the user should answer. The remaining
issues are MINOR/NIT and can be fixed in-session.

---

## Strengths

1. **Research base is real and primary-sourced.** The 4 research reports (~315 KB total) cite live-probe logs (R-2 Appendix B, R-3 §11 references) rather than relying on documentation snippets. AniList rate-limit degradation to 30/min was verified live; Kitsu CDN domain mismatch (`media.kitsu.app` not `.io`) was caught by direct HTTP probe. This is unusual rigor.

2. **Decision IDs are traceable end-to-end.** D-001..D-019 each cite a Source (research report + section) and a Rationale. Architecture.md §2 cross-references each tech-stack row to its D-NNN. A fresh agent can follow any claim back to primary research.

3. **Open-questions are clearly tiered.** 12 blocking / 13 non-blocking / 6 deferred — the user knows what they must answer before Phase 1 vs. what the agent will proceed with by default.

4. **ANI-KUTA leakage is handled explicitly, not silently.** CORE_RULES §0 header + Appendix A both state which reference sections were KEPT / ADAPTED / DROPPED. MPV, SQLDelight, `app.confused.anikuta`, ARM-only, DASHBOARD are each called out as dropped. The reference file lives at `REFERENCES/core-rules-reference.md` as read-only structural inspiration.

5. **"Non-Material" is enforced in code, not just intent.** CORE_RULES §34 + D-002 specify a Detekt CI rule that forbids `androidx.compose.material3.*` imports in feature code, with explicit allowed primitives (`Surface`, `Ripple`, `ModalBottomSheet`). This is the right level of operationalization.

6. **Backup/restore is treated as a first-class feature.** CORE_RULES §32 covers backup contents, weekly cadence with WorkManager constraints, location, restore validation, design-token preservation, and explicitly forbids LLM key inclusion. This is more thorough than most production apps.

7. **Multi-source merge is field-level explicit.** CORE_RULES §30 rule 5 lists per-field priority (Thumbnail→Kitsu, Filler→Jikan-only, Air date→Jikan TZ-aware, etc.) — not just "merge somehow."

8. **Lessons-learned is seeded with concrete insights, each with a source + date tag.** 19 entries with `[INSIGHT]` / `[PATTERN]` tags. The dedup-and-grep rule (§9) is in place.

9. **The "agent is OPTIONAL" claim is in CORE_RULES §28 rule 10** — not buried in a research report. This is the right elevation.

10. **Credentials handling is sound.** PAT stored at `/home/z/my-project/android-project/credentials/.git-credentials` (600 perms, outside the repo tree), `.gitignore` covers `credentials/`, `**/credentials/`, `**/.git-credentials`, `**/local.properties`, `**/api-keys.json`. Session-log §"Push status" records a secret scan: 0 PAT occurrences in the commit.

11. **Architecture.md includes data-flow diagrams** (§4 offline-first, §5 design-token flow, §6 agent integration, §7 episode-metadata flow, §8 backup flow) — not just a module graph. A fresh agent can trace the runtime behavior.

12. **Repo structure matches CORE_RULES §4.** Single wrapper folder `ANDROIDDESIGN/` at repo root, `.github/workflows/` at repo root (GitHub constraint), zones inside the wrapper folder. 23 files committed in `3106091`, all intentional planning artifacts.

---

## Issues Found

### BLOCKER

None. The plan is shippable to the user as-is for review.

### MAJOR

#### MAJOR-1 — "Agent is OPTIONAL" guarantee is not architecturally complete

**Files:** `CORE_RULES.md` §28 rule 10, §29, §33; `knowledge/architecture.md` §5

**What's wrong:** §28 rule 10 says "The app must be fully usable without ever invoking the agent." But the only documented paths to mutate design tokens are:
- `apply_token_patch` (agent tool — §28 rule 4)
- `apply_image_palette` (agent tool — §28 rule 4)
- "Preview before commit" (§33 rule 4) — but the user is shown a preview, and `commit` is an agent tool

There is **no documented user-facing path** to: (a) edit `theme.json` by hand in a Settings JSON editor with live validation, (b) commit a palette extraction without invoking the agent, or (c) save a snapshot manually. A fresh agent implementing Phase 1 could ship a UI where the ONLY way to change the theme is via the AI agent — directly contradicting §28 rule 10.

**Suggested fix:** Add a new CORE_RULES section (or extend §29):

> **§29.1 User-facing theme editing (no agent required)**
> 1. Settings → "Edit Theme JSON" — a JSON editor with live validation against the token schema. Saving writes to the active `StateFlow<DesignTokens>` and snapshots to `design_snapshots` via the same `ThemeRepository.commit()` method the agent uses.
> 2. Settings → "Pick Preset Theme" — instant switch between bundled presets ("Midnight", "Paper", "Sunset"). Switching calls `ThemeRepository.setActive(themeId)`.
> 3. Settings → "Pick Image → Generate Theme" — non-agent path to §33 dynamic theming. User picks an image, the app extracts the palette + applies the default mapping + shows a preview + a "Save" button (the same `commit()` method).
> 4. The agent's `commit` tool calls the same repository method. There is ONE commit path; the agent is just one of multiple callers.

This makes the "agent is OPTIONAL" claim architecturally enforceable, not aspirational.

---

#### MAJOR-2 — Sorting rules + layout selections have no defined storage location

**Files:** `CORE_RULES.md` §28 rule 4 (`set_sorting_rule`, `swap_layout`), §31 rule 2 (cache schema), §22 (DB doc structure), §32 (backup contents)

**What's wrong:**
- §28 rule 4 lists `set_sorting_rule(list, rule)` as an agent tool.
- §32 rule 1 lists "Sorting rules / layout selections / component variant selections" as backup contents.
- But §31 rule 2 (Room cache schema) lists only: `media`, `episode`, `media_list_entry`, `airing_schedule`, `character`, `studio`, `metadata_source_state`, `user` — **no `sorting_rules` table, no `screen_layouts` table**.
- §22 file structure has no `sorting-rules.md` or `layout-selections.md`.
- R-4 §D22 says `swap_layout` writes to a `screen_layouts` JSON, but CORE_RULES doesn't say where this JSON lives (in `theme.json`? in a separate file? in Room?).
- §29 (Design Tokens) lists `componentVariants` as a `theme.json` section but doesn't mention `screenLayouts` or `sortingRules`.

A fresh agent implementing Phase 2 will have to invent the storage location, and the result will likely diverge from what Phase 4 (agent) expects.

**Suggested fix:** Add to §29:

> 7. **Layout selections** live in `theme.json` under `screenLayouts` (a `Map<screen, layout_id>`). The agent's `swap_layout` tool edits this section. The user can also edit this via Settings → "Layout Preferences" (ties to MAJOR-1).
> 8. **Sorting rules** live in a Room table `sorting_rules(id, list_type, rule_dsl, created_at, updated_at)` keyed by `(list_type, rule_id)`. Exported as JSON in backups. DSL is `sort by: score desc, title asc` (free-form text — same as R-3 §6). Documented in `APP/ani-design/DOCUMENTATION/database/sorting-rules.md`.

Add `sorting-rules.md` to the §22 file structure list.

---

#### MAJOR-3 — Seven risks missed by the "Highlights & Concerns" section

**File:** `memory/open-questions.md` §"Highlights & Concerns" (items 1–10)

**What's wrong:** The 10 highlights cover the right surface (rate limits, no push, Jikan fragility, agent scope, Implicit Grant, sandbox, no local builds, ANI-KUTA reference, non-Material, x86_64). But these risks are not flagged:

| # | Missing risk | Source |
|---|--------------|--------|
| a | **LLM cost** — every agent run costs the user money via their own API key. No spending cap, no per-session token budget, no "free preview" tier. | R-4 open-questions §3 |
| b | **Battery drain** from long agent loops — R-1 §14.1 explicitly recommends a hard iteration cap of 25 with a user-visible countdown. Not in CORE_RULES. | R-1 §11.4, §14.1 |
| c | **AniList ToS compliance** — AniList's ToS section 4 prohibits "automated requests beyond what's reasonable." A polling cadence + single-flight queue could trip this if scaled. The app should mention this in a Privacy/ToS screen. | (inferred from R-2; not explicitly cited) |
| d | **Image CDN hotlinking** — AniList (`s4.anilist.co`), Kitsu (`media.kitsu.app`), Jikan (`cdn.myanimelist.net`) images are loaded by Coil 3 directly. Hotlinking may violate ToS or incur bandwidth costs for the source. Should at least be acknowledged + (ideally) cached aggressively to reduce source load. | R-2 §7, R-3 §1.6 |
| e | **GDPR / data-privacy for backups** — §32 rule 3 lets the user export a backup to `MediaStore.Downloads` (shared storage). If the user picks "no passphrase," the AniList token (encrypted only with device Keystore) is in shared storage in plaintext-to-other-apps form. This is a GDPR / data-privacy concern for any user who exports a backup. | (gap in §32) |
| f | **OAuth custom-scheme interception** — Q-006 proposes `<applicationId>://anilist-auth`. Another Android app registering the same custom scheme could intercept the OAuth redirect and capture the JWT. R-2 §14.1 explicitly recommends Android App Links (`https://` + `assetlinks.json`) as the v1+ hardening. This is a real attack vector. | R-2 §14.1, §2.9 |
| g | **Backup passphrase loss** — if the user sets a passphrase, forgetting it = data loss. R-4 open-questions §6 flags this. Not in highlights. | R-4 §Q6 |

**Suggested fix:** Add items 11–17 to the Highlights & Concerns section, each one paragraph + a pointer to the relevant research section. Items (a), (b), (e), (f) should also be promoted to blocking questions:
- New Q-033: "LLM cost guardrails? Options: (a) hard iteration cap (default 25), (b) per-session token budget with user-set ceiling, (c) no cap (user trusts themselves)."
- New Q-034: "AniList OAuth: ship custom-scheme redirect in v1 (faster, vulnerable to interception by malicious apps registering the same scheme) or implement Android App Links from day 1 (slower setup, requires `assetlinks.json` hosting)?"
- New Q-035: "Backup export to MediaStore.Downloads: allow without passphrase (convenient, shared-storage readable) or require passphrase for any cross-device export (inconvenient, secure)?"

---

#### MAJOR-4 — Auth flow has no token-expiry handling

**Files:** `CORE_RULES.md` (no auth section), `memory/decisions.md` D-017

**What's wrong:** D-017 says "Tokens are 1-year JWTs." CORE_RULES has no rule for what happens when the token expires. R-2 §14.1 explicitly recommends: "Auto-logout: schedule a check 1 year from `token_issued_at`; show a 'Re-link AniList' prompt when within 7 days of expiry." This research recommendation did not make it into rules.

A fresh agent implementing Phase 2 (data layer) won't know to implement expiry handling.

**Suggested fix:** Add a new CORE_RULES §31.5 (or a new section "§36.5 Auth & Token Lifecycle"):

> 1. AniList JWT is decoded client-side on app launch; the `exp` claim is read.
> 2. If `exp` is within 7 days → show a non-blocking banner "Your AniList session expires in N days. Re-link to stay connected."
> 3. If `exp` has passed → on the next authenticated operation, surface a blocking dialog "Your AniList session has expired. Re-link to continue." All reads continue to work from cache; writes are blocked until re-auth.
> 4. There is NO refresh token (D-017). The user must re-auth explicitly via the OAuth flow.
> 5. The token's `sub` claim = user ID — cache it; skip the `Viewer` round-trip on cold start.

---

#### MAJOR-5 — R-1's iteration cap + context-truncation threshold not in CORE_RULES

**Files:** `CORE_RULES.md` §28 rule 3 (Context management)

**What's wrong:** R-1 §11.4 + §14.1 explicitly recommend:
- "Hard cap on iterations per session (default 25). User sees countdown." (battery mitigation)
- "Aggressive context truncation — start truncating at 50% utilization, not 70%." (memory-pressure mitigation, R-1 §14.4)

CORE_RULES §28 rule 3 says only "quarter-truncation strategy when approaching the context window limit" — no threshold, no iteration cap.

A fresh agent porting Cline wouldn't know to implement these mobile-specific mitigations.

**Suggested fix:** Extend §28 with two rules:

> 11. **Iteration cap (battery mitigation):** Hard cap of 25 LLM round-trips per agent session. User sees a countdown in the Design Studio UI. The cap is configurable in Settings. When the cap is hit, the agent calls `attempt_completion` with a "Hit iteration cap — partial result" summary. (R-1 §11.4.)
>
> 12. **Context truncation threshold (memory mitigation):** Begin quarter-truncation at **50%** of context-window utilization (not 70% as Cline does) — Android apps have smaller heaps and shorter sessions than desktop. Always preserve the system prompt + the 4 most-recent tool results verbatim. (R-1 §14.4.)

---

#### MAJOR-6 — No open-question about analytics / crash reporting / telemetry

**File:** `memory/open-questions.md`

**What's wrong:** The user is asked 31 questions but not whether the app should include Firebase Crashlytics, Sentry, PostHog, or any telemetry. This affects:
- Privacy policy + GDPR obligations (if shipping to Play)
- Third-party dependencies (each adds APK size + a privacy surface)
- CI setup (Firebase requires `google-services.json` — should be gitignored, currently not in `.gitignore`)
- CORE_RULES §26 (Crash Handling) shows the local crash UI but doesn't say whether crash logs are also sent to a remote aggregator

A fresh agent implementing Phase 2 won't know whether to wire telemetry.

**Suggested fix:** Add a blocking Q-036:

> ### Q-036 — Analytics / crash reporting
> Should the app include remote telemetry?
> - (a) None — crash logs stay in `filesDir/last_crash.txt` only (the §26 design). The user manually shares if they want to report a bug.
> - (b) Opt-in Firebase Crashlytics (requires `google-services.json` — adds Firebase SDK to deps).
> - (c) Opt-in Sentry (self-host or SaaS — requires DSN, no `google-services.json`).
> - (d) Opt-in custom endpoint (you provide a URL).
> Default: (a) none — privacy-by-default.

Also: if (b) or (c) is chosen, add `google-services.json` and `sentry.properties` to `.gitignore` (currently missing — see NIT-2 below).

---

### MINOR

#### MINOR-1 — JDK version inconsistency between R-4 and planning artifacts

**Files:** `research/R-4-android-design-system.md` line 1194 vs `.github/workflows/README.md` line 19 vs `knowledge/architecture.md` §9

**What's wrong:**
- R-4 line 1194: "androiddesign.android.application — applies `com.android.application`, sets compileSdk/targetSdk/minSdk, abiFilters, **Java 21**, Compose."
- `.github/workflows/README.md` line 19: "actions/setup-java@v4 (**JDK 17**)"
- `architecture.md` §9 line 202: "setup-java 17"

No explanation for the divergence. Both are valid (AGP 8.6+ requires JDK 17 minimum; JDK 21 is the modern LTS). But the inconsistency should be resolved or explained.

**Suggested fix:** Pick one and document it. Recommended: **JDK 17** (LTS, broadest AGP/Kotlin compatibility, fewer CI surprises). Add to CORE_RULES §8: "Build JDK: 17 (LTS), pinned in `actions/setup-java@v4`. R-4's JDK 21 suggestion deferred until a dependency requires it."

---

#### MINOR-2 — Wrapper folder status is "proposed" in §0 but treated as canonical in §4 + README

**Files:** `CORE_RULES.md` §0 vs §4 + `README.md`

**What's wrong:**
- §0 table: "Wrapper folder | `ANDROIDDESIGN/` (single folder at repo root) | **proposed — awaiting confirmation**"
- §4 text: "The wrapper folder's name is the project name (`ANDROIDDESIGN/`)" — stated as fact, no "proposed" qualifier.
- `README.md` line 7: "All project content lives inside the `ANDROIDDESIGN/` wrapper folder." — stated as fact.

Per Q-003, the user confirmed the wrapper-folder rename but the new name "ANDROIDDESIGN" is still proposed. The documents disagree on whether the name is locked.

**Suggested fix:** Either:
- (a) Change §0 status to "confirmed (user approved wrapper rename; ANDROIDDESIGN matches repo name `testplay-byte/ANDROIDDESIGN` — pending only the formal Q-003 confirmation)"; OR
- (b) Add a footnote to §4 + README: "Wrapper folder name `ANDROIDDESIGN/` is tentative pending Q-003 confirmation."

Recommended: (a) — the user already approved the rename, and `ANDROIDDESIGN` matches the repo name (which IS confirmed).

---

#### MINOR-3 — `workflow.md` referenced but doesn't exist

**Files:** `CORE_RULES.md` §1, §4

**What's wrong:**
- §1: "The concrete step-by-step task loop lives in `memory/workflow.md` (to be written). This section is the **mindset**; `workflow.md` is the **procedure**."
- §4 folder layout: "rules/ (CORE_RULES.md (this file) + workflow.md)"
- §19 documentation table does NOT list `workflow.md`.
- The file does not exist in the repo.

A fresh agent reading §1 will look for `workflow.md` and not find it.

**Suggested fix:** Three options:
- (a) Write a stub `AGENT-CONTEXT/rules/workflow.md` in Phase 0: "TODO: fill in during Phase 1. Until then, the cognitive sequence in CORE_RULES §1 is the procedure."
- (b) Remove the `workflow.md` references from §1 + §4 until it exists.
- (c) Reword §1: "The concrete step-by-step task loop will live in `rules/workflow.md` (to be written in Phase 1). Until then, treat the cognitive sequence below as both mindset and procedure."

Recommended: (c) — keeps the future intent visible without leaving a dangling reference.

---

#### MINOR-4 — D-017, D-018, D-019 not in architecture.md §2 tech-stack table

**File:** `knowledge/architecture.md` §2

**What's wrong:** §2's tech-stack table covers D-001..D-016 (16 rows). D-017 (Auth: AniList OAuth), D-018 (Episode metadata merge), D-019 (Notifications: local polling) are not in the table. They ARE conceptually represented in §4 (data flow with auth implicit), §7 (multi-source episode flow), and D-019 in §10's open-architectural-questions — but the decision IDs aren't linked.

**Suggested fix:** Add three rows to §2's table:

| Auth | AniList OAuth2 Implicit Grant + custom-scheme redirect | D-017 |
| Episode metadata | Kitsu+Jikan+AniList merge (per-field priority) | D-018 |
| Notifications | Local polling (no push — AniList has no subscriptions) | D-019 |

---

#### MINOR-5 — CORE_RULES §16 naming rule contradicts itself

**File:** `CORE_RULES.md` §16

**What's wrong:** §16 says:

> Folders: kebab-case (`ani-design`, `core-rules-reference`). Uppercase for top-level project zones (`APP/`, `AGENT-CONTEXT/`, `REFERENCES/`, `research/` — lowercase `research/` is fine since it's data, not a code zone).

This is internally inconsistent: "research/" IS a top-level project zone per §4's folder layout, but it's exempted from the uppercase rule. The justification ("data, not a code zone") is fuzzy — AGENT-CONTEXT also contains data (markdown), not code.

**Suggested fix:** Reword §16:

> Folders: kebab-case for sub-folders (`ani-design`, `core-rules-reference`). Uppercase for top-level project zones that contain code or canonical project material (`APP/`, `AGENT-CONTEXT/`, `REFERENCES/`). Lowercase for top-level data-only zones (`research/`).

Or: rename `research/` → `Research/` for consistency and drop the exemption. (Lowercase is the convention for `docs/`-style folders, so the reword is preferable.)

---

#### MINOR-6 — Apollo Kotlin schema-refresh workflow not mentioned

**Files:** `knowledge/architecture.md` §9, `CORE_RULES.md` §8

**What's wrong:** R-4 open-questions §7 says: "Apollo Kotlin on Android requires the GraphQL schema downloaded from AniList. Need a CI step to refresh the schema periodically (separate `update-schema` workflow) — else operations drift from the live API."

Architecture.md §9 lists CI steps but doesn't mention schema refresh. Without this, a year from now AniList could ship a schema change and our generated operations silently break.

**Suggested fix:** Add to architecture.md §9:

> - **Weekly `update-schema.yml` workflow**: downloads the latest AniList GraphQL schema via Apollo's `downloadApolloSchema` Gradle task, regenerates Apollo Kotlin operations, commits to a `chore/sync-anilist-schema` branch, opens a PR if the schema diff is non-empty. Runs every Sunday.

Add a corresponding rule to CORE_RULES §8.

---

#### MINOR-7 — Compose `@Immutable` rule not in CORE_RULES

**File:** `CORE_RULES.md` §34

**What's wrong:** R-4 open-questions §8 says: "Compose stability of token classes. Critical to mark `@Immutable`. If we forget, recompositions explode on theme swap. Add a Detekt rule to enforce `@Immutable` on data classes in `:core:designsystem:tokens`."

CORE_RULES §34 describes the Detekt rule for `material3.*` imports but doesn't mention the `@Immutable` rule.

**Suggested fix:** Add to CORE_RULES §34:

> 7. **Token stability:** All data classes in `:core:designsystem:tokens` MUST be annotated `@Immutable` (Compose stability — prevents recomposition storms on theme swap). Enforced by a Detekt rule that fails the build if a `data class` in `:core:designsystem:tokens` lacks `@Immutable`.

---

#### MINOR-8 — progress.md doc-drift: Cline clone status

**File:** `memory/progress.md` line 23

**What's wrong:**
- `progress.md` line 23 (under "Done this session"): "Initiated shallow clone of Cline into `references/cline/` (background)."
- `session-log.md` lines 66-69: "Attempted a shallow clone into `references/cline/` ... Cleaned up. Deferred to the porting phase (Phase 4). The R-1 research report (64KB, 1310 lines) is the distilled analysis and is sufficient for planning."

`progress.md` says the clone is "in background" (implying ongoing) when it's actually been cleaned up + deferred. CORE_RULES §23 (Documentation Verification) explicitly warns about this kind of drift.

**Suggested fix:** Update progress.md line 23 to reflect the actual outcome:

> - 🔄 Shallow Cline clone attempted into `references/cline/`, stalled at 116KB (sandbox network limit), cleaned up. **Deferred to Phase 4** — the R-1 research report (1310 lines) is sufficient for planning. (See session-log.md §"Cline full-source clone".)

Move the line from "Done" → a new "Deferred" subsection, or just mark it `🔄 deferred`.

---

#### MINOR-9 — Accessibility not addressed in CORE_RULES

**Files:** `CORE_RULES.md` (no section)

**What's wrong:** CORE_RULES §20 (UI/UX Quality) doesn't mention accessibility. R-4 open-questions §5 explicitly flags: "Custom Canvas radar chart doesn't get Compose's a11y for free." WCAG AA contrast check is in §33 (palette extraction) — but only for generated themes, not for the starter theme or user-edited themes.

No rule about: screen reader (TalkBack) support, content descriptions, font scaling, touch target sizes, focus order.

**Suggested fix:** Add a new CORE_RULES §36.5 (or extend §20):

> ### Accessibility
> 1. All custom composables in `:core:designsystem` MUST expose semantics: `Modifier.semantics { contentDescription = "..." }` for any non-text UI element.
> 2. Custom Canvas charts (radar, spider, bar) MUST expose values via `Modifier.semantics` — at minimum, the chart's data summary as `contentDescription`.
> 3. Touch targets ≥ 48dp (Material accessibility guideline — applies even though we're non-Material).
> 4. Honor `LocalDensity.current.fontScale` — do NOT cap font size; let the user's system setting win.
> 5. WCAG AA contrast (4.5:1 for body text) applies to ALL themes, not just generated ones. The starter theme + user-saved themes are validated at commit time.
> 6. Test with TalkBack on a physical device before declaring any screen done.

---

#### MINOR-10 — Internationalization (i18n) not addressed

**File:** `memory/open-questions.md`

**What's wrong:** No question, no rule about whether the app is English-only or i18n-ready. AniList supports ROMAJI/ENGLISH/NATIVE title languages; Jikan returns Japanese titles; Kitsu returns canonical/en/jp. A fresh agent might hard-code English strings in composables, making i18n retrofit painful.

**Suggested fix:** Add a non-blocking Q-037:

> ### Q-037 — Internationalization (non-blocking)
> English-only for v1, or i18n-ready from day 1? Recommendation: i18n-ready from day 1 (use `strings.xml` for all user-facing strings — even if only English is shipped). AniList media titles already support user-preferred language via `UserTitleLanguage`. The agent's design-token system is language-agnostic. Cost of retrofitting i18n later is high; cost of doing it from day 1 is ~5% extra effort.

---

#### MINOR-11 — Test strategy not defined

**Files:** `CORE_RULES.md` (no section), `memory/decisions.md` D-005

**What's wrong:** D-005 trade-off mentions "a `KoinTest` resolvability check in CI" — that's the only test mentioned. No mention of: unit tests for repositories, instrumented tests for Room, Compose UI tests, screenshot tests for design-token swaps.

**Suggested fix:** Add a CORE_RULES §37 (Testing):

> ### Testing
> 1. **Unit tests** for repositories (with fake data sources), reconciliation logic (field-level COALESCE, LWW, append-never-overwrite), and agent tools (with fake `ThemeRepository`). Target ≥ 70% line coverage on `:core:data` and `:core:agent:tools`.
> 2. **Compose UI tests** for state changes (loading → loaded → error) and design-token swaps (preview → commit → rollback).
> 3. **KoinTest resolvability test** in CI — starts all Koin modules and asserts every `single<X>` resolves. Recovers ~90% of the compile-time safety Hilt would give (per D-005).
> 4. **Screenshot tests** for design-token swaps (post-MVP) — capture baseline screenshots of each screen with each preset theme; fail if a token swap changes layout.
> 5. **No instrumented tests on local sandbox** — they require an emulator, which we don't have (CORE_RULES §8). Run them in CI on a separate `instrumented-tests.yml` workflow with `reactivecircus/android-emulator-runner`.

---

#### MINOR-12 — No explicit `android:allowBackup` rule

**File:** `CORE_RULES.md` §32

**What's wrong:** R-1 §14.5 says: "Key exfiltrated via backup → `android:allowBackup="false"` on the application tag, or exclude the credentials dir."

CORE_RULES §32 implements a custom backup/restore feature (zip to `filesDir/backups/`) but doesn't say whether `android:allowBackup` (Auto-Backup for Apps) is enabled or disabled in AndroidManifest.

If `android:allowBackup="true"` (the default), Android's Auto-Backup will copy `filesDir` (including the Keystore-encrypted AniList token + Room DB + `theme.json`) to Google's servers on a daily basis — bypassing our custom backup encryption. This is a real privacy leak.

**Suggested fix:** Add to CORE_RULES §32:

> 9. **Disable Android Auto-Backup.** Set `android:allowBackup="false"` and `android:fullBackupContent="false"` in `<application>` in `AndroidManifest.xml`. We implement our own backup (this section). Android's Auto-Backup would bypass our encryption by uploading `filesDir` to Google's servers — including the Keystore-wrapped AniList token. (R-1 §14.5.)

---

#### MINOR-13 — Detekt custom rule scope is fuzzy

**File:** `CORE_RULES.md` §34 rule 2

**What's wrong:** §34 rule 2 says: "Forbidden imports in feature UI code (enforced by Detekt CI rule): `androidx.compose.material3.Button`, `Card`, `TopAppBar`, `Scaffold`, `NavigationBar` / `NavigationRail`, `androidx.compose.material3.*` (any high-level component)."

§34 rule 3 says: "Allowed primitives (reused under the hood, with justification): `androidx.compose.foundation.Surface`, `androidx.compose.material3.Ripple`, `androidx.compose.material3.ModalBottomSheet`."

But `androidx.compose.material3.*` is a wildcard — how does the rule allow `Ripple` and `ModalBottomSheet` (which ARE in `material3.*`) but forbid `Button` and `Scaffold` (also in `material3.*`)? A fresh agent implementing the Detekt rule would have to guess: is it a blacklist of specific class names, or a whitelist?

**Suggested fix:** Specify the Detekt rule implementation:

> 2. **Forbidden imports in feature UI code** — Detekt custom rule `NoMaterial3Components`:
>    - **Blacklist** (forbidden fully-qualified class names): `androidx.compose.material3.Button`, `OutlinedButton`, `TextButton`, `FloatingActionButton`, `ElevatedButton`, `Card`, `ElevatedCard`, `OutlinedCard`, `TopAppBar`, `CenterAlignedTopAppBar`, `Scaffold`, `NavigationBar`, `NavigationRail`, `NavigationDrawerItem`, `Slider`, `Switch`, `Checkbox`, `RadioButton`, `TextField`, `OutlinedTextField`, `AlertDialog`, `Snackbar`, `LinearProgressIndicator`, `CircularProgressIndicator`, `Divider`, `HorizontalDivider`, `VerticalDivider`, `Badge`, `BadgeBox`, `AssistChip`, `FilterChip`, `InputChip`, `SuggestionChip`, `DropdownMenu`, `ExposedDropdownMenuBox`, `SegmentedButton`.
>    - **Whitelist** (explicitly allowed primitives, must be used under the hood of `:core:designsystem` components, with a `// justified: <reason>` comment): `androidx.compose.foundation.Surface`, `androidx.compose.material3.Ripple`, `androidx.compose.material3.ModalBottomSheet`, `androidx.compose.material3.SheetState`, `androidx.compose.material3.rememberModalBottomSheetState`.
>    - The Detekt rule checks imports in `:feature:*` modules. The `:core:designsystem` module is exempt (it implements the wrappers).

---

#### MINOR-14 — `set_color_role` / `set_typography` / `set_shape` / `set_motion` + `apply_text_patch` not in architecture.md tool list

**File:** `knowledge/architecture.md` §6

**What's wrong:** CORE_RULES §28 rule 4 lists `set_color_role(role, value)` / `set_typography(...)` / `set_shape(...)` / `set_motion(...)` as convenience wrappers + `apply_text_patch` as a SEARCH/REPLACE tool. Architecture.md §6 ToolExecutor list omits all five. Only `set_sorting_rule` is shown.

Architecture.md is illustrative, not exhaustive — but the omission is inconsistent.

**Suggested fix:** Either:
- (a) Expand architecture.md §6's ToolExecutor list to include all 13+ tools from CORE_RULES §28 rule 4; OR
- (b) Add a note: "Full tool list: see CORE_RULES §28 rule 4. Architecture-impacting tools shown above; the rest are stateless wrappers."

Recommended: (b) — keeps the diagram readable.

---

#### MINOR-15 — Architecture §10's "architectural questions" list mixes identity questions in

**File:** `knowledge/architecture.md` §10

**What's wrong:**
> See `memory/open-questions.md` for the full list. The architectural ones:
> - Q-005: Should we use Apollo Kotlin's normalized cache in addition to Room, or just Room?
> - Q-006: Should the AI agent's design snapshots be included in backups by default?
> - Q-007: Should we add a project web dashboard (like the old ANI-KUTA project had)?
> - Q-008: Confirm app name + package name.

- Q-005 ✅ architectural (Apollo vs Room)
- Q-006 ✅ architectural (backup scope)
- Q-007 ❌ not architectural — it's a feature-scope question (do we want a dashboard at all)
- Q-008 ❌ not architectural — it's identity (app name + package)

The list mixes concerns.

**Suggested fix:** Replace Q-007 + Q-008 with the actually-architectural questions:
- Q-009 (Agent approval mode — affects agent tool surface) ✅ architectural
- Q-014 (Dynamic theming in v1 — affects feature scope) ✅ architectural

And add a note: "Q-008 (identity) is in `open-questions.md` blocking section — see Q-001/Q-002 instead."

---

#### MINOR-16 — Q-005 doesn't reference D-004's existing decision

**File:** `memory/open-questions.md` Q-005

**What's wrong:**
- D-004 says: "Networking: Apollo Kotlin (AniList GraphQL) + Ktor 3. Rationale: Apollo Kotlin gives codegen + **normalized cache** for AniList GraphQL."
- Q-005 asks: "Should we use Apollo Kotlin's normalized cache in addition to Room, or just Room?"

D-004 already implies "yes, use Apollo's normalized cache" — but Q-005 is asking the same question without referencing the existing decision. A reader might be confused about whether Q-005 overrides D-004.

**Suggested fix:** Reword Q-005:

> ### Q-005 — Apollo Kotlin normalized cache: keep or drop?
> D-004 PROPOSED using Apollo Kotlin's normalized cache (in-memory + persistent SQLite) for AniList GraphQL alongside Room. Two SQLite stores means a sync surface. Options:
> - (a) Keep both (D-004's proposal) — Apollo's cache is purpose-built for GraphQL normalization; Room holds domain models. Accept the sync surface.
> - (b) Drop Apollo's cache, use Room only — simpler, but lose Apollo's automatic query-result normalization (we'd hand-roll it).
> Default: (a) — keep both, accept the sync surface, document the reconciliation rule ("Apollo cache is invalidation-only; Room is the source of truth for domain models").

---

### NIT

#### NIT-1 — `.gitignore` missing Android signing files + Firebase config

**File:** `.gitignore`

**What's wrong:** The `.gitignore` covers credentials, build artifacts, IDE files, and the sandbox workspace. But it doesn't anticipate:
- `*.keystore` / `*.jks` (Android signing keystores — should never be committed)
- `key.properties` (Android signing config — contains keystore passwords)
- `google-services.json` (Firebase config — if MAJOR-6 Q-036 option (b) is chosen)
- `sentry.properties` (Sentry DSN — if MAJOR-6 Q-036 option (c) is chosen)
- `play-service-account.json` (Play Store upload key — for releases)
- `*.iml` (Android Studio module files — sometimes outside `.idea/`)

None of these exist in the repo today, so this is not an active leak. But adding them now prevents future mistakes.

**Suggested fix:** Append to `.gitignore`:

```
# --- Android signing (never commit) ---
*.keystore
*.jks
key.properties
release.keystore
debug.keystore

# --- Third-party service configs (if added) ---
google-services.json
sentry.properties
play-service-account.json

# --- Kotlin / Gradle extras ---
*.iml
.kotlin/
```

---

#### NIT-2 — open-questions.md summary is accurate but the count "(12 blocking + 13 non-blocking + 6 deferred = 31)" could be made explicit

**File:** `memory/open-questions.md` §Summary (line 169-173)

**What's wrong:** The summary says "12 blocking", "13 non-blocking", "6 deferred" — accurate. But it doesn't show the math (31 total) and doesn't mention that answering the 12 blocking unblocks Phase 1.

**Suggested fix:** Append: "Total: 31 questions. Answering the 12 blocking ones unblocks Phase 1 (Gradle scaffolding)."

---

#### NIT-3 — Architecture.md §6 has unbalanced parens in the loop description

**File:** `knowledge/architecture.md` §6 line 145

**What's wrong:**
> `while(isActive) { buildContext → llm.stream → parse tool calls → for each: (if destructive: await approval) → execute → observe → repeat until attempt_completion }`

The `(if destructive: await approval)` opens a paren that doesn't close before the arrow.

**Suggested fix:**
> `while(isActive) { buildContext → llm.stream → parse tool calls → for each: (if destructive: await approval) → execute → observe → repeat → if attempt_completion: break }`

---

#### NIT-4 — R-1 has a numbering typo (line 1195–1196)

**File:** `research/R-1-cline-agent.md` "Reimplement" section

**What's wrong:** The numbered list goes 1, 2, 3, 4, 5, 6, 8, 9, 10, 11 — missing "7."

```
6. ApprovalGateway — suspend fun approve(toolName, input): ApprovalResult. Runs on
   UI thread via Compose; no polling.
8. PromptRegistry — one variant per provider family (Claude/GPT/Gemini/Generic).
```

This is in a research report, not a planning artifact — but the planning agent treats R-1 as authoritative (e.g., `PromptRegistry` is referenced implicitly via CORE_RULES §28). A future agent reading R-1 might miss the missing item.

**Suggested fix:** Renumber to fill the gap, or insert "7. FileReadCache — mtime map for file-read dedup." (which is listed in the module shape but not in the Reimplement list).

---

#### NIT-5 — `set_color_role` etc. wrappers are described as "convenience wrappers over `apply_token_patch`" but their input schema is unspecified

**File:** `CORE_RULES.md` §28 rule 4

**What's wrong:** §28 rule 4 says: "`set_color_role(role, value)` / `set_typography(...)` / `set_shape(...)` / `set_motion(...)` — convenience wrappers over `apply_token_patch`."

But the input schemas for these wrappers aren't specified. R-4 §D22 lists more detailed schemas (`set_color_role: role: String, value: HexColor`; `set_typography: scale: String, family: String?, size: Int?, weight: Int?, lineHeight: Int?, tracking: Float?`). The rules file is less specific than the research report.

**Suggested fix:** Either link to R-4 §D22 in §28 rule 4, or copy the schemas inline. The agent implementing Phase 4 needs the schemas visible from the rules.

---

## Missing Questions

Open-questions the main agent should have asked but didn't (in priority order):

1. **Q-033 (new, blocking) — LLM cost guardrails.** Per MAJOR-3. Every agent run costs the user money via their own API key. No spending cap, no per-session token budget, no warning threshold. Options: iteration cap, token budget, or no cap. (Source: R-4 open-questions §3.)

2. **Q-034 (new, blocking) — AniList OAuth: custom-scheme (faster, interceptable) or Android App Links (slower, hardened)?** Per MAJOR-3. Implicit Grant + custom scheme is vulnerable to interception by another app registering the same scheme. R-2 §14.1 explicitly recommends App Links for v1+ hardening.

3. **Q-035 (new, blocking) — Backup export to MediaStore.Downloads: allow without passphrase, or require passphrase for any cross-device export?** Per MAJOR-3. Current §32 design allows unencrypted-to-other-apps backup in shared storage.

4. **Q-036 (new, blocking) — Analytics / crash reporting.** Per MAJOR-6. None / Firebase Crashlytics / Sentry / custom. Affects privacy policy, third-party deps, GDPR, CI.

5. **Q-037 (new, non-blocking) — Internationalization.** Per MINOR-10. English-only or i18n-ready from day 1? `strings.xml` from day 1 is recommended.

6. **Q-038 (new, non-blocking) — Predictive back gesture.** Android 13+ has predictive back; should the app opt in (via `android:enableOnBackInvokedCallback="true"`)? Affects nav stack implementation. Default: yes (modern Android UX).

7. **Q-039 (new, non-blocking) — Tablet / foldable / landscape support.** Architecture mentions `NavigationSuiteScaffold` (rail on tablet, drawer on TV/desktop) but doesn't ask whether v1 targets phone-only or phone+tablet. Affects layout work + Design Studio preview.

8. **Q-040 (new, non-blocking) — Material You dynamic color (Android 12+).** User said no Material Design — but does that include the OS-level dynamic color (which adapts the app's accent to the user's wallpaper)? Most non-Material apps still honor it. Default: opt-out for v1 (the user wants a fixed starter design).

9. **Q-041 (new, non-blocking) — App Links / assetlinks.json hosting.** If Q-034 = App Links, where does `assetlinks.json` live? (Needs `https://anidesign.testplaybyte.com/.well-known/assetlinks.json` or similar — requires a domain the user controls.)

10. **Q-042 (new, deferred) — Third-party dependency licensing strategy.** Bundled fonts (OFL), Cline inspiration (Apache 2.0) are mentioned. But what about Vico, Coil, Apollo, Ktor, Koin, OkHttp, kotlinx.serialization — all Apache 2.0 or MIT. Where does the consolidated OSS Licenses screen live? Settings → "Open Source Licenses" (mentioned in §35) — confirmed, but no rule on what to include + how to generate it (Google's `oss-licenses-plugin` or hand-rolled).

---

## Risk Gaps

Risks not flagged in the Highlights & Concerns section (already covered in MAJOR-3 — repeated here as a standalone list for the main agent's convenience):

1. **LLM cost** (every agent run = real money; no spending cap; user's own API key).
2. **Battery drain** from long agent loops (R-1 §14.1 recommends iteration cap of 25 — currently absent from CORE_RULES).
3. **AniList ToS compliance** — polling cadence could be flagged as automated abuse if scaled.
4. **Image CDN hotlinking** — AniList, Kitsu, MAL CDN; may violate ToS; mitigate with aggressive Coil caching.
5. **GDPR / data-privacy for backups** — exported backups in MediaStore.Downloads are shared-storage readable.
6. **OAuth custom-scheme interception** — another app registering `com.testplaybyte.anidesign://` could capture the JWT.
7. **Backup passphrase loss** — user forgets → data loss (no recovery mechanism).
8. **Memory pressure** — Android apps have smaller heaps than desktop; aggressive context truncation recommended (R-1 §14.4).
9. **Process isolation** — a buggy agent tool can crash the whole app (R-1 §14.7 recommends `SupervisorJob` + separate `:agent` process for v2 if stability issues).
10. **WorkManager reliability on Chinese OEMs** (Xiaomi, Huawei) — aggressive battery killers will skip weekly backup (R-4 open-questions §2).
11. **Apollo Kotlin normalized cache ↔ Room duplication** — two SQLite stores, sync surface (R-4 open-questions §1).
12. **AniList `idMal` coverage** — what % of AniList media have non-null `idMal`? If low, Jikan fallback is unreliable. (R-3 §10.3 — flagged as a follow-up task but not as a risk.)

---

## ANI-KUTA Leakage Check

I grep'd CORE_RULES.md, decisions.md, architecture.md, open-questions.md, lessons-learned.md, progress.md, README.md, and REFERENCES/README.md for: `MPV`, `SQLDelight`, `app.confused.anikuta`, `ANI-KUTA`, `ARM-only`, `DASHBOARD`, `anikuta`, `Injekt`.

Findings:

| Term | Where it appears | Status |
|------|------------------|--------|
| `MPV` | CORE_RULES §5 ("No MPV / media-player carve-over in this project"), §7 (no carve-out), Appendix A (dropped) | ✅ Explicitly called out as dropped. NOT a leak. |
| `SQLDelight` | CORE_RULES §0 header, §30 (no), D-003 ("the prior ANI-KUTA project used SQLDelight; we diverge"), Appendix A | ✅ Explicitly diverged. NOT a leak. |
| `app.confused.anikuta` | CORE_RULES §0 header only ("Anything ANI-KUTA-specific (MPV player, SQLDelight, `app.confused.anikuta`, ARM-only ABIs, DASHBOARD zone) is NOT carried over") | ✅ Mentioned only to say it's NOT carried over. NOT a leak. |
| `ANI-KUTA` | CORE_RULES Appendix A, lessons-learned ("the prior ANI-KUTA project used SQLDelight"), open-questions Q-026 ("the old ANI-KUTA project had a Next.js web dashboard") | ✅ All historical references, explicitly framed as "the old project". NOT a leak. |
| `ARM-only` | CORE_RULES §0 header, Appendix A | ✅ Explicitly diverged (we add x86_64). NOT a leak. |
| `DASHBOARD` | CORE_RULES §4 ("No DASHBOARD zone (yet)"), Appendix A (dropped), Q-026 ("Web project dashboard") | ✅ Explicitly dropped, with a "re-add if user requests" note. NOT a leak. |
| `anikuta` (lowercase, as a tag/package fragment) | CORE_RULES §20 (filtered logging — "Tags renamed `Anikuta:` → `AniDesign:`" in Appendix A only) — actual rules use `AniDesign:` | ✅ Renamed consistently. NOT a leak. |
| `Injekt` (the DI lib the old project used before Koin) | Not found anywhere in the refined planning artifacts | ✅ Fully dropped. NOT a leak. |
| `Nav3` | CORE_RULES Appendix A ("removed Nav3 reference"), D-006 ("NOT Nav3"), lessons-learned ("Nav3 is still alpha; the prior project tried it and removed it") | ✅ Explicitly excluded. NOT a leak. |
| `lime accent, warm darks` | open-questions Q-004 ("similar to the old project's 'lime accent, warm darks' aesthetic") | ✅ Explicitly framed as "(similar to the old project's...)" — intentional historical reference for the user's benefit, not a leaked assumption. NOT a leak. |

**Verdict on leakage: CLEAN.** No ANI-KUTA-specific assumptions leaked through uncritically. Every old-project term is either explicitly dropped, explicitly diverged, or explicitly framed as "what the old project did, for historical context."

The reference ruleset (`REFERENCES/core-rules-reference.md`) DOES contain all the ANI-KUTA-specific terms (MPV, SQLDelight, `app.confused.anikuta`, ARM-only, DASHBOARD, Injekt, Nav3, `.sqm` files, etc.) — but this is intentional. `REFERENCES/README.md` explicitly frames it as "the CORE_RULES.md from the prior ANI-KUTA project. Kept as a structural inspiration... It is NOT authoritative for this project." Acceptable.

---

## Final Recommendation

The planning package is **stronger than typical Phase 0 output** — the research is primary-sourced, the decision IDs are traceable, the ANI-KUTA leakage is handled explicitly, and the open-questions are well-tiered. The plan is **shippable to the user as-is for review**, but I recommend the main agent do **three things before presenting it**:

1. **Address MAJOR-1 + MAJOR-2** (the "agent is OPTIONAL" architectural gap + the sorting-rules / layout-selections storage gap). These are the only issues that would cause a Phase 1/2 implementing agent to make wrong assumptions. A 1-paragraph addition to CORE_RULES §29 (user-facing theme editing) + a 1-paragraph addition to §29/§31 (sorting-rules + screen-layouts storage) closes both gaps.

2. **Add the 4 new blocking questions** (Q-033 LLM cost guardrails, Q-034 OAuth App Links vs custom scheme, Q-035 Backup export encryption requirement, Q-036 Analytics/crash reporting). These shape the user's answer to existing questions (e.g. Q-008 LLM key handling is incomplete without Q-033 cost guardrails; Q-006 redirect URI is incomplete without Q-034 App Links decision).

3. **Add the 7 missing risks to Highlights & Concerns** (LLM cost, battery drain, AniList ToS, image CDN hotlinking, GDPR for backups, OAuth interception, passphrase loss). The user is being asked to approve a design whose risks they should see upfront — not discover later.

The MINOR and NIT issues can be fixed in-session as the main agent incorporates the feedback — they don't need to block the user-facing presentation. Specifically: MINOR-3 (workflow.md reference), MINOR-8 (Cline clone status drift), and NIT-1 (.gitignore additions) are 5-minute fixes worth doing now to set a clean baseline.

After those changes, this is an APPROVE.

---

## Appendix: review verification log

| Claim checked | Source | Status |
|---|---|---|
| Decision IDs D-001..D-019 sequential + non-reused | `decisions.md` line-by-line | ✅ Verified |
| Architecture §2 references D-001..D-016 (missing D-017/18/19) | `architecture.md` §2 vs `decisions.md` | ✅ Verified (MINOR-4) |
| Placeholder values consistent across CORE_RULES §0, §8, README, decisions | `CORE_RULES.md` §0 + §8 + `README.md` + `decisions.md` | ✅ Verified (app name, package, ABIs, SDK all consistent) |
| Phase numbering (0..6) consistent between progress.md, README, architecture | `progress.md` Phase map + `README.md` line 6 + `architecture.md` (no phase map) | ✅ Verified (architecture.md doesn't list phases — only progress.md does — so no contradiction) |
| `set_sorting_rule` tool exists but no `sorting_rules` table documented | `CORE_RULES.md` §28 rule 4 + §31 rule 2 + §22 file structure | ✅ Verified gap (MAJOR-2) |
| `swap_layout` tool exists but `screen_layouts` storage undefined | `CORE_RULES.md` §28 rule 4 + §29 + §31 + §32 | ✅ Verified gap (MAJOR-2 / MINOR-13) |
| "Agent is OPTIONAL" guarantee has no user-facing manual edit path | `CORE_RULES.md` §28 rule 10 + §29 + §33 + `architecture.md` §5 | ✅ Verified gap (MAJOR-1) |
| `workflow.md` referenced but file doesn't exist | `CORE_RULES.md` §1 + §4 vs repo file listing | ✅ Verified (MINOR-3) |
| `.gitignore` covers credentials + build artifacts | `.gitignore` line-by-line | ✅ Verified (missing Android signing — NIT-1) |
| Repo folder structure matches CORE_RULES §4 | repo listing vs §4 layout | ✅ Verified |
| git log shows only intentional planning artifacts in last commit | `git show --stat 3106091` | ✅ Verified (23 files, all planning artifacts, no secrets) |
| Cline clone status drift between progress.md and session-log.md | `progress.md` line 23 vs `session-log.md` lines 66-69 | ✅ Verified (MINOR-8) |
| JDK version: R-4 says 21, planning artifacts say 17 | `R-4` line 1194 vs `.github/workflows/README.md` line 19 vs `architecture.md` §9 | ✅ Verified (MINOR-1) |
| ANI-KUTA leakage check (MPV, SQLDelight, app.confused.anikuta, ARM-only, DASHBOARD, Injekt, Nav3) | grep across all refined planning artifacts | ✅ Verified clean (see "ANI-KUTA Leakage Check" section) |
