# R-4 — Android Design System + Architecture Research

> Research sub-agent report for the **ONLYLIST** project
> (AI-driven, infinitely-customizable design system; anime/manga tracker as test-bed).
> Authored: Phase 0 (Setup / Planning / Research). NOT application code.
> Authoritative sources: developer.android.com, androidx docs, Apollo Kotlin docs,
> Coil docs, Koin/Hilt docs, Gradle docs, Reddit/ProAndroidDev community signals (2024-2026).
> Where claims could not be verified against primary docs they are marked **(unverified)**.

## 0. TL;DR — Decisions at a glance

| Area | Decision | Confidence |
|---|---|---|
| Design-system substrate | Custom `AppTheme` composable + own `LocalColors/Typography/Shapes/Motion/Spacing`; do NOT use `MaterialTheme` as the mechanism | High |
| Design tokens | Kotlin serializable data class ↔ JSON; one `theme.json` file is the AI-editable surface | High |
| Palette | `androidx.palette` (kmpalette for KMP) for extraction; HSL-shift tonal palette generation (NOT Material You HCT — too complex for v1) | High |
| Fonts | Bundle as `.ttf` variable fonts in `res/font`; starter set **Inter** (UI) + **Sora** or **Space Grotesk** (display), all OFL | High |
| DB | **Room** (KMP-capable, annotation-driven, AI-friendly, type-safe) over SQLDelight | Medium-High |
| Image cache | **Coil 3** (Compose-native, KMP) | High |
| DI | **Koin 4** (KMP-friendly, runtime but simple, easiest for an LLM to write correctly) | Medium-High |
| Navigation | **Navigation Compose** (stable, well-documented). Avoid Nav3 (still 1.0-alpha, prior project removed it). Voyager only if KMP is required sooner | High |
| GraphQL | **Apollo Kotlin** with normalized cache (SQLite-backed) | High |
| REST | **Ktor 3** client (KMP) | High |
| Charts | **Vico** (line/bar/column/stacked) + **custom Compose Canvas** for radar/spider | High |
| Backup | Single zip = Room DB file + DataStore prefs + `theme.json` + encrypted AniList token; one rolling copy; weekly via WorkManager | High |
| ABIs | `arm64-v8a`, `armeabi-v7a`, `x86_64` via convention plugin `abiFilters`; ship AAB (Play auto-splits) + universal APK for sideload/emulator | High |
| SDK | `compileSdk=36`, `targetSdk=36`, `minSdk=26` | High |
| Build | GitHub Actions only; `actions/setup-java` + `gradle/actions/setup-gradle`; cache Gradle + Konan + AVD | High |
| Crash | Global `Thread.setDefaultUncaughtExceptionHandler` → persist → `ErrorActivity` (still good practice in 2026) | High |
| Agent | `:core:agent` multi-module: `core` (loop), `llm` (provider abstraction), `tools` (registry), `permissions` (approval); tool surface ~12 tools; git-style snapshot rollback | High |

---

## A. Design system / theming (NON-Material)

### A1. Custom design system in Compose — MaterialTheme vs. fully custom

**What `MaterialTheme` gives you for free:**
- `LocalColorScheme`, `LocalTypography`, `LocalShapes`, `LocalRippleConfiguration`, `LocalContentColor`, `LocalIndication`, `LocalElevationOverlay`, `LocalAbsoluteTonalElevation` — all `CompositionLocal`s pre-wired.
- Pre-built components (`Button`, `Card`, `TopAppBar`, `Scaffold`, `ModalBottomSheet`, `DatePicker`, `Slider`, …) that read those locals. ~50 production-quality components you do not have to write.
- Predictable accessibility defaults (`LocalContentColor` contrast, focus indicators, minimum touch target 48dp via `Modifier.minimumInteractiveComponentSize`).
- A well-tuned ripple (`RippleConfiguration`) + press/elevation/disabled state handling.

**What we'd have to rebuild if we go fully custom:**
- A `Button`-equivalent (clickable surface with press/ripple/disabled). At minimum ~150 LOC for a polished one.
- A `Scaffold`/`TopAppBar`/`Snackbar` triad — but we don't *need* Material's; we want our own.
- A ripple implementation (or reuse `androidx.compose.foundation`'s `Ripple` directly — it is decoupled from MaterialTheme via `LocalRippleConfiguration`).
- Touch-target enforcement, focus visuals, semantics.

**Three viable approaches:**

| Approach | Pros | Cons | Verdict |
|---|---|---|---|
| **(a) Use `MaterialTheme` as mechanism, supply non-Material token set** | Reuse all M3 components for free; minimal work to ship v1. | The `ColorScheme` type has 29 fixed color roles (primary, onPrimary, primaryContainer, …) — Material-specific. "Non-Material" only by changing values, not structure. LLM agent constrained to M3's vocabulary. Risk of looking Material-ish by default. | ❌ Violates user's "NOT Material" intent. |
| **(b) Skip `MaterialTheme`, build a custom `AppTheme` with own CompositionLocals** | Full token vocabulary freedom (e.g. `surface`, `surfaceRaised`, `accent`, `onAccent`, `scrim`, `success`…). AI edits *our* schema, not Material's. Forces us to build a small component lib (good — full control). | Have to write ~15-25 base components ourselves. Need to wire `LocalRippleConfiguration` if we want ripples on custom `clickable`s (or just use `Modifier.clickable` with `LocalIndication`). | ✅ **Recommended.** |
| **(c) Hybrid: use Material for low-level primitives (`Ripple`, ` indication`, `Surface` color-aware) but wrap them in our own components and our own `AppTheme`.** | We get the ripple + elevation + overlay plumbing from `androidx.compose.foundation` / `material3.*Surface` without committing to Material's component shapes. | Requires careful import discipline so LLM agents don't accidentally import `androidx.compose.material3.Button`. | ✅ Acceptable refinement of (b). |

**Recommendation: Approach (c).** Build a custom `AppTheme { }` composable that:
1. Defines its own `LocalAppColors`, `LocalAppTypography`, `LocalAppShapes`, `LocalAppMotion`, `LocalAppSpacing`, `LocalAppElevation` (six `CompositionLocal`s — six is the sweet spot, not the kitchen sink).
2. Internally calls `MaterialTheme(colorScheme = derivedFromAppColors, typography = derivedFromAppTypography, shapes = derivedFromAppShapes) { }` **only** so that low-level Material primitives we reuse (e.g. `androidx.compose.material3.Surface` for elevation overlay, `Ripple` indication) keep working. The public API for our app is `AppTheme`, not `MaterialTheme`.
3. Exports a **base component library** (`AppButton`, `AppCard`, `AppTopBar`, `AppBottomNav`, `AppSheet`, `AppField`, `AppSwitch`, `AppChip`, `AppDivider`, `AppProgress`) that reads *our* locals.

This satisfies "NOT Material Design" at the visual + token level, while not throwing away working plumbing. The LLM agent edits our token JSON, never Material's.

**Why not pure (b)?** Pure (b) means re-implementing ripple + elevation overlay + `Surface` color blending. That is ~1-2 weeks of polish work for no real benefit; the foundation primitives in `androidx.compose.foundation` and `androidx.compose.material3` (the `Surface`/`Ripple`/`Indication` ones, not the Button-shaped ones) are not "Material Design" — they're the framework's primitives.

### A2. Design tokens as data — recommended schema

The token file is the AI agent's primary edit surface. It must be:
- **Serializable** to JSON (kotlinx.serialization) for human + LLM editing, diffing, backup.
- **Versioned** (schema version field for forward compat).
- **Role-based** (semantic roles, not raw colors), so swapping a palette re-themes everything.
- **Stable across renames** — never reuse a role name for a different meaning.

**Recommended schema (one `theme.json`):**

```jsonc
{
  "$schema": "1.0",
  "name": "Aurora (default)",
  "dark": true,
  "colors": {
    "background":      "#0F1115",
    "surface":         "#161A22",
    "surfaceRaised":   "#1E232E",
    "surfaceHighest":  "#262C39",
    "scrim":           "#000000CC",
    "primary":         "#7C5CFF",
    "onPrimary":       "#FFFFFF",
    "accent":          "#22D3EE",
    "onAccent":        "#00131A",
    "success":         "#34D399",
    "warning":         "#FBBF24",
    "error":           "#F87171",
    "onBackground":     "#E6E8EE",
    "onSurface":       "#D6DAE3",
    "onSurfaceMuted":  "#8A92A3",
    "outline":         "#2A3140",
    "outlineStrong":   "#3A4252"
  },
  "typography": {
    "family":          "inter",          // key into res/font map
    "familyDisplay":   "sora",
    "scale": {
      "display":  { "size": 36, "weight": 700, "lineHeight": 44, "tracking": -0.5 },
      "title":    { "size": 22, "weight": 600, "lineHeight": 28, "tracking": 0 },
      "body":     { "size": 15, "weight": 400, "lineHeight": 22, "tracking": 0.1 },
      "label":    { "size": 13, "weight": 600, "lineHeight": 18, "tracking": 0.4 },
      "caption":  { "size": 11, "weight": 500, "lineHeight": 14, "tracking": 0.5 },
      "mono":     { "size": 13, "weight": 500, "family": "jetbrains_mono", "lineHeight": 18, "tracking": 0 }
    }
  },
  "shapes": {
    "cornerNone":    0,
    "cornerSmall":   8,
    "cornerMedium":  14,
    "cornerLarge":   20,
    "cornerFull":    9999,
    "cardShape":     "cornerLarge",
    "sheetShape":    "cornerLarge",
    "buttonShape":   "cornerMedium",
    "fieldShape":    "cornerMedium",
    "chipShape":     "cornerFull"
  },
  "motion": {
    "durations":  { "micro": 90, "short": 160, "medium": 240, "long": 360, "xlong": 540 },
    "easings":    {
      "emphasized":       "cubic-bezier(0.2,0.0,0,1.0)",
      "emphasizedDecel":  "cubic-bezier(0.05,0.7,0.1,1.0)",
      "emphasizedAccel":  "cubic-bezier(0.3,0.0,0.8,0.15)",
      "standard":         "cubic-bezier(0.2,0.0,0,1.0)"
    }
  },
  "elevation": { "level0": 0, "level1": 1, "level2": 3, "level3": 6, "level4": 8, "level5": 12 },
  "spacing":   { "xxs": 2, "xs": 4, "sm": 8, "md": 12, "lg": 16, "xl": 24, "xxl": 32, "xxxl": 48 },
  "componentVariants": {
    "button":   "filled",   // filled | tonal | outline | text
    "card":     "elevated", // flat | elevated | outlined
    "navStyle": "pill"      // pill | bar | rail
  },
  "image": {
    "placeholderColor": "surfaceRaised",
    "errorColor":       "error",
    "cornerRadius":     "cornerMedium"
  }
}
```

**Implementation:**
- `DesignTokens` data class (kotlinx.serialization, `@SerialName` per field).
- `ThemeRepository` exposes `Flow<DesignTokens>`.
- A `DesignTokensResolver` maps tokens → Compose `AppColors` / `AppTypography` / etc. (resolves role-name references like `"surfaceRaised"` to actual hex, parses cubic-bezier strings to `Easing`).
- Validation on load: every `*Shape` reference must exist in `shapes`; every color reference in `image` must exist in `colors`. On invalid theme → fall back to bundled default + log a WARN.

**Why this shape (not flatter, not deeper):**
- Flat enough that the LLM agent can edit a single field (`colors.primary`) without touching nested arrays.
- Deep enough that role groups stay separable (`motion.durations.short` is discoverable; `motionShort` would not be).
- Schema-versioned (`$schema`) so we can evolve without breaking user backups.

### A3. Runtime theme switching + persistence

**Switching mechanism:**
- `ThemeRepository` (in `:core:designsystem`) holds a `MutableStateFlow<DesignTokens>` initialized from DataStore on app start.
- `AppTheme` composable: `val tokens by themeRepository.tokens.collectAsState()` → resolves → provides via the six `CompositionLocal`s.
- Token change → `StateFlow` emits → `collectAsState` recomposes → all consumers of `LocalAppColors.current` re-read. Compose's snapshot system handles this efficiently; no manual invalidation needed.
- Sub-100ms recomposition for token changes is achievable if we don't bloat the tree (avoid reading tokens in tight `LazyColumn` item lambdas — read at the screen level and pass down).

**Persistence (DataStore Preferences, not Proto):**
- Store only the **active theme id** + a list of saved theme ids in `preferencesDataStore`.
- The themes themselves (token JSON) live as files in `filesDir/themes/<id>.json` — easier to back up, easier for the agent to edit on disk, easier to diff.
- Proto DataStore is overkill here (we don't need schema-evolved protobufs; we have kotlinx.serialization + a `$schema` field).

**Live preview path for the Design Studio screen:**
- Design Studio screen binds to a **preview** `StateFlow<DesignTokens>` that the agent's tools mutate in memory.
- "Apply" copies preview → committed (writes the JSON file + bumps `ThemeRepository.tokens`).
- "Revert" discards preview (re-loads committed).

### A4. Dynamic theming from an image

**Palette API (recommended):**
- `androidx.palette:palette-ktx` extracts up to 16 swatches from a `Bitmap`. Each swatch has RGB, population (pixel count), and HSL.
- Predicates: `DominantSwatch`, `VibrantSwatch`, `MutedSwatch`, `LightVibrantSwatch`, `DarkVibrantSwatch`, `LightMutedSwatch`, `DarkMutedSwatch`.
- For KMP (or to avoid the platform Palette impl): **`jordond/kmpalette`** is a drop-in Compose Multiplatform port that does the same quantization. Recommended if we want KMP later; for Android-only v1 the standard Palette API is fine.

**Palette's limitations:**
- 16 swatches max — fine.
- No semantic mapping (Palette gives you "vibrant" but not "primary"; *you* decide primary = vibrant).
- No tonal palette generation (Palette gives one shade per predicate; you don't get a 0–100 tone scale like Material You does).
- Bitmap-only input — must decode the image first (cheap with Coil's `ImageLoader.execute`).

**Recommended role mapping (default):**
| Token role | Source |
|---|---|
| `surface` | DominantSwatch (darkened 30%) |
| `background` | DominantSwatch (darkened 50%) |
| `surfaceRaised` | DominantSwatch (darkened 15%) |
| `primary` | VibrantSwatch or DarkVibrantSwatch (fallback: DominantSwatch) |
| `accent` | LightVibrantSwatch or MutedSwatch (fallback: VibrantSwatch rotated +60° hue) |
| `onBackground` / `onSurface` | Computed contrast (white or black) |
| `success` / `warning` / `error` | Keep the defaults (don't derive from image — they have semantic meaning) |
| `outline` | surfaceRaised lightened 10% |

**Tonal palettes: HCT vs. HSL shift.**

Material You uses **HCT** (Hue-Chroma-Tone, Google's color space, `androidx.compose.material3.colorscheme` uses it internally via `dynamicColorScheme`). HCT produces perceptually-uniform tints. Implementing HCT from scratch is ~600 LOC + a lot of color science.

**Recommendation: HSL shifts for v1.** Build a tiny tonal-palette generator:
- For each accent color, generate 5 tones (T0=very dark, T25, T50=base, T75, T100=very light) by adjusting HSL L (and slightly desaturating as L→0 or L→100).
- Not as perceptually uniform as HCT, but good enough that the eye accepts the result, and trivially editable by the AI agent later.
- Provide an upgrade path: ship behind an interface `TonalPaletteGenerator` with two impls (`HslTonalPaletteGenerator` default, `HctTonalPaletteGenerator` if we ever pull in the Material color science lib). Per CORE_RULES §5, an interface-with-one-impl is OK when a future swap is explicitly planned.

**Custom quantizer?** Not needed. Palette's median-cut quantizer is fine. If we need exact-palette extraction (e.g. an 8-color posterized look) we'd build one, but that's a v2 feature.

### A5. Bundled fonts

**Mechanism:** `res/font/<font>.ttf` (or `.otf`), loaded via:
```kotlin
val interFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
```
For variable fonts, single `R.font.inter_variable` file + `Font(..., variationSettings = listOf(FontVariation.Settings(...)))`.

**Confirmed via Android Developers' "Work with fonts" doc:** `FontFamily` + `R.font.*` is the canonical approach; bundling (vs. downloadable via `GoogleFonts` contract) avoids the "font not found at runtime" issue (no network dependency, no provider contract failure).

**Recommended starter set (all OFL-licensed):**
| Role | Font | Why |
|---|---|---|
| UI body / default | **Inter** | Industry-standard UI sans; OFL; variable version available; renders crisply at 13–15sp. |
| Display / titles | **Sora** (or Space Grotesk) | Geometric display sans, OFL, distinctive but legible. Pairs with Inter. |
| Monospace | **JetBrains Mono** | OFL; great for the Design Studio "token diff" view and code-ish UI. |
| Optional CJK fallback | **Noto Sans CJK** (subset) | For anime/manga titles in JP/CN. OFL. Note: full Noto Sans CJK is large (~16MB per weight) — bundle only Regular weight, or use the system fallback for CJK and only force Latin. |

**License considerations:**
- All four are **OFL 1.1 (SIL Open Font License)** — free to bundle, redistribute, and modify (with rename). No attribution prompt required in-app.
- DO NOT bundle fonts under non-OFL licenses (e.g. some Helvetica clones) — that's a legal minefield.
- Ship a `THIRD_PARTY_NOTICES.md` listing each font + license URL.

**Making the font family part of the token:**
- `theme.json.typography.family` is a **string key** (`"inter"`, `"sora"`, `"jetbrains_mono"`).
- A `FontRegistry` maps keys → `FontFamily` instances (resolved at app init from `R.font.*`).
- The AI agent can swap the family by editing `family: "sora"` → `family: "inter"` without touching code.
- Future: support loading a `.ttf` from app storage (user-supplied font) — `FontFamily(File(path))` is supported. Out of scope for v1.

### A6. Animations — Compose APIs to master

| API | Use for |
|---|---|
| `animate*AsState` (`animateColorAsState`, `animateDpAsState`, `animateFloatAsState`) | Single-value state-driven animations (color on toggle, dp on elevation change). |
| `AnimatedVisibility` | Enter/exit of views (sheet, banner, FAB hide-on-scroll). |
| `AnimatedContent` | Swap content with a transition (e.g. switch tab content with fade+slide). |
| `Crossfade` | Cheap fade between two states (loading → content). |
| `Modifier.animateContentSize()` | Container grows/shrinks smoothly (expand card, open details inline). |
| `Modifier.animateItemPlacement()` (LazyItemScope) | Reorder/insert/remove animations in LazyColumn — critical for library list. |
| `SharedTransitionLayout` + `Modifier.sharedElement()` | Hero animations: poster on Home → expanded on Details. Stable since Compose 1.7. |
| `rememberInfiniteTransition` | Loops (shimmer, breathing accent, loading dots). |
| `Animatable` | Imperatively driven values (custom gesture-driven progress, drag-to-dismiss with spring). |
| `updateTransition` | Multi-target coordinated animation (tab switch animating 4 properties together). |
| `PointerInputScope.detectDragGestures` + `Animatable` | Custom gesture-driven (sheet drag, swipe-to-dismiss). |
| `AnimatedNavHost` (via `Navigation Compose`) enterTransition/exitTransition | Screen transitions (fade-through, slide-horizontal). |
| `LocalSharedTransitionScope` | For shared elements across nav destinations (more advanced). |

**60fps guardrails:**
1. **No heavy work on the main thread** during animation. Image decode, JSON parsing, DB writes → `Dispatchers.IO` or `withContext`. Profile with the **Layout Inspector → Compose → Recomposition Counts** and Android Studio's Profiler.
2. **Avoid overdraw** — don't stack opaque backgrounds. Use `Modifier.drawBehind` for one-off backgrounds instead of `Box { Canvas {} }`.
3. **`derivedStateOf`** to avoid recomposing large subtrees when only a derived bit changes. Example: `val showFab by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 } }`.
4. **`@Stable` / `@Immutable`** on token classes — critical: if `AppColors` is unstable, *every* color token change recomposes the entire UI tree as if it all changed. Mark `AppColors`, `AppTypography`, `AppShapes` etc. as `@Immutable` (they're data classes of vals).
5. **Defer reads** — `Modifier.alpha(if (state) 1f else 0f)` recomposes on state change; `Modifier.graphicsLayer { alpha = if (state) 1f else 0f }` defers to draw phase (no recomposition). Use graphicsLayer for any animation that runs every frame (scroll-driven, infinite).
6. **`key()` in LazyColumn items** so Compose knows what changed.
7. **Baseline profile** — ship one (`:app:baselineprofile`). ~30% cold-start + scroll perf uplift. Recommended for a buttery-smooth bar.
8. **Test on a low-end real device** (not just emulator) — the rule from CORE_RULES §22 still applies.
9. **Disable `showLayoutBounds` and `R8 fullMode=false` for release perf testing.**
10. **`Choreographer` callback for any custom Canvas-driven chart** so animation ticks are frame-aligned.

**Navigation transitions:** use `fadeThrough()` (alpha + slight scale) for tab switches, `slideInHorizontally` for push, `slideInVertically` for sheet-style screens. Avoid `SharedTransitionLayout` across nav boundaries until v1.1 — it's powerful but fiddly to get right under Nav Compose.

### A7. Custom components over Material

**Decision:** Build our own base component library. Do NOT wrap `androidx.compose.material3.Button` etc. — those bake in Material's shape/elevation/ripple behavior we want to override.

**Base component library (`:core:designsystem:components`):**
| Component | Built on | Notes |
|---|---|---|
| `AppButton` | `Box + Modifier.clickable` (with `LocalIndication` ripple) | Variants: `filled`, `tonal`, `outline`, `text`. Sizes: `sm`, `md`, `lg`. |
| `AppIconButton` | `Box + clickable` | Min 44dp touch target (smaller than Material's 48 — design choice). |
| `AppCard` | `Box + clip + background(color, shape)` | Variants: `flat`, `elevated`, `outlined`. Elevation uses `Modifier.shadow` (which is just framework `graphicsLayer`, not Material). |
| `AppTopBar` | `Row + Modifier` | Not `Scaffold`'s `TopAppBar`. Supports scroll blur (`Modifier.graphicsLayer` alpha on background). |
| `AppBottomNav` (pill style) | `Row + AnimatedVisibility` for labels | The "floating pill nav" from the old project's design language. |
| `AppSheet` (bottom sheet) | `ModalBottomSheet` from `material3` IS OK to use here — it's a sheet primitive, not a styled component. Wrap it with our colors + shapes. | One of the few M3 components worth reusing because the gesture handling is hard to redo. |
| `AppField` | `BasicTextField` (from `foundation.text`) + our own decoration | Avoid `OutlinedTextField` (Material-styled). |
| `AppSwitch` | Custom `Box + Modifier.pointerInput + animateColorAsState` | Material's `Switch` is too opinionated (thumb shadow, track shape). |
| `AppChip` | `Box + clickable` | Variants: `assist`, `filter`, `input`. |
| `AppProgress` | `Canvas + rememberInfiniteTransition` (linear + circular) | Custom, matches our token motion. |
| `AppDivider` | `Box + background + height` | Trivial. |
| `AppScaffold` | `Box` with slot APIs for topBar/bottomBar/snackbar | NOT Material's `Scaffold` — we want full control. We DO reuse `SnackbarHost` because it's gesture-correct. |

**Rule:** any `import androidx.compose.material3.*` must be reviewed. Allowed: `Surface` (for color blending), `ModalBottomSheet`/`SheetState` (gesture primitive), `Ripple`/`LocalRippleConfiguration`, `SnackbarHost`/`Snackbar`. Disallowed: `Button`, `Card`, `TopAppBar`, `Scaffold`, `OutlinedTextField`, `Switch`, `Chip`, `NavigationBar`, `Slider`, `AlertDialog`. Lint rule (custom Detekt check or a simple `kotlin.text.Regex` in CI) can enforce this.

**Why build instead of wrap:** Wrapping means every Material update to those components leaks into our UI (their ripple physics changes, their default shapes change, their a11y behavior changes). Building on `foundation` primitives is more stable across Compose versions.

---

## B. Offline-first data layer

### B8. Room vs SQLDelight — recommendation

**Fair comparison for this project (Android-first, possibly KMP later, Compose, AI-agent-friendly):**

| Axis | Room | SQLDelight |
|---|---|---|
| **Maturity / official status** | Jetpack library, Google-recommended, very mature. | Cash App / Square, mature, third-party but well-maintained. |
| **Kotlin Multiplatform** | Yes — Room KMP stable since 2.7.0 (mid-2024); works on iOS, JVM, Android. | Native KMP since inception; longer track record on iOS. |
| **Compose ergonomics** | Excellent — `Flow<List<T>>` from DAOs collected directly in composables. | Excellent — same, via `asFlow()`. |
| **Type safety** | Compile-time checked via annotation processor (KSP). Schema is in Kotlin. | Compile-time checked via Gradle plugin. Schema is in `.sq` files (SQL with Kotlin type extensions). |
| **Migration tooling** | `Migration` classes, `AutoMigration`, fallbackToDestructiveMigration. Mature. | `.sqm` files. Decent but less automation. |
| **Learning curve** | Lower — most Android devs know it; tons of Stack Overflow + docs. | Moderate — SQL-first mindset; less common knowledge. |
| **AI-agent-friendliness** | ✅ **Higher.** LLMs are trained on far more Room code than SQLDelight. Annotations are discoverable. Errors are clear (KSP). | ⚠️ Lower. LLMs hallucinate SQLDelight syntax more often. `.sq` file layout has multiple conventions. |
| **Schema-as-data (for backup)** | DB file is standard SQLite — backup is a file copy. | Same — standard SQLite file. |
| **KMP future-proofing** | Good (Room KMP works); but Room's type converters are Android-leaning. | Excellent — first-class KMP. |
| **Community signal (2025)** | Multiple "Why I switched from SQLDelight to Room in KMP" posts (LinkedIn, Medium). ProAndroidDev Jan 2025 article recommends Room for KMP beginners. | "Still preferred for SQL-purist teams and existing KMP projects." |
| **License** | Apache 2.0 (Jetpack). | Apache 2.0. |

**Recommendation: Room.** Reasons:
1. AI-agent-friendliness is decisive for THIS project — the agent will be writing DB queries for cache invalidation, backup verification, etc. Room's annotation-based syntax has more training data.
2. Room KMP is stable; we don't sacrifice future KMP.
3. Migration tooling (`AutoMigration`) will pay off if we ever ship production (per CORE_RULES §30 we're debug-only for now, but the tool exists when needed).
4. Compose integration (`@Query fun observe(): Flow<List<X>>`) is the most ergonomic pattern.
5. The old ANI-KUTA project used SQLDelight. The user explicitly wants a NEW project with different requirements — we are not bound to the old choice. (And the old choice was driven by MPV/KMP streaming constraints that don't apply here.)

**Caveats:**
- Room uses KSP — adds ~5-10s to a clean build. Acceptable.
- Room KMP requires the `androidx.room:room-runtime` multiplatform artifact + a per-platform driver. Android uses the framework SQLite driver (`AndroidSQLiteDriver`); iOS/JVM would use `JdbcSQLiteDriver` / native. For Android-only v1 we just use the Android driver.
- Type converters must be in `commonMain` if we go KMP later. For v1 Android-only, they can be in `:core:data`.

### B9. Cache schema — recommended entities

All tables prefixed by source where multi-source is possible. AniList = `al_`, Kitsu = `kt_`, Jikan = `jk_`. Shared denormalized cache uses no prefix.

```sql
-- Core media (denormalized union across sources)
CREATE TABLE media (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,    -- internal surrogate
  source          TEXT NOT NULL,                       -- 'anilist' | 'kitsu' | 'jikan'
  source_id       TEXT NOT NULL,                      -- source-specific id (string for safety)
  idMal           INTEGER,                             -- MyAnimeList cross-ref, nullable
  type            TEXT NOT NULL,                       -- 'anime' | 'manga'
  title_en        TEXT,
  title_jp        TEXT,
  title_romaji   TEXT,
  title_native   TEXT,
  synopsis        TEXT,
  cover_image     TEXT,
  banner_image    TEXT,
  episodes        INTEGER,                             -- for anime
  chapters        INTEGER,                             -- for manga
  volumes         INTEGER,
  status         TEXT,                                 -- 'releasing' | 'finished' | 'cancelled' | 'not_yet_released'
  season         TEXT,
  season_year    INTEGER,
  average_score  INTEGER,
  popularity     INTEGER,
  favourites     INTEGER,
  updated_at     INTEGER NOT NULL,                    -- epoch millis
  UNIQUE(source, source_id)
);
CREATE INDEX idx_media_type_season ON media(type, season_year);
CREATE INDEX idx_media_idmal ON media(idMal);

-- Episode metadata (anime only)
CREATE TABLE episode (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  media_id        INTEGER NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  source          TEXT NOT NULL,
  source_id       TEXT NOT NULL,
  episode_number  REAL NOT NULL,                      -- REAL for 12.5 specials
  title           TEXT,
  thumbnail       TEXT,
  air_date        INTEGER,
  duration_sec    INTEGER,
  fill            INTEGER DEFAULT 0,                  -- boolean: is filler
  updated_at      INTEGER NOT NULL,
  UNIQUE(media_id, episode_number)
);
CREATE INDEX idx_episode_media ON episode(media_id, episode_number);

-- User's library entry (per source; user is one AniList user typically)
CREATE TABLE media_list_entry (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  media_id        INTEGER NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  source          TEXT NOT NULL,
  user_source_id  TEXT NOT NULL,                       -- AniList user id
  status          TEXT NOT NULL,                       -- 'current' | 'planning' | 'completed' | 'dropped' | 'paused' | 'repeating'
  score           INTEGER,
  progress        INTEGER,                              -- episodes/chapters watched
  progress_volumes INTEGER,
  repeat_count    INTEGER DEFAULT 0,
  notes           TEXT,
  started_at      INTEGER,
  completed_at    INTEGER,
  private         INTEGER DEFAULT 0,
  updated_at      INTEGER NOT NULL,                    -- last write to this row
  source_updated_at INTEGER,                           -- last remote value (for conflict detection)
  UNIQUE(media_id, source, user_source_id)
);
CREATE INDEX idx_list_status ON media_list_entry(status);

-- Airing schedule (anime)
CREATE TABLE airing_schedule (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  media_id        INTEGER NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  episode_number  INTEGER NOT NULL,
  airing_at       INTEGER NOT NULL,                    -- epoch millis UTC
  countdown_sec   INTEGER,                             -- denormalized for cheap queries
  updated_at      INTEGER NOT NULL,
  UNIQUE(media_id, episode_number)
);
CREATE INDEX idx_airing_time ON airing_schedule(airing_at);

-- Characters
CREATE TABLE character (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  source          TEXT NOT NULL,
  source_id       TEXT NOT NULL,
  name_en         TEXT,
  name_native     TEXT,
  image           TEXT,
  description     TEXT,
  updated_at      INTEGER NOT NULL,
  UNIQUE(source, source_id)
);

-- Media ↔ Character junction with role
CREATE TABLE media_character (
  media_id        INTEGER NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  character_id    INTEGER NOT NULL REFERENCES character(id) ON DELETE CASCADE,
  role            TEXT NOT NULL,                       -- 'main' | 'supporting' | 'background'
  PRIMARY KEY(media_id, character_id, role)
);

-- Studios
CREATE TABLE studio (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  source          TEXT NOT NULL,
  source_id       TEXT NOT NULL,
  name            TEXT NOT NULL,
  image           TEXT,
  is_animation_studio INTEGER DEFAULT 1,
  updated_at      INTEGER NOT NULL,
  UNIQUE(source, source_id)
);
CREATE TABLE media_studio (
  media_id   INTEGER NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  studio_id  INTEGER NOT NULL REFERENCES studio(id) ON DELETE CASCADE,
  role       TEXT NOT NULL,                           -- 'main' | 'studio' (vs 'producer' etc.)
  PRIMARY KEY(media_id, studio_id, role)
);

-- Genre / tag
CREATE TABLE genre (
  id    INTEGER PRIMARY KEY AUTOINCREMENT,
  name  TEXT NOT NULL UNIQUE
);
CREATE TABLE media_genre (
  media_id INTEGER NOT NULL REFERENCES media(id) ON DELETE CASCADE,
  genre_id INTEGER NOT NULL REFERENCES genre(id) ON DELETE CASCADE,
  PRIMARY KEY(media_id, genre_id)
);

-- Per-source cache state (the reconciliation table)
CREATE TABLE metadata_source_state (
  source          TEXT NOT NULL,                       -- 'anilist' | 'kitsu' | 'jikan'
  source_kind     TEXT NOT NULL,                       -- 'media' | 'episode' | 'character' | 'studio' | 'list_entry' | 'airing'
  source_id       TEXT NOT NULL,                       -- null for collection-level (e.g. 'all airing this week')
  etag            TEXT,
  last_fetched_at INTEGER NOT NULL,                    -- epoch millis of last attempted fetch
  last_success_at INTEGER,                             -- epoch millis of last 2xx
  failure_count   INTEGER NOT NULL DEFAULT 0,
  backoff_until   INTEGER,                             -- epoch millis; do not retry before this
  http_status     INTEGER,                             -- last HTTP status seen
  stale           INTEGER NOT NULL DEFAULT 0,         -- boolean: keep local, mark dirty
  PRIMARY KEY(source, source_kind, source_id)
);
CREATE INDEX idx_state_backoff ON metadata_source_state(backoff_until);

-- Watch/read progress local override (for optimistic updates + offline edits)
CREATE TABLE local_progress_override (
  media_id    INTEGER PRIMARY KEY REFERENCES media(id) ON DELETE CASCADE,
  progress    INTEGER,
  score       INTEGER,
  status      TEXT,
  notes       TEXT,
  updated_at  INTEGER NOT NULL,
  pending_sync INTEGER NOT NULL DEFAULT 1             -- boolean
);

-- Search history (for "recent searches" UI)
CREATE TABLE search_history (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  query       TEXT NOT NULL,
  type        TEXT,                                    -- 'anime' | 'manga' | null for both
  searched_at INTEGER NOT NULL
);
CREATE INDEX idx_search_at ON search_history(searched_at DESC);

-- FTS for full-text search on media
CREATE VIRTUAL TABLE media_fts USING fts4(
  media_id UNINDEXED,
  title_en, title_jp, title_romaji, title_native,
  tokenize=unicode61
);
```

**Notes:**
- `INTEGER` epoch millis throughout — JSON-friendly, timezone-safe.
- `media.source + source_id` is the canonical external key; the internal `id` is a surrogate for joins (cheaper than composite keys across N junction tables).
- `metadata_source_state` is the heart of the reconciliation layer (see B10).
- FTS4 (not FTS5) for broader SQLite-version compatibility on `minSdk=26` devices (Android API 26 ships SQLite 3.19 which supports FTS4; FTS5 needs 3.9+). On `minSdk=26`, API 26 devices have SQLite ≥ 3.19, so FTS5 works too — but FTS4 is a safer default. (unverified: exact SQLite version per API level; verify on first device test.)
- We use Room `@Entity` annotations, not raw SQL — the above is the *logical* schema.

### B10. Reconciliation strategy

**Default policy: last-write-wins per field, with explicit richer-data rule for episodes.**

**On refresh (e.g. user pulls-to-refresh on Details):**
1. Repository fetches remote media by `(source, source_id)`.
2. Local DB row is loaded.
3. For each field: take remote value if non-null, else keep local. (`COALESCE(remote.x, local.x)` at the SQL level after a temp insert, or do it in Kotlin.)
4. For `media_list_entry`: compare `source_updated_at` — if remote's `updated_at` is newer than local `source_updated_at`, take remote fields. If local's `updated_at` is newer than local `source_updated_at` (we have an unsynced optimistic edit), keep local and queue for upload.
5. Write merged row. Bump `metadata_source_state.last_success_at = now`.

**On 404 (source went away):**
- Do NOT delete the local row.
- Set `metadata_source_state.stale = 1`, `http_status = 404`, `backoff_until = now + 24h`.
- UI shows a "stale" badge on the affected media (a small chip "source unavailable").
- Re-check daily; if it comes back, clear the stale flag.

**On 5xx / network error:**
- `failure_count += 1`, `backoff_until = now + min(60s * 2^failure_count, 1h)`.
- Keep local data; mark `stale = 0` (it's not the source's fault, just network).

**On new episodes (append-never-overwrite rule):**
- If remote returns episode N: check if local has it.
  - If no → insert.
  - If yes → for each field, take remote value ONLY if remote is non-null AND local's value was populated from the SAME source (i.e. local wasn't hand-edited). Concretely: `UPDATE episode SET title = COALESCE(remote.title, local.title), thumbnail = COALESCE(remote.thumbnail, local.thumbnail) WHERE media_id=? AND episode_number=?`.
  - Never overwrite `air_date` if local has a confirmed value and remote has a different one — flag for human review via a log entry (rare).
- Append-only for airing_schedule: never delete future airings; if remote has fewer, assume source is incomplete.

**Conflict resolution for `media_list_entry`:**
- AniList is the source of truth for the *user's list*. If we also cache Kitsu/Jikan, those are read-only metadata; they don't write `media_list_entry`.
- Optimistic updates (CORE_RULES §23): when the user changes status locally, write to `media_list_entry` immediately, bump `updated_at`, mark `pending_sync = 1` in `local_progress_override`, then upload to AniList in the background. On success, clear `pending_sync`. On failure, retry with exponential backoff (WorkManager), and if it fails permanently, show a banner "couldn't sync to AniList, tap to retry."

**Field-level merge vs. LWW:** Field-level (per-field COALESCE) for media metadata; LWW for `media_list_entry` (per-row, source-of-truth comparison); append-never-overwrite for episodes. This is a 3-tier policy — explicitly documented so the agent knows.

### B11. Image caching — Coil vs. Glide vs. Kingfisher

**Recommendation: Coil 3.**

| Choice | Reason |
|---|---|
| Coil 3 (not 2) | Compose-native (`AsyncImage` composable), KMP-ready (Android + iOS + JVM), built on coroutines + Okio. Maintained by Colin White. |
| vs. Glide | Glide is View-system-first; Compose integration is via `accompanist-glide` (deprecated) or community wrappers. Coil is Compose-first. |
| vs. Kingfisher | Kingfisher is iOS/Swift. Not applicable. |
| vs. Picasso | Deprecated in practice. |

**Disk cache size policy:**
- Default: `255MB` (Coil's default). Adequate for an anime tracker (typical poster is 50-150KB → ~2000 posters cached).
- Set explicitly: `ImageLoader.Builder(context).diskCache { DiskCache.Builder().maxSizeBytes(250L * 1024 * 1024).build() }`.
- Allow override in Settings (low-storage devices): 50MB / 250MB / 500MB / unlimited.
- Use a per-image cache key that includes the resolved size, so a thumbnail and a full-res poster can both be cached.

**Placeholder/error drawables from the design token:**
- Do NOT use R.drawable placeholders. Use composables that read `LocalAppColors.current`.
- `AppAsyncImage`: wraps Coil's `AsyncImage`, accepts `placeholderRole: ColorRole = "surfaceRaised"` and `errorRole: ColorRole = "error"` and resolves them through the token system.
- This way, a theme swap instantly updates placeholders too.

**Memory cache:**
- Coil default is 25% of available app memory — fine.
- For very long lists (e.g. library with 5000 entries): rely on `LazyColumn`'s disposal + Coil's memory cache (LRU). Tune via `memoryCachePolicy` if jank appears.

---

## C. App architecture

### C12. Recommended architecture — modules & layering

**Pattern:** **Unidirectional Data Flow (UDF) + MVVM with StateFlow + Repository pattern.** Not strict MVI (MVI's `Intent` + `reduce()` boilerplate is overkill for an AI-customizable UI; we want the agent to edit *pluggable* components, not a sealed-class intent hierarchy). Not strict Clean Architecture's 3-layer (domain/data/presentation) — for an app this size, that's premature ceremony. We use 2 effective layers: **data (Repository + Room + Network)** and **ui (ViewModel + Compose)**, with the `:core:designsystem` and `:core:agent` as horizontal slices.

**Module graph (Gradle):**

```
:app                                  ← composition root: Application, MainActivity, ErrorActivity, nav graph
:core:designsystem                    ← tokens, AppTheme, CompositionLocals, base components, FontRegistry
:core:designsystem:tokens             ← DesignTokens data class, serialization, validation
:core:designsystem:components         ← AppButton, AppCard, etc.
:core:designsystem:icons              ← AppIcon (custom icon set, NOT Material Icons)
:core:data                            ← repositories, Room DB, daos, entities
:core:data:db                         ← Room database, DAOs, entities
:core:data:repository                 ← MediaRepository, ThemeRepository, BackupRepository, etc.
:core:network                         ← Apollo client (AniList GraphQL), Ktor (Kitsu/Jikan REST)
:core:network:anilist                 ← Apollo Kotlin + generated operations
:core:network:kitsu                   ← Ktor REST
:core:network:jikan                   ← Ktor REST
:core:agent                           ← AI agent (Cline port)
:core:agent:core                      ← agent loop, context, system prompt
:core:agent:llm                       ← provider abstraction (Anthropic / OpenAI / local), HTTP streaming
:core:agent:tools                     ← tool registry + per-tool impls (set_color_role, etc.)
:core:agent:permissions               ← approval flow (UI surfaces pending changes)
:core:common                          ← Logger, Result wrappers, time, dispatchers, crypto helpers
:core:ui                              ← shared UI state patterns, UiState, UiEvent
:core:backup                          ← backup/restore logic, zip builder, encryption
:core:charts                          ← Vico wrappers + custom Canvas radar
:feature:home
:feature:search
:feature:library
:feature:airing
:feature:details
:feature:settings
:feature:designstudio                 ← the AI customization UI (chat + token diff + preview)
:feature:profile                      ← uses :core:charts
```

**Why this many modules (and not fewer):**
- CORE_RULES §7 mandates modularity. Each module is a unit the AI agent can be told to "edit only files in `:feature:library`" without it accidentally touching `:core:data`.
- Horizontal slices (`:core:designsystem`, `:core:agent`) keep cross-cutting concerns out of features.
- Per-source network modules (`:core:network:anilist` vs `:core:network:jikan`) — the AniList GraphQL codegen is heavy and isolated; we don't want Jikan REST changes to re-trigger Apollo's codegen.
- `:core:designsystem:tokens` is split from `:core:designsystem:components` so the agent can read/write *just* the token schema without loading Compose.
- `:core:agent` is its own four-module sub-graph because it's the most novel part; isolating `tools` from `core` lets us add tools without touching the loop.

**Why not fewer modules (e.g. single `:core`):**
- Single `:core` means every change recompiles everything. With ~20 modules, a change in `:feature:library` only recompiles that + `:app`.
- AI agent context: telling the agent "the bug is in `:core:data:repository`" is more navigable than "the bug is in `:core` somewhere."

**Why not more (e.g. per-screen design-system modules):**
- Diminishing returns. 20 modules is the sweet spot for an app of this scope (~150-300 source files estimated).

**App composition root (`:app`):**
- `AndroidDesignApplication` — installs crash handler first (CORE_RULES §29), then Koin modules, then Coil ImageLoader singleton, then Apollo client, then schedules WorkManager jobs.
- `MainActivity` — hosts `AppTheme { AppNavHost() }`.
- `AppNavHost` — `NavHost` from Navigation Compose with all destinations.

### C13. DI — Hilt vs. Koin vs. Anvil

**Recommendation: Koin 4.**

| Axis | Hilt | Koin 4 | Anvil |
|---|---|---|---|
| Official | Yes (Google) | No (community, but very widely used) | No (Square) |
| Compile-time | Yes (Dagger/KSP) | Runtime | Yes (Kotlin compiler plugin) |
| KMP support | ❌ Android-only | ✅ Native KMP | ✅ Via kotlin-inject |
| Build speed impact | +10-30s on large graphs | ~0 (runtime) | Minimal |
| Learning curve | Steep (scopes, qualifiers, @InstallIn) | Gentle (module { single { } }, `by inject()`) | Steep |
| AI-agent-friendliness | ⚠️ LLMs often confuse Hilt scopes; `@HiltViewModel` + `@Inject constructor` patterns are fine but qualifier mistakes are common | ✅ Very LLM-friendly — `single<X> { X(get()) }` is hard to get wrong; errors are runtime but clear | ⚠️ Less training data; LLMs hallucinate Anvil's `@ContributesBinding` syntax |
| Multi-module | Excellent | Excellent | Excellent (its purpose) |
| Compose integration | `hiltViewModel()` | `koinViewModel()` | via `kotlin-inject-anvil-compose` |
| Hot reload / edit-and-continue | OK | Excellent (runtime resolution) | OK |

**Why Koin 4 over Hilt:**
1. **AI-agent-friendliness is decisive.** An LLM can write `val dataModule = module { single<MediaRepository> { MediaRepositoryImpl(get(), get()) } }` correctly every time. Writing the Hilt equivalent (`@Module @InstallIn(SingletonComponent::class) abstract class DataModule { @Binds abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository }` + `@Inject constructor(...)`) has more failure modes (qualifier collisions, scope mismatches, KSP errors that block the build).
2. **KMP future-proof.** If we go iOS later, Koin already works.
3. **Build speed.** No annotation processor for DI.
4. **Runtime errors are clear.** Koin's `NoBeanDefFoundException` is actionable. The "Hilt silently doesn't inject" cases (e.g. `@Inject constructor` on a class with no `@Module` providing its deps) are subtler.
5. **The old project used Koin/Injekt.** Familiarity within the team (and the user's prior context).

**Koin 4 specifics (not Koin 3):**
- Koin 4 (released ~2024) has a new resolver, better startup perf, and Compose multiplatform viewmodel scope out of the box.
- `koin-android` + `koin-androidx-compose` artifacts.
- Use `koinApplication { }` in `Application.onCreate()`, then `KoinJavaComponent.get<X>()` from non-Composable code, `koinViewModel<X>()` from Composables.

**Why not Anvil:** Anvil is for *very large* Dagger graphs (100+ modules) where the annotation processor becomes a build bottleneck. We're not there. Adding Anvil adds complexity for no benefit at our scale.

**Risk note (honest):** Koin being runtime means a missing dependency fails at first-use, not at compile. Mitigation: a `KoinTest` JUnit test that starts all modules and asserts every `single<X>` is resolvable. Run in CI on every PR. This recovers 90% of the compile-time safety Hilt would give.

### C14. Navigation — Navigation Compose vs Nav3 vs Voyager

**Recommendation: Navigation Compose (the original).**

| Axis | Nav Compose (androidx.navigation:navigation-compose) | Navigation 3 (androidx.navigation:navigation3) | Voyager |
|---|---|---|---|
| Stability (late 2025/2026) | Stable, mature | **Alpha / 1.0.0-alpha** as of late 2025. Still seeing breaking API changes per release notes. | Stable, third-party |
| Docs quality | Excellent | Sparse; mostly blog posts + Google I/O talk | Good (voyager.gitbook.io) |
| KMP | No (Android-only) | Yes (it's why it exists) | Yes |
| Back stack control | OK (opaque, you manipulate via routes) | Excellent (explicit `NavEntry` list) | OK |
| Type-safe routes | Yes (`@Serializable` route classes) | Yes | Yes (Screen objects) |
| Adaptive layouts | Manual (NavigationSuiteScaffold) | First-class (designed for it) | Manual |
| AI-agent-friendliness | ✅ High (most training data) | ⚠️ Low (API churn) | ⚠️ Medium |
| Community adoption | Vast | Small (new) | Medium |

**Why Nav Compose:**
1. **Stability.** The prior ANI-KUTA project adopted Nav3 then removed it (per CORE_RULES §8 / D-150). That's a clear signal: don't pioneer on this project.
2. **AI-agent-friendliness.** LLMs have orders of magnitude more Nav Compose training data.
3. **Type-safe routing** (`@Serializable route objects`) is available since 2.8.0 — closes the gap with Nav3's main selling point.
4. **Predictive back** support is mature.

**When to reconsider:** If the app later needs to share nav logic with iOS (KMP), revisit Nav3 or Voyager. For now, Android-only → Nav Compose.

**Setup:**
```kotlin
// build.gradle.kts
implementation("androidx.navigation:navigation-compose:2.8.+") // use BOM-aligned version
```
Use `NavigationSuiteScaffold` for adaptive (bottom nav on phone, rail on tablet, drawer on desktop).
For shared-element transitions across destinations: wrap `NavHost` in a `SharedTransitionLayout`, pass `SharedTransitionScope` down via CompositionLocal, use `Modifier.sharedElement(...)` with matching `key`s. Stable in Compose 1.7+.

### C15. Networking — Apollo Kotlin for GraphQL AniList, Ktor for Kitsu/Jikan REST

**AniList:** GraphQL API at `https://graphql.anilist.co`. **Recommend Apollo Kotlin.**
- Codegen: write `.graphql` files in `:core:network:anilist/src/commonMain/graphql/...`, Apollo's Gradle plugin generates Kotlin classes for operations + types.
- **Normalized cache:** `apollo-kotlin-normalized-cache` — persists to SQLite, gives automatic response merging by `__typename + id`. Invaluable for offline-first: query → cache → if cache is fresh and offline, return cache; if stale, fetch + merge.
- New normalized cache (Apollo blog March 2026 — though that date is in the future; treat as **(unverified)** that the API is exactly as described, but normalized cache itself is real and stable).
- Fetch policies: `CacheFirst`, `NetworkFirst`, `CacheOnly`, `NetworkOnly`. Use `NetworkFirst` for fresh data with offline fallback, `CacheFirst` for `AiringSchedule` (low-staleness tolerance).
- File uploads: not needed for AniList (read-only as a client).
- Auth: header `Authorization: Bearer <token>` — Apollo's `HttpInterceptor`.

**Kitsu & Jikan:** REST. **Ktor 3 client.**
- Ktor is KMP, coroutine-native, well-documented.
- Use `ktor-client-core` + `ktor-client-okhttp` (Android) + `ktor-client-content-negotiation` + `ktor-serialization-kotlinx-json`.
- Build a `KitsuClient` and `JikanClient` as thin wrappers, each returning `Flow<Result<T>>` or `HttpResponse` for the repository to handle.
- Jikan (the unofficial MyAnimeList API) is rate-limited (3 req/s, 60 req/min). Ktor client must enforce a `Mutex`-based rate limiter (or use `kotlinx-coroutines`'s `Semaphore`).

**Why Apollo over a custom Ktor-based GraphQL client:**
- Codegen eliminates hand-written query parsing.
- Normalized cache is the killer feature — implementing that manually is weeks of work.
- Subscription support (not needed for AniList now, but Apollo gives it for free).

**Caveat:** Apollo's normalized cache uses its own SQLite store (via `apollo-normalized-cache-sqlite`). This is *separate* from our Room DB. That's OK — they serve different purposes (Apollo's = GraphQL response cache; Room = canonical source-truth). If duplication bothers us, we can have Room mirror from Apollo's cache via a hook, but that's premature optimization.

### C16. State management — StateFlow / SharedFlow pattern + optimistic updates

**Pattern (mandatory across the project):**

1. **Repository → Flow<Data>** for reads.
   - `MediaRepository.observeMedia(id: Long): Flow<Media>` — wraps a Room `@Query fun observeMedia(id): Flow<Media>`.
   - Cold flow (starts on collect, stops on cancel). Room handles the underlying SQL change notifications.
2. **Repository → Result<T>** for writes (not Flow — one-shot).
   - `suspend fun setStatus(mediaId: Long, status: Status): Result<Unit>`.
3. **ViewModel → StateFlow<UiState>**:
   ```kotlin
   data class DetailsUiState(
     val loading: Boolean = true,
     val media: Media? = null,
     val episodes: List<Episode> = emptyList(),
     val error: String? = null,
     val isStale: Boolean = false,
   )
   class DetailsViewModel(...) : ViewModel() {
     private val _state = MutableStateFlow(DetailsUiState())
     val state = _state.asStateFlow()
     init { load() }
     fun load() = viewModelScope.launch { ... }
   }
   ```
4. **UI collects** with `collectAsStateWithLifecycle()` (lifecycle-aware — stops collecting when the screen isn't visible).
5. **Optimistic update pattern (per CORE_RULES §23):**
   ```kotlin
   fun setStatus(mediaId, newStatus) {
     // 1. Optimistic local write — UI updates immediately
     _state.update { it.copy(media = it.media?.copy(status = newStatus)) }
     viewModelScope.launch {
       val result = repository.setStatus(mediaId, newStatus)
       if (result.isFailure) {
         // 2. Rollback + show error
         _state.update { it.copy(media = it.media?.copy(status = oldStatus), error = "Couldn't sync") }
         _events.emit(UiEvent.ShowSnackbar("Couldn't sync — tap to retry"))
       }
     }
   }
   ```
6. **Cross-screen consistency:** shared state via a single repository's `Flow`. E.g. `LibraryViewModel` and `DetailsViewModel` both collect from `mediaRepository.observeListEntries()` — when one writes, Room emits to both.

**UiEvent vs. UiState:** UiState is what's rendered. UiEvent is one-shot (snackbar, navigation). Use `SharedFlow<UiEvent>` (replay=0, extraBufferCapacity=8) and `LaunchedEffect` to collect. Don't put "show snackbar" booleans in UiState — they're hard to dismiss cleanly.

### C17. Charts — Vico + custom Canvas for radar

**Recommendation:**
- **Vico** (`com.patrykandpatrick.vico:compose-m3` and `compose`) for: line, bar, column, stacked, candle charts on the profile page (e.g. episodes watched per month, score distribution).
  - Compose-native, KMP, actively maintained, MIT-style license (Apache 2.0 — verify on use).
  - Has good defaults but is heavily themable.
  - Note: Vico ships a `compose-m3` artifact that pulls Material theme — we want `compose` (the foundation one) and pass our own colors via `CartesianChart`'s `lineSpec` / `columnSpec`.
- **Custom Compose Canvas** for the **radar/spider chart** (Vico doesn't ship one as of v2.x — (unverified) check before locking; if Vico ships radar by the time we build, prefer it).
  - Radar is ~150 LOC of Canvas code: N axes radiating from center, polygon overlay for the user's values, concentric reference rings.
  - Animate with `Animatable` from 0 → 1 on first appearance, lerping each vertex.

**Why not others:**
- **MPAndroidChart (wrapped)**: View-system, not Compose. Wrapping a View in Compose is fine (via `AndroidView`) but loses Compose's recomp position; plus MPAndroidChart is in maintenance mode.
- **YCharts**: Less Compose-native than Vico.
- **Compose Canvas only (everything custom)**: More control but slower to ship the bar/line charts.

**Radar chart implementation sketch:**
```kotlin
@Composable
fun RadarChart(values: List<Float>, labels: List<String>, maxValue: Float, modifier: Modifier) {
  val anim = remember { Animatable(0f) }
  LaunchedEffect(values) { anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
  Canvas(modifier.aspectRatio(1f)) {
    val cx = size.width / 2; val cy = size.height / 2
    val r = min(cx, cy) * 0.85f
    val n = values.size
    // reference rings (5)
    for (i in 1..5) {
      val rr = r * i / 5
      drawPath(Path().apply {
        for (j in 0 until n) {
          val a = -PI/2 + 2*PI*j/n
          val x = cx + rr * cos(a); val y = cy + rr * sin(a)
          if (j==0) moveTo(x,y) else lineTo(x,y)
        }
        close()
      }, color = LocalAppColors.current.outline, style = Stroke(1.dp.toPx()))
    }
    // data polygon
    drawPath(Path().apply {
      for (j in 0 until n) {
        val v = (values[j] / maxValue).coerceIn(0f, 1f) * anim.value
        val a = -PI/2 + 2*PI*j/n
        val x = cx + r * v * cos(a); val y = cy + r * v * sin(a)
        if (j==0) moveTo(x,y) else lineTo(x,y)
      }
      close()
    }, color = LocalAppColors.current.primary.copy(alpha = 0.3f))
    // ... axes, labels via drawIntoCanvas
  }
}
```
Real impl should hoist labels out to a `Box` overlay (drawing text on Canvas is less crisp than Composables). For buttery-smooth, animate via `graphicsLayer` rotation only.

### C18. Backup/restore — zip in app-specific storage, WorkManager weekly

**File format:** a single zip:
```
androiddesign-backup-2026-01-15.zip
├── manifest.json             ← version, app version, schema versions, timestamp
├── database/
│   └── android-design.db     ← Room file, checkpointed (WAL flushed)
├── datastore/
│   └── preferences.pb        ← DataStore preferences file
├── themes/
│   ├── active.json           ← pointer to active theme id
│   └── <theme-id>.json       ← each saved theme
├── design-state/
│   └── agent-snapshots.json  ← the design rollback stack (see D23)
└── secrets/
    └── anilist-token.enc     ← AES-GCM encrypted, see below
```

**Location:**
- **Default: app-specific external storage** (`getExternalFilesDir("backups")`). Not visible to the user in their Gallery, but accessible via a file manager. Survives uninstall? No — app-specific is wiped on uninstall, but it's the only path with no scoped-storage permission dance.
- **User choice in Settings: export to Downloads** (visible to the user, requires `MediaStore` API on API 29+, no storage permission needed for the app's own files in `Downloads/<app-name>/`).
- **Auto-backup target:** app-specific external storage (`backups/rolling-latest.zip`). User can copy it out manually via Settings → "Export backup to Downloads."

**Weekly auto-backup via WorkManager:**
- `PeriodicWorkRequest` with `repeatInterval = 7, DAYS`.
- Constraints (per user pref): `NetworkType.NOT_REQUIRED` (we're offline-first), `requiresCharging = true`, `requiresBatteryNotLow = true`.
- Unique work name `"weekly-backup"`, `ExistingPeriodicWorkPolicy.KEEP` (survives app upgrade).
- One rolling copy: on each run, write `backups/rolling-latest.zip.tmp`, then atomic rename to `backups/rolling-latest.zip`. The previous file is overwritten. (One rolling copy = no accumulation of backup files.)

**AniList token encryption:**
- The token must be in the backup so the user can restore on a new device without re-login.
- **Default: encrypt with Android Keystore-backed AES-GCM key**, no user passphrase (the key is hardware-backed where available; on devices without StrongBox, software-backed Keystore still beats plaintext).
- **Optional user-set passphrase:** when enabled (Settings → "Encrypt backup with password"), derive a key from the passphrase via PBKDF2 (600k iterations, salt = random 16 bytes per backup) and use that instead of the Keystore key. The salt + KDF params go in `manifest.json`.
- Trade-off: passphrase-encrypted backups are portable across devices; Keystore-encrypted backups are device-bound. Default is Keystore (more secure, less portable); passphrase is opt-in.

**Restore:**
- `restoreFromFile(uri)`: read manifest → validate schema versions (reject if newer than current app supports) → close DB → overwrite files → reopen DB → reload themes → re-resolve active theme.
- On schema mismatch (older backup vs newer app): attempt Room's `fallbackToDestructiveMigrationOnDowngrade` for the DB. For tokens/themes: if `$schema` is newer, refuse with a clear error; if older, attempt to migrate (best-effort).

### C19. WorkManager task list + cadences

| Task | Cadence | Constraints | Notes |
|---|---|---|---|
| **Weekly backup** | 7 days | charging + battery-not-low | One rolling copy (C18). |
| **Airing schedule refresh** | 6 hours | network connected | Fetches next 24h of airings for the user's library. |
| **Library list sync** | 12 hours | network unmetered | Pulls AniList list changes; pushes `local_progress_override.pending_sync = 1` rows. |
| **Stale media retry** | 24 hours | network connected | Walks `metadata_source_state WHERE stale = 1 OR backoff_until <= now`, refetches each. |
| **Pending edits upload** | exponential: 30s → 1m → 5m → 30m → 2h → 12h (cap) | network connected | One-shot per edit, scheduled via `WorkManager.enqueueUniqueWork(...)`. Not periodic. |
| **Episode metadata refresh** (for library items) | 7 days | network unmetered | AniList rarely changes episode metadata for finished shows; weekly is enough. |
| **Token housekeeping** | on demand | — | Snapshot pruning in agent history (see D23). |

**WorkManager API:** use `setForeground` for the backup (so it survives Doze if user-initiated). Periodic workers must respect the minimum 15-minute period (we don't need anything faster).

### C20. Crash handling — global handler + ErrorActivity

**Pattern (per old CORE_RULES §29) is still good practice in 2025/2026.** Specifically:
- `Thread.setDefaultUncaughtExceptionHandler { thread, throwable -> ... }` in `Application.onCreate()` *before* any other init (Koin, Apollo, etc.). Capture the previous handler and call it after persisting (so the system still gets the crash for crashlytics if we add it later).
- Persist to `filesDir/last_crash.txt` (timestamp, thread, exception class + message, full stack trace, app version, device info).
- Launch `ErrorActivity` via `Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`.
- `ErrorActivity` shows: error icon, "Something went wrong" heading, scrollable monospace log, Copy button (clipboard), Restart button, Close button.
- Register in `AndroidManifest.xml` with `android:exported="false"`, `android:configChanges="orientation|screenSize|keyboardHidden"` (prevents recreation on rotation), `android:process=":error"` (optional isolation — but not necessary; same process is fine).

**Refinement for 2026:** also integrate a lightweight in-app bug reporter (since we have no external crashlytics by default). Settings → "Send debug report" reads `filesDir/last_crash.txt` + recent logcat lines and lets the user email them.

**Don't:** install an uncaught-exception handler *and* Crashlytics (Firebase) without chaining — the chain rule is: capture original handler, do your thing, then call original. (CORE_RULES §29.1 already says this.)

---

## D. AI agent integration

### D21. On-device agent architecture — module shape

The agent is a **Cline-style loop**: read user request → build context (system prompt + tool results so far) → call LLM → execute tool calls (with user approval) → loop until LLM stops emitting tool calls or user interrupts.

**Module shape (4 sub-modules under `:core:agent`):**

```
:core:agent
├── :core:agent:core           ← AgentLoop, ContextBuilder, SystemPrompt, TokenBudget
├── :core:agent:llm             ← LlmProvider interface, AnthropicProvider, OpenAIProvider, LocalLlamaProvider, HttpStreaming
├── :core:agent:tools           ← ToolRegistry, Tool interface, all per-tool impls
└── :core:agent:permissions     ← ApprovalRequest, ApprovalResponse, ApprovalGate (UI hook)
```

**Why this split:**
- The loop (`core`) should be reusable for any LLM (`llm`) — swap providers without touching the loop.
- Tools (`tools`) are the agent's *capabilities*; they're the highest-churn surface (new tool per design feature). Isolating them means new tools don't risk breaking the loop.
- Permissions (`permissions`) is a UI-adjacent concern — the agent asks "can I do X?" and the UI surfaces it. Keeping this separate means the agent can run headlessly in tests.

**How the agent reads/writes design tokens safely:**
- All token access goes through a `ThemeRepository` interface (defined in `:core:designsystem:tokens`, implemented in `:core:data:repository`).
- The agent never touches files directly. Its tools call repository methods:
  - `themeRepository.preview(tokens: DesignTokens)` — sets the preview state flow.
  - `themeRepository.commit(previewId)` — writes preview to disk + bumps active.
  - `themeRepository.rollback(snapshotId)` — restores a prior snapshot.
- This indirection means: (a) the agent can't corrupt the disk by writing bad JSON (validation is in the repository); (b) the permission gate sits between tool invocation and repository write; (c) tests can swap a fake repository.

**Provider abstraction (`:core:agent:llm`):**
```kotlin
interface LlmProvider {
  fun stream(messages: List<Message>, tools: List<ToolSchema>): Flow<StreamChunk>
}
sealed class StreamChunk {
  data class Text(val delta: String) : StreamChunk()
  data class ToolUse(val id: String, val name: String, val args: JsonObject) : StreamChunk()
  data class ToolUseComplete(val id: String) : StreamChunk()
  data class Usage(val tokensIn: Int, val tokensOut: Int) : StreamChunk()
  object Done : StreamChunk()
}
```
- Anthropic (Claude) is the obvious first target (best tool-use track record; Cline is Anthropic-first).
- OpenAI-compatible as second.
- A local-LLM provider (llama.cpp via jni, or `MLC LLM` Android) for "fully on-device" mode is a future feature; the interface accommodates it but v1 ships cloud-only.

**HTTP streaming:** Okio's `Okio.source(response.body!!.byteStream()).buffer()` → parse SSE lines → emit chunks. Ktor client also has `HttpStatement.executeIn` for streaming; either works.

**Token budget management:**
- Hard cap: e.g. 100k input tokens for context windowing.
- If approaching cap: summarize older tool results (replace with `[tool X returned: success, 12 lines]`).
- Always keep the system prompt + last 4 tool results verbatim.

### D22. Tool surface for design customization

**Recommended tool list (v1, ~12 tools):**

| Tool | Args | Effect |
|---|---|---|
| `get_active_theme` | — | Returns current committed tokens JSON. |
| `get_preview_theme` | — | Returns preview (in-flight) tokens JSON. |
| `set_color_role` | `role: String, value: HexColor` | Updates preview's `colors[role]`. Validates role exists. |
| `set_typography` | `scale: String, family: String?, size: Int?, weight: Int?, lineHeight: Int?, tracking: Float?` | Updates one entry of `typography.scale`. |
| `set_shape` | `role: String, radius: Int` | Updates one entry of `shapes`. |
| `set_motion` | `role: String, duration: Int?, easing: String?` | Updates `motion.durations` or `motion.easings`. |
| `set_spacing` | `key: String, value: Int` | Updates `spacing`. |
| `set_component_variant` | `component: String, variant: String` | Updates `componentVariants` (e.g. button → "outline"). |
| `apply_image_palette` | `image_uri: String, mapping: Map<Role, SwatchKind>` | Extracts palette via Palette API, maps swatches to roles per the mapping. Updates preview. |
| `swap_font_family` | `role: "ui"\|"display"\|"mono", family_key: String` | Updates `typography.family` or `familyDisplay` or `familyDisplay` etc. Validates family exists in FontRegistry. |
| `swap_layout` | `screen: String, layout_id: String` | For screens with multiple layouts (Home = grid vs. list, Profile = compact vs. expanded). Updates `screen_layouts` JSON. |
| `preview` | — | Marks preview as "ready to show" — UI shows live preview. |
| `commit` | `message: String` | Writes preview → active, takes a snapshot (D23). |
| `rollback` | `to_snapshot_id: String?` | Rolls back to last snapshot (or a specific one). |
| `list_snapshots` | — | Returns the snapshot history. |

**Tool design rules:**
- Every tool is **idempotent** (calling it twice with same args = same result; no side effects beyond preview mutation).
- Every tool **validates** args against the token schema. Invalid → return structured error (`{ "error": "color 'foo' not in roles" }`), don't throw.
- Every tool returns JSON (not free text) — easier for LLM to consume.
- Tools that mutate (set_*, apply_image_palette, swap_*) write to **preview**, not active. Only `commit` writes to active.
- `swap_layout` is intentionally limited to layout *variants we've pre-built* — the agent does NOT generate arbitrary Compose code in v1. (That's a v2 ambition; risk of build breakage is too high for v1.)

**Approval flow:**
- Mutating tools are **auto-applied to preview** (no approval needed — preview is ephemeral and visible).
- `commit` ALWAYS requires approval (user taps "Apply" in Design Studio).
- `rollback` requires approval only if rolling back more than 1 step.

### D23. Undo/rollback — snapshot system

**Recommendation: in-app versioned snapshot list (NOT a shadow git repo).**

A shadow git repo (e.g. `libgit2` via jni) is overkill: the design state is a single JSON file, not a tree. The git model's value is branching + 3-way merge — we don't need that. What we need is "linear history of states + jump back."

**Snapshot store:**
- `filesDir/agent-snapshots.json` — a JSON list:
  ```jsonc
  [
    { "id": "snap_001", "at": 1734567890, "message": "initial", "tokens": { ... full tokens ... } },
    { "id": "snap_002", "at": 1734567900, "message": "warmer accent", "tokens": { ... } },
    ...
  ]
  ```
- Cap at 50 most-recent snapshots; older evicted (configurable).
- Each `commit` writes a new snapshot (with the agent's commit message).
- `rollback(to_snapshot_id)` sets the active theme = that snapshot's tokens (does NOT delete newer snapshots — they're still in history for redo).
- "Redo" = forward-roll to the next snapshot in the list.

**Why not git:**
- Git's index/staging/HEAD model maps poorly to "user picks a state from a list."
- libgit2 native deps would bloat APK by ~3-5MB and add an ABI-splitting complication we don't need.
- 50 JSON snapshots is ~1MB worst case — trivial to back up.

**Why this works for the agent:**
- The agent calls `commit` after each user turn → automatic history.
- The agent can call `list_snapshots` to see what's been tried → can reason about undoing.
- The agent can call `rollback` to revert a change the user disliked.

**Crash safety:**
- Snapshots are written atomically (write to `.tmp` then rename).
- On app start: if the snapshots file is corrupt (JSON parse fails), back it up to `agent-snapshots.corrupt.json` and start fresh from the bundled default theme. Log a WARN.

---

## E. Build & CI

### E24. ABIs — `arm64-v8a` + `armeabi-v7a` + `x86_64`

**Configuration (via convention plugin in `build-logic/`):**
```kotlin
// build-logic/convention/src/main/kotlin/AndroidApplicationConvention.kt
android {
  ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
  }
  splits {
    abi {
      isEnable = false  // we use App Bundle, not splits
    }
  }
}
```

**APK size impact analysis:**
- For a Compose app with **no heavy native libs**, each ABI's `lib/` folder is tiny (Compose ships as Java/Kotlin bytecode, not native; the only native libs are typically Skia, ICU, etc., which are already in the system image and not packaged).
- The only ABI-specific native code we MIGHT ship:
  - Apollo SQLite driver (uses the system's libsqlite — no native lib packaged).
  - If we add a local-LLM provider (llama.cpp) later: ~5-10MB per ABI. At that point, ABI splits matter.
- **For v1: shipping all 3 ABIs in a single universal APK adds <1MB to APK size.**

**Recommendation: ship via App Bundle (.aab) to Google Play (Play handles per-ABI splits server-side). Ship a universal APK for sideload/emulator (CI uploads `*-universal.apk`).**
- AAB is mandatory for new Play apps since Aug 2021 anyway.
- For sideload (and CI artifact), the universal APK lets the user install on any device including x86_64 emulator.
- Do NOT enable `splits.abi` — it complicates the artifact list without benefit when AAB is the Play path.

**Convention plugin verification:** add a CI step that inspects each built APK's `lib/` and fails if any forbidden ABI (`x86`, `mips`, `mips64`) appears. The old project's "Verify ABIs" step (CORE_RULES §8) is a good pattern; we mirror it but allow x86_64.

### E25. compileSdk / targetSdk / minSdk

**Recommendation:**
- `compileSdk = 36` (Android 16) — required for Compose BOM 2025+ and future AGP versions. (CORE_RULES §8 settled on 36 for the old project; we match.)
- `targetSdk = 36` — Google Play requires new apps to target API 36 by Aug 31 2026 (per Play Console targetSdk requirement). Starting at 36 future-proofs for the first Play submission.
- `minSdk = 26` (Android 8.0) — covers ~98% of active Android devices (as of late 2025). Going lower (24 = Android 7) adds ~1% more devices but:
  - Loses access to some AndroidX libraries' newer features (e.g. some require minSdk 26).
  - Adds back-compat workaround burden.
  - Android 7's WebView is no longer updated by Chrome (security risk for users).
  Going higher (28 = Android 9) loses ~3-5% more devices — not worth it for a consumer anime app where Android 8 users exist.
- `buildToolsVersion = "36.0.0"` (implicit via AGP).
- AGP version: latest stable in 2026 (AGP 8.7+).

### E26. GitHub Actions build — CI-only build rule

**Per CORE_RULES §8, no local builds.** CI workflow:

```yaml
# .github/workflows/build-apk.yml
name: Build APK
on:
  push:
    branches: [main, 'feature/**', 'fix/**']
  pull_request:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}
          # main branch writes cache; feature branches read-only (prevents cache poisoning)
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
        with:
          packages: 'platforms;android-36 build-tools;36.0.0'
      - name: Cache Konan (KMP, if used)
        uses: actions/cache@v4
        with:
          path: ~/.konan
          key: konan-${{ runner.os }}-${{ hashFiles('**/libs.versions.toml') }}
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Verify ABIs
        run: |
          for apk in app/build/outputs/apk/debug/*.apk; do
            unzip -l "$apk" | grep -E 'lib/(arm64-v8a|armeabi-v7a|x86_64)/' || \
              { echo "No expected ABIs in $apk"; exit 1; }
            unzip -l "$apk" | grep -E 'lib/(x86|mips|mips64)/' && \
              { echo "Forbidden ABI in $apk"; exit 1; } || true
          done
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: apk-debug-${{ github.sha }}
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 30
```

**Caching strategy:**
1. **Gradle User Home** via `gradle/actions/setup-gradle@v4` — caches `~/.gradle/caches`, `~/.gradle/wrapper`, `~/.gradle/native`, configuration-cache. This is the single most impactful cache (saves 5-15 min per build).
2. **Konan** (only if we use KMP) — `~/.konan` for native cross-compilation artifacts. Cache key on `libs.versions.toml`.
3. **AVD** (only for instrumented tests, future) — prewarm an emulator snapshot.
4. **Configuration cache** — enable `org.gradle.configuration-cache=true` in `gradle.properties`. setup-gradle reuses it across CI runs.
5. **Build cache** — `org.gradle.caching=true` + remote build cache (Gradle Enterprise or self-hosted Develocity optional).

**Release APK (signed):** separate workflow `release-apk.yml` triggered on tags (`v*`). Uses a keystore stored as a GitHub secret (base64). Outputs `*-release.apk` + `*.aab`.

**Lint/detekt on PR:** separate job, fast, fails fast.

### E27. Fonts bundling — confirm res/font approach

**Confirmed:** `res/font/<font>.ttf` (or `.otf`) is the canonical mechanism. Android's resource system compiles them into the APK's `resources.arsc` (small reference) + the raw `.ttf` is stored as an asset. At runtime, `R.font.inter_regular` resolves to a `Font` object via Compose's `Font(R.font.inter_regular)`.

**Why this avoids "font not found at runtime":**
- The `R.font.*` ID is assigned at compile time. If the file isn't there, the build fails — there is no runtime "font not found" error.
- This contrasts with the `GoogleFonts` downloadable-fonts contract, which requires the Google Play Services font provider + network — that DOES fail at runtime if no network / Play Services missing.

**Bundled fonts in the design token:** see A5. `theme.json.typography.family = "inter"` resolves to `FontRegistry["inter"]` which returns a `FontFamily` built from `R.font.inter_*`.

**APK size impact:**
- Inter (variable, single file, all weights): ~700KB.
- Sora (variable): ~500KB.
- JetBrains Mono (variable): ~600KB.
- Total ~1.8MB — acceptable.
- Strip unused language subsets if APK size becomes a concern: `fonttools` `pyftsubset` can subset to Latin + CJK as needed. (Likely unnecessary at v1.)

---

## Proposed Module Graph

```
:app
├── :core:designsystem
│   ├── :core:designsystem:tokens        ← DesignTokens, serializers, validator, FontRegistry
│   ├── :core:designsystem:components     ← AppButton, AppCard, AppTopBar, AppSheet, etc.
│   └── :core:designsystem:icons         ← AppIcon (custom SVG icon set, NOT Material Icons)
├── :core:data
│   ├── :core:data:db                     ← Room database, DAOs, entities, migrations
│   └── :core:data:repository             ← MediaRepository, ThemeRepository, BackupRepository, etc.
├── :core:network
│   ├── :core:network:anilist             ← Apollo Kotlin + generated operations
│   ├── :core:network:kitsu                ← Ktor REST
│   └── :core:network:jikan                ← Ktor REST + rate limiter
├── :core:agent
│   ├── :core:agent:core                  ← AgentLoop, ContextBuilder, SystemPrompt, TokenBudget
│   ├── :core:agent:llm                  ← LlmProvider, AnthropicProvider, OpenAIProvider, HttpStreaming
│   ├── :core:agent:tools                 ← ToolRegistry, set_color_role, apply_image_palette, etc.
│   └── :core:agent:permissions           ← ApprovalGate, ApprovalRequest
├── :core:common                          ← Logger, Result, time, dispatchers, crypto helpers
├── :core:ui                              ← UiState, UiEvent, ViewModel base, collectAsStateWithLifecycle wrappers
├── :core:backup                          ← zip builder, encryption, WorkManager backup worker
├── :core:charts                          ← Vico wrappers + custom RadarChart Canvas
├── :feature:home
├── :feature:search
├── :feature:library
├── :feature:airing
├── :feature:details
├── :feature:settings
├── :feature:designstudio                  ← AI customization UI (chat + token diff + live preview)
└── :feature:profile                      ← uses :core:charts (radar + bar)
```

**Dependency direction rules:**
- `:app` depends on everything (composition root).
- `:feature:*` depend on `:core:designsystem`, `:core:ui`, `:core:common`. They depend on a Repository interface (not impl) — the impl is bound via Koin in `:app`.
- `:core:data:repository` depends on `:core:data:db`, `:core:network:*`, `:core:common`. Does NOT depend on any `:feature:*`.
- `:core:agent:tools` depends on `:core:designsystem:tokens` (the DesignTokens type) + a `ThemeRepository` interface from `:core:data:repository`. Does NOT depend on `:core:agent:llm`.
- `:core:agent:core` depends on `:core:agent:llm`, `:core:agent:tools`, `:core:agent:permissions`. Does NOT depend on `:core:data` directly (the tools do that).
- `:core:designsystem` has NO upstream deps — it's the leaf. This is critical: the design system must not depend on data, agent, or features (it would make the AI agent's edits ripple).

**Convention plugins (in `build-logic/`):**
- `androiddesign.android.application` — applies `com.android.application`, sets compileSdk/targetSdk/minSdk, abiFilters, Java 21, Compose.
- `androiddesign.android.library` — applies `com.android.library`, same SDK + Compose.
- `androiddesign.android.feature` — extends library, adds baseline-profile hook + Hilt-less ViewModel setup.
- `androiddesign.kotlin.multiplatform` — only for the few modules we may take KMP later (`:core:designsystem:tokens`, `:core:network:*`).
- `androiddesign.compose` — applies `org.jetbrains.kotlin.plugin.compose` + BOM.

---

## Open questions / risks to flag

1. **Apollo Kotlin normalized cache ↔ Room duplication.** Two SQLite stores. Acceptable, but watch for sync bugs.
2. **WorkManager reliability on Chinese OEMs (Xiaomi, Huawei).** Aggressive battery killers will skip the weekly backup. Document for users; recommend disabling battery optimization.
3. **Agent token cost.** Cloud LLM calls cost money. Need a setting for "use local LLM only" before this is shippable to non-dev users. (Out of scope for v1, but the LlmProvider interface accommodates it.)
4. **Palette API on JDK 11+ Android.** The `androidx.palette` lib is Android-only. For KMP later, swap to `jordond/kmpalette`. The `TonalPaletteGenerator` interface (A4) means we don't have to change call sites.
5. **Custom Canvas radar chart doesn't get Compose's a11y for free.** Need to add `Modifier.semantics` with `contentDescription` describing the values.
6. **Encrypted backup passphrase UX.** User-set passphrase + forget = data loss. Recommend: passphrase is *optional*, default is Keystore-bound; if user enables passphrase, show a warning + recovery hint.
7. **Apollo Kotlin on Android requires the GraphQL schema downloaded from AniList.** Need a CI step to refresh the schema periodically (separate `update-schema` workflow) — else operations drift from the live API.
8. **Compose stability of token classes.** Critical to mark `@Immutable`. If we forget, recompositions explode on theme swap. Add a Detekt rule to enforce `@Immutable` on data classes in `:core:designsystem:tokens`.

---

## References (verified during research)

- Android Developers — *Anatomy of a theme in Compose*: https://developer.android.com/develop/ui/compose/designsystems/anatomy
- Android Developers — *Material Design 3 in Compose*: https://developer.android.com/develop/ui/compose/designsystems/material3
- Android Developers — *Work with fonts | Compose*: https://developer.android.com/develop/ui/compose/text/fonts
- Android Developers — *Select colors with the Palette API*: https://developer.android.com/develop/ui/views/graphics/palette-colors
- Android Developers — *Shared element transitions in Compose*: https://developer.android.com/develop/ui/compose/animation/shared-elements
- Android Developers — *Jetpack Navigation 3*: https://developer.android.com/guide/navigation/navigation-3
- Android Developers — *DataStore*: https://developer.android.com/topic/libraries/architecture/datastore
- Android Developers — *Android Keystore*: https://developer.android.com/privacy-and-security/keystore
- Android Developers — *Android ABIs (NDK)*: https://developer.android.com/ndk/guides/abis
- Apollo Kotlin — *Normalized caches*: https://www.apollographql.com/docs/kotlin/caching/normalized-cache
- Apollo Kotlin — *New Normalized Cache (blog)*: https://www.apollographql.com/blog/the-new-apollo-kotlin-normalized-cache
- Coil — *Image Loaders*: https://coil-kt.github.io/coil/image_loaders
- Ktor — *Client (KMP)*: https://ktor.io/docs/client-create-multiplatform-application.html
- Vico charts: https://github.com/patrykandpatrick/vico and https://guide.vico.patrykandpatrick.com
- ProAndroidDev — *Which local database in 2025 (Realm vs SQLDelight vs Room)*: https://proandroiddev.com/which-local-database-should-you-choose-in-2025-comparing-realm-sqldelight-and-room-4221b354c899
- ProAndroidDev — *Hilt vs Koin: hidden cost of runtime injection*: https://proandroiddev.com/hilt-vs-koin-the-hidden-cost-of-runtime-injection-and-why-compile-time-di-wins-3d8c522a073b
- ProAndroidDev — *Migrating to Jetpack Compose Navigation 3*: https://proandroiddev.com/migrating-to-navigation-3-in-jetpack-compose-34b0389a9aea
- ProAndroidDev — *Goodbye EncryptedSharedPreferences: A 2026 Migration Guide*: https://proandroiddev.com/goodbye-encryptedsharedpreferences-a-2026-migration-guide-4b819b4a537a
- Gradle — *Gradle on GitHub Actions*: https://docs.gradle.org/current/userguide/github-actions.html
- Cline (open-source AI coding agent, SDK): https://github.com/cline/cline and https://cline.bot
- Braintrust — *Canonical agent architecture: a while loop with tools*: https://www.braintrust.dev/blog/agent-while-loop
- MakersDen — *AI Agent Architecture: Tools, Memory, Permissions & Guardrails*: https://makersden.io/blog/ai-agent-architecture-tools-memorhy-permissions-guardrails
- jordond/kmpalette (Compose Multiplatform Palette): https://github.com/jordond/kmpalette
