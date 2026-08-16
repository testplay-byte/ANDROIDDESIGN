# Screen Plan — Agent Tech Android App

> Detailed specs for every screen. Informed by R-A5 (Primary Glass design) + R-A6 (SAF + virtual commands) + R-A7 (codebase search).

---

## App structure

### Bottom navigation (4 tabs)
1. **Chat** — agent conversation (default screen when a project is open)
2. **Files** — workspace file browser
3. **Runs** — task/run history
4. **Settings** — LLM providers, API keys, usage/limits, auto-approve

### Navigation flow
```
App Launch
  ↓
[No projects?] → Onboarding (welcome + "Add Project" CTA)
  ↓
[Has projects?] → Project Selector (list of project folders)
  ↓
[Select project] → Chat screen (default tab) with that project active
  ↓
[Bottom nav] → Chat / Files / Runs / Settings
```

---

## Screen 1: Onboarding (first launch)
- Full-screen welcome with the app logo (red/yellow/blue glass-inspired).
- "What is Agent Tech?" — 1-line description.
- "Add your first project" CTA button (blue, primary).
- Tapping CTA → opens SAF folder picker (`ACTION_OPEN_DOCUMENT_TREE`).
- After folder selected → creates a Project entity → navigates to Chat.
- Skip if projects already exist.

## Screen 2: Project Selector (Projects)
- List of project cards (folder name + path + last-active timestamp).
- Each card: project name, folder path (truncated), "N messages" count.
- FAB or top-right "+" button → "Add Project" → SAF folder picker.
- Tap a project → opens Chat with that project active.
- Long-press a project → context menu: "Rename", "Delete" (removes from app, does NOT delete the folder).
- Empty state: "No projects yet. Tap + to add your first project folder."
- Top bar: "Projects" title (collapsible header).

## Screen 3: Chat (agent conversation)
- **Top bar**: project name (collapsible) + workspace selector dropdown.
- **Message list** (LazyColumn):
  - User messages: blue bubble (`#1E88E5`), right-aligned, white text, bottom-right tail.
  - Assistant messages: dark grey bubble (`#2E2E2E`), left-aligned, light text, bottom-left tail.
  - Code blocks: `#1F1F1F` inset inside assistant bubble, monospace, syntax-highlighted (future).
  - Tool call cards: light surface body + dark grey header bar (tool name + status dot). 5 states: queued (grey), running (yellow pulse), success (green), error (red), needs-approval (amber).
  - Streaming cursor: thin vertical blue bar (1.5dp × 16sp, opacity 1.0↔0.0 over 800ms).
- **Input bar** (bottom, above nav): text field + send button. Disabled while agent is responding.
- **Stop button** (replaces send button while agent is running): stops the current run.
- When agent is running: ForegroundService notification shows live status.

## Screen 4: Files (workspace file browser)
- **Top bar**: "Files" title (collapsible) + current path breadcrumb.
- **File tree**: `DocumentsContract.buildChildDocumentsUriUsingTree()` walk.
  - Folders: folder icon + name + child count. Tap → navigate into.
  - Files: file icon + name + size + last-modified. Tap → preview (text only for v1).
- **Breadcrumb**: current path (e.g., `src/main/kotlin/`), tap a segment to navigate up.
- **Search bar** (top): filter files by name (instant). Content search via "Search in files" action.
- **Restriction**: cannot navigate above the project root folder (SAF-scoped).
- Long-press a file → context menu: "Open", "Delete", "Properties".

## Screen 5: Runs (task history)
- **Top bar**: "Runs" title (collapsible).
- **Run list** (LazyColumn): each run = a card with:
  - Run title (first user message, truncated).
  - Status badge: queued / running / success / error / stopped.
  - Timestamp (started + duration).
  - Token usage (input + output + total).
  - Estimated cost (if user set pricing).
  - Tap → opens the Chat screen scrolled to that run.
- **Filter**: by status (All / Success / Error / Running).
- Empty state: "No runs yet. Start a conversation in Chat."

## Screen 6: Settings
- **Top bar**: "Settings" title (collapsible).
- **Sections** (GlassCards):
  - **LLM Providers**: list of configured providers (OpenAI, Anthropic, Gemini, Ollama, Custom). Each shows status badge (configured/unconfigured). Tap → provider config screen.
  - **Active Model**: shows the currently active provider + model. Tap → switch.
  - **API Keys**: list of stored keys (provider + key, viewable — NOT encrypted per user request). Tap → edit. "View" button to reveal.
  - **Auto-Approve**: toggle (on by default). "All actions are performed in the dedicated project folder."
  - **Usage & Limits**: tap → opens Usage/Limits screen.
  - **About**: app version, open-source licenses.

## Screen 7: Provider Config (sub-screen of Settings)
- Provider name (e.g., "OpenAI").
- API key input (text field, with "View" toggle to show/hide).
- Base URL input (for custom endpoints — defaults to provider's official URL).
- Model name input (e.g., "gpt-4o", "claude-3-5-sonnet-20241022").
- Advanced settings: context window, max tokens, temperature.
- Pricing (optional): input price per 1K input tokens + per 1K output tokens (for usage cost calculation).
- "Test Connection" button → sends a simple test request.
- "Save" button.

## Screen 8: Usage / Limits
- **Top bar**: "Usage & Limits" title (collapsible).
- **Summary cards**:
  - Total tokens used (input + output).
  - Total estimated cost (if pricing configured).
  - Runs count (total + this month).
- **Per-provider breakdown** (list): each provider's token usage + cost.
- **Limit settings**:
  - "Per-run token cap" (input number). When exceeded → agent stops + error message.
  - "Per-run price cap" (input number, in user's currency). When exceeded → agent stops.
  - "Monthly token cap" (optional). When exceeded → agent disabled until next month.
  - Toggle: "Enable limits" (on/off).
- **Usage log** (recent runs with token + cost breakdown).

---

## Design language (Primary Glass — per R-A5)
- **Background**: `#F5F5F5` (cool light grey)
- **Surface**: `#FFFFFF` (cards)
- **Surface dark**: `#2E2E2E` (bottom nav, assistant bubble, tool card header)
- **Red**: `#E53935` (error/stop/destructive)
- **Yellow**: `#FFC107` (active/executing/caution)
- **Blue**: `#1E88E5` (primary action/agent/user message)
- **Frosted glass**: Haze 1.1.1, `backgroundColor = surface` (opaque), `tint = HazeTint(surface.copy(alpha = 0.73f))`, `blurRadius = 24.dp`
- **Fonts**: Inter (body) + Sora (display) + JetBrains Mono (code)
- **Bottom nav**: dark grey glass pill, 4 tabs, blue active indicator
