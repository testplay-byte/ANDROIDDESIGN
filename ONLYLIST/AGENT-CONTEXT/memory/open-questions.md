# Open Questions — Only-List

> All blocking questions from session 1 have been ANSWERED (see `decisions.md` for the confirmed answers).
> This file now holds only: (a) deferred questions for later phases, (b) new questions arising this session.

---

## ✅ Answered (session 2)

All Q-001..Q-036 from session 1 are answered. Key confirmations:
- App: **Only-List**, package **`com.confused.onlylist`**, folder **`ONLYLIST`**.
- Theme: **Midnight Coral** (dark + coral) — see `APP/only-list/DESIGN-LANGUAGE.md`.
- AniList: custom-scheme OAuth, 60 req/min target, NO auto-polling. Client NOT registered yet (D-031 deferred).
- Agent: Cline + Kilo Code combination, auto-approve + undo + reset, all providers OpenAI-compatible by default.
- v1 screens: Home, Library, Search, Airing, Details, Settings. No Design Studio in v1.
- Manga: tracker only. Fonts: all weights bundled (fix bold). Backup: opt-in password. Crash: "Something went wrong" screen.

---

## 🟡 New questions (session 2)

### Q-101 — Profile screen in v1?
Your v1 screen list was: Home, Library, Search, Airing, Details, Settings. My original list also had **Profile** (stats with radar/bar charts). Did you intend to drop Profile from v1, or was it implicit? I'll include Profile in v1 (it's a key tracker feature + shows off the design system's charts). **Default: include Profile.** Object if you want it deferred.

### Q-102 — "Reset to defaults" scope
You said the reset button "reverts the UI to its original state — everything related to UI, the agent's context, everything deleted." Confirm the reset wipes:
- Active `theme.json` → starter Midnight Coral
- All saved themes
- All design snapshots
- All sorting rules
- Agent context/conversation history
- (Does NOT wipe: AniList token, local DB cache of anime/manga, user's list entries — those are DATA not DESIGN.)

**Default: reset wipes ONLY design + agent state, NOT user data.** Object if you want a full reset.

### Q-103 — Haptic feedback
The design language spec includes light haptic on tap (press feedback). Some users disable haptics system-wide. Confirm: respect the system haptic setting (off if user disabled system haptics). **Default: respect system.**

### Q-104 — "Coral theme for the Now"
You mentioned "utilize a coral team for the Now." I interpreted "the Now" as a speech-to-text artifact and went with coral as the single accent on the dark theme. Did you mean something specific by "the Now" (e.g., a "Now Playing" / "Now Airing" section with its own coral treatment)? **Default: coral is the global accent.** Clarify if you meant a specific section.

---

## 🔵 Deferred (from session 1, still deferred)

### Q-026 — Web project dashboard
The old ANI-KUTA project had a Next.js dashboard showing progress. Want one for Only-List? Defer until app is further along.

### Q-027 — Release APK signing
Need a keystore when approaching release. Defer — v1 is debug sideload only.

### Q-028 — Distribution channel
Play / sideload / F-Droid? Defer until release.

### Q-029 — Cline/Kilo vendoring as git submodule
Currently web-research only. Add as submodule later if needed for the porting phase.

### Q-030 — Other reference repos
Add Animiru or other references? Defer.

### Q-031 — Emulator AVD retest
Retest the emulator once we have a buildable APK (post Phase 1 CI green).

### Q-105 (new) — AniList API client registration timing
When should you register the AniList API client? (Needed before testing authenticated flows — Viewer, lists, mutations.) Suggest: register it when we finish Phase 2 (data layer) so we can test auth in Phase 3 (screens). Until then, develop against the public API + mock data.

### Q-106 (new) — Agent: separate app timing
You mentioned the goal of a separate LLM agent app. When should we start that? Suggest: after Only-List v1 is feature-complete + the agent modules (`:core:agent:*`) are proven in-app. Roughly Phase 5+.

---

## Summary

- **All session-1 blocking questions: ANSWERED.**
- **4 new questions** (Q-101..Q-104) — non-blocking, I'll proceed with defaults unless you object.
- **7 deferred questions** (Q-026..Q-031, Q-105, Q-106) — for later phases.

Ready to proceed with Phase 1 (Gradle project scaffold + CI + core screens).
