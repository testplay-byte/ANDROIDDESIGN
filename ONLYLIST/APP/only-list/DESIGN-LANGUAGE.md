# Design Language — Only-List

> Canonical design spec. The active `theme.json` is its runtime representation.
> Informed by `research/R-5-anikuta-design.md` (ANI-KUTA patterns) + `research/R-7-modern-android-design.md` (modern trends).
> Follow strictly. Update this + `theme.json` together when the design evolves.

## Theme: Midnight Coral

Dark-first, warm-dark backgrounds, single coral accent, translucent layered surfaces. Minimalistic. **NOT Material Design.**

---

## 1. Color Palette

### Backgrounds (warm dark)
| Role | Hex | Use |
|------|-----|-----|
| `background` | `#14110F` | App background |
| `surface` | `#1C1815` | Cards, sheets, bottom nav base |
| `surfaceVariant` | `#241F1B` | Elevated cards, dialogs |
| `surfaceHighest` | `#2E2823` | Pressed states, modals |
| `outline` | `#3A322C` | Borders, dividers |

### Coral accent (single accent — use sparingly)
| Role | Hex | Use |
|------|-----|-----|
| `primary` | `#FF6B5C` | Active states, key actions, focus |
| `primaryHover` | `#FF8A7C` | Hover/pressed-light |
| `primaryPressed` | `#E55648` | Pressed |
| `primaryMuted` | `#3A2420` | Accent backgrounds (pills, chips, selected) |
| `onPrimary` | `#1A0B08` | Text/icon on coral |

### Text (warm off-white — NOT pure white, avoids halo anti-alias)
| Role | Hex | Contrast | Use |
|------|-----|----------|-----|
| `textPrimary` | `#F5EFE9` | 15.2:1 ✅ | Headlines, body |
| `textSecondary` | `#B5A89D` | 8.1:1 ✅ | Subtitles, metadata |
| `textTertiary` | `#8A7E72` | 4.6:1 ✅ | Timestamps, hints |
| `textDisabled` | `#5A5249` | — | Disabled |

### Semantic (NOT tonal-derived from coral)
| Role | Hex | Use |
|------|-----|-----|
| `success` | `#6FCF97` | Completed, sync OK |
| `warning` | `#F2C94C` | Paused, airing-soon |
| `error` | `#EB5757` | Error, dropped |
| `info` | `#56CCF2` | Info banners |

### List status colors
| Status | Color |
|--------|-------|
| CURRENT (watching) | `primary` coral |
| COMPLETED | `success` green |
| PAUSED | `warning` amber |
| DROPPED | `error` red |
| PLANNING | `info` blue |
| REPEATING | `#BB6BD9` purple |

---

## 2. Typography

### Font families (bundled `res/font/`, all OFL)
| Token | Family | Weights | Use |
|-------|--------|---------|-----|
| `body` | **Inter** | 400, 500, 600 | Body, UI text |
| `display` | **Sora** | 600, 700, 800 | Headlines, screen titles |
| `mono` | **JetBrains Mono** | 400, 500 | Numbers, tabular data |

**Bundle ALL weights.** Past bold-rendering issues came from missing weight files. `FontRegistry` maps token + weight → correct `FontFamily` + `Font` resource. Variable fonts preferred.

### Type scale
| Role | Font | Size | Weight | Line height |
|------|------|------|--------|------------|
| `displayLarge` | Sora | 30sp | 700 | 36sp |
| `displayMedium` | Sora | 24sp | 700 | 30sp |
| `headingLarge` | Sora | 20sp | 600 | 26sp |
| `titleLarge` | Inter | 18sp | 600 | 24sp |
| `titleMedium` | Inter | 16sp | 500 | 22sp |
| `bodyLarge` | Inter | 15sp | 400 | 22sp |
| `bodyMedium` | Inter | 14sp | 400 | 20sp |
| `bodySmall` | Inter | 13sp | 400 | 18sp |
| `caption` | Inter | 12sp | 500 | 16sp |
| `numberMedium` | JetBrains Mono | 14sp | 500 | 18sp (tabular) |
| `numberLarge` | JetBrains Mono | 22sp | 600 | 26sp (tabular) |

---

## 3. Shapes

| Role | Radius | Use |
|------|--------|-----|
| `small` | 4dp | Chips, tags, small buttons |
| `medium` | 8dp | Buttons, inputs, list items |
| `large` | 12dp | Cards |
| `xlarge` | 20dp | Sheets, dialogs |
| `pill` | 28dp | Bottom nav, FAB, segmented control |

---

## 4. Motion

### Durations
| Token | ms | Use |
|-------|----|-----|
| `instant` | 50 | Press feedback start |
| `quick` | 150 | Color cross-fade, icon scale |
| `short` | 220 | Label reveal, spring start |
| `medium` | 300 | Screen transition, expand/collapse |
| `long` | 450 | Theme cross-fade |

### Easings
| Token | Spec | Use |
|-------|------|-----|
| `standard` | `FastOutSlowInEasing` | Most UI |
| `standardDecel` | `LinearOutSlowInEasing` | Enter |
| `standardAccelerate` | `FastOutLinearInEasing` | Exit |
| `springDefault` | Spring(dampingRatio=0.7, stiffness=380) | Tap feedback, indicator slide |
| `springBouncy` | Spring(dampingRatio=0.6, stiffness=300) | Label reveal (slight overshoot) |

---

## 5. Elevation & Depth

NOT Material elevation. Depth via:
- **Translucent layers**: bottom nav `#1C1815` at 88% opacity + 24dp blur. Header scrim 0→0.55 alpha on scroll.
- **Subtle borders**: `outline` (`#3A322C`) 0.5-1dp on cards (not shadows).
- **Scrim stack**: background → surface → surfaceVariant.

---

## 6. Spacing

| Token | dp |
|-------|----|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 12 |
| `lg` | 16 |
| `xl` | 24 |
| `xxl` | 32 |
| `xxxl` | 48 |

Use these tokens (NOT raw `dp`). Formal `Spacing` object in `:core:designsystem`.

---

## 7. Component Patterns

### 7.1 Bottom navigation bar (floating pill + animated label reveal)
- Floating pill overlay (NOT in Scaffold.bottomBar). Centered, 16dp side margin, 8dp bottom margin (safe area).
- Pill: 28dp radius, `surface` 88% opacity + 24dp blur, 8dp shadow.
- Height: 58dp outer, 42dp pill content.
- **Inactive**: icon-only, `textTertiary`, `weight(1f)`, 10dp padding.
- **Active**: content-sized (no `weight`), `primaryMuted` bg, 14dp padding, icon + label (`AnimatedVisibility`).
- **Label reveal**: `expandHorizontally(220ms, springBouncy) + fadeIn(150ms)`. Exit: `fadeOut(100ms) + shrinkHorizontally(150ms)`.
- **Bg cross-fade**: 300ms `standard`. Text color: 150ms.
- **Press**: scale 0.95 via `graphicsLayer` (deferred, no recomposition), 150ms, `indication = null` (NO ripple), light haptic.
- **Active indicator**: 4dp × 24dp coral pill at top, slides horizontally (`springDefault`, 240ms).
- **5 tabs**: Home, Search, Airing, Library, Settings. (Details is navigated, not a tab.)

### 7.2 Collapsible header with scroll-blur (gradient scrim, NOT RenderEffect)
- Header pinned OUTSIDE the scroll container.
- Large title: `displayLarge` (30sp Sora 700) at scroll=0 → `titleLarge` (18sp Inter 600) at scroll>20dp. `animateFloatAsState(tween(300ms, standard))`.
- Padding: top 8→2dp, bottom 4→0dp as it collapses.
- **Blur scrim**: 36dp-tall, 7-stop vertical gradient (`surface` solid → transparent) with `graphicsLayer { alpha = smoothstep(scroll / 24dp) }` where smoothstep = `t² × (3 - 2t)`.
- `-2dp translationY` overlap.
- Sharp top, 24dp-rounded bottom.
- **Draw-phase only** — zero recomposition, 60fps.
- LazyList guard: `if (firstVisibleItemIndex > 0) Float.MAX_VALUE`.
- **Progressive blur 40%–70%** (per R-7): blur 0→24dp ramped 40%-70% scroll (avoids twitch on first micro-scroll).

### 7.3 Cards (opaque, bordered — NOT glassmorphism)
- `surface` bg 100%. `outline` border 0.5dp. `large` (12dp) radius.
- Press: scale 0.98 + `primaryMuted` bg tint 50ms.

### 7.4 Press feedback (every tappable)
`Modifier.pressScale()` reusable extension:
- Scale 1.0 → 0.96 → 1.02 → 1.0 over 150ms `standardDecel`.
- `graphicsLayer { scaleX = scale; scaleY = scale }` (deferred, no recomposition).
- `indication = null` (no ripple — we do our own feedback).
- Light haptic on press-down.

### 7.5 3-way segmented control
- `pill` (28dp) container, `surfaceVariant` bg, 4dp padding.
- 3 equal-weight segments. Active: `primaryMuted` bg pill, `primary` text. Inactive: `textTertiary`.
- **Sliding indicator**: coral pill behind active, slides horizontally (`springDefault`, 240ms).
- Tap: `pressScale` + haptic.
- Use for: Anime/Manga toggle (Search, Library), status filters, sort options.

### 7.6 Loading states (skeleton + shimmer, never spinners)
- Skeleton: `surfaceVariant` block matching final shape.
- Shimmer: gradient sweep `surfaceVariant` → `surfaceHighest` → `surfaceVariant`, 1200ms loop, `LinearEasing`.
- **Cache-first**: if local data exists, show it immediately (no skeleton). Skeleton ONLY on first-ever empty-cache load.

### 7.7 List items (anime/manga rows)
- Cover: 64×90dp (2:3), `medium` radius, `outline` border.
- Title: `titleMedium`, 2 lines, ellipsis.
- Subtitle: `bodySmall` `textSecondary` — status + progress + score.
- Status dot: 8dp, status color.
- Right: `numberMedium` score/progress.
- Press: `pressScale` + `surfaceHighest` tint.

---

## 8. Screen Layout

Each screen:
1. **Collapsible header** (§7.2) with screen title.
2. **Content**: LazyColumn/VerticalStaggeredGrid. 16dp horizontal padding, 8dp top (below header), 80dp bottom (clear floating nav).
3. **Floating bottom nav** (§7.1) overlays content.

---

## 9. Anti-Patterns (do NOT use)

- ❌ Material You dynamic color (respect the chosen coral).
- ❌ Glassmorphism on cards (only nav + header).
- ❌ Neumorphism. Aurora/gradient backgrounds. Gradient text. Purple-violet "AI slop" gradients.
- ❌ Pure black (`#000000`) — use warm dark `#14110F`.
- ❌ Pure white (`#FFFFFF`) text — use `#F5EFE9`.
- ❌ Over-rounded everything (mix radii per §3).
- ❌ Confirmation dialogs for reversible actions (use undo toast).
- ❌ Spinners when local data exists (cache-first + skeletons).
