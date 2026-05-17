# CLAUDE.md — xpotrack

Local-only Android notes + tasks app. Kotlin + Compose, Room behind SQLCipher, no network.

## Read these first
- `docs/goal.md` — original technical plan (stack, data model, alarms, encryption, export)
- `docs/design-spec.md` — palette, type, screen priority, **fidelity rule** (§1a), Compose-vs-CSS gaps (§10)
- `docs/progress.md` — running log of every milestone shipped + the roadmap at the bottom
- `misc/mockups/screens/*.jsx` — the design source of truth. Every screen must match these.

## Non-negotiable rules
- **Design fidelity is exact, no compromises.** Mockups are the spec. CSS-vs-Compose gaps are flagged in `design-spec.md` §10 and resolved with the user before deviation.
- **Local only.** No network code. No analytics. No cloud. Plan §1.
- **Milestone discipline.** Build + install + on-device verify before each next milestone. Never accumulate untested code.
- Follow `~/.claude/CLAUDE.md` (Karpathy rules) — surgical changes, simplicity first, files near 200 lines.

## Stack
- Kotlin 2.1.0, Compose BOM 2024.12, AGP 8.7.3, JDK 21, Gradle 8.10.2
- `compileSdk = 35`, `minSdk = 29`, `targetSdk = 35`
- Room 2.6.1 + SQLCipher 4.6.1 (whole-DB encryption)
- AndroidX EncryptedSharedPreferences for the 32-byte DB passphrase
- KSP for Room codegen, no Hilt (manual DI via `XpApp`)

## Project shape
```
app/src/main/java/com/xpotrack/app/
  MainActivity.kt · XpApp.kt           # entry + DI container
  data/
    model/ReminderLevel.kt             # pure domain enum
    db/{Entities,Daos,XpDatabase}.kt   # Room + SQLCipher wiring
    repo/{Notes,Tasks}Repository.kt    # entity ↔ UI model mapping
    repo/SeedData.kt                   # mockup data on first launch
    security/PassphraseStore.kt        # Keystore-backed key
  ui/
    AppRoot.kt                         # tab state + content router
    theme/{XpTokens,XpTheme,XpTypography}.kt
    components/{XpBottomTabs, XpReminderPill, ReminderStyle}.kt
    notes/{NotesData, NotesListScreen, NotesCategoryView,
           NotesChronoView, QuickEntryStrip, NotesViewModel}.kt
    tasks/{TasksData, TasksTimelineScreen, DayChips, Timeline, TasksViewModel}.kt
    vault/VaultStubScreen.kt           # placeholder until milestone 10
    more/MoreStubScreen.kt             # placeholder until milestone 13
app/src/main/res/
  drawable/ic_*.xml    font/geist_*.ttf    values/{strings,themes}.xml
```

## Build & deploy
```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

Phone is iQOO Neo 7 (Android 14, SDK 34) over wireless adb on LAN. Tailscale + Arch's adb do not pair (mDNS stripped from `android-tools 35.0.2` + CGNAT mDNS quirks). For a fresh device: pair on same Wi-Fi with `adb pair IP:PORT CODE` then `adb connect IP:PORT`.

`local.properties` (gitignored) must set `sdk.dir=/home/xpo/Android/Sdk`.

## Layering
```
ui  ──imports──→  data
data ──no imports──→  ui*
```
*Exception: repositories import `NoteRow` / `TaskRow` from `ui/notes` and `ui/tasks`. Those are plain data classes with no Compose deps. Move to a `domain/` package when a third feature surface forces the issue (likely milestone 8 — alarms need a domain `Task`).

## Conventions
- Tokens from `system.jsx` live in `XpTokens.kt` — every color is hex-equivalent to the mockup CSS.
- Vector drawables (`res/drawable/ic_*.xml`) for SVG icons. `Icon` composable applies `tint` per call site.
- Geist + Geist Mono bundled as `.ttf` in `res/font/`. No Google Fonts at runtime.
- No `Modifier.shadow()` for teal glow — Compose shadows are grayscale. Use stacked `drawBehind` radial gradients (see `design-spec.md` §10).
- DAOs return `Flow<List<…>>`. ViewModels expose `StateFlow` via `stateIn(WhileSubscribed(5_000))`. UI collects with `collectAsStateWithLifecycle()`.
- Seed runs once per fresh install (`dao.count() == 0` guard). Uninstall destroys the Keystore key and makes the DB unrecoverable — feature, not bug.

## Where we are
Milestones 1–5 + audit shipped. Notes + Tasks read from an encrypted DB behind a 4-tab nav. Vault and More are placeholders. No writes yet — the FAB renders but does nothing. Next: milestone 6 (notes editor + first writes). Full roadmap in `docs/progress.md`.
