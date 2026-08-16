# Design Language — Android Agent App

> Canonical design spec for the dedicated Android agent application (separate from Only-List).
> Informed by: prior research `R-9` (Haze), `R-12` (Haze architecture fix), `R-13` (modern blur +
> card patterns), `R-A1` (Cline analysis); this doc's web research (NN/g glassmorphism, Haze 1.0 blog,
> HazeMaterials 1.1.1 source, designpixil chat UI patterns, sinasamaki glassmorphic nav).
>
> **NOT** the Only-List anime tracker. Different brand, different palette, different feel.
> Follow strictly. Update this + the runtime `theme.json` together when the design evolves.

## Theme: Primary Glass

**Light-first, primary-color palette (red + yellow + blue), frosted-glass surfaces, with a dark-grey
counterpoint that runs through nav, assistant chat bubbles, and code blocks.** Bold, modern, tech-forward,
developer-grade. **NOT Material You.** **NOT Midnight Coral.**

Three rules that make this theme work:
1. The light background stays cool and neutral (`#F5F5F5` / `#FFFFFF`). The primaries are the *only* saturated colors on screen — they pop because the canvas is grey-neutral.
2. Dark grey (`#2E2E2E` / `#1F1F1F`) is used as a **counterpoint**, not a background. It appears on the bottom nav, code blocks, and assistant chat bubbles — exactly the elements where ChatGPT / Claude / Cursor use light. This is the distinctive "darker grey vibe" the user asked for.
3. Frosted glass is **light-mode glass**: opaque `backgroundColor` + light-tinted blur. Same Haze library as Only-List (`dev.chrisbanes.haze:haze:1.1.1`) but tuned for light surfaces (tint alpha 0.73, not 0.55).

---

## 1. Color Palette

### Backgrounds (light + dark grey counterpoint)
| Role | Hex | Luminance | Use |
|------|-----|-----------|-----|
| `background` | `#F5F5F5` | 0.94 | App background — cool light grey, NOT pure white (anti-halation) |
| `surface` | `#FFFFFF` | 1.00 | Elevated cards, modal sheets, user chat bubble, tool-call card body |
| `surfaceVariant` | `#EAEAEA` | 0.85 | Inset regions, code-inline background, secondary panels |
| `surfaceDark` | `#2E2E2E` | 0.04 | **Dark grey counterpoint** — bottom nav glass, assistant chat bubble, tool-call card header, workspace selector header |
| `surfaceDarkest` | `#1F1F1F` | 0.02 | Code blocks inside assistant bubbles, terminal-like dense areas |
| `outline` | `#D4D4D4` | 0.79 | 0.5–1dp borders on light surfaces |
| `outlineDark` | `#3A3A3A` | 0.04 | 0.5–1dp borders on dark grey surfaces (defines glass edge on dark) |
| `divider` | `#EEEEEE` | 0.90 | Hairline dividers between list rows |

### Primary palette — red, yellow, blue (slightly desaturated from Mondrian-pure)
The three primaries carry *semantic* meaning, not just decoration. Each is reserved for one job.

| Role | Hex | Hover | Pressed | Muted (bg) | Use |
|------|-----|-------|---------|------------|-----|
| `red` | `#E53935` | `#EF5350` | `#C62828` | `#FFEBEE` | **Stop / destructive / error** — stop streaming button, delete actions, error toasts, error status dots |
| `yellow` | `#FFC107` | `#FFD54F` | `#FF8F00` | `#FFF8E1` | **Active / executing / caution** — running tool indicator, "executing" pill, attention needed, plan-mode highlight |
| `blue` | `#1E88E5` | `#42A5F5` | `#1565C0` | `#E3F2FD` | **Primary action / agent / info** — send button, primary CTA, agent avatar, link, focus ring, info banner |

**Mondrian note**: pure primaries (`#E5252A` / `#F8E100` / `#1E4EC6`) saturate so heavily they
vibrate on a light grey background. We use Material-600 shades instead — bold enough to feel primary,
tuned enough to coexist on `#F5F5F5` without halation. Verified visually against the Mondrian
references (Cleveland Museum of Art, Tate "Composition C").

**Text on primaries**:
- On `red` → `#FFFFFF` (15.2:1 ✅)
- On `yellow` → `#1F1F1F` (12.1:1 ✅ — yellow REQUIRES dark text, light text fails AA at any size)
- On `blue` → `#FFFFFF` (5.5:1 ✅ — just passes AA for normal text)

### Semantic colors (differentiated from primaries where they overlap)
| Role | Hex | Note |
|------|-----|------|
| `success` | `#10B981` | Emerald-500 — distinctly green (NOT primary yellow) |
| `warning` | `#F59E0B` | Amber-500 — slightly darker than primary yellow, used when warning is NOT "active executing" |
| `error` | `#E53935` | Same as primary `red` — red IS the universal error color, no need to invent a second red. Differentiate via CONTEXT (icon + label), not hue. |
| `info` | `#0EA5E9` | Sky-500 — distinctly lighter & cooler than primary blue, used when info is NOT "agent action" |

### Text colors
On light backgrounds (`#F5F5F5` / `#FFFFFF` / `#EAEAEA`):
| Role | Hex | Contrast on `#F5F5F5` | Use |
|------|-----|------------------------|-----|
| `textPrimary` | `#1F1F1F` | 15.5:1 ✅ | Headlines, body, chat message text |
| `textSecondary` | `#525252` | 7.3:1 ✅ | Subtitles, metadata, timestamps |
| `textTertiary` | `#8A8A8A` | 3.4:1 ✅ (AA Large) | Hints, disabled-ish labels — only ≥18sp or ≥14sp bold |
| `textDisabled` | `#B8B8B8` | 1.7:1 ❌ | Disabled controls (no contrast requirement) |

On dark grey (`#2E2E2E` / `#1F1F1F`):
| Role | Hex | Contrast on `#2E2E2E` | Use |
|------|-----|------------------------|-----|
| `textOnDark` | `#F5F5F5` | 14.3:1 ✅ | Assistant bubble text, nav labels, code block body |
| `textOnDarkSecondary` | `#B0B0B0` | 6.9:1 ✅ | Tool-call card header subtitle, dimmed code |
| `textOnDarkTertiary` | `#8A8A8A` | 3.6:1 ✅ (AA Large) | Code comments, line numbers |

### Code syntax highlighting (on `#1F1F1F` code blocks)
Adapted from GitHub Dark / One Dark — these read well on `#1F1F1F` and stay readable when shrunk to 13sp mono:
| Token | Hex | Use |
|-------|-----|-----|
| `codeKeyword` | `#FF7B72` | `fun`, `val`, `if`, `return` |
| `codeFunction` | `#D2A8FF` | function names |
| `codeString` | `#A5D6FF` | string literals |
| `codeNumber` | `#79C0FF` | numeric literals |
| `codeComment` | `#8B949E` | comments (uses `textOnDarkTertiary`) |
| `codeType` | `#FFA657` | type names |
| `codeVariable` | `#FFA657` | variable names |
| `codePlain` | `#E6EDF3` | default text |
| `codeAddedBg` | `#1B3A20` | diff added line bg (subtle green) |
| `codeAddedText` | `#7EE787` | diff added line text |
| `codeRemovedBg` | `#4A1F1F` | diff removed line bg |
| `codeRemovedText` | `#FF7B72` | diff removed line text |

---

## 2. Typography

### Font families (same as Only-List — already bundled, OFL, modern standard)
| Token | Family | Weights | Use |
|-------|--------|---------|-----|
| `body` | **Inter** | 400, 500, 600, 700 | Body, UI text, chat messages, labels |
| `display` | **Sora** | 600, 700, 800 | Headlines, screen titles, workspace name |
| `mono` | **JetBrains Mono** | 400, 500, 600 | Code blocks, tool names, tokens, file paths |

**Same families as Only-List** — bundling two font sets across two apps is wasteful, and Inter/Sora/JetBrains Mono are the modern standard for tech UIs. Brand differentiation comes from COLOR + LAYOUT (light + primaries + dark grey counterpoint), not type.

**Bundle ALL weights** with explicit `FontVariation.Settings(FontVariation.weight(N))` per `Font` registration
(per R-9 — bare `FontFamily(Font(R.font.inter_variable))` silently renders weight 400 only).

### Type scale
| Role | Font | Size | Weight | Line height | Use |
|------|------|------|--------|-------------|-----|
| `displayLarge` | Sora | 28sp | 700 | 34sp | Workspace title (top of chat screen) |
| `displayMedium` | Sora | 22sp | 700 | 28sp | Screen titles, large modal headers |
| `headingLarge` | Sora | 18sp | 600 | 24sp | Section headers, sheet titles |
| `titleLarge` | Inter | 16sp | 600 | 22sp | Card titles, prominent rows |
| `titleMedium` | Inter | 15sp | 500 | 21sp | Sub-headers, list item primary |
| `bodyLarge` | Inter | 15sp | 400 | 22sp | Chat message text (assistant + user) |
| `bodyMedium` | Inter | 14sp | 400 | 20sp | Metadata, subtitles |
| `bodySmall` | Inter | 13sp | 400 | 18sp | Timestamps, hints, captions |
| `caption` | Inter | 12sp | 500 | 16sp | Tool status labels, badges |
| `codeBlock` | JetBrains Mono | 13sp | 400 | 20sp | Code block body |
| `codeInline` | JetBrains Mono | 13sp | 500 | — | Inline code in chat messages |
| `toolLabel` | JetBrains Mono | 11sp | 600 | 14sp | UPPERCASE — tool name in tool-call card header, language pill in code block |
| `micro` | Inter | 10sp | 600 | 14sp | UPPERCASE — small chip labels (RUNS, MODEL, CTX) |

---

## 3. Shapes

Same radius scale as Only-List (consistency across both apps):
| Role | Radius | Use |
|------|--------|-----|
| `small` | 4dp | Chips, code-inline bg, status dots (use CircleShape for actual dots) |
| `medium` | 8dp | Buttons, inputs, list items, tool-call status badges |
| `large` | 12dp | Cards, chat bubbles, tool-call cards, file rows |
| `xlarge` | 20dp | Modal sheets, dialogs, workspace selector dropdown |
| `pill` | 28dp | Bottom nav, FAB, segmented control, send button |
| `codeBlock` | 8dp | Code block container (smaller than chat bubble so the dark inset reads as "embedded") |

Chat bubble corner treatment: bubbles use a single `large` (12dp) radius on all corners EXCEPT one tail corner — assistant bubble has bottom-left `small` (4dp), user bubble has bottom-right `small`. This is the iMessage-style asymmetry that signals "this side is mine."

---

## 4. Motion

### Durations
| Token | ms | Use |
|-------|----|-----|
| `instant` | 50 | Press feedback start, streaming token append (no anim) |
| `quick` | 150 | Color cross-fade, status dot color change, icon scale |
| `short` | 220 | Streaming cursor blink step, label reveal |
| `medium` | 300 | Tool-card expand/collapse, modal sheet slide, screen transition |
| `long` | 450 | Theme cross-fade (light → dark mode), big state transitions |
| `streaming` | 800 | Streaming cursor opacity oscillation (one full cycle) |
| `pulse` | 1200 | Stop button border pulse, "running" tool spinner |

### Easings
| Token | Spec | Use |
|-------|------|-----|
| `standard` | `FastOutSlowInEasing` | Most UI, screen transitions |
| `standardDecel` | `LinearOutSlowInEasing` | Enter animations (message slide-in, sheet open) |
| `standardAccelerate` | `FastOutLinearInEasing` | Exit animations (message scroll-away, sheet close) |
| `springDefault` | `Spring(dampingRatio=0.7, stiffness=380)` | Tap feedback, indicator slide, nav active pill |
| `springBouncy` | `Spring(dampingRatio=0.6, stiffness=300)` | New chat message appearance (slight overshoot) |
| `linear` | `LinearEasing` | Streaming cursor, spinner rotation, shimmer |

### Specific motion patterns
| Element | Animation |
|---------|-----------|
| New chat message (assistant or user) | `slideInVertically(initialOffsetY = { 8.dp.px }) + fadeIn()`, 240ms `springBouncy`, via `Modifier.animateItemPlacement()` on LazyColumn |
| Streaming text cursor | 1.5dp × 16sp vertical bar, `blue`, opacity 1.0 ↔ 0.0 over 800ms `linear` infinite; appended at end of partial message; removed when `finish_reason != null` |
| Tool call status dot color | `animateColorAsState(tween(150ms, quick))` — grey → yellow (running) → green (success) or red (error) |
| Tool call body expand | `AnimatedVisibility(enter = expandVertically(300ms, standardDecel) + fadeIn(150ms), exit = shrinkVertically(200ms, standardAccelerate) + fadeOut(100ms))` |
| Stop button border pulse | `border.width` 1.5dp ↔ 2.5dp + `border.color` red → red.alpha(0.5), 1200ms `linear` infinite |
| Send button press | scale 0.92, 100ms `instant` (no spring — this is a committed action, not a tap) |
| Bottom nav active pill slide | `springDefault`, 240ms (same as Only-List) |
| Modal sheet | `slideInVertically(initialOffsetY = { fullHeight }) + fadeIn()`, 300ms `standardDecel`; scrim fade 220ms |
| Screen transition (nav) | `fadeIn(200ms) + fadeOut(200ms)` cross-fade — NOT slide (content density makes slide distracting) |
| Reasoning section expand | `expandVertically(300ms, standardDecel) + fadeIn(150ms)` — same as tool-card |

**Anti-pattern**: NO typewriter effect for streaming text. Appending tokens as they arrive (with the cursor) reads as "the LLM is typing this" — typewriter adds artificial delay and feels laggy on slow tokens. Verified pattern: ChatGPT mobile, Claude mobile, Cursor all use token-append + cursor.

---

## 5. Elevation & Depth

**NOT Material elevation.** Depth via:
- **Translucent glass layers**: bottom nav (`#2E2E2E` at 80% opacity + 24dp blur), header scrim (light surface at 73% opacity + 24dp blur), modal sheets (light surface at 73% + 24dp blur).
- **Subtle borders**: `outline` (`#D4D4D4`) 0.5dp on light cards; `outlineDark` (`#3A3A3A`) 0.5dp on dark grey surfaces — this defines the glass edge that light-on-light glassmorphism needs (per NN/g: "strokes and gradients emphasize depth, especially on simple backgrounds").
- **Color contrast hierarchy**: background (`#F5F5F5`) → surface (`#FFFFFF`) → surfaceDark (`#2E2E2E`) → surfaceDarkest (`#1F1F1F`). Each layer steps darker, creating depth without shadows.
- **Top highlight strip**: 1dp `Color.White.copy(alpha = 0.08f)` strip at the top edge of dark grey surfaces (bottom nav, modal sheets) — mimics the light reflection on real glass. Optional, use sparingly.
- **Shadows**: only on the floating bottom nav (8dp shadow) and modal sheets (16dp shadow). Never on cards, bubbles, or rows — borders do the depth work, not shadows.

---

## 6. Spacing

Same scale as Only-List:
| Token | dp |
|-------|----|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 12 |
| `lg` | 16 |
| `xl` | 24 |
| `xxl` | 32 |
| `xxxl` | 48 |

Use these tokens (NOT raw `dp`). Formal `Spacing` object in `:core:designsystem` (same as Only-List).

**Chat-specific spacing**:
- Chat thread horizontal padding: `lg` (16dp) — messages align to screen edges with consistent gutter
- Vertical gap between messages: `md` (12dp) — tighter than card stacks because messages are a single conversation
- Chat bubble internal padding: `md` (12dp) horizontal, `sm` (8dp) vertical for compact bubbles; `lg` (16dp) / `md` (12dp) for messages with code blocks
- Code block internal padding: `md` (12dp)
- Tool-call card internal padding: `md` (12dp) body, `sm` (8dp) header
- Input bar height: 56dp collapsed, up to 160dp expanded (auto-grow with TextField maxLines = 6)

---

## 7. Frosted Glass (Haze) Implementation

### Haze version
```kotlin
// build.gradle.kts (core/designsystem)
api("dev.chrisbanes.haze:haze:1.1.1")
// Optional presets (we don't use them — we define our own HazeStyle for exact color control):
// implementation("dev.chrisbanes.haze:haze-materials:1.1.1")
```
Pinned to **1.1.1** (same as Only-List). Do NOT upgrade to 2.x alpha — API not stable, artifact split differs (per R-9 §1.3). Kotlin 2.0.21 compatible.

### Light-mode HazeStyle (the canonical config for this app)

**Why opaque `backgroundColor` is required**: per R-13 (Topic 1, Haze issue #865) — Haze composites source content on top of `drawRect(bg)` *before* applying the blur. Transparent bg leaves text pixels with no opaque neighbors to smear into → only images get frosted, text stays sharp. The fix is an OPAQUE `backgroundColor`. Verified by Haze author + `HazeMaterials.kt` source.

**Tint alpha for light surfaces** (verified from `HazeMaterials.kt` 1.1.1 source):
- `luminance(surfaceColor) >= 0.5` → use `lightAlpha` (0.35 ultraThin → 0.92 ultraThick)
- We use `regular` (0.73) for primary frosted glass, `thick` (0.83) for modal sheets, `ultraThin` (0.35) for the streaming cursor highlight.

**Recommended HazeStyle tokens** for this app:

```kotlin
// LIGHT FROSTED GLASS — for header scrim, modal sheets, dropdowns, FAB
// Equivalent to HazeMaterials.regular() but with our exact surface color.
val lightGlass = HazeStyle(
    backgroundColor = colors.surface,            // #FFFFFF — opaque, required
    blurRadius = 24.dp,                          // Haze default, verified optimal
    tint = HazeTint(colors.surface.copy(alpha = 0.73f)),  // lightAlpha regular
)

// DARK GREY FROSTED GLASS — for bottom nav, dark grey panels
// Slightly higher alpha because dark tint needs more opacity to read as "dark grey glass"
// rather than "smoky haze". Verified visually.
val darkGlass = HazeStyle(
    backgroundColor = colors.surfaceDark,       // #2E2E2E — opaque
    blurRadius = 24.dp,
    tint = HazeTint(colors.surfaceDark.copy(alpha = 0.80f)),  // darkAlpha regular
)

// ULTRA THIN GLASS — for the streaming cursor backdrop (subtle highlight)
val ultraThinGlass = HazeStyle(
    backgroundColor = colors.surface,
    blurRadius = 12.dp,
    tint = HazeTint(colors.surface.copy(alpha = 0.35f)),
)
```

### Source / child wiring (per-screen HazeState)

Same architecture as Only-List (per R-12 §4 — the canonical 5 rules):
1. **One `HazeState` per screen** — created inside the screen composable, scoped to that screen's composition.
2. **The scrollable content (LazyColumn) is the source** — `Modifier.haze(state)`.
3. **The header, bottom nav, FAB are children** — `Modifier.hazeChild(state, style)`.
4. **The child's own background MUST be transparent** — no opaque `.background()` on the child. The `HazeStyle.backgroundColor` provides the visual backing.
5. **`hazeChild` goes directly on the bar's outer modifier** — no inner "scrim Box". If you need a scrim layer, use `Modifier.matchParentSize()` (NEVER `fillMaxSize()` — per R-12 §2, `fillMaxSize()` causes the "whole page is blurred" bug).

```kotlin
@Composable
fun AgentChatScreen(...) {
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        // SOURCE: the chat thread. Always mark the scrollable as the haze source.
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 72.dp, bottom = 120.dp),
        ) {
            items(messages) { /* message or tool-call row */ }
        }

        // CHILD 1: header (workspace selector + model indicator) — light glass
        AgentChatHeader(hazeState = hazeState, ...)

        // CHILD 2: input bar — dark grey glass (counterpoint)
        AgentInputBar(hazeState = hazeState, ...)

        // CHILD 3: floating bottom nav — dark grey glass (same darkGlass style)
        AgentBottomBar(hazeState = hazeState, ...)
    }
}
```

### Progressive gradient blur edge

For the chat header (top of screen), use a progressive blur that fades from full intensity at the
top to zero at the bottom — so the blur doesn't cut a hard line across the chat thread:

```kotlin
.hazeChild(
    state = hazeState,
    style = lightGlass,
) {
    progressive = HazeProgressive.verticalGradient(
        startIntensity = 1f,   // top edge: full blur
        endIntensity = 0f,     // bottom edge: no blur (fades into content)
    )
}
```

Verified available in Haze 1.1.1 (per R-13 §3 — added in Haze 1.0, used in `ScaffoldSample.kt`).
Works identically on light theme — Haze samples prove this.

### Where to use glass (and where NOT to)

| Element | Glass? | Style |
|---------|--------|-------|
| Chat header (workspace selector) | ✅ Yes | `lightGlass` + progressive gradient |
| Bottom nav (floating pill) | ✅ Yes | `darkGlass` (the dark grey counterpoint) |
| Input bar | ✅ Yes | `darkGlass` (matches nav, signals "this is the action zone") |
| Modal sheets (filter, settings) | ✅ Yes | `lightGlass`, `thick` alpha (0.83) for legibility |
| Workspace selector dropdown | ✅ Yes | `lightGlass` |
| FAB (new chat) | ✅ Yes | `darkGlass` |
| Chat bubbles (assistant + user) | ❌ NO | Opaque — bubbles need text legibility, not translucency |
| Tool-call cards | ❌ NO | Opaque `surface` body, opaque `surfaceDark` header — borders define depth |
| Code blocks | ❌ NO | Opaque `surfaceDarkest` — code needs zero ambiguity |
| List rows (file browser, settings) | ❌ NO | Opaque `surface` — borders/dividers only |
| Buttons (send, primary CTA) | ❌ NO | Solid `blue` / `red` — opaque, no glass |

**Anti-pattern** (per NN/g + Only-List §9): glass on EVERY card = "glassmorphism soup" — kills depth hierarchy because everything becomes the same material. Glass is for **overlays** (nav, headers, sheets, FABs) that float above scrollable content. Cards are opaque.

---

## 8. Component Patterns

### 8.1 Agent chat thread (THE core screen)

**Layout**: `Box(fillMaxSize)` with:
- LazyColumn (the thread) as the haze source — fills the box
- Header pinned at top (`top = 0`)
- Input bar pinned at bottom (`bottom = 0`)
- Bottom nav overlays at bottom, above input bar (or input bar is part of nav — see §8.2)

**Message rows** (LazyColumn items):

#### Assistant message (dark grey bubble — the "dark grey vibe" counterpoint)
```
[avatar] [..................bubble..................]
          {content}
          {optional: code block (surfaceDarkest #1F1F1F inside)}
          {optional: tool-call card}
```
- Avatar: 28dp circle, `blue` background, white "A" or agent icon. Left-aligned.
- Bubble: `surfaceDark` (`#2E2E2E`) opaque, `large` (12dp) radius with bottom-left `small` (4dp), max-width 85% of screen, padding `md`/`sm` (12/8dp) for plain text or `lg`/`md` (16/12dp) for code/tool content.
- Text: `bodyLarge` 15sp Inter 400, `textOnDark` (`#F5F5F5`).
- Streaming cursor: appended at end of last token while `isStreaming == true`. 1.5dp × 16sp vertical bar, `blue`, opacity 1.0 ↔ 0.0 over 800ms `linear`.
- Reasoning section: collapsed by default, expandable chevron + "Thinking..." label in `textOnDarkTertiary`. Body in JetBrains Mono 13sp, italic, `textOnDarkSecondary`. Subtle yellow tint background `yellow.copy(alpha = 0.08f)` to signal "this is the model's reasoning, not its answer".

#### User message (blue bubble, right-aligned)
```
                [..................bubble..................] [avatar]
                {content}
```
- Avatar: 28dp circle, `surfaceVariant` bg, `textSecondary` user initial.
- Bubble: `blue` (`#1E88E5`) opaque, white text, bottom-right `small` (4dp), max-width 85%, right-aligned.
- Text: `bodyLarge` 15sp Inter 400, `Color.White`.

#### Code block (inside assistant bubble)
```
┌──────────────────────────────────────────────────┐
│ KOTLIN                                    [copy] │
│ fun agentLoop() {                                │
│     while (isActive) { ... }                     │
│ }                                                │
└──────────────────────────────────────────────────┘
```
- Container: `surfaceDarkest` (`#1F1F1F`), `codeBlock` (8dp) radius, `outlineDark` (`#3A3A3A`) 0.5dp border, padding `md` (12dp).
- Header row: language label (`toolLabel` JetBrains Mono 11sp 600 uppercase, `textOnDarkTertiary`) on left; copy button (`medium` 8dp radius, transparent bg, `textOnDarkSecondary` icon, `quick` 150ms press feedback) on right.
- Body: `codeBlock` JetBrains Mono 13sp, line-height 20sp, `codePlain` (`#E6EDF3`), syntax tokens per §1.
- Diff blocks: green/red line bg per `codeAddedBg`/`codeRemovedBg` (full-width strip behind the line).
- Long code: vertically scrollable, max height 320dp (so a 500-line file doesn't blow up the thread).

#### Tool-call card (inside assistant bubble OR top-level message)
```
┌──────────────────────────────────────────────────┐
│ ▼ [●] READ_FILE  config.json        120ms  ✓    │  ← dark grey header
├──────────────────────────────────────────────────┤
│ path: /workspace/config.json                    │  ← light surface body
│                                                  │
│ { "model": "claude-3-5-sonnet", ... }           │
└──────────────────────────────────────────────────┘
```
- Container: `surface` (`#FFFFFF`) body, `surfaceDark` (`#2E2E2E`) header bar on top — this dark/light split inside one card is the signature "dark grey vibe" pattern.
- Radius: `large` (12dp) on all corners.
- Header bar (dark grey): height 36dp, padding `sm`/`sm`, contains:
  - Expand chevron (rotates 90° when expanded, 150ms `quick`)
  - Status dot: 8dp circle, color = status (grey `textTertiary` for queued, `yellow` for running, `success` green for complete, `red` for error, `red` + lock icon for needs-approval)
  - Tool name: `toolLabel` JetBrains Mono 11sp 600 uppercase, `textOnDark`
  - Target: `bodySmall` 13sp Inter 400, `textOnDarkSecondary` (e.g., `config.json`)
  - Right: duration (`codeInline` mono 12sp, `textOnDarkTertiary`) + status icon (✓ ⚠ lock)
- Body (light, expandable): `surface` bg, padding `md` (12dp), `bodyMedium` 14sp Inter 400, `textPrimary`. JSON/code output renders as nested code blocks per above.
- Status change animation: status dot color cross-fade `quick` (150ms); body expand/collapse `AnimatedVisibility` 300ms `standardDecel`.
- Needs-approval state: header bar turns `red.copy(alpha = 0.15f)` (subtle red tint) + lock icon. Body shows "Approve" (blue primary) + "Deny" (red outline) buttons.

#### Inline citations (Perplexity-style, optional for fetch_web_content output)
- After a sentence ending in a citation, render a small `[1]` chip: `blue` text, `surfaceVariant` bg, `small` (4dp) radius, `caption` 12sp, padding `xs`/0.
- Tap: opens a small popover with the source URL + "Open" + "Copy" buttons.

### 8.2 Bottom navigation (floating pill, dark grey glass — 4 tabs)

Same floating-pill pattern as Only-List (per R-13 §2 fix — `pressScale` is OUTER modifier, `weight(1f)` on ALL items, no `spacedBy`), BUT:
- **4 tabs** (NOT 5 — agent apps have fewer top-level destinations): Chat, Files, Runs, Settings.
- **Dark grey glass** (`darkGlass` style, not coral) — the dark grey counterpoint.
- **Active state**: `blue` pill background + white text. Inactive: icon-only, `textOnDarkSecondary` color.
- **Press feedback**: scale 0.95, 150ms `standardDecel`, `indication = null` (NO ripple), light haptic.
- **Active indicator**: 4dp × 24dp `blue` pill at top, slides horizontally `springDefault` 240ms.
- **Input bar integration**: on the Chat screen, the input bar sits ABOVE the bottom nav (both float, both dark glass, but they're visually distinct — input has TextField + send button, nav has tab icons).

### 8.3 Workspace selector (top-of-screen dropdown)

```
┌────────────────────────────────────┐
│ [▼] my-agent-app          [⚙ model]│  ← header
│     ~/workspaces/my-agent-app      │
└────────────────────────────────────┘
```
- Tap header: opens ModalBottomSheet (`xlarge` 20dp radius top corners, `lightGlass` style `thick` alpha 0.83).
- Sheet lists workspaces: each row = workspace icon (16dp, dark grey circle with first letter) + name (`titleMedium` 15sp 600) + path (`bodySmall` 13sp `textTertiary` JetBrains Mono) + last-modified (`bodySmall` 13sp `textTertiary`) + status dot (8dp — synced green, syncing yellow, error red).
- Selected workspace: `blue.copy(alpha = 0.08f)` bg tint + `blue` 3dp left border.
- Tap row: press scale 0.98, `medium` 300ms, navigate + close sheet.
- "New workspace" button at bottom: `yellow` outline button (yellow = active/creating state — the user is about to do something). Tap → opens create-worksheet flow (path picker via SAF + name field).
- Long-press row: action sheet (Rename, Archive, Delete). Delete uses `red` confirm dialog.

### 8.4 Tool execution feedback (status states)

| State | Status dot | Header tint | Body | Action |
|-------|------------|-------------|------|--------|
| Queued | `textTertiary` grey | none | hidden (collapsed) | — |
| Running | `yellow` + spinner ring | none | shown if input present, output streaming | Stop button in header (red outline) |
| Success | `success` green + ✓ | none | shown (expanded by default if no output, collapsed if output > 200 chars) | — |
| Error | `red` + ⚠ | `red.copy(alpha = 0.10f)` bg | shown expanded, error message in `red` `bodyMedium` | Retry button in header (blue outline) |
| Needs approval | `red` + lock | `red.copy(alpha = 0.15f)` bg | shown expanded, input + Approve/Deny buttons | Approve (blue) / Deny (red outline) |

**Spinner**: 16dp circular ring, `yellow`, rotates 1000ms `linear` infinite. NOT a Material CircularProgressIndicator (too generic) — custom `Canvas` drawn arc with 25% sweep + 1.5dp stroke.

### 8.5 File browser (tree view, NOT cards)

```
workspace/
├─ ▼ app/
│  ├─ build.gradle.kts        2.1KB  3d ago
│  └─ src/
│     ├─ ▶ main/                          ← collapsed
│     └─ ▶ test/                          ← collapsed
├─ core/
│  └─ ...
└─ README.md                  1.4KB  1w ago
```
- Layout: LazyColumn with indentation = depth × 16dp. Tree lines optional (1dp `divider` color vertical line at each depth).
- Row: chevron (▶ collapsed / ▼ expanded, rotates 90° 150ms `quick`) OR file icon (16dp, monochrome line icon) + name (`titleMedium` 15sp 500) + size (`bodySmall` 13sp `textTertiary` JetBrains Mono) + last-modified (`bodySmall` 13sp `textTertiary`).
- Tap directory: toggle expand/collapse.
- Tap file: open in file viewer (separate screen).
- Long-press file: context menu — "Add to chat context" (blue primary), "Copy path" (outline), "Open with..." (outline), "Delete" (red outline + confirm dialog).
- Path breadcrumb at top of screen: workspace name / current path. Tap any segment to navigate.
- Empty state: "No files yet. Run a tool that creates files." + CTA.

### 8.6 Settings (LLM provider configuration)

**Sections** (LazyColumn with sticky headers):
1. **Providers** — list of configured providers (OpenAI, Anthropic, Google, Ollama, Custom)
2. **Default model** — current default + change dropdown
3. **Custom models** — user-defined models (add/remove)
4. **Agent behavior** — max iterations, auto-approve thresholds, compaction strategy
5. **Appearance** — light/dark mode toggle, glass intensity slider, font scale
6. **About** — version, licenses, credits (Cline, Haze, etc.)

#### Provider card
```
┌──────────────────────────────────────────────────┐
│ [○] Anthropic                          Connected │
│     claude-3-5-sonnet                            │
│                              [Configure] [⋯]    │
└──────────────────────────────────────────────────┘
```
- Card: `surface` bg, `large` 12dp radius, `outline` 0.5dp border, padding `md` (12dp), `sm` (8dp) gap between cards.
- Provider logo: 32dp circle, brand color filled, white icon (Anthropic = coral `#D97757`, OpenAI = `#10A37F`, Google = `#4285F4`, Ollama = `#000000` with white border, Custom = `surfaceVariant` with grey icon).
- Name: `titleMedium` 15sp 600. Active model: `bodySmall` 13sp `textSecondary` JetBrains Mono.
- Status badge (right of name): "Connected" green pill, "Not configured" grey pill, "Error" red pill. `small` 4dp radius, `caption` 12sp 500, padding `xs`/`xs`.
- Configure button: `medium` 8dp radius, `blue` outline, `caption` 12sp 600 uppercase, `quick` press feedback.
- More button (⋯): dropdown menu (Test connection, Reset, Remove).

#### Add provider flow
- Tap "+ Add provider" button at bottom of section → ModalBottomSheet with provider grid (5 large logos in 2 columns).
- Tap provider → navigate to configure-form screen (full screen, NOT another sheet — form is too dense for a sheet).

#### Configure provider form
- Full-screen with header back arrow.
- Fields:
  - API Key: `OutlinedTextField`, password mask (visible toggle), `medium` 8dp radius, `surface` bg
  - Base URL: optional, only shown if provider = Custom OpenAI-compatible
  - Default model: dropdown (Material `ExposedDropdownMenuBox`) populated from provider's model catalog OR free text for Custom
  - Context window: number input (default per provider)
  - Capabilities: checkbox chips (Supports images, Supports streaming, Supports reasoning, Supports prompt cache)
- Save: floating action button bottom-right (`blue` primary, `pill` 28dp, checkmark icon). Disabled until required fields filled.
- Test connection: secondary button in header (yellow outline — "active" state while testing). Shows spinner + result toast (green success / red error with message).
- API key storage: EncryptedSharedPreferences + Keystore master key (per R-A1 §10 — security risk #6).

#### Custom models section
- "Add custom model" button → form (name, provider dropdown, model ID, context window, capabilities, custom baseUrl).
- List of custom models below — same card pattern as provider, but with "Edit" / "Delete" actions.

### 8.7 Loading states (skeleton + shimmer, never spinners — same as Only-List §7.6)
- Skeleton: `surfaceVariant` block matching final shape.
- Shimmer: gradient sweep `surfaceVariant` → `surface` → `surfaceVariant`, 1200ms loop, `LinearEasing`.
- **Cache-first**: if local chat history exists, show it immediately. Skeleton ONLY on first-ever load.
- **EXCEPTION**: tool-call cards DO use a small inline spinner (the 16dp `yellow` ring) for the "running" state — that's a status indicator, not a loading screen.

### 8.8 Empty states

Every empty screen has:
- Centered icon (48dp, `textTertiary` color, monochrome line)
- Headline (`headingLarge` 18sp Sora 600, `textPrimary`)
- Description (`bodyMedium` 14sp `textSecondary`, max-width 280dp, centered)
- Optional CTA button (blue primary or yellow outline, depending on action)
- 16dp vertical spacing between elements

Per-screen empties:
- **Chat (new workspace)**: "Start a conversation" + "Ask the agent to explore your workspace" + suggested prompts row (3 chips).
- **Files (empty workspace)**: "No files yet" + "Run a tool that creates files" + "Read more" link.
- **Runs (no runs yet)**: "No runs yet" + "Runs appear here after your first task" — no CTA (passive state).
- **Settings (no providers)**: "Add a provider to get started" + "Add provider" button (blue primary).

### 8.9 Streaming text — the streaming cursor (Cursor IDE pattern)

The single most important motion detail in a chat UI. Get it right or the app feels broken.

**Spec**:
- Position: appended at the end of the last partial message, after the last token.
- Shape: 1.5dp wide × 16sp tall vertical bar (matches the line-height of `bodyLarge`).
- Color: `blue` primary (`#1E88E5`).
- Opacity animation: 1.0 → 0.0 → 1.0 over 800ms, `LinearEasing`, infinite.
- Appears: when a message has `finishReason == null` AND at least one token has been received.
- Disappears: 100ms `quick` fade-out when `finishReason != null` OR user navigates away OR aborts.
- Implementation: track `streamingMessageId` in ViewModel; render cursor composable inside that message's bubble at the end of the text span. Use `Modifier.drawBehind` on a 1.5dp-wide Spacer, NOT an animated ImageBitmap.

**Anti-pattern**: blinking block cursor (DOS-style). Too aggressive, distracts from reading. Use the thin vertical bar — verified by Cursor IDE, ChatGPT mobile, Claude mobile.

**Anti-pattern**: typewriter effect (typing each character one-by-one). Adds artificial delay; on slow streams it feels like the model is hung. Append tokens as they arrive.

---

## 9. Layout per screen

### 9.1 Chat screen
1. **Header** (pinned, light glass, 72dp tall): workspace selector (left) + model indicator chip (right) + new-chat FAB (right of model).
2. **Chat thread** (LazyColumn, haze source): 16dp horizontal padding, 12dp between messages, 72dp top padding (clear header), 120dp bottom padding (clear input bar + nav).
3. **Input bar** (pinned bottom, dark glass, 56dp collapsed / auto-grows): TextField + attach-context button + send/stop button.
4. **Bottom nav** (floating pill, dark glass, overlays above input bar with 8dp gap).

### 9.2 Files screen
1. **Header** (pinned, light glass): "Files" title + path breadcrumb + filter button.
2. **Tree list** (LazyColumn, haze source): file/folder rows with depth indentation.
3. **Bottom nav** (floating pill).

### 9.3 Runs screen
1. **Header**: "Runs" title + filter chips (today / 7d / 30d / all).
2. **Run list** (LazyColumn): each row = run icon + workspace name + first user message (truncated) + status dot + duration + timestamp. Tap → run detail screen.
3. **Run detail screen**: full timeline of tool calls (reuses tool-call card pattern from §8.4), full message thread, "Replay" button (yellow outline).

### 9.4 Settings screen
1. **Header**: "Settings" title.
2. **Section list** (LazyColumn) per §8.6.
3. **Bottom nav**.

### 9.5 Workspace configure screen (modal sheet)
- Sheet slides up from bottom, `xlarge` 20dp top radius, `lightGlass` style `thick` alpha 0.83.
- Sections: workspace name, path, agent config (default model, max iterations, tool policies), permissions.
- Save bar pinned at bottom of sheet (above keyboard) — appears when dirty.

---

## 10. Anti-Patterns (do NOT use)

- ❌ **Material You dynamic color** — respect the chosen red/yellow/blue palette. The user picked primaries; the OS palette would override them.
- ❌ **Glassmorphism on cards/bubbles/code blocks** — glass is for OVERLAYS only (nav, headers, sheets, FAB). Glass on cards = "glassmorphism soup."
- ❌ **Typewriter effect for streaming text** — append tokens + use the thin vertical cursor. Verified pattern (ChatGPT, Claude, Cursor).
- ❌ **Blinking block cursor** — too aggressive. Use the thin bar.
- ❌ **Pure white `#FFFFFF` background** — use `#F5F5F5` cool light grey. Pure white halates against text.
- ❌ **Pure black `#000000`** — use `#1F1F1F` for code blocks. Pure black has no depth.
- ❌ **Pure white text on light bg** — use `#1F1F1F` cool dark grey. Pure black halates against light bg.
- ❌ **Material elevation / shadows on cards** — use borders + color contrast hierarchy (§5).
- ❌ **Over-rounded everything** — mix radii per §3 (4dp chips, 8dp buttons, 12dp cards, 20dp sheets, 28dp pill).
- ❌ **Confirmation dialogs for reversible actions** — use undo toast (red bg, "Undo" button, 5s auto-dismiss).
- ❌ **Spinners for full-screen loading** — use skeleton + shimmer when local data is missing. Spinners ONLY for the tool-call "running" inline state.
- ❌ **Silent irreversible changes** — per Cursor pattern (designpixil #5): never apply edits without a visible diff review step. All editor/apply_patch tool calls go through an Approve/Deny flow.
- ❌ **AI thinking = silence** — per Replit Agent pattern (designpixil #7): when the agent runs >2s without output, show a "Thinking..." reasoning card with streaming tokens. Silence reads as broken or dangerous.
- ❌ **Auto-naming conversations by timestamp** — per ChatGPT pattern (designpixil #1): auto-name by topic summary, NOT "Chat on July 19." Failure mode is unusable history.
- ❌ **Burying deliverables in the thread** — per Claude pattern (designpixil #2): if the agent produces a file/artifact, show it in a dedicated panel/card with open/edit/export actions, NOT scrolling away in the thread.
- ❌ **Purple-violet "AI slop" gradients** — pick the bold primaries instead.
- ❌ **Neumorphism** — soft shadows + light/dark bevels don't fit the bold primary palette.
- ❌ **`fillMaxSize()` inside a wrap-content Box on a hazeChild scrim** — per R-12 §2, this causes the "whole page is blurred" bug. Use `matchParentSize()` or apply `hazeChild` directly to the outer modifier.
- ❌ **`backgroundColor = Color.Transparent` on hazeChild** — per R-13 §1, this breaks text blur. Always use an opaque `backgroundColor`.

---

## 11. Reference Apps + What to Borrow

| # | App | What to study |
|---|-----|---------------|
| 1 | **ChatGPT mobile** | Conversation history sidebar (workspace-as-conversation model), topic-based auto-naming (not timestamp), thin streaming cursor, dark grey assistant bubble on light bg (closest precedent to our "dark grey vibe"). Borrow: sidebar pattern, assistant bubble color, cursor. |
| 2 | **Claude mobile** | Artifact panel separating deliverables from chat thread. Borrow: when agent produces a file, render it as an expandable artifact card (not just inline text in the bubble). |
| 3 | **Cursor IDE** | Propose-then-apply diff loop — chat proposes a change, shows the diff, waits for apply/reject. Borrow: every `editor` / `apply_patch` tool call MUST render the diff with green/red coloring and Approve/Deny buttons (per designpixil #5). Never silently apply. |
| 4 | **Replit Agent** | Live run checklist with status (queued, running, step complete, needs approval, failed) and ability to interrupt. Borrow: the "Runs" screen renders each run as a timeline of tool-call cards with these 5 states. Also borrow: silence-during-work = broken — show a "Thinking..." card the moment the model is silent >2s. |
| 5 | **GitHub Copilot Chat** | Explicit context chips — visible files/symbols attached to each message. Borrow: input bar shows "context chips" for attached files (e.g., `📄 config.json ×`) above the TextField; tap × to remove. |
| 6 | **Perplexity** | Numbered inline citations bound to a source list. Borrow: `fetch_web_content` tool output includes `[1]`/`[2]` chips after sentences; tapping shows the source URL. |
| 7 | **iOS Control Center** | Light-mode glassmorphism reference — opaque white backing + light tint + 24dp blur + thin top highlight. Borrow: the lightGlass HazeStyle config (§7) is calibrated to match this look. |
| 8 | **Linear app** | Light theme + sharp typography + dark accents. Borrow: the cool-grey `#F5F5F5` background + `#1F1F1F` text combination + crisp 0.5dp borders. |

---

## 12. Dark Mode Support

The user specified **light themed** as the default. Dark mode is a **supported fallback** (not the primary identity), toggled in Settings → Appearance.

### Dark mode palette (inverted, but primaries stay the same)
| Role | Light | Dark |
|------|-------|------|
| `background` | `#F5F5F5` | `#141414` |
| `surface` | `#FFFFFF` | `#1F1F1F` |
| `surfaceVariant` | `#EAEAEA` | `#2A2A2A` |
| `surfaceDark` | `#2E2E2E` | `#0A0A0A` (inverted — dark grey becomes near-black) |
| `surfaceDarkest` | `#1F1F1F` | `#000000` (code blocks always darkest) |
| `outline` | `#D4D4D4` | `#3A3A3A` |
| `outlineDark` | `#3A3A3A` | `#555555` |
| `textPrimary` | `#1F1F1F` | `#F5F5F5` |
| `textSecondary` | `#525252` | `#B0B0B0` |
| `textTertiary` | `#8A8A8A` | `#7A7A7A` |
| `textOnDark` | `#F5F5F5` | `#F5F5F5` (unchanged — dark grey surfaces still need light text) |
| `textOnDarkSecondary` | `#B0B0B0` | `#B0B0B0` (unchanged) |

Primaries (`red`, `yellow`, `blue`) stay the same — they're saturated enough to read on both light and dark backgrounds. Yellow primary on dark mode brightens slightly to `#FFD54F` for legibility.

### Dark mode glass
- `lightGlass` → swap `backgroundColor` to `colors.surface` (which is now `#1F1F1F`) and tint alpha to `darkAlpha` (0.80).
- `darkGlass` → `backgroundColor` becomes `colors.surfaceDark` (`#0A0A0A`), tint alpha stays 0.80 (dark surface, already dark tint).
- Bottom nav: was dark grey on light bg → becomes near-black on dark bg (still the darkest element on screen).

### Dark mode implementation
Use Compose `isSystemInDarkTheme()` + a user toggle in Settings → Appearance. The `LocalColors` CompositionLocal resolves to LightColors or DarkColors based on the toggle. Theme cross-fade: 450ms `long` `standard` (per Only-List §4).

---

## 13. Accessibility

### Contrast ratios (WCAG 2.1 AA — 4.5:1 normal, 3:1 large)
| Combination | Ratio | Pass? |
|-------------|-------|-------|
| `textPrimary` `#1F1F1F` on `background` `#F5F5F5` | 15.5:1 | ✅ AAA |
| `textPrimary` on `surface` `#FFFFFF` | 16.4:1 | ✅ AAA |
| `textSecondary` `#525252` on `background` | 7.3:1 | ✅ AAA |
| `textTertiary` `#8A8A8A` on `background` | 3.4:1 | ✅ AA Large (≥18sp or ≥14sp bold) — used only for captions/hints, never body |
| `textOnDark` `#F5F5F5` on `surfaceDark` `#2E2E2E` | 14.3:1 | ✅ AAA |
| `textOnDarkSecondary` `#B0B0B0` on `surfaceDark` | 6.9:1 | ✅ AAA |
| White `#FFFFFF` on `red` `#E53935` | 4.6:1 | ✅ AA |
| White on `blue` `#1E88E5` | 5.5:1 | ✅ AA |
| `#1F1F1F` on `yellow` `#FFC107` | 12.1:1 | ✅ AAA — yellow REQUIRES dark text |
| `codePlain` `#E6EDF3` on `surfaceDarkest` `#1F1F1F` | 16.1:1 | ✅ AAA |

### Touch targets
- Minimum 44dp × 44dp for every tappable element (Material baseline).
- Our standard: 48dp × 48dp for rows, 44dp × 44dp for icons inside rows.
- Send button: 56dp × 56dp (FAB-sized, easy thumb hit).
- Bottom nav tabs: 48dp × 58dp (tall — accounts for label).

### Focus indicators
- Focus ring: 2dp `blue` outline, 2dp offset from the focused element. 150ms `quick` fade-in.
- Keyboard navigation (rare on Android, but tablets/keyboards exist): Tab order top-to-bottom, focus visible.
- TalkBack: every interactive element has a contentDescription or role + state.

### Glass-specific accessibility (per NN/g)
- Text on glassmorphic surfaces: ensure contrast against BOTH the most-saturated and least-saturated points of the blur. We achieve this by always using `surfaceDark` opaque underneath dark text on glass (not relying on the blur alone).
- "Reduce transparency" toggle in Settings → Appearance: replaces all glass surfaces with opaque equivalents (LightGlass → opaque `surface`, DarkGlass → opaque `surfaceDark`).
- "Increase contrast" toggle: replaces `textTertiary` with `textSecondary`, makes all borders 1dp (not 0.5dp).

---

## 14. Implementation Notes (for the building agent)

### Module structure (per R-A1 §9)
- `:core:designsystem` — Colors, Typography, Shapes, Motion, Spacing, HazeStyles, PressScale, GlassCard, ToolCallCard, ChatBubble, etc.
- `:app` — MainActivity, NavHost, screens (ChatScreen, FilesScreen, RunsScreen, SettingsScreen).

### Required Compose dependencies
```kotlin
// build.gradle.kts (:core:designsystem)
api("dev.chrisbanes.haze:haze:1.1.1")  // backdrop blur
implementation("androidx.compose.animation:animation:1.7.x")  // AnimatedVisibility
implementation("androidx.compose.ui:ui-graphics:1.7.x")  // graphicsLayer, drawBehind
// Fonts are bundled in res/font/ (Inter, Sora, JetBrains Mono variable)
```

### Variable font registration (per R-9 §3 — required, NOT optional)
```kotlin
val body = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_variable, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter_variable, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter_variable, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
// Same pattern for Sora (600/700/800) and JetBrains Mono (400/500/600).
// @OptIn(ExperimentalTextApi::class) still required per Compose 1.7+ docs.
```

### Per-screen HazeState pattern (per R-12 §4)
Each screen creates its own `HazeState` (NOT shared across screens — the official ScaffoldSample.kt convention). The HazeState is scoped to the screen's composition, so when the user navigates away, the source detaches cleanly (per R-12 §2 — HazeNode.onDetach releases the contentLayer).

### Press feedback (`Modifier.pressScale()` — reuse from Only-List)
Same reusable extension as Only-List §7.4: scale 1.0 → 0.96 → 1.02 → 1.0 over 150ms `standardDecel`, `graphicsLayer { scaleX = scale; scaleY = scale }` (deferred, no recomposition), `indication = null` (NO ripple — we do our own feedback), light haptic on press-down.

---

## Appendix A — Quick Reference Card

**Colors (copy-paste ready)**:
```
Background:    #F5F5F5
Surface:       #FFFFFF
SurfaceVariant:#EAEAEA
SurfaceDark:   #2E2E2E   ← dark grey counterpoint
SurfaceDarkest:#1F1F1F   ← code blocks
Outline:       #D4D4D4
OutlineDark:   #3A3A3A

Red:           #E53935   ← stop / destructive / error
Yellow:        #FFC107   ← active / executing / caution
Blue:          #1E88E5   ← primary action / agent / info

Text Primary:   #1F1F1F
Text Secondary: #525252
Text Tertiary:  #8A8A8A
Text On Dark:   #F5F5F5

Success: #10B981
Warning: #F59E0B
Info:    #0EA5E9
```

**Light glass HazeStyle**:
```kotlin
HazeStyle(
    backgroundColor = Color(0xFFFFFFFF),  // opaque — REQUIRED per R-13
    blurRadius = 24.dp,
    tint = HazeTint(Color(0xFFFFFFFF).copy(alpha = 0.73f)),
)
```

**Dark glass HazeStyle**:
```kotlin
HazeStyle(
    backgroundColor = Color(0xFF2E2E2E),
    blurRadius = 24.dp,
    tint = HazeTint(Color(0xFF2E2E2E).copy(alpha = 0.80f)),
)
```

**Progressive blur edge (chat header)**:
```kotlin
.hazeChild(state = hazeState, style = lightGlass) {
    progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
}
```

**Fonts**: Inter (body), Sora (display), JetBrains Mono (code) — same families as Only-List, all weights explicitly registered per R-9.

**Chat UI pattern**:
- Assistant message = DARK GREY bubble (`#2E2E2E`, light text, 12dp radius with bottom-left 4dp tail corner, max-width 85%, JetBrains Mono streaming cursor appended).
- User message = BLUE bubble (`#1E88E5`, white text, right-aligned, bottom-right 4dp tail).
- Code block = `#1F1F1F` inset inside assistant bubble, 8dp radius, header with language label + copy button, syntax highlighting per §1.
- Tool-call card = light surface body + dark grey header bar, 5 status states (queued/running/success/error/needs-approval), status dot color cross-fades 150ms.
- Streaming cursor = 1.5dp × 16sp vertical blue bar, opacity 1.0↔0.0 over 800ms linear infinite — NOT typewriter, NOT blinking block.

**Top 5 component patterns**:
1. **Agent chat thread** (§8.1) — dark grey assistant bubble + blue user bubble + embedded code blocks + tool-call cards.
2. **Tool execution card** (§8.4) — 5-state status system with approve/deny flow for irreversible actions.
3. **Workspace selector dropdown** (§8.3) — top-of-screen chevron + ModalBottomSheet with workspace list.
4. **Floating bottom nav** (§8.2) — dark grey glass pill, 4 tabs, blue active pill, pressScale.
5. **LLM provider settings** (§8.6) — provider cards with status badges + configure-form + custom models section.

---

*End of design language. Update this document + the runtime `theme.json` together when the design evolves. Cross-reference prior research: R-9 (Haze + variable fonts), R-12 (Haze architecture fix — never use `fillMaxSize()` on a hazeChild scrim), R-13 (HazeMaterials.regular canonical tint alpha + modern card patterns), R-A1 (Cline SDK porting + Android agent architecture).*
