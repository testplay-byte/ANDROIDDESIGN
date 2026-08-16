# R-A4 — OpenHands Analysis for Android Agent

**Task ID:** R-A4
**Date:** 2026-08-16
**Source repo:** https://github.com/OpenHands/OpenHands (cloned locally to `references/openhands/`, 22 MB, depth=1)
**Cross-referenced:** https://github.com/OpenHands/software-agent-sdk (Python runtime, referenced from this repo; analyzed via docs + Dockerfile + AGENTS.md + web search)

---

## 0. TL;DR — the single most important finding

OpenHands has been **architecturally refactored** since its OpenDevin origins. The `OpenHands/OpenHands` repo on GitHub is **no longer the autonomous agent runtime** — it is now **Agent Canvas**, a TypeScript/React frontend (with an Electron desktop wrapper). The actual autonomous agent runtime, sandbox, tools, and LLM provider layer now live in a separate Python repo called **`software-agent-sdk`** (the `openhands-agent-server`, `openhands-sdk`, `openhands-tools`, `openhands-workspace` packages) plus a separate **`openhands-automation`** repo for scheduled/event-driven runs.

This split matters enormously for Android porting: the *patterns* to port live in the Python SDK, while the *UI patterns* to ignore live in this repo. Porting OpenHands to Android does **not** mean porting this TypeScript repo — it means porting the Python agent runtime to Kotlin, optionally borrowing UX concepts from the React frontend.

The full autonomous pattern (ReAct loop, condensation, sub-agents, code-execution sandbox, MCP, Skills, LLM profile switching) is **portable** and is **better suited for a background Android agent** than Cline's interactive pattern.

---

## A. Architecture Overview

### A.1 What is OpenHands?

- **Origin**: Started as "OpenDevin" in early 2024 (autonomous AI software engineer inspired by Devin). Rebranded to OpenHands mid-2024. In 2025–2026 the project was re-architected into a multi-component system with a commercial cloud offering (OpenHands Cloud) and an ACP-based agent protocol that can drive third-party agents (Claude Code, Codex, Gemini CLI).
- **Purpose**: A self-hostable autonomous coding agent that runs *unattended* on a user's machine or server, with multi-step planning, code execution, file editing, browser automation, and a persistent event stream. Designed to be left running on a server (`SELF_HOSTING.md` walks through systemd setup).
- **License**: **MIT** — confirmed at `LICENSE` line 1 (`The MIT License (MIT), Copyright © 2025 OpenHands contributors`). Safe to port.
- **Status**: README badge "status-beta". Current pinned versions in `config/defaults.json`: `agentServer 1.42.1`, `agentCanvas 1.13.0`, `automation 1.7.1`.

### A.2 Tech stack

| Component | Language | Stack |
|---|---|---|
| Agent Canvas (this repo) | TypeScript | React 19, react-router 7, Vite 8, Zustand 5, TanStack Query 5, Tailwind 4, HeroUI 2, xterm 6, Monaco Editor, axios, socket.io-client, framer-motion. Electron desktop wrapper. Node ≥ 22.12. |
| Agent Server (software-agent-sdk, not in this repo) | Python | FastAPI + uvicorn + pydantic + httpx + LiteLLM + Jupyter kernel (for `run_ipython`). Distributed via `uv`/`uvx` and the `ghcr.io/openhands/agent-server` image. |
| Automation Server (openhands-automation) | Python | FastAPI + uvicorn + SQLAlchemy + asyncpg/SQLite + boto3 (for S3 tarball storage). |
| Sandboxes | Various | Docker, E2B, Daytona, RemoteRuntime, Apptainer, Local (no isolation). |

The TypeScript `package.json` confirms: `@openhands/typescript-client` (1.38.0) is the SDK client; `@openhands/extensions` (0.16.0) provides the Skills catalog. **There is no Python in this repo** — only `tools/` with a legacy `canvas_ui_tool` Python compatibility shim for persisted conversations (`OH_EXTRA_PYTHON_PATH=/opt/agent-canvas/tools`).

### A.3 What kind of app is it?

It is **all four** — a hybrid system:

1. **A web app** — Agent Canvas serves a static React SPA at `:8000` (or `:8000/canvas` in Docker).
2. **A REST + WebSocket API server** — Agent Server exposes `/api/conversations`, `/api/settings`, `/api/file/*`, `/api/bash/*`, `/api/git/*`, `/api/options/models`, `/api/skills`, `/api/mcp/*`, `/api/automation/*`, `/server_info`, `/sockets`, `/alive`, `/health`, `/ready`, `/docs`, `/openapi.json`. WebSocket `/sockets` carries the live event stream.
3. **A runtime** — the Agent Server embeds the sandbox runtime; that runtime owns a Jupyter kernel and bash session per conversation. Tools execute there.
4. **A sandbox** — explicitly, the runtime IS the sandbox (Docker container, E2B sandbox, Daytona sandbox, RemoteRuntime, Apptainer, or LocalRuntime).

### A.4 How does it differ from Cline/Kilo/OpenCode?

| Aspect | Cline / Kilo / OpenCode | OpenHands |
|---|---|---|
| Interaction model | **Interactive** — one user turn → one assistant turn (with tool calls). User watches and approves. | **Autonomous** — given a high-level task, runs multi-step ReAct loop until `finish` or budget cap. Can run unattended. |
| Sandbox | Host shell (no isolation) | Docker/E2B/Daytona/Remote/Apptainer/Local (selectable) |
| Code execution | Bash only (host) | Bash + **IPython (Jupyter)** — full Python kernel |
| Browser tool | None / limited | Full browser automation (`browser_*` action set) |
| Sub-agents | Recent Cline addition | First-class — `delegate` action + child conversations |
| Persistence | Per-session file in workspace | Event-stream DB on disk (`~/.openhands/agent-canvas/conversations/`) — resumable |
| Multi-conversation | One per VSCode window | Multi-conversation server, switchable |
| Background ops | None | Automation backend (cron/webhook) |
| LLM | One model per session | **LLM profile switching mid-task** (`SwitchLLMAction`) |
| Confirmation mode | Per-tool approval | `NeverConfirm` / `AlwaysConfirm` / `ConfirmRisky` with optional LLM-based risk analyzer |
| Extensibility | MCP (recent) | MCP + Skills (markdown playbooks) + Plugins (git-installed Python tools) |
| Multi-agent providers | No | **ACP** — drives Claude Code/Codex/Gemini CLI as alternative agents |
| Budget caps | No | `max_iterations` + `max_budget_per_task` |

OpenHands is **architecturally closer to a server-side engineering team mate** than to an IDE pair-programmer.

---

## B. Tool System Analysis

### B.1 Full tool enumeration

Source: `src/types/agent-server/core/base/action.ts` (the discriminated union `Action`) + `src/types/action-type.tsx` (the `ActionType` enum).

| # | Action (TS interface) | ActionType enum | Description | Portable to Android? |
|---|---|---|---|---|
| 1 | `ExecuteBashAction` | `RUN = "run"` | Run a bash command with timeout, is_input, reset. Captures stdout/stderr/exit_code/PS1 metadata. | **Partial** — works via Termux or `Runtime.exec("sh")`; loses Docker isolation. |
| 2 | `TerminalAction` | (tool name `terminal`) | Persistent terminal session, supports `is_input` for sending keystrokes to a running process, `reset` to recreate session. | **Partial** — Termux; built-in PTY is harder. |
| 3 | `FileEditorAction` | `READ`/`WRITE` | Legacy file editor with `view`/`create`/`str_replace`/`insert`/`undo_edit`. | **Yes** — pure file IO over SAF or scoped storage. |
| 4 | `StrReplaceEditorAction` | (tool name `file_editor`) | Modern file editor with same ops, returns `old_content`/`new_content` diff in observation. | **Yes** |
| 5 | `GlobAction` | (in `file_editor` tool group) | Glob match files, sorted by mtime, capped at 100 results. | **Yes** |
| 6 | `GrepAction` | (in `file_editor` tool group) | Regex search file contents, returns matches + include glob filter, capped at 100 files. | **Yes** |
| 7 | `ExecuteIPythonAction` (implied) | `RUN_IPYTHON = "run_ipython"` | Execute Python code in a persistent Jupyter kernel. State persists across calls (variables, imports). | **Partial** — needs Python interpreter (Termux Python / Chaquopy / Pyodide / remote sandbox). |
| 8 | `BrowserNavigateAction` | `BROWSE = "browse"` | Open URL in embedded browser. | **Yes** — Android WebView. |
| 9 | `BrowserClickAction` | (in `browse_interactive` group) | Click element by index. | **Partial** — needs DOM extraction from WebView; doable via JavaScript interface. |
| 10 | `BrowserTypeAction` | (in `browse_interactive` group) | Type text into input by index. | **Partial** — same. |
| 11 | `BrowserGetStateAction` | (in `browse_interactive` group) | Get DOM state + optional screenshot (base64). | **Partial** — WebView can screenshot + evaluate JS for AX tree. |
| 12 | `BrowserGetContentAction` | (in `browse_interactive` group) | Extract page text content (paginated by `start_from_char`). | **Yes** |
| 13 | `BrowserScrollAction`, `BrowserGoBackAction`, `BrowserListTabsAction`, `BrowserSwitchTabAction`, `BrowserCloseTabAction` | (in `browse_interactive` group) | Tab + scroll + back controls. | **Partial** |
| 14 | `ThinkAction` | `THINK = "think"` | Log a thought (Chain-of-Thought scratchpad). No side effect. | **Yes** — pure metadata. |
| 15 | `FinishAction` | `FINISH = "finish"` | Terminate the task with a final message. | **Yes** |
| 16 | `TaskTrackerAction` | `TASK_TRACKING = "task_tracking"` | `view` or `plan` a structured task list (`TaskItem[]`). Used for long-horizon planning. | **Yes** — Room DB / in-memory. |
| 17 | `PlanningFileEditorAction` | (planning tool) | Edit `workspace/project/PLAN.md` with same ops as FileEditor. The plan persists across the conversation. | **Yes** |
| 18 | `TaskAction` (delegate) | `DELEGATE = "delegate"` | Spawn a subagent of `subagent_type` with a `prompt`; returns result + `task_id` + `status`. Gated by `enable_sub_agents` → `task_tool_set` tool group. | **Yes** — recursive agent loop. |
| 19 | `MCPToolAction` | `MCP = "call_tool_mcp"` | Call a tool on an MCP server. `data` is a dynamic record. | **Yes** — MCP works on Android (JSON-RPC over stdio/HTTP/SSE). |
| 20 | `InvokeSkillAction` | (skills tool) | Invoke a loaded Skill (a markdown playbook with `triggers`). The skill content is returned as `TextContent` for the LLM. | **Yes** |
| 21 | `SwitchLLMAction` | (LLM tool) | Switch the active LLM profile mid-task with a `reason`. Enables cost control (cheap model for easy steps, expensive for hard). | **Yes** |
| 22 | `CanvasUIAction` | (client tool) | UI commands: `navigate_to_file`, `open_tab`, `show_preview`. Emitted over the existing WebSocket; intercepted by the frontend. | **Drop** — UI-coupled; re-implement natively if wanted. |
| 23 | `LaunchChildConversationAction` | (client tool) | Launch a child conversation (separate `conversation_id`, `parent_conversation_id`) with `target`/`task`/`title`/`repository`/`branch`/`isolation`. | **Yes** — multi-conversation manager. |
| — | `MessageEvent` (system) | `MESSAGE = "message"` | User/assistant/system messages (not a tool per se). | **Yes** |
| — | `SystemEvent` | `SYSTEM = "system"` | System prompt + tool manifest announcement. | **Yes** |
| — | `ChangeAgentStateAction` | `CHANGE_AGENT_STATE` | Internally change agent state (pause/resume). | **Yes** |
| — | `RejectAction` | `REJECT = "reject"` | Reject a request. | **Yes** |
| — | `INIT` action | `INIT = "initialize"` | Bootstrap the agent (client-only). | **Yes** |

**Default tools enabled at conversation start** (`src/api/agent-server-adapter.ts:105`):
```ts
const DEFAULT_TOOL_NAMES = ["terminal", "file_editor", "task_tracker"];
const BROWSER_TOOL_SET_NAME = "browser_tool_set";   // gated by VITE_ENABLE_BROWSER_TOOLS
const TASK_TOOL_SET_NAME  = "task_tool_set";          // gated by enable_sub_agents
```

`browser_tool_set` is the entire `browse_interactive` action group (9 actions) — they ship or are dropped together. Same for `task_tool_set` (delegate + child conversation).

### B.2 Tool registration + dispatch

**Registration (frontend → backend)**: At conversation start, `getAgentTools()` builds the `tools: [{name, params}]` array from `DEFAULT_TOOL_NAMES` + `agentSettings.tools` (user-configured list) and posts it in the start request. The agent-server persists it per-conversation. The agent's system prompt is constructed from the tool manifest.

**Dispatch (backend)**: When the LLM emits a tool_call, the agent-server matches it to a registered tool, executes it inside the sandbox runtime, and emits an `ObservationEvent` over WebSocket with the result. The agent-server advertises `usable_tools` in `/api/server_info` so the frontend can gate UI affordances (`isAgentServerToolAvailable(name)` in `src/api/agent-server-compatibility.ts:149`).

**Confirmation policy** (`getConversationConfirmationPolicy` in `agent-server-adapter.ts:581`):
- `NeverConfirm` (default)
- `AlwaysConfirm` (when `confirmation_mode=true`, no analyzer)
- `ConfirmRisky` (when `security_analyzer="llm"`, threshold HIGH, confirm_unknown=true)

Each tool call may be intercepted by a security analyzer:
- `LLMSecurityAnalyzer` — calls an LLM to assess risk; `ActionEvent.security_risk` field carries the verdict.
- `PatternSecurityAnalyzer` — regex-based pattern matching.
- `PolicyRailSecurityAnalyzer` — declarative policy rules.

### B.3 Custom tools

Three extension mechanisms — all portable to Android:

1. **MCP servers** (`mcp_config` in settings, `MCPConfig` from `@openhands/typescript-client`). Any JSON-RPC-over-stdio/HTTP/SSE MCP server works. Tools surface as `MCPToolAction`. **Best Android extension path** — MCP works fine on Android.
2. **Skills** (`@openhands/extensions/skills` catalog + user/project skills). A Skill is a markdown file with frontmatter (`name`, `description`, `triggers: [keywords]`, `allowed_tools: [...]`). Loaded by trigger or by `InvokeSkillAction`. The skill content gets injected into the LLM context. **Trivially portable** — just markdown + a resolver.
3. **Plugins** (`PluginSpec` — `source: "github:owner/repo"`, `ref`, `repo_path`, `parameters`). Installed into the sandbox; adds Python tool classes via `openhands-tools` package mechanism. **Hard to port** — assumes Python runtime inside sandbox. Skip on Android; use MCP instead.

---

## C. Agent Loop + Context Management

### C.1 Loop pattern

OpenHands uses a **ReAct-style loop with event-stream history** — but more sophisticated than textbook ReAct:

```
    ┌──────────────────────────────────────────────────────────┐
    │ 1. Build system prompt (tools + skills + runtime info)   │
    │ 2. Append user message → event history                   │
    │ 3. Loop:                                                  │
    │    a. Send {system, history} to LLM (with tool manifest)  │
    │    b. LLM streams tokens → StreamingDeltaEvent            │
    │    c. LLM emits one or more tool_calls (parallel ok)       │
    │       → ActionEvent {thought, action, tool_call, ...}    │
    │    d. For each action:                                    │
    │       - Security analyzer (optional)                     │
    │       - If confirmation_mode → AWAITING_USER_CONFIRMATION │
    │       - Dispatch to sandbox runtime                       │
    │       - → ObservationEvent                                │
    │       - Append to history                                 │
    │    e. Check max_iterations, max_budget_per_task           │
    │    f. If FinishAction → break                             │
    │    g. If error → AgentErrorEvent                          │
    │ 4. Persist conversation state to disk                     │
    └──────────────────────────────────────────────────────────┘
```

**Distinctive features vs textbook ReAct**:
- **Parallel tool calls**: `llm_response_id` groups actions from the same LLM response. Supported via OpenAI/Anthropic native parallel tool calling.
- **`think` action**: Explicit Chain-of-Thought scratchpad (separate from `reasoning_content`/`thinking_blocks` which are LLM-native reasoning).
- **`task_tracking`**: The LLM can `view`/`plan` a structured task list at any step — for long-horizon planning. Persistent across the conversation.
- **`PlanningFileEditorAction`**: Edits a `PLAN.md` file in the workspace. Different from task tracker — it's a free-form doc the agent writes for itself.
- **`SwitchLLMAction`**: Mid-task model switching. The agent picks the model that fits the next step.
- **Confirmation mode**: Optional human-in-the-loop safety gate (per-action).

**Agent state machine** (`src/types/agent-state.tsx`):
```
LOADING → INIT → RUNNING ⇄ AWAITING_USER_INPUT
                  ↓
              AWAITING_USER_CONFIRMATION → USER_CONFIRMED → RUNNING
                                        → USER_REJECTED  → RUNNING
                  ↓
              PAUSED → RUNNING
                  ↓
              STOPPED / FINISHED / REJECTED / ERROR / RATE_LIMITED
```

This is **already designed for both interactive and unattended operation** — important for §J.

### C.2 Context management

**Event-stream history**: The conversation is a list of `OpenHandsEvent`s (`src/types/agent-server/core/openhands-event.ts`):
- `ActionEvent` (with thought, tool_call, security_risk, optional critic_result)
- `ObservationEvent` (the tool result)
- `MessageEvent` (user/assistant/system text)
- `UserRejectObservation` (when user rejects an action)
- `AgentErrorEvent`, `ConversationErrorEvent`, `ServerErrorEvent`
- `SystemPromptEvent` (the assembled system prompt — recorded for replay)
- `CondensationEvent`, `CondensationRequestEvent`, `CondensationSummaryEvent`
- `ConversationStateUpdateEvent` (status transitions)
- `HookExecutionEvent` (pre/post tool hooks fired)
- `PauseEvent`
- `ACPToolCallEvent` (sub-agent tool call when using ACP agent)
- `StreamingDeltaEvent` (token-level stream)

**Condensation** (the killer feature): When the running token count approaches the model's context window, the server emits `CondensationRequestEvent`, runs a condenser (configurable: `LLMSummarizingCondenser`, `RecentEventsCondenser`, `BrowserOutputCondenser`, etc.), and emits `CondensationEvent` listing `forgotten_event_ids` + an optional `summary` + `summary_offset`. The LLM's view becomes `[summary, recent_events]`. Old events stay on disk for audit but are dropped from the LLM context. This is **first-class** in the architecture — not a hack. Settings: `enable_default_condenser`, `condenser_max_size`.

**Resource caps**: `max_iterations` (turn count) and `max_budget_per_task` (USD cost). The agent hard-stops when either is hit.

### C.3 Sub-agents + task delegation

Two mechanisms:

1. **`TaskAction` (delegate)** — synchronous sub-agent call. The parent agent spawns a subagent of `subagent_type` with a `prompt`. The subagent runs in the **same conversation** (shared event history) until it finishes, then returns its result as `TaskObservation.content`. Lightweight — used for "research X, then come back".

2. **`LaunchChildConversationAction`** — spawns a **separate conversation** with `parent_conversation_id`, `target`, `task`, `title`, `repository`, `branch`, `isolation`. Async — the parent gets an acknowledgement, the child runs in parallel, results surface via `sub_conversation_ids` on the parent `AppConversation`. Heavier — used for "decompose this into N parallel sub-tasks".

Both are gated by `enable_sub_agents` → `task_tool_set` tool availability. For Android, the child-conversation pattern maps cleanly to a WorkManager-driven multi-conversation manager.

### C.4 Key question: different pattern than Cline?

**Yes, fundamentally.** Cline is a single-turn-per-user-input assistant. OpenHands is an autonomous multi-turn agent. The differences that matter for Android porting:

| Concern | Cline | OpenHands |
|---|---|---|
| Loop driver | User submit | Agent itself (until `finish`) |
| State persistence | Per-session file | Event-stream DB, resumable |
| Context overflow handling | Sliding window + custom summary | **Condensation events** (auditable, replayable) |
| Code execution | Host bash | Sandboxed bash + IPython |
| Sub-agents | Recent addition, basic | First-class (delegate + child convs) |
| Budget caps | None | `max_iterations` + `max_budget_per_task` |
| Background operation | Not designed | **Designed for it** (Agent Server runs headless; Automation backend schedules runs) |

---

## D. LLM Provider Abstraction

### D.1 Supported providers

OpenHands uses **LiteLLM** (Python library, in `software-agent-sdk`) under the hood → **100+ providers** out of the box: OpenAI, Anthropic, Google (Gemini/Vertex), AWS Bedrock, Azure OpenAI, Mistral, Cohere, Groq, Together AI, OpenRouter, Anyscale, Replicate, Fireworks, AI21, DeepInfra, Lemonade, local Ollama, vLLM, LM Studio, llama.cpp server, etc.

The frontend doesn't hardcode the list — `GET /api/options/models` returns:
```ts
interface ModelsResponse {
  models: string[];              // flat "provider/model" list
  verified_models: string[];     // OpenHands-tested
  verified_providers: string[];  // for the "Verified" section in the picker
  default_model: string;         // e.g. "openhands/claude-opus-4-5-20251101"
}
```
The backend is the single source of truth (`src/api/option-service/option.types.ts:9`).

**Plus ACP** — when `agent_kind="acp"`, the LLM is owned by the spawned subprocess (Claude Code, Codex, Gemini CLI). OpenHands just relays turns. Authentication: subscription login (auto-detected from `~/.claude/.credentials.json`, `~/.codex/auth.json`, `~/.gemini/oauth_creds.json`) OR API key (`ANTHROPIC_API_KEY` / `OPENAI_API_KEY` / `GEMINI_API_KEY`). For Android, the ACP path is less interesting than the native OpenHands LiteLLM path — but ACP proves the architecture is provider-agnostic.

### D.2 Provider interface

```ts
// src/types/settings.ts
type Settings = {
  llm_model: string;             // "anthropic/claude-3-5-sonnet-20241022"
  llm_base_url: string;          // optional, for OpenAI-compatible endpoints
  llm_api_key: string | null;    // encrypted at rest (OH_SECRET_KEY)
  llm_api_key_set: boolean;      // cloud-shape "key on file"
  llm_api_key_is_set?: boolean;  // agent-server-shape "key on file"
  // ... plus per-provider tokens for GitHub/GitLab/Bitbucket/AzureDevOps/Forgejo
  provider_tokens_set: Partial<Record<Provider, string | null>>;
  // ...
};
```

**LLM profiles**: Saved named profiles (`title_llm_profile`, `agent_profile_id`), each with its own model/key/base_url. The `SwitchLLMAction` lets the agent switch profiles mid-task. Stored server-side and encrypted.

### D.3 Custom models?

**Yes**, three ways:
1. **`llm_base_url`** — point at any OpenAI-compatible endpoint (Ollama, vLLM, LM Studio, llama.cpp, LocalAI). Works for any model the endpoint serves. **Best Android path** — local model served by Termux+Ollama, or a custom OpenAI-compatible gateway.
2. **LiteLLM provider config** — full LiteLLM provider dict supports custom auth, custom headers, custom model routing. Not all surfaced in the UI but exposed in the SDK.
3. **ACP** — spawn any third-party agent CLI.

### D.4 Streaming?

**Yes, full token-level streaming.** `StreamingDeltaEvent` over WebSocket carries deltas as the LLM generates them. Supports:
- Text deltas
- **Thinking blocks** (Anthropic Claude) — both `ThinkingBlock` and `RedactedThinkingBlock`
- **Reasoning content** (DeepSeek/other reasoning models) — separate `reasoning_content` field on `ActionEvent`
- Tool call deltas (parallel tool calls stream together, grouped by `llm_response_id`)

The frontend renders streaming via `useHandleWsEvents` (`src/hooks/use-handle-ws-events.ts`) and `useTerminal` for the live terminal pane.

---

## E. File System + Sandboxing (CRITICAL)

### E.1 How OpenHands sandboxes the agent

OpenHands supports **multiple sandbox runtimes** (in `software-agent-sdk`, Python; this repo references them via `RuntimeServicesInfo` and `docker/`):

| Runtime | Where it runs | Isolation | Used in |
|---|---|---|---|
| **DockerRuntime** (default) | Docker container (`ghcr.io/openhands/agent-server` image) | Container namespaces, UID-isolated, seccomp profile | Default local install via `docker run`, and the `docker/Dockerfile` all-in-one image |
| **E2BRuntime** | E2B cloud sandbox | E2B-managed microVM | Optional config |
| **DaytonaRuntime** | Daytona cloud sandbox | Daytona-managed container | Optional config |
| **RemoteRuntime** | OpenHands Cloud remote sandbox | Cloud-managed | OpenHands Cloud commercial offering |
| **ApptainerSandbox** | Apptainer (Singularity) container | HPC-style container, no root needed | Documented at `docs.openhands.dev/sdk/guides/agent-server/apptainer-sandbox` |
| **LocalRuntime** | Host process directly | **None — full filesystem access** | `npm install -g @openhands/agent-canvas && agent-canvas` (Option 1 in README — flagged "WARNING: agent will have full access to your filesystem") |

Per-conversation, the agent-server provisions a sandbox instance (or reuses one from a pool), starts a bash session + Jupyter kernel inside, and routes all `run` / `run_ipython` / `file_editor` tool calls through the sandbox's API. The sandbox exposes:
- `execute_bash(command, is_input, timeout, reset)` → stdout/stderr/exit_code
- `run_ipython(code)` → Jupyter output (text, images, errors)
- `read_file` / `write_file` / `list_files`
- `browse` / `browse_interactive` (browser is also sandboxed — runs inside the sandbox container)

### E.2 Folder restriction

**Docker path**: `docker run -v "$PROJECTS_PATH:/projects" ...` bind-mounts the host's `~/projects` (or whatever) into `/projects` inside the container. The agent can only see what's under `/projects` + what the image ships. The agent-server's working directory is set per-conversation (`AppConversationStartRequest` → `selected_repository` + `selected_branch` + `agent_type: "default" | "plan"`).

**Path resolution** (`src/api/agent-server-home.ts`): The frontend calls `GET /api/file/home` to get `Path.home()` on the server (e.g. `/root` or `/Users/foo`). `resolveAbsoluteAgentServerPath` resolves relative dirs against this home. The `/api/file/upload` endpoint requires absolute paths and `mkdir -p`s the parent — the README warns about macOS SIP if you naively prepend `/`.

**Volume mounts in `docker/Dockerfile`**:
```
VOLUME ["/home/openhands/.openhands", "/projects"]
```
- `/home/openhands/.openhands` — settings, secrets, conversations, automation DB (state)
- `/projects` — user code the agent can read/edit

Bind-mount both for data to survive container restarts.

### E.3 File edits

`StrReplaceEditorAction` is the primary file-editing tool (`src/types/agent-server/core/base/action.ts:96`):

```ts
interface StrReplaceEditorAction {
  command: "view" | "create" | "str_replace" | "insert" | "undo_edit";
  path: string;                          // absolute path inside sandbox
  file_text: string | null;              // for create
  old_str: string | null;                // for str_replace (must match exactly)
  new_str: string | null;                // replacement (empty = delete)
  insert_line: number | null;            // for insert (after this line, 1-indexed)
  view_range: [number, number] | null;  // for view (line range)
}
```

The `StrReplaceEditorObservation` returns `old_content` + `new_content` + `prev_exist` + `error`. The LLM sees the diff in the observation. **This is identical to Cline's `str_replace_editor` tool** — same author DNA. (`FileEditorAction` is the legacy alias; same shape.)

For binary files (images), `MessageImageContent` with `image_urls: string[]` (base64 data URLs) is used.

### E.4 Key question: Android sandbox options?

**Docker is NOT available on Android** (no kernel namespace support for unprivileged apps; even Termux can't run Docker). What are the alternatives?

| Option | Isolation | Code exec? | Difficulty | Recommended? |
|---|---|---|---|---|
| **SAF-only file ops (no shell)** | Android app sandbox + Storage Access Framework scoped dirs | No shell, no Python | Easy | **Yes — minimal viable baseline** |
| **Termux as runtime** | Termux app sandbox (separate UID); agent runs inside Termux process | Bash + Python + IPython (via pkg) | Medium — Termux:Boot + IPC bridge | **Yes — recommended for code exec** |
| **proot-distro (Termux)** | proot chroot into a Linux distro image (Ubuntu/Alpine) — no root needed | Bash + Python + IPython + apt | Medium | Yes — better isolation than bare Termux |
| **Android Isolated Process** (`android:isolatedProcess="true"`) | Separate UID, no permissions by default, no network | Limited (no shell, no Python by default) | Hard — restricted APIs | No — too restrictive for an agent |
| **chroot (root required)** | Full Linux chroot | Full | Hard + needs root | No — requires root, breaks non-rooted users |
| **Pyodide (WASM Python in WebView)** | Browser sandbox | Python (limited — no native packages, no FS) | Medium | Yes — for "lite" code exec |
| **Chaquopy / python-for-android** | In-process Python (same UID as app) | Real Python, limited packages | Medium | Yes — if no isolation needed |
| **Remote sandbox (E2B/Daytona cloud)** | Cloud-managed microVM | Full | Easy (just HTTP calls) | Yes — optional premium tier |
| **No sandbox (just SAF + app-level file ops)** | App sandbox only | Bash via `Runtime.exec("sh")` only | Easy | Acceptable for a personal agent |

**Recommended Android sandbox strategy** (for our project):
- **Tier 1 (mandatory baseline)**: SAF-scoped folder selection. Agent can `read`/`write`/`str_replace`/`glob`/`grep` files inside the user-selected folder. No shell exec, no Python. This already covers ~80% of agent use cases (code editing, file management, doc generation).
- **Tier 2 (optional, opt-in)**: Termux integration. User installs Termux + Termux:Boot. App connects via a local socket (or `am startservice` Intent). Agent gains `run` (bash) + `run_ipython` (Python). Workspace = SAF folder mounted into Termux via `termux-setup-storage` or symlinks.
- **Tier 3 (optional, premium)**: Remote cloud sandbox (E2B/Daytona) for heavy code execution. App is a thin client.

**OpenHands' `LocalRuntime` is the closest existing analog** to Tier 1+2 (no Docker, runs on host). The OpenHands team explicitly calls out this mode as "dangerous" because of full FS access — but on Android the app sandbox itself provides the isolation OpenHands lacks on desktop.

---

## F. Background Execution + Autonomous Operation

### F.1 How OpenHands handles autonomous / long-running tasks

The Agent Server is **designed for unattended operation**:

1. **State persistence**: Every event (Action, Observation, Message, Condensation, etc.) is persisted to disk under `~/.openhands/agent-canvas/conversations/`. The Docker entrypoint (`docker/entrypoint.sh:66`) sets `OH_CONVERSATIONS_PATH` and `OH_BASH_EVENTS_DIR`. Bash command outputs are separately persisted under `bash_events/` (paginated via `/api/bash/bash_events/search`). If the server crashes, it resumes from the persisted event stream.

2. **Async conversation start**: `POST /api/v1/app-conversations` returns immediately with an `AppConversationStartTask` that has its own lifecycle (`src/api/conversation-service/agent-server-conversation-service.types.ts:92`):
   ```
   WORKING → WAITING_FOR_SANDBOX → PREPARING_REPOSITORY →
   RUNNING_SETUP_SCRIPT → SETTING_UP_GIT_HOOKS → SETTING_UP_SKILLS →
   STARTING_CONVERSATION → READY | ERROR
   ```
   The client polls `/api/v1/app-conversations/tasks` (paginated) until READY.

3. **Resource caps**: `max_iterations` (turn count) and `max_budget_per_task` (USD) hard-stop runaway agents. `MetricsSnapshot.accumulated_cost` is tracked per conversation; `RuntimeMetrics` tracks per-model cost + response latency + token usage. The agent stops itself with `FinishAction` when done, or hits a cap.

4. **WebSocket with reconnect**: The frontend (`src/hooks/use-websocket.ts`, `use-handle-ws-events.ts`, `use-unified-websocket-status.ts`) maintains a socket.io connection with auto-reconnect. Events emitted while disconnected are replayed from the persisted event store on reconnect.

5. **Systemd / tmux**: The `SELF_HOSTING.md` guide shows production deployment via systemd unit (`agent-canvas.service`, `Restart=on-failure`). This is the canonical "leave it running" pattern.

### F.2 Task queue / job system

**Yes — two layers**:

1. **App-conversation start tasks** (Agent Server): `AppConversationStartTask` is a job in the start-task queue. Status polled via `use-task-polling.ts` hook. Multi-tenant — `created_by_user_id` field.

2. **Automation Server** (separate Python service, `openhands-automation` package): A full job system with:
   - **Triggers**: `cron` (schedule) or `event` (webhook — GitHub `pull_request.opened`, `push`, `release.*` with JMESPath filters).
   - **Automation spec**: name, trigger, repository, model, prompt, branch, plugins, timeout (default 600s/10min), notification.
   - **Run records**: `AutomationRun` with status `PENDING | RUNNING | COMPLETED | FAILED | CANCELLED | SKIPPED`, `bash_command_id` (run logs), `cost` (USD), `started_at`, `completed_at`, `error_detail`.
   - **Storage**: SQLite by default (`AUTOMATION_DB_URL=sqlite+aiosqlite:///...`), Postgres for production.
   - **Prebuilt integrations**: Slack, GitHub, Linear, Notion, Datadog.

### F.3 Headless (no UI)?

**Yes, fully.** Agent Server is a standalone CLI:
```bash
openhands-agent-server --port 18000   # production binary
# or
python -m openhands.agent_server --port 18000   # source
```
No UI required. The frontend is optional. The Automation Server is also fully headless (`uvicorn openhands.automation.app:app --host 0.0.0.0 --port 18001`). The entire Docker image is headless-capable — the static-server is just one of three services and can be skipped.

The Dockerfile confirms: the binary build starts with `FROM ${AGENT_SERVER_IMAGE} AS final` — the Agent Server image is the base; everything else is layered on top.

### F.4 Key question: Android background operation?

**Yes, the pattern works** — with caveats around Android's process lifecycle:

| Android mechanism | Use for | Limitations |
|---|---|---|
| **Foreground Service** (with notification) | Active agent loop (RUNNING state) | Required for any persistent work; user sees notification; survives background limits |
| **WorkManager** (expedited) | Scheduled automations (cron-equivalent), short bursts | ~10 min cap on expedited jobs; longer jobs need Foreground Service |
| **WorkManager** (regular) | Background compaction, history cleanup, low-priority tasks | Deferred under Doze; not for time-sensitive work |
| **AlarmManager** (exact) | Time-critical triggers (cron replacement) | Requires `SCHEDULE_EXACT_ALARM` permission; user-revocable on Android 12+ |
| **JobScheduler** | Event-driven triggers (e.g. file-changed, charging) | Coalesced by OS |
| **WebSocket keep-alive** | Live event streaming when app is foregrounded | Doze kills sockets; reconnect on resume |

**Persistence strategy that fits**: OpenHands' event-stream-on-disk model is **ideal** for Android. Every step persists to disk (Room DB or SQLite). If the OS kills the Foreground Service (low memory, battery saver), the next launch replays from the last persisted event. The agent's `AgentState` machine supports this natively — `PAUSED` → `RUNNING` transitions are first-class.

**Recommended Android architecture**:
```
┌──────────────────────────────────────────────────┐
│ Android App (Kotlin/Compose)                     │
│                                                  │
│  ┌─────────────┐    ┌────────────────────────┐  │
│  │ UI Layer    │◄──►│  Agent Runtime (Kotlin) │  │
│  │ (Compose)   │    │  - ReAct loop           │  │
│  └─────────────┘    │  - Tool dispatcher      │  │
│                     │  - LLM client (OkHttp)  │  │
│  ┌─────────────┐    │  - Event stream         │  │
│  │ Foreground  │◄──►│  - Condenser           │  │
│  │ Service     │    │  - MCP client           │  │
│  │ (long-running)   └──────────┬─────────────┘  │
│  └──────┬──────┘               │                │
│         │                      ▼                │
│  ┌──────▼──────┐    ┌────────────────────────┐  │
│  │ WorkManager │    │  Room DB (events,      │  │
│  │ (scheduled) │    │  conversations, runs)  │  │
│  └─────────────┘    └────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │ Tool Sandbox:                            │   │
│  │  - SAF-scoped FileEditor (always on)     │   │
│  │  - Termux bridge (opt-in: bash + python) │   │
│  │  - WebView (browser tool)                │   │
│  │  - MCP servers (extension)                │   │
│  └──────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```

---

## G. Code Execution (CRITICAL)

### G.1 Can OpenHands execute code?

**Yes — three ways:**

1. **`run` action (ExecuteBashAction)** — runs a bash command in the sandbox's terminal session. Supports:
   - `command: string` — the command (or `""` to retrieve more buffered output, or `"C-c"` to interrupt)
   - `is_input: boolean` — if `true`, `command` is sent as stdin to a running process (not a new command)
   - `timeout: number | null` — soft timeout (returns exit_code -1 if hit, can resume)
   - `reset: boolean` — recreate the terminal session (loses env vars + state)
   
   Returns `ExecuteBashObservation` with `content[]`, `command`, `exit_code`, `error`, `timeout`, `metadata` (PS1 metadata — cwd, exit_code, git branch, etc.).

2. **`run_ipython` action (RUN_IPYTHON)** — runs Python code in a **persistent Jupyter kernel**. Variables, imports, and state persist across calls. Returns notebook-style output (text, images via `MessageImageContent`, errors with traceback). This is **the killer feature** — agents can iteratively build up Python state (load a DataFrame, clean it, plot it, save the plot) the same way a human would in a notebook.

3. **`terminal` tool (TerminalAction)** — same as `run` but exposed as a separate persistent terminal session for the UI's terminal pane. The agent can run a long-lived process (e.g. `npm run dev`) and interact with it across turns via `is_input`.

### G.2 How the code execution sandbox works

The Agent Server provisions a sandbox per conversation. Inside the sandbox:
- A **bash session** (persistent PTY) is started for `run` / `terminal`.
- A **Jupyter kernel** (IPython) is started for `run_ipython`.
- A **browser** (headless Chromium via Playwright in the Docker image) is started for `browse` / `browse_interactive`.

The sandbox exposes a uniform action API (`execute_bash`, `run_ipython`, `read_file`, `write_file`, `list_files`, `browse`, etc.) that the agent-server calls over its internal RPC. The sandbox can be:
- A Docker container (DockerRuntime) — the most common; the `ghcr.io/openhands/agent-server` image bundles Python, IPython, Jupyter, Playwright, git, common CLI tools.
- An E2B sandbox (E2BRuntime) — cloud, microVM-based.
- A Daytona sandbox (DaytonaRuntime) — cloud, container-based.
- A RemoteRuntime — OpenHands Cloud sandbox.
- An Apptainer sandbox — HPC container, no root needed.
- A LocalRuntime — runs directly on the host process. No isolation.

### G.3 Key question: code execution on Android?

**Yes, multiple paths** — pick based on capability/isolation tradeoff:

| Path | What runs | Isolation | Package install? | Best for |
|---|---|---|---|---|
| **Termux + Python/IPython** | Real CPython + IPython in Termux | Termux UID (separate from app) | `pkg install python ipython jupyter` — yes | Full code execution; closest to OpenHands semantics |
| **proot-distro + Ubuntu** | Real Linux distro via proot (no root) | proot namespace | `apt install` — yes | Heavier isolation; closer to Docker feel |
| **Chaquopy / python-for-android** | Embedded CPython in app process | App sandbox | Limited (no apt; pip works for pure-Python) | In-app Python without external install |
| **Pyodide (WASM in WebView)** | CPython compiled to WASM | Browser sandbox | pip pure-Python only; no native ext | Lite code exec; no install friction |
| **Termux:Tasker / SL4A** | Shell + scripting | Termux | pkg | Shell-only path |
| **BeanShell / Rhino (JVM scripting)** | JVM-based scripting | App sandbox | n/a | Lightweight shell-like scripts |
| **Remote sandbox (E2B / Daytona)** | Cloud microVM | Cloud-managed | Full | Premium tier; thin client |
| **No code exec (shell via `Runtime.exec("sh")` only)** | Android's toybox/sh | App sandbox | No | Minimal viable; lose IPython |

**Recommended for our Android project**:
- **Tier 1 (default)**: SAF-scoped file ops + WebView browser. No code exec. Covers most agent tasks (editing, browsing, planning, doc generation).
- **Tier 2 (opt-in, "Pro mode")**: Termux integration. User installs Termux + Termux:API + Termux:Boot. App bridges via a Unix socket or `am` Intent. Provides real bash + Python + IPython — the full OpenHands experience.
- **Tier 3 (opt-in, "Cloud mode")**: Remote sandbox (E2B or self-hosted Agent Server). Thin client. Bypasses all Android limits; runs even when phone is locked.

**Important**: Termux runs as a separate app with its own UID. The agent app cannot directly write to Termux's home; IPC must go through Termux's Run Command Intent (`com.termux.RUN_COMMAND`) or a socket bridge. The user's selected SAF folder is the shared workspace — both apps access it via SAF.

---

## H. Android Portability Assessment

### H.1 Minimal viable subset to port

The "minimum viable Android agent" borrowing from OpenHands:

1. **ReAct agent loop** (Kotlin coroutines) — think→act→observe, with parallel tool calls
2. **Event-stream history** (Room DB) — every action + observation persisted
3. **Condensation** — LLM-based summarization when context fills
4. **LLM client** (OkHttp + SSE) — OpenAI-compatible REST, multi-provider
5. **Tools**:
   - `StrReplaceEditor` (view/create/str_replace/insert/undo_edit) — pure file IO over SAF
   - `Glob` + `Grep` — file search (use `java.nio.file` + regex)
   - `Think` (no-op metadata)
   - `Finish` (loop terminator)
   - `TaskTracker` (in-memory or Room-backed task list)
   - `SwitchLLM` (mid-task profile switch)
6. **Agent state machine** (the 13 states from `agent-state.tsx`)
7. **MCP client** (JSON-RPC over stdio/HTTP/SSE) for extensibility
8. **Skills loader** (markdown files with frontmatter)
9. **LLM profiles** (saved configs, encrypted at rest via Android Keystore)
10. **Resource caps** (`max_iterations`, `max_budget_per_task`)
11. **Foreground Service** for the agent loop + notification
12. **Compose UI** showing the live event stream + conversation list

This delivers ~70% of OpenHands' value on Android with **zero Python, zero Docker**.

### H.2 What MUST be dropped

| Component | Why drop | Replacement |
|---|---|---|
| **Docker sandbox** | No kernel namespace support on Android | SAF + app sandbox + optional Termux |
| **E2B / Daytona / RemoteRuntime** | Requires cloud accounts; out of scope for v1 | Optional premium tier (could re-add) |
| **Web UI (Agent Canvas React app)** | Wrong platform | Native Compose UI |
| **Python runtime in-process** | Heavy; Android apps shouldn't embed a Python interpreter in the main process | Skip; or use Termux out-of-process |
| **Automation Server** | Needs Postgres/SQLite + uvicorn; overkill for v1 | WorkManager + AlarmManager |
| **ACP subprocess agents** (Claude Code/Codex/Gemini CLI) | Requires Node.js + the CLI; CLI doesn't run on Android | Skip; native LLM only |
| **Ingress proxy / static server** | Web-server concern | Android app is its own server |
| **Apptainer sandbox** | HPC container; irrelevant on Android | Skip |
| **Playwright (headless Chrome for browse_interactive)** | Too heavy; doesn't run on Android | WebView + JS accessibility-tree extraction, or skip browser tool |
| **VSCode integration** (`/api/vscode/*`) | No VSCode on Android | Skip |
| **PostHog telemetry** | Optional; privacy concern on mobile | Optional, off by default |

### H.3 What MUST be reimplemented

| Component | OpenHands impl | Android port |
|---|---|---|
| Sandbox runtime | Docker container with bash + Jupyter | SAF-scoped FileEditor (Tier 1) + Termux bridge (Tier 2) |
| WebSocket event stream | socket.io over HTTP | Kotlin coroutines Flow + OkHttp WebSocket (or local StateFlow if no remote server) |
| Conversation persistence | JSON files on disk | Room DB (events table, conversations table, runs table) |
| Background execution | systemd / uvicorn | Foreground Service + WorkManager |
| LLM client | LiteLLM (Python) | Custom Kotlin client over OkHttp + SSE; multi-provider; OpenAI-compatible base |
| Tool dispatcher | Python tool classes | Kotlin `interface Tool { suspend fun execute(params): Observation }` + registry |
| Condenser | Python condenser strategies | Kotlin condenser (LLM-summarize or recent-events) |
| MCP client | Python MCP SDK | Kotlin MCP client (JSON-RPC; stdio via Termux or HTTP/SSE direct) |
| Skills loader | `@openhands/extensions` npm package | Bundled markdown resources in APK assets + user-added skills in SAF folder |
| Browser tool | Playwright in container | WebView with `evaluateJavascript` for AX tree extraction + `PixelCopy` for screenshots |
| Settings/secrets encryption | `OH_SECRET_KEY` (Fernet) | Android Keystore (AES-GCM) |
| Multi-conversation manager | Agent Server REST | In-app `ConversationManager` (Room) |

### H.4 Estimated effort per component

| Component | Effort (eng-weeks) | Risk |
|---|---|---|
| LLM client (OkHttp + SSE + multi-provider) | 2 | Low — well-trodden path |
| ReAct agent loop + tool dispatcher | 2 | Low |
| Event-stream persistence (Room) | 1 | Low |
| Condenser | 1 | Medium — LLM summarization quality |
| StrReplaceEditor + Glob + Grep + Read/Write | 1.5 | Low |
| TaskTracker + SwitchLLM + Think + Finish | 0.5 | Low |
| MCP client | 1.5 | Medium — protocol conformance |
| Skills loader | 0.5 | Low |
| Foreground Service + WorkManager | 1.5 | Medium — Android lifecycle quirks |
| Settings + LLM profiles + Keystore encryption | 1 | Low |
| Compose UI (conversation list + chat + event stream) | 3 | Medium — UX quality |
| Termux bridge (optional Tier 2) | 2 | High — Termux IPC is fiddly |
| WebView browser tool | 2 | Medium — AX tree extraction is brittle |
| Remote sandbox client (optional Tier 3) | 1.5 | Low — just HTTP |
| **Total MVP (Tier 1 only)** | **~14 weeks** | |
| **Total + Tier 2 + browser** | **~20 weeks** | |

### H.5 Key risks for Android

1. **Background execution kills**: Android will kill background services under memory pressure or battery saver. Mitigation: persist every event to Room; resume on next launch; warn user about battery optimization settings.
2. **No real sandbox isolation**: SAF + app sandbox is weaker than Docker. A malicious LLM-generated script could escape the workspace folder if Termux is enabled. Mitigation: user must opt in to Termux; clearly warn; default to SAF-only.
3. **LLM API keys on device**: Storing API keys on a phone is riskier than on a server. Mitigation: Android Keystore (hardware-backed on supported devices); never log keys; redact in exports.
4. **Token cost on mobile data**: Streaming long responses over cellular is expensive. Mitigation: Wi-Fi-only toggle; data saver mode; show estimated cost before run.
5. **Battery drain**: A long-running agent loop can drain battery fast. Mitigation: adaptive polling; back off when battery < 20%; prefer WorkManager deferred jobs for non-interactive tasks.
6. **Termux dependency**: If we rely on Termux for code execution, the user must install a separate app. Mitigation: clear onboarding; provide a no-Termux mode (Tier 1).
7. **Browser tool fidelity**: WebView AX tree extraction won't match Playwright's. Some sites won't work. Mitigation: mark as best-effort; fall back to `browse` (open URL, fetch content) when `browse_interactive` fails.
8. **MCP server discovery**: Most MCP servers are stdio-based; Android can't spawn arbitrary stdio subprocesses without Termux. Mitigation: support HTTP/SSE MCP servers natively; stdio MCP servers only via Termux.
9. **Condenser quality**: A weak condenser loses context and the agent loops. Mitigation: start with `RecentEventsCondenser` (simple, no LLM call); add `LLMSummarizingCondenser` as opt-in.
10. **Multi-model cost tracking**: Different providers price differently; hard to give accurate USD cost. Mitigation: use LiteLLM's cost model JSON (portable); show "estimated" cost.

---

## I. Feature Highlights to Adopt

### I.1 OpenHands' BEST features (ranked for our Android agent)

1. **Autonomous ReAct loop with multi-step planning** — the core differentiator. Lets the agent work unattended.
2. **Event-stream architecture** — every action + observation persisted as an event. Perfect for replay, audit, UI streaming, and resume-after-kill on Android.
3. **Condensation as a first-class event** — clean context management. The condenser is pluggable; the LLM view is well-defined.
4. **LLM profile switching mid-task** (`SwitchLLMAction`) — use a cheap model for "ok, list files", switch to Opus for "now write the migration script". Huge cost win.
5. **Code execution sandbox (bash + IPython)** — the ability to *run* code, not just edit it. Enables "test it, then ship it" workflows.
6. **Sub-agent delegation** (delegate + child conversations) — decompose complex tasks; parallelize.
7. **Skills as markdown playbooks** — super easy to author, version, share. The `@openhands/extensions` catalog model is worth copying.
8. **MCP integration** — universal extensibility. Any MCP server adds tools.
9. **Confirmation mode with LLM-based risk analysis** — `NeverConfirm` / `AlwaysConfirm` / `ConfirmRisky` lets users dial in their risk tolerance.
10. **Persistent conversation state** (resume across restarts) — essential for Android where OS kills are routine.
11. **Resource caps** (`max_iterations`, `max_budget_per_task`) — prevents runaway cost when the user isn't watching.
12. **Agent state machine** (13 states) — clean lifecycle. Maps to Android Foreground Service states.
13. **Tool availability advertising** (`usable_tools` in `/api/server_info`) — runtime can gate tools based on what the sandbox supports. Useful for our Tier 1/2/3 capability ladder.
14. **Hooks** (`pre_tool_use`, `post_tool_use`, `stop`) — Claude Code-style hooks for custom logic injection. Useful for our plugin system.
15. **Runtime services info in system prompt** (`<RUNTIME_SERVICES>` block) — the agent knows what services are reachable without guessing. Clean pattern for telling the agent about Android-specific capabilities (e.g. "you can call `termux-sms-send`").

### I.2 What OpenHands has that Cline/Kilo/OpenCode don't

- True autonomous mode (no human-in-the-loop unless `confirmation_mode` is on)
- Code execution sandbox with **IPython/Jupyter** (not just bash)
- Multi-runtime backends (Docker/E2B/Daytona/Remote/Apptainer/Local) — pick your isolation level
- **Automation backend** for scheduled/webhook-driven agent runs (cron + GitHub/Slack/Linear/Notion event triggers)
- Multi-agent: `delegate` + `LaunchChildConversationAction`
- **ACP integration** (drive Claude Code/Codex/Gemini CLI as alternative agents)
- Skills marketplace (`@openhands/extensions`)
- Pre-built headless agent server (run on a server, not a laptop)
- **LLM profile switching mid-task**
- `max_budget_per_task` (USD cap)
- Condensation as a first-class event type (auditable)
- Task tracker (`view`/`plan`) + Planning file (`PLAN.md`) — explicit long-horizon planning primitives
- Tool availability advertising (runtime → agent)

### I.3 Is OpenHands' autonomous pattern better for a background Android agent?

**Yes, decisively.** The interactive Cline pattern assumes the user is watching; OpenHands is designed to run unattended. Specific reasons:

- Cline's loop blocks on user input between turns; OpenHands' loop self-drives until `finish`.
- Cline has no concept of "I'll come back later"; OpenHands persists every event and resumes.
- Cline has no budget caps; OpenHands has `max_iterations` + `max_budget_per_task`.
- Cline has no autonomous finish; OpenHands has `FinishAction` + `RejectAction`.
- Cline's UI is the primary surface; OpenHands' UI is optional (REST + WebSocket are primary).
- Cline can't decompose; OpenHands has `delegate` + child conversations.

For a phone you put in your pocket, OpenHands is the right shape.

---

## J. Key Question: Autonomous vs Interactive for Android

### J.1 For our Android agent (background, user uses phone normally), is OpenHands' autonomous pattern better than Cline's interactive pattern?

**Yes.** For a backgrounded agent, OpenHands' autonomous pattern is fundamentally the right fit:

| Concern | Cline (interactive) | OpenHands (autonomous) | Winner |
|---|---|---|---|
| User absent | Blocks on user input; task stalls | Self-drives until `finish` | OpenHands |
| OS kills process | Loses state | Resumes from persisted events | OpenHands |
| Cost control | None | `max_budget_per_task` | OpenHands |
| Long-horizon tasks | Sliding window only | TaskTracker + PLAN.md + condensation | OpenHands |
| Code execution | Host bash only | Sandboxed bash + IPython | OpenHands |
| Sub-tasks | Single thread | `delegate` + child conversations | OpenHands |
| UI required | Yes | No (headless REST) | OpenHands |
| Per-action approval | Always (or never) | Configurable (`NeverConfirm`/`AlwaysConfirm`/`ConfirmRisky`) | OpenHands |
| Resume after restart | Manual | Built-in | OpenHands |

### J.2 Can we combine both?

**Yes — and OpenHands' design already supports this duality natively.** The 13-state `AgentState` machine is the key:

```
LOADING → INIT → RUNNING ⇄ AWAITING_USER_INPUT ⇄ AWAITING_USER_CONFIRMATION
                  ↓                                    ↓
                  PAUSED                              USER_CONFIRMED/USER_REJECTED
                  ↓
              STOPPED / FINISHED / REJECTED / ERROR / RATE_LIMITED
```

**Proposed dual-mode Android agent**:

- **Background mode** (phone in pocket, screen off):
  - `confirmation_mode = NeverConfirm` (no prompts)
  - `max_iterations` tight (e.g. 50)
  - `max_budget_per_task` tight (e.g. $0.50)
  - Driven by Foreground Service
  - OS kills → resume from Room DB on next launch
  - Notifications on key events (task complete, error, budget exceeded, awaiting input)

- **Foreground mode** (user is watching):
  - `confirmation_mode = ConfirmRisky` (LLM risk analyzer) or `AlwaysConfirm` (paranoid)
  - `max_iterations` loose (e.g. 200)
  - `max_budget_per_task` loose (e.g. $5.00)
  - UI shows live event stream (token streaming + tool calls + observations)
  - User can `pause` (state → PAUSED), inject messages (state → AWAITING_USER_INPUT), confirm/reject risky actions (state → USER_CONFIRMED/USER_REJECTED), switch LLM profile manually, edit the task list

- **Mode transitions**:
  - App foregrounded → switch to foreground mode settings (looser caps, confirmation on)
  - App backgrounded → switch to background mode settings (tighter caps, no confirmation)
  - User long-presses notification → "pause agent" (state → PAUSED)
  - User taps notification → open app to foreground mode

This is **exactly the design OpenHands already implements** when run with the optional frontend connected — the agent-server runs autonomously; the UI is a view that can inject `pause` events and confirmation responses. We just translate the WebSocket to in-process Kotlin channels.

### J.3 Recommendation

Build a **dual-mode Android agent** with:
- OpenHands' autonomous ReAct loop + event-stream + condensation as the core
- A native Compose UI that mirrors Agent Canvas's conversation/terminal/files tabs
- A Foreground Service that runs the loop in background mode by default
- Mode switching on foreground/background transitions
- Tier 1 (SAF-only) tools at MVP; Tier 2 (Termux) and Tier 3 (cloud sandbox) as opt-in upgrades
- MCP for extensibility instead of Python plugins
- Skills as bundled markdown for common workflows

---

## K. Summary Table — what to take from OpenHands

| Take | What | Why |
|---|---|---|
| ✅ Take | ReAct loop + event-stream history | Core autonomous pattern; perfect for Android |
| ✅ Take | Condensation (LLM + recent-events) | Context management without losing state |
| ✅ Take | Agent state machine (13 states) | Clean lifecycle for foreground/background |
| ✅ Take | LLM profiles + SwitchLLMAction | Cost control via mid-task model switching |
| ✅ Take | StrReplaceEditor + Glob + Grep + TaskTracker + PlanningFile | The non-execution toolset — pure file IO |
| ✅ Take | MCP integration | Universal extensibility on Android |
| ✅ Take | Skills (markdown playbooks) | Easy to author, distribute, version |
| ✅ Take | max_iterations + max_budget_per_task | Runaway protection when user isn't watching |
| ✅ Take | Tool availability advertising | Gate tools by Tier (1/2/3) |
| ✅ Take | Hooks (pre/post tool, stop) | Plugin system |
| ✅ Take | Sub-agent delegation (delegate + child convs) | Decompose complex tasks |
| ✅ Take | Confirmation mode + LLM risk analyzer | Configurable safety |
| ✅ Take | Persistent conversation state | Resume after OS kill |
| ⚠️ Adapt | Browser tool | WebView + JS AX-tree instead of Playwright |
| ⚠️ Adapt | Sandbox runtime | SAF + Termux instead of Docker |
| ⚠️ Adapt | Automation backend | WorkManager + AlarmManager instead of cron+uvicorn |
| ❌ Drop | Docker / E2B / Daytona / Remote / Apptainer runtimes | Not on Android (except as optional cloud Tier 3) |
| ❌ Drop | Agent Canvas React web UI | Wrong platform; build native Compose UI |
| ❌ Drop | ACP agents (Claude Code/Codex/Gemini CLI subprocess) | No Node.js on Android |
| ❌ Drop | VSCode integration | No VSCode on Android |
| ❌ Drop | Python runtime in-process | Use Termux out-of-process or skip |
| ❌ Drop | PostHog telemetry (default off) | Privacy concern on mobile |

---

## L. References

- This repo: `references/openhands/` (TypeScript frontend / Agent Canvas)
  - `README.md` — overview, quickstart (3 install paths: no-sandbox, Docker, source)
  - `docs/architecture.md` — system boundaries, runtime services, frontend modules
  - `docs/SELF_HOSTING.md` — systemd deployment, security hardening
  - `docs/ACP_AGENTS.md` — ACP agent integration (Claude Code/Codex/Gemini CLI)
  - `LICENSE` — MIT
  - `package.json` — `@openhands/agent-canvas` v1.13.0, MIT, Node ≥ 22.12
  - `config/defaults.json` — version pins (agentServer 1.42.1, agentCanvas 1.13.0, automation 1.7.1; ports 18000/18001/8000)
  - `docker/Dockerfile` + `docker/entrypoint.sh` — all-in-one image: Agent Server + Automation + Frontend behind ingress
  - `src/types/agent-server/core/base/action.ts` — **the full tool catalog** (24 action types)
  - `src/types/agent-server/core/base/observation.ts` — observation shapes for each tool
  - `src/types/agent-server/core/openhands-event.ts` — event union (16 event types)
  - `src/types/agent-server/core/events/condensation-event.ts` — condensation events
  - `src/types/agent-state.tsx` — 13-state agent state machine
  - `src/types/action-type.tsx` — ActionType enum
  - `src/types/settings.ts` — Settings shape (LLM, providers, confirmation_mode, security_analyzer, max_iterations, max_budget_per_task, condenser, agent_kind)
  - `src/api/agent-server-adapter.ts` — tool registration logic (`DEFAULT_TOOL_NAMES`, `getAgentTools`, confirmation policy, security analyzer selection, skills packaging, runtime services suffix)
  - `src/api/agent-server-compatibility.ts` — `usable_tools` advertising, version compatibility checks
  - `src/api/agent-server-home.ts` — `Path.home()` resolution + absolute path handling
  - `src/api/bash-service/bash-service.api.ts` — bash event pagination
  - `src/api/automation-service/automation-service.api.ts` — Automation backend client
  - `src/api/conversation-service/agent-server-conversation-service.types.ts` — conversation lifecycle, AppConversationStartTask, metrics
  - `src/types/automation.ts` — Automation/AutomationRun/AutomationTrigger types
  - `AGENTS.md` — repo notes (frontend-only, SDK is separate)

- External (referenced from this repo):
  - `software-agent-sdk` repo: https://github.com/OpenHands/software-agent-sdk (Python agent runtime, agent-server, SDK, tools, workspace — NOT cloned here)
  - `automation` repo: https://github.com/OpenHands/automation (Python automation backend)
  - `@openhands/typescript-client` npm package (1.38.0) — typed REST/WS client
  - `@openhands/extensions` npm package (0.16.0) — Skills catalog
  - OpenHands docs: https://docs.openhands.dev/openhands/usage/sandboxes/overview — sandbox overview
  - OpenHands docs: https://docs.openhands.dev/openhands/usage/v0/runtimes/V0_overview — runtime overview (Docker, E2B, Daytona, Remote, Apptainer, Local)
  - OpenHands docs: https://docs.openhands.dev/sdk/guides/agent-server/apptainer-sandbox — Apptainer sandbox
  - arXiv 2511.03690 — "The OpenHands Software Agent SDK: A Composable and ..." (Nov 2025) — peer-reviewed architecture paper
  - Daytona blog: https://www.daytona.io/dotfiles/introducing-runtime-for-openhands-secure-ai-code-execution
  - Agent Client Protocol: https://agentclientprotocol.com/protocol/overview
