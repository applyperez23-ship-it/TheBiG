# Implementation Plan - TheBiG v1.3 Refactoring & Engine Hardening [COMPLETED]

Architectural upgrade and logic hardening for TheBiG, delivering crash-proof media management, official 4-mode tournament scheduling algorithms, isolated tournament finances with automatic 100% prize balancing, dedicated `.tb` backup format, fluid shield retouching, and a two-step goal/assist incident workflow.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Based on your preferences from Phase 1, the following choices were established and implemented:
> - **Hybrid Mode Advancement**: The top 2 teams from each group qualify directly to the knockout stage (quarterfinals or semifinals depending on group count).
> - **Backup Format**: Dedicated `.tb` binary/JSON custom package format used exclusively for app and tournament backups.
> - **League Scheduling**: Single round (Ida) by default, with an instant toggle for Home-and-Away (Ida y Vuelta) double round-robin.

---

### Implementation Progress & Completed Modules

- [x] **Version Control & App Identity**:
  - `versionCode = 3`, `versionName = "1.3"` in `app/build.gradle.kts`.
  - App name is exclusively "TheBiG" across `metadata.json`, `strings.xml`, `MainActivity`, and all UI headers.
  - Creator credit "Daniel (el Grande)" preserved once and subtly in "Ajustes / Acerca de"; removed from tournament titles, tables, and fixtures.
  - App starts with 0 fake data (0 tournaments, 0 teams, 0 players).
  - Minimalist vector app icon representing a tournament cup split diagonally by a clean line.

- [x] **Safe Media & Crash-Proof Image Pipeline**:
  - `TeamCrestHelper`: Implemented `takePersistableUriPermission`, background decoding on `Dispatchers.IO`, `inSampleSize` downsampling, and internal app file caching.
  - `ChampionStudioHelper`: Memory-safe sampled bitmap decoding for high-res camera pictures, Spanish date formatting ("Octubre 2026"), and official "TheBiG" watermark.
  - `OfflineShieldRemover` & `ShieldEditorDialog`: Touch-interpolated brush drag without gaps, 6dp–48dp brush size slider, and 5-step undo stack.

- [x] **Tournament Scheduling Engine (4 Official Modes)**:
  - `TournamentScheduler.kt`:
    1. *Liga (Round-Robin)*: Berger algorithm with bye rounds for odd teams, supporting Single Round (Ida) or Home-and-Away (Ida y Vuelta).
    2. *Copa (Eliminatoria Directa)*: Full bracket hierarchy (16avos -> 8avos -> Cuartos -> Semis -> Final) with automatic winner progression.
    3. *Híbrido (Fase de Grupos + Playoff)*: Groups of 3–5 teams with top 2 advancing to the knockout playoffs.
    4. *Mixto (Champions + Consolación)*: Split into main cup and consolation cup for non-advancing teams.
  - `BracketDiagramView.kt`: Interactive Compose canvas visualizer showing match scores, shields, and progression lines.

- [x] **Finances & Auto-Balancing 100% Prizes**:
  - `PrizeCalculator.rebalancePercentages()` dynamically adjusts remaining prize categories so the sum is guaranteed to equal exactly 100%.
  - Isolated tournament finances tab with player fee checklist.
  - Removed duplicate fee indicators from individual team player cards.

- [x] **Live Match Refereeing & Dual-Modal Incident Flow**:
  - `MatchTimerService`: Reads configured match duration (1T / Descanso / 2T), avoiding hardcoded 20-minute defaults.
  - High-contrast interactive scoreboard with one-tap scoring.
  - 2-Step Goal Modal: Select Scorer first -> Select Assister second (or "Sin Asistencia" button). Enforces non-null player IDs.

- [x] **Exclusive `.tb` Backups & QR Spectator Mode**:
  - `TheBiGBackupHelper.kt`: Dedicated `.tb` archive format containing full database entities and embedded Base64 custom club crests.
  - Network connectivity validation before launching QR Spectator mode.

- [x] **Support & Secret Developer Panel**:
  - "Comunicar una sugerencia o error" dialog with internal ticket submission and WhatsApp link (+53 63270465 - Daniel el Grande).
  - Secret developer panel protected by password `XhQzniwJ` displaying received feedback logs.
