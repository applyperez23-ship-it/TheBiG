# Implementation Plan - TheBiG v1.7 Refactoring & Architectural Upgrade

Comprehensive architectural and user experience upgrade for **TheBiG** (v1.7), delivering bug-free numeric inputs, crash-proof QR network validation, smart exterior contour shield eraser, 10 new professional football crests with dedicated color picker, dual financial mode (per-team or per-player) with exact 10-unit rounding, guaranteed Round-Robin scheduling with 3-0 forfait handling for retired clubs, standalone friendly matches, a rock-solid fixed foreground service referee notification, structured `.tb` ZIP backups, an ultra-compact 2-column goal/assist selector, 3-mode champion photo studio, and a global Material 3 Expressive motion engine.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Based on your preferences from Phase 1, the following choices have been confirmed and incorporated:
> - **Duplicate Team Handling on `.tb` Import**: Prompt user with a clean conflict dialog to choose between *Sobrescribir* (update existing team) or *Crear Copia* (import with new unique ID).
> - **Friendly Quick Match Statistics**: Friendly and quick matches record full match events, and their goals, assists, and disciplinary records are merged into overall player career statistics.
> - **Goal and Assist Selector Layout**: Rendered as an ultra-compact two-column clean grid highlighting player names with minimal jersey icons and a quick "Sin Asistencia" button, fitting completely in a single screen without vertical scrolling.

---

### 1. Overview & Core Concept

**TheBiG (v1.7)** is a professional tournament management, refereeing, and statistics suite for amateur and grassroots football. This release focuses on eliminating usability frictions, preventing all reported crashes, enhancing visual prestige, and standardizing competition logic:
- **Flawless Numeric Inputs**: Backspace works naturally on first press across all numeric inputs (dorsal, players count, amounts) without having to overwrite numbers.
- **Resilient Offline Shield Retouching**: Real-time fluid touch dragging with stroke interpolation and an exterior contour-detection algorithm that erases backgrounds without touching internal shield designs.
- **10 Professional Vector Crests**: Brand new vector shields with dedicated primary color picker and upper/central monogram zones (up to 3 letters).
- **Dual Financial Mode**: Choose between fixed fee per team or unit fee per player (auto-calculated by roster limit), with strict rounding to tens ($2000 instead of $2004.24) and hierarchical prize precedence (Campeón > Subcampeón > Resto).
- **Guaranteed Round-Robin & Retired Teams**: Strict Berger scheduling ensuring all teams play every round. If a team drops out mid-tournament, all remaining fixtures are awarded as 3-0 forfait victories to their opponents.
- **Fixed Referee Notification**: Notification drawer clock remains completely static and fixed in place using high-priority ongoing status flags.
- **Structured `.tb` ZIP Container**: True compressed package storing tournaments, rosters, financials, and high-fidelity transparent shield PNGs with correct upload/download icon orientations.
- **M3 Expressive Motion Engine**: Fluid microinteractions, curved spinning football match start animation, and a master toggle in Settings to disable all animations for performance saving.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Club Creator & Shield Customizer**:
   - User browses 10 brand new modern vector shields (Classic Iberian, Premier, French Chevron, Swiss Ears, German Heptagonal, Gothic Sash, Calcio Oval, Castle Battlement, Diamond Dynasty, Centenary Laurel).
   - Dedicated color picker modifies the shield's primary vector tone independently from jersey colors.
   - For uploaded crests, the smart eraser runs exterior flood-fill contour detection to strip only the outer background, leaving white stars, footballs, or inner stripes intact.
2. **Tournament Wizard with Dual Financials**:
   - Mode 1: Fixed fee per Team.
   - Mode 2: Unit fee per Player ($X × players per team limit).
   - Dynamic 100% prize distribution strictly preserves hierarchy (Campeón ≥ Subcampeón) and rounds final prizes to exact multiples of 10.
3. **Live Match & Instant 2-Step Goal Modal**:
   - Scoreboard tap triggers Step 1: 2-column compact grid showing player names in prominent typography with mini jersey icons.
   - Immediate auto-transition to Step 2 (Assister) with a dedicated top button: "⚽ Sin Asistencia".
   - Foreground notification stays rock-solid on the status bar without jumping or vibrating erratically.
4. **Friendly / Quick Matches**:
   - Direct button in the Fixture tab to play a quick friendly match outside any tournament, while seamlessly accumulating player stats.
5. **Champion Photo Studio**:
   - Mode 1 (*Mitad y Mitad*): Photo cropped on one half, tournament laurels/stats on the other.
   - Mode 2 (*Contenido Central*): Photo framed in the center with customizable scale.
   - Mode 3 (*Fondo Completo*): Full-bleed background with transparent "TheBiG • [Mes Año]" watermark.

#### Theme & Typographic Hierarchy
- **Adaptive Theme & Status Bar**: Automatic system theme detection with manual toggle. In Light Mode, `WindowInsetsController` sets `isAppearanceLightStatusBars = true` to guarantee dark, legible icons against white backgrounds.
- **Clean Typography**: Elimination of bracketed text (e.g. `[ ] #1 Daniel` -> clean bold player names with dorsal embedded in miniature jersey vectors).

---

### 3. Key Product Decisions & Trade-Offs

- **Numeric Input State Management**:
  - *Chosen Approach*: Maintain string state in text fields allowing empty string `""` on backspace, converting to integer/double only for calculations or on blur.
  - *Why*: Solves the stubborn backspace bug where parsing `it.toIntOrNull() ?: 0` constantly forced `"0"` back into the field on every keystroke.
- **Exterior Contour Flood-Fill for Shields**:
  - *Chosen Approach*: Boundary-connected Breadth-First Search (BFS) seeded from the image borders (x=0, y=0, etc.) with color tolerance.
  - *Why*: Prevents destroying white/light elements situated inside the shield body (e.g. white football or yellow star) while cleanly deleting the exterior photo canvas.
- **True ZIP Container for `.tb` Files**:
  - *Chosen Approach*: Package JSON databases and PNG images into a standard ZIP archive renamed `.tb`.
  - *Why*: Eliminates giant Base64 text payloads, maintains full transparency support, and ensures tiny file sizes during WhatsApp or local sharing.
- **In-Place Bitmap Mutation for Eraser/Brush**:
  - *Chosen Approach*: Mutate the active bitmap directly and trigger recomposition via an integer state counter (`renderTrigger++`) instead of `bitmap.copy()`.
  - *Why*: Stops GC thread blocking and ensures 60 FPS fluid drag response under the user's finger.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          TheBiG Architecture                           │
│                                                                        │
│  ┌───────────────────────┐   ┌──────────────────────────────────────┐  │
│  │   UI Screens & M3E    │   │         Live Referee Engine          │  │
│  │ (Tournaments, Teams,  │   │ ┌──────────────────────────────────┐ │  │
│  │  Champion Studio 3-way│   │ │ MatchTimerService (Static FGS)   │ │  │
│  │  Friendly Quick Match)│   │ └──────────────────────────────────┘ │  │
│  └───────────┬───────────┘   │ ┌──────────────────────────────────┐ │  │
│              │               │ │ 2-Step Compact Goal/Assist Modal │ │  │
│              │               │ └──────────────────────────────────┘ │  │
│              │               └──────────────────┬───────────────────┘  │
│              │                                  │                      │
│  ┌───────────▼──────────────────────────────────▼───────────────────┐  │
│  │                        Domain Engine                             │  │
│  │  ┌─────────────────────────┐     ┌────────────────────────────┐  │  │
│  │  │   TournamentScheduler   │     │      PrizeCalculator       │  │  │
│  │  │ (Berger Round-Robin,    │     │ (Dual Mode, 10-Unit Round, │  │  │
│  │  │  3-0 Forfait on Retire) │     │  Hierarchical Precedence)  │  │  │
│  │  └─────────────────────────┘     └────────────────────────────┘  │  │
│  └───────────────────────────┬──────────────────────────────────────┘  │
│                              │                                         │
│  ┌───────────────────────────▼──────────────────────────────────────┐  │
│  │                   Persistence & Storage Engine                   │  │
│  │  ┌────────────────────────┐      ┌─────────────────────────────┐ │  │
│  │  │ TheBiGDatabase (Room)  │      │ TheBiGBackupHelper (.tb ZIP)│ │  │
│  │  │ (Tournaments, Teams,   │      │ (JSON Metadata + PNG Shields│ │  │
│  │  │  Matches, Players)     │      │  + Conflict Resolution)     │ │  │
│  │  └────────────────────────┘      └─────────────────────────────┘ │  │
│  │  ┌─────────────────────────────────────────────────────────────┐ │  │
│  │  │ OfflineShieldRemover (BFS Exterior Contour Flood-Fill)      │ │  │
│  │  └─────────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Step-by-Step Implementation Sequence:
1. **Compilation & Version Setup**: Bump `versionCode = 4`, `versionName = "1.7"` in `app/build.gradle.kts`.
2. **Keyboard & Numeric Input Fix**: Create safe numeric text field composables handling backspace without snapping to zero.
3. **Smart Exterior Contour Eraser & Fluid Brush**: Rewrite `OfflineShieldRemover.kt` and `ShieldEditorDialog.kt` to use exterior edge-seeded BFS flood-fill and eliminate per-frame bitmap cloning.
4. **10 Vector Football Crests & Dedicated Color Picker**: Implement 10 professional geometric shield paths in `TeamCrestHelper.kt` with initials support and add primary shield color selection in `TeamsScreen.kt` and `TeamDetailScreen.kt`.
5. **Dual Financial Model & Strict Rounding**: Update `PrizeCalculator.kt` with per-player quota calculation, 10-unit rounding, and hierarchical slider boundaries.
6. **Round-Robin Engine, Forfait Logic & Friendly Matches**: Hardcode Berger rotation guarantees in `TournamentScheduler.kt`, add retired team 3-0 forfeit auto-resolution, and implement standalone Friendly Match launch.
7. **Fixed Foreground Service Notification**: Reconfigure `MatchTimerService.kt` with persistent ongoing flags, priority low, and silent status categories.
8. **Proprietary `.tb` ZIP Container**: Implement ZIP-based `.tb` archive packaging with transparent PNG crests and interactive conflict dialog on duplicate imports.
9. **Ultra-Compact Goal & Assist 2-Column Grid**: Redesign live incident dialogs in `MatchLiveSheetScreen.kt` into a non-scrolling 2-column player matrix.
10. **Status Bar Visibility in Light Theme**: Configure `WindowCompat.getInsetsController` in `Theme.kt` for crisp contrast.
11. **Champion Photo Studio (3 Modes)**: Implement Split-Half, Centered-Contained, and Full-Background modes in `ChampionPhotoStudioScreen.kt`.
12. **M3 Expressive Animation Engine & Settings Toggle**: Implement curved spinning football entrance animation and master animation toggle in Settings.
13. **Verification**: Compile with `compile_applet` and verify zero errors.
