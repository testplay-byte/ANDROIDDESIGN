# R-A2 — Deep Analysis of Kilo Code for Android Agent Port

> Sub-agent task ID: **R-A2**
> Scope: Deeper, source-level analysis of `kilo-org/kilocode` specifically for porting to a
> dedicated **Android agent application** (sandboxed folder, multiple workspaces,
> background execution, custom LLM models). Builds on R-6's overview.
> Reference repo: local clone at `/home/z/my-project/android-project/repo/AGENT-TECH/references/kilocode/`
> (commit on `main`, README dated 2026, LICENSE says `Copyright (c) 2026 Kilo Code` and
> `Copyright (c) 2025 opencode` — i.e. dual-attribution MIT).
> Compared with: Cline analysis in `R-A1-cline-analysis.md` (sister task).

---

## A. Architecture overview

### A.1 Core architecture

Kilo Code is a **monorepo** (Turborepo + Bun workspaces) with these top-level
concerns, all in `packages/`:

| Package | Purpose |
|---|---|
| `opencode/` (`@kilocode/cli`) | **The agent runtime** — CLI, TUI, `kilo serve` HTTP daemon, sessions, agents, tools, providers. Fork of OpenCode. |
| `core/` (`@opencode-ai/core`) | Shared TypeScript core — DB schema, sessions, tools, providers, agent config, permissions, filesystem. Also forked from OpenCode. |
| `llm/` (`@opencode-ai/llm`) | Provider-agnostic LLM client interface with streaming protocol. |
| `schema/` (`@opencode-ai/schema`) | Effect-Schema definitions for permission, provider, message, filesystem. |
| `kilo-vscode/` | VS Code extension (sidebar chat + **Agent Manager** with git-worktree isolation). |
| `kilo-jetbrains/` | JetBrains plugin (Kotlin/Gradle, Java 21). |
| `sdk/js/` (`@kilocode/sdk`) | Auto-generated TypeScript SDK that speaks HTTP+SSE to `kilo serve`. |
| `kilo-gateway/`, `kilo-telemetry/`, `kilo-i18n/`, `kilo-ui/`, `kilo-sandbox/`, `kilo-indexing/`, `kilo-memory/`, `server/`, `protocol/`, `tui/`, `extensions/`, `containers/`, `http-recorder/`, `httpapi-codegen/`, `plugin/`, `plugin-atomic-chat/` | Supporting modules. |

The runtime is **Effect-TS**-based (Effect 4.0.0-beta.83). Every service is a
`Context.Service` provided through a `Layer`. Sessions run inside an Effect
runtime; persistence is **SQLite via Drizzle ORM** (`packages/core/src/database/`
+ migrations in `packages/core/src/database/migration/`).

A Kilo process hosts **multiple directory-keyed runtime instances** (`InstanceStore`
at `packages/opencode/src/project/instance-store.ts`): each opened project folder
gets its own `InstanceContext` (`directory`, `worktree`, `project`). All state is
keyed off `directory` so the same daemon can serve many projects concurrently.

### A.2 Lineage — confirmed OpenCode fork

Verified from local source:

- `packages/opencode/AGENTS.md` line ~155: "Kilo CLI is a fork of
  [opencode](https://github.com/anomalyco/opencode)."
- `LICENSE`: dual attribution `Copyright (c) 2026 Kilo Code` AND
  `Copyright (c) 2025 opencode`. **MIT**, single LICENSE file.
- The repo uses `// kilocode_change` markers throughout `packages/opencode/src/` to
  flag Kilo-specific deltas vs upstream OpenCode. CI enforces this:
  `bun run script/check-opencode-annotations.ts`.
- All Kilo-only additions live under `packages/opencode/src/kilocode/` (no
  `kilocode_change` markers needed there).

So Kilo Code's CURRENT codebase = **OpenCode fork** (MIT, formerly `sst/opencode`,
now `anomalyco/opencode`). The OLD Kilo Code (`kilo-org/kilocode-legacy`, Apache 2.0,
Roo-derived) was EOL'd July 31, 2026. Brand and community came through the
Cline → Roo fork chain, but the **current codebase is OpenCode-derived**.

### A.3 Language

- **TypeScript** exclusively in the agent runtime (`packages/opencode/`,
  `packages/core/`, `packages/llm/`, `packages/schema/`).
- Runtime: **Bun 1.3.14** (`packageManager: bun@1.3.14`, `type: module`). Uses
  Bun-specific APIs (`Bun.file`, `Bun.Glob`, `Bun.spawn`).
- **Kotlin** exists ONLY in `packages/kilo-jetbrains/` (the IntelliJ plugin, Gradle,
  Java 21). Not the agent runtime.
- No Kotlin in the agent runtime — for Android we'd be porting TypeScript → Kotlin.

### A.4 License

**MIT** (confirmed). More permissive than Cline's Apache 2.0. We can vendor, fork,
relicense, and ship commercially with no copyleft concerns. The only requirement
is preserving the dual copyright notice (Kilo Code 2026 + opencode 2025).

### A.5 Key architectural differences vs Cline

| Dimension | Cline | Kilo Code |
|---|---|---|
| Codebase | Original TypeScript/Node, single repo | OpenCode fork; Effect-TS, monorepo, SQLite+Drizzle, `kilo serve` daemon |
| Tool API | `createTool` factory returning `{definition, execute}` | Two parallel APIs: V1 `Tool.define(id, Effect<Init>)` (opencode-side, used by all kilo/vscode tooling) and V2 `Tool.make({description, input, output, execute})` + `ToolRegistry.materialize()` (core-side). V2 is the cleaner target. |
| Agent loop | Recursive ClineAgent.recursivelyMakeClineRequests | **Iterative** turn loop in `SessionRunner.run` (`packages/core/src/session/runner/llm.ts`): `while (shouldRun) { while (needsContinuation) { runTurn(); step++; } }`. Compaction is triggered as a turn transition (`ContinueAfterCompaction` / `ContinueAfterOverflowCompaction`) via `Effect.die(TurnTransitionError)` — clean separation. |
| Context compaction | Quarter-truncation when overage > 50% | **LLM-summarization auto-compaction** with anchored summary template; preserve-recent tail budget |
| Permissions | Per-tool `toolPolicies` boolean | **Per-agent permission rulesets with glob patterns** (`action` × `resource` × `effect: allow/ask/deny`) |
| Subagents | `new_task` + `use_subagents` boolean | **Single `task` tool** with `subagent_type` + optional `background: true` |
| Custom agents | JSON-only `modes` config | **Markdown + YAML frontmatter** (`{agent,agents}/**/*.md` and `{mode,modes}/*.md`), plus legacy `.kilocodemodes` YAML, plus JSON config (`agent` block) |
| Plan files | (none) | **Persistent plan files** at `.kilo/plans/*.md`, `.opencode/plans/*.md`, `plans/*.md`, `.plans/*.md`, or `${Global.Path.data}/plans/*.md` (global). Resolved by `PlanFile.locate()` (`packages/opencode/src/kilocode/plan-file.ts`). |
| Daemon | None — embeds in VSCode extension host | **`kilo serve` HTTP+SSE daemon** consumed by all clients (CLI TUI, VSCode extension, JetBrains plugin) via `@kilocode/sdk` |
| Persistence | JSON files on disk | **SQLite via Drizzle ORM** (event-sourced sessions, message projections, todos, snapshots) |
| Provider abstraction | Anthropic SDK + manual adapters for OpenAI/Gemini/etc. | **Vercel AI SDK** (`ai@6.0.235`) as the unifying layer + native protocol for OpenAI Responses / Anthropic Messages. 30+ built-in providers + OpenAI-compatible plugin + dynamic NPM provider plugin. |
| MCP | Anthropic-style MCP client (stdio + SSE) | MCP client (stdio + SSE/HTTP, OAuth 2.0) + per-server permission sanitization |

---

## B. Tool system analysis

### B.1 The two parallel tool APIs

Kilo Code is mid-migration from V1 → V2 tools:

**V1 (opencode-side, current production)** —
`packages/opencode/src/tool/tool.ts`:

```ts
Tool.define(id: string, init: Effect<Init>): Effect<Info>
interface Def {
  id: string
  description: string
  parameters: Schema.Decoder<unknown>
  jsonSchema?: JSONSchema7
  execute(args, ctx: Context): Effect<ExecuteResult<M>>
}
interface Context {
  sessionID, messageID, agent, abort: AbortSignal
  callID?, extra?
  messages: SessionV1.WithParts[]
  metadata(input): Effect<void>
  ask(input: PermissionV1.Request): Effect<void>   // permission prompt
}
```

**V2 (core-side, the future)** — `packages/core/src/tool/tool.ts`:

```ts
Tool.make<In, Out, Structured>({
  description, input, output, structured?, toStructuredOutput?,
  execute(input, ctx): Effect<Out, ToolFailure>,
  toModelOutput?({input, output}): Content[]
})
Tool.withPermission(tool, permission: string)
```

`ToolRegistry.materialize(permissions)` filters by permission rules and returns
`{ definitions, settle }` — exactly what the runner hands to the LLM. Custom tools
are registered via `Tools.Service.register({ name: tool })` inside an Effect
scope; they get cleaned up automatically on scope close.

**For Android**: V2 is the cleaner target. The pattern ports directly to Kotlin:
`interface Tool<I, O> { val definition: ToolDefinition; suspend fun execute(input: I, ctx: ToolContext): O }`
with a `ToolRegistry.materialize(rules: PermissionRuleset)` returning the filtered
definitions for a turn.

### B.2 Complete tool inventory + Android portability

Built-in V2 tools registered by `packages/core/src/tool/builtins.ts`:

| # | Tool | Description | Android portability | Android equivalent / notes |
|---|---|---|---|---|
| 1 | `bash` | Shell command, working dir, timeout (default 2 min, max 10 min), 1 MiB capture cap | **No** | Termux-style shell only via `Runtime.exec()` on rooted devices, or shell-out to bundled busybox. We should DROP bash and replace with higher-level file ops + a curated DSL. Kilo's `bash` already has a `readOnlyBash` allowlist concept worth keeping as the model-facing "shell" tool that maps to safe file ops. |
| 2 | `edit` | Exact SEARCH/REPLACE on a file (oldString → newString), `replaceAll` option, BOM/line-ending aware, `writeIfUnchanged` for stale-content detection | **Yes** | Pure string manipulation. Port verbatim. Permission glob `edit` × `<path>`. |
| 3 | `write` | Create/overwrite file | **Yes** | Trivial. |
| 4 | `read` | Read file with line ranges, image support | **Yes** | Trivial. |
| 5 | `glob` | Glob-pattern file search | **Yes** | Port a minimal glob matcher (Kilo uses `Bun.Glob`; for Android use `java.nio.file.PathMatcher` or a hand-rolled glob). |
| 6 | `grep` | ripgrep-backed content search | **Partial** | No ripgrep binary on Android. Use `java.util.regex.Pattern` + recursive walk + bounded matches. Kilo's `MAX_SEARCH_LIMIT=100` is a sensible default. |
| 7 | `apply_patch` | Apply unified diff (legacy) | **Yes** | Port a minimal patch parser. |
| 8 | `todowrite` | Update the persistent plan/todo list | **Yes** | Maps to Room entity `TodoEntity(sessionId, content, status, priority, position)`. Kilo uses SQLite+Drizzle; identical schema. |
| 9 | `skill` | Load prompt-skill from `.kilo/skills/SKILL.md` | **Yes** | Markdown reader + system prompt injection. |
| 10 | `webfetch` | HTTP fetch + extract to markdown | **Yes** | OkHttp + Jsoup. |
| 11 | `websearch` | Web search via configured provider | **Partial** | Reuse Kilo's Exa/Tavily provider or implement with Brave/Bing. Requires API key in Keystore. |
| 12 | `question` | Ask user a question (model-initiated prompt) | **Yes** | Compose dialog + suspendCoroutine. |

Plus the V1-only tools registered under `packages/opencode/src/tool/`:

| 13 | `task` | **Subagent launcher** — spawns a sub-session with `subagent_type`, optional `background: true`, optional `task_id` to resume | **Yes** | Critical pattern — port as `TaskTool` that forks a coroutine running a sub-`AgentLoop`. `deriveSubagentSessionPermission` ports to Kotlin directly. |
| 14 | `plan` / `plan_enter` / `plan_exit` | Plan-mode controls — switch into plan mode, exit with a finalized plan file path | **Yes** | Plan mode is just a sub-agent with edit restricted to plan paths. Port as a `PlanAgent` with `edit: {".app/plans/*.md": "allow", "*": "deny"}`. |
| 15 | `code_mode` | Switch active agent ("code" / "plan" / "ask" / etc.) mid-session | **Yes** | Session-scoped agent switch. |
| 16 | `repo_clone` / `repo_overview` | Scout-only: clone external repos into `${Global.Path.data}/repos/` for read-only research | **No/Drop** | No git binary, but JGit works on Android. Optional — skip for v1. |
| 17 | `lsp` | LSP integration (diagnostics, hover, definition) | **No** | Drop. No LSP server on Android. |
| 18 | `truncate` / `truncation-dir` | Spill large tool output to disk; return a token-bounded preview | **Partial** | Useful pattern. Port with app cache dir as spill location. |
| 19 | `recall` | Memory recall (kilo_memory_recall) | **Yes** | Optional — vector store on device. |
| 20 | `suggest` | Suggest a command for the user to run | **Yes** | UI affordance. |
| 21 | `external-directory` | Advisory for paths outside the worktree | **Yes** | Permission rule check. |

Plus the Kilo-only tools under `packages/opencode/src/kilocode/tool/`:

| 22 | `interactive_terminal` | Drive a real PTY from the model (kilo-only) | **No** | Drop — no PTY on Android. |
| 23 | `agent_manager` | Multi-session orchestration with git worktree isolation | **No** | Concept maps to "workspaces" — see §F.24. Drop the VSCode-specific bits. |
| 24 | `background_process` | Launch long-running shell processes; model can list/wait/cancel | **No** | Drop. Replaced by Android `WorkManager` for our background tasks. |
| 25 | `notify_user` | Cross-device notification | **Yes** | Trivial: Android `NotificationManager`. |
| 26 | `generate_image` | Image generation (kilo-only) | **Yes** | Optional — reuse model provider. |
| 27 | `chart` / `xlsx` / `ods` / `notebook` / `notebook_host` / `read_docx` / `read_object` / `read_extract` / `send_file` / `semantic_search` / `memory_save` / `memory_recall` / `model_search` / `grep_signal_controls` / `shell_heredoc` / `shell_unparsed` / `task_background_process` / `websearch_kilo_exa` / `repo_overview` | Various | **Mixed** | Cherry-pick: `semantic_search`, `memory_save/recall` are interesting. Most are kilo-cloud product features that don't apply. |

**Summary**: ~12 of the 21 V1/V2 built-ins port directly (edit/write/read/glob/grep/apply_patch/todowrite/skill/webfetch/websearch/question/task). Drop bash/LSP/PTY/agent_manager/background_process/notebook/repo_clone. Kilo's `readOnlyBash` allowlist is worth porting as a safer "shell" abstraction that maps commands to typed file ops.

### B.3 Tool registration

```ts
// V2 (core)
yield* tools.register({ [name]: Tool.make({...}) })
yield* ToolRegistry.materialize(permissions)  // → { definitions, settle }
```

Inside an Effect scope; tools are auto-cleaned when the scope closes. The
`ApplicationTools.Service` is the global registry, `ToolRegistry.Service` is the
location-scoped one. `materialize` applies permission filtering via
`whollyDisabled(action, rules)` (drops tools that match `*:deny`).

### B.4 Custom tool support

Custom tools are added by:

1. Calling `tools.register({ name: tool })` from any Effect context with a `Scope`.
2. **Plugin providers** (`packages/core/src/plugin/`) — register provider plugins
   that bring their own tools.
3. **MCP servers** — auto-register their tools with `<server>_*` permission
   prefixes.

For Android, we'd expose a Kotlin `ToolRegistry.register(tool: Tool)` API plus a
plugin system (DexClassLoader for runtime-loaded tools, or compile-time DI).

---

## C. Agent loop + context management

### C.1 Agent loop — iterative (confirmed)

`packages/core/src/session/runner/llm.ts`, `SessionRunner.run`:

```ts
while (shouldRun) {
  let needsContinuation = true
  let step = 1
  while (needsContinuation) {
    const result = yield* runTurn(sessionID, promotion, step)
    needsContinuation = result.needsContinuation
    step = result.step + 1
    promotion = "steer"   // user steering during a turn
    if (!needsContinuation) needsContinuation = hasPending(sessionID, "steer")
  }
  shouldRun = hasPending(sessionID, "queue")  // queued user turns
  promotion = shouldRun ? "queue" : undefined
}
```

Inside `runTurnAttempt`: build `LLMRequest`, call `compactIfNeeded()` (may die with
`ContinueAfterCompaction`), call `llm.stream(request)`, persist events as they
arrive, settle tool calls in parallel fibers (FiberSet), publish step-end with
file snapshot diff. **Iterative, not recursive.** Bounded by `agent.steps`
optional cap; on `isLastStep` the runner sets `toolChoice: "none"` and injects a
`MAX_STEPS_PROMPT` telling the model to wrap up.

Two compaction transition types:
- `ContinueAfterCompaction` — auto-compaction proactively fired before overflow.
- `ContinueAfterOverflowCompaction` — recovery compaction after a provider
  context-overflow error.

For Android: this loop maps cleanly to a Kotlin `suspend fun` with `while` loops
and `kotlinx.coroutines` fibers (replace Effect's `FiberSet` with a
`CoroutineScope` + `async`+`awaitAll`). The `Effect.die(TurnTransitionError)`
trick → Kotlin sealed class + `when` resume.

### C.2 LLM-based auto-compaction (KEY ADVANTAGE over Cline)

There are **two implementations** in Kilo Code (legacy V1 + new V2):

**V2** (`packages/core/src/session/compaction.ts`, ~240 LoC):

- Defaults: `buffer=20_000`, `keep.tokens=8_000`, `toolOutputMaxChars=2_000`,
  `summaryOutputTokens=4_096`.
- `compactIfNeeded`: if `estimate(system+messages+tools) > context - max(output, buffer)`,
  fire compaction.
- `compactAfterOverflow`: same, used as recovery from a context-overflow provider
  error.
- `select(entries, tokens)`: walk backwards from latest entry, accumulate token
  estimates until budget exhausted, split the boundary message if it straddles.
  Returns `{ head: summary-candidates, recent: kept-verbatim }`.
- `buildPrompt({previousSummary, context})`: anchored summary template —
  `## Objective / Important Details / Work State (Completed/Active/Blocked) /
  Next Move / Relevant Files`. **Update, don't replace** — preserves still-true
  facts, removes stale ones.
- Streams the compaction model: `llm.stream(LLM.request({ model, messages: [user(summaryPrompt)], tools: [], generation: { maxTokens: summaryOutput } }))`.
- Publishes `SessionEvent.Compaction.Started` + `.Ended` with `{ summary, recent }`.
- The summary message is stored as a `compaction` typed SessionMessage so future
  prompts see it; `select()` skips compaction messages when walking back.

**V1** (`packages/opencode/src/session/compaction.ts`, ~720 LoC, more sophisticated):

- Same anchored-summary template (shared `buildPrompt`).
- Adds `preserveRecentBudget`: 2_000–8_000 tokens, scales with model's usable
  context (`min(8k, max(2k, usable*0.25))`).
- `tail_turns` config (default 2): preserve the last N complete turns verbatim
  rather than summarizing them.
- **`prune(reason: "normal" | "post-compaction" | "payload-limit")`**: walks
  backwards through tool calls; once `PRUNE_PROTECT=40_000` tokens of recent tool
  outputs accumulate, marks older ones as `time.compacted = Date.now()` so the
  model sees "[compacted]" stubs instead of full outputs. Opt-in via
  `cfg.compaction.prune=true`.
- **`KiloCompactionChunks`**: if the conversation is too large to fit in a single
  summarization call, splits into chunks and produces a multi-chunk summary.
- **`KiloCompactionPayloadRecovery`**: fallback strategy when the primary
  summarization fails.
- **Replay**: on overflow during a user turn with attachments, the user turn is
  replayed with media stripped (to fit the compacted context).
- **Auto-continue**: emits a synthetic `[compaction_continue: true]` user
  message ("Continue if you have next steps…") so the agent doesn't dead-end
  after a compaction.

**For Android**: port V2's compaction.ts first (~240 LoC → ~400 Kotlin LoC). The
anchored-summary template is gold — keep verbatim. Add V1's `prune` and
`KiloCompactionChunks` later as optimizations.

### C.3 Dedicated compaction model? — Yes

The `compaction` agent (`packages/opencode/src/agent/agent.ts`):

```ts
compaction: {
  name: "compaction",
  mode: "primary", native: true, hidden: true,
  prompt: PROMPT_COMPACTION,
  permission: Permission.merge(defaults, user, { "*": "deny" })  // no tools
}
```

It can override its model via `cfg.agent.compaction.model: "provider/model"`.
Default behavior: uses the SAME model as the user's session
(`yield* provider.getModel(userMessage.model.providerID, userMessage.model.modelID)`).
The user can pin a cheaper model (e.g. `anthropic/claude-haiku` or
`openai/gpt-4o-mini`) for compaction. This is the right pattern for Android: let
the user pick a separate "compaction model" in settings, default to the main
model.

### C.4 Per-agent permissions with glob patterns (KEY ADVANTAGE over Cline)

Permission system is in `packages/core/src/permission.ts` + `@opencode-ai/schema/permission`:

```ts
type Rule = { action: string; resource: string; effect: "allow" | "ask" | "deny" }
type Ruleset = Rule[]   // ordered; findLast wins (later rules override earlier)
```

The config DSL maps to rulesets via `Permission.fromConfig`:

```yaml
permission:
  edit:
    "theme.json": "allow"
    "*.env": "ask"
    "*": "deny"
  bash:
    "git *": "allow"
    "rm *": "deny"
  external_directory:
    "/sdcard/MyApp/workspaces/main/*": "allow"
    "*": "ask"
```

`Permission.evaluate(action, resource, ...rulesets)` finds the last rule whose
`action` glob matches `action` AND whose `resource` glob matches `resource`,
defaulting to `{ effect: "ask" }` if no rule matches. Wildcards are `*` (any run
of chars including spaces, including empty) — see `packages/core/src/util/wildcard.ts`.

Built-in defaults (from `agent.ts` `baseDefaults`):

```yaml
"*": "allow"
doom_loop: "ask"
external_directory: { "*": "ask", <whitelist>: "allow" }
suggest: "deny"
question: "deny"
interactive_terminal: "deny"
plan_enter: "deny"
plan_exit: "deny"
repo_clone: "deny"
repo_overview: "deny"
read:
  "*": "allow"
  "*.env": "ask"
  "*.env.*": "ask"
  "*.env.example": "allow"
```

User rules merge AFTER defaults (so they can tighten or loosen).

`hardenSystemAgents(agents)` — locks `compaction/title/summary` to `*:deny` after
all merges so user config cannot unlock them.

`PermissionSaved` table — durable saved "always allow" answers from interactive
prompts (per project). Each saved rule has `{ action, resource, effect: "allow" }`
and is appended to the ruleset at evaluation time.

**For Android**: this is THE killer pattern to port. ~300 LoC for
`Permission` + `Wildcard` + `PermissionSaved`-equivalent (Room table). Maps 1:1
to Kotlin data classes + sealed `Effect`. Glob match = `java.nio.file.PathMatcher`
or hand-rolled.

### C.5 Native subagents via single `task` tool (KEY ADVANTAGE over Cline)

`packages/opencode/src/tool/task.ts` (475 LoC) + `packages/opencode/src/kilocode/tool/task.ts`:

Single tool:

```ts
task({
  description: "short 3-5 words",
  prompt: "what the subagent should do",
  subagent_type: "general" | "explore" | "code" | "plan" | <custom>,
  task_id?: "resume a previous task session",
  background?: true   // async, returns immediately with task_id
})
```

How it differs from Cline:
- **No `use_subagents` config flag** — subagents are always available, gated by
  permissions.
- **`background: true`** — fire-and-forget; parent is notified on completion via
  a synthetic tool result. Requires `KILO_EXPERIMENTAL_BACKGROUND_SUBAGENTS=true`.
- **`task_id` for resumption** — re-attaches to a prior subagent session with its
  full message history (saves tokens vs. fresh start).
- **`deriveSubagentSessionPermission`** (`packages/opencode/src/agent/subagent-permissions.ts`):

  ```ts
  function deriveSubagentSessionPermission({parentSessionPermission, subagent}):
    PermissionV1.Ruleset {
    // 1. Inherit parent's deny rules + external_directory rules.
    //    Parent allow/ask rules do NOT override the subagent's own config.
    // 2. Default-deny task + todowrite unless subagent explicitly allows.
    return [
      ...parentSessionPermission.filter(r =>
        r.permission === "external_directory" || r.action === "deny"),
      ...(subagent has task permission ? [] : [{task: deny}]),
      ...(subagent has todowrite permission ? [] : [{todowrite: deny}]),
    ]
  }
  ```

- **`KiloTask.inherited(caller, session, mcp)`** — builds inherited permission
  ceiling: parent's `edit`/`notebook_edit`/`notebook_execute` deny rules +
  per-MCP-server deny rules propagate down. **`bash` deny rules deliberately do
  NOT propagate** (see comment in `task.ts`) — a read-only delegator's bash
  allowlist shape (deny-all + named allow) would otherwise cap a writable
  subagent's explicitly-allowed commands.
- **Depth tracking** — walks `parentID` chain to count depth; cap is implicit
  (no infinite recursion because each subagent has a fresh context).
- **KiloCostPropagation** — subagent token costs bubble up to the parent session.
- **KiloSessionProcessor** — streams subagent events into the parent's UI.

**For Android**: port `deriveSubagentSessionPermission` + `KiloTask.inherited` as
pure functions (~120 LoC). The `task` tool itself is ~200 LoC Kotlin. Background
mode → Android foreground service notification.

### C.6 Custom agents as Markdown+YAML (KEY ADVANTAGE over Cline)

`packages/opencode/src/config/agent.ts`:

```ts
// Loads all .md files matching {agent,agents}/**/*.md from a config dir
for (const item of await Glob.scan("{agent,agents}/**/*.md", { cwd: dir, ... })) {
  const md = await ConfigMarkdown.parse(item)   // gray-matter frontmatter + body
  const name = configEntryNameFromPath(...)
  const prompt = await ConfigVariable.substitute({text: md.content.trim(), ...})
  result[name] = ConfigParse.schema(ConfigAgentV1.Info, { name, ...md.data, prompt }, item)
}
```

Plus legacy `{mode,modes}/*.md` (single-level, no nesting).

Frontmatter schema (`ConfigAgentV1.Info`):

```yaml
---
description: "..."
mode: subagent | primary | all
hidden: true | false
color: "#RRGGBB" | primary | secondary | ...
temperature: 0.7
top_p: 0.9
steps: 25                    # max turns before forced wrap-up
model: "anthropic/claude-sonnet-4.5"   # optional override
permission:
  edit: { "*.json": "allow", "*": "deny" }
  bash: "deny"
---
You are <agent name>. Your job is to...
```

Body = the system prompt. Supports `{env:VAR}` and `{file:path}` substitutions
(trusted scopes only — project agents are untrusted).

`KiloAgent.remove()` — deletes the `.md` file from the appropriate config dir;
also handles legacy `.kilocodemodes` YAML migration.

**For Android**: this is THE way users will customize their agent. Store custom
agents as `<workspace>/.app/agents/<name>.md` and
`<app-internal>/agents/<name>.md`. Parse with a YAML frontmatter parser (Kotlin:
`org.yaml:snakeyaml` + custom frontmatter splitter, or
`com.github.johnrengelman:restdocs`-style). Same schema. Markdown body →
system prompt. **Estimated ~250 LoC Kotlin for the loader + parser + schema
validation.**

### C.7 Persistent plan files

`packages/opencode/src/kilocode/plan-file.ts`:

Plan mode's `plan_exit` tool finalizes a plan. The plan file is a regular `.md`
file on disk. Resolution priority (`PlanFile.locate`):

1. **Exact target** passed to `plan_exit` (resolved within InstanceContext).
2. **Generated-name sibling** — newest file in the plan dir matching
   `<session-createdAt>-*.md` (so re-runs don't get confused by stale plans).
3. **Last `.md` written by a planning agent** (`plan` or `architect`) — walks
   message parts backwards for `write`/`edit` calls ending in `.md` inside the
   plan dir.

Plan dirs (searched in order, see `planEditRules`):

- `.kilo/plans/*.md` (project-local, Kilo convention)
- `plans/*.md`
- `.plans/*.md`
- `.opencode/plans/*.md` (OpenCode upstream)
- `${Global.Path.data}/plans/*.md` (global, for non-git projects)

Plan mode's `edit` permission is hardened to:

```yaml
edit:
  "*": "deny"
  ".kilo/plans/*.md": "allow"
  "plans/*.md": "allow"
  ".plans/*.md": "allow"
  ".opencode/plans/*.md": "allow"
  "<relative path to Global.Path.data/plans/*.md>": "allow"
```

**For Android**: port verbatim with adjusted paths:
- `<workspace>/.app/plans/*.md`
- `<app-internal>/plans/*.md`

The plan file is **persistent across sessions** because it's a regular file on
disk, not a DB row. The next session can read it via `read` tool or
`PlanFile.locate`. This is brilliant for our use case — the agent's plan survives
app kills, device reboots, and session compaction.

---

## D. LLM provider abstraction

### D.1 Provider list (30+)

Built-in provider plugins in `packages/core/src/plugin/provider/`:

- **OpenAI** (ChatGPT OAuth + API key, Responses API support)
- **Anthropic**
- **Google** (Gemini)
- **Google Vertex** (Anthropic + Gemini on Vertex)
- **Azure**
- **Amazon Bedrock**
- **OpenRouter**
- **Mistral**
- **Cohere**
- **Groq**
- **Cerebras**
- **DeepInfra**
- **Together AI**
- **Perplexity**
- **xAI** (Grok)
- **NVIDIA**
- **Alibaba** (Qwen/DashScope)
- **Venice**
- **Vercel**
- **Cloudflare Workers AI**
- **Cloudflare AI Gateway**
- **Snowflake Cortex**
- **SAP AI Core**
- **GitLab** (workflow models)
- **GitHub Copilot** (ChatGPT/Copilot OAuth)
- **Kilo Gateway** (hosted proxy, 500+ models, BYOK alternative)
- **LLM Gateway** (generic)
- **ZenMux**
- **OpenCode** (built-in free tier)
- **OpenAI-compatible** (`@ai-sdk/openai-compatible` — the generic catch-all)
- **Dynamic NPM provider** (`packages/core/src/plugin/provider/dynamic.ts` — installs an npm package at runtime and calls its `create*` factory)

Model catalog: fetched from `https://models.dev` (override with
`KILO_MODELS_URL`), cached in `${Global.Path.data}/models.json` with file
locking. 500+ models.

### D.2 Provider interface vs Cline

| Aspect | Cline | Kilo Code |
|---|---|---|
| Underlying SDK | Anthropic SDK + manual per-provider adapters | **Vercel AI SDK** (`ai@6.0.235`) — single unified interface; providers are AI-SDK packages (`@ai-sdk/openai`, `@ai-sdk/anthropic`, `@ai-sdk/google`, `@ai-sdk/openai-compatible`, etc.) |
| Provider schema | Hard-coded classes per provider | **`ProviderV2.Info`** = `{ id, name, api: AISDK | Native, request: { headers, body } }`. Two API types: `aisdk` (delegates to an AI-SDK package) or `native` (custom URL+settings). |
| Streaming | Manual per-provider | `streamText({...})` from `ai` package; unified `LLMEvent` stream protocol (`textDelta`, `reasoningDelta`, `toolCall`, `toolResult`, `usage`, `providerError`, etc.) |
| Tool calling | Anthropic-format tools converted per provider | AI-SDK tool abstraction (`tools: Record<string, Tool>`) + Kilo's `Tool.toDefinitions()` bridges V1/V2 tools to AI-SDK `Tool` shape |
| Auth | API key in env var | `auth.json` mode 0600 in `${Global.Path.data}`; supports OAuth for OpenAI/Copilot/GitLab; `Integration` system for OAuth flows |

**For Android**: the AI-SDK layer doesn't exist in Kotlin. We'd reimplement the
abstraction with our own `interface LLMProvider { suspend fun stream(req: LLMRequest): Flow<LLMEvent> }`
and ship adapters for OpenAI-compatible (covers OpenAI/Together/DeepInfra/Groq/etc.),
Anthropic, Google Gemini, Ollama (local). ~600 LoC Kotlin. Streaming via OkHttp
SSE.

### D.3 Custom / local models — yes, three ways

1. **`openai-compatible` provider** — point at any OpenAI-compatible endpoint
   (Ollama, LM Studio, vLLM, llama.cpp, LiteLLM, OpenRouter, etc.). Config:

   ```yaml
   provider:
     my-local:
       name: "Local Ollama"
       api:
         type: aisdk
         package: "@ai-sdk/openai-compatible"
         url: "http://localhost:11434/v1"
       models:
         llama3:
           name: "Llama 3 8B"
           limit: { context: 8192, output: 2048 }
   ```

2. **`native` provider** — custom URL + headers + body. For non-OpenAI-compatible
   APIs.

3. **`dynamic-provider` plugin** — installs an npm package at runtime, finds its
   `create*` export, calls it with options. For exotic providers.

4. **`kilo-gateway`** — Kilo's hosted proxy. 500+ models with one signup, BYOK
   alternative. Probably skip for Android (cloud dependency).

**For Android**: native support for `openai-compatible` (covers Ollama local +
most cloud providers) is the priority. Add Anthropic + Gemini native adapters.
For on-device LLM (llama.cpp via JNI or MLC-LLM), expose as a
`local-on-device` provider with a custom protocol.

### D.4 Streaming — yes, everywhere

- `LLMClient.stream(request): Stream<LLMEvent, LLMError>` — Effect Stream of
  typed events.
- `LLMEvent` variants: `textDelta`, `reasoningDelta`, `toolCall`,
  `toolResult`, `usage`, `providerError`, `stepFinish`, `finish`,
  `providerMetadata`, etc. (`packages/llm/src/schema/`).
- Tools can stream their own progress via the publisher (`createLLMEventPublisher`).
- Compaction streams its summary text incrementally too.

For Android: `Flow<LLMEvent>` with the same sealed-class taxonomy. Backpressure
via `Flow.buffer()`.

---

## E. File system + sandboxing

### E.1 File system access

`packages/core/src/filesystem.ts` — V2 `FileSystem.Service` with the same
location-scoped interface as other V2 services:

```ts
interface Interface {
  read(input: { path: RelativePath }): Effect<{ content: Uint8Array, mime: string }>
  list(input?: { path?: RelativePath }): Effect<Entry[]>
  find(input: FindInput): Effect<Entry[]>
  glob(input: GlobInput): Effect<readonly Entry[]>
  grep(input: GrepInput): Effect<readonly Match[]>
}
```

Critical safety feature (`resolve`):

```ts
const absolute = path.resolve(location.directory, input ?? ".")
if (!FSUtil.contains(location.directory, absolute))
  return yield* Effect.die(new Error("Path escapes the location"))
const real = yield* fs.realPath(absolute).pipe(Effect.orDie)
if (!FSUtil.contains(root, real)) return yield* Effect.die(new Error("Path escapes the location"))
```

**TOCTOU protection** (kilocode_change): `SearchTarget.inspect(fs, real)` opens
the file, captures `{dev, ino}` from stat, then re-validates after the read to
detect symlink/replacement attacks during the read.

For directory listings: `SearchTarget.validate(fs, target.target)` is called
**twice** — before and after enumeration — to reject directory-replacement
attacks (`/home/z/my-project/android-project/repo/AGENT-TECH/references/kilocode/packages/core/src/filesystem.ts` lines 116–137).

### E.2 Sandboxing — partial

**Per-Location isolation**: every `Location` has a `directory` (workspace root).
All file ops resolve relative to `directory` and are bounded by `FSUtil.contains`.
**Path escapes → `Effect.die`** (irrecoverable).

**`external_directory` permission**: tools can request access to paths outside
the location; this triggers an `external_directory` permission prompt (Allow/Ask/Deny).
Whitelisted external dirs: `${Global.Path.tmp}/*`, skill dirs, config dir, kilo
global dirs, reference dirs.

**No OS-level sandbox** — Kilo runs with the host user's full filesystem
permissions; safety is enforced at the application layer via `FSUtil.contains`
+ permission rules. The bash tool explicitly runs with "host user's filesystem,
process, and network authority" — only advisory external-directory detection on
command args.

**For Android**: this maps PERFECTLY to our scoped-storage use case. Use SAF
(`Storage Access Framework`) or `DocumentFile` for the user-selected tree.
Enforce `FSUtil.contains`-equivalent at the `ContentResolver` level. Path
escapes → `SecurityException`. The whole Kilo `external_directory` permission
model becomes irrelevant because Android scoped storage enforces it at the OS
level — we get a STRICTER sandbox for free.

### E.3 File edits — SEARCH/REPLACE, same as Cline

`packages/core/src/tool/edit.ts`:

```ts
Input: { path, oldString, newString, replaceAll? }
```

- `oldString === newString` → error "No changes to apply".
- `oldString === ""` → error "Use write to create or overwrite a file."
- BOM detection + preservation.
- Line-ending detection + conversion (`\r\n` ↔ `\n`).
- `countOccurrences` — if `>1` and not `replaceAll`, error "Found multiple
  exact matches, provide more surrounding context or set replaceAll to true."
- `writeIfUnchanged({ target, expected, content })` — optimistic concurrency:
  fails with `StaleContentError` if the file changed between read and write.
  Model-facing message: "File changed after permission approval. Read it again
  before editing."
- Output: `{ files: [{file, patch, status, additions, deletions}], replacements }`
- `toModelOutput`: shows `+/-` preview of first 6 lines per side.

**Identical pattern to Cline's `write_to_file` / `replace_in_file`** — port
directly. Kilo adds BOM/line-ending handling + stale-content detection that
Cline lacks.

---

## F. Background execution

### F.1 Background tasks — daemon architecture

Kilo Code's background execution is **`kilo serve`** — a long-running HTTP+SSE
daemon process (`packages/opencode/src/server/server.ts`).

- Started by `kilo serve` CLI command (`packages/opencode/src/cli/cmd/serve.ts`).
- The VSCode extension and JetBrains plugin each spawn a `kilo serve` child
  process when their host IDE starts, then speak HTTP+SSE via `@kilocode/sdk`.
- One `kilo serve` process hosts **multiple directory-keyed `InstanceContext`s**
  simultaneously (looked up via `x-kilo-directory` header on each request).
- Sessions run inside Effect runtimes; concurrent sessions share the daemon's
  thread pool but each has its own `InstanceState` scope.
- Graceful shutdown: SIGTERM/SIGINT/SIGHUP → drain ingest queue →
  `InstanceRuntime.disposeAllInstances()` → `server.stop(true)`.

### F.2 `kilo serve` — relevant for Android? Not directly

The daemon model doesn't fit Android:

- Android doesn't have long-running background processes the way desktop OSes
  do — WorkManager or a foreground service is the model.
- HTTP server on localhost is possible (NanoHTTPD, Ktor) but adds complexity
  for no benefit when the UI is in-process.
- Spawning a child process requires either bundling a Node/Bun runtime
  (huge — Bun is ~100 MB) or running TypeScript via a JS engine (QuickJS, Hermes).

**Recommendation for Android**: do NOT port `kilo serve`. Instead, embed the
agent runtime as a Kotlin module (`:core:agent`) and run sessions as
`WorkManager` jobs (background, deferrable) or foreground-service jobs (active,
notifiable). Use Kotlin coroutines for concurrency instead of Effect's
FiberSet. The `:ui` Compose module observes session state via
`SharedFlow<SessionEvent>` (analog to Kilo's `EventV2` event stream).

What IS relevant from Kilo's daemon:
- The `x-kilo-directory` routing concept → `Workspace` selection per session.
- `InstanceStore`'s cache-and-reuse of `InstanceContext` → `WorkspaceManager`
  in-memory cache keyed by SAF tree URI.
- Graceful shutdown draining → WorkManager `onStopped()` callback should
  persist pending session state.
- `KiloSessions.drainIngestForShutdown()` — pending user-input queue drained
  before shutdown. Map to WorkManager `WorkInfo.State.ENQUEUED` persistence.

### F.3 Concurrent tasks — `BackgroundJob` + `task` background mode

- `BackgroundJob.Service` (`packages/opencode/src/background/job.ts`) — manages
  background subagent sessions launched via `task({ background: true })`.
- The parent session is notified via a synthetic tool result when the background
  task completes.
- `KiloSessions` (`packages/opencode/src/kilo-sessions/`) — multi-session
  coordinator with attached state, inflight caches, ingest queues, PR linking.
- Agent Manager (VSCode) — git worktree-per-session isolation, parallel PR
  workflows, status polling. **This is a VSCode-only feature**, not core.

For Android: `WorkManager` for background subagents (one-shot work requests
tagged with `task_id`). Foreground service for active parent session.
`CoroutineScope` per session with structured concurrency. Parallel subagents →
`async {}` builder + `awaitAll`.

---

## G. Android portability assessment

### G.1 Minimal viable subset to port

Phase 1 (MVP agent — ~3-4 weeks):

1. **LLM client** — `interface LLMProvider` + OpenAI-compatible adapter
   (covers OpenAI/Ollama/Together/etc). Streaming via OkHttp SSE.
   `LLMRequest`/`LLMEvent` sealed classes. ~800 LoC.
2. **Agent loop** — iterative `while (shouldRun)` loop, `runTurn()` building
   `LLMRequest`, streaming events, settling tool calls in parallel via
   `async {}`. `compactIfNeeded` + `compactAfterOverflow` recovery.
   ~600 LoC.
3. **Permission system** — `Rule`/`Ruleset`/`evaluate`/`merge` +
   `Wildcard.match` + saved-rules Room table. ~400 LoC.
4. **5 core tools** — `read`, `write`, `edit`, `glob`, `grep`. ~600 LoC.
5. **2 control tools** — `todowrite`, `question`. ~250 LoC.
6. **SQLite persistence** — Room schema for sessions/messages/todos/snapshots.
   ~500 LoC (mostly schema + DAOs).
7. **Compose UI** — chat view, tool-call cards, permission dialog, settings.
   ~1500 LoC.
8. **Foreground service + WorkManager integration** — session lifecycle, SAF
   tree selection, notification. ~400 LoC.

**Phase 1 total: ~5 KLOC Kotlin** (vs ~5.8 KLOC estimated in R-6, slightly less
because we're scoping down).

### G.2 MUST drop (incompatible with Android)

| Component | Reason |
|---|---|
| `bash` tool (full shell) | No shell on Android; replace with typed file ops |
| `lsp` tool | No LSP server; not needed for our use case |
| `interactive_terminal` tool | No PTY |
| `repo_clone` tool | Skip for v1 (JGit possible later) |
| `background_process` tool | Use WorkManager instead |
| `notebook` / `notebook_host` | Jupyter concept; drop |
| `agent_manager` (VSCode) | VSCode-specific; replace with WorkspaceManager |
| `kilo serve` daemon | Embed runtime instead |
| `kilo-gateway` cloud proxy | Cloud dependency; skip |
| `kilo-telemetry` (PostHog) | Drop or replace with Firebase Crashlytics |
| `kilo-indexing` (embeddings) | Optional v2 feature |
| `kilo-memory` (vector store) | Optional v2 feature |
| `tui` (OpenTUI) | Use Compose instead |
| `kilo-vscode` / `kilo-jetbrains` / `kilo-web-ui` / `kilo-console` / `session-ui` / `extensions` | IDE/web specific |
| MCP stdio transport | MCP HTTP/SSE still possible; stdio needs child process |
| `pty` (`packages/core/src/pty/`) | No PTY on Android |
| `models.dev` catalog fetch | Hardcode a small model list + user-config custom models |
| Bun-specific APIs (`Bun.file`, `Bun.Glob`, `Bun.spawn`) | Replace with `java.nio.file` + Kotlin coroutines |

### G.3 MUST reimplement

| Component | Reason |
|---|---|
| Effect-TS runtime | Kotlin coroutines + structured concurrency |
| Effect `Layer`/`Context.Service` DI | Hilt or Koin |
| Drizzle ORM migrations | Room migrations |
| `Bun.Glob` | `java.nio.file.PathMatcher` + recursive walk |
| ripgrep subprocess | `java.util.regex.Pattern` + recursive walk + bounded matches |
| `EventV2` event bus | `SharedFlow<SessionEvent>` (Kotlin) or LiveData |
| `gray-matter` frontmatter parser | SnakeYAML + custom frontmatter splitter |
| `xstate` (UI state machines) | Kotlin sealed classes + `when` + Compose state |
| SolidJS UI (kilo-ui) | Jetpack Compose |
| `kilo serve` HTTP+SSE | In-process `:core:agent` module directly observable by `:ui` |
| `@kilocode/sdk` (generated TS client) | Direct Kotlin module dependency |
| File watcher (`packages/core/src/filesystem/watcher.ts`) | `android.os.FileObserver` |

### G.4 Effort estimate (per component)

| Component | LoC (Kotlin) | Effort |
|---|---|---|
| LLM client + OpenAI-compatible adapter + streaming | 800 | M |
| Anthropic + Gemini adapters | 400 | S |
| Agent loop + turn runner + compaction | 1000 | L |
| Permission system + glob + saved rules | 400 | S |
| Tools (12 ported × ~100 LoC avg) | 1200 | M |
| Custom agents (Markdown+YAML loader) | 250 | S |
| Plan files | 150 | S |
| Subagent `task` tool + permission derivation | 350 | M |
| Room schema + DAOs + migrations | 600 | M |
| Workspace manager (multi-folder) | 300 | S |
| Foreground service + WorkManager + lifecycle | 500 | M |
| Compose UI (chat, tools, perms, settings, agents) | 1500 | L |
| SAF integration + scoped-storage adapter | 250 | S |
| Keystore integration (API key storage) | 100 | S |
| Telemetry / logging / crash reporting | 100 | S |
| Tests (unit + integration) | 1500 | L |
| **Total** | **~8 KLOC** | **XL (8-12 weeks, 1 dev)** |

### G.5 Key risks for Android

1. **Effect-TS → Kotlin translation** — Kilo's architecture is deeply Effect-idiomatic
   (Layers, Context.Service, FiberSet, Stream, uninterruptibleMask, Effect.die for
   control flow). Kotlin coroutines don't have direct equivalents for
   `uninterruptibleMask`, `Effect.scoped`, or `Effect.die(transition)` as control
   flow. Translation requires judgment — not mechanical.

2. **SQLite vs Effect's transaction semantics** — Kilo uses Drizzle transactions
   for atomicity. Room has `@Transaction` and `withTransaction {}`. Close enough
   but watch for subtle ordering bugs.

3. **Bun-specific APIs scattered everywhere** — `Bun.file`, `Bun.Glob`,
   `Bun.spawn`, `Bun.password` are sprinkled across the codebase. Every
   transplanted module needs audit.

4. **On-device LLM** — if we want true offline (the killer feature for Android),
   we need llama.cpp via JNI or MLC-LLM. Kilo has no precedent for on-device
   inference — its `openai-compatible` provider points at remote URLs only.

5. **SAF performance** — `DocumentFile` is slow for large trees. May need to
   copy SAF tree to app-internal storage on workspace open (with user consent)
   and sync back. Kilo's `FSUtil.contains` doesn't have this concern.

6. **Background execution limits** — Android 14+ aggressively kills background
   work. Long agent sessions need a foreground service (notification required).
   WorkManager is for short deferrable tasks only. Sessions >10 min need FGS.

7. **Compaction cost** — LLM-based compaction requires a model call. If the user
   only has on-device models, compaction will be slow. Need a "compaction model"
   picker (Kilo supports this — port the pattern).

8. **Plan file SAF access** — Kilo writes plan files inside the workspace; with
   SAF we need write permission to the workspace tree (already granted when user
   selects it). Good.

9. **Tool output truncation** — Kilo spills to `${Global.Path.tmp}/truncate/*` and
   returns a path. On Android, spill to app cache dir; clean up on session end.

10. **Provider OAuth flows** — Kilo spawns a localhost HTTP server for OAuth
    callbacks (`packages/core/src/plugin/provider/openai.ts`). On Android, use
    `androidx.browser:browser` Custom Tabs + App Link deep link instead.

---

## H. Feature highlights to adopt

### H.1 BEST features to adopt (must-have)

1. **LLM-based auto-compaction with anchored summary template** — the
   `## Objective / Important Details / Work State / Next Move / Relevant Files`
   structure is gold. Update-don't-replace preserves facts. The `keep.tokens=8_000`,
   `buffer=20_000`, `summaryOutputTokens=4_096` defaults are sensible starting
   points. **~400 LoC Kotlin.**

2. **Per-agent permissions with glob patterns** — `Rule{action, resource, effect}`
   + `Wildcard.match` + `findLast-wins` semantics + saved-rule persistence. This
   is THE permission model. **~400 LoC Kotlin.**

3. **Single `task` tool with subagent_type + background + task_id** — simpler
   than Cline's `new_task`/`use_subagents` split. The `deriveSubagentSessionPermission`
   logic (parent denies + external_directory inherit, bash denies don't
   propagate) is well-reasoned. **~350 LoC Kotlin.**

4. **Custom agents as Markdown+YAML frontmatter** — `agents/<name>.md` with
   frontmatter for permission/model/steps and body for system prompt. Users can
   share agents as files. **~250 LoC Kotlin.**

5. **Persistent plan files** — `.app/plans/*.md` survive sessions, can be
   version-controlled, can be opened in any text editor. PlanExit tool resolves
   the actual file by exact-path → generated-name sibling → last write.
   **~150 LoC Kotlin.**

6. **Anchored-summary anchored to previous summary** — the compaction prompt
   includes the previous summary as `<previous-summary>` so the model can
   update rather than rewrite. Critical for long sessions.

7. **Stale-content detection on edit** — `writeIfUnchanged({expected, content})`
   fails if the file changed since read. Eliminates a class of bugs.

8. **Iterative agent loop with turn-transition control flow** — `Effect.die(transition)`
   as a goto-like mechanism for compaction recovery. Maps to Kotlin sealed
   `TurnTransition` + `when` resume.

9. **Doom-loop protection** — `doom_loop: "ask"` default permission prompts the
   user when the agent appears stuck repeating itself.

10. **System-prompt variable substitution** — `{env:VAR}` and `{file:path}` in
    agent prompts (trusted scopes only). Useful for injecting project-specific
    context.

### H.2 Kilo Code weaknesses

1. **No embeddable SDK** — the runtime is a `kilo serve` HTTP daemon. To embed,
   you'd spawn the process and speak HTTP. Not friendly for Android in-process
   use. **Cline wins here** with its layered `@cline/agents` SDK.

2. **Effect-TS everywhere** — steep learning curve. Translating Effect idioms
   to Kotlin requires judgment. `Layer`/`Context.Service` DI doesn't map cleanly
   to Hilt.

3. **Bun-specific APIs scattered** — `Bun.file`, `Bun.Glob`, `Bun.spawn` in
   hot paths. Every transplant needs audit.

4. **Heavy** — full Kilo Code is ~500 KLOC across all packages. Even
   `packages/core/` + `packages/opencode/` is ~200 KLOC. We're porting ~5% of it.

5. **No on-device inference story** — Kilo assumes cloud LLMs. On-device LLM
   (llama.cpp/MLC-LLM) needs original work.

6. **Compaction is V1/V2 split** — two parallel implementations in
   `packages/core/src/session/compaction.ts` (V2, ~240 LoC, simple) and
   `packages/opencode/src/session/compaction.ts` (V1, ~720 LoC, sophisticated
   with prune/chunks/recovery). Confusing. Pick V2 + cherry-pick V1 features.

7. **`bash` tool is fundamental to many workflows** — its `readOnlyBash`
   allowlist is a clever defense-in-depth pattern, but it depends on a real
   shell. Porting it requires either dropping shell entirely or building a
   fake shell that maps commands to typed file ops (lots of edge cases).

8. **Cloud-coupled features** — Kilo Gateway, Kilo Sessions (remote sync), PR
   linking, Anaconda Desktop, Agent Manager — all assume a desktop/cloud
   workflow. None map to Android.

9. **License carries dual attribution** — `Copyright (c) 2026 Kilo Code` AND
   `Copyright (c) 2025 opencode`. We must preserve both in any ported code.

10. **Two tool APIs (V1/V2)** — migration is incomplete. Documentation and
    tests span both. We pick V2 and ignore V1.

### H.3 Kilo Code features Cline lacks

| Feature | Kilo | Cline |
|---|---|---|
| LLM-based auto-compaction with anchored summary | ✅ | ❌ (quarter-truncation) |
| Per-agent permissions with glob patterns | ✅ | ❌ (per-tool booleans) |
| Single `task` tool with background mode | ✅ | ❌ (`new_task`+`use_subagents`) |
| Custom agents as Markdown+YAML | ✅ | ❌ (JSON only) |
| Persistent plan files on disk | ✅ | ❌ |
| SQLite persistence + event-sourced sessions | ✅ | ❌ (JSON files) |
| `kilo serve` daemon for multi-client | ✅ | ❌ (embedded only) |
| Stale-content detection on edit | ✅ | ❌ |
| Doom-loop permission ask | ✅ | ❌ |
| System-prompt variable substitution | ✅ | ❌ |
| Dedicated compaction model per-agent | ✅ | ❌ |
| Tail-turn preservation in compaction | ✅ | ❌ |
| Tool-output pruning (post-compaction) | ✅ | ❌ |
| AI-SDK unified provider interface | ✅ | ❌ (manual adapters) |
| models.dev catalog integration | ✅ | ❌ |
| Snapshot-based file change tracking | ✅ | Partial |
| Plan Mode as first-class agent | ✅ | Partial |
| `explore` / `debug` / `orchestrator` / `ask` / `scout` built-in agents | ✅ | Partial |
| Glob-based bash allowlist (`readOnlyBash`) | ✅ | ❌ |
| MCP per-server permission sanitization | ✅ | Partial |
| Reference agents (clone external repo for read-only research) | ✅ | ❌ |

---

## I. Custom model support

### I.1 Custom model configuration

Three places:

1. **`provider` config block** (`opencode.json` / `kilo.json` / `~/.config/kilo/config.json`):

   ```json
   {
     "provider": {
       "my-local": {
         "name": "Local Ollama",
         "api": {
           "type": "aisdk",
           "package": "@ai-sdk/openai-compatible",
           "url": "http://localhost:11434/v1"
         },
         "models": {
           "llama3": { "name": "Llama 3 8B", "limit": { "context": 8192 } }
         }
       }
     }
   }
   ```

2. **Per-agent model override**:

   ```json
   { "agent": { "compaction": { "model": "my-local/llama3" } } }
   ```

3. **Runtime model selection** — the `task` tool stores a model choice per
   subagent session in `KiloSession.ModelState` (zod-validated), so the user
   can switch mid-conversation.

### I.2 OpenAI-compatible endpoints — first-class

`packages/core/src/plugin/provider/openai-compatible.ts` — wraps
`@ai-sdk/openai-compatible`'s `createOpenAICompatible()`:

```ts
if (evt.package.includes("@ai-sdk/openai-compatible")) {
  if (evt.options.includeUsage !== false) evt.options.includeUsage = true
  evt.sdk = (await import("@ai-sdk/openai-compatible")).createOpenAICompatible(evt.options)
}
```

This covers: OpenAI, OpenRouter, Together AI, Groq, DeepInfra, Fireworks, Anyscale,
Lemonade, LiteLLM, Ollama, LM Studio, vLLM, llama.cpp server, any
OpenAI-compatible endpoint.

### I.3 Model-specific parameters

- **`Model.api`** schema (`packages/core/src/config/provider.ts`): each model can
  specify its API config (`AISDK` or `Native`) plus per-model `cost`, `limit`,
  `capabilities`, `request` (headers/body), and `variants` (multiple API
  configs for one model — e.g. Anthropic has separate Messages vs Count-tokens
  endpoints).
- **`providerOptions`** in `LLMRequest` — free-form per-provider options bag
  (e.g. `{openai: {promptCacheKey: "..."}}`). Each provider plugin can read and
  shape this in `ProviderTransform`.
- **`AgentVariant`** — one model can have multiple variants (e.g.
  `claude-sonnet-4.5` with `thinking: high` vs `thinking: low`) selectable
  via `variant` field.
- **`ProviderTransform`** (`packages/opencode/src/provider/transform.ts`) —
  plugin hook to mutate the request per provider (e.g. DeepSeek auto-detection,
  Gemini image conversion, Copilot usage tracking).

For Android: expose a `ModelConfig` data class with `id`, `providerId`, `name`,
`apiBaseUrl`, `apiKeyRef` (Keystore alias), `contextLimit`, `outputLimit`,
`costPerInputToken`, `costPerOutputToken`, `capabilities`, `extraOptions:
Map<String, Any>`. Stored in Room. Per-agent override stored on the Agent
entity. ~300 LoC.

---

## J. Combination recommendation

### J.1 Backbone choice: Cline structural, Kilo pattern-rich

Reaffirming R-6's recommendation with deeper source-level evidence:

**Use Cline as the structural backbone** because:

- Cline has a **clean layered SDK** (`@cline/agents`) with browser-compatible
  TypeScript that compiles under Hermes. The `:core:agent:{shared, llm, core, tools}`
  module structure maps directly to Kotlin modules.
- Cline's `createTool` API is one API, not two. Cleaner to port.
- Cline's recursive `recursivelyMakeClineRequests` is simpler than Kilo's
  Effect-TS iterative loop with `Effect.die` transitions — but **we should port
  Kilo's iterative loop pattern** because it has better compaction integration.
- Cline's checkpoint store (JSON) is simpler than Kilo's Drizzle+SQLite, but
  **we should use Kilo's SQLite pattern** because Room is Android-native.

**Use Kilo Code for these advanced patterns** (~600 LoC of Kotlin):

1. Auto-compaction with anchored summary + previous-summary update
   (`packages/core/src/session/compaction.ts` V2, ~400 LoC Kotlin).
2. Per-agent permission rulesets with glob patterns + saved rules
   (`packages/core/src/permission.ts` + `packages/core/src/util/wildcard.ts`,
   ~400 LoC Kotlin).
3. Single `task` tool + `deriveSubagentSessionPermission`
   (`packages/opencode/src/tool/task.ts` + `subagent-permissions.ts`, ~350 LoC Kotlin).
4. Custom agents as Markdown+YAML frontmatter
   (`packages/opencode/src/config/agent.ts` + `markdown.ts`, ~250 LoC Kotlin).
5. Persistent plan files
   (`packages/opencode/src/kilocode/plan-file.ts`, ~150 LoC Kotlin).
6. Stale-content detection on edit (in Kilo's `edit.ts`, ~50 LoC Kotlin).
7. Doom-loop permission ask (one permission rule default, ~10 LoC).

### J.2 Gap that NEITHER repo fills for Android

| Gap | Cline | Kilo | Android-required original work |
|---|---|---|---|
| **On-device LLM streaming** | ❌ | ❌ | llama.cpp JNI or MLC-LLM; expose as `LocalOnDeviceProvider` implementing `LLMProvider`. ~600 LoC Kotlin + NDK build. |
| **Compose agent UI** | SolidJS webview | SolidJS / OpenTUI | Compose chat, tool cards, permission dialog, settings, agent picker. ~1500 LoC. |
| **Android lifecycle integration** | ❌ | ❌ | Foreground service (active session), WorkManager (background subagents), ProcessLifecycleOwner, doze-mode handling. ~500 LoC. |
| **SAF / scoped storage adapter** | ❌ | ❌ | `DocumentFile`-based `FileSystem.Service` impl; `ContentResolver` for media; user-picked tree URI persisted in `SharedPrefs`. ~300 LoC. |
| **Keystore integration** | env vars | `auth.json` 0600 | `AndroidKeystore` for API keys; biometric-protected unlock. ~150 LoC. |
| **Multi-workspace manager** | (single workspace) | `InstanceStore` (directory-keyed) | `WorkspaceManager` with SAF tree URIs; switch active workspace; per-workspace session list. ~300 LoC. |
| **Notification + permission UX** | VSCode webview | VSCode/TUI/JetBrains | Compose dialogs for `ask` permissions, `question` tool, background-task notifications. ~400 LoC. |
| **OAuth flows for providers** | ❌ | localhost HTTP server | Custom Tabs + App Links deep link for OpenAI/Copilot OAuth. ~200 LoC. |
| **WorkManager background subagents** | ❌ | `BackgroundJob` Service | One-shot WorkRequest per `task(background: true)`; tag with task_id; observe via `WorkInfo` Flow. ~200 LoC. |
| **Crash reporting + telemetry** | Sentry | PostHog | Firebase Crashlytics + custom events. ~100 LoC. |
| **App settings + onboarding** | VSCode settings JSON | `~/.config/kilo/config.json` | Compose settings screens + onboarding wizard for first-run model config. ~500 LoC. |
| **APK packaging + signing** | n/a | n/a | Gradle + R8 + APK signing; Play Store readiness. ~50 LoC build scripts. |
| **Foreground service notification** | n/a | n/a | Active-session FGS with pause/cancel actions. ~150 LoC. |

**Total gap-filling original work: ~5 KLOC Kotlin.**

### J.3 Final architecture (recommended)

```
:app                            (Android application module)
  ├─ :ui:compose                 (Compose UI, MVVM, ~1500 LoC)
  ├─ :core:agent                 (Kotlin agent runtime)
  │   ├─ :core:agent:shared      (types, schemas, ~500 LoC)
  │   ├─ :core:agent:llm         (LLMProvider interface + adapters, ~1200 LoC)
  │   │   ├─ openai-compatible   (~400 LoC)
  │   │   ├─ anthropic           (~250 LoC)
  │   │   ├─ gemini              (~250 LoC)
  │   │   └─ local-on-device     (llama.cpp JNI, ~600 LoC)
  │   ├─ :core:agent:core        (loop, compaction, permissions, ~1800 LoC)
  │   │   ├─ AgentLoop           (iterative, Kilo pattern)
  │   │   ├─ Compaction          (Kilo V2 + V1 prune)
  │   │   ├─ Permissions         (Kilo glob rulesets)
  │   │   └─ Subagents           (Kilo task tool)
  │   ├─ :core:agent:tools       (~1200 LoC, 12 tools)
  │   └─ :core:agent:persistence (Room, ~600 LoC)
  ├─ :core:android               (~1500 LoC)
  │   ├─ WorkspaceManager        (SAF trees)
  │   ├─ SessionService          (foreground service + WorkManager)
  │   ├─ KeystoreProvider       (API key storage)
  │   ├─ NotificationProvider
  │   └─ CrashlyticsProvider
  └─ :data:models                (Room entities + DAOs)
```

**Estimated grand total**: ~8 KLOC Kotlin (vs ~5.8 KLOC estimated in R-6, the
increase reflects deeper source-level analysis surfacing more required
original work — particularly on-device LLM, Compose UI, and SAF adapter).

---

## Appendix A — Verified claims (source-level)

| # | Claim | Source file:line | Verification |
|---|---|---|---|
| 1 | MIT license, dual attribution | `LICENSE:1-22` | Read directly |
| 2 | Fork of OpenCode | `AGENTS.md:155` "Kilo CLI is a fork of opencode" | Read directly |
| 3 | Effect-TS based | `package.json` deps `effect@4.0.0-beta.83`, `@effect/platform-node` | Read directly |
| 4 | Bun runtime | `package.json` `packageManager: bun@1.3.14`, `bun.lock` | Read directly |
| 5 | Iterative agent loop | `packages/core/src/session/runner/llm.ts:384-407` | Read directly |
| 6 | LLM-based compaction V2 | `packages/core/src/session/compaction.ts:170-242` | Read directly |
| 7 | Anchored summary template | `packages/core/src/session/compaction.ts:16-46` | Read directly |
| 8 | Default compaction buffer=20k, keep=8k | `packages/core/src/session/compaction.ts:12-13` | Read directly |
| 9 | Permission ruleset with glob | `packages/core/src/permission.ts:76-90` | Read directly |
| 10 | Built-in defaults (doom_loop, env asks) | `packages/opencode/src/agent/agent.ts:151-174` | Read directly |
| 11 | Per-agent model override | `packages/opencode/src/agent/agent.ts:61-66` | Read directly |
| 12 | Compaction agent has `*:deny` permission | `packages/opencode/src/agent/agent.ts:298-312` | Read directly |
| 13 | `task` tool with background + task_id | `packages/opencode/src/tool/task.ts:65-72, 111-114` | Read directly |
| 14 | `deriveSubagentSessionPermission` logic | `packages/opencode/src/agent/subagent-permissions.ts:14-27` | Read directly |
| 15 | `KiloTask.inherited` bash-deny exclusion | `packages/opencode/src/kilocode/tool/task.ts:60-95` | Read directly |
| 16 | Custom agents from `{agent,agents}/**/*.md` | `packages/opencode/src/config/agent.ts:31` | Read directly |
| 17 | gray-matter frontmatter parse | `packages/core/src/config/markdown.ts:4-9` | Read directly |
| 18 | Plan files at `.kilo/plans/*.md` etc. | `packages/opencode/src/kilocode/agent/index.ts:209-218` | Read directly |
| 19 | `PlanFile.locate` resolution priority | `packages/opencode/src/kilocode/plan-file.ts:77-85` | Read directly |
| 20 | `edit` tool stale-content detection | `packages/core/src/tool/edit.ts:212-217` (`writeIfUnchanged`) | Read directly |
| 21 | `bash` tool with readOnlyBash allowlist | `packages/opencode/src/kilocode/agent/index.ts:51-117` | Read directly |
| 22 | `kilo serve` daemon | `packages/opencode/src/cli/cmd/serve.ts:6-56` | Read directly |
| 23 | InstanceStore directory-keyed cache | `packages/opencode/src/project/instance-store.ts:33-43` | Read directly |
| 24 | 30+ built-in providers | `ls packages/core/src/plugin/provider/` | Listed directly |
| 25 | OpenAI-compatible provider | `packages/core/src/plugin/provider/openai-compatible.ts:1-17` | Read directly |
| 26 | Dynamic NPM provider plugin | `packages/core/src/plugin/provider/dynamic.ts:1-32` | Read directly |
| 27 | models.dev catalog | `packages/core/src/models-dev.ts:171` (KILO_MODELS_URL fallback) | Read directly |
| 28 | AI-SDK as provider abstraction | `package.json` deps `ai@6.0.235` | Read directly |
| 29 | `LLMClient.stream` Effect Stream | `packages/llm/src/llm.ts:46-47` | Read directly |
| 30 | `TaskTool` with subagent_type | `packages/opencode/src/tool/task.ts:94-105` | Read directly |
| 31 | `task` tool background mode requires flag | `packages/opencode/src/tool/task.ts:111-114` | Read directly |
| 32 | V2 vs V1 tool API split | `packages/core/src/tool/tool.ts` vs `packages/opencode/src/tool/tool.ts` | Read directly |
| 33 | `ToolRegistry.materialize(permissions)` filters | `packages/core/src/tool/registry.ts:106-122` | Read directly |
| 34 | FileSystem TOCTOU protection | `packages/core/src/filesystem.ts:84-110` | Read directly |
| 35 | Plan file system-prompt variable substitution | `packages/opencode/src/config/agent.ts:60-85` | Read directly |
| 36 | `kilocode_change` markers in shared code | `AGENTS.md:22, 187, 202-207` | Read directly |
| 37 | `gray-matter` for frontmatter | `packages/core/src/config/markdown.ts:3` | Read directly |
| 38 | Drizzle ORM migrations | `packages/core/src/database/migration/*.ts` (50+ files) | Listed directly |
| 39 | Compaction prompt template (anchored) | `packages/core/src/session/compaction.ts:16-46` | Read directly |
| 40 | `search` SemanticSearch exists | `packages/opencode/src/kilocode/tool/semantic-search.ts` | Listed |
| 41 | `doom_loop: "ask"` default | `packages/opencode/src/agent/agent.ts:153` | Read directly |
| 42 | `external_directory` permission | `packages/opencode/src/agent/agent.ts:154-157` | Read directly |
| 43 | `KiloAgent.patchAgents` renames build→code, adds debug/orchestrator/ask | `packages/opencode/src/kilocode/agent/index.ts:400-564` | Read directly |
| 44 | Agent Manager is VSCode-only | `AGENTS.md` Products table | Read directly |
| 45 | OAuth callback via localhost HTTP server | `packages/core/src/plugin/provider/openai.ts:51-60` | Read directly |
| 46 | `KiloSession.ModelState` zod schema | `packages/opencode/src/kilocode/tool/task.ts:13-22` | Read directly |
| 47 | `KiloCompactionChunks` multi-chunk summarization | `packages/opencode/src/session/compaction.ts:436-470` | Read directly |
| 48 | `prune` tool-output compaction | `packages/opencode/src/session/compaction.ts:269-318` | Read directly |
| 49 | `preserveRecentBudget` 2k-8k tokens | `packages/opencode/src/session/compaction.ts:97-103` | Read directly |
| 50 | Snapshot-based file change tracking | `packages/core/src/session/runner/llm.ts:319-325` | Read directly |

---

## Appendix B — Source files read in detail

| File | Lines | Purpose |
|---|---|---|
| `LICENSE` | 23 | MIT, dual attribution |
| `package.json` | 80+ | Workspace config, Bun, deps |
| `AGENTS.md` | ~250 (head) | Repo conventions, fork lineage, build/test |
| `packages/core/src/tool/tool.ts` | 163 | V2 `Tool.make` API |
| `packages/core/src/tool/registry.ts` | 148 | V2 `ToolRegistry.materialize` |
| `packages/core/src/tool/builtins.ts` | 49 | V2 built-in tool list |
| `packages/core/src/tool/application-tools.ts` | 58 | Global tool registry |
| `packages/core/src/tool/bash.ts` | 208 | V2 bash tool |
| `packages/core/src/tool/edit.ts` | 244 | V2 edit tool (stale-content detection) |
| `packages/core/src/session/compaction.ts` | 242 | V2 LLM-based auto-compaction |
| `packages/opencode/src/session/compaction.ts` | 722 | V1 compaction with prune/chunks/recovery |
| `packages/opencode/src/agent/agent.ts` | 660 | Agent definitions, defaults, model overrides |
| `packages/opencode/src/agent/subagent-permissions.ts` | 28 | Subagent permission derivation |
| `packages/opencode/src/kilocode/agent/index.ts` | 688 | Kilo agent patches (debug/orchestrator/ask, plan/explore hardening) |
| `packages/opencode/src/tool/task.ts` | 475 (head) | V1 task tool with background mode |
| `packages/opencode/src/kilocode/tool/task.ts` | 120 (head) | Kilo task tool extensions |
| `packages/opencode/src/tool/plan.ts` | 2 | Plan tool re-export |
| `packages/opencode/src/kilocode/tool/plan.ts` | 60 | PlanExit tool |
| `packages/opencode/src/kilocode/plan-file.ts` | 94 | Plan file resolution |
| `packages/opencode/src/config/agent.ts` | 161 | Markdown agent loader |
| `packages/core/src/config/markdown.ts` | 37 | gray-matter frontmatter parser |
| `packages/core/src/config/agent.ts` | 25 | Agent config schema |
| `packages/core/src/config/provider.ts` | 72 | Provider config schema |
| `packages/core/src/config/compaction.ts` | 16 | Compaction config schema |
| `packages/core/src/permission.ts` | 200 (head) | V2 permission service |
| `packages/core/src/session/runner/llm.ts` | 434 | V2 agent loop |
| `packages/core/src/session/runner/index.ts` | 29 | SessionRunner interface |
| `packages/core/src/session/todo.ts` | 79 | Todo persistence |
| `packages/core/src/filesystem.ts` | 148 | V2 filesystem service (TOCTOU-protected) |
| `packages/core/src/global.ts` | 104 | XDG paths, app="kilo" |
| `packages/opencode/src/cli/cmd/serve.ts` | 57 | kilo serve daemon command |
| `packages/opencode/src/project/instance-store.ts` | 80 (head) | InstanceStore (directory-keyed cache) |
| `packages/opencode/src/server/server.ts` | 80 (head) | HTTP server |
| `packages/core/src/plugin/provider/openai-compatible.ts` | 17 | OpenAI-compatible provider plugin |
| `packages/core/src/plugin/provider/dynamic.ts` | 32 | Dynamic NPM provider plugin |
| `packages/core/src/models-dev.ts` | 50 (head) | models.dev catalog integration |
| `packages/schema/src/provider.ts` | 73 | Provider/Api schema (AISDK | Native) |
| `packages/llm/src/llm.ts` | 187 (head) | LLM client interface |
| `packages/opencode/src/session/llm.ts` | 100 (head) | LLM streaming entry |
| `packages/sdk/js/src/client.ts` | 67 (head) | SDK client (x-kilo-directory routing) |
| `packages/core/src/v1/permission.ts` | 34 | V1 permission errors |
| `packages/opencode/src/tool/tool.ts` | 190 | V1 Tool.define API |

---

## Appendix C — Quick-reference for the Android team

### C.1 Port priorities (do in this order)

1. **Permission system** (foundational, used everywhere) — 400 LoC, 2 days.
2. **LLM provider interface + OpenAI-compatible adapter + streaming** — 800 LoC, 4 days.
3. **Room schema + migrations** — 600 LoC, 3 days.
4. **Agent loop + turn runner** — 600 LoC, 4 days.
5. **5 core tools (read/write/edit/glob/grep)** — 600 LoC, 3 days.
6. **Compaction (V2 only first)** — 400 LoC, 3 days.
7. **`todowrite` + `question` tools + Compose permission dialog** — 400 LoC, 2 days.
8. **Custom agents Markdown loader** — 250 LoC, 2 days.
9. **`task` tool + subagent permissions** — 350 LoC, 3 days.
10. **Plan files** — 150 LoC, 1 day.
11. **Foreground service + WorkManager** — 500 LoC, 3 days.
12. **Compose UI (chat, settings, agent picker)** — 1500 LoC, 7 days.
13. **SAF adapter + WorkspaceManager** — 300 LoC, 2 days.
14. **Keystore + onboarding** — 250 LoC, 2 days.
15. **Anthropic + Gemini adapters** — 500 LoC, 3 days.
16. **Tests + integration** — 1500 LoC, 7 days.

**Total: ~8.5 KLOC, ~14 weeks with 1 dev; ~7 weeks with 2 devs.**

### C.2 What to read FIRST when porting

- `packages/core/src/session/runner/llm.ts` (the agent loop — read first, ~434 LoC)
- `packages/core/src/session/compaction.ts` (V2 compaction, ~240 LoC)
- `packages/core/src/permission.ts` (permission system, ~311 LoC)
- `packages/opencode/src/agent/agent.ts` (agent definitions + defaults, ~660 LoC)
- `packages/opencode/src/agent/subagent-permissions.ts` (subagent derivation, ~28 LoC)
- `packages/core/src/tool/edit.ts` (edit tool pattern, ~244 LoC)
- `packages/opencode/src/kilocode/plan-file.ts` (plan resolution, ~94 LoC)
- `packages/opencode/src/config/agent.ts` (markdown agent loader, ~161 LoC)

### C.3 Top 3 patterns to adopt verbatim

1. **Anchored-summary compaction prompt** (`packages/core/src/session/compaction.ts:16-46`).
2. **Permission ruleset with `findLast` + `Wildcard.match`** (`packages/core/src/permission.ts:76-90`).
3. **`deriveSubagentSessionPermission`** (`packages/opencode/src/agent/subagent-permissions.ts:14-27`).

---

**Report complete.** Author: R-A2 sub-agent. Cross-reference: R-A1 (Cline analysis),
R-6 (Kilo Code overview), `worklog.md` (project coordination).
