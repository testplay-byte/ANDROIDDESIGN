# AGENT-TECH — Android Agent Application

> A dedicated Android AI agent application — separate from the Only-List anime tracker.
> **Branch:** `agent-tech` (dedicated to this project only)
> **Status:** Research + Planning phase (no code yet)

---

## What this is

A **dedicated Android Agent application** that:
- 🔒 Works inside a **user-selected folder** (sandboxed — cannot access files outside that folder)
- 📁 Creates **multiple workspaces** within that folder
- 🔄 Runs in the **background** (user can use the phone normally while the agent works)
- 🤖 Supports **custom LLM models** (user-configurable: OpenAI, Anthropic, Gemini, Ollama, OpenAI-compatible)
- 🎨 Uses a **red/yellow/blue glass-inspired** design language with **frosted glass** effects + **light theme with dark grey vibes**

---

## Research completed (5 sub-agents, all source-verified)

| Report | Topic | Key finding |
|--------|-------|-------------|
| `R-A1-cline-analysis.md` | Cline | Iterative `AgentRuntime` + 9 SDK tools (portable) + dual compaction. Apache 2.0. NO sandboxing — must enforce SAF. |
| `R-A2-kilocode-analysis.md` | Kilo Code | Auto-compaction + glob permissions + plan files + custom agents. MIT (OpenCode fork). ~600 LoC Kotlin to port. |
| `R-A3-opencode-analysis.md` | OpenCode | **Daemon-first architecture** — best backbone for Android. HTTP/SSE/WebSocket protocol. BackgroundJob task queue. Multi-workspace native. |
| `R-A4-openhands-analysis.md` | OpenHands | **Autonomous agent pattern** — best for background execution. 13-state machine. Code execution sandbox. Event-stream persistence. Budget caps. |
| `DESIGN-LANGUAGE.md` | Design | "Primary Glass" — light bg `#F5F5F5` + dark grey `#2E2E2E` + red `#E53935` + yellow `#FFC107` + blue `#1E88E5`. Haze frosted glass. |

---

## Architecture recommendation (synthesized from all 4 analyses)

### Backbone: **Cline SDK structure + OpenHands autonomous pattern**

| Layer | Source | What to port |
|-------|--------|-------------|
| **Agent loop** | Cline `AgentRuntime` | Iterative `while(isActive)` + `maxIterations=25` + `AbortController` + overflow recovery |
| **Autonomous mode** | OpenHands | 13-state machine + event-stream persistence + budget caps + `SwitchLLMAction` + `FinishAction` |
| **Tool system** | Cline `createTool` factory | 9 SDK tools (read_files, editor, apply_patch, fetch_web, search_codebase, skills, ask_question, submit_and_exit, spawn_agent) |
| **Context management** | Cline dual compaction + Kilo Code auto-compaction | Anchored-summary template + update-don't-replace + tail preservation |
| **Permissions** | Kilo Code glob patterns | `Rule{action, resource, effect}` + `Wildcard.match` + per-workspace rulesets |
| **Plan files** | Kilo Code | `.kilo/plans/*.md` persistent + 3-tier resolution |
| **Custom agents** | Kilo Code | Markdown+YAML frontmatter → system prompt |
| **Background tasks** | OpenCode `BackgroundJob` + OpenHands automation | Start/extend/wait/cancel + Room persistence for OS-kill resilience |
| **Sandboxing** | OpenHands 3-tier + Android SAF | Tier 1: SAF (mandatory) / Tier 2: Termux bridge (opt-in) / Tier 3: Cloud sandbox (opt-in premium) |
| **Multi-workspace** | OpenCode daemon multi-directory | One process, many workspaces, directory-keyed |
| **LLM providers** | Cline 5 adapters | OpenAI-compatible (default), Anthropic, Gemini, Ollama, Bedrock (optional) |
| **UI** | Original Compose | Chat thread + tool cards + workspace selector + file browser + settings |

### Estimated effort
- **~8 KLOC Kotlin** (original work)
- **~2 KLOC** (Cline backbone port)
- **~600 LoC** (Kilo Code pattern ports)
- **XL effort**: 8-12 weeks / 1 dev for MVP

---

## Module structure (proposed)

```
:app                              ← Compose UI (chat, workspace, settings)
├── :agent:shared                 ← pure Kotlin (types, schemas, contracts)
├── :agent:llm                   ← 5 provider adapters (OpenAI-compatible default)
├── :agent:core                   ← AgentRuntime + ContextManager + CheckpointStore
├── :agent:tools                  ← 9 tools + tool registry + executor
├── :agent:permissions            ← glob permission ruleset + approval gateway
├── :agent:storage                ← Room (sessions, events, plan files, snapshots)
├── :agent:sandbox                ← SAF adapter + Termux bridge (opt-in)
├── :agent:background             ← ForegroundService + WorkManager + BackgroundJob
├── :core:designsystem            ← Primary Glass theme + components
├── :core:common                  ← Logger + utils
└── :core:database                 ← Room database
```

---

## Key limitations + risks (highlighted)

### 🔴 Critical limitations
1. **No Docker on Android** — OpenHands' sandbox approach (Docker containers) doesn't work. Must use SAF (Tier 1) or Termux (Tier 2) or cloud (Tier 3).
2. **No terminal/shell** — Cline's `run_commands` tool is dropped. Android has no shell access (unless Termux installed).
3. **No ripgrep** — Cline's `search_codebase` needs a Java regex walk replacement.
4. **Background execution limits** — Android kills background processes. Must use ForegroundService (persistent notification) + WorkManager for reliability.
5. **No MCP-stdio** — stdio transport impossible on Android (no subprocess). HTTP/SSE MCP only.
6. **Battery drain** — 25-iteration agent run with streaming + LLM calls = battery heavy. WorkManager constraints (charging + wifi) recommended.
7. **API key security** — Must use EncryptedSharedPreferences + Android Keystore. Never in plain SharedPreferences.
8. **Memory pressure** — Streaming large responses can OOM on low-end devices. Stream to Room, not memory.

### 🟡 Moderate risks
9. **Provider OAuth on mobile** — Some providers' OAuth flows may not work well on mobile (redirect URI issues).
10. **50MB+ Node.js runtime** — If using OpenCode's daemon approach (Bun/Node on Android), the runtime itself is ~50MB. Native Kotlin port avoids this.
11. **Rate limits** — User's LLM provider may rate-limit. Must implement exponential backoff + `Retry-After` handling.
12. **Permission fatigue** — Too many approval prompts = bad UX. Smart auto-approve defaults needed.

### 🟢 Low risks
13. **Haze 1.1.1 on light theme** — verified works (same as Only-List, just different tint colors).
14. **Variable fonts** — already solved in Only-List (FontVariation.Settings).
15. **Custom models** — OpenAI-compatible endpoint pattern covers 95% of use cases.

---

## Next steps (planning, not implementation)

1. **Screen design** — plan the exact screens (Chat, Workspace selector, File browser, Runs/History, Settings, Onboarding)
2. **Tool spec** — finalize the 9 tools + their Android-specific implementations
3. **Sandbox spec** — SAF adapter design + Termux bridge protocol
4. **Background execution spec** — ForegroundService + WorkManager + Room event-stream
5. **LLM provider spec** — 5 adapters + custom model config UI
6. **Build the app** — scaffold Gradle project + CI + design system starter

---

## For AI agents starting a new session

1. Read this README.
2. Read `DESIGN-LANGUAGE.md` for the visual spec.
3. Read the 4 research reports in `research/` for architecture details.
4. Read `AGENT-CONTEXT/memory/` for progress + decisions.
