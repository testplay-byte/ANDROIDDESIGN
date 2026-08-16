# Knowledge: Cline Agent (Porting Notes)

> Quick-reference. Full research: `research/R-1-cline-agent.md` (64KB, 1310 lines).

## What Cline is
- Open-source AI coding agent, originally a VSCode extension. Repo: https://github.com/cline/cline
- **License: Apache 2.0** — port ideas/architecture freely; ship a NOTICES screen. Do NOT translate source verbatim; write idiomatic Kotlin.
- TypeScript/Node. Clean layered SDK: `@cline/shared` → `@cline/llms` → `@cline/agents` → `@cline/core`. `@cline/agents` is browser-compatible (proof the agent loop needs no Node primitives).

## What to KEEP (port to Kotlin)
- **ReAct agent loop** — but iterative `while(isActive)` Kotlin coroutine, NOT recursive like Cline's `recursivelyMakeClineRequests`.
- **Tool system** — `createTool({name, description, inputSchema(JSON Schema), execute})` maps 1:1 to a Kotlin `interface Tool { val name, description, inputSchema; suspend fun execute(input): ToolResult }`.
- **Context management** — quarter-truncation strategy (~25% oldest undeletable messages dropped when approaching limit; preserve initial exchange + recent tool results; file-read dedup with `[DUPLICATE FILE READ]` notices). ~50 lines of Kotlin.
- **LLM provider abstraction** — `ApiHandler` interface (`createMessage→ApiStream`, `getModel`, `abort`). Ship 4 providers (Anthropic, OpenAI, OpenRouter, Gemini) + 1 OpenAI-compatible generic. SSE streaming.
- **Approval flow** — `toolPolicies: Map<tool, {autoApprove, enabled}>` + `suspend approve(toolName, input)` callback. Kotlin coroutines give us direct suspension — no polling.
- **Tool call parsing** — handle both native function-calling + XML-style tool use (Claude's XML format). Keep Cline's parser logic.

## What to DROP (non-portable to Android)
- Puppeteer browser automation.
- `bash` / `execute_command` — no terminal on Android (and we don't want one; the agent edits design tokens, not code).
- stdio MCP transport — impossible on Android (no subprocess). HTTP MCP servers are portable but defer to post-MVP.
- Shadow-git checkpoint system — unviable on Android (sandboxed FS). Use Room-backed `design_snapshots` table capped at 50.
- VS Code host integration — we have our own Compose UI.
- Hooks shell scripts — replace with Kotlin callbacks.
- tree-sitter code parsing — not needed (agent edits JSON tokens, not source code).

## What to REIMPLEMENT
- LLM HTTP client (OkHttp + SSE) per provider.
- Tool dispatch (Kotlin coroutines).
- Context truncation (~50 lines).
- Checkpoint store (Room table, not shadow git).
- Approval gateway (Kotlin suspend callback, not 100ms polling).

## Diff-based editing
- Cline legacy: `<<<<<<< SEARCH / ======= / >>>>>>> REPLACE` blocks.
- Cline modern: `apply_patch` (Codex-style `*** Begin Patch ***` with `@@` anchors).
- **Our choice**: **JSON-Patch (RFC 6902)** as primary for design-token JSON (deterministic, schema-aware, atomic). SEARCH/REPLACE as fallback for free-form text (rules, notes).

## Proposed Kotlin module shape
```
:core:agent:shared    — pure Kotlin, JVM-targetable (types, schemas)
:core:agent:llm       — 4 providers via OkHttp + SSE
:core:agent:core      — AgentRuntime (while isActive), ContextManager, ToolExecutor, Room-backed CheckpointStore, ApprovalGateway, PromptRegistry
:core:agent:tools     — ReadDesignTokens, ApplyTokenPatch, ApplyTextPatch, SetSortingRule, SetThemeVariant, PreviewState, AskUser, AttemptCompletion, NewTask
:app                  — Compose UI, AniList integration, theme engine, Android Keystore
```

## Tool surface for our design-system agent (the ONLY tools)
`read_design_tokens`, `apply_token_patch` (JSON-Patch), `apply_text_patch` (SEARCH/REPLACE), `set_color_role`, `set_typography`, `set_shape`, `set_motion`, `apply_image_palette`, `swap_layout`, `set_component_variant`, `set_sorting_rule`, `preview`, `commit`, `rollback`, `ask_user`, `attempt_completion`.

## Key risks
- On-device LLM latency (cloud API). Mitigate with streaming + preview-while-thinking.
- Context window management for long design sessions. Mitigate with quarter-truncation + snapshot summaries.
- User trust in AI edits. Mitigate with preview StateFlow + snapshot rollback + approval gateway.
