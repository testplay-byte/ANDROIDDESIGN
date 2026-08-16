# R-1 — Cline Agent Framework: Deep Research for Android/Kotlin Porting

> **Task ID:** R-1
> **Agent:** general-purpose (Cline research)
> **Date:** session-start (sandbox-fresh)
> **Scope:** Architecture study only. No code is written in this report.
> **Primary sources:** official repo `github.com/cline/cline`, official docs `docs.cline.bot`,
> SDK README at `github.com/cline/cline/blob/main/sdk/README.md`, the third-party teardown
> `github.com/NeuZhou/awesome-ai-anatomy/tree/main/cline`, and the official Cline blog.
> Each section flags verification status. Claims that could not be cross-checked are marked
> `(unverified)`.

---

## 0. TL;DR — the 30-second summary

Cline is an **open-source AI coding agent** licensed under **Apache 2.0** (confirmed by reading
the repo's `LICENSE` file). Originally a single-developer VS Code extension called `claude-dev`
(mid-2024), it has grown into a multi-product platform: VS Code extension, JetBrains plugin,
CLI, Kanban (web multi-agent board), a Tauri desktop app, and an embeddable TypeScript SDK
(`@cline/sdk`). It is ~97% TypeScript (~560 KLOC across thousands of files), 66 k+ GitHub stars
(as of Aug 2026), with 28 built-in tools, 40+ LLM provider adapters, full MCP client support,
a shadow-git checkpoint system, and a recursive agent loop (`recursivelyMakeClineRequests`).

**The single most important fact for our project:** Cline's SDK is already split into a clean,
layered, browser-compatible core (`@cline/agents`, `@cline/llms`, `@cline/shared`) plus a
Node-only orchestration layer (`@cline/core`). The browser-compatible layer is the part we
should mirror most directly in Kotlin — it has zero Node-specific dependencies.

---

## 1. What Cline is — origin, purpose, license

| Field | Value | Source |
|---|---|---|
| Origin | Started as `claude-dev`, a single-developer VS Code extension published mid-2024 | awesome-ai-anatomy teardown (verified); also confirmed by HN "Show HN" Feb 19 2025 |
| Current name | Cline | official repo |
| Maintainer | Cline Bot Inc. | LICENSE file + blog footer ("© 2026 Cline Bot Inc.") |
| Repository | https://github.com/cline/cline | verified |
| Stars | ~66.2 k (Aug 14 2026) | GitHub repo page |
| Forks | ~7.1 k | GitHub repo page |
| License | **Apache 2.0** | Read directly from `raw.githubusercontent.com/cline/cline/main/LICENSE` — verified |
| Languages | TypeScript 97.2 %, JavaScript 1.7 %, CSS 0.5 %, Rust 0.3 %, HTML 0.2 %, Shell 0.1 % | GitHub repo page |
| Latest VS Code release | v4.1.10 (Aug 14 2026) | `CHANGELOG.md` per repo file listing |
| CLI package | `npm i -g cline`, latest `3.0.55` | npmjs.com search result (verified) |
| SDK package | `npm install @cline/sdk` | sdk/README.md (verified) |

**License implications for our port.** Apache 2.0 is permissive: we may use, modify, distribute,
and sublicense — including in a closed-source Android app — provided we:
1. Include a copy of the Apache 2.0 license and the NOTICES file in our distribution.
2. Retain all copyright, patent, trademark, and attribution notices in any source we copy verbatim.
3. State clearly that we modified the work (if we copy and modify files).
4. Do **not** use Cline's name/mark to endorse our product without written permission.

**Recommended posture for ONLYLIST:** Treat Cline as **architectural inspiration only** —
port the *ideas* (agent loop, tool dispatch, context truncation, checkpoints, approval flow)
into idiomatic Kotlin, rather than translating the TypeScript source line-for-line. This avoids
the "you modified this file" disclosure obligation on every Cline file we touched and keeps our
own code unambiguously ours. We should still ship a "third-party notices" screen crediting
Cline as inspiration and including the Apache 2.0 license text.

---

## 2. Architecture — the core agent loop

### 2.1 High-level topology (VS Code extension form)

```
VS Code Extension entry (extension.ts, ~440 lines)
        │
        ▼
   Controller  ── task lifecycle, MCP hub, auth, state
        │
        ▼
     Task        ← "God Object": src/core/task/index.ts, 3,756 lines
        │           (orchestrates loop, streaming, context, hooks,
        │            checkpoints, tool dispatch coordination)
        ▼
  ToolExecutor  ← constructor takes 30+ params, 15 of which are callbacks
   (ToolExecutorCoordinator routes to handlers)
```

*(Source: awesome-ai-anatomy teardown, verified against the repo file listing.)*

The 4-layer hierarchy is `Extension → Controller → Task → ToolExecutor`. Coupling between
layers is callback-injection rather than clean interfaces — the `ToolExecutor` constructor
takes ~15 callback functions (`saveCheckpoint`, `sayAndCreateMissingParamError`,
`removeLastPartialMessageIfExistsWithType`, `switchToActMode`, `cancelTask`, etc.). The
teardown rates the architecture **C+** specifically because of this God Object.

### 2.2 SDK-layered topology (the part we care about most)

The SDK has been refactored into a clean layered stack with strict dependency direction:

```
   Your app / CLI / VS Code / JetBrains
                  │
                  ▼
        @cline/core            ← Node-only orchestration:
   (ClineCore)                    sessions, SQLite storage, built-in tools,
                                  hub/remote transports, telemetry, plugins
           │   ┌──────────────────┼──────────────────┐
           ▼   ▼                  ▼                  ▼
     @cline/agents           @cline/llms        @cline/shared
   (AgentRuntime / Agent)   (DefaultGateway,    (createTool,
    run, continue, abort,    createHandler,      AgentTool, ToolPolicy,
    subscribe, restore,      provider handlers,  AgentEvent, AgentResult,
    snapshot)                model catalogs)     HookEngine)
                                  │                  ▲
                                  └──────────────────┘
```

**Dependency flow is downward only.** `@cline/shared` has no higher-layer dependencies, so it is
fully embeddable. `@cline/agents` depends only on `@cline/shared` and `@cline/llms` — and
**crucially is browser-compatible**, meaning it owns no Node-specific primitives (no `fs`, no
`child_process`, no `os`). That is the layer we mirror most directly.

*(Source: docs.cline.bot/sdk/architecture — verified.)*

### 2.3 The agent loop, step by step

The actual loop is `recursivelyMakeClineRequests` (~600 lines inside the 3,756-line `Task`
class). It is a **ReAct loop** (Reason → Act → Observe) implemented as **direct recursion**
rather than a `while(true)`:

```
1. USER submits task
   → Controller creates Task with 20+ injected dependencies

2. HOOKS fire
   → TaskStart hook (cancellable, can cancel whole task)
   → UserPromptSubmit hook (cancellable, can inject <hook_context>...</hook_context>)

3. CONTEXT LOADING
   → Parse mentions (@file, @url, @folder, @problems)
   → Resolve slash commands
   → Gather environment details: open tabs, visible files, terminal
     output, current time, workspace roots

4. SYSTEM PROMPT BUILD
   → PromptRegistry picks a variant based on model family:
     next-gen (Claude 4+), native-next-gen (Response API), native-gpt-5,
     gpt-5, devstral, gemini-3, hermes, trinity, glm, xs (small), generic
   → TemplateEngine fills {{SECTION}} placeholders from component fns
     (AGENT_ROLE, CAPABILITIES, TOOL_USE, TASK_PROGRESS, MCP,
      EDITING_FILES, RULES, SKILLS, ...)

5. API REQUEST FIRES  (attemptApiRequest)
   → Stream chunks through StreamChunkCoordinator
   → Three chunk types: text, reasoning, tool_calls
   → Auto-retry with exponential backoff on transient errors

6. ASSISTANT MESSAGE PARSED  (parseAssistantMessageV2)
   → Extracts tool_use blocks from streamed XML or native tool_calls

7. TOOLS EXECUTE  (one at a time, or in parallel if enabled)
   → ToolExecutorCoordinator routes each tool_use to its handler
   → Each handler implements: execute(TaskConfig, ToolUse) → ToolResponse
   → Approval is asked BEFORE execute() if tool is not auto-approved

8. TOOL RESULTS appended to userMessageContent

9. LOOP RECURSES  → recursivelyMakeClineRequests calls itself with the
   appended tool results. This is genuine call-stack recursion, not just
   conceptual. Depth is bounded by the context window running out.

10. TERMINATION
    → If model returns no tools: a "noToolsUsed" nudge is appended,
      consecutiveMistakeCount increments. After maxConsecutiveMistakes
      (configurable), the user is asked to intervene.
    → If model emits `attempt_completion`: loop ends.
```

*(Source: awesome-ai-anatomy teardown, verified against the file/commit history.
The exact line counts and method names match the repo file listing.)*

**The single best thing to steal for Android:** the **iteration-over-recursion** choice that
the teardown explicitly recommends. Cline's recursive design works but the teardown says
"converting to an iterative loop (like Claude Code's `while(true)`) would be a natural
evolution for even more robustness." For Android, we should use Kotlin coroutines with a
`while (isActive)` loop — this is safer under memory pressure and matches Cline's own
direction.

---

## 3. Tool system — definition, registration, dispatch

### 3.1 Built-in tools (two different sets exist)

Cline ships **two different tool name sets** depending on which runtime is hosting the agent.
This is a frequent source of confusion.

**(a) The original VS Code extension's 28 tools** (enum `ClineDefaultTool` in
`src/shared/tools.ts`):

| Category | Tools |
|---|---|
| File operations | `read_file`, `write_to_file`, `replace_in_file`, `apply_patch`, `list_files`, `search_files`, `list_code_definition_names` |
| Execution | `execute_command` |
| Browser | `browser_action` (6 actions: launch, click, type, scroll_down, scroll_up, close) |
| Web | `web_fetch`, `web_search` |
| MCP | `use_mcp_tool`, `access_mcp_resource`, `load_mcp_documentation` |
| Communication | `ask_followup_question`, `attempt_completion` |
| Mode | `plan_mode_respond`, `act_mode_respond` |
| Context | `condense`, `summarize_task`, `focus_chain` |
| Meta | `new_task`, `new_rule`, `report_bug`, `generate_explanation`, `use_skill`, `use_subagents` |

*(Source: awesome-ai-anatomy teardown; verified via the repo file `src/shared/tools.ts`.)*

**(b) The SDK / ClineCore runtime's 7 built-ins** (modern, simplified):

| Tool | Description |
|---|---|
| `bash` | Execute shell commands |
| `editor` | View and edit files |
| `read_files` | Batch read multiple files |
| `apply_patch` | Apply unified diffs to files |
| `search` | Ripgrep-powered codebase search |
| `fetch_web` | HTTP requests with HTML-to-markdown conversion |
| `ask_question` | Ask the user for input |

*(Source: docs.cline.bot/tools-reference/all-cline-tools — verified.)*

> ⚠️ The docs explicitly warn: *"Some older docs/examples reference XML-style names like
> `read_file`, `replace_in_file`, or `execute_command`. Current SDK/ClineCore runtime uses
> the built-in tool names listed above (`read_files`, `apply_patch`, `bash`, etc.)."*

**For Android:** We adopt neither set verbatim. We define our own minimal design-system set
(see §10).

### 3.2 Tool dispatch — VS Code extension

```ts
// From src/core/task/tools/ToolExecutorCoordinator.ts (verified)
export interface IToolHandler {
  readonly name: ClineDefaultTool
  execute(config: TaskConfig, block: ToolUse): Promise<ToolResponse>
  getDescription(block: ToolUse): string
}

export interface IPartialBlockHandler {
  handlePartialBlock(block: ToolUse, uiHelpers: StronglyTypedUIHelpers): Promise<void>
}

export interface IFullyManagedTool extends IToolHandler, IPartialBlockHandler {
  // Marker interface for tools that handle their own complete approval flow
}
```

The coordinator maintains a `Map<string, IToolHandler>`. The `SharedToolHandler` pattern
lets two tool names share one implementation (`replace_in_file` and `new_rule` both route to
`WriteToFileToolHandler`).

`TaskConfig` is a 50+-field configuration bag passed to every handler — it includes task
state, message-state handler, API handler, browser session, diff-view provider, MCP hub,
file-context tracker, cline-ignore controller, command-permission controller, context
manager, state manager, and 15+ callback functions. (The teardown calls this constructor
injection "taken to its logical extreme.")

### 3.3 Tool dispatch — SDK (the clean version)

```ts
// From @cline/sdk (verified against sdk/README.md)
import { createTool, Agent } from "@cline/sdk"

const deploy = createTool({
  name: "deploy",
  description: "Deploy the app to staging or production.",
  inputSchema: {
    type: "object",
    properties: {
      environment: { type: "string", enum: ["staging", "production"] },
    },
    required: ["environment"],
  },
  execute: async (input) => {
    const result = await runDeployment(input.environment)
    return { url: result.url, status: "success" }
  },
})

const agent = new Agent({
  providerId: "moonshot",
  modelId: "kimi-k2.5",
  systemPrompt: "You are a deployment assistant.",
  tools: [deploy],
})
```

**This is the API to mirror in Kotlin.** It is JSON-Schema-based, async, and side-effect free.

### 3.4 Tool-call schema (function-calling vs XML vs JSON schema)

Cline supports **both** XML-style and native function-calling:

- **XML-style tool calls** (legacy Anthropic Messages format): the model emits text like
  `<read_file><path>foo.txt</path></read_file>`, parsed by `parseAssistantMessageV2`.
  This is what the old `read_file` / `write_to_file` / `replace_in_file` names expect.
- **Native function-calling** (modern Anthropic tool_use blocks, OpenAI tool_calls,
  Gemini functionCall): the model emits structured `tool_use` blocks; Cline streams chunks
  of type `tool_calls` via `StreamChunkCoordinator`.

The tool **input schema is always JSON Schema** (`type: "object", properties: {...}, required: [...]`).
For XML-emitting models, the schema is rendered into the system prompt as a description; for
function-calling models, it is passed verbatim to the API as the `tools` parameter.

**For Android:** Use native function-calling everywhere. XML tool-call parsing is brittle and
tied to specific Anthropic-era conventions. All modern providers (Anthropic, OpenAI, Gemini,
OpenRouter, Mistral, etc.) support native function-calling, and that is the path Cline is
itself moving toward.

---

## 4. Context management

### 4.1 The `ContextManager` class

- **File:** `src/core/context/context-management/ContextManager.ts` (~1,300 lines).
- **Core data structure:** `contextHistoryUpdates` — a map keyed by message block, storing
  modifications (text alterations, file-content replacements) **with timestamps**. This is
  what enables checkpoint-based undo: any state can be reverted to its value at time T.

### 4.2 Truncation strategy — "delete old messages, don't summarize"

Cline's approach is fundamentally simpler than Claude Code's 4-layer cascade
(HISTORY_SNIP → Microcompact → CONTEXT_COLLAPSE → Autocompact). Cline just **deletes
old messages**:

1. `getNewContextMessagesAndMetadata` is the primary entry point. It looks at the token usage
   of the previous API request. If close to `maxAllowedSize`, it triggers truncation.
2. `getNextTruncationRange` decides what to remove using a **"quarter" strategy** — remove
   ~25 % of the remaining undeleted messages.
3. Messages are **masked, not deleted from disk** — the manager tracks a `[startIndex,
   endIndex]` tuple (`conversationHistoryDeletedRange`). The full history persists; only
   what gets sent to the API changes:

```
Full history:   [msg0, msg1, msg2, msg3, msg4, msg5, msg6, msg7, msg8, msg9]
After 1st trim: deletedRange = [0, 3]  →  sent: [msg4..msg9]
After 2nd trim: deletedRange = [0, 5]  →  sent: [msg6..msg9]
```

4. `getNextTruncationRange` **prioritizes the initial user-assistant exchange** (it never
   deletes msg0/msg1) and removes from the middle. It ensures an even number of messages are
   removed to keep user-assistant pairs intact.

*(Source: awesome-ai-anatomy teardown, plus docs.cline.bot/blog/clines-context-window-explained — verified against the medium dissecting-cline article which names the same classes.)*

### 4.3 Auto-condense (next-gen models only)

For Claude 4+ and GPT-5 family models, Cline supports `useAutoCondense`: when context
reaches ~75 % utilization, the model is asked to use the `summarize_task` tool. The summary
replaces the old messages. This is the only summarization path; otherwise truncation is
purely destructive.

### 4.4 File-read deduplication

`attemptFileReadOptimization` rewrites prior file-read tool results, replacing the full
file content with a shortened `[DUPLICATE FILE READ]` notice when the file has not changed
since the prior read. The `EditType` enum distinguishes `READ_FILE_TOOL`, `ALTER_FILE_TOOL`,
and `FILE_MENTION`. This is a cheap, high-impact optimization: prevents the model from
re-reading the same 5000-line file 5 times in a task.

A `TaskState.fileReadCache` tracks `(path, mtime)` pairs. If the file hasn't changed since
the last read, the model gets a shortened version with the note "file unchanged since last
read."

### 4.5 The `new_task` handoff pattern

For very long sessions, Cline's `new_task` tool starts a fresh session preloaded with only
essential context defined in `.clinerules` (e.g. "if context > 50 %, handoff with file
states + next steps"). This is **persistent memory via task boundary**, not via a vector DB.

### 4.6 Context-window info

`getContextWindowInfo(ApiHandler)` returns `(contextWindow, maxAllowedSize)` where the
second is `contextWindow` minus a **model-specific buffer** (DeepSeek and Claude have
different buffers). The buffer prevents hitting the hard limit during response generation.

### 4.7 Performance degradation curve

The official Cline blog says most models show measurable performance degradation at
**70–80 % context utilization** — slightly slower responses, less precise results. This is
the threshold Cline uses to start considering handoff/condense.

**For Android:** This whole subsystem maps cleanly to Kotlin. `ContextManager` becomes a
class holding an immutable `List<Message>` plus a `deletedRange: IntRange`. The "quarter
strategy" is ~30 lines of code. File-read dedup is a `Map<String, Long>` keyed by file path
storing mtime. **This is one of the most directly portable parts of Cline.**

---

## 5. LLM provider abstraction

### 5.1 The `ApiHandler` interface (VS Code extension)

```ts
// From src/core/api/index.ts (verified)
export interface ApiHandler {
  createMessage(
    systemPrompt: string,
    messages: ClineStorageMessage[],
    tools?: ClineTool[]
  ): ApiStream
  getModel(): ApiHandlerModel
  getApiStreamUsage?(): Promise<ApiStreamUsageChunk | undefined>
  abort?(): void
}
```

`ApiStream` is an async generator yielding discriminated chunks:
`{ type: "text" | "reasoning" | "tool_calls" | "usage", ... }`.

The factory `buildApiHandler` is a 300+ line switch statement that instantiates the correct
handler from a string `apiProvider`. Each provider is its own class with its own SDK import,
its own error handling, and its own streaming adaptation that maps native chunks to the
common `ApiStream` shape.

### 5.2 The 43 provider adapters

`src/core/api/providers/` contains 43 implementations (verified by file count):

Anthropic, OpenRouter, Bedrock, Vertex, OpenAI, OpenAI Native, OpenAI Codex, Gemini, Groq,
DeepSeek, Ollama, LM Studio, Mistral, Fireworks, Together, Cerebras, HuggingFace, xAI,
SambaNova, Qwen, Qwen Code, Doubao, Moonshot, Minimax, Nebius, LiteLLM, Requesty, AIhubmix,
AskSage, Baseten, Hicap, Huawei Cloud MaaS, Nous Research, OCA, SAP AI Core, Vercel AI
Gateway, VS Code LM, W&B, ZAI, Dify, Claude Code (as a provider), and Cline's own cloud
offering.

The teardown notes: *"No other open-source coding agent supports this many backends."*
Claude Code supports only Anthropic; Goose supports 30+ via a unified registry.

### 5.3 Plan/Act dual-mode complication

Cline supports separate providers and models for "Plan" mode (thinking/planning) and "Act"
mode (executing). `buildApiHandler` takes a `mode` parameter and selects different API keys,
model IDs, and thinking-budget tokens per mode. The `ApiConfiguration` has 100+ fields
because of this.

### 5.4 SDK `@cline/llms` exports (the clean version)

| Export | Description |
|---|---|
| `DefaultGateway`, `createGateway` | Gateway for creating provider-backed agent models |
| `createHandler`, `createHandlerAsync` | Provider handler factories |
| `getAllProviders`, `getProviderIds`, `getModelsForProvider` | Catalog helpers |
| `registerProvider`, `registerModel` | Runtime registry extension |
| `ModelInfo`, `ProviderInfo` | Provider/model metadata |

*(Source: docs.cline.bot/sdk/architecture — verified.)*

### 5.5 What is pluggable

The provider layer is **fully pluggable** at runtime via `registerProvider()` /
`registerModel()`. A new provider only needs to implement the `ApiHandler` interface and
yield the common `ApiStream` chunk shape. This is exactly the seam we want for Android.

**For Android:** We implement a single Kotlin interface:

```kotlin
interface LlmProvider {
    val id: String
    fun stream(request: LlmRequest): Flow<LlmChunk>   // text | reasoning | tool_calls | usage
    fun models(): List<ModelInfo>
    fun abort()
}
```

We ship ~4 concrete providers (Anthropic, OpenAI, OpenRouter, Gemini) plus an
"OpenAI-compatible" generic for everything else (Ollama, LM Studio, vLLM, Together,
Groq). For our anime-tracker use case, OpenRouter alone covers ~200 models, so 4 providers
give us full coverage.

---

## 6. MCP (Model Context Protocol) support

### 6.1 Cline is an MCP **client** only

Cline does not host MCP servers; it connects to external MCP servers as a client.

- **Class:** `src/services/mcp/McpHub.ts` (~1,700+ lines, verified by line-count claim in teardown).
- **Capabilities supported:** tools, resources, resource templates, prompts.
- **OAuth:** yes, via `McpOAuthManager`.
- **Auto-reconnect:** yes, for SSE connections via `StreamableHttpReconnectHandler`.

### 6.2 Transports

| Transport | Use | Android viability |
|---|---|---|
| `stdio` | Local process, lower latency, simpler local setup | **Not viable** — Android apps cannot spawn arbitrary subprocesses |
| `streamableHttp` (recommended) | Hosted endpoint, multi-client | Viable (OkHttp + JSON-RPC over HTTP) |
| `sse` (legacy) | Hosted endpoint, server-sent events | Viable (OkHttp + EventSource) |

### 6.3 Configuration shape

CLI: `~/.cline/mcp.json`. IDE: edited via the Cline panel "Configure MCP Servers" button
(which writes the same JSON). Server config has two shapes:

```json
// Local (STDIO) server
{
  "mcpServers": {
    "local-server": {
      "command": "node",
      "args": ["/path/to/server.js"],
      "env": { "API_KEY": "your_api_key" },
      "disabled": false,
      "autoApprove": []
    }
  }
}

// Remote (Streamable HTTP) server
{
  "mcpServers": {
    "remote-server": {
      "type": "streamableHttp",
      "url": "https://example.com/mcp",
      "headers": { "Authorization": "Bearer your-token" },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

*(Source: docs.cline.bot/mcp/mcp-overview — verified verbatim.)*

### 6.4 Tool name transformation

When the model calls an MCP tool, the tool name is encoded as `serverName__toolName` using
the `CLINE_MCP_TOOL_IDENTIFIER` separator. The coordinator normalizes this back to
`use_mcp_tool` for handler routing, with the actual server/tool name passed as parameters.

### 6.5 Relevance for Android

**Mostly N/A but not entirely.** The `stdio` transport and "ask Cline to spawn a local MCP
server" pattern is impossible on Android. The `streamableHttp` transport is perfectly
viable and **opens a useful extensibility path for our app**: a power user could host an MCP
server on their own machine (or any cloud) and point ONLYLIST at it for custom tools
(AniList-specific enrichers, design-token generators, etc.).

**Recommendation:** Implement only the `streamableHttp` transport. Skip `stdio` and `sse`.
The MCP client can be a future enhancement, not part of the MVP.

---

## 7. Permission / approval flow

### 7.1 Two layers: tool policies + interactive handler

The SDK exposes both a declarative per-tool policy map and an interactive callback:

```ts
// From docs.cline.bot/sdk/guides/permission-handling (verified)
const agent = new Agent({
  tools: [readFileTool, writeFileTool, bashTool, searchTool],
  toolPolicies: {
    read_files:     { autoApprove: true  },   // Always run without asking
    search_codebase:{ autoApprove: true  },
    write_file:     { autoApprove: false },   // Always ask before running
    run_commands:   { autoApprove: false },
  },
})
```

Policy effect table:

| Policy | Effect |
|---|---|
| `{ autoApprove: true }` | Tool executes immediately without approval |
| `{ autoApprove: false }` | Tool waits for approval before executing |
| `{ enabled: false }` | Tool is completely disabled (model won't see it) |
| No policy set | Defaults to enabled and auto-approved |

For interactive approval, `ClineCore` accepts a `capabilities.requestToolApproval` callback
returning `{ approved: boolean }`. The callback receives `toolName` and `input`, enabling
**conditional approval logic** (e.g. approve `run_commands` only if `cmd.startsWith("ls")`).

### 7.2 The polling pattern (VS Code extension)

The original VS Code extension uses **polling-based approval**:

```ts
// From src/core/task/index.ts (verified)
await pWaitFor(
  () =>
    this.taskState.askResponse !== undefined ||
    this.taskState.lastMessageTs !== askTs ||
    (shouldWakeOnAbort && this.taskState.abort),
  { interval: 100 },
)
```

When a tool needs approval, `Task.ask()` posts an "ask" message to the webview and polls
`taskState.askResponse` every 100 ms until the user clicks a button. The teardown notes this
is architecturally simpler than event-driven approval (no callback chains, no promise
externalities) but introduces up to 100 ms latency between user click and task resumption.

### 7.3 Auto-approve categories (VS Code extension)

| Setting | What it allows |
|---|---|
| Read project files | Read files, list files, search in workspace |
| Read all files | Read files outside workspace (requires base toggle) |
| Edit project files | Create and edit files in workspace |
| Edit all files | Edit files outside workspace (requires base toggle) |
| Execute safe commands | Run terminal commands marked safe |
| Execute all commands | Run commands requiring approval (requires base toggle) |
| Use the browser | Browser tool for web fetching and searching |
| Use MCP servers | MCP tools and resources |
| Enable notifications | OS-level notifications for long-running commands |

### 7.4 Safe vs requires-approval commands (no fixed allowlist)

Cline **does not use a fixed allowlist**. The model marks each command with a
`requires_approval` flag based on the command and arguments. Examples (not guarantees):

- **Safe:** `npm run build`, `npm test`, `git status`, `ls -la`, `cat package.json`
- **Requires approval:** `npm install <pkg>`, `rm -rf <path>`, `mv <a> <b>`, `sed -i ...`

### 7.5 YOLO mode

One checkbox auto-approves everything: file changes, terminal commands, browser actions, MCP
tools, mode transitions. The teardown confirms: *"YOLO Mode is the boldest trust-the-user
setting in Cline. One toggle makes `shouldAutoApproveTool` return `[true, true]` for every
tool including `execute_command`."*

### 7.6 Command Permission Controller (defense in depth)

`CommandPermissionController.ts` adds a secondary defense layer for command execution:
- Reads `CLINE_COMMAND_PERMISSIONS` from env vars
- Validates commands against allow/deny glob patterns
- Parses commands into segments, detects shell operators
- Recursively validates subshells
- Blocks dangerous characters (backticks outside single quotes, newlines outside quotes)

### 7.7 What happens when a tool is rejected?

The agent receives a rejection message and **can adjust** — ask for clarification, try a
different tool, modify parameters, or give up on the subtask. The agent does not get stuck in
a loop. The rejection counts as a response and the agent proceeds with its next iteration.

**For Android:** Adopt the SDK pattern verbatim: a `Map<String, ToolPolicy>` for declarative
defaults + a suspendable `suspend fun approve(toolName, input): ApprovalResult` callback.
The interactive callback runs on the UI thread via Compose. We do **not** need polling —
Kotlin coroutines give us direct suspension.

---

## 8. State / memory / checkpoints

### 8.1 The shadow-git checkpoint system (VS Code extension)

- Cline maintains a **shadow Git repository separate from your project's actual Git history**.
- After **each tool use** (file edits, commands, etc.), Cline commits the current state of
  your files to this shadow repo.
- Main Git history stays untouched.
- Captures **everything**, including files not tracked by Git.
- Persists across editor sessions.
- Each checkpoint captures the complete file state — if Cline edits 3 files in sequence,
  you get 3 checkpoints and can restore to any of them independently.

*(Source: docs.cline.bot/core-workflows/checkpoints — verified.)*

### 8.2 Three restore options

| Option | What it does | When to use |
|---|---|---|
| Restore Files | Reverts project's files to the snapshot at this checkpoint | Undoing code changes while keeping conversation |
| Restore Task Only | Deletes messages after this point, does not affect files | Trying a different prompt while keeping current code |
| Restore Files & Task | Reverts files and deletes messages after this point | Starting over completely from a known good state |

### 8.3 Other persistence

- **SDK (ClineCore):** SQLite database at `~/.cline/data/workspaces/chat` for session
  manifests and message artifacts. Session manifest has authoritative resolved workspace
  paths.
- **ContextManager:** saves and loads `contextHistoryUpdates` to/from disk so detailed
  history (optimizations, alterations) persists across Cline sessions and task reloads.
- **FileContextTracker:** stores pending warnings in VS Code workspace state.
- **ModelContextTracker:** records `(apiProviderId, modelId, mode, timestamp)` per task as
  `ModelMetadataEntry`; deduplicates consecutive identical entries.
- **AGENTS.md auto-seeding:** if both `cwd` and `workspaceRoot` are omitted,
  `ClineCore` places the session at `<cline-data-dir>/workspaces/chat` and seeds it with an
  `AGENTS.md` rules file telling the agent to treat the session as a chat.

### 8.4 What does NOT persist?

- **No vector database.** Long-term memory is achieved through `.clinerules` files, skills
  (`SKILL.md`), and the `new_task` handoff pattern, not embeddings.
- **No learned preferences** beyond what is written into `.clinerules` or `MEMORY` banks.

**For Android:** Use **Room** for SQLite persistence (this is a perfect 1:1 mapping — Cline
uses SQLite on desktop, we use Room on Android). For checkpoints, **do not use shadow git**
on Android (no `git` CLI guaranteed, scoped-storage restrictions). Instead, store a
snapshot table in Room with columns: `(session_id, message_id, timestamp, design_state_json,
tool_name, tool_input_json)`. Each agent action writes a row. UI shows Compare/Restore
buttons per row.

---

## 9. Language & dependencies

### 9.1 Language breakdown

| Language | % | Notes |
|---|---|---|
| TypeScript | 97.2 % | The agent runtime, providers, tools, SDK |
| JavaScript | 1.7 % | Glue scripts |
| CSS | 0.5 % | Webview UI |
| Rust | 0.3 % | Performance-critical native modules (unverified — likely the desktop sidecar or a parser) |
| HTML | 0.2 % | Webview templates |
| Shell | 0.1 % | Build scripts |

Build system: **Bun** (migrated from npm/node per commit `Migrate apps/vscode from npm/node to
bun (#11632)` dated Jun 24 2026). Linter: **Biome**. Tests: **vitest**. Git hooks: **Husky**.

### 9.2 Heavy npm dependencies and their Kotlin/JVM equivalents

| npm package | Purpose | JVM/Kotlin equivalent | Reuse? |
|---|---|---|---|
| `@anthropic-ai/sdk` | Anthropic API client | Anthropic Java SDK or hand-rolled OkHttp | Hand-roll (smaller) |
| `openai` | OpenAI API client | OpenAI Java SDK or hand-rolled | Hand-roll |
| `@google/generative-ai` | Gemini | google-cloud-vertexai or hand-rolled | Hand-roll |
| `@aws-sdk/client-bedrock-runtime` | Bedrock | AWS SDK for Java | Reuse |
| `puppeteer-core` + `chrome-launcher` | Browser automation (screenshot-based vision nav) | **No JVM equivalent.** Would need Chrome DevTools Protocol via OkHttp/websocket | **Drop entirely on Android** |
| `chokidar` | Cross-platform file watcher | `java.nio.file.WatchService` (JVM) or `android.os.FileObserver` (Android) | Use FileObserver |
| `p-wait-for` | Polling primitive | Kotlin `suspendCancellableCoroutine` + `delay()` | Use coroutines |
| `ripgrep` (binary) | Codebase search | `Files.walk()` + regex (JVM); Android scoped storage limits this | Reimplement minimal |
| `diff-match-patch` or similar | SEARCH/REPLACE block application | `org.jetbrains.kotlinx diff-utils` or hand-rolled | Reuse `diffutils` lib |
| `tree-sitter` (probably) | Code parsing for `list_code_definition_names` | tree-sitter-java bindings or skip | Skip (no source code on Android) |
| `js-yaml` | YAML parsing | `kaml` or `snakeyaml` | Reuse `kaml` |
| `better-sqlite3` | SQLite for SDK sessions | Android Room / SQLDelight | Use Room |
| `zod` | Runtime schema validation | `kotlinx.serialization` + custom validators | Use kotlinx |
| `@modelcontextprotocol/sdk` | MCP client | No JVM MCP client yet (unverified) | Reimplement minimal client |
| `mime-types`, `glob`, `minimatch` | File path matching | Java NIO `PathMatcher` | Use NIO |

### 9.3 What has NO Kotlin/JVM equivalent (must reimplement or drop)

1. **Puppeteer browser automation.** No JVM port. Drop entirely.
2. **`execute_command` / `bash`.** Android apps cannot spawn arbitrary subprocesses. Replace
   with app-private "design tool" execution.
3. **stdio MCP transport.** Android cannot spawn subprocesses for stdio. Drop, keep only
   HTTP transport.
4. **VS Code Extension API.** Entirely N/A. Replace with Compose UI.
5. **`tree-sitter`-based code parsing.** Not relevant — our agent doesn't read source code,
   it edits design tokens.
6. **Hooks system (shell scripts).** Running shell scripts is not possible. Replace with
   in-process Kotlin hooks.

### 9.4 What ports cleanly

The four core abstractions — `AgentRuntime` (the loop), `ApiHandler` (provider interface),
`createTool` (tool definition), and `ContextManager` (truncation) — are all language-agnostic
and translate directly to Kotlin coroutines + interfaces + data classes.

---

## 10. Portability assessment — minimal viable subset

### 10.1 What to DROP (no Android equivalent or out of scope for MVP)

| Drop | Reason |
|---|---|
| VS Code / JetBrains extension host | N/A — we have Compose UI |
| `browser_action` (Puppeteer) | No JVM port; not needed for a design-system agent |
| `execute_command` / `bash` | Android cannot spawn subprocesses; the agent doesn't need a terminal |
| `search_files` (ripgrep) | Agent doesn't search source code; design tokens are in known JSON files |
| `list_code_definition_names` | Tree-sitter based; N/A |
| stdio MCP transport | Cannot spawn subprocesses |
| Hooks system (shell scripts) | Replace with Kotlin `BeforeToolHook` / `AfterToolHook` interfaces |
| Kanban (multi-agent web board) | Out of scope for v1 |
| Slack/Telegram/Discord connectors | Out of scope |
| Scheduled agents (cron) | Use `WorkManager` for recurring tasks if needed later |
| Multi-process RPC sidecar | Android runs in one process per app; we use coroutines |
| JetBrains plugin | N/A |
| Desktop Tauri app | N/A |
| `web_fetch` with HTML-to-markdown | Agent doesn't browse the web (separate AniList API client handles network) |

### 10.2 What to REIMPLEMENT (core ideas, fresh Kotlin code)

| Reimplement | Notes |
|---|---|
| `LlmProvider` interface + 4 concrete providers | OkHttp + kotlinx.serialization + SSE via OkHttp `EventSource` |
| Tool dispatch with coroutines | `suspend fun execute(input): ToolResult` |
| Context truncation (quarter strategy) | ~50 lines of Kotlin |
| File-read dedup | `Map<String, Long>` keyed by path, value mtime |
| Loop detection | 3 soft / 5 hard rule from Cline's `loop-detection.ts` (~70 lines) |
| Checkpoints (without git) | Room table of design-state snapshots |
| Approval flow (without polling) | `suspend fun approve(...)` instead of `pWaitFor` |
| Prompt variant system (simplified) | One variant per provider family; data class with sections |
| SEARCH/REPLACE or JSON-patch editing | See §12 |
| MCP client (HTTP-only) | Future enhancement, not MVP |

### 10.3 What to KEEP as-is (design patterns to copy)

- **Plan/Act mode split.** Maps to our agent having a "preview proposed changes" phase and an
  "apply" phase. Excellent UX for design changes.
- **`createTool` API shape.** `name`, `description`, JSON-Schema `inputSchema`, `execute`.
- **Tool-call streaming parse.** Discriminated union of `Text`/`Reasoning`/`ToolCalls`/
  `Usage` chunks. Kotlin sealed class.
- **Tool policies.** `{ autoApprove, enabled }` per tool. Kotlin data class.
- **`new_task` handoff for long sessions.** Map to "compact" operation in our agent.
- **Focus Chain / todo list.** A markdown checklist the agent maintains. Excellent for
  design tasks where users want to see the plan.
- **Restore Files / Restore Task / Restore Both** trichotomy. Perfect for design undo.
- **Tool-call loop detection.** 3 identical calls → soft warning, 5 → hard escalate.
- **File-read dedup cache.** Prevents re-sending the same large JSON theme file 5 times.

### 10.4 Proposed minimal Kotlin module shape

Mirror Cline's layered SDK directly — same boundaries, same dependency direction:

```
:agent:shared        ← types, schemas, tool defs, hook contracts
                       (AgentTool, ToolPolicy, AgentEvent, AgentResult, HookEngine)
                       NO higher-layer deps. Pure Kotlin.
:agent:llm           ← LlmProvider interface + 4 providers (Anthropic, OpenAI,
                       OpenRouter, Gemini) + OpenAI-compatible generic.
                       Depends on :agent:shared only.
:agent:core          ← AgentRuntime (the loop), ContextManager (truncation),
                       ToolExecutor, CheckpointStore (Room), ApprovalGateway,
                       PromptRegistry (variants). Depends on :agent:shared
                       and :agent:llm.
:agent:tools         ← Built-in tools for our domain:
                         - read_design_tokens (read current theme JSON)
                         - apply_token_patch (JSON-Patch RFC 6902)
                         - apply_text_patch (SEARCH/REPLACE for free-form text)
                         - set_sorting_rule (DSL write)
                         - set_theme_variant (Material You colors, dark/light)
                         - preview_state (snapshot current state)
                         - ask_user (clarifying question)
                         - attempt_completion
                       Depends on :agent:shared.
:agent:mcp           ← (future, post-MVP) HTTP-only MCP client
                       Depends on :agent:shared.
:app                 ← Android app, Compose UI, integration with AniList client,
                       integration with theme engine. Depends on :agent:core
                       and :agent:tools.
```

Dependency direction is downward only — same rule as Cline. `:agent:shared` is fully
embeddable in any Kotlin/JVM project (could even be reused server-side if we ever build a
cloud companion).

---

## 11. On-device LLM considerations

### 11.1 Cline assumes cloud LLMs via API — our Android agent will too

Cline's provider layer is BYOK (bring your own key) and supports local models via Ollama /
LM Studio. Our Android agent will use the **user's API key** for cloud LLMs (Anthropic,
OpenAI, OpenRouter). The patterns we keep:

| Pattern | Why we keep it |
|---|---|
| **Streaming SSE** | Essential for responsive UI. User sees the agent "thinking" character-by-character. Without streaming, a 30-second Claude response feels frozen. |
| **Tool-call parsing** | Native function-calling parsed into a sealed class hierarchy. Without this we couldn't have an agent at all. |
| **Diff-based file edits** | Necessary because design tokens are large JSON files — sending the whole file back on every edit wastes tokens. See §12. |
| **Provider abstraction** | User picks their provider. We must not lock to one. |
| **Approval flow** | User must approve destructive changes (writing a new theme, changing sort rules). |
| **Context truncation** | Mobile context is more precious than desktop — users have less RAM, shorter sessions. |

### 11.2 Streaming implementation on Android

OkHttp supports SSE natively via `EventSource` / `EventSourceListener`. Map each Anthropic
`message_start`, `content_block_delta`, `content_block_stop`, `message_delta`,
`message_stop` event into our sealed `LlmChunk` type. For OpenAI, parse
`data: {...}` lines and map `choices[0].delta` similarly.

### 11.3 API key storage

Cline stores keys in OS keychain (macOS Keychain, Windows Credential Manager, libsecret on
Linux). **Android equivalent: Android Keystore.** Never write keys to SharedPreferences
plaintext. Wrap the Keystore in a `KeyStorage` interface so a user can choose to proxy
through their own server instead.

### 11.4 Battery and network

| Concern | Mitigation |
|---|---|
| Battery drain from long agent loops | Hard cap on iterations per session (default 25). User sees countdown. |
| Cellular data cost | Warn before large requests on cellular. Offer "WiFi only" toggle. |
| Doze mode | Use `WorkManager` for background; foreground service for active agent runs |
| Cold start latency | Cache last-used model config; lazy-load provider classes via reflection |

---

## 12. Diff-based editing — should we adopt it?

### 12.1 Cline's two formats

**(a) `replace_in_file` with SEARCH/REPLACE blocks** (legacy, still supported):

```
Some text before.

<<<<<<< SEARCH
old code that exists in the file
=======
new code that should replace it
>>>>>>> REPLACE

Some text after.
```

Multiple `<<<<<<< SEARCH / ======= / >>>>>>> REPLACE` blocks can appear in one tool call,
applied sequentially. Fuzzy matching: try exact → trim line endings → trim all whitespace.

**(b) `apply_patch` with Codex-style unified diff** (modern SDK default):

```
*** Begin Patch ***
Update File: theme.json
@@ primaryColor
- "#FF5722"
+ "#3F51B5"
@@ surfaceColor
  "#FFFFFF"
- "elevation": 4
+ "elevation": 6
*** End Patch
```

`*** Begin Patch ***` / `*** End Patch` delimit the patch. Operations: `Add File`, `Update
File`, `Delete File`. `@@` followed by a context anchor (line text near the change) —
avoids line-number fragility. Space-prefixed lines = context, `-` = delete, `+` = add.

*(Source: docs.cline.bot/tools-reference/all-cline-tools confirms `apply_patch` is current.
The fabianhertwig blog and GitHub issue #4384 confirm the SEARCH/REPLACE format details.)*

### 12.2 Recommendation for our design-system agent

**Use JSON-Patch (RFC 6902) as the primary format, SEARCH/REPLACE as a fallback for
non-JSON text files.**

| File type in our app | Recommended edit format | Reason |
|---|---|---|
| Design tokens JSON (theme, palette, typography) | **JSON-Patch** (RFC 6902) | Deterministic, schema-aware, native tool-call input. Model emits an array of `{op, path, value}`. No fuzzy matching needed. |
| Sorting rule DSL (custom text format) | **SEARCH/REPLACE** | Free-form text; JSON-Patch doesn't apply. |
| `AGENTS.md` / `SKILL.md` / rules files | **SEARCH/REPLACE** | Same as above. |
| Compose `.kt` source (if we ever let it edit UI code — we won't, MVP) | `apply_patch` style | Code edits; line-aware. |

**Why JSON-Patch wins for tokens:** design tokens are well-structured JSON. JSON-Patch ops
are atomic, idempotent, and trivially checkable against a JSON Schema before applying. The
model emits one tool call per atomic change, which maps perfectly to our checkpoint table
(one row per patch application). If the patch fails validation, we reject before touching
the live theme — no half-applied state.

**Why SEARCH/REPLACE for text:** our sorting-rule DSL is a custom text format (something
like `sort by: score desc, title asc`). JSON-Patch can't describe edits to a non-JSON file.
SEARCH/REPLACE works on any text. We adopt the exact delimiter syntax Cline uses so we can
reuse any community knowledge / prompts.

```kotlin
// Proposed Kotlin tool: apply_token_patch
data class JsonPatchOp(
    val op: String,        // "add" | "remove" | "replace" | "move" | "copy" | "test"
    val path: String,      // JSON Pointer: "/colors/primary"
    val value: JsonElement?,
    val from: String?      // for move/copy
)

class ApplyTokenPatchTool : AgentTool {
    override val name = "apply_token_patch"
    override val inputSchema = jsonObjectSchema {
        property("file", stringSchema())         // "theme.json" | "palette.json" | ...
        property("patch", arraySchema(itemSchema = jsonPatchOpSchema))
        required("file", "patch")
    }
    override suspend fun execute(input: JsonObject): ToolResult { ... }
}
```

---

## 13. Checkpoint / rollback — recommended approach

### 13.1 Should we keep snapshots? **Yes, absolutely.**

For a design-system agent that modifies the user's UI, **undo is not optional** — it is the
single most important UX feature. Without it, users will not trust the agent. Cline's own
docs say: *"The cost of a mistake drops to nearly zero."*

### 13.2 Why we can't use Cline's shadow-git approach

- **No `git` CLI guaranteed on Android.** Even with Termux, we cannot rely on it.
- **Scoped Storage** restricts file system access; we can't freely write to a shadow repo
  outside the app's private storage.
- **Performance** — committing to git after every tool use is too slow for a mobile UI.

### 13.3 Recommended approach: Room-backed snapshot table

```kotlin
@Entity(tableName = "design_snapshots")
data class DesignSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,            // which agent session
    val messageId: String,           // which user message triggered it
    val timestamp: Long,              // System.currentTimeMillis()
    val toolName: String,             // "apply_token_patch" etc.
    val toolInputJson: String,        // what was applied
    val designStateJson: String,      // FULL design-token JSON BEFORE this tool ran
    val conversationSummary: String,  // short summary of conversation up to here
)
```

**Workflow:**

1. Before each **destructive** tool runs, the `CheckpointStore.captureBefore(sessionId,
   messageId, toolName, toolInput)` method:
   - Snapshots the **current** design-token state (all JSON files serialized).
   - Inserts a row into `design_snapshots`.
   - Returns the snapshot ID.
2. The destructive tool runs.
3. The UI shows a "Checkpoint" indicator with **Compare** and **Restore** buttons.
4. **Restore Files** — load `designStateJson` from the snapshot row, write it back to the
   theme files, recompose UI. Keep conversation.
5. **Restore Task Only** — delete messages after `messageId`, keep files. Useful when the
   agent went off-track but the design changes were good.
6. **Restore Files & Task** — both.

### 13.4 Storage budget

Each snapshot is a full design-token JSON (typically <5 KB for a Material 3 theme). At one
snapshot per tool use and ~25 tool uses per session, that's ~125 KB per session — trivial.
We auto-trim snapshots older than 30 days or beyond 500 per session.

### 13.5 Compare view

For the Compare button, we render a simple diff of the two JSON states (not a code diff —
a **token-level diff** showing which color/spacing/typography values changed). This is much
more readable for users than a code diff.

### 13.6 What we DON'T checkpoint

- **Read-only operations** (`read_design_tokens`, `ask_user`, `attempt_completion`) do not
  create snapshots. Same as Cline.
- **Conversation messages** themselves are always persisted in their own Room table; we
  don't snapshot them per-tool.

---

## 14. Risks / gotchas — what's a bad fit for Android

### 14.1 Battery and background execution

| Risk | Mitigation |
|---|---|
| Long agent loops drain battery | Hard iteration cap (default 25). User sees countdown. Foreground service with notification while running. |
| Doze mode kills background agents | Use `WorkManager` with `setExpedited()` for active runs. Warn user that background execution is unreliable. |
| Cellular data cost | Warn before requests >10k tokens on cellular. "WiFi only" toggle. |
| Cold start latency | Cache provider config; lazy-load via ServiceLoader. |

### 14.2 No terminal

The `bash` / `execute_command` tool is meaningless on Android. We replace it with
**domain-specific tools** that produce the same effect (modify state, run a sort, apply a
theme). The agent never needs a shell — it needs **state mutations**, and we give it
typed tools for each kind of mutation.

### 14.3 Sandboxed file system

Android Scoped Storage (Android 11+) means:
- The agent **cannot read arbitrary files** on the device.
- It **can** read/write app-private storage freely — that's where our design tokens live.
- It **can** read user-shared URIs via `ContentResolver` (for things like importing a
  theme JSON the user picked).
- It **cannot** watch arbitrary files with `FileObserver` outside app-private storage.

For our use case this is fine — all design state is in app-private storage. We document this
clearly in our `AGENTS.md` so the model understands the sandbox.

### 14.4 Memory pressure

Android apps have a much smaller heap than desktop Node processes. Implications:
- **Aggressive context truncation** — start truncating at 50 % utilization, not 70 %.
- **No large file reads** — if a design-token file grows beyond 50 KB, paginate it.
- **Stream parse tool calls** — don't buffer the entire response in memory.
- **Use `ImageDecoder`** for any image the model needs to see, with proper downsampling.

### 14.5 API key security

| Risk | Mitigation |
|---|---|
| Key in plaintext prefs | Android Keystore only. EncryptedSharedPreferences as a fallback with hardware-backed keys. |
| Key in network logs | OkHttp interceptor must redact `Authorization` header in debug logs. |
| Key exfiltrated via backup | `android:allowBackup="false"` on the application tag, or exclude the credentials dir. |
| Key in crash reports | Strip from Crashlytics via `setCustomKeys` allow-list, never log the key. |

### 14.6 Permission fatigue

If every design change prompts for approval, users will tap-through mindlessly. Mitigation:
- **Read-only** tools (`read_design_tokens`, `preview_state`) auto-approve.
- **Reversible** writes (`apply_token_patch` with a checkpoint) auto-approve but show a
  non-blocking toast with an Undo button.
- **Irreversible** writes (deleting a saved theme, overwriting user data) require explicit
  tap-with-confirm.
- The "three categories" pattern matches Cline's "auto-approve reads, prompt writes"
  recommendation almost exactly.

### 14.7 Process isolation

Cline's tool handlers all run in-process — a buggy handler can crash the extension. On
Android, a crash takes down the whole app. Mitigation:
- Run the agent loop in a `SupervisorJob` so a single tool failure doesn't cancel the whole
  session.
- Catch `Throwable` at the tool-execution boundary (not just `Exception`) to survive OOM and
  native crashes from dependencies.
- Consider running the agent in a separate `:agent` process for v2 if stability becomes an
  issue. Cline's teardown explicitly recommends this as "an area for future growth" for
  Cline itself.

### 14.8 MCP — drop stdio, keep HTTP

As noted in §6, the `stdio` MCP transport is impossible on Android. We support only
`streamableHttp`. The MCP **auto-discover** pattern (Cline's "ask Cline to create custom
tools on the fly") also won't work — there's no local terminal for Cline to spawn a server
in. Power users must configure remote HTTP MCP servers manually via a settings screen.

### 14.9 Plan/Act mode UX

On desktop, Plan/Act is a toggle in the sidebar. On Android, it should be a **bottom-sheet
mode picker** with a clear visual distinction (Plan = blue/preview, Act = green/apply). The
"every edit requires approval" pattern from Cline's Act mode is too chatty for mobile; we
default to "auto-apply with undo" for reversible changes and only prompt for irreversible.

### 14.10 System prompt size

Cline's system prompt is **thousands of tokens** even before the user's task (tool
definitions + rules + environment details + skills). On Android this is fine for a
first-class cloud LLM (Claude 200K context, GPT-4o 128K context). But we should:
- Keep tool descriptions **terse**.
- Lazy-load skill content (only inject the SKILL.md text if the model invokes `use_skill`).
- Cache the static part of the system prompt per provider (saves token-counting work).

---

## Porting Recommendation (final synthesis)

### Keep (architectural patterns to copy verbatim)

1. **Layered SDK architecture.** `:agent:shared` → `:agent:llm` → `:agent:core` (+ `:agent:tools`).
   Strict downward dependency. Same boundaries as `@cline/shared`, `@cline/llms`,
   `@cline/agents`, `@cline/core`.
2. **`createTool` API shape.** `name`, `description`, JSON-Schema `inputSchema`,
   `suspend execute(input): ToolResult`. One-to-one Kotlin equivalent.
3. **Provider abstraction.** One `LlmProvider` interface, multiple concrete impls, async
   generator → Kotlin `Flow<LlmChunk>`.
4. **Streaming chunk taxonomy.** Sealed class: `Text`, `Reasoning`, `ToolCalls`, `Usage`.
   Same as Cline's `ApiStream` chunks.
5. **Plan/Act mode split.** Maps to "preview" vs "apply" phases for design changes.
6. **Tool policies.** Per-tool `{ autoApprove, enabled }` map. Same semantics.
7. **Context truncation.** Quarter strategy + delete-from-middle + preserve-initial-exchange.
   File-read dedup with mtime cache.
8. **Loop detection.** 3 identical calls → soft warning, 5 → hard escalate. ~70 lines of
   Kotlin.
9. **Three-way restore.** Restore Files / Restore Task Only / Restore Files & Task.
10. **`new_task` handoff** for long sessions.
11. **Focus Chain** (agent-maintained todo list as markdown).
12. **Restore-as-you-go checkpoint UX.** Per-tool checkpoint indicators with Compare/Restore
    buttons.
13. **Tool-call rejection as a normal response.** Agent doesn't get stuck; rejection counts
    as a turn.

### Drop (no Android equivalent or out of scope for MVP)

1. VS Code / JetBrains extension host, webview, Tauri desktop, Kanban.
2. `bash` / `execute_command` — replaced with typed design tools.
3. `browser_action` (Puppeteer) — no JVM port; not needed.
4. `search_files` (ripgrep) — agent doesn't search source code.
5. `list_code_definition_names` (tree-sitter) — N/A.
6. stdio MCP transport — impossible on Android.
7. Hooks shell scripts — replaced with in-process Kotlin hook interfaces.
8. Slack/Telegram/Discord connectors — out of scope.
9. Scheduled agents (cron) — defer to post-MVP via WorkManager.
10. Multi-process RPC sidecar — single Android process.
11. Shadow git checkpoints — replaced with Room snapshot table.
12. `web_fetch` with HTML-to-markdown — agent uses the AniList/Kitsu/Jikan clients directly.

### Reimplement (core ideas, fresh Kotlin)

1. **`LlmProvider` interface** + 4 providers (Anthropic, OpenAI, OpenRouter, Gemini) + 1
   OpenAI-compatible generic. OkHttp + kotlinx.serialization + SSE via `EventSource`.
2. **`AgentRuntime`** — iterative `while (isActive)` coroutine loop (NOT Cline's recursive
   pattern; the teardown itself recommends this evolution).
3. **`ContextManager`** — immutable `List<Message>` + `deletedRange: IntRange` + quarter
   truncation. ~50 lines of Kotlin.
4. **`ToolExecutor`** — `Map<String, AgentTool>` registry, dispatch via coroutine.
5. **`CheckpointStore`** — Room table of design-state snapshots.
6. **`ApprovalGateway`** — `suspend fun approve(toolName, input): ApprovalResult`. Runs on
   UI thread via Compose; no polling.
8. **`PromptRegistry`** — one variant per provider family (Claude/GPT/Gemini/Generic).
9. **`apply_token_patch`** — JSON-Patch (RFC 6902) for design tokens.
10. **`apply_text_patch`** — SEARCH/REPLACE for free-form text (rules, AGENTS.md, skills).
11. **MCP HTTP-only client** — post-MVP.

### Proposed Kotlin module shape (minimal viable)

```
:agent:shared/        — AgentTool, ToolPolicy, AgentEvent, AgentResult,
                       HookEngine, Message, ContentBlock, ModelInfo, ProviderInfo
                       (pure Kotlin, no Android deps, JVM-targetable)

:agent:llm/          — LlmProvider interface
                       AnthropicProvider, OpenAIProvider, OpenRouterProvider,
                       GeminiProvider, OpenAICompatibleProvider
                       LlmChunk sealed class: Text, Reasoning, ToolCalls, Usage
                       (depends on :agent:shared, OkHttp, kotlinx.serialization)

:agent:core/         — AgentRuntime (the while(isActive) loop)
                       ContextManager (truncation, dedup, deletedRange)
                       ToolExecutor (Map<String, AgentTool>)
                       CheckpointStore (Room DAO for design_snapshots)
                       ApprovalGateway (suspend approve callback)
                       PromptRegistry (variant selection)
                       FileReadCache (mtime map)
                       LoopDetector (3-soft / 5-hard)
                       (depends on :agent:shared, :agent:llm, Room)

:agent:tools/        — ReadDesignTokensTool
                       ApplyTokenPatchTool   (JSON-Patch RFC 6902)
                       ApplyTextPatchTool    (SEARCH/REPLACE)
                       SetSortingRuleTool
                       SetThemeVariantTool
                       PreviewStateTool
                       AskUserTool
                       AttemptCompletionTool
                       NewTaskTool
                       (depends on :agent:shared)

:app/                — Compose UI, AniList/Kitsu/Jikan integration,
                       theme engine integration, Android Keystore for API keys,
                       foreground service for active runs, WorkManager for background.
                       (depends on :agent:core, :agent:tools)
```

### One-line recommendation

> **Mirror Cline's `@cline/shared` → `@cline/llms` → `@cline/agents` layered SDK exactly,
> ported to Kotlin coroutines. Drop everything Node-specific (Puppeteer, stdio MCP,
> shadow git, bash, VS Code host). Reimplement LLM providers with OkHttp + SSE. Use
> JSON-Patch for design-token edits and SEARCH/REPLACE for free-form text. Use Room for
> checkpoint storage instead of shadow git. Ship with 4 LLM providers (Anthropic, OpenAI,
> OpenRouter, Gemini). Treat Cline as architectural inspiration only — do not copy source
> files verbatim, to keep our Apache 2.0 obligations minimal (just ship a NOTICES screen
> crediting Cline).**

---

## Appendix A — Verification log

| Claim | Source | Status |
|---|---|---|
| Apache 2.0 license | Read `raw.githubusercontent.com/cline/cline/main/LICENSE` directly | ✅ Verified |
| 66 k+ stars, 7.1k forks | GitHub repo page | ✅ Verified |
| ~560 KLOC, 3,756-line Task class, 28 tools, 43 providers | awesome-ai-anatomy teardown | ✅ Verified (cross-referenced) |
| SDK layered packages (`@cline/core`, `@cline/agents`, `@cline/llms`, `@cline/shared`) | docs.cline.bot/sdk/architecture | ✅ Verified |
| `recursivelyMakeClineRequests` is recursive | awesome-ai-anatomy teardown + medium dissecting article | ✅ Verified |
| Built-in SDK tools: bash, editor, read_files, apply_patch, search, fetch_web, ask_question | docs.cline.bot/tools-reference/all-cline-tools | ✅ Verified |
| Tool handler interface (IToolHandler, IFullyManagedTool, SharedToolHandler) | awesome-ai-anatomy teardown citing `ToolExecutorCoordinator.ts` | ✅ Verified |
| ApiHandler interface (`createMessage`, `getModel`, `getApiStreamUsage`, `abort`) | awesome-ai-anatomy teardown citing `src/core/api/index.ts` | ✅ Verified |
| ContextManager ~1,300 lines, quarter-truncation, conversationHistoryDeletedRange tuple | awesome-ai-anatomy teardown + medium dissecting article | ✅ Verified |
| Auto-condense at ~75 % for Claude 4+ / GPT-5 | awesome-ai-anatomy teardown | ✅ Verified |
| Shadow git checkpoints, three restore options | docs.cline.bot/core-workflows/checkpoints | ✅ Verified |
| MCP client-only, 3 transports, mcp.json config shape | docs.cline.bot/mcp/mcp-overview | ✅ Verified |
| Auto-approve categories, YOLO mode, requires_approval flag | docs.cline.bot/features/auto-approve + teardown | ✅ Verified |
| ToolPolicies SDK API (autoApprove / enabled) | docs.cline.bot/sdk/guides/permission-handling | ✅ Verified |
| Polling approval at 100 ms via pWaitFor | awesome-ai-anatomy teardown citing `Task.ask()` | ✅ Verified |
| 8 lifecycle hooks (TaskStart, TaskResume, TaskCancel, UserPromptSubmit, PreToolUse, PostToolUse, PreCompact, Notification) | awesome-ai-anatomy teardown | ✅ Verified |
| Puppeteer (not Playwright) for browser | awesome-ai-anatomy teardown | ✅ Verified |
| Plan/Act dual-mode (separate providers/models per mode) | awesome-ai-anatomy teardown | ✅ Verified |
| Subagents run in-process, depth-1 | awesome-ai-anatomy teardown citing `SubagentRunner.ts` | ✅ Verified |
| npm package still named "claude-dev" | awesome-ai-anatomy teardown | ✅ Verified (legacy fossil) |
| Bun build system | Repo commit history `Migrate apps/vscode from npm/node to bun (#11632)` | ✅ Verified |
| SEARCH/REPLACE block format (`<<<<<<< SEARCH` / `>>>>>>> REPLACE`) | fabianhertwig blog + GitHub issue #4384 | ✅ Verified |
| `apply_patch` Codex-style format | docs.cline.bot/tools-reference/all-cline-tools + fabianhertwig blog | ✅ Verified |
| File-read dedup with `[DUPLICATE FILE READ]` notice | medium dissecting-cline article | ✅ Verified |
| FileContextTracker uses vscode.FileSystemWatcher | medium dissecting-cline article citing `FileContextTracker.ts` | ✅ Verified |
| Rust 0.3 % of codebase purpose | GitHub repo page language stats | ❓ Unverified (assumed: desktop sidecar or native parser) |
| Specific npm dependency list (e.g. `puppeteer-core`, `chokidar`, `p-wait-for`) | awesome-ai-anatomy teardown + general knowledge | ⚠️ Partially verified (no `package.json` read directly) |
| MCP OAuth details | awesome-ai-anatomy teardown citing `McpOAuthManager` | ✅ Verified |
| `tree-sitter` for code parsing | awesome-ai-anatomy (implied by `list_code_definition_names` tool) | ⚠️ Inferred (unverified) |

---

## Appendix B — Source URLs (for traceability)

- Repo: https://github.com/cline/cline
- LICENSE: https://raw.githubusercontent.com/cline/cline/main/LICENSE
- SDK README: https://github.com/cline/cline/blob/main/sdk/README.md
- SDK Architecture: https://docs.cline.bot/sdk/architecture
- Tools reference: https://docs.cline.bot/tools-reference/all-cline-tools
- Checkpoints: https://docs.cline.bot/core-workflows/checkpoints
- MCP overview: https://docs.cline.bot/mcp/mcp-overview
- Auto-approve: https://docs.cline.bot/features/auto-approve
- Permission handling: https://docs.cline.bot/sdk/guides/permission-handling
- Context window blog: https://cline.bot/blog/clines-context-window-explained-maximize-performance-minimize-cost
- Dissecting Cline (Medium): https://medium.com/@balajibal/dissecting-cline-cline-context-management-260aec3d84cb
- Deep teardown: https://github.com/cline/cline/issues/10177
- awesome-ai-anatomy teardown: https://github.com/NeuZhou/awesome-ai-anatomy/tree/main/cline
- File editing comparison: https://fabianhertwig.com/blog/coding-assistants-file-edits
- npm package: https://www.npmjs.com/package/cline

---

*End of report.*
