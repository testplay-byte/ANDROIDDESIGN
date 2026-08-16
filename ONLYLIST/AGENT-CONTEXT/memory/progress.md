# Progress — Only-List

> Living document. Most recent session at the top. Read FIRST at session start.

---

## Session 7 — R-13 modern UI overhaul (COMPLETE)

**Phase:** 1 (R-13 modern redesign) ✅ — CI green

### Done this session

#### R-13 Research (7-topic deep-dive sub-agent)
Diagnosed + provided fixes for 7 user-reported UI issues. Key findings:
1. **Bottom nav blur**: `backgroundColor = Color.Transparent` was the root cause — Haze needs an OPAQUE backing for text to blur. (Confirmed by Haze GitHub issue #865 + chrisbanes' comment.)
2. **Bottom nav tap-through**: `pressScale` was applied AFTER padding → clickable region was the inner padded box, leaving ~116dp of dead tap zone.
3. **Top bar gradient blur**: Haze 1.1.1 supports `HazeProgressive.verticalGradient(startIntensity, endIntensity)` — a REAL progressive blur (better than the R-5 color scrim).
4. **Modern card design**: overlay title + score on cover via bottom gradient scrim.
5. **Modern Details**: parallax banner, cover thumbnail, genre chips, score ring, action buttons, expandable synopsis.
6. **Modern Search**: filter modal, sort, grid/list toggle, staggered animations.
7. **AniList linking feedback**: success state with ✓ + color change.

#### Fixes applied (4 commits, 4 CI iterations → green)

1. **REAL frosted glass for ALL content** (R-13 Topic 1)
   - BottomBar: `backgroundColor = colors.surface` (opaque, was `Color.Transparent`).
   - `blurRadius = 28.dp`, tint alpha = 0.6 (light frosted glass).
   - Now text + images behind the nav are BOTH frosted.

2. **Bottom nav tap-through fixed** (R-13 Topic 2)
   - `pressScale` applied FIRST (outermost) → full slot is tappable.
   - Every item gets `weight(1f)` — equal slots, no gaps.
   - No `spacedBy` — items butt against each other.
   - No more tap-through between buttons.

3. **Top bar gradient blur edge** (R-13 Topic 3)
   - Added `HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)`.
   - Real progressive blur — full frost at top → no blur at bottom edge.
   - The "gradient blur effect at the bottom" the user wants.

4. **Modern anime cards** (R-13 Topic 4)
   - Title + score overlaid ON the cover via 80dp bottom gradient scrim (transparent → black 70%).
   - Score badge top-right: black pill + ★ star.
   - Press scale 0.96 (gentler).
   - Cover image full bleed (Coil).
   - Gradient fallback when no cover URL.

5. **Modern Details screen** (complete redo — R-13 Topic 5)
   - Parallax banner (280dp, 0.5x scroll speed, blur-on-collapse 0-16dp).
   - 3-stop gradient overlay (black top → transparent → bg bottom).
   - Cover thumbnail (100×150) overlapping banner by -40dp.
   - Title + metadata (format · season year · episodes).
   - Genre chips (horizontal LazyRow, pill-shaped, surfaceVariant bg).
   - Score display + "Add to List" action button (primary color).
   - Expandable synopsis (GlassCard, "Read more"/"Show less").
   - Episode list with real thumbnails (Coil) + title + air date + synopsis.
   - Back button (top-left, status-bar padded, frosted bg).

6. **Modern Home screen**
   - Trending Now as horizontal scroll carousel (LazyRow, 140dp cards).
   - Your Stats in GlassCard.
   - Currently Watching list.

7. **AniList linking feedback** (R-13 Topic 7)
   - Settings: "AniList Account" subtitle shows "✓ Linked — tap to view profile" in success color (green) when authenticated.
   - "Not linked — tap to connect" in tertiary color when not.
   - Subtitle color changes based on auth state.

### CI builds
- Run #58: ✅ (R-13 core fixes — frosted glass + tap-through + gradient blur + cards)
- Run #59: FAILURE (missing imports: statusBarsPadding, width, LocalShapes)
- Run #60: ✅ (modern Details + Home + Settings — commit `eface6d`)
- APK artifact: 10.7MB

### What's improved
- Bottom nav now frosts ALL content (text + images) — not just images.
- No tap-through gaps between bottom nav buttons.
- Top bar has a gradient blur edge (full frost → no blur at bottom).
- Anime cards are modern (overlay design, gradient scrim, score badge).
- Details screen is completely redone (parallax, chips, actions, synopsis).
- Home screen has a horizontal trending carousel.
- AniList linking shows clear visual feedback (✓ + green color).

### Deferred (next session)
1. Search page improvements (filter modal, sort, grid/list toggle, staggered animations).
2. Profile page improvements (modern layout, animated count-up stats).
3. Smooth, snappy animations throughout (screen transitions, card press, etc.).

### Phase map
- **Phase 0** ✅: Planning / Setup / Research
- **Phase 1** ✅: Project scaffolding + design system + frosted glass + header + fonts + modern UI
- **Phase 2** ✅: Data layer (Room + AniList + Kitsu/Jikan + repositories)
- **Phase 3** ✅: Real data on all screens + ViewModels + Coil images + Profile with charts
- **Phase 3.5** ✅: Kitsu/Jikan episode metadata + real Viewer stats + logging screen
- **Phase 3.6** (next): Search improvements + Profile improvements + animations
- **Phase 4**: AI agent port + Design Studio
- **Phase 5**: Backup/restore + dynamic theming
- **Phase 6**: Polish

---

## How to read this file at session start

1. Read the top section (most recent session).
2. Read `decisions.md` (✅ confirmed vs 🟡 proposed vs 🔵 deferred).
3. Read `open-questions.md` (any new blocking questions?).
4. Grep `lessons-learned.md` for tags matching your current task type.
5. If the sandbox feels off, follow CORE_RULES §10 (re-clone from GitHub).
