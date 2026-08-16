# R-A3 — OpenCode Deep Analysis for Android Agent Port

> **Task ID:** R-A3
> **Target repo:** https://github.com/anomalyco/opencode (formerly `sst/opencode`)
> **Analysis goal:** Assess OpenCode as the architectural backbone for a dedicated Android Agent application (sandboxed folder, multiple workspaces, background execution, custom LLM models, highly customizable).
> **Method:** Direct source inspection of the OpenCode engine inside the Kilo Code fork at `references/kilocode/packages/opencode/` (the OpenCode engine is preserved there with `kilocode_change` markers around Kilo-specific deltas, so OpenCode-native code is identifiable). Cross-checked against the official docs at opencode.ai and the anomalyco/opencode GitHub page via web search.

---

## A. Architecture Overview

### A.1 What is OpenCode?

**OpenCode** is the **open-source AI coding agent** originally built by the SST team (now **Anomaly**) and now hosted at **`anomalyco/opencode`** (formerly `sst/opencode`). It is a CLI-first, terminal-based coding agent that has grown into a multi-surface product:

- **Terminal TUI** (`opencode` interactive command, built on SolidJS + OpenTUI)
- **One-shot CLI** (`opencode run "<task>"` headless single-task mode)
- **Headless HTTP daemon** (`opencode serve` — the centerpiece for our Android use case)
- **Desktop app** (BETA, multi-platform binary)
- **IDE extension** (Zed via Agent Client Protocol; VS Code + JetBrains via the HTTP daemon)
- **Cloud variant**

Repository stats (as observed):
- **~198k GitHub stars**, **950+ contributors**, MIT-licensed.
- Top-level `package.json` declares `"license": "MIT"`. ✅ MIT confirmed.
- The Kilo Code fork's `LICENSE` carries dual attribution: *"Copyright (c) 2026 Kilo Code / Copyright (c) 2025 opencode"* — confirming OpenCode is the upstream.

### A.2 Language / Tech Stack

| Layer | Technology |
|---|---|
| **Language** | TypeScript (strict, ESM, `"type": "module"`) |
| **Runtime** | Bun (primary), Node.js (fallback for some sub-paths) |
| **Build** | Bun scripts (`script/build.ts`); TypeScript via `tsgo` for typecheck |
| **Functional core** | `effect` (Effect-TS) — `Effect`, `Layer`, `Context.Service`, `Schema`, `Stream`, `Scope` everywhere |
| **LLM SDK** | Vercel **AI SDK** (`ai` + `@ai-sdk/*` provider packages) |
| **HTTP server** | `effect/unstable/http` (`HttpRouter`, `HttpServer`) on top of `@effect/platform-node` + `node:http` |
| **API spec** | OpenAPI auto-generated via `effect/unstable/httpapi` (`OpenApi.fromApi(...)`) |
| **TUI** | **SolidJS + OpenTUI** (`@opentui/solid`) — JSX renders to terminal `<box>` / `<text>` / `<scrollbox>` elements |
| **CLI** | `yargs` + `@clack/prompts` + custom `effectCmd` wrapper |
| **DB / Storage** | Filesystem JSON for sessions (`Storage.write(["session", projectID, sessionID], data)`); SQLite via `drizzle-orm` + `effect-drizzle-sqlite` for richer queries |
| **Process spawning** | `cross-spawn`, `@lydell/node-pty` / `bun-pty`, `ChildProcessSpawner` |
| **Git** | `simple-git` + direct `git` CLI (for snapshot/checkpoint system) |
| **Code parsing** | `web-tree-sitter` + `tree-sitter-{bash,powershell,...}` |
| **Search** | `ripgrep` (bundled) |
| **File watching** | `@parcel/watcher`, `chokidar`, `glob`, `minimatch` |
| **MCP** | `@modelcontextprotocol/sdk` v1.29 |
| **ACP** | `@agentclientprotocol/sdk` v0.21 (Agent Client Protocol for IDE integration — used by Zed) |
| **Auth** | `@openauthjs/openauth`, `google-auth-library`, `@aws-sdk/credential-providers`, GitLab auth, GitHub Copilot auth, Kilo Gateway auth |
| **Observability** | OpenTelemetry (`@opentelemetry/*` + `@effect/opentelemetry`) |
| **Networking** | `ws` (WebSocket), `bonjour-service` (mDNS for LAN daemon discovery) |

### A.3 CLI? Library? Daemon? VSCode extension?

**All of the above**, but with a clear separation between an **engine/daemon** and **thin clients**:

1. **Engine package** (`@opencode-ai/core`, `@opencode-ai/server`, `@opencode-ai/llm`, `@opencode-ai/protocol`, `@opencode-ai/schema`, `@opencode-ai/sdk`, `@opencode-ai/tui`, `@opencode-ai/codemode`) — reusable TypeScript libraries published as workspace packages. They expose the Engine as a set of Effect `Layer`s and `Context.Service`s.
2. **CLI binary** (`opencode`) — yargs-based, with subcommands: `run`, `serve`, `attach`, `tui`, `agent`, `mcp`, `models`, `providers`, `session`, `pr`, `github`, `generate`, `export`, `import`, `plug`, `account`, `acp`, `db`, `config`, `stats`, `remote`, `upgrade`, `uninstall`, `debug/*`.
3. **TUI** (default `opencode` invocation) — a SolidJS-in-terminal app rendered via OpenTUI.
4. **Headless HTTP daemon** (`opencode serve`) — opens a port, advertises via mDNS, speaks HTTP + SSE + WebSocket. The TUI, IDE extensions, and remote clients all talk to this daemon.
5. **IDE extensions** (VS Code, JetBrains) — thin clients that connect to the local `opencode serve` daemon over HTTP/SSE.
6. **Desktop app** — bundled Electron-style wrapper.

**This is a fundamentally different shape from Cline** (which is a VSCode-extension-only TypeScript module that runs inside the extension host process, with no daemon or HTTP API surface).

### A.4 How does OpenCode differ from Cline + Kilo Code?

| Dimension | **Cline** | **OpenCode** | **Kilo Code (current)** |
|---|---|---|---|
| Origin | Roo Code fork → independent | Original (sst → anomaly) | **Fork of OpenCode** |
| License | Apache 2.0 | **MIT** | MIT (dual-attribution) |
| Surface | VS Code extension only | CLI + TUI + daemon + Desktop + IDE + Cloud | CLI + VS Code + JetBrains (all built on the OpenCode daemon) |
| Engine location | Extension host process | Standalone TS engine, runs as own process via `opencode serve` | Same — inherits OpenCode's daemon architecture |
| UI tech | React (webview) | SolidJS + OpenTUI (terminal) | SolidJS terminal + VS Code webview |
| Protocol for clients | None (in-process) | **HTTP REST + SSE + WebSocket + OpenAPI + ACP** | Same — inherits OpenCode protocols |
| LLM abstraction | `@anthropic-ai/sdk`, `openai`, etc. directly | **Vercel AI SDK** + `models.dev` catalog | Same (Vercel AI SDK) |
| Provider count | ~10 | **75+** (via AI SDK + models.dev) | 500+ (via Kilo Gateway overlay) |
| Storage | JSON files in extension globalStorage | Filesystem JSON (`~/.local/share/opencode/storage/`) + SQLite (drizzle) | Same + Kilo Cloud sync |
| Checkpoints | Git-based snapshots in extension workspace | **Git worktree-based snapshots** in `~/.local/share/opencode/snapshot/<projectID>/<hash>` | Same (Kilo extends with diff/revert) |
| Custom tools | `cline_providerSettings`, MCP | **Filesystem-discovered (`{tool,tools}/*.{js,ts}`) + Plugin tools + MCP** | Same + Kilo plugin registry |
| Sub-agents | "Modes" (custom agents via YAML frontmatter) | `task` tool → spawns `subagent` mode agents (in-process or background) | Same + agent requirements + agent-manager |
| Auto-compaction | "Context Condensing" (LLM-based) | **LLM-based compaction** with PRUNE_MINIMUM=20k / PRUNE_PROTECT=40k tokens, turn-preserving tail | Same + Kilo preflight compaction + payload-limit pruning |
| Background execution | No (extension lifecycle) | **First-class: `BackgroundJob` service** with start/extend/wait/promote/cancel | Same |
| MCP transports | stdio + SSE + HTTP | **stdio + SSE + HTTP + OAuth callback** | Same |
| ACP support | No | **Yes** (native — Agent Client Protocol for IDEs) | Yes (inherited) |
| mDNS LAN discovery | No | **Yes** (`bonjour-service`) | Yes (inherited) |
| Tech stack maturity | TypeScript / Node | TypeScript / **Bun + Effect-TS** (functional effects, typed errors, layered DI) | Same (Effect-TS heavy) |

**Headline difference:** OpenCode is a **daemon-first, protocol-first** engine. Cline is a **UI-first, in-process** extension. For an Android port that needs UI/engine decoupling and background execution, this is the single most important distinction.

---

## B. Tool System Analysis

### B.1 OpenCode-Native Built-in Tools

Source: `packages/opencode/src/tool/registry.ts` (the `tool` Effect.gen block) — every entry below is initialized with `Tool.init(...)` and added to `builtin`. Kilo-only tools (`clone`, `overview`, `suggest`) are excluded — they are marked `// kilocode_change` in the source.

| # | Tool ID | Description | Portable to Android? |
|---|---|---|---|
| 1 | `bash` (shell) | Execute shell commands in workspace, with OS/shell context injection and a pre-approved temp dir `${tmp}`. Tree-sitter parses output. | **Partial** — Android has no bash by default. Need to reimplement on `sh`/`mksh`/Termux or a custom command-runner. The OS context block must be Android-aware. |
| 2 | `read` | Read a file or directory; absolute paths; offset/limit paging; 2000-line default. | **Yes** — pure filesystem, trivially portable. |
| 3 | `glob` | File pattern matching via `@parcel/watcher`'s `Glob.scan` + `minimatch`. | **Yes** — pure JS, portable. |
| 4 | `grep` | Content search using **bundled ripgrep binary**. | **Partial** — needs an Android ripgrep binary (Termux ships one; or compile ripgrep for aarch64-linux-android). Otherwise reimplement on a JS regex walker. |
| 5 | `edit` | Exact string replacement in a file (requires prior `read`). Fails on missing `oldString`. | **Yes** — pure JS string ops. |
| 6 | `write` | Write/create file (requires prior `read` if file exists). | **Yes** — pure FS. |
| 7 | `apply_patch` | Apply a stripped-down unified-diff patch format (*** Begin Patch / *** End Patch). Used as an alternative to `edit` for some models. | **Yes** — pure JS diff application. |
| 8 | `task` | Launch a sub-agent (subagent_type) for complex multi-step work. Supports `background: true` for async delegation via `BackgroundJob`. Can resume a prior task_id. | **Yes** — pure in-process agent spawn. |
| 9 | `webfetch` | Fetch a URL, convert content to markdown (via `turndown`). | **Yes** — `fetch` + turndown, both JS. |
| 10 | `websearch` | Real-time web search via session's configured search provider (Exa / Parallel / Tavily / built-in). Live-crawl modes `fallback` / `preferred`. | **Yes** — HTTP-only. Provider keys configurable. |
| 11 | `todowrite` | Create / update structured task list for the session. | **Yes** — pure state, trivially portable. |
| 12 | `plan` (`plan_enter` / `plan_exit`) | Switch the agent into a planning mode (read-only) and back. Plan-exit hands the plan file back to the user. | **Yes** — pure orchestration, no system deps. |
| 13 | `skill` | Load a specialized skill (instructions + scripts) by name from configured skill dirs. | **Yes** — pure file-loading. |
| 14 | `lsp` (experimental) | LSP queries: goToDefinition, findReferences, hover, documentSymbol, workspaceSymbol. | **No** — depends on running LSP servers (tsserver, etc.). For Android, drop entirely or run LSPs in a companion process. |
| 15 | `question` | Ask the user a multi-choice question mid-turn. | **Yes** — surfaces via the permission/event bus; UI-side concern. |
| 16 | `invalid` | Fallback tool the LLM is routed to when its tool call fails schema validation or names an unknown tool — feeds back a structured "rewrite the input" error. | **Yes** — pure plumbing. |
| 17 | `execute` (code-mode, experimental) | Call MCP-served tools through a code DSL the model emits. | **Yes** — pure orchestration over MCP. |

**OpenCode-native tools total: 17** (including experimental `lsp` and `code-mode`).

**Kilo-added tools (NOT in upstream OpenCode, listed for completeness):** `clone` (repo cloning), `overview` (repo overview), `suggest` (suggestions).

**MCP-served tools** are dynamically registered at runtime from configured MCP servers (stdio/SSE/HTTP transports), wrapped with permission rules, and exposed alongside built-ins.

### B.2 Tool Registration + Dispatch

#### Registration pattern (`src/tool/tool.ts` + `src/tool/registry.ts`)

```ts
// 1. Define a tool — returns Effect<Info>
export const MyTool = Tool.define("my_tool", Effect.gen(function* () {
  return {
    description: "...",
    parameters: Schema.Struct({ /* Effect Schema */ }),
    execute: async (args, ctx) => Effect.succeed({ title, metadata, output }),
  }
}))

// 2. ToolRegistry Service collects all tools
class Service extends Context.Service<Service, Interface>()("@opencode/ToolRegistry") {}

// 3. Interface:
interface Interface {
  ids(): Effect<string[]>
  all(): Effect<Tool.Def[]>
  named(): Effect<{ task: TaskDef; read: ReadDef }>
  tools(model, agent, permission?, networkRestricted?): Effect<Tool.Def[]>
}
```

#### Dispatch pipeline (per LLM turn, in `session/llm.ts` + `session/processor.ts`)

1. `ToolRegistry.tools({ providerID, modelID, family, agent, permission, networkRestricted })` resolves the **visible toolset** for this turn — filters by agent permissions, network sandbox, model family, experimental flags, and `apply_patch` vs `edit` selection.
2. Each `Tool.Def` is converted to a Vercel AI SDK `Tool` object (`{ description, parameters: jsonSchema, execute }`).
3. AI SDK's `streamText({ tools, toolChoice, experimental_repairToolCall })` drives the LLM.
4. When the model calls a tool, AI SDK invokes `tool.execute(args, ctx)`. The wrapped `execute`:
   - Decodes args via the Effect Schema (`Schema.decodeUnknownEffect`) — on failure raises `InvalidArgumentsError`, which AI SDK feeds back to the model as the tool result, prompting a rewrite.
   - Calls the inner `execute(args, ctx)` which receives a `Tool.Context`:
     ```ts
     type Context = {
       sessionID, messageID, agent, abort: AbortSignal, callID?,
       messages: SessionV1.WithParts[],
       metadata(input: { title?, metadata? }): Effect<void>,
       ask(permissionRequest): Effect<void>  // per-call permission check
     }
     ```
   - Runs the `Truncate.Service` over the output (auto-truncate long output, spill to `outputPath`).
   - Returns `{ title, metadata, output, attachments? }`.
5. The processor streams the result back to the model in the next turn, plus publishes `Session.Event.*` events for UIs.

**Custom tools:** loaded from filesystem globs `{tool,tools}/*.{js,ts}` in any configured config directory (`~/.config/opencode/`, project `.opencode/`, etc.). They export `ToolDefinition` objects (`{ args, description, execute }`). Plugin tools are also registered via `Plugin.Service.list()`. Both go through `fromPlugin(id, def)` to be normalized into the native `Tool.Def` shape.

### B.3 Can Custom Tools Be Added?

**Yes — three tiers:**

1. **Filesystem-discovered JS/TS modules** in `tool/` or `tools/` directories (loaded via dynamic `import(pathToFileURL(match).href)`). This is the **primary extension point** for users.
2. **Plugin tools** via `@opencode-ai/plugin` / `@kilocode/plugin` packages (npm-installable, registered declaratively).
3. **MCP tools** — any MCP server (stdio/SSE/HTTP, with OAuth) exposes its tools, which are merged into the registry at runtime via `MCP.Service`.

All three converge on the same `Tool.Def` shape and go through the same dispatch/permission/truncation pipeline. **This is a clean, idiomatic extension model — and a major strength of OpenCode.**

---

## C. Agent Loop + Context Management

### C.1 The Agent Loop

Source: `src/session/prompt.ts` (function `loop`, line ~1487). The loop is a `while (true)` with explicit continue/break points:

```
prompt(input)              // create user message
  └── loop(input)            // main turn loop
        while (true):
          1. status.set("busy")
          2. msgs = MessageV2.filterCompacted(sessionID)
          3. Trim/prompt-queue-scope msgs
          4. Find latest user msg + latest assistant msg
          5. If last assistant finished && no pending tool calls && no orphan tools
                 -> break (turn done)
          6. If plan_exit pending -> ask followup -> continue or break
          7. step++
             - On step 1: fork off title generation (uses Agent.generate + LLM)
          8. Resolve model (provider.getLanguage / provider.getModel)
          9. Pop next task from queue:
             - "subtask" -> delegate to sub-agent (in-process or background)
             - "compaction" -> run compaction turn
          10. If overflow detected -> create compaction task -> continue
          11. Resolve agent (Agent.get(lastUser.agent))
              - agent.steps (max) -> inject MAX_STEPS_PROMPT on last step
          12. Apply SessionReminders (inject reminders like git status, todos)
          13. Create assistant message (msg), attach processor handle
          14. handle.process(streamInput):
                - LLM.stream(input) -> Vercel AI SDK streamText()
                - SessionProcessor consumes LLMEvent stream:
                    * text deltas -> PartDelta events
                    * reasoning deltas -> ReasoningPart events
                    * tool calls -> permission.ask() (if needed) -> tool.execute() -> ToolPart events
                    * step-start / step-finish -> snapshot tracking
                - Returns Result ∈ { "compact", "stop", "continue" }
          15. If "compact" -> create compaction task -> continue
              If "stop"     -> break
              If "continue" -> loop again
        end while
```

**Key safety valves:**
- `DOOM_LOOP_THRESHOLD = 3` (in `processor.ts`) — detects consecutive identical failed tool calls and breaks out.
- `MAX_STEPS_PROMPT` — when `agent.steps` is set (per-agent), the final step gets a system-injected warning telling the model to wrap up.
- **Abort:** `AbortController` + `AbortSignal` propagated through `ctx.abort` to every tool. Cancel via `SessionPrompt.cancel(sessionID)`.
- **Subtask delegation** (the `task` tool with `background: true`) starts a `BackgroundJob` and returns immediately; the parent agent is notified asynchronously when the child finishes (via the event bus).

### C.2 Context Management + Auto-Compaction

Source: `src/session/compaction.ts`. OpenCode has **first-class auto-compaction** that is materially more sophisticated than Cline's:

- **Constants:**
  - `PRUNE_MINIMUM = 20_000` tokens — below this, never compact.
  - `PRUNE_PROTECT = 40_000` tokens — protected tail kept verbatim.
  - `TOOL_OUTPUT_MAX_CHARS = 2_000` — older tool outputs are pruned to this size.
  - `PRUNE_PROTECTED_TOOLS = ["skill"]` — these tool outputs are never pruned.
  - `DEFAULT_TAIL_TURNS = 2` — keep the last 2 turns intact.
  - `MIN_PRESERVE_RECENT_TOKENS = 2_000`, `MAX_PRESERVE_RECENT_TOKENS = 8_000` — adaptive tail budget (25% of usable context, clamped).

- **Trigger:** `compaction.isOverflow({ tokens, model })` checks against `usable({ cfg, model, outputTokenMax })` — when the next turn would overflow the model's context window, a `compaction` task is queued.
- **Algorithm:** LLM-based summarization of older turns (`buildPrompt()` from `@opencode-ai/core/session/compaction`), with:
  - **Tail preservation:** the most recent N turns (configurable) are kept verbatim after the summary, so the model retains working state.
  - **Tool output pruning:** old tool outputs shrunk to 2k chars (with spill to a sidecar file the model can re-read via `read`).
  - **Compaction-as-summary marker:** a special `compaction` part is inserted so future compactions know prior summaries exist (prevents double-summarization).
- **Recovery:** `KiloCompactionPayloadRecovery` (Kilo extension) handles provider payload-limit errors gracefully.
- **Preflight compaction** (Kilo extension, in `session/llm.ts`): before contacting the provider, estimate token count; if would-overflow, raise `PreflightError` to route through compaction first — avoids wasting a paid API call.

**Difference from Cline/Kilo:** Cline's "Context Condensing" is a simpler LLM-based summarization triggered by a % threshold. OpenCode's algorithm has **turn-based tail preservation, protected tools, and tool-output pruning with sidecar spill files** — a more surgical approach that preserves working state better.

### C.3 Sub-agents / Task Delegation

**Yes — first-class.** The `task` tool (source: `src/tool/task.ts`):

```ts
Tool.define("task", Effect.gen(function* () {
  return {
    description: "Launch a new agent to handle complex, multistep tasks autonomously.",
    parameters: Schema.Struct({
      description: Schema.String,        // 3-5 word summary
      prompt: Schema.String,             // the task
      subagent_type: Schema.String,      // which agent mode to spawn
      task_id: Schema.optional(Schema.String),  // resume prior task
      command: Schema.optional(Schema.String),
      background: Schema.optional(Schema.Boolean),  // async mode
    }),
    execute: ...
  }
}))
```

**Behavior:**
- **Foreground (default):** spawns a child session, runs the agent loop in it, returns the child's final assistant text wrapped as `<task id="..." state="completed"><task_result>...</task_result></task>`.
- **Background (`background: true`):** starts a `BackgroundJob`, returns immediately with a "task started" message. Parent is notified via event bus when complete. The model is explicitly instructed **not** to poll or sleep — just to continue with non-overlapping work.
- **Resume:** `task_id` from a prior (failed or incomplete) task continues the same child session instead of creating a new one.
- **Permission inheritance:** child sessions inherit a tightened permission ruleset via `deriveSubagentSessionPermission` (subagents get a restricted tool subset by default).

**Agents** are defined declaratively (`src/agent/agent.ts` `Agent.Info` schema):
```ts
{
  name, displayName?, description?, deprecated?,
  mode: "subagent" | "primary" | "all",
  native?, hidden?,
  topP?, temperature?, color?,
  permission: PermissionV1.Ruleset,
  model?: { providerID, modelID },
  variant?, prompt?, options,
  requirements?,
  steps?: number,   // max steps before forced stop
}
```

OpenCode ships with **two built-in agents** (a primary coder + a planner). Custom agents are loaded from config directories (`agent/*.md` files with YAML frontmatter).

---

## D. LLM Provider Abstraction

### D.1 Providers Supported

OpenCode uses the **Vercel AI SDK** + the **models.dev** catalog. From `BUNDLED_PROVIDERS` in `src/provider/provider.ts`:

| # | Provider | AI SDK package |
|---|---|---|
| 1 | AWS Bedrock | `@ai-sdk/amazon-bedrock` (+ Mantle variant) |
| 2 | Anthropic | `@ai-sdk/anthropic` |
| 3 | Azure OpenAI | `@ai-sdk/azure` |
| 4 | Google Gemini | `@ai-sdk/google` |
| 5 | Google Vertex AI | `@ai-sdk/google-vertex` |
| 6 | Vertex Anthropic | `@ai-sdk/google-vertex/anthropic` |
| 7 | OpenAI | `@ai-sdk/openai` |
| 8 | **OpenAI-compatible** | `@ai-sdk/openai-compatible` ← for custom endpoints |
| 9 | OpenRouter | `@openrouter/ai-sdk-provider` |
| 10 | xAI (Grok) | `@ai-sdk/xai` |
| 11 | Mistral | `@ai-sdk/mistral` |
| 12 | Groq | `@ai-sdk/groq` |
| 13 | DeepInfra | `@ai-sdk/deepinfra` |
| 14 | Cerebras | `@ai-sdk/cerebras` |
| 15 | Cohere | `@ai-sdk/cohere` |
| 16 | AI Gateway | `@ai-sdk/gateway` |
| 17 | Together AI | `@ai-sdk/togetherai` |
| 18 | Perplexity | `@ai-sdk/perplexity` |
| 19 | Vercel | `@ai-sdk/vercel` |
| 20 | Alibaba (Qwen) | `@ai-sdk/alibaba` |
| 21 | GitLab AI | `gitlab-ai-provider` |
| 22 | GitHub Copilot | `@opencode-ai/core/github-copilot/copilot-provider` |
| 23 | Venice AI | `venice-ai-sdk-provider` |
| 24 | "opencode" (built-in free tier) | custom loader |

Per OpenCode docs: **75+ providers via models.dev catalog**, plus **local models** (Ollama, LM Studio, etc. — via OpenAI-compatible baseURL).

### D.2 Provider Interface

Each provider is represented by an `Info` (config) + a `CustomLoader` (runtime adapter):

```ts
type CustomLoader = (provider: Info) => Effect.Effect<{
  autoload: boolean,                          // load on startup?
  getModel?: (sdk, modelID, options?, model?) => Promise<LanguageModelV3>,
  vars?: (options) => Record<string, string>,  // env var resolution
  options?: Record<string, any>,                // provider-level options (headers, baseURL, etc.)
  discoverModels?: () => Promise<Record<string, Model>>,  // dynamic model list
}>
```

The engine calls `provider.getLanguage(model)` (returns `LanguageModelV3` from AI SDK) and passes that to `streamText()`. The model's `streamText` invocation supports:
- `temperature`, `topP`, `topK`, `maxOutputTokens`, `providerOptions`, `headers`, `maxRetries`, `abortSignal`, `toolChoice` (auto/required/none), `activeTools`, `experimental_repairToolCall`, `allowSystemInMessages`.

### D.3 Custom Models

**Yes — three ways:**

1. **OpenAI-compatible endpoint** (the easiest path): in `opencode.json` `provider` section, declare a provider with `npm: "@ai-sdk/openai-compatible"`, `baseURL`, `apiKey`, and a `models` map. Auth via `opencode auth login` (select "Other") or env var. Confirmed by official docs and issue #5674.
2. **Custom loader (`custom()` map in `provider.ts`)**: write a TypeScript adapter that returns `getModel`, `vars`, `options`, `discoverModels`. This is how OpenCode itself implements the `opencode`, `anthropic`, and other built-in custom loaders.
3. **Dynamic NPM install**: providers can be loaded at runtime by `npm install`-ing their `@ai-sdk/*` package (the `Npm` service handles this).

### D.4 Model-specific parameters

OpenCode handles this through:
- `ProviderTransform` (`src/provider/transform.ts`) — transforms per-model: applies anthropic-beta headers, provider-specific options, request timeout (`OPENAI_HEADER_TIMEOUT_DEFAULT = 300_000`), SSE read-timeout wrap, etc.
- `ProviderOptions` map (model-specific options like reasoning effort, prompt caching, etc.).
- `ProviderTransform.OUTPUT_TOKEN_MAX` — output token ceiling, computed from `model.limits.output` and the session's reported context size.
- Per-model `providerOptions` — passed through to AI SDK verbatim.

### D.5 Streaming

**Yes — full streaming throughout.** The LLM layer (`src/session/llm.ts`) uses `streamText()` from the AI SDK, which returns an async iterable of `LLMEvent`s. The `SessionProcessor` consumes this as an `effect/Stream`, emitting per-token `PartDelta` events over the bus, which the daemon pushes to clients over SSE/WebSocket. There is also a `LLMNativeRuntime` experimental path that bypasses AI SDK for providers that need direct HTTP streaming. Both runtimes converge on the same `LLMEvent` shape so downstream code is runtime-agnostic.

---

## E. File System + Sandboxing

### E.1 Filesystem Access

- The engine operates on a **per-instance working directory** (`Instance.state` keyed by directory + worktree via `AsyncLocalStorage`).
- `FSUtil.Service` is the abstraction layer — provides `read`, `write`, `glob`, `watch`, etc. Multiple implementations can be swapped via Effect `Layer`s.
- The `Snapshot` service (`src/snapshot/index.ts`) uses **git** directly: it creates a separate gitdir at `~/.local/share/opencode/snapshot/<projectID>/<worktree-hash>` and uses the user's worktree as a git worktree. `track()` makes a commit (a checkpoint); `restore(hash)` checks out a prior commit; `diff(hash)` and `diffFull(from, to)` produce diffs. Retention: 7 days (`prune = "7.days"`).
- `Storage` service: filesystem-based JSON under `~/.local/share/opencode/storage/`. Keys are path arrays: `Storage.write(["session", projectID, sessionID], data)`.

### E.2 Folder Restriction

**Yes.** The permission system (`src/permission/index.ts` + the `external_directory` permission key) restricts file access:

- Rules are `{ permission: "external_directory", pattern: "<glob>", action: "allow" | "ask" | "deny" }`.
- The agent registers a default ruleset for each instance:
  ```ts
  readonlyExternalDirectory = {
    "*": "ask",
    ...whitelistedDirs.map(dir => [dir, "allow"]),
  }
  ```
  Whitelisted dirs include: skill dirs, `${tmp}/*`, global config dirs, project dirs, etc.
- Any file outside the worktree + whitelist triggers `Permission.ask()` → user must approve (or `deny` always).
- The `external_directory` permission has its own evaluator (`ExternalDirectoryPermission.evaluate`) that handles path normalization edge cases.

**This means we can hard-pin the agent to a user-selected folder by overriding the default ruleset to `deny` everything outside that folder.** This is exactly the Android sandbox requirement.

### E.3 File Edits

Two editing tools, mutually exclusive per turn (selected by `KiloToolRegistry.usePatch(input)` based on model family):

- **`edit`** — exact string replacement. Requires prior `read` of the file (tracked in session state). Fails atomically if `oldString` not found.
- **`apply_patch`** — unified-diff-style patch with `*** Begin Patch` / `*** End Patch` envelope, supporting multi-file operations. Preferred for some models (Claude).

Both go through the same `Tool.Context.ask()` permission flow, and both are tracked by `Snapshot.track({ messageID })` so they're revertible via `Snapshot.revert(patches)` or `restore(hash)`.

---

## F. Background Execution

### F.1 Daemon Mode

**Yes, first-class.** `opencode serve` is documented at https://opencode.ai/docs/server:

> *"The `opencode serve` command runs a headless HTTP server that exposes an OpenAPI endpoint that an opencode client can use."*

Source: `src/cli/cmd/serve.ts` — minimal handler:

```ts
ServeCommand.handler:
  1. Import Server module (lazy)
  2. Warn if KILO_SERVER_PASSWORD unset (unsecured)
  3. Resolve network options (hostname, port, --network flag)
  4. Server.listen(opts)  -> Hono-on-effect HttpServer bound to {port, hostname}
     - Default port fallback: 4096, then any free port
     - mDNS publish (if hostname != loopback) for LAN discovery
     - WebSocket tracker installed
     - Per-instance context loaded per-request via x-kilo-directory header
  5. Signal handlers (SIGTERM/SIGINT/SIGHUP) -> graceful shutdown:
     - Drain KiloSessions ingest
     - Dispose all InstanceRuntime instances
     - server.stop(true)  (force-close active sockets)
```

The server runs **multiple project instances in one process**, keyed by the `x-kilo-directory` request header (workspace routing middleware in `server/routes/instance/httpapi/middleware/workspace-routing.ts`). This is exactly what an Android app needs: one long-lived background process hosting multiple workspaces.

### F.2 Long-Running Tasks + Background Jobs

The `BackgroundJob` service (`src/background/job.ts` + `@opencode-ai/core/background-job`):

```ts
interface Interface {
  list(): Effect<Job[]>
  get(id): Effect<Job | undefined>
  start(input: StartInput): Effect<Job>          // launch async job
  extend(input: ExtendInput): Effect<WaitResult> // add input to running job (resume)
  wait(input: WaitInput): Effect<WaitResult>     // await completion
  waitForPromotion(id): Effect<Job>              // wait until job becomes foreground
  promote(id): Effect<void>                      // foreground a background job
  cancel(id): Effect<void>                       // kill job
}
```

The `task` tool with `background: true` starts a `BackgroundJob` that runs the child agent loop. **This is a real task queue with promotion semantics** — a background agent can be foregrounded later if the user wants to interact with it.

### F.3 Task Queue

Yes — `BackgroundJob` is the queue. It's instance-scoped (one queue per project/workspace). Jobs have statuses (`running`, `completed`, `error`, `cancelled`). The `task` tool emits `<task id="..." state="running|completed|error">` markers so the model can distinguish states. The `KiloTaskBackgroundProcess` extension (Kilo) adds proper lifecycle hooks.

There's also a `prompt-queue` (`KiloSessionPromptQueue`) that queues user prompts within a session — if a user sends multiple prompts while the agent is running, they queue up and are processed in order.

---

## G. Android Portability Assessment

### G.1 Minimal Viable Subset to Port

| Component | Required? | Notes |
|---|---|---|
| **Engine core** (`@opencode-ai/core` Effect layers, `Instance.state`, `Bus`, `EventV2`) | ✅ Required | Pure TS, runs on Bun/Node. **Port as-is** to a background service. |
| **Session + LLM** (`session/`, `provider/`) | ✅ Required | Pure TS, network-only. **Port as-is**. |
| **Agent loop** (`session/prompt.ts` + `processor.ts` + `compaction.ts`) | ✅ Required | Pure TS orchestration. **Port as-is**. |
| **Tool registry** (`tool/registry.ts`, `tool/tool.ts`) | ✅ Required | Pure TS. **Port as-is**. |
| **Tools: read, write, edit, glob, apply_patch, task, todowrite, plan, skill, question, webfetch, websearch, invalid** | ✅ Required | All pure JS, no system deps. **Port as-is**. |
| **Tool: grep** | ⚠️ Optional | Needs ripgrep binary for aarch64-android. Termux ripgrep works. Or fall back to JS regex walker. |
| **Tool: shell (bash)** | ⚠️ Reimplement | Android has no bash. Options: (a) Termux `sh`/`mksh`, (b) custom command runner with whitelisted ops, (c) drop entirely and rely on file tools. |
| **Tool: lsp** | ❌ Drop | No LSP servers on Android by default. |
| **Tool: code-mode (experimental)** | ❌ Drop | MCP-over-code DSL, not needed for MVP. |
| **Provider abstraction** | ✅ Required | Port as-is. Essential for "custom LLM models" requirement. |
| **HTTP server + SDK** (`@opencode-ai/server`, `@opencode-ai/sdk`) | ✅ Required | This is the **Android UI ↔ engine bridge**. Run server inside the Android app process; UI talks over localhost HTTP/SSE. |
| **MCP client** | ✅ Keep (optional) | `@modelcontextprotocol/sdk` works in pure JS. Useful for extensibility. |
| **ACP server** | ❌ Drop | IDE-only protocol; not needed on Android. |
| **Snapshot/checkpoint** (git-based) | ⚠️ Optional | Needs a git binary. Termux git works. Or reimplement on a simple content-addressed store. |
| **TUI** (`@opentui/solid`, `cli/cmd/tui/`, `cli/cmd/run/`) | ❌ Drop | Android uses its own native UI (Jetpack Compose) talking to the daemon over HTTP. |
| **CLI yargs commands** | ⚠️ Keep `serve` only | The Android app launches `opencode serve` programmatically; the rest of the CLI is unused. |
| **Plugin discovery** (`Plugin.Service`) | ✅ Keep | Useful for user customization. |
| **Skill discovery** (`Skill.Service`) | ✅ Keep | Pure file-loading; great for customization. |
| **Permission system** | ✅ Required | **Critical** for the sandboxed-folder requirement. Default ruleset: deny everything outside the user-selected folder. |
| **Background jobs** | ✅ Required | Essential for the "runs in background" requirement. |
| **mDNS publish** | ❌ Drop | Not needed on Android (app is local). |
| **OAuth flows for providers** | ⚠️ Partial | Some providers need browser OAuth. On Android, use `CustomTabsIntent` to handle the callback. |
| **OpenTelemetry** | ❌ Drop | Not needed for MVP. |
| **Storage (filesystem JSON)** | ✅ Keep | Maps to Android app-private storage. |
| **SQLite (drizzle)** | ✅ Keep | `effect-sqlite-node` needs swapping for an Android SQLite binding, but drizzle-orm itself is portable. |

**Minimum viable port = engine + HTTP server + provider abstraction + 12 core tools + permission system + background jobs + storage.** Estimated ~70-80% of the OpenCode engine codebase can be reused unmodified.

### G.2 What MUST Be Dropped

1. **TUI** (SolidJS/OpenTUI terminal UI) — replaced by native Android UI.
2. **CLI yargs subcommands** except `serve` (which is launched programmatically).
3. **ACP server** (`src/acp/`) — IDE-only protocol, no Android use case.
4. **mDNS publish** (`src/server/mdns.ts`) — no LAN use case (unless we want desktop pairing later).
5. **LSP tool** — no LSP servers on Android.
6. **JetBrains / VS Code extension packages** — irrelevant on Android.
7. **Desktop app packaging** — irrelevant.
8. **GitHub PR / GitHub CLI integration** (`src/cli/cmd/github.ts`, `pr.ts`) — drop or make optional.
9. **Cloud sync / Kilo Gateway** — Kilo-specific, drop.
10. **OpenTelemetry / OTLP exporters** — drop for MVP.
11. **Tree-sitter parsers for shell output** (`tree-sitter-bash`, `tree-sitter-powershell`) — drop unless shell tool is kept.

### G.3 What MUST Be Reimplemented

1. **Android process host** — a Kotlin/Java foreground service that spawns the Bun/Node runtime running `opencode serve`. (Or, alternatively, port the engine to a JS runtime that runs natively on Android — e.g., via Termux's `node` package, or Hermes/React Native's JS engine, or `bun-android` if it matures.)
2. **Native UI layer** (Jetpack Compose) that consumes the HTTP/SSE API exposed by the daemon — using the auto-generated `@opencode-ai/sdk` TypeScript types as a contract (could be codegen'd to Kotlin via the OpenAPI spec at `/doc`).
3. **Filesystem layer** — Android Storage Access Framework (SAF) or scoped storage for the user-selected folder. The `FSUtil.Service` needs an Android-aware implementation that maps SAF URIs to paths the engine understands.
4. **Shell tool** — either drop, or wrap Termux's `sh`, or implement a custom command-runner with whitelisted operations.
5. **OAuth callback handler** — Android `CustomTabsIntent` + deep link back into the app.
6. **Foreground service notification** + wakelock management for background agent execution.
7. **Permission UI** — Android dialogs for `Permission.ask()` events (instead of terminal prompts).
8. **Git binary** (for snapshots) — bundle Termux git, or skip snapshots for MVP.
9. **Config file location** — `~/.config/opencode/` → Android app-private storage.
10. **ripgrep binary** (for `grep` tool) — bundle Termux ripgrep, or fall back to JS regex.

### G.4 Estimated Effort per Component

| Component | Effort (engineer-weeks) | Notes |
|---|---|---|
| Engine bootstrap on Android (Bun/Node runtime, foreground service) | 2-3 wk | Hardest unknown: which JS runtime to ship. Termux node is heavy (~50MB); Hermes lacks Node APIs. |
| HTTP server binding to localhost | 0.5 wk | Already in OpenCode — just invoke `Server.listen({ hostname: "127.0.0.1", port: 0 })`. |
| Native UI shell (sessions list, chat, tool approval dialogs) | 4-6 wk | Pure Android work; SSE client + OpenAPI codegen. |
| FSUtil Android impl (SAF/scoped storage bridge) | 1-2 wk | Path-mapping is fiddly. |
| Provider config UI (custom model screen) | 1-2 wk | Form-driven editor for `opencode.json` provider section. |
| Permission UI + event subscription | 1 wk | Subscribe to `Permission.Event.*`, render dialogs. |
| Shell tool replacement | 1-2 wk | Either Termux integration or custom runner. |
| Snapshot/checkpoint integration | 1-2 wk | If keeping git; otherwise skip for MVP. |
| Background execution + wakelock + foreground-notif | 1 wk | Standard Android pattern. |
| OAuth flows for providers | 1 wk | Per-provider, but mostly boilerplate. |
| Config + onboarding flow | 1 wk | First-run setup. |
| Testing, hardening, perf | 2-3 wk | Always under-budgeted. |
| **Total MVP** | **~16-24 engineer-weeks** (~4-6 months for 1 engineer) |

### G.5 Key Risks for Android

1. **JS runtime on Android** — biggest unknown. Options:
   - **Termux + node**: works but ~80MB extra, requires user to install Termux, awkward UX.
   - **Hermes (RN's engine)**: lightweight but no Node APIs (`fs`, `child_process`, `http` server) — would need shimming.
   - **Bun on Android**: experimental, not production-ready as of 2026.
   - **QuickJS / embedded V8**: requires manual Node-compat layer.
   - **Recommendation:** ship a pre-built Node.js binary bundled inside the APK (Termux-style), accept the ~50MB size penalty. This is the lowest-risk path.
2. **Bun-specific dependencies** — the package.json shows `bun-pty`, `bunfig.toml`, `bun.lock`. These need replacing if not running on Bun.
3. **`@parcel/watcher` native bindings** — has prebuilt binaries for darwin/linux/win but **not for android-aarch64**. Need to either compile it for Android or fall back to `chokidar` (pure JS).
4. **`@lydell/node-pty` native bindings** — same issue.
5. **`ripgrep` binary** — no prebuilt android-aarch64 binary; needs cross-compile or Termux.
6. **`simple-git` + `git` CLI** — same; Termux git works.
7. **Effect-TS learning curve** — the entire engine is built on Effect (Layer/Context.Service/Effect/Schema). Anyone porting needs Effect fluency. Mitigated by reusing the engine as-is (not rewriting).
8. **Foreground service lifecycle** — Android aggressively kills background services. Need a robust foreground notification + restart-on-boot + wakelock strategy.
9. **Network access for LLM providers** — Android network security config must allow cleartext to localhost (for SSE) + HTTPS to provider hosts. Standard.
10. **Provider OAuth** — many providers (Anthropic, GitHub Copilot) use OAuth with browser redirect. On Android, `CustomTabsIntent` + deep link. Some providers' OAuth flows may not work on mobile.
11. **APK size** — bundling Node + ripgrep + git + JS deps will push the APK to ~80-120MB. Acceptable for a power-user tool, but worth flagging.
12. **Cold-start latency** — Node + Effect-TS app + HTTP server boot on Android may take 1-3 seconds. Need a splash + loading state.
13. **Concurrent sessions / multiple workspaces** — the daemon supports this natively (via the `x-kilo-directory` header), but Android memory pressure may limit how many concurrent sessions are practical. Need a session eviction policy.

---

## H. Feature Highlights to Adopt

### H.1 OpenCode's BEST Features (to adopt)

1. **Daemon-first architecture** — the engine runs as a long-lived process exposing HTTP/SSE/WebSocket. **This is the single most important architectural decision for our Android app.** It decouples UI from engine, enables background execution, and allows multiple UIs (phone, tablet, watch, web).
2. **Effect-TS layered DI** — every subsystem is a `Context.Service` provided by a `Layer`, composable and testable. Makes mocking FS, network, and providers trivial.
3. **`Tool.define` + `ToolRegistry.tools({ model, agent, permission, network })`** — declarative tool definition with per-turn visibility filtering. Excellent for sandboxing (hide dangerous tools for certain agents/permissions).
4. **Permission system with `allow/ask/deny` + wildcard patterns** — fine-grained, user-overridable, with persistent "always allow" rules. **Critical for the sandboxed-folder requirement.**
5. **Auto-compaction with tail preservation + tool-output pruning** — more sophisticated than Cline's. Preserves working state better.
6. **`task` tool with `background: true`** — sub-agents can run asynchronously, parent is notified on completion. **Directly satisfies "multiple workspaces, runs in background".**
7. **`BackgroundJob` service** — start/extend/wait/promote/cancel. Real task queue with promotion semantics.
8. **OpenAPI auto-generated SDK** — the daemon emits its OpenAPI spec at `/doc`; a TypeScript SDK (`@opencode-ai/sdk`) is auto-generated. We can codegen a Kotlin SDK from the same spec. **This is the Android↔engine contract.**
9. **Filesystem-discovered custom tools** — drop a `.js`/`.ts` file in `tool/` dir and it's auto-registered. **Perfect for user customization.**
10. **Skills** — declarative markdown instruction packs loaded on demand. Great for "easy to use, customizable".
11. **Plugin system** — npm-installable extensions with tool definitions, agent definitions, hooks.
12. **MCP client (stdio + SSE + HTTP + OAuth)** — full MCP support means the agent can use any MCP server (database, browser, file system, etc.).
13. **Provider-agnostic via Vercel AI SDK + models.dev** — 75+ providers, OpenAI-compatible custom endpoints, custom loaders. **Directly satisfies "custom LLM models".**
14. **Snapshot/checkpoint system** (git-based) — revert any tool's effect. Important safety feature.
15. **ACP support** — bonus: the same engine can be driven from Zed or other ACP-speaking editors. (Not directly Android-relevant, but shows protocol maturity.)
16. **`InvalidArgumentsError` → "rewrite the input"** — when the model's tool call fails schema validation, the error is fed back as the tool result so the model retries. **Smart UX, free.**
17. **Auto-truncation with sidecar spill** — long tool outputs auto-truncated to a sane size and spilled to a temp file the model can re-read via `read`. Prevents context bloat.
18. **Session resume / fork** — sessions are resumable (`--continue`), forkable (`--fork`), exportable. Aligns with "multiple workspaces" requirement.
19. **mDNS LAN discovery** — could be repurposed later for desktop ↔ Android pairing.
20. **Worktree-based sessions** — sessions are tied to git worktrees, enabling parallel branches of work in the same project.

### H.2 What OpenCode has that Cline + Kilo Code don't

- **A real daemon (`opencode serve`)** with HTTP REST + SSE + WebSocket + OpenAPI + auto-generated SDK. (Kilo inherited this; Cline never had it.)
- **ACP (Agent Client Protocol) support** — OpenCode speaks ACP natively, so it can be driven by Zed. Cline doesn't.
- **mDNS LAN discovery** — bonjour-service publishing; a daemon on the LAN can be discovered by clients. Neither Cline nor Kilo exposes this (Kilo inherited but doesn't expose it the same way).
- **`BackgroundJob` with `promote`/`extend`/`waitForPromotion` semantics** — true task queue with promotion. Cline has no concept of this.
- **Filesystem-discovered custom tools** (drop a `.ts` file, get a tool). Cline requires editing the extension's source.
- **Vercel AI SDK as the unifying abstraction** — Cline uses individual provider SDKs (`@anthropic-ai/sdk`, `openai`) with hand-rolled glue; OpenCode/Kilo use AI SDK uniformly, getting 75+ providers for free.
- **`models.dev` integration** — OpenCode pulls its model catalog from models.dev (an external community-maintained registry), so new models appear without code changes.
- **Effect-TS throughout** — typed errors (`NamedError`, `InvalidArgumentsError`), structured errors with Zod schemas, layered DI, `AsyncLocalStorage`-based per-instance state. Cline uses plain try/catch.
- **ACP `loadSession` / `forkSession`** — sessions can be loaded and forked via the IDE protocol.
- **Plan mode as a first-class agent** — `plan_enter`/`plan_exit` tools + a dedicated planner agent. Cline has "Plan Mode" too but it's a setting, not a separate agent.
- **OpenAPI spec at `/doc`** — the daemon publishes its full API spec; clients can be code-generated for any language. Cline has no API.

### H.3 What's Unique About OpenCode's Architecture

- **CLI-first / daemon-first**: the engine is a long-lived process. CLI commands are thin clients of the daemon. IDE extensions are thin clients of the daemon. The desktop app is a thin client of the daemon. **One engine, many surfaces.**
- **Protocol-first**: HTTP REST + SSE + WebSocket + OpenAPI + ACP + MCP. Every external interaction is a typed protocol.
- **Effect-TS functional core**: the entire engine is Effect `Layer`s and `Context.Service`s. `Instance.state()` provides per-directory lazy singletons via `AsyncLocalStorage`. Testable, mockable, composable.
- **`fn(schema, callback)` pattern**: every exported function is wrapped with Zod/Effect-Schema input validation. Structured errors (`NamedError.create(name, schema)`) over raw throws.
- **`BusEvent.define(type, schema)` + `Bus.publish()`**: in-process pub/sub for cross-module communication, bridged to the SSE/WebSocket layer for external clients.
- **Snapshot/checkpoint = git worktree**: instead of inventing a snapshot format, OpenCode uses git itself (separate gitdir + worktree).battle-tested, diffable, revertible.
- **Per-instance `x-kilo-directory` header**: one daemon, many projects, switched per request. No need to spawn a process per workspace.

---

## I. Custom Model Support

### I.1 Custom Model Configuration

Models live in **`opencode.json`** (project-local) or **`~/.config/opencode/opencode.json`** (global). The `provider` section declares providers + models:

```jsonc
{
  "provider": {
    "my-custom": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "My Custom Endpoint",
      "options": {
        "baseURL": "https://api.my-endpoint.com/v1",
        "apiKey": "{env:MY_API_KEY}"
      },
      "models": {
        "my-model-v1": {
          "name": "My Model v1",
          "limit": { "context": 128000, "output": 4096 },
          "cost": { "input": 1, "output": 2 }
        }
      }
    }
  }
}
```

Auth is added via `opencode auth login` → select "Other" → enter API key (stored in `~/.local/share/opencode/auth.json` mode 0600). Or via env vars (`{env:MY_API_KEY}` interpolation in config).

### I.2 OpenAI-Compatible Endpoints

**Yes, natively** via `@ai-sdk/openai-compatible`. This is the primary path for self-hosted models (vLLM, LM Studio, Ollama, LiteLLM, llama.cpp server, etc.). Confirmed by:
- Official docs (`opencode.ai/docs/providers`): "Any OpenAI-compatible endpoint becomes a custom provider via npm + baseURL + models".
- Issue #5674 (Dec 2025): user adding a custom OpenAI-compatible provider in `opencode.json`.
- Source code: `BUNDLED_PROVIDERS["@ai-sdk/openai-compatible"]` in `src/provider/provider.ts`.

### I.3 Model-Specific Parameters

The `ProviderTransform` service (`src/provider/transform.ts`) applies per-model transforms:
- Anthropic beta headers (`interleaved-thinking`, `fine-grained-tool-streaming`).
- Provider-specific request timeout (default 300s, SSE read-timeout wrap).
- Output token cap based on `model.limits.output` and reported context size.
- `providerOptions` pass-through — model-specific options (reasoning effort, prompt caching, etc.) flow verbatim to AI SDK.

Per-model config in `opencode.json`:
```jsonc
"models": {
  "model-id": {
    "name": "Display Name",
    "limit": { "context": 128000, "output": 4096 },
    "cost": { "input": 1, "output": 2 },
    "options": { /* arbitrary provider options */ },
    "variant": "reasoning"  // optional
  }
}
```

The `variant` field (e.g., "reasoning", "fast") lets the UI group model variants. The agent's `model` field can also specify a variant.

---

## J. Key Question: Is OpenCode a Better Backbone Than Cline?

### J.1 Architecture Comparison for Android

| Requirement | Cline (VSCode ext) | OpenCode (daemon) | Winner |
|---|---|---|---|
| **Works inside user-selected folder** | Hard — extension runs in workspace, but workspace = VS Code workspace | Easy — `Instance.state({ directory, worktree })` keyed per request via `x-kilo-directory` header | **OpenCode** |
| **Multiple workspaces** | One workspace per VS Code window | One daemon, multiple instances, switched per request | **OpenCode** |
| **Background execution** | Impossible — tied to extension lifecycle | Native — `opencode serve` + `BackgroundJob` + foreground service | **OpenCode** |
| **Custom LLM models** | Possible but per-SDK glue | First-class — AI SDK + OpenAI-compatible + custom loaders | **OpenCode** |
| **Highly customizable** | Limited — extension config | Excellent — filesystem tools, skills, plugins, MCP | **OpenCode** |
| **Easy to use** | Requires VS Code | Just needs an HTTP client | **OpenCode** (after UI built) |
| **Flexible** | Rigid (VS Code shape) | Flexible (any client of the daemon) | **OpenCode** |
| **Android portability** | Requires ripping out VS Code APIs | Requires only the engine + a Kotlin UI | **OpenCode** |

**Verdict: OpenCode is unambiguously the better backbone for a dedicated Android agent app.** Its daemon-first, protocol-first architecture is purpose-built for exactly this kind of UI/engine decoupling. Cline's VS Code extension shape would need to be ripped apart and re-hosted, losing most of its value.

### J.2 Headless (No UI) Execution

**Yes, fully headless.** Three modes:

1. **`opencode serve`** — long-lived HTTP daemon, no UI. Clients connect over HTTP/SSE/WebSocket. This is what an Android app would launch in a foreground service.
2. **`opencode run "<task>"`** — one-shot: send a single prompt, stream events to stdout, exit on idle. (Used in CI / scripting.)
3. **`opencode run --format json`** — raw event stream as JSON lines. (Used for programmatic consumption.)

Source: `src/cli/cmd/serve.ts` and `src/cli/cmd/run.ts`. The `run` command's three modes (non-interactive, interactive local `--mini`, interactive attach `--mini --attach`) cover all headless use cases.

### J.3 Protocol / API for Android UI

**Yes — three complementary protocols, all native:**

1. **HTTP REST + OpenAPI** at `/doc`:
   - Auto-generated from `effect/unstable/httpapi` route definitions.
   - Route groups: `instance`, `tui`, `file`, `sync`, `query`, `pty`, `control-plane`, `session`, `workspace`, `global`, `control`, `project-copy`, `metadata`, `provider`, `config`, `permission`, `event`, `question`, `mcp`.
   - A Kotlin SDK can be code-generated from the OpenAPI spec (using e.g. `openapi-generator-cli` with the `kotlin` generator).
2. **SSE (Server-Sent Events)** for streaming:
   - Real-time `Session.Event.*` (message part deltas, tool calls, completions), `Permission.Event.*` (approval requests), `BackgroundJob.Event.*`, `MCP.Event.*`, etc.
   - The Android UI subscribes to the SSE stream and renders events as they arrive.
3. **WebSocket** (the `WebSocketTracker` service):
   - For bidirectional needs (rare; mostly PTY terminal sessions).
4. **ACP (Agent Client Protocol)** — the JSON-RPC protocol used by Zed. **Android could speak ACP over a local socket**, but the HTTP API is more idiomatic for a mobile UI.

**The auto-generated `@opencode-ai/sdk` TypeScript SDK is the reference implementation.** It already implements all the typed clients (`KiloClient`, `Session`, `ToolPart`, etc.). A Kotlin port can follow the same shape.

---

## K. Summary Table — Android Suitability Scorecard

| Requirement (from project brief) | OpenCode capability | Score (1-5) |
|---|---|---|
| Works inside user-selected folder | `Instance.state` keyed by directory + `external_directory` permission ruleset | **5** |
| Creates multiple workspaces | Daemon hosts multiple directory-keyed instances; workspace routes in server | **5** |
| Runs in background | `opencode serve` daemon + `BackgroundJob` service + foreground service (Android) | **5** |
| Supports custom LLM models | AI SDK + models.dev + OpenAI-compatible + custom loaders + `opencode.json` provider section | **5** |
| Highly capable | 17 built-in tools + MCP + plugins + skills + sub-agents + plan mode | **5** |
| Customizable | Filesystem tools + plugins + skills + custom agents + config | **5** |
| Flexible | Effect-TS layered DI, every subsystem swappable | **5** |
| Easy to use | Requires UI work, but the engine API is clean | **3** (UI not built yet) |
| **Overall** | | **4.75 / 5** |

---

## L. Recommendations

1. **Use OpenCode (anomalyco/opencode, MIT) as the engine backbone**, not Cline. The daemon-first architecture is purpose-built for our use case.
2. **Do NOT fork OpenCode** — fork or vendoring complicates upgrades. Instead, depend on `@opencode-ai/core`, `@opencode-ai/server`, `@opencode-ai/llm`, `@opencode-ai/sdk`, `@opencode-ai/protocol`, `@opencode-ai/schema`, `@opencode-ai/tui` (skip TUI) as npm packages, and write a thin Kotlin/Kotlin-Multiplatform UI that talks to the daemon over HTTP/SSE.
3. **Run the daemon inside the Android app process** via a bundled Node.js runtime (Termux-style, ~50MB penalty). The Android UI binds to `127.0.0.1:<port>` for all engine calls.
4. **Codegen the Kotlin SDK from the OpenAPI spec** at `/doc` — keeps the contract authoritative and auto-updates with OpenCode releases.
5. **Default the `external_directory` permission to `deny *`** outside the user-selected folder; add per-folder `allow` rules only on explicit user action.
6. **Drop the TUI, ACP server, LSP tool, mDNS, GitHub/PR integrations, OpenTelemetry, JetBrains/VSCode extension packages** for the MVP. Keep everything else.
7. **Keep MCP client support** — it's a free extensibility story (any MCP server works).
8. **Plan a future "desktop pairing" mode** using the existing mDNS code — let the Android app expose itself as a server that a desktop browser can connect to (or vice versa). Already 80% built.
9. **Investigate Bun-on-Android maturity** — if it stabilizes, swapping Node for Bun would dramatically reduce APK size and improve cold-start. Track the `bun-android` issue.
10. **For shell access**, prefer Termux integration (the user installs Termux separately and the app delegates shell commands to it). This avoids bundling a shell emulator and respects Android's security model.

---

## M. Open Questions for Next Research Phase

1. **What does the OpenAPI spec at `/doc` actually look like in detail?** Need to fetch and inspect to assess Kotlin SDK codegen quality. → Next task: curl `opencode serve` locally and capture the spec.
2. **Which providers' OAuth flows break on Android?** Need to test each major provider (Anthropic, GitHub Copilot, Google Vertex, Bedrock) on a real Android device with `CustomTabsIntent`.
3. **Can `@parcel/watcher` be cross-compiled for android-aarch64?** If not, what's the perf cost of `chokidar` for large repos?
4. **What's the actual cold-start time** of Node + Effect-TS + OpenCode engine on a mid-range Android device? Need to benchmark.
5. **Is there a path to Bun-on-Android?** Track https://github.com/oven-sh/bun/issues for Android target support.
6. **Does the `@opencode-ai/sdk` TypeScript SDK cleanly port to Kotlin Multiplatform?** Or do we need to write a native Kotlin client from the OpenAPI spec?
7. **How do Kilo's `kilocode_change` deltas affect porting?** Are there Kilo-only features we'd want to backport (e.g., the `KiloSessionOverflow` preflight compaction)?

---

## N. References

- **OpenCode repo:** https://github.com/anomalyco/opencode (formerly sst/opencode)
- **OpenCode docs:** https://opencode.ai/docs
- **Server docs:** https://opencode.ai/docs/server — confirms `opencode serve` headless daemon + OpenAPI
- **Providers docs:** https://opencode.ai/docs/providers — confirms AI SDK + models.dev + 75+ providers + custom OpenAI-compatible
- **Local source:** `references/kilocode/packages/opencode/` (Kilo Code fork — preserves OpenCode engine with `kilocode_change` markers)
- **Kilo Code analysis (R-A2):** `references/AGENT-TECH/research/R-A2-kilocode-analysis.md`
- **Cline analysis (R-A1):** `references/AGENT-TECH/research/R-A1-cline-analysis.md`

---

**End of R-A3.**
