# Open Questions — ANDROIDDESIGN

> Questions for the user. Numbered `Q-NNN`. When answered, move to `decisions.md` as a D-NNN
> (or update an existing one) and mark the question answered here with ✅ + the answer.
>
> **Read the "Highlights & Concerns" section first** — it flags risks/limitations you should
> know about before answering the questions.

---

## Highlights & Concerns (please read first)

These are things I want to flag proactively (CORE_RULES §2: proactively highlight concerns before the user discovers them). They're not questions — they're context that shapes the questions below.

1. **AniList's rate limit is currently degraded.** Documented as 90 req/min, but live verification shows `X-RateLimit-Limit: 30` right now. The client is designed for 30/min. If AniList restores 90, we get headroom for free; if it drops further, we already throttle correctly. (R-2 research.)

2. **AniList has NO push notifications and NO WebSocket/subscriptions.** The app must poll for airing notifications. We poll `Viewer.unreadNotificationCount` cheaply + fetch full notifications only when the count is > 0. This is daily-WorkManager + on-app-open, not real-time. Acceptable for a weekly-episode tracker; not acceptable for a chat app (which we are not).

3. **Jikan's per-episode synopsis endpoint is fragile.** `/v4/anime/{id}/episodes/{episode}` 504'd ~100% during research. We use Kitsu for synopses (reliable) and treat Jikan's single-episode endpoint as a lazy fallback only — never blocking the UI. (R-3 research.)

4. **The AI agent is a multi-phase engineering effort.** Porting Cline's architecture to Kotlin (loop, context manager, tool system, 4 LLM providers, approval gateway, snapshot store) is ~2-3 phases of work. The app is **fully usable without the agent** — the agent is a power-user feature for design customization. I'll phase the build so the app works end-to-end (tracker + offline + design tokens editable by hand) BEFORE the agent is wired in.

5. **AniList OAuth is Implicit Grant (no PKCE, no refresh token).** This is the best AniList offers for mobile. Implicit Grant is deprecated in OAuth 2.1, but AniList doesn't support Authorization Code + PKCE. Tokens are 1-year JWTs; when they expire, the user re-auths. This is fine — it's how every AniList mobile app works. (R-2 research.)

6. **The sandbox environment can clear randomly.** Everything important is pushed to GitHub. Credentials (PAT, AniList token, LLM key) live locally only — never in the repo. If the sandbox clears, you'll need to re-provide the GitHub PAT at the next session start (the AniList token + LLM key live on your device, not here).

7. **I cannot build the Android APK in this sandbox.** It has no Android SDK/JDK and must not acquire them (CORE_RULES §8). All builds happen via GitHub Actions. Compile errors are found by: careful reading → sub-agent review → push to CI → read annotations → iterate. This is slower than local builds but it's the constraint we have.

8. **The reference CORE_RULES was from a streaming app (ANI-KUTA with MPV player).** I dropped all streaming/player-specific rules since this project is a tracker, not a streaming app. If you DO want streaming later, that's a separate large feature and we'd add rules back.

9. **I'm NOT using Material Design**, per your explicit instruction. The starter design language is intentionally simple + customizable. The AI agent + you will evolve it. A CI Detekt rule forbids `material3.*` imports in feature code so it stays non-Material.

10. **I built x86_64 into the ABI set** (per your approval this session) so the Android emulator works now that you've enabled Hyper-V. Physical device remains the primary test target; emulator is secondary.

---

## Blocking questions (need answers before Phase 1 starts)

These block scaffolding the Gradle project. Please answer these first.

### Q-001 — App display name
I proposed **"AniDesign"** as a working name. Confirm, or give me the name you want.

### Q-002 — App package name (applicationId)
I proposed **`com.testplaybyte.anidesign`** (based on your GitHub account `testplay-byte`). Confirm, or specify a different reverse-DNS package. This is hard to change later (it's the app's identity).

### Q-003 — Wrapper folder name (at repo root)
You confirmed the wrapper-folder rename is good. I propose **`ANDROIDDESIGN`** (matches the repo name). Was a different name already decided in a prior (cleared) session? If not, is `ANDROIDDESIGN` acceptable?

### Q-004 — Starter design aesthetic
You said: *"a simple starting foundation of the design... users can customize it later."* What direction for the starter?
- (a) Dark-first minimal (warm darks, single accent — similar to the old project's "lime accent, warm darks")
- (b) Light-first minimal (paper-like, restrained)
- (c) High-contrast mono (black/white/one accent)
- (d) You pick a reasonable default and I'll iterate via the agent.

I propose **(a) dark-first minimal with a single configurable accent**, since it matches the "warm darks" aesthetic you liked before + shows off the design system without competing with anime cover art.

### Q-005 — AniList OAuth client registration
Have you registered (or are you willing to register) an AniList API client at https://anilist.co/settings/developer? I need a **Client ID** to wire the OAuth flow. (No client secret needed — we use Implicit Grant.) If you haven't, please register one and give me the Client ID. The redirect URI will be `<applicationId>://anilist-auth` (see Q-006).

### Q-006 — Redirect URI scheme
I propose **`<applicationId>://anilist-auth`** (e.g. `com.testplaybyte.anidesign://anilist-auth`). AniList allows custom URI schemes. Confirm, or specify a different scheme (e.g. `anidesign://callback`).

### Q-007 — LLM providers for the AI agent (v1)
I propose supporting **Anthropic + OpenAI + OpenRouter + Gemini + an OpenAI-compatible generic endpoint** (so you can point at Ollama/LM Studio/local LLMs). Should I include all 5 in v1, or prioritize a subset? (Adding more later is easy — each is ~1 provider adapter file.)

### Q-008 — LLM API key handling
How should the user provide their LLM API key?
- (a) Paste into Settings → stored in Android Keystore (encrypted, never leaves device, never in backups). **Default.**
- (b) Some other mechanism (specify).

### Q-009 — Agent approval mode (default)
For destructive agent tools (`apply_token_patch`, `swap_layout`, `set_component_variant`):
- (a) Require approval for every call (safest, slower).
- (b) Auto-approve with live preview + one-tap undo via snapshots (faster, still safe — every commit is snapshotted to Room, capped at 50). **Default.**
- (c) Let the user pick per-session in the Design Studio.

### Q-010 — Starter fonts
Confirm the bundled set: **Inter (body) + Sora (display) + JetBrains Mono (numbers/mono)**, all OFL. Are there others you want bundled?
- A serif font for variety? (e.g. Source Serif Pro)
- A CJK font for Japanese titles? (e.g. Noto Sans JP — but this is large, ~3-4MB; maybe defer to dynamic loading)

### Q-011 — Screen set for v1
Confirm the v1 screen set:
1. Home (user info, new content, quick stats)
2. Profile (stats with charts: radar/spider, bar, etc.)
3. Library (anime list + manga list, sortable by status: watching/completed/paused/dropped/planning)
4. Search (anime/manga toggle at top-right, filters, sort)
5. Airing (schedule — calendar view default, customizable)
6. Details (multi-source episode metadata)
7. Settings (auth, backup, agent config, design history)
8. Design Studio (the AI agent chat UI + design customization surface)

Am I missing any screens you want? (e.g. Characters, Studios, Notifications list, Activity feed, Friends/social, Manga reader?)

### Q-012 — Manga scope
Same screens as anime (tracker only, no reader), OR do you want a manga reader for tracking chapter progress? I assume **tracker-only for v1** (no reader). Confirm.

---

## Non-blocking questions (proposed defaults — I'll proceed with these unless you object)

I'll proceed with these defaults. Object if you want something different.

### Q-013 — Dark mode
Starter ships with **both** a dark theme (default) and a light theme. The user + agent can customize both. The app respects system dark-mode setting by default, with a manual override in Settings.

### Q-014 — Dynamic theming (palette from image) in v1?
I propose **including it in v1** as a marquee feature of the design system — it's a strong demo of "AI-powered design." If you'd rather defer, I can move it to a later phase. Default: **include in v1** (after the agent core is done).

### Q-015 — Airing screen default view
**Calendar view** (with theme-color highlights from covers) as the default, with a list view available via the agent or a toggle. Confirm or specify a different default.

### Q-016 — Notifications
**Local-only** (WorkManager daily poll + on-app-open check). No server-side push (would need a backend). AniList has no push, so this is the only option anyway.

### Q-017 — Character/Staff/Studio detail pages
**Defer to a later phase.** v1 shows Characters/Staff/Studios as lists on the Details screen (tappable to a simple detail page in a later phase). Confirm.

### Q-018 — Backup default contents
Backup includes by default: Room DB + DataStore + theme.json + saved themes + snapshots + AniList token (encrypted with user-set passphrase). The user can toggle snapshots off. The LLM API key is NEVER in backups (device-bound). Confirm.

### Q-019 — Auto-backup constraints
**Charging + battery-not-low + unmetered network + device idle**, weekly (7 days), one rolling copy. User can relax constraints in Settings. Confirm or specify different constraints.

### Q-020 — Design snapshot cap
**50 snapshots** (oldest evicted). At ~2KB per snapshot (theme.json is small), that's ~100KB max — negligible. Confirm, or specify a different cap (100/200).

### Q-021 — Agent skill system
**Defer.** v1 agent has the tool surface only (no skill files). We can add a skill system (standalone markdown files the agent reads on demand) in a later phase if the agent needs project-specific knowledge. Confirm.

### Q-022 — Branching
`main` (stable, CI-green) + `feature/*` branches for work. Merge to `main` only after CI passes + your review. Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`). Never force-push to `main`. Confirm.

### Q-023 — Commit cadence
Commit + push at the end of every significant work unit (not just session end). Frequent pushes protect against sandbox clears. Confirm.

### Q-024 — Primary test target
**Physical device primary, emulator secondary.** I'll write the GitHub Actions workflow to build x86_64 + arm64 + armv7 APKs. You install on your phone for primary testing; use the emulator for quick iteration. Confirm.

### Q-025 — ntfy.sh task notifications
**Optional / deferred.** The old project used `ntfy.sh/TASKISDONE` (public topic — anyone could read/spoof). I propose: defer until you confirm you want notifications, then use a long random topic stored as a GitHub secret (not the public one). Should I set this up, or skip notifications entirely?

---

## Deferred questions (for later phases — answer when we get there)

### Q-026 — Web project dashboard
The old ANI-KUTA project had a Next.js web dashboard showing project progress/decisions/architecture. Do you want one for this project? It's not required for the app. **Defer** — I'll ask again when the app is further along.

### Q-027 — Release APK signing
When we approach a release build, I'll need a keystore. Options: (a) you provide one, (b) I generate one + store it as a GitHub secret. **Defer** until we approach release. v1 is debug-only.

### Q-028 — Distribution channel
Google Play, direct APK sideload, F-Droid, or all? **Defer** until we approach release. v1 is debug sideload only.

### Q-029 — Cline vendoring as git submodule
I'm doing a shallow clone of Cline into `references/cline/` locally (sandbox only, NOT committed). Do you want Cline ALSO vendored as a git submodule in the GitHub repo (so it travels with the repo)? Or is local clone + GitHub URL sufficient? **Defer** — I'll keep the local clone for now; we can add a submodule later if needed.

### Q-030 — Other reference repos
The old project referenced "animiru" (an open-source Android anime app). Should I add Animiru or other references to `REFERENCES/`? **Defer** — I'll keep Cline as the primary reference for now; add others when relevant.

### Q-031 — Emulator AVD retest
Once the app skeleton is built + pushed to CI + APK artifact is available, should I attempt to verify the Android emulator works (re-run the AVD test that failed before Hyper-V was enabled)? **Defer** until we have a buildable APK. Confirm we should retest then.

---

## Summary

- **12 blocking questions** (Q-001..Q-012) — need answers before Phase 1.
- **13 non-blocking questions** (Q-013..Q-025) — I'll proceed with my proposed defaults unless you object.
- **6 deferred questions** (Q-026..Q-031) — for later phases.

Take your time answering. The blocking ones are mostly identity (app name, package, wrapper folder, starter aesthetic) + AniList OAuth client registration + LLM provider preference + screen/manga scope confirmation. Once those are answered, I can scaffold the Gradle project and begin Phase 1.
