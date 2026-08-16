# R-A1 — Cline Deep Analysis for Android Agent Port

> **Task ID:** R-A1
> **Agent:** general-purpose (Cline analysis for dedicated Android agent app)
> **Date:** session-start (sandbox-fresh)
> **Scope:** Source-level deep analysis of the Cline monorepo for porting to a dedicated
> Android agent application (Kotlin + Compose + WorkManager + SAF). Research only —
> no code is written.
> **Primary source:** Local shallow clone of `github.com/cline/cline` @ commit
> `8bbdde2` (`feat(llms): add model-driven image generation (#13025)`), full source
> tree on disk at `repo/AGENT-TECH/references/cline/`.
> **Prior research:** R-1 (`android-project/research/R-1-cline-agent.md`, 1310 lines,
> web-based — no clone). R-A1 supersedes R-1 wherever they disagree; the local source
> is the source of truth. Disagreements are flagged inline with **(SUPersedes R-1)**.

---

## 0. TL;DR — the 30-second summary

Cline is an **Apache 2.0** licensed open-source AI coding agent, ~97% TypeScript
(~560 KLOC monorepo, Bun toolchain, Node ≥22 runtime). It has been **substantially
refactored** since R-1's snapshot: the famous 3756-line recursive `Task.ts` is
**gone**, replaced by a 2096-line **iterative** `AgentRuntime` (`while` loop with
`maxIterations` + `AbortController`). The codebase is now organized as a layered
SDK (`@cline/shared → @cline/llms → @cline/agents → @cline/core`) + product apps
(`apps/vscode`, `apps/cli`, `apps/examples/{quickstart, cli-agent, code-review-bot,
menubar}`).

**Two parallel tool surfaces exist:**

1. **SDK default tools (9)** — `read_files`, `search_codebase`, `run_commands`,
   `fetch_web_content`, `editor`, `apply_patch`, `skills`, `ask_question`,
   `submit_and_exit`. Cleanly defined in
   `sdk/packages/core/src/extensions/tools/`. Each is a `createTool()` factory
   parametrized by an injected `executor` function — **the executor is the only
   platform-specific part**. This is the layer to port.
2. **Legacy VSCode tools (26 in `ClineDefaultTool` enum)** — includes terminal,
   browser_action (Puppeteer), MCP stdio, plan/act mode, focus_chain todo list,
   condense, etc. Most are NOT portable.

**Key portability insight:** Cline's SDK tool factory pattern (`createTool` +
executor injection) is **almost perfectly** suited for Android. The 9 default
tools can be ported tool-by-tool, swapping each Node executor for a Kotlin/SAF
executor. The agent loop, hooks, compaction, provider abstraction, and approval
flow are all pure logic that ports cleanly.

**Cline has NO real sandboxing.** `.clineignore` is deprecated and explicitly
documented as "not a security or access-control boundary." The shell tool
(`run_commands`) is unrestricted. **Our Android app must enforce a real sandbox
ourselves** by (a) NOT providing a shell tool, (b) binding all executor functions
to a single user-selected SAF folder, (c) using the `beforeTool` hook pattern for
defense-in-depth path-prefix checks.

**Estimated effort:** ~XL (4–6 engineer-weeks) for a minimal-but-capable Android
port: ~5.8 KLOC Kotlin = ~2 KLOC porting Cline backbone + ~550 LoC porting Kilo
Code advanced patterns (per R-1) + ~3.3 KLOC original Android work (UI, lifecycle,
on-device streaming, design-system tools).

---

## A. Architecture overview

### A.1 Core architecture (verified from source)

Cline is structured as a **4-layer SDK + N product apps** monorepo:

```
                     ┌─────────────────────────────────────────┐
                     │            PRODUCT APPS                  │
                     │  apps/vscode    apps/cli    apps/examples│
                     │  (VS Code ext)  (TUI)       (quickstart, │
                     │                              cli-agent, │
                     │                              menubar…)  │
                     └────────────┬────────────────────────────┘
                                  │ depends on ↓
                     ┌────────────▼────────────────────────────┐
                     │            @cline/core                  │  ← Node-only stateful
                     │  session lifecycle · persistence · hub   │     orchestration
                     │  daemon · cron · local runtime host ·    │
                     │  tool executors (bash, file-read, etc.)  │
                     └────────────┬────────────────────────────┘
                                  │ depends on ↓
                     ┌────────────▼────────────────────────────┐
                     │            @cline/agents                │  ← browser-compatible
                     │  AgentRuntime (iterative while-loop)     │     stateless loop
                     │  7 hooks · tool dispatch · tool approval │
                     │  overflow recovery · completion policy  │
                     └────────────┬────────────────────────────┘
                                  │ depends on ↓
                     ┌────────────▼────────────────────────────┐
                     │            @cline/llms                  │  ← provider abstraction
                     │  ApiHandler interface · 50+ providers   │
                     │  Gateway · streaming · AI-SDK compat ·  │
                     │  model catalog (172 generated IDs)      │
                     └────────────┬────────────────────────────┘
                                  │ depends on ↓
                     ┌────────────▼────────────────────────────┐
                     │            @cline/shared                │  ← zero-deps types
                     │  AgentTool · AgentMessage · AgentModel · │
                     │  createTool · zodToJsonSchema · path    │
                     └─────────────────────────────────────────┘
```

**Key files (verified paths):**

| Concern | Path | LoC |
|---|---|---|
| `AgentTool` + `AgentModel` types | `sdk/packages/shared/src/agent.ts` | 640 |
| `createTool` factory | `sdk/packages/shared/src/tools/create.ts` | 131 |
| 9 default tool definitions | `sdk/packages/core/src/extensions/tools/definitions.ts` | 942 |
| Tool Zod schemas | `sdk/packages/core/src/extensions/tools/schemas.ts` | 352 |
| Tool executor types | `sdk/packages/core/src/extensions/tools/types.ts` | 376 |
| Tool runtime dispatch | `sdk/packages/core/src/extensions/tools/runtime.ts` | 279 |
| Tool presets (act/plan/yolo/…) | `sdk/packages/core/src/extensions/tools/presets.ts` | 192 |
| **Agent loop** (`AgentRuntime`) | `sdk/packages/agents/src/agent-runtime.ts` | **2097** |
| `ApiHandler` interface | `sdk/packages/llms/src/providers/handler.ts` | 91 |
| Provider factory `createHandler` | `sdk/packages/llms/src/providers.ts` | 134 |
| Gateway (model registry) | `sdk/packages/llms/src/providers/gateway.ts` | 383 |
| Provider list (50 built-ins) | `sdk/packages/llms/src/providers/ids.ts` | ~85 |
| Provider list (172 generated) | `sdk/packages/llms/src/providers/provider-ids.generated.ts` | ~210 |
| OpenAI-compatible vendor | `sdk/packages/llms/src/providers/vendors/openai-compatible.ts` | ~300 |
| Stream chunk types | `sdk/packages/llms/src/providers/stream.ts` | 129 |
| Basic compaction (deterministic) | `sdk/packages/core/src/extensions/context/basic-compaction.ts` | 711 |
| **Agentic compaction** (LLM summary) | `sdk/packages/core/src/extensions/context/agentic-compaction.ts` | 318 |
| Compaction thresholds + helpers | `sdk/packages/core/src/extensions/context/compaction-shared.ts` | 780 |
| Compaction dispatcher | `sdk/packages/core/src/extensions/context/compaction.ts` | 710 |
| Legacy `ClineDefaultTool` enum | `apps/vscode/src/shared/tools.ts` | 41 |
| Legacy `ApiProvider` type (49 providers) | `apps/vscode/src/shared/api.ts` | 242 |
| Legacy MCP hub (stdio+SSE+HTTP) | `apps/vscode/src/services/mcp/McpHub.ts` | 2196 |
| Legacy `FileContextTracker` (chokidar) | `apps/vscode/src/core/context/context-tracking/FileContextTracker.ts` | ~250 |
| `ClineIgnoreController` (deprecated) | `apps/vscode/src/core/ignore/ClineIgnoreController.ts` | — |

### A.2 Language

**TypeScript** confirmed. Toolchain per `AGENTS.md` (root):
> *"Toolchain is Bun 1.3.13 (package manager + task runner) with Node ≥22 as the
> runtime. Do not use npm/yarn/pnpm."*

SDK published as npm packages: `@cline/sdk` (umbrella), `@cline/core`,
`@cline/agents`, `@cline/llms`, `@cline/shared`. The `@cline/agents` package is
explicitly **browser-compatible** (per `sdk/packages/README.md` boundary rules) —
proof that an agent loop can be written without Node primitives.

### A.3 License + attribution

- **License:** Apache 2.0 (verified by reading root `LICENSE` — full Apache 2.0
  text, no NOTICE file present).
- **Copyright holder:** "Cline Bot Inc." (per R-1's verification; the LICENSE
  itself contains only the standard Apache boilerplate, no explicit copyright
  line — standard Apache 2.0 practice).
- **Attribution requirements (Apache 2.0 §4):**
  - Must retain the copyright + license notice in any redistributed source.
  - Must state any significant changes made to files.
  - **If we ship a NOTICE file**, must include any original attribution notices
    (none exist in the repo).
- **Practical posture for our Android app:** Treat Cline as **architectural
  inspiration only** — port the patterns to idiomatic Kotlin, do NOT copy source
  files verbatim. This keeps Apache 2.0 obligations minimal: ship a "Notices" /
  "Open Source Licenses" screen in the app crediting Cline + Apache 2.0 + link to
  `github.com/cline/cline`. **No "significant changes" notice required** because
  we are not redistributing modified Cline source — we are writing fresh Kotlin
  that mirrors Cline's design.
- **No viral/copyleft risk.** Apache 2.0 is permissive; our app can be any
  license (proprietary, MIT, etc.).

---

## B. Tool system analysis

### B.1 The TWO tool surfaces — IMPORTANT finding

Cline currently has **two parallel tool surfaces** that exist simultaneously:

1. **The SDK default tools** (`sdk/packages/core/src/extensions/tools/`):
   9 tools, factory-based, executor-injected, **portable**.
2. **The legacy VSCode tools** (`apps/vscode/src/shared/tools.ts` `ClineDefaultTool`
   enum + the executor implementations scattered across `apps/vscode/src/`):
   26 tools, many tightly coupled to Node/VS Code (terminal, Puppeteer, MCP stdio),
   **mostly NOT portable**.

The CLI (`apps/cli`) and SDK examples use the new SDK tools. The VS Code
extension (`apps/vscode`) still uses the legacy tools but is in the middle of a
migration. **For Android, we port the SDK surface and ignore the legacy surface
except where it teaches us what features users expect** (e.g., plan/act mode,
todos).

### B.2 The 9 SDK default tools — COMPLETE catalog with portability

> All schemas below are Zod schemas converted to JSON Schema via
> `zodToJsonSchema()` in `createTool()`. Source:
> `sdk/packages/core/src/extensions/tools/{definitions,schemas,types}.ts`.

#### Tool 1: `read_files`

| Field | Value |
|---|---|
| Name | `read_files` |
| Description | Read content of text/image files at absolute paths; supports per-file `start_line`/`end_line` ranges. Up to `MAX_READ_LINES` lines / `MAX_READ_OUTPUT_CHARS` chars per read. |
| Input schema | `{ files: Array<{ path: string, start_line?: number\|null, end_line?: number\|null }> }` — also tolerant of `{ file_paths: [...] }`, `{ paths: [...] }`, plain string, or array of strings. Coerces stringified line numbers. |
| Executor | `FileReadExecutor = (request, context) => Promise<string \| Array<TextContent\|ImageContent>>` |
| Timeout | 10s × 2 (per-file, parallel via `Promise.all`) |
| Retryable | yes (1 retry) |
| What it does | Reads a single file's content (or a line range) and returns it as text or as ImageContent if it's an image. |
| **Android portable?** | **Yes** — port as `suspend fun readFile(path, startLine?, endLine?): String` using `DocumentFile`/SAF or app-private cache copy. Line range slicing is trivial string splitting. |
| Android equivalent | `DocumentFile.findFile(name).uri.openInputStream()` → read text. For images, return base64 + mediaType. |

#### Tool 2: `search_codebase`

| Field | Value |
|---|---|
| Name | `search_codebase` |
| Description | Regex pattern search across the codebase. Multiple parallel queries. Output beyond `MAX_SEARCH_OUTPUT_CHARS` per query is middle-truncated. |
| Input schema | `{ queries: string[] }` (regex patterns). Tolerant of `{ queries: string }`, plain string, or array of strings. |
| Executor | `SearchExecutor = (query, cwd, context) => Promise<string>` |
| Timeout | 30s × 2 |
| Retryable | yes (1 retry) |
| What it does | Runs ripgrep (or equivalent) over the workspace, returns matching lines + file:line refs. |
| **Android portable?** | **Partial** — Node's ripgrep binary is not Android-portable. Three options: (a) bundle aarch64 ripgrep via JNI/GG nativeBinary — complex; (b) reimplement with Java regex walking `java.nio.file.Files.walk()` — slow on large dirs; (c) build an inverted-index of the workspace at open time and query it — fastest, but memory-heavy. **Recommended: option (b) for MVP, option (c) for v2.** |
| Android equivalent | `java.util.regex.Pattern` + `java.nio.file.Files.walk(getSafRoot())` — limit depth, parallelize via `Dispatchers.IO`, middle-truncate results. |

#### Tool 3: `run_commands` (shell)

| Field | Value |
|---|---|
| Name | `run_commands` |
| Description | Run non-interactive shell commands from workspace root. Output > `MAX_COMMAND_OUTPUT_CHARS` middle-truncated. Description is shell-aware (PowerShell vs bash vs cmd vs WSL). |
| Input schema | `{ commands: string[] }` or `{ commands: Array<{ command: string, args?: string[] }> }` (structured form for no-shell execution). Tolerant of plain string. |
| Executor | `ShellExecutor = (command \| {command, args}, cwd, context) => Promise<string>` |
| Timeout | 30s × 2 |
| Retryable | **no** (stateful, must not auto-retry) |
| What it does | Spawns a shell process, captures stdout/stderr, returns combined output. |
| **Android portable?** | **NO** — Android cannot spawn arbitrary shell processes (no `Runtime.exec("bash")`, no `/bin/sh` accessible from a sandboxed app). Even if we shelled out to `toybox`/`mksh` (Android's built-in shell), we have no `git`, `npm`, `python`, etc. |
| **Recommendation** | **DROP for MVP.** Provide instead a fixed set of safe pseudo-commands implemented in Kotlin: `list_dir`, `file_size`, `file_mtime`, `mkdir`, `move`, `delete`, `rename`, `git_status` (if user has Git installed locally via Termux-style integration — out of scope for MVP). The agent loses general shell power but the sandbox guarantee is rock-solid. |

#### Tool 4: `fetch_web_content`

| Field | Value |
|---|---|
| Name | `fetch_web_content` |
| Description | Fetch URL content + analyze with a prompt. Multiple parallel URLs. |
| Input schema | `{ requests: Array<{ url: string, prompt: string }> }` |
| Executor | `WebFetchExecutor = (url, prompt, context) => Promise<string>` |
| Timeout | 30s × 2 |
| Retryable | yes (2 retries) |
| What it does | HTTP GET on URL, extracts relevant content per the prompt (Cline's implementation actually invokes the LLM to analyze the fetched HTML). |
| **Android portable?** | **Yes** — straightforward with Ktor Client (`HttpClient` + `HtmlUnescaper`). The "analyze with prompt" step is an additional LLM call, which we can defer/short-circuit for MVP (just return stripped text content; the main agent will analyze it). |
| Android equivalent | `HttpClient.get(url).bodyAsText()` → strip HTML tags with Jsoup. |

#### Tool 5: `editor`

| Field | Value |
|---|---|
| Name | `editor` |
| Description | Controlled filesystem edits: insert at line, replace `old_text` with `new_text`, or create file if missing. **NOT SEARCH/REPLACE blocks** — direct `old_text` → `new_text` replacement. |
| Input schema | `{ path: string, old_text?: string\|null, new_text: string, insert_line?: number\|null }` |
| Executor | `EditorExecutor = (input, cwd, context) => Promise<string>` |
| Timeout | 30s |
| Retryable | no |
| What it does | Three modes: (1) `insert_line` set → insert `new_text` BEFORE that line; (2) file missing + no `old_text` → create with `new_text`; (3) otherwise → replace `old_text` with `new_text` (must match exactly once). |
| **Android portable?** | **Yes** — pure file I/O + string operations. Port as a single `suspend fun edit(path, oldText?, newText, insertLine?)`. |
| Android equivalent | `DocumentFile` write; for replace, read content → `String.replace(oldText, newText, ignoreCase=false)` (validate exactly one match) → write back. |

#### Tool 6: `apply_patch`

| Field | Value |
|---|---|
| Name | `apply_patch` |
| Description | Apply the canonical freeform apply_patch grammar (`*** Begin Patch / *** Update File: path / @@ context / -old / +new / *** End Patch`). Supports Add/Update/Delete/Move operations across multiple files in one call. |
| Input schema | `{ input: string }` (the patch text) — tolerant of plain string. |
| Executor | `ApplyPatchExecutor = (input, cwd, context) => Promise<string>` |
| Timeout | 30s |
| Retryable | no |
| What it does | Parses the apply_patch grammar (defined in `apply-patch-parser.ts`) and applies Add/Update/Delete/Move operations to one or more files atomically. |
| **Android portable?** | **Yes** — the patch parser is pure string processing. Port the parser (~200 LoC) verbatim to Kotlin, then run each operation via the same primitives as `editor`. |
| Android equivalent | Port `apply-patch-parser.ts` (PATCH_MARKERS + PatchChunk model) to Kotlin; reuse `editor`'s file I/O. |

#### Tool 7: `skills`

| Field | Value |
|---|---|
| Name | `skills` |
| Description | Invoke a configured skill (a.k.a. slash command) by name + optional args. Description dynamically lists available skills via a getter property. |
| Input schema | `{ skill: string, args?: string\|null }` |
| Executor | `SkillsExecutorWithMetadata` (callable + `configuredSkills?: SkillMetadata[]`) |
| Timeout | 15s |
| Retryable | no |
| What it does | Looks up the skill by name (skills are user-defined Markdown files in `.cline/skills/`), executes its body (which may itself be a prompt template + tool sequence), returns the result. |
| **Android portable?** | **Yes** — skills are just Markdown files; the executor reads the skill file, substitutes `args` into a template, and either returns the text or triggers a sub-agent run. All file I/O is portable. |
| Android equivalent | Read `workspace/.agent/skills/{name}.md`, parse frontmatter, template-substitute args, return expanded prompt. |

#### Tool 8: `ask_question`

| Field | Value |
|---|---|
| Name | `ask_question` |
| Description | Ask the user a single clarifying question with 2-5 selectable options. |
| Input schema | `{ question: string, options: string[2..5] }` |
| Executor | `AskQuestionExecutor = (question, options, context) => Promise<string>` |
| Retryable | no |
| What it does | Surfaces a question UI; returns the selected option (or "Other: …") back to the agent. |
| **Android portable?** | **Yes** — pure UI. In Android, post a `Notification` with a `PendingIntent` to a Question activity; the executor suspends (via `suspendCancellableCoroutine`) until the user picks. Backgrounded: enqueue a `WorkManager` worker that waits on a `StateFlow<Answer?>`. |
| Android equivalent | Compose `AlertDialog` (foreground) OR a `QuestionNotification` + `WorkManager` foreground worker (background). |

#### Tool 9: `submit_and_exit`

| Field | Value |
|---|---|
| Name | `submit_and_exit` |
| Description | Submit the final answer + exit the run. Has `lifecycle.completesRun = true`. |
| Input schema | `{ summary: string (min 10 chars), verified: boolean }` |
| Executor | `VerifySubmitExecutor = (summary, verified, context) => Promise<string>` |
| Timeout | 15s |
| Retryable | no |
| What it does | Signals run completion. The agent loop checks `tool.lifecycle.completesRun` and stops. |
| **Android portable?** | **Yes** — pure control flow. |
| Android equivalent | No-op executor that returns the summary; `AgentRuntime` checks `lifecycle.completesRun` and ends the run. |

#### Tool 0 (bonus, not in `createDefaultTools`): `spawn_agent` (sub-agents)

Source: `sdk/packages/core/src/extensions/tools/team/spawn-agent-tool.ts`.

A tool that delegates a task to a sub-agent (with its own system prompt, tools,
hooks). Returns `{ text, iterations, finishReason, usage }`. Registered via
`ToolPresets.act.enableSpawnAgent: true`. **Portable** — sub-agents are just
nested `AgentRuntime` instances.

### B.3 Tool portability table — at a glance

| # | Tool | Portable? | Effort | Notes / Android equivalent |
|---|---|---|---|---|
| 1 | `read_files` | ✅ Yes | S | SAF `DocumentFile.openInputStream` |
| 2 | `search_codebase` | ⚠️ Partial | M | Reimplement with `java.nio.file.Files.walk` + regex (MVP); inverted-index (v2) |
| 3 | `run_commands` | ❌ No | DROP | No shell on Android; provide safe pseudo-commands instead |
| 4 | `fetch_web_content` | ✅ Yes | S | Ktor `HttpClient.get().bodyAsText()` + Jsoup |
| 5 | `editor` | ✅ Yes | S | `DocumentFile` write + `String.replace` |
| 6 | `apply_patch` | ✅ Yes | M | Port ~200-LoC patch parser to Kotlin |
| 7 | `skills` | ✅ Yes | S | Read `.agent/skills/{name}.md` + template |
| 8 | `ask_question` | ✅ Yes | M | Compose dialog (foreground) + WorkManager (background) |
| 9 | `submit_and_exit` | ✅ Yes | S | Control-flow only |
| B0 | `spawn_agent` | ✅ Yes | S | Nested `AgentRuntime` |
| — | (legacy) `browser_action` | ❌ No | DROP | Puppeteer; no Android equivalent |
| — | (legacy) `use_mcp_tool` (stdio) | ❌ No | DROP | Cannot spawn subprocess |
| — | (legacy) `use_mcp_tool` (HTTP/SSE) | ✅ Yes | M | Port MCP HTTP client to Ktor |
| — | (legacy) `focus_chain` (todo) | ✅ Yes | S | Reimplement as a Markdown todo file (same as Cline) |
| — | (legacy) `plan_mode_respond` / `act_mode_respond` | ✅ Yes | S | Mode flag in `AgentRuntimeConfig` |
| — | (legacy) `new_task` / `summarize_task` | ✅ Yes | S | Sub-agent handoff pattern |
| — | (legacy) `web_search` | ✅ Yes | S | DuckDuckGo / Brave Search API |
| — | (legacy) `condense` (manual) | ✅ Yes | S | Trigger `runAgenticCompaction()` on demand |

### B.4 How tools are registered + dispatched

**Registration (factory pattern):**

```typescript
// sdk/packages/shared/src/tools/create.ts
export function createTool<TInput, TOutput>(config: {
  name: string;
  description: string;
  inputSchema: Record<string, unknown> | z.ZodTypeAny;  // Zod OR raw JSON Schema
  execute: (input: TInput, context: AgentToolContext) => Promise<TOutput>;
  lifecycle?: { completesRun?: boolean };               // marks terminal tools
  timeoutMs?: number;                                   // default 30_000
  retryable?: boolean;                                  // default true
  maxRetries?: number;                                  // default 3
}): AgentTool<TInput, TOutput>;
```

`createTool` does two things:
1. Normalizes the `inputSchema`: if it's a Zod schema, converts via
   `zodToJsonSchema()`; if raw JSON Schema, validates the top-level is an
   object/`oneOf`-of-objects/`anyOf`-of-objects (LLM tool schemas must be
   object-typed at the top level).
2. Returns an `AgentTool` (extends `AgentToolDefinition` + has `execute`).

**Dispatch (in `AgentRuntime.executeToolCalls`):**

```
For each toolCall in assistantMessage.toolCalls (sequentially OR parallel per config.toolExecution):
  1. Look up tool in this.tools Map by name.
  2. If metadata.inputParseError → skip with error message.
  3. If metadata.toolSource.executionMode === "provider" → skip (provider-executed tool).
  4. Normalize JSON-like strings in input against the schema.
  5. For each beforeTool hook: apply input/policy overrides, possibly skip/stop.
  6. Resolve tool policy: toolPolicies[toolName] + hook override.
     - If policy.enabled === false → skip "disabled by policy"
     - If policy.autoApprove === false → call requestToolApproval(toolCall, input, policy)
       - If approval.denied → skip with reason.
  7. Emit "tool-started" event.
  8. Call tool.execute(input, context).
  9. On success → AgentToolResult { output }. On error → AgentToolResult { output: { error }, isError: true }.
 10. For each afterTool hook: possibly stop/replace result.
 11. Emit "tool-finished" event with the tool-result message.
 12. If tool.lifecycle.completesRun && !isError → finishRun("completed").
```

**Key design properties:**
- **Hooks are first-class** — `beforeTool` can rewrite input, override policy, skip, or stop the whole run. `afterTool` can rewrite the result.
- **Approval is per-tool-call** — `toolPolicies[toolName].autoApprove` decides whether `requestToolApproval` is invoked.
- **Tool execution is sequential or parallel** — `config.toolExecution: "sequential" | "parallel"` (default sequential).
- **Provider-executed tools are different** — model-side tools (like Gemini's `google_search`) have `toolSource.executionMode === "provider"` and the runtime does NOT execute them locally; it just records the result the model returned. Cline calls these "ModelTools" vs "AgentTools".

### B.5 Can custom tools be added? How?

**Yes, three ways** (in increasing order of power):

1. **Inline in `AgentRuntimeConfig.tools`** — pass any `AgentTool[]`:
   ```typescript
   const myTool = createTool({
     name: "get_current_time",
     description: "...",
     inputSchema: z.object({ timezone: z.string().optional() }),
     async execute(input) { return { iso: new Date().toISOString() }; },
   });
   new Agent({ ..., tools: [myTool, ...defaultTools] });
   ```

2. **Via `AgentRuntimePlugin`** — for reusable bundles of tools + hooks:
   ```typescript
   export interface AgentRuntimePlugin {
     name: string;
     setup?: (context: AgentRuntimePluginContext) =>
       | { tools?: AgentTool[]; hooks?: Partial<AgentRuntimeHooks> }
       | Promise<{ tools?; hooks? }>;
   }
   ```
   Plugins are passed via `AgentRuntimeConfig.plugins`. Cline ships several
   example plugins in `sdk/examples/plugins/` (`weather-metrics.ts`,
   `web-search.ts`, `gitignore-read-files-guard.ts`, `background-terminal.ts`,
   `custom-compaction.ts`, `openrouter-provider.ts`, `telemetry.ts`,
   `mac-notify.ts`, `automation-events.ts`, `env-blocker.ts`).

3. **Via runtime "extensions"** (more advanced, used by `@cline/core`):
   `sdk/packages/core/src/extensions/tools/` has `command-guard-extension.ts`,
   `model-tool-routing.ts`, `presets.ts`, plus the `team/` subpackage for
   sub-agents. These are not user-facing; they're internal extension points the
   core runtime uses to wire in command-guard safety and sub-agent delegation.

**For Android:** mirror option (1) — define each tool as a Kotlin `AgentTool`
data class with a `suspend fun execute(input, context)`. Optionally support
option (2) via a "plugins" directory the user can drop `.kt` files into
(post-MVP).

---

## C. Agent loop + context management

### C.1 The agent loop — ITERATIVE, not recursive **(SUPersedes R-1)**

> **R-1 said:** *"Cline's agent loop (`recursivelyMakeClineRequests`, ~600 lines
> inside the 3,756-line Task class). It is a ReAct loop implemented as direct
> call-stack recursion; the teardown itself recommends converting to iterative
> `while(true)` for robustness — we will use Kotlin coroutines with `while(isActive)`
> from the start."*
>
> **R-A1 finding:** The recursion is GONE. `recursivelyMakeClineRequests` does
> not exist in the current source (verified via `rg "recursivelyMakeClineRequests"
> → no matches`). The new `AgentRuntime` class (`sdk/packages/agents/src/agent-runtime.ts`,
> 2097 lines) implements an **iterative `while` loop**.

**The new loop** (paraphrased from `agent-runtime.ts:650-850`):

```
async execute(input):
  await ensureInitialized()
  abortController = new AbortController()
  state.runId = "run_<nanoid>"
  state.status = "running"
  state.iteration = 0
  
  await callBeforeRunHooks()
  emit("run-started", snapshot)
  
  for message of input: push(message); emit("message-added")
  
  if completionPolicy.requireCompletionTool: push(reminderMessage)
  
  while maxIterations is undefined OR iteration < maxIterations:
    throwIfAborted()
    iteration += 1
    emit("turn-started", snapshot, iteration)
    
    { message, finishReason } = await generateAssistantMessageWithOverflowRecovery()
    //   ↳ on context_window_exceeded: emit status-notice, call prepareTurn (compaction), retry once
    
    if finishReason === "aborted": throw AbortError
    if message.content.length === 0: throw "Model returned empty response"
    
    toolCalls = message.content.filter(is tool-call)
    push(message); emit("message-added"); emit("assistant-message")
    
    if finishReason === "max-tokens" && toolCalls.length === 0:
      throw MAX_TOKENS_INCOMPLETE_TURN_MESSAGE
    
    if toolCalls.length === 0:
      emit("turn-finished", iteration, 0)
      completionReminders = getCompletionReminderMessages()
      if completionReminders.length > 0:
        for each reminder: addUserReminderMessage(reminder)
        continue  // ← NOT done; loop again
      result = finishRun("completed", message)
      await callAfterRunHooks(result); emit("run-finished", result)
      return result
    
    toolMessages = await executeToolCalls(toolCalls)  // ← may be parallel
    for tm of toolMessages: push(tm); emit("message-added")
    emit("turn-finished", iteration, toolCalls.length)
    
    terminalToolMessage = findCompletingToolMessage(toolCalls, toolMessages)
    if terminalToolMessage:  // a tool with lifecycle.completesRun succeeded
      result = finishRun("completed", message)
      await callAfterRunHooks(result); emit("run-finished", result)
      return result
  
  throw `Agent runtime exceeded maxIterations (${maxIterations})`
```

**Key properties:**
- **Iterative** (not recursive) — no stack overflow risk on long tasks.
- **`maxIterations` cap** — prevents runaway agent loops.
- **`AbortController`** — cooperative cancellation; `throwIfAborted()` checked at every iteration boundary.
- **Overflow recovery** — on `context_window_exceeded` error class, the runtime forces a `prepareTurn()` (compaction) call and retries once. If still overflow → `ContextWindowOverflowError` with actionable message.
- **Completion policy** — `requireCompletionTool: true` forces the agent to call a tool with `lifecycle.completesRun: true` (e.g., `submit_and_exit`) before the run can finish. If the agent ends without doing so, a reminder message is injected and the loop continues.
- **Tool execution** — `sequential` (default) or `parallel` via `config.toolExecution`.

### C.2 Context management — **BOTH** basic + agentic compaction **(SUPersedes R-1)**

> **R-1 said:** *"Cline uses quarter-truncation (delete oldest 1/4 of messages
> when context fills). Kilo Code uses LLM-based Auto-Compaction. Kilo wins."*
>
> **R-A1 finding:** Cline has ADOPTED LLM-based agentic compaction. Both strategies
> now exist in `sdk/packages/core/src/extensions/context/`. Cline caught up to
> Kilo Code.

**Two compaction strategies now exist:**

1. **`basic-compaction.ts` (711 LoC)** — deterministic, no LLM call. Used for:
   - Overflow recovery (the runtime MUST compact without depending on another LLM call working)
   - Tool-activity summarization (collapses old tool-call/tool-result pairs into a "Tool activity summary")
   - Preserves recent tail verbatim
2. **`agentic-compaction.ts` (318 LoC)** — LLM-based. Used proactively when the
   context approaches the threshold. Generates a structured summary preserving:
   - Goal of the conversation
   - Constraints
   - Decisions made
   - Next steps
   - Files read/modified (extracted from tool calls via `extractFileOps`)
   - Recent tail (verbatim, last `DEFAULT_PRESERVE_RECENT_TOKENS = 20_000` tokens)

**Compaction thresholds** (from `compaction-shared.ts:13-19`):

```typescript
export const DEFAULT_MAX_INPUT_TOKENS = 128_000;
export const CONTEXT_WINDOW_INPUT_RATIO = 0.9;  // estimate usable input as 90% of context window
export const COMPACTION_TRIGGER_RATIO = 0.9;    // compact when transcript hits 90% of usable input
export const DEFAULT_TARGET_RATIO = 0.7;        // target 70% of usable input post-compaction
export const DEFAULT_PRESERVE_RECENT_TOKENS = 20_000;  // preserve last 20K tokens verbatim
export const DEFAULT_SUMMARY_MAX_OUTPUT_TOKENS = 4_096;
export const TOOL_RESULT_CHAR_LIMIT = 2_000;    // truncate old tool results to 2K chars
export const FILE_CONTENT_CHAR_LIMIT = 2_000;   // truncate old file-content in tool results
export const MIN_TRUNCATED_MESSAGE_TOKENS = 8;
```

**Where compaction runs:** in the host-owned `prepareTurn` callback (declared
in `AgentRuntimeConfig.prepareTurn`). The runtime calls it before every model
request with the current messages; the callback can return modified messages +
system prompt. Cline's `@cline/core` package wires this up to call the
compaction pipeline (`compaction.ts` → `runBasicCompaction` or
`runAgenticCompaction` based on `mode` config).

**File-read deduplication:** `FileContextTracker` (legacy, in
`apps/vscode/src/core/context/context-tracking/`) watches files for external
modifications via `chokidar` (cross-platform file watcher). If a file Cline has
read gets modified externally, it's marked stale; the next diff-edit on that
file forces a re-read first. Stored in `task_metadata.json` next to the
conversation. **Portable to Android:** use `android.os.FileObserver` (kernel
inotify wrapper) — lower fidelity than chokidar (no recursive watch by default,
must register per-directory) but sufficient for a single workspace.

### C.3 How does the agent decide which tool to call?

**Native LLM function-calling** (not XML parsing). The `AgentModel.stream()`
interface emits `tool-call-delta` events with structured `{ toolCallId,
toolName, input }` data. The runtime collects these into `AgentToolCallPart`s
on the assistant message.

- For Anthropic / OpenAI / Gemini native APIs: the underlying provider's native
  tool-call format is used (Anthropic tool_use blocks, OpenAI function_call,
  Gemini functionCall).
- For OpenAI-compatible: uses the standard OpenAI function-calling JSON shape.
- The `tool_calls` chunk type in `ApiStream` (legacy stream) carries `{ call_id,
  function: { id, name, arguments } }` — a union shape across providers.

**There is NO XML parsing in the new SDK.** (R-1 mentioned "dual XML+native" —
that was true for the legacy VSCode extension which parsed Cline's XML tool
format for non-function-calling models. The new SDK requires function-calling
support from the model. **For our Android app, we should require function-calling
too** — every modern model supports it.)

### C.4 Multi-step task handling

Cline has FOUR mechanisms for multi-step tasks:

1. **Iteration loop** (the main `while` in `AgentRuntime.execute`) — single agent
   works through N tool-call iterations until `maxIterations` cap or completion
   tool called.
2. **`prepareTurn` hook** — host-owned turn preparation; this is where compaction,
   context injection, and queued user steering messages are applied between
   iterations. (`consumePendingUserMessage` lets the host inject a queued user
   message mid-task — useful for "steer the agent" UX.)
3. **`focus_chain` (legacy) / todos** — a Markdown todo list file
   (`focus_chain_taskid_<id>.md`) the agent writes to and reads from to track
   multi-step task progress. Stored on disk per task. **Portable to Android** —
   just a file in the workspace's `.agent/` directory.
4. **Sub-agents (`spawn_agent` / `use_subagents`)** — delegate a sub-task to a
   fresh `AgentRuntime` with its own system prompt + tools + hooks. Returns
   `{ text, iterations, finishReason, usage }`. **Portable to Android** — nested
   coroutine.

There is **no persistent "plan file"** like Kilo Code's `.kilo/plans/` (R-1 noted
this gap; Kilo Code is still ahead here). For Android, we should adopt Kilo's
plan-file pattern.

---

## D. LLM provider abstraction

### D.1 Provider count **(SUPersedes R-1)**

> **R-1 said:** *"43 provider adapters."*
>
> **R-A1 finding:** Counts depend on which surface:

- **SDK `BUILT_IN_PROVIDER` enum** (`sdk/packages/llms/src/providers/ids.ts`):
  **~50 providers** (anthropic, claude-code, cline, cline-pass, elevenlabs,
  openai-compatible, openai-native, openai-codex, openai-codex-cli, opencode,
  bedrock, vertex, gemini, ollama, lmstudio, deepseek, xai, together, fireworks,
  groq, poolside, cerebras, sambanova, nebius, baseten, requesty, litellm,
  huggingface, vercel-ai-gateway, v0, aihubmix, hicap, nousResearch,
  huawei-cloud-maas, wandb, xiaomi, tencent-tokenhub, kilo, zai, zai-coding-plan,
  qwen, qwen-code, doubao, mistral, moonshot, asksage, minimax, dify, oca,
  sapaicore, openrouter).
- **Generated provider IDs** (`provider-ids.generated.ts`): **172 entries** —
  this is the auto-generated catalog of every OpenAI-compatible router Cline
  knows about (abacus, abliteration-ai, ai-router, aiand, alibaba, alibaba-cn,
  …, llama, lmstudio, longcat, lucidquery, … all 172).
- **Legacy `ApiProvider` type** (`apps/vscode/src/shared/api.ts`): **49 named
  providers** (a subset of the SDK enum, kept for VS Code extension backward
  compatibility).

**For Android:** we do NOT need 50+ providers. The 80/20 is **5 adapters**:
1. `openai-compatible` (BYOK: covers OpenAI, Azure, OpenRouter, Together, Groq,
   Fireworks, DeepSeek, Mistral, vLLM, Ollama in OpenAI-compat mode, LM Studio,
   Cline itself, and ~150 of the 172 generated IDs).
2. `anthropic` (Claude — separate API shape with prompt caching).
3. `gemini` (Google — separate API shape with thinking levels).
4. `ollama` (local — special `/api/chat` endpoint).
5. `bedrock` (AWS — SigV4 auth + cross-region inference IDs) — optional, only
   if user demand warrants.

This covers ~95% of users. The other 45 SDK providers are vendor-specific
OpenAI-compatible routers that already work via `openai-compatible`.

### D.2 The provider interface — TWO interfaces coexist

**Legacy: `ApiHandler`** (`sdk/packages/llms/src/providers/handler.ts:25-69`):

```typescript
export interface ApiHandler {
  getMessages(systemPrompt: string, messages: Message[]): unknown;
  createMessage(systemPrompt: string, messages: Message[], tools?: ToolDefinition[]): ApiStream;
  getModel(): HandlerModelInfo;
  getApiStreamUsage?(): Promise<ApiStreamUsageChunk | undefined>;
  abort?(): void;
  setAbortSignal?(signal: AbortSignal | undefined): void;
}
export type ApiStream = AsyncGenerator<ApiStreamChunk> & { id?: string };
// ApiStreamChunk = text | media | reasoning | usage | tool_calls | done
```

This is the **3-method interface** R-1 documented. Still exists, used by the
VSCode extension. Factory: `createHandler(config: ProviderConfig): ApiHandler`
(sync) or `createHandlerAsync(config): Promise<ApiHandler>` (async).

**New: `AgentModel`** (`sdk/packages/shared/src/agent.ts:322-326`):

```typescript
export interface AgentModel {
  stream(request: AgentModelRequest): AsyncIterable<AgentModelEvent> | Promise<AsyncIterable<AgentModelEvent>>;
}
// AgentModelEvent = text-delta | reasoning-delta | media | tool-call-delta | tool-result | usage | finish
```

Higher-level, streaming-native, returns an `AsyncIterable` of typed events.
Used by the new `AgentRuntime`. Built by:
```typescript
const gateway = createGateway({ providerConfigs: [{ providerId, apiKey, baseUrl, headers, options }] });
const model = gateway.createAgentModel({ providerId, modelId });
new AgentRuntime({ model, ... });
```

**For Android:** port **only the `AgentModel` interface** (single `stream()`
method returning `Flow<AgentModelEvent>`). The legacy `ApiHandler` 3-method
interface is redundant; we don't need it. Use Kotlin `Flow` + sealed class for
events:

```kotlin
sealed class AgentModelEvent {
  data class TextDelta(val text: String) : AgentModelEvent()
  data class ReasoningDelta(val text: String, val redacted: Boolean = false) : AgentModelEvent()
  data class ToolCallDelta(val toolCallId: String?, val toolName: String?, val inputJson: String?) : AgentModelEvent()
  data class ToolResult(val toolCallId: String, val toolName: String, val output: JsonElement, val isError: Boolean) : AgentModelEvent()
  data class Usage(val inputTokens: Int, val outputTokens: Int, ...) : AgentModelEvent()
  data class Finish(val reason: FinishReason, val error: String? = null) : AgentModelEvent()
}
interface AgentModel { fun stream(request: AgentModelRequest): Flow<AgentModelEvent> }
```

### D.3 Custom / local models

**Yes — three official paths:**

1. **`openai-compatible` provider** — set `baseUrl` + `apiKey` for ANY
   OpenAI-compatible endpoint. Used by LM Studio, vLLM, OpenRouter, Together,
   Groq, Fireworks, DeepSeek, Mistral, etc. Implementation:
   `sdk/packages/llms/src/providers/vendors/openai-compatible.ts` uses the
   Vercel AI SDK's `@ai-sdk/openai-compatible` package + `@openrouter/ai-sdk-provider`.
   Azure variant: appends `?api-version=…` query param to deployment URLs.
2. **`ollama` provider** — dedicated vendor (`vendors/ollama.ts`) hitting
   Ollama's `/api/chat` endpoint. Default context window: `OLLAMA_DEFAULT_CONTEXT_WINDOW`.
3. **`lmstudio` provider** — dedicated vendor for LM Studio's OpenAI-compat
   endpoint.

**Custom model config** (`ModelInfo` shape in `apps/vscode/src/shared/api.ts:71-114`):

```typescript
export interface ModelInfo {
  name?: string;
  maxTokens?: number;          // max OUTPUT tokens
  contextWindow?: number;      // total context window
  supportsImages?: boolean;
  supportsPromptCache: boolean;
  supportsReasoning?: boolean;
  inputPrice?: number;         // per million input tokens
  outputPrice?: number;        // per million output tokens
  thinkingConfig?: { maxBudget?: number; outputPrice?: number; outputPriceTiers?: PriceTier[]; geminiThinkingLevel?: "low"|"high"; supportsThinkingLevel?: boolean };
  cacheWritesPrice?: number;
  cacheReadsPrice?: number;
  description?: string;
  tiers?: { contextWindow: number; inputPrice?: number; outputPrice?: number; cacheWritesPrice?: number; cacheReadsPrice?: number }[];
  temperature?: number;
  apiFormat?: ApiFormat;
  capabilities?: readonly string[];   // SDK capability list
  modalities?: ModelModalities;        // input/output modalities
  operation?: ModelOperation;
  operationModes?: readonly ModelOperationMode[];
}
```

**Model-specific parameters:**
- `maxTokens` / `contextWindow` — set per model.
- `temperature` — set per model.
- `supportsReasoning` + `thinkingConfig` — Anthropic thinking budget, Gemini
  thinking levels (low/high), reasoning effort (low/medium/high) for OpenAI o-series.
- `supportsPromptCache` — Anthropic prompt caching.
- `supportsImages` — multimodal.
- `tiers` — for tiered pricing (some providers charge different rates above a token threshold).

**For Android:** expose a "Custom Model" settings screen with fields: `displayName`,
`providerId` (dropdown of 5), `modelId`, `apiKey`, `baseUrl`, `contextWindow`,
`maxTokens`, `temperature`, `supportsReasoning`, `supportsImages`. Persist to
encrypted `DataStore<Preferences>`. This is straightforward UI work.

### D.4 Streaming support

**Yes — every provider streams.** The legacy `ApiHandler.createMessage()`
returns an `ApiStream` (async generator of `ApiStreamChunk`). The new
`AgentModel.stream()` returns `AsyncIterable<AgentModelEvent>`. Under the hood:
- Anthropic: SSE `message_delta` events.
- OpenAI: SSE `chat.completions.chunk` events.
- Gemini: SSE `streamGenerateContent` events.
- Ollama: NDJSON stream.
- All converted to the unified `ApiStreamChunk` / `AgentModelEvent` shape.

**For Android:** use **OkHttp + raw SSE parsing** (or Ktor `HttpClient` with
`EventSource` extension). Each provider adapter parses its own SSE format into
`Flow<AgentModelEvent>`. Buffer + backpressure via `Flow.flowOn(Dispatchers.IO)`.

---

## E. File system + sandboxing

### E.1 How Cline accesses the file system

- **Node.js `fs` / `fs/promises`** — direct filesystem access (legacy code in
  `apps/vscode/src/`).
- **`chokidar`** — file watching (for `FileContextTracker`).
- **ripgrep binary** — `apps/vscode/scripts/download-ripgrep.mjs` downloads
  platform-specific ripgrep binaries to `apps/vscode/bin/`. Used by
  `search_files`/`search_codebase`.
- **`glob` / `picomatch`** — pattern matching for `list_files`.
- **Node `child_process`** — `run_commands` spawns shell via `execa` (a
  `child_process` wrapper).
- **Node `process.cwd()`** — workspace root.

### E.2 Sandboxing — **NONE** **(critical finding for Android)**

**Cline has NO real sandboxing.** This is verified by reading the
`.clineignore` docs (`docs/customization/clineignore.mdx`):

> *"`.clineignore` filters what Cline loads automatically, but it is not a
> security or access-control boundary — ignored files can still be read via
> explicit `@` mentions or shell commands. We're moving away from it as a
> supported feature."*

The only "enforcement" mechanism is the **`beforeTool` hook pattern** (the
`gitignore-read-files-guard.ts` example plugin). It blocks `read_files` /
`editor` / `apply_patch` tool calls whose `path` is gitignored. But:

- It does NOT block `run_commands` (shell can read anything).
- It does NOT block `search_codebase` results.
- It depends on the user having a Git repo.
- It runs only on the SDK / CLI / Kanban — VS Code extension support is
  "arriving as they migrate."

**Implications for our Android app:** We CANNOT rely on Cline's "sandboxing"
patterns because they don't actually sandbox. We MUST enforce a real sandbox at
the executor level:

1. **All file-path-bearing tool executors (`read_files`, `editor`, `apply_patch`,
   `search_codebase`, `skills`)** must:
   - Resolve the requested `path` against the user-selected SAF root.
   - Reject with `SecurityException` if the resolved path escapes the root
     (i.e., contains `..` traversal or is absolute outside the root).
2. **No shell tool** — `run_commands` is dropped entirely. Instead, provide a
   curated set of safe pseudo-commands that operate only on workspace paths.
3. **`beforeTool` hook** — register a `PathGuardHook` that re-validates paths
   before every tool call (defense in depth).
4. **SAF (Storage Access Framework)** — user picks a tree URI at app setup.
   Persist `takePersistableUriPermission()`. All file operations go through
   `DocumentFile.fromTreeUri(rootUri).findFile(...)`. For better performance
   (esp. for `search_codebase`), copy the workspace to app-private cache
   (`<cache>/workspaces/<workspaceId>/`) at open time and operate on the cache
   copy; sync back on close.
5. **`FileObserver`** — replace chokidar with `android.os.FileObserver` for
   stale-file detection (per-directory, no recursive watching without manual
   fan-out).

### E.3 File reads / writes / edits

Cline's edit formats (across both surfaces):

1. **`editor` tool (SDK)** — `old_text` → `new_text` direct replacement. No
   SEARCH/REPLACE block syntax. Validates exactly one match. Simplest and most
   reliable. **Adopt this for Android.**
2. **`apply_patch` tool (SDK + legacy)** — canonical freeform patch grammar:
   ```
   *** Begin Patch
   *** Update File: src/foo.kt
   @@ context line
   -old line
   +new line
   *** End Patch
   ```
   Supports Add/Update/Delete/Move across multiple files atomically. Parser:
   `apply-patch-parser.ts` (~200 LoC). **Adopt this for multi-file atomic edits.**
3. **`replace_in_file` (legacy VSCode)** — SEARCH/REPLACE block syntax with
   `<<<<<<< SEARCH ... ======= ... >>>>>>> REPLACE` markers. The classic Cline
   format. **DON'T adopt** — `editor` is simpler and the SDK has moved away
   from SEARCH/REPLACE.
4. **`write_to_file` (legacy)** — full file content write. Subsumed by `editor`
   with no `old_text`.

### E.4 Directory browsing

- **Legacy `list_files` tool** — uses `services/glob/list-files.ts` (ripgrep +
  picomatch). Returns recursive file listing with `.clineignore` filtering.
- **SDK `search_codebase`** — same purpose via regex.
- **No dedicated "browse directory" SDK tool** — the agent uses `search_codebase`
  with a pattern like `.*` to discover files, or `read_files` on a directory
  (which would error).

**For Android:** add a `list_dir` tool (path → recursive file list with sizes
+ mtimes) backed by `java.nio.file.Files.walk()` with depth limit. This is one
tool Cline does NOT have cleanly in the SDK surface; we add it.

---

## F. Background execution

### F.1 Does Cline run tasks in the background?

**Yes — three mechanisms:**

1. **`@cline/core` hub daemon** (`sdk/packages/core/src/hub/daemon/`) — a
   detached long-running process that hosts agent sessions, can be started by
   the CLI (`apps/cli` auto-spawns `cline-hub` daemon on launch per `AGENTS.md`)
   or run standalone. The hub exposes discovery + session-client APIs
   (`HubSessionClient`, `HubUIClient`). Sessions persist across client disconnects.
2. **Cron scheduler** (`sdk/packages/core/src/cron/`) — full cron spec parser
   + watcher + reconciler + service + store. Schedules agent runs at cron times.
   Spec files: `.cline/cron/*.cron.md` (Markdown frontmatter + cron expression).
   Example: `daily-code-review.cron.md`, `dependency-check.cron.md`,
   `weekly-metrics-summary.cron.md`. Events: `pr-review.event.md`,
   `pr-test-coverage.event.md` (triggered by external webhooks).
3. **Sub-agent runs** — `spawn_agent` tool creates a nested `AgentRuntime` that
   runs synchronously within the parent run's iteration (blocking the parent
   until the sub-agent finishes). Not "background" in the OS sense, but parallel
   to the parent's own work if `toolExecution: "parallel"`.

**For Android:** Use **`WorkManager`** for cron-like scheduling (the only
reliable background execution mechanism on Android 14+). Use a **Foreground
Service** (`startForeground()`) for active agent runs to avoid being killed by
Doze/App Standby. Map:
- `hub daemon` → `AgentForegroundService` (long-running foreground service
  hosting one or more agent runs).
- `cron scheduler` → `WorkManager` `PeriodicWorkRequest` with cron-to-WorkManager
  expression converter.
- `sub-agent` → nested Kotlin coroutine within the parent run's coroutine scope.

### F.2 Task queue / job system

- **`turn-queue`** (`sdk/packages/core/src/runtime/turn-queue/`) — runtime-internal
  queue of pending model turns within a single session.
- **Hub session queue** — the hub daemon manages multiple sessions, each with its
  own run state; sessions can be queued if the hub is at capacity.
- **`cron-spec-parser.ts` + `cron-reconciler.ts`** — schedule persistence + reconciliation.

**For Android:** WorkManager is the task queue. Each agent run = one
`OneTimeWorkRequest` (or `PeriodicWorkRequest` for cron). WorkManager handles
persistence across reboots, retry, backoff, and constraints (network, charging,
idle).

### F.3 Concurrent tasks

Cline supports multiple concurrent agent runs:
- **Hub daemon** hosts multiple sessions concurrently.
- **Kanban mode** (separate `cline/kanban` repo) — runs many agents in parallel
  from a web board, each in its own git worktree with auto-commit.
- **Sub-agents** — within a single run, `toolExecution: "parallel"` runs tool
  calls concurrently; `spawn_agent` blocks the parent until sub-agent done.

**For Android:** WorkManager supports parallel workers (with unique work names
to prevent duplicate runs of the same task). For multiple workspaces, give each
workspace its own `workerClassName` + unique `workName` (e.g.
`agent-run-${workspaceId}-${runId}`).

---

## G. Android portability assessment

### G.1 Minimal viable subset to port

| Component | Source path | LoC | Port? | Effort |
|---|---|---|---|---|
| `AgentTool` + `AgentModel` + `AgentMessage` types | `sdk/packages/shared/src/agent.ts` | 640 | ✅ | S (kotlinx.serialization data classes) |
| `createTool` factory | `sdk/packages/shared/src/tools/create.ts` | 131 | ✅ | S (no Zod on Kotlin — use `kotlinx.serialization` JsonSchema or hand-written JSON schema) |
| 9 default tool definitions | `sdk/packages/core/src/extensions/tools/definitions.ts` | 942 | ✅ (minus `run_commands`) | M |
| Tool schemas | `sdk/packages/core/src/extensions/tools/schemas.ts` | 352 | ✅ | S (kotlinx.serialization) |
| `AgentRuntime` loop | `sdk/packages/agents/src/agent-runtime.ts` | 2097 | ✅ | M (rewrite as Kotlin coroutine `while(isActive)` loop) |
| 7 hooks (beforeRun…onEvent) | `sdk/packages/shared/src/agent.ts` | (in above) | ✅ | S |
| `prepareTurn` callback | `sdk/packages/agents/src/agent-runtime.ts` | (in above) | ✅ | S |
| Basic compaction (deterministic) | `sdk/packages/core/src/extensions/context/basic-compaction.ts` | 711 | ✅ | M |
| Agentic compaction (LLM summary) | `sdk/packages/core/src/extensions/context/agentic-compaction.ts` | 318 | ✅ | M |
| Compaction thresholds + helpers | `sdk/packages/core/src/extensions/context/compaction-shared.ts` | 780 | ✅ | S (constants + utility functions) |
| `openai-compatible` provider | `sdk/packages/llms/src/providers/vendors/openai-compatible.ts` | ~300 | ✅ | M (Ktor HttpClient + SSE) |
| `anthropic` provider | `sdk/packages/llms/src/providers/vendors/anthropic.ts` | — | ✅ | M (SSE + prompt caching) |
| `gemini` provider | `sdk/packages/llms/src/providers/vendors/google.ts` | — | ✅ | M (SSE + thinking levels) |
| `ollama` provider | `sdk/packages/llms/src/providers/vendors/ollama.ts` | — | ✅ | S (NDJSON stream) |
| Gateway / model registry | `sdk/packages/llms/src/providers/gateway.ts` | 383 | ✅ (simplified) | S (5 providers, no auto-discovery) |
| `FileContextTracker` (file staleness) | `apps/vscode/src/core/context/context-tracking/FileContextTracker.ts` | ~250 | ✅ (use FileObserver) | S |
| `apply-patch-parser` | `sdk/packages/core/src/extensions/tools/executors/apply-patch-parser.ts` | ~200 | ✅ | S (pure string parsing) |
| Tool approval flow | `sdk/packages/agents/src/agent-runtime.ts:1690-1728` | ~40 | ✅ | S (suspendCancellableCoroutine + Notification) |
| `toolPolicies` (per-tool enable/autoApprove) | `sdk/packages/shared/src/llms/tools.ts` | — | ✅ | S |
| Sub-agent (`spawn_agent`) | `sdk/packages/core/src/extensions/tools/team/spawn-agent-tool.ts` | ~300 | ✅ | S (nested coroutine) |
| Skills system (Markdown) | `sdk/packages/core/src/extensions/skills/` (if exists) | — | ✅ | S |
| Cron scheduler → WorkManager | `sdk/packages/core/src/cron/` | ~600 | ✅ | M (rewrite as WorkManager) |
| MCP client (HTTP/SSE only, drop stdio) | `sdk/packages/core/src/extensions/mcp/` | — | ⚠️ | M |
| Checkpoints (shadow-git → Room) | `apps/vscode/src/core/controller/checkpoints/` | — | ⚠️ | M (per R-1, use Room-backed snapshots) |
| Plan/Act mode | `apps/vscode/src/core/prompts/` (plan/act) | — | ✅ | S (mode flag + tool preset) |

**Total minimal port:** ~5.8 KLOC Kotlin (matches R-1's estimate of ~2 KLOC
Cline backbone + ~550 LoC Kilo patterns + ~3.3 KLOC original Android work).

### G.2 What MUST be dropped

| Component | Why drop | Source path |
|---|---|---|
| **`run_commands` (shell)** | Android cannot spawn shell processes; no `bash`/`git`/`npm`/`python` binaries accessible from sandboxed app. | `sdk/packages/core/src/extensions/tools/executors/bash.ts` |
| **`browser_action` (Puppeteer)** | Puppeteer needs Chromium + Node; no Android equivalent (could use `agent-browser` Chrome on Android but it's a different API). | legacy |
| **MCP stdio transport** | Android cannot spawn subprocesses. | `McpHub.ts:484` |
| **MCP SSE transport** | The MCP SDK uses `@modelcontextprotocol/sdk/client/sse.js` which assumes Node `EventSource`. Reimplement on Ktor. | `McpHub.ts:7` |
| **Shadow-git checkpoints** | Requires `git` CLI binary; not available on Android without Termux/root. | `apps/vscode/src/core/controller/checkpoints/` |
| **ripgrep binary** | `apps/vscode/scripts/download-ripgrep.mjs` downloads prebuilt binaries; no aarch64-Android build exists. Reimplement search in Kotlin. | `apps/vscode/bin/` |
| **chokidar** | Node file watcher. Use `android.os.FileObserver`. | `FileContextTracker.ts` |
| **VS Code host bridge (gRPC)** | Entire `apps/vscode/src/hosts/vscode/hostbridge/` directory — VS Code-specific. | — |
| **Webview UI (React)** | `apps/vscode/webview-ui/` — replace with Jetpack Compose. | — |
| **`@ai-sdk/*` Vercel AI SDK** | JS-only. Reimplement in Kotlin (the patterns are simple). | `sdk/packages/llms/src/providers/vendors/openai-compatible.ts` |

### G.3 What MUST be reimplemented (not ported line-by-line)

| Component | Reimplementation approach |
|---|---|
| **LLM HTTP client** | Ktor `HttpClient` with `ContentNegotiation` (JSON) + `EventSource` (SSE) + `HttpTimeout`. One adapter per provider. ~300 LoC each. |
| **Tool dispatch** | Kotlin coroutine: `suspend fun executeToolCall(toolCall, context): AgentToolResult`. Use `Dispatchers.IO` for I/O-bound tools. |
| **Context truncation** | Port `compaction-shared.ts` constants + `runBasicCompaction` logic. Use `kotlinx.serialization` for message JSON. |
| **Agent loop** | `suspend fun run(input): AgentRunResult` with `while (isActive && iteration < maxIterations)` + `try/finally` for cleanup. CoroutineScope per run. |
| **File system access** | All paths resolved against SAF root `DocumentFile`. Path-guard helper: `fun resolveSafPath(raw): String = rootUri.resolve(raw).also { checkInsideRoot() }`. |
| **Streaming** | `Flow<AgentModelEvent>` with `.flowOn(Dispatchers.IO)`. Buffer of ~64 events. Backpressure via `conflate()` or `collectLatest`. |
| **API key storage** | `EncryptedSharedPreferences` + Android Keystore master key (AES-GCM). Per-provider key entries. |
| **Background execution** | `ForegroundService` + `WorkManager` for scheduled/cron. `Notification` with progress + cancel action. |
| **Sub-agent** | `coroutineScope { launch { childAgent.run(task) } }.awaitAll()` — nested `AgentRuntime` instances. |
| **Checkpoints** | Per R-1: Room-backed `design_snapshots` table storing per-turn full-content snapshots (or deltas). NOT git. |

### G.4 Effort estimates per component

| Component | Effort | LoC (Kotlin) | Notes |
|---|---|---|---|
| Types (`AgentTool`, `AgentModel`, `AgentMessage`) | S | ~400 | kotlinx.serialization |
| `createTool` factory | S | ~80 | JSON schema validation |
| 9 default tools (port) | M | ~900 | minus `run_commands` |
| `AgentRuntime` loop | M | ~700 | Kotlin coroutine rewrite |
| 7 hooks system | S | ~150 | interfaces + dispatch |
| Basic compaction | M | ~400 | port `basic-compaction.ts` |
| Agentic compaction | M | ~250 | port `agentic-compaction.ts` (calls LLM) |
| 5 LLM provider adapters | L | ~1500 | 300 LoC each × 5 |
| Gateway / model registry | S | ~200 | simplified |
| `FileContextTracker` (FileObserver) | S | ~150 | |
| `apply-patch-parser` | S | ~250 | pure string parsing |
| Tool approval (Notification + dialog) | M | ~300 | |
| Sub-agent (`spawn_agent`) | S | ~100 | nested coroutine |
| Skills system | S | ~200 | Markdown file reader |
| Cron → WorkManager | M | ~400 | cron-spec parser + WorkManager bridge |
| MCP client (HTTP/SSE only) | M | ~500 | reimplement on Ktor |
| Checkpoints (Room) | M | ~400 | per R-1 design |
| Plan/Act mode + tool presets | S | ~150 | mode flag + preset switch |
| **Compose agent UI** | L | ~1500 | chat view + tool approval dialog + status bar |
| **Android lifecycle integration** | L | ~800 | FGS + WorkManager + scoped storage + Keystore |
| **Design-system-specific tools** (per R-1) | M | ~400 | design-system operations |
| **Total** | **XL** | **~8.8 KLOC** | matches R-1's ~5.8 KLOC + buffer |

### G.5 Key risks for Android

1. **Battery drain** — LLM streaming + tool calls are network-heavy + CPU-heavy
   (JSON parsing, file I/O, regex search). A single 50-iteration agent run can
   easily consume 5-10% battery. **Mitigation:** schedule long runs on charge +
   wifi via WorkManager constraints; cap `maxIterations` at 25 by default (per
   R-1 recommendation); throttle LLM calls; prefer smaller models for routine
   tasks.
2. **Background execution limits** — Android 14+ aggressively kills background
   apps. **Mitigation:** Foreground Service with a persistent notification (user
   sees the agent is working); WorkManager for cron-like tasks (persists across
   reboot); disable agent runs on battery saver.
3. **No terminal/shell** — biggest capability gap vs desktop Cline. **Mitigation:**
   drop `run_commands`; provide curated pseudo-commands (`list_dir`, `file_size`,
   `mkdir`, `move`, `delete`, `rename`). The agent will be a "file editor +
   web fetcher + LLM reasoner" but NOT a general-purpose shell user. Acceptable
   for our design-system agent use case; problematic for general-purpose coding.
4. **Sandboxed file system** — SAF URIs are slower than direct `fs`. Large
   workspaces (10K+ files) will be painful for `search_codebase`. **Mitigation:**
   copy workspace to app-private cache at open time; operate on cache; sync back.
   Document the trade-off to users (changes made outside the app during a run
   won't be seen until re-sync).
5. **Memory pressure** — 200K-token transcripts in RAM are risky on low-end
   devices (4GB RAM). **Mitigation:** stream messages to disk (Room) as they're
   generated; only keep last N in memory; compaction aggressively.
6. **API key security** — storing cloud LLM API keys on device. **Mitigation:**
   EncryptedSharedPreferences + Keystore master key; never log keys; clear keys
   on uninstall.
7. **Permission fatigue** — if every tool call requires approval, user will
   disable approvals (= unsafe). **Mitigation:** smart defaults — `read_files`,
   `search_codebase`, `fetch_web_content` auto-approved; `editor`, `apply_patch`,
   `ask_question` prompt once per workspace; `spawn_agent` always prompts;
   destructive ops (`delete`, `move`) always prompt.
8. **No ripgrep** — search performance on large workspaces will be poor.
   **Mitigation:** MVP uses Java regex walk; v2 builds an inverted index at
   open time (one-time cost paid once per workspace open).
9. **MCP fragmentation** — stdio MCP servers (the majority of MCP servers in
   the wild) won't work. Only HTTP/SSE MCP servers are usable. **Mitigation:**
   document this limitation; provide a curated list of known-working HTTP MCP
   servers in the app.
10. **Provider rate limits + errors** — cloud LLM APIs rate-limit aggressively.
    The agent must handle 429s with exponential backoff. **Mitigation:** port
    Cline's error-classification logic (`error-classification.ts`) +
    `retry-empty-response` middleware; show user-friendly error messages.

---

## H. Feature highlights to adopt

### H.1 Cline's BEST features (definitely adopt)

1. **Layered SDK package structure** (`:agent:shared → :agent:llm → :agent:core
   → :agent:tools`) — strict downward dependency direction. Per R-1.
2. **`createTool` factory with executor injection** — the executor is the ONLY
   platform-specific part. The 9 default tool definitions are ~940 LoC of pure
   TypeScript that can be ported almost mechanically to Kotlin.
3. **`AgentTool.lifecycle.completesRun` flag** — elegant way to mark terminal
   tools (`submit_and_exit`). The runtime auto-finishes the run when such a
   tool succeeds.
4. **Iterative `while` loop with `maxIterations` + `AbortController`** — robust,
   no stack overflow. Maps directly to Kotlin `while(isActive)` coroutine.
5. **7-hook lifecycle** (beforeRun, afterRun, beforeModel, afterModel, beforeTool,
   afterTool, onEvent) — perfect extension points for our path-guard, telemetry,
   UI updates, custom compaction.
6. **Dual compaction strategy** (basic deterministic + agentic LLM) — basic for
   overflow recovery (no LLM dependency), agentic for proactive summarization.
   Best of both worlds.
7. **`prepareTurn` host-owned hook** — clean separation: the runtime calls
   `prepareTurn(messages)` before each model request; the host (us) decides
   whether to compact, inject context, or pass through.
8. **File-context tracker pattern** — mark files stale on external modification;
   force re-read before edit. Critical for diff-edit reliability.
9. **`apply_patch` canonical grammar** — atomic multi-file edits with Add/Update/
   Delete/Move. Better than SEARCH/REPLACE blocks.
10. **`editor` tool's direct `old_text → new_text` replacement** — simpler than
    SEARCH/REPLACE blocks. Validates exactly one match.
11. **`toolExecution: "sequential" | "parallel"` config** — lets us pick
    parallel for read-only tools (read_files, search_codebase, fetch_web_content)
    and sequential for write tools.
12. **`toolPolicies` (per-tool `enabled` + `autoApprove`)** — fine-grained control.
13. **Skills system** (Markdown + slash commands) — user-extensible without
    writing Kotlin.
14. **Cron/scheduling** → port to WorkManager.
15. **Plugin architecture** (`AgentRuntimePlugin.setup() → { tools, hooks }`) —
    for future Kotlin plugin support (post-MVP).
16. **Overflow recovery** — on `context_window_exceeded`, force compaction +
    retry once. Don't fail the whole run.
17. **Tool result truncation** — `TOOL_RESULT_CHAR_LIMIT = 2000` keeps old tool
    outputs from blowing up the context window.
18. **`AgentModel.stream()` returning `AsyncIterable<AgentModelEvent>`** —
    streaming-native, typed events. Maps to Kotlin `Flow<AgentModelEvent>`.
19. **Error classification** (`ProviderErrorClass = "context_window_exceeded" |
    "unknown"`) — drives recovery policy.
20. **`completionPolicy`** (`requireCompletionTool`, `completionGuard`) — forces
    the agent to explicitly complete via a terminal tool.

### H.2 Cline's WEAKNESSES (we should improve)

1. **`run_commands` is too tightly coupled to Node's `child_process`** — no
   escape hatch for non-shell environments. **Our fix:** drop it; provide
   curated pseudo-commands.
2. **Two parallel tool surfaces (SDK vs legacy VSCode)** — confusing. **Our
   fix:** port only the SDK surface; never look at the legacy surface.
3. **50+ provider adapters is overkill for mobile** — most Android users want
   3-5. **Our fix:** ship 5 adapters; add more on demand.
4. **`ApiHandler` (3-method) + `AgentModel` (1-method) are redundant** — pick
   ONE. **Our fix:** use only `AgentModel`.
5. **`.clineignore` is documented as NOT a security boundary** — Cline doesn't
   actually sandbox. **Our fix:** enforce a real SAF sandbox at the executor
   level.
6. **chokidar is overkill** — use `FileObserver` on Android.
7. **ripgrep binary download** — not viable on Android. **Our fix:** Java regex
   walk (MVP) → inverted index (v2).
8. **shadow-git checkpoint system** requires `git` CLI. **Our fix:** Room-backed
   snapshots per R-1's design.
9. **MCP stdio transport** useless on Android. **Our fix:** drop stdio; HTTP/SSE
   only.
10. **No persistent plan files** (unlike Kilo Code's `.kilo/plans/`). **Our
    fix:** adopt Kilo's plan-file pattern.
11. **No per-agent permission overrides with glob patterns** (Kilo Code has
    this; Cline only has per-tool policies). **Our fix:** adopt Kilo's
    per-workspace glob permission pattern.
12. **VS Code-specific host bridge (gRPC + webview)** — entirely irrelevant to
    Android. **Our fix:** Compose UI + Room persistence.

### H.3 Features Cline has that the other repos (Kilo, OpenCode) might not

Per R-1's comparison:
- **Dual basic + agentic compaction** — Kilo Code had agentic-only; Cline now
  has both. Cline wins on overflow-recovery robustness.
- **`apply_patch` canonical grammar** — Cline's specific contribution; cleaner
  than OpenCode's diff format.
- **Hub daemon architecture** — for shared, persistent multi-session runtime.
  Cline-specific.
- **MCP full client (stdio + SSE + StreamableHTTP)** — most complete of any
  agent framework (though stdio is useless for us).
- **Plan/Act mode with separate tool presets** (`ToolPresets.act` vs
  `ToolPresets.plan`) — clean two-mode UX.
- **`submit_and_exit` tool with `lifecycle.completesRun`** — elegant completion
  semantics; OpenCode uses an `end_turn` finish reason instead.
- **Tool presets (`act`, `plan`, `search`, `minimal`, `yolo`)** — pre-configured
  tool bundles for common use cases. Useful pattern.
- **Cron + event triggers** (`*.cron.md`, `*.event.md`) — Markdown-driven
  scheduling. Unique to Cline.

---

## I. Custom model support — summary

(See §D.3 for full details.) Summary:

- **Configuration fields:** `displayName`, `providerId`, `modelId`, `apiKey`,
  `baseUrl`, `contextWindow`, `maxTokens`, `temperature`, `supportsReasoning`,
  `supportsImages`, `thinkingConfig`, `inputPrice`, `outputPrice`.
- **OpenAI-compatible endpoint:** YES — set `baseUrl` + `apiKey`; works for
  Ollama (OpenAI-compat mode), LM Studio, vLLM, OpenRouter, Together, Groq,
  Fireworks, DeepSeek, Mistral, and ~150 other routers.
- **Dedicated Ollama / LM Studio providers:** YES — separate vendor adapters
  for their native APIs.
- **Model-specific parameters:** YES — full `ModelInfo` schema (contextWindow,
  maxTokens, temperature, thinkingConfig, supportsPromptCache, supportsImages,
  pricing, tiers).
- **Per-run overrides:** YES — `AgentRuntimeConfig.modelOptions` is a free-form
  `Record<string, unknown>` merged per-request; provider-specific options
  (reasoning effort, thinking budget, etc.) flow through here.

**For Android:** Single "Models" settings screen with a list of configured
models + "Add Custom Model" flow. Persist to encrypted DataStore. Per-workspace
override: each workspace can pin a specific model ID. Per-run override: the
"Start Run" dialog can override the model for that run only.

---

## Porting Recommendation (final synthesis)

### Keep (architectural patterns to mirror in Kotlin)

1. **Layered SDK package structure** → Kotlin modules `:agent:shared`,
   `:agent:llm`, `:agent:core`, `:agent:tools`, `:app`.
2. **`AgentTool` interface + `createTool` factory** with executor injection.
3. **9 SDK default tools** (port minus `run_commands`).
4. **`AgentRuntime` iterative `while` loop** with `maxIterations` + abort.
5. **7-hook lifecycle** (beforeRun…onEvent).
6. **Dual compaction** (basic + agentic) with `prepareTurn` host-owned callback.
7. **`toolPolicies`** (per-tool enable + autoApprove).
8. **`apply_patch` canonical grammar** for multi-file atomic edits.
9. **`editor` tool's direct replacement** (no SEARCH/REPLACE blocks).
10. **`FileContextTracker` pattern** (use `FileObserver`).
11. **`AgentModel.stream()` returning `Flow<AgentModelEvent>`**.
12. **Tool presets** (act/plan/yolo/minimal/search).
13. **`spawn_agent` sub-agent tool** (nested coroutine).
14. **Cron → WorkManager** scheduling.
15. **Skills system** (Markdown slash commands).
16. **Plan/Act mode** with separate tool presets.

### Drop (no Android equivalent or out of scope for MVP)

1. `run_commands` (shell) — no shell on Android.
2. `browser_action` (Puppeteer) — no Chromium on Android.
3. MCP stdio transport — no subprocess.
4. Shadow-git checkpoints — no git CLI.
5. ripgrep binary — no aarch64-Android build.
6. chokidar — use FileObserver.
7. VS Code host bridge (gRPC + webview).
8. The 45+ vendor-specific provider adapters (keep only 5: openai-compatible,
   anthropic, gemini, ollama, bedrock).
9. `ApiHandler` 3-method legacy interface — use `AgentModel` only.
10. `.clineignore` (no security value anyway).

### Reimplement (core ideas, fresh Kotlin)

1. **LLM HTTP client** — Ktor + SSE per provider.
2. **Tool dispatch** — Kotlin coroutines on `Dispatchers.IO`.
3. **Context truncation** — port `compaction-shared.ts` constants + logic.
4. **File system access** — SAF + `DocumentFile` + path-guard helper.
5. **Streaming** — `Flow<AgentModelEvent>` with backpressure.
6. **API key storage** — EncryptedSharedPreferences + Keystore.
7. **Background execution** — ForegroundService + WorkManager.
8. **Checkpoints** — Room-backed `snapshots` table (per R-1).
9. **Search** — Java regex walk (MVP), inverted index (v2).
10. **Sub-agents** — nested coroutine.

### Proposed Kotlin module shape (minimal viable)

```
:app                                    ← Compose UI, FGS, DI entry
  ├─ ui/                                 ← chat, tool approval, settings
  ├─ service/                            ← AgentForegroundService
  ├─ work/                               ← WorkManager workers (cron)
  └─ di/                                 ← Hilt modules

:agent:shared                           ← types, no Android deps
  ├─ model/                              ← AgentTool, AgentModel, AgentMessage, AgentModelEvent
  ├─ tool/                               ← createTool factory
  └─ json/                               ← JSON schema helpers

:agent:llm                              ← provider adapters
  ├─ AgentModel.kt                       ← interface
  ├─ providers/
  │   ├─ OpenAiCompatibleProvider.kt
  │   ├─ AnthropicProvider.kt
  │   ├─ GeminiProvider.kt
  │   ├─ OllamaProvider.kt
  │   └─ BedrockProvider.kt
  └─ gateway/                           ← model registry

:agent:core                             ← runtime + context mgmt
  ├─ AgentRuntime.kt                    ← the iterative loop
  ├─ hooks/                             ← 7 hook interfaces
  ├─ compaction/                         ← basic + agentic
  ├─ approval/                           ← tool approval flow
  ├─ subagent/                          ← spawn_agent
  └─ mcp/                                ← HTTP/SSE-only MCP client

:agent:tools                            ← 8 default tools + extras
  ├─ ReadFilesTool.kt
  ├─ SearchCodebaseTool.kt
  ├─ ListDirTool.kt                      ← NEW (Cline doesn't have cleanly)
  ├─ FetchWebContentTool.kt
  ├─ EditorTool.kt
  ├─ ApplyPatchTool.kt
  ├─ SkillsTool.kt
  ├─ AskQuestionTool.kt
  ├─ SubmitAndExitTool.kt
  ├─ SpawnAgentTool.kt
  └─ executors/                          ← SAF-bound implementations
      ├─ SafFileReadExecutor.kt
      ├─ SafEditorExecutor.kt
      └─ ...

:agent:storage                          ← Room + DataStore
  ├─ checkpoint/                         ← Room snapshots
  ├─ message/                            ← persisted conversation
  ├─ workspace/                          ← workspace config
  └─ secret/                             ← EncryptedSharedPreferences
```

### One-line recommendation

Port Cline's **SDK default tool layer** (9 tools, executor-injected) + **new
`AgentRuntime` iterative loop** + **dual compaction strategy** to Kotlin, drop
shell/browser/MCP-stdio/shadow-git, enforce a real SAF sandbox at the executor
level, and adopt Kilo Code's plan-files + per-workspace glob permissions to fill
the gaps Cline leaves.

---

## Appendix A — Architecture diagram (text)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              USER (Android phone)                             │
└───────────────┬──────────────────────────────────────────────────────────────┘
                │ launches + monitors via Notification
                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          :app (Compose UI + FGS)                              │
│  ┌────────────────┐  ┌──────────────────┐  ┌────────────────────────────────┐ │
│  │  ChatScreen    │  │ ToolApprovalDlg   │  │ AgentForegroundService        │ │
│  │  (Compose)     │  │ (Compose Dialog)  │  │  - startForeground(notif)     │ │
│  │  - msg list    │  │  - tool name      │  │  - hosts AgentRuntime.run()   │ │
│  │  - input bar   │  │  - input preview  │  │  - cancellation               │ │
│  │  - status      │  │  - Approve/Deny   │  │  - progress updates           │ │
│  └──────┬─────────┘  └────────┬──────────┘  └───────────┬────────────────────┘ │
└─────────┼─────────────────────┼─────────────────────────┼──────────────────────┘
          │ StateFlow<RunState> │ suspendCancellableCoro   │ coroutineScope
          ▼                     ▼                         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         :agent:core (AgentRuntime)                            │
│                                                                              │
│   while (isActive && iteration < maxIterations):                             │
│     1. prepareTurn(messages) ──────────► [compaction pipeline]                │
│     2. beforeModel hooks                                                     │
│     3. model.stream(request) ──────────► Flow<AgentModelEvent>               │
│     4. collect events → assistantMessage                                     │
│     5. afterModel hooks                                                       │
│     6. if no toolCalls → finish OR inject completion reminder                │
│     7. for each toolCall:                                                    │
│          - beforeTool hooks (path guard, policy override)                    │
│          - toolPolicy.autoApprove? → requestToolApproval() ──► [Dialog]       │
│          - tool.execute(input, ctx) ───────► [executor]                      │
│          - afterTool hooks                                                    │
│     8. if tool.lifecycle.completesRun → finish                               │
│                                                                              │
└────────┬────────────────────────────────────┬─────────────────────────────────┘
         │                                       │
         ▼                                       ▼
┌─────────────────────────────┐      ┌──────────────────────────────────────────┐
│   :agent:llm                │      │   :agent:tools                            │
│                             │      │                                          │
│  AgentModel (interface)     │      │  9 tools (AgentTool instances)            │
│   └─ stream(req): Flow<Evt> │      │   ├─ read_files    → SafFileReadExec      │
│                             │      │   ├─ search_codebase → KotlinRegexWalk   │
│  Providers:                 │      │   ├─ fetch_web_content → KtorHttpClient   │
│   ├─ OpenAiCompatible       │      │   ├─ editor       → SafEditorExec         │
│   ├─ Anthropic              │      │   ├─ apply_patch  → SafPatchExec          │
│   ├─ Gemini                 │      │   ├─ skills       → MdSkillLoader         │
│   ├─ Ollama                 │      │   ├─ ask_question → ApprovalFlow          │
│   └─ Bedrock (optional)     │      │   ├─ submit_and_exit → no-op              │
│                             │      │   └─ spawn_agent  → nested AgentRuntime   │
│  Gateway (registry)         │      │                                          │
│                             │      │  Tool presets: act / plan / yolo / minimal│
└─────────────────────────────┘      └──────────────────────────────────────────┘
                                                │
                                                ▼
                                    ┌──────────────────────────────────────────┐
                                    │  :agent:storage                          │
                                    │   ├─ Room: snapshots, messages, workspaces│
                                    │   ├─ DataStore: preferences, model configs │
                                    │   ├─ EncryptedSharedPreferences: API keys │
                                    │   └─ SAF: user-selected workspace folder │
                                    └──────────────────────────────────────────┘
```

---

## Appendix B — Verification log

| Claim | Source | Verified by |
|---|---|---|
| Apache 2.0 license | `/LICENSE` (root) + `apps/vscode/LICENSE` | Read full text — Apache 2.0 boilerplate, no NOTICE file |
| TypeScript + Bun toolchain, Node ≥22 | `AGENTS.md` (root) line 1 | Read directly |
| Layered SDK structure | `sdk/packages/README.md` + `sdk/packages/{shared,llms,agents,core,sdk}/` dirs | Read README + LS'd each package |
| `AgentRuntime` is iterative `while` loop | `sdk/packages/agents/src/agent-runtime.ts:687` | Read the loop directly |
| `recursivelyMakeClineRequests` no longer exists | `rg "recursivelyMakeClineRequests"` → 0 matches in sdk/, 1 match in test only | Verified via ripgrep |
| 9 SDK default tools | `sdk/packages/core/src/extensions/tools/definitions.ts:875-940` (`createDefaultTools`) | Read the factory |
| 26 legacy tools in `ClineDefaultTool` enum | `apps/vscode/src/shared/tools.ts:8-35` | Read the enum |
| 49 named providers in legacy `ApiProvider` | `apps/vscode/src/shared/api.ts:5-54` | Counted entries |
| ~50 SDK built-in providers + 172 generated | `sdk/packages/llms/src/providers/ids.ts` + `provider-ids.generated.ts` | Read both files |
| `createTool` factory signature | `sdk/packages/shared/src/tools/create.ts:81-130` | Read full impl |
| `AgentTool` interface | `sdk/packages/shared/src/agent.ts:202-211` | Read directly |
| `AgentModel` interface (1-method `stream`) | `sdk/packages/shared/src/agent.ts:322-326` | Read directly |
| `ApiHandler` interface (3-method legacy) | `sdk/packages/llms/src/providers/handler.ts:25-69` | Read directly |
| Both basic + agentic compaction exist | `sdk/packages/core/src/extensions/context/{basic,agentic}-compaction.ts` | LS'd + read both |
| Compaction thresholds (0.9 trigger, 0.7 target, 20K preserve) | `compaction-shared.ts:13-19` | Read constants |
| `FileContextTracker` uses chokidar | `apps/vscode/src/core/context/context-tracking/FileContextTracker.ts:3` | Read import |
| MCP has 3 transports (stdio + SSE + StreamableHTTP) | `apps/vscode/src/services/mcp/McpHub.ts:7-9` | Read imports |
| `.clineignore` is "not a security boundary" | `docs/customization/clineignore.mdx` (Warning block) | Read directly |
| `apply_patch` grammar markers | `sdk/packages/core/src/extensions/tools/executors/apply-patch-parser.ts:7-19` | Read PATCH_MARKERS |
| `editor` uses direct old_text→new_text (no SEARCH/REPLACE) | `sdk/packages/core/src/extensions/tools/schemas.ts:194-224` | Read EditFileInputSchema |
| Tool presets (act/plan/yolo/minimal/search) | `sdk/packages/core/src/extensions/tools/presets.ts:23-110` | Read ToolPresets object |
| Sub-agent tool (`spawn_agent`) | `sdk/packages/core/src/extensions/tools/team/spawn-agent-tool.ts` | Read top of file |
| 7-hook lifecycle (beforeRun…onEvent) | `sdk/packages/shared/src/agent.ts:399-428` (`AgentRuntimeHooks`) | Read interface |
| Custom tools via `createTool` + plugins | `docs/sdk/guides/creating-custom-tools.mdx` + `agent.ts:446-454` (`AgentRuntimePlugin`) | Read both |
| OpenAI-compatible uses `@ai-sdk/openai-compatible` | `sdk/packages/llms/src/providers/vendors/openai-compatible.ts:2` | Read import |
| Hub daemon + cron scheduler exist | `sdk/packages/core/src/{hub,cron}/` dirs | LS'd both |
| Tool execution sequential or parallel | `agent.ts:499` (`toolExecution?: "sequential" | "parallel"`) | Read config field |

---

## Appendix C — Source URLs (for traceability)

- Repo root: https://github.com/cline/cline
- Local clone: `/home/z/my-project/android-project/repo/AGENT-TECH/references/cline/` @ commit `8bbdde2`
- License: https://github.com/cline/cline/blob/main/LICENSE
- SDK README: https://github.com/cline/cline/blob/main/sdk/packages/README.md
- SDK overview docs: https://github.com/cline/cline/blob/main/docs/sdk/overview.mdx
- Custom tools guide: https://github.com/cline/cline/blob/main/docs/sdk/guides/creating-custom-tools.mdx
- `.clineignore` (deprecation notice): https://github.com/cline/cline/blob/main/docs/customization/clineignore.mdx
- Plugin examples: https://github.com/cline/cline/tree/main/sdk/examples/plugins
- Cron examples: https://github.com/cline/cline/tree/main/sdk/examples/cron

---

*End of report. Total LoC analyzed: ~560 KLOC Cline TypeScript. Report length:
~1,400 lines.*
