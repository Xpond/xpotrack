# xpotrack — Progress Log

A running log of everything completed, why, and the exact commands/files involved. Newest milestone at the bottom. If anyone (including future-Claude) picks this up cold, this doc + `docs/design-spec.md` + `docs/goal.md` should be enough.

---

## Phase 0 — Planning & design intake

### What we agreed on
- **Plan source:** `docs/goal.md` — TaskNotes technical plan (Kotlin + Compose + Room + SQLCipher, single-module, local-only, no network).
- **Design fidelity rule:** the JSX mockups in `misc/mockups/screens/` are the spec. Every screen must render to match them. Where Compose can't replicate a CSS effect 1:1, the deviation is flagged in `docs/design-spec.md` §10 and resolved before shipping.
- **Toolchain:** CLI only (no IDE). User installs everything themselves.
- **Deploy target:** physical phone over wireless `adb`.
- **First slice:** walking skeleton — proves the stack works before we invest in feature breadth. Tightened mid-session into 5 milestones (see below) after we caught ourselves writing too much code without builds.

### Mockups read end-to-end
All 16 screens in `misc/mockups/screens/`:

| File | Role |
|---|---|
| `system.jsx` | Design tokens (colors, type, spacing, shared components) — the source of truth for `XpTokens.kt` |
| `notes-list.jsx` | Home screen, category & chrono modes |
| `note-editor.jsx` | Hybrid live-markdown editor |
| `markdown-preview.jsx` | Rendered preview state |
| `new-note.jsx` | Empty editor |
| `tasks-timeline.jsx` | Tasks home, hour-grid timeline |
| `task-create.jsx` | 3 variants — **A (bottom sheet)** is the chosen one |
| `task-detail.jsx` | Task detail/edit |
| `alarm.jsx` | Full-screen alarm ring |
| `vault.jsx`, `vault-unlock.jsx`, `locked-note.jsx` | Vault tab |
| `category-manager.jsx` | Bottom sheet |
| `quick-notes.jsx` | 24h ephemeral notes |
| `settings.jsx` | More tab |
| `app.jsx` | Mockup-canvas wiring (not implemented) |

### Design spec written
**File:** `docs/design-spec.md`

Sections cover: palette (hex values pulled from `system.jsx`), typography scale, shape/rhythm tokens, screen inventory & priority (MVP-1 → MVP-4), navigation tree, fidelity-notes list (§10 — Compose-vs-CSS gaps with concrete translation strategies for each: hairline 0.5dp borders, teal glow, gradients, mask gradients, backdrop blur, pulse animations, caret blink, font hosting).

Key design decisions locked here:
1. Task-create UI = **bottom sheet variant** (A in `task-create.jsx`). Variants B (inline composer) and C (natural language) are exploration only.
2. **Vault must render exactly as mocked** — cool gradient bg, hairline teal lock chrome, masked-row secret editor. Underlying data still lives in the notes table with an `isLocked` flag; that's an implementation detail and never surfaces in the UI.
3. Bundle Geist + Geist Mono + Instrument Serif as `.ttf` assets rather than using Google Fonts at runtime — the plan says no network code.
4. Deferred until later passes: Quick notes, linked-note field on tasks, custom category colors, search, pinning, repeat, snooze, category reordering, markdown shortcut auto-expansion, export to `Documents/TaskNotes/`.

---

## Phase 1 — Toolchain & device setup

### Toolchain (user-installed, our reference)
Arch Linux. No prior Android tooling. Installed:

```
sudo pacman -S --needed jdk21-openjdk android-tools unzip
# Google command-line-tools 13114758 unzipped to ~/Android/Sdk/cmdline-tools/latest
# Env in ~/.bashrc:
#   ANDROID_HOME=$HOME/Android/Sdk
#   JAVA_HOME=/usr/lib/jvm/java-21-openjdk
#   PATH += $ANDROID_HOME/{cmdline-tools/latest/bin,platform-tools} + $JAVA_HOME/bin
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Note: AGP 8.7.3 auto-installed `build-tools;34.0.0` on first build too — both are present in the SDK now.

### Wireless adb pairing
- First attempt over **Tailscale** (`100.67.x.x` IP) failed with `protocol fault: Success`. Root cause: `adb pair` uses mDNS-style service discovery on top of TCP, and mDNS doesn't traverse Tailscale. Ping worked (~270ms RTT) but the handshake failed.
- Tried Arch's adb's `mdns check` — Arch strips Bonjour out of `android-tools 35.0.2`, so the `adb mdns` subcommand reports `unknown host service`. QR pairing therefore won't work with this adb.
- Switched to **same Wi-Fi LAN** + manual pairing-code entry. That worked:
  ```
  adb pair 192.168.31.198:38819 405668     # one-shot pairing
  adb connect 192.168.31.198:40381          # persistent endpoint
  ```
- **Connected device:** iQOO Neo 7 (model `I2017`), Android 14, SDK 34.
- Implication: phone is SDK 34, plan targets SDK 35. `targetSdk = 35` is fine — that's a "tested-against" declaration, not a runtime requirement. `minSdk = 29` still covers SDK 34.

---

## Phase 2 — Project scaffold (Milestone 1: launchable hello-world)

### Scope tightening (mid-session course-correct)
Original plan: scaffold project + Room + SQLCipher + two real screens, then build once at the end. After writing several files I noticed I was accumulating untested code with broken APIs (e.g. a homemade `PathBuilder.getNodes()` extension that doesn't exist). Re-cut into 5 milestones, each ending in a verified on-device build:

1. **Hello-world that launches** ← this milestone
2. Notes list screen (hardcoded data)
3. Tasks timeline screen (hardcoded data)
4. Bottom-tab navigation between them + Vault/More placeholders
5. Room + SQLCipher swapped in behind the screens

This trades a few more `./gradlew assembleDebug` cycles for near-zero risk of compounding mistakes. Matches the Karpathy rules (`~/.claude/CLAUDE.md`): "Goal-driven execution. Loop until verified."

### Project structure written
```
xpotrack/
├── .gitignore                    # adds .gradle/, build/, local.properties, .kotlin/, *.apk
├── build.gradle.kts              # root — declares plugins (apply false)
├── settings.gradle.kts           # repos + module list (only :app for now)
├── gradle.properties             # JVM args, parallel/cached builds, AndroidX on
├── gradlew, gradlew.bat          # downloaded from gradle/gradle v8.10.2 tag
├── gradle/
│   ├── libs.versions.toml        # version catalog (AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12)
│   └── wrapper/
│       ├── gradle-wrapper.jar    # downloaded from gradle/gradle v8.10.2 tag
│       └── gradle-wrapper.properties   # gradle-8.10.2-bin.zip
├── local.properties              # sdk.dir=/home/xpo/Android/Sdk (gitignored)
├── docs/
│   ├── goal.md                   # original technical plan
│   ├── design-spec.md            # design system + screen priority + fidelity notes
│   └── progress.md               # this file
├── misc/mockups/                 # design source (gitignored)
└── app/
    ├── build.gradle.kts          # Android app config: compileSdk 35, minSdk 29, JVM 17
    ├── proguard-rules.pro        # placeholder; minify off
    └── src/main/
        ├── AndroidManifest.xml   # single MainActivity, no permissions yet
        ├── res/values/
        │   ├── strings.xml       # app_name = "xpotrack"
        │   └── themes.xml        # Theme.Xpotrack: Material no-action-bar, transparent system bars
        └── java/com/xpotrack/app/
            ├── MainActivity.kt   # edge-to-edge + XpTheme + a centered teal dot
            └── ui/theme/
                ├── XpTokens.kt   # every token from system.jsx → Compose Color/Dp/Sp
                ├── XpTheme.kt    # MaterialTheme darkColorScheme wrapper
                └── XpTypography.kt   # Material 3 type scale tuned to spec §3
```

### Build & run sequence

```
./gradlew --version          # downloads Gradle 8.10.2 first time, ~1 min
./gradlew assembleDebug      # builds debug APK, ~1m 41s first build
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

### What's on the phone right now
Fully edge-to-edge dark screen, `#06100F` background, a 64dp teal (`#5EEAD4`) circle centered. App label `xpotrack` in the launcher, default Android robot icon (not customized yet).

### What this verifies
- JDK 21 + Gradle 8.10.2 + AGP 8.7.3 + Kotlin 2.1.0 + Compose BOM 2024.12 all compose together cleanly.
- `XpTokens.Bg` and `XpTokens.Teal` resolve to the exact hex values from `system.jsx`.
- Edge-to-edge rendering + transparent status/nav bars works on the target device.
- Wireless `adb install` round-trips work consistently.

### Known caveats / things we'll fix later
- **App icon** is the default robot. Replace with a real adaptive icon in a later pass.
- **Geist fonts** are not bundled yet — we're on `FontFamily.SansSerif` / `FontFamily.Monospace`. Add `.ttf` files to `app/src/main/res/font/` before any screen that pixel-matches type.
- **No Room / SQLCipher** wired yet. Dependencies were removed from `app/build.gradle.kts` for milestone 1 to reduce surface area; they come back in milestone 5.
- **`gradle.properties`** has `org.gradle.parallel=true` and caching on — first build was 1m 41s, subsequent should drop to seconds.

---

---

## Phase 3 — Milestone 2: Notes list screen (no DB)

### Goal
Render `misc/mockups/screens/notes-list.jsx` (category mode) on the phone with hardcoded data. No database, no navigation — just one screen, against the mockup, to verify our token/spacing translation works before we layer more on.

### Geist font bundled
Mockup uses Geist + Geist Mono. Pulled official `.ttf` files from `github.com/vercel/geist-font` (OFL-licensed) into `app/src/main/res/font/`:

```
geist_regular.ttf, geist_medium.ttf, geist_semibold.ttf, geist_bold.ttf
geist_mono_regular.ttf, geist_mono_medium.ttf
```

~810KB total. `XpTypography.kt` now references `FontFamily(Font(R.font.geist_*, …))` rather than `FontFamily.SansSerif`. No Google Fonts, no network — matches the plan's "no network code" rule.

### Vector drawables for icons
Recreated the SVG paths from the mockups as Android vector drawables in `app/src/main/res/drawable/`. Faster than wrestling with `ImageVector.Builder` in Kotlin and they render identically:

```
ic_search.xml      ic_sort_date.xml    ic_grouped.xml    ic_plus.xml
ic_lightning.xml   ic_chevron_right.xml ic_star.xml
```

`strokeColor="#FFFFFFFF"` on all of them — the Compose `Icon` composable applies its `tint` parameter to recolor them, so we tint with `XpTokens.Teal` / `XpTokens.Ink3` / etc. at call sites.

### Screen split into 4 files
To stay near the 200-line target from `~/.claude/CLAUDE.md`:

```
app/src/main/java/com/xpotrack/app/ui/notes/
├── NotesData.kt         # NoteRow + Category data classes + NotesDb / Categories / PinnedIds
├── NotesListScreen.kt   # Top-level layout, header, mode toggle, top-halo glow, FAB
├── NotesCategoryView.kt # Pinned horizontal strip, CategoryGroup, CategoryNoteRow, NewCategory btn
├── NotesChronoView.kt   # Flat chronological list (toggle target — basic but works)
└── QuickEntryStrip.kt   # Dashed teal "Quick" pill at top of both views
```

Data file mirrors `NOTES_DB`, `PINNED_IDS`, `CATEGORIES` from `notes-list.jsx` verbatim so we can A/B against the mockup with the same content.

`MainActivity.kt` now renders `NotesListScreen` with `Modifier.systemBarsPadding()` so content sits below the status bar.

### Build & install
`./gradlew assembleDebug` finished in **5 seconds** on the incremental rebuild — compiled cleanly first try (no compile errors despite ~500 lines of new Compose code, which I attribute to having read all the mockups first and the tighter milestone discipline).

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

### What's verified on the phone
- Geist + Geist Mono render correctly at every weight used
- `XpTokens` palette maps 1:1 to the mockup
- Category mode renders: header eyebrow, h1, search/sort buttons, mode-strip cue, quick-entry strip, pinned section with horizontal scroll, category groups (Personal/Work/Ideas/Inbox/Trip/Essay/Recipe), correct count chips, "+ N more" overflow, "New category" dashed button
- FAB renders at the right position (low for now since there's no tab bar)
- Sort toggle works — tapping it swaps to a basic chronological view
- User-approved as "good enough for now" — moving on

### Deferred from this milestone (will revisit when relevant)
- **FAB teal glow shadow.** `Modifier.shadow()` is grayscale; the mockup wants a teal halo. Spec §10 has the `drawBehind` radial-gradient fix; not blocking, deferred.
- **True 0.5dp hairlines.** Currently render as 1px on this density. Will only fix if it visually bothers us — most users will never notice.
- **Sort-toggle border-color animation.** Switches color discretely; mockup has a soft transition. Trivial to add later.
- **Quick-entry strip dashed border.** Using a solid teal-alpha border for now — Compose has no built-in dashed border. Will add a custom `Modifier.dashedBorder()` when polish pass happens.
- **Category chip pill spacing.** Approximated; not pixel-perfect. Deferred.

### Project tree after milestone 2
```
app/src/main/
├── AndroidManifest.xml
├── java/com/xpotrack/app/
│   ├── MainActivity.kt
│   └── ui/
│       ├── theme/{XpTokens.kt, XpTheme.kt, XpTypography.kt}
│       └── notes/{NotesData.kt, NotesListScreen.kt, NotesCategoryView.kt,
│                  NotesChronoView.kt, QuickEntryStrip.kt}
└── res/
    ├── drawable/ic_*.xml         # 7 vector icons
    ├── font/geist_*.ttf          # 6 font files
    └── values/{strings.xml, themes.xml}
```

---

---

## Phase 4 — Milestone 3: Tasks timeline screen (no DB)

### Goal
Render `misc/mockups/screens/tasks-timeline.jsx` on the phone with hardcoded data. Continue the milestone-2 pattern: one screen, hardcoded data, build, install, compare.

### Files added

```
app/src/main/java/com/xpotrack/app/
├── ui/components/XpReminderPill.kt    # Shared pill component (level + time chip)
└── ui/tasks/
    ├── TasksData.kt                    # ReminderLevel enum, TaskRow, TasksDb, timeline math
    ├── TasksTimelineScreen.kt          # Top-level layout + header (Today + 4/9 counter)
    ├── DayChips.kt                     # 7-day chip strip with Friday active
    └── Timeline.kt                     # Hour grid + task pills + NOW indicator
```

Three new vector drawables in `app/src/main/res/drawable/`:

```
ic_reminder_silent.xml    crossed-out bell
ic_reminder_notify.xml    bell
ic_reminder_alarm.xml     alarm clock with ringers
ic_check.xml              checkmark for done tasks
```

`MainActivity.kt` temporarily renders `TasksTimelineScreen` (proper navigation lands in milestone 4).

### Implementation notes
- **Reminder levels are first-class data.** `ReminderLevel` enum carries its own `accent` color, `tint`, `cardBg`, and `iconRes` so the per-level styling never needs branching in the UI code.
- **Timeline math is centralized.** `TimelineStartHour=6`, `TimelineEndHour=22`, `HourHeightDp=56`. `timeToOffsetDp("09:15")` is the single function that translates `HH:mm` to a vertical offset. Everything else (`HourGrid`, `TaskPill`, `NowIndicator`) calls into it.
- **Absolute positioning via `Modifier.offset(x, y)`.** Matches the CSS `position: absolute; top: X` from the mockup. Compose `Layout` would be more "idiomatic" but `offset` is simpler and translates the mockup 1:1.
- **`XpReminderPill` is shared.** Both timeline pills (size `Sm`) and the upcoming task-detail screen (size `Md`) use it.
- **Done tasks** dim to 45% opacity, strikethrough the label, drop the card border + background, and tint the rail at 30%. One conditional `alpha` modifier on the row + per-element `if (task.done)` branches.

### Build & install
Built in **4s** incremental. Compiled cleanly first try.

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

### What's verified on the phone
- Header: "FRIDAY · MAY 16" eyebrow + "Today" h1; mono "4/9" counter with teal numerator and ink3 denominator + "DONE" cue
- 7-day chip strip with Friday active (teal border + tint) and Thursday faded
- Hour labels 6 AM → 10 PM rendering with their hairline dividers at the correct heights
- 9 tasks pinned to their times with the right rail color and card tint per reminder level
- Done tasks (Morning pages, Stretch + coffee) dimmed with strikethrough + checkmark
- NOW indicator at 9:41 with the teal dot and horizontal line
- User-approved as "good enough" — moving on

### Deferred from this milestone
- **NOW dot outer halo.** Mockup has `box-shadow: 0 0 0 4px rgba(94,234,212,0.18)` — same Compose-shadow limitation as the FAB. Will fix when we do a polish pass on the timeline.
- **Hour-label letter spacing.** Mockup sets `letterSpacing: 0.05em` on hour labels; I dropped it after a botched `em` extension at first build. The label-medium type style still has its own letter spacing so it reads correctly, just not pixel-identical.
- **Task pill animations.** No tap feedback / state transitions yet.
- **Scroll-to-now on open.** Timeline opens scrolled to 6 AM; should auto-scroll so 9:41 is near the top.
- **The day chip strip is hardcoded** to Thu–Wed of week-of-May-16. Real date math lands when tasks come from the DB.

### Project tree after milestone 3
```
app/src/main/
├── AndroidManifest.xml
├── java/com/xpotrack/app/
│   ├── MainActivity.kt
│   └── ui/
│       ├── theme/{XpTokens.kt, XpTheme.kt, XpTypography.kt}
│       ├── components/XpReminderPill.kt
│       ├── notes/{NotesData.kt, NotesListScreen.kt, NotesCategoryView.kt,
│       │          NotesChronoView.kt, QuickEntryStrip.kt}
│       └── tasks/{TasksData.kt, TasksTimelineScreen.kt, DayChips.kt, Timeline.kt}
└── res/
    ├── drawable/ic_*.xml            # 11 vector icons
    ├── font/geist_*.ttf             # 6 font files
    └── values/{strings.xml, themes.xml}
```

---

---

## Phase 5 — Milestone 4: Bottom tab navigation

### Goal
Connect Notes and Tasks behind a bottom tab bar matching the mockup, plus add placeholder screens for Vault and More so all 4 tabs are reachable.

### Files added

```
app/src/main/java/com/xpotrack/app/
├── ui/AppRoot.kt                       # Hosts active-tab state + lays out content above tabs
├── ui/components/XpBottomTabs.kt       # The tab bar component (4 tabs, gradient fade, hairline top)
├── ui/vault/VaultStubScreen.kt         # "Coming in milestone-3 of the design plan" placeholder
└── ui/more/MoreStubScreen.kt           # "Coming later" placeholder
```

Four new tab-icon drawables:

```
ic_tab_notes.xml    ic_tab_tasks.xml    ic_tab_vault.xml    ic_tab_settings.xml
```

`MainActivity.kt` simplifies to just `XpTheme { AppRoot() }` — all layout (system-bar padding, root background) moved into `AppRoot`.

### Implementation notes

- **No Navigation-Compose yet.** With 4 tabs and no deep linking, a `var active by rememberSaveable { mutableStateOf(XpTab.Notes) }` + `when (active)` block is simpler and faster to compile. Will reach for Navigation-Compose when we need stacking (note editor pushed over notes list, task detail pushed over timeline, etc.).
- **`rememberSaveable`** so the active tab survives configuration changes and process death.
- **Tab bar lives in `XpBottomTabs.kt`** with a hairline top divider (`Hair` token) and a vertical gradient fade from transparent to `Bg` so content scrolling underneath blurs into the bar (matches `.xp-bottomtabs` from `system.jsx`).
- **Active tab is just a color swap** — icon + label both shift to `Teal`. Inactive tabs use `Ink3`. No ripple/indicator overlay; we suppress the ripple with `indication = null` to match the mockup's calm aesthetic.
- **Vault stub** uses the cool gradient background (`#050D0C → Bg`) from the real vault mockup, so even the placeholder telegraphs "separate space" — keeps the design promise alive while the full screen waits.

### Build & install
Built in **3s** incremental. Compiled clean.

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

### What's verified on the phone

- App opens on Notes (default tab)
- Bottom tab bar renders with NOTES (active, teal) · TASKS · VAULT · MORE
- Tapping each tab swaps the content and updates the active highlight
- Vault tab shows the cool-gradient placeholder
- More tab shows the plain-bg placeholder
- FAB on Notes and Tasks correctly sits above the tab bar
- User-approved as "quite good" — moving on

### Deferred from this milestone

- **No tab-switch animation.** Cross-fade or slide would be nice; not blocking.
- **No deep-link / back-stack handling.** Pressing system back from Tasks doesn't return to Notes — it exits the app. Will land with Navigation-Compose when we add the editor/detail destinations.
- **Real Vault and More screens.** Still placeholders. Will be built when those features come online.
- **Notes/Tasks screens reset on tab switch** in the worst case — scroll position survives because `rememberScrollState` is composition-scoped to the `when` branch, but if Compose decides to dispose the branch we lose it. Will revisit if it actually annoys.

### Project tree after milestone 4

```
app/src/main/
├── AndroidManifest.xml
├── java/com/xpotrack/app/
│   ├── MainActivity.kt                 # XpTheme + AppRoot
│   └── ui/
│       ├── AppRoot.kt                  # Tab state + content router
│       ├── theme/{XpTokens.kt, XpTheme.kt, XpTypography.kt}
│       ├── components/{XpBottomTabs.kt, XpReminderPill.kt}
│       ├── notes/{NotesData.kt, NotesListScreen.kt, NotesCategoryView.kt,
│       │          NotesChronoView.kt, QuickEntryStrip.kt}
│       ├── tasks/{TasksData.kt, TasksTimelineScreen.kt, DayChips.kt, Timeline.kt}
│       ├── vault/VaultStubScreen.kt
│       └── more/MoreStubScreen.kt
└── res/
    ├── drawable/ic_*.xml                # 15 vector icons
    ├── font/geist_*.ttf                 # 6 font files
    └── values/{strings.xml, themes.xml}
```

---

---

## Phase 6 — Milestone 5: Room + SQLCipher backing the screens

### Goal
Replace the hardcoded `NotesDb` / `TasksDb` constants with a real, encrypted Room database. Screens read via ViewModels exposing `StateFlow<List<…>>`. Database file encrypted at rest with SQLCipher, key generated once on first launch.

### Decisions locked at the start of this milestone
- **Key storage:** 32-byte random passphrase in AndroidX `EncryptedSharedPreferences` (Keystore-backed master key, `AES256_GCM` scheme). Industry-standard pattern for SQLCipher keys. Survives app updates; destroyed by uninstall (which makes the DB unrecoverable — that's by design).
- **Seeding:** mockup data is inserted on first launch only, so visual comparison against the mockup keeps working. Subsequent launches load from DB only.

### Files added

```
app/src/main/java/com/xpotrack/app/
├── XpApp.kt                            # Application subclass — DI container (notesRepo, tasksRepo)
├── data/security/PassphraseStore.kt    # Keystore-backed 32-byte passphrase, get-or-create
├── data/db/
│   ├── Entities.kt                     # NoteEntity, TaskEntity, MetaEntity
│   ├── Daos.kt                         # NoteDao, TaskDao, MetaDao — Flow-returning observers
│   └── XpDatabase.kt                   # @Database; SQLCipher SupportOpenHelperFactory wiring
└── data/repo/
    ├── NotesRepository.kt              # entity → NoteRow mapping, observeAll(), seedIfEmpty()
    ├── TasksRepository.kt              # entity → TaskRow mapping, observeAll(), seedIfEmpty()
    └── SeedData.kt                     # 9 notes + 9 tasks lifted from the mockup
ui/notes/NotesViewModel.kt              # exposes StateFlow<List<NoteRow>>
ui/tasks/TasksViewModel.kt              # exposes StateFlow<List<TaskRow>>
```

### Files changed
- `gradle/libs.versions.toml` — re-added `room`, `sqlcipher`, `sqliteKtx`, `securityCrypto`, `ksp` plugin, plus `lifecycle-runtime-compose` so we get `collectAsStateWithLifecycle()`.
- `build.gradle.kts` (root) + `app/build.gradle.kts` — re-enabled KSP, added the new deps.
- `AndroidManifest.xml` — `android:name=".XpApp"` on the `<application>` tag.
- `MainActivity.kt` — unchanged behaviorally; still just `XpTheme { AppRoot() }`.
- `ui/AppRoot.kt` — now resolves `NotesViewModel` and `TasksViewModel` via `viewModel(factory = …)`, collects state with `collectAsStateWithLifecycle()`, passes `notes`/`tasks` lists down.
- `ui/notes/NotesListScreen.kt` + `NotesCategoryView.kt` + `NotesChronoView.kt` — now accept `notes: List<NoteRow>` as a parameter rather than reaching into `NotesDb` directly. Header and mode-strip counters compute from the parameter.
- `ui/tasks/TasksTimelineScreen.kt` + `Timeline.kt` — same treatment with `tasks: List<TaskRow>`. The "done" counter (was hardcoded `4/9` in the mockup) now computes from the data — currently shows `2/9` because that's what the seed actually contains. User chose truthful over decorative.

### Implementation notes

- **Tiny manual DI.** `XpApp` constructs the DB and two repos, exposes them as fields. No Hilt, no Koin — we have two repos. The `viewModel(factory = …)` call in `AppRoot` pulls them via `LocalContext.current.applicationContext as XpApp`. Easy to swap for Hilt later if the project grows; not worth the annotation overhead today.
- **Seed runs once.** `seedIfEmpty` checks `dao.count() == 0` and bails otherwise. Seeding happens in `appScope.launch { … }` on `Dispatchers.IO` — completes within milliseconds of cold start; the UI shows an empty list for a frame and then populates. Acceptable for now; if it gets noticeable we can `runBlocking` the seed on first launch only.
- **Repository maps entity → UI model.** `NoteEntity → NoteRow`. The screens never see Room types — clean separation lets us reshape the entity later without touching the UI. `formatWhen()` is a placeholder ("Today" / "Yesterday" / "Nd" / "Nd ago"); the mockup's nicer formatter ("Tue", "Apr 28") lands when we have real updateAt values flowing through edits.
- **`PinnedIds` is still hardcoded** in `NotesData.kt` and the seed cooperates by setting `isPinned = true` on the matching rows. The screen filter should eventually use `note.isPinned` and drop the `PinnedIds` set entirely — flagged below.
- **`NotesDb` / `TasksDb` lists kept as defaults** for the screen composable parameters. This is a fallback path (used only if someone invokes the screen without a ViewModel — e.g. previews). They're noise now and should arguably be deleted in the cleanup pass.

### Build & install + verification

```
./gradlew assembleDebug          # 12s incremental (KSP runs)
adb uninstall com.xpotrack.app   # clean install — destroys any prior keystore key
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

**Database encryption verified.** First 16 bytes of `databases/xpotrack.db`:

```
aa 21 0b 0f 42 cd 29 a2 d5 3a 9f d1 8e 2f 2b 9b
```

Random — a plaintext SQLite file would start with `SQLite format 3\0` (ASCII). SQLCipher is working.

**No crashes in logcat.** The only warnings are `JNI critical lock held` from SQLCipher writing the seed (one-time, hundreds of milliseconds), which is fine.

### What's verified on the phone
- Notes screen renders the 9 seeded notes
- Tasks screen renders the 9 seeded tasks with truthful 2/9 done counter
- Bottom tabs navigate
- Background+foreground the app — state persists (no re-seed; data comes from the encrypted DB)
- User-approved

### Deferred from this milestone
- **Real `formatWhen`** that produces "Tue", "Apr 28", "Today" like the mockup. Currently uses days-ago math.
- **Drop `PinnedIds`** — the screen filter should read `note.isPinned`. Currently relies on the seeded rows getting ids 1 and 8 by Room's autoGenerate. Works for now, fragile if anyone adds a note before this is fixed.
- **No writes yet.** Repositories only expose `observeAll` and `seedIfEmpty`. No `insert`, `update`, `delete`. That's the next milestone (or fold into editor work).
- **First-launch UI flash.** The list is briefly empty until the seed completes. Negligible (<100ms) but visible if you watch.

### Project tree after milestone 5

```
app/src/main/
├── AndroidManifest.xml
├── java/com/xpotrack/app/
│   ├── MainActivity.kt
│   ├── XpApp.kt
│   ├── data/
│   │   ├── db/{Entities.kt, Daos.kt, XpDatabase.kt}
│   │   ├── repo/{NotesRepository.kt, TasksRepository.kt, SeedData.kt}
│   │   └── security/PassphraseStore.kt
│   └── ui/
│       ├── AppRoot.kt
│       ├── theme/{XpTokens.kt, XpTheme.kt, XpTypography.kt}
│       ├── components/{XpBottomTabs.kt, XpReminderPill.kt}
│       ├── notes/{NotesData.kt, NotesListScreen.kt, NotesCategoryView.kt,
│       │          NotesChronoView.kt, QuickEntryStrip.kt, NotesViewModel.kt}
│       ├── tasks/{TasksData.kt, TasksTimelineScreen.kt, DayChips.kt,
│       │          Timeline.kt, TasksViewModel.kt}
│       ├── vault/VaultStubScreen.kt
│       └── more/MoreStubScreen.kt
└── res/
    ├── drawable/ic_*.xml                # 15 vector icons
    ├── font/geist_*.ttf                 # 6 font files
    └── values/{strings.xml, themes.xml}
```

---

---

## Phase 7 — Audit pass

After milestone 5 the project had ~30 source files. Did a sweep for dead code, layering violations, files over the 200-line target, and leftover hardcoded data that should be gone now that the DB is wired.

### What we found

- **`NotesDb` / `TasksDb` constants still living in `ui/notes/NotesData.kt` and `ui/tasks/TasksData.kt`** — the milestone-2 / milestone-3 stub data. They were left as default values on the screen composables (`fun NotesListScreen(notes: List<NoteRow> = NotesDb, …)`). Silent footgun: if anything composes the screen without explicit data (a Preview, a test), it'd render stub data not matching the DB.
- **`fillMaxWidthHack`** — a no-op `Modifier` extension I added during milestone 3 (`private fun Modifier.fillMaxWidthHack() = this`). Pure dead weight from a moment of "I'll come back to this".
- **`ReminderLevel` enum carrying UI types** — it held `Color` fields and `R.drawable.*` ids. Lived in `ui/tasks/TasksData.kt`. That meant `data/repo/TasksRepository.kt` had to import from `ui/tasks` to call `ReminderLevel.valueOf(...)` — a direction-of-dependency violation (`data → ui`).
- **`XpTokens` had ~11 unused fields** — type-size constants (`H1`, `H2`, `Body`, `Meta`, `Cue`, `BodyStrong`) and spacing constants (`ScreenPad`, `CardPad`, `Pill`, `RadiusSm`, `RadiusLg`). All aspirational; per-screen literals + `XpTypography` shadowed them everywhere. Color tokens kept (they map literal mockup hex values 1:1).
- **`NotesCategoryView.kt` at 216 lines** — 16 over the 200-line guideline from CLAUDE.md. User chose to leave it; splitting would create thin satellite files that need to be read together anyway.

### What we changed

```
ADDED
  app/src/main/java/com/xpotrack/app/data/model/ReminderLevel.kt    # 4 lines — pure enum
  app/src/main/java/com/xpotrack/app/ui/components/ReminderStyle.kt # was moved from ui/tasks
                                                                     # then re-packaged so
                                                                     # components/ doesn't
                                                                     # depend on tasks/

DELETED
  NotesDb constant + screen default fallbacks                       # NotesData.kt trimmed
  TasksDb constant + screen default fallbacks                       # TasksData.kt trimmed
  fillMaxWidthHack extension + its call site                        # Timeline.kt
  11 unused fields from XpTokens                                    # type/spacing constants

CHANGED
  XpReminderPill                  takes ReminderLevel (domain) and calls styleFor()
  Timeline.kt:TaskPill            uses styleFor(task.level) instead of task.level.cardBg
  TasksRepository                 imports ReminderLevel from data.model
  SeedData.kt                     stringly-typed "Silent"/"Notify"/"Alarm" replaced with
                                  ReminderLevel.Silent.name + private vals — no more risk
                                  of a typo passing review and exploding at parse time
```

### Residual layering tension (documented, not fixed)

`data/repo/NotesRepository.kt` and `data/repo/TasksRepository.kt` still import `NoteRow` and `TaskRow` from `ui/notes` and `ui/tasks`. Strictly that's data depending on ui. But:

- `NoteRow` / `TaskRow` are plain Kotlin data classes — no Compose, no Android, no UI library imports.
- Moving them to a `domain/` package would add a layer for one shared type each, with no callers in non-UI code today.

The right move when the project grows beyond what one person can hold in their head: introduce a `domain/model/` package and move both. Until then, the convention is "UI models can be referenced from repositories iff they have no UI imports". Re-evaluate when the third feature surface lands (probably alarms, where a domain `Task` will need to exist independent of any screen).

### Build & runtime verification

```
./gradlew assembleDebug          # 4s, clean
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

App launches with the same 9 notes / 9 tasks / 2-of-9 counter. No regression. No new logcat errors.

### Final shape after the audit

```
app/src/main/java/com/xpotrack/app/
├── MainActivity.kt
├── XpApp.kt
├── data/
│   ├── model/ReminderLevel.kt
│   ├── db/{Entities.kt, Daos.kt, XpDatabase.kt}
│   ├── repo/{NotesRepository.kt, TasksRepository.kt, SeedData.kt}
│   └── security/PassphraseStore.kt
└── ui/
    ├── AppRoot.kt
    ├── theme/{XpTokens.kt, XpTheme.kt, XpTypography.kt}
    ├── components/{XpBottomTabs.kt, XpReminderPill.kt, ReminderStyle.kt}
    ├── notes/{NotesData.kt, NotesListScreen.kt, NotesCategoryView.kt,
    │          NotesChronoView.kt, QuickEntryStrip.kt, NotesViewModel.kt}
    ├── tasks/{TasksData.kt, TasksTimelineScreen.kt, DayChips.kt,
    │          Timeline.kt, TasksViewModel.kt}
    ├── vault/VaultStubScreen.kt
    └── more/MoreStubScreen.kt
```

Total: **1781 lines** of Kotlin across 28 source files. Largest file `NotesCategoryView.kt` at 216.

---

## Phase 8 — APK size sweep

### Why
After milestone 5 the debug APK was **48 MB** despite the project containing only mockup-level UI and a tiny encrypted DB. Investigated before starting milestone 6 so we don't carry the bloat forward.

### What we found via `unzip -l`
- **~21 MB native libs** — SQLCipher AAR ships `libsqlcipher.so` for all 4 ABIs (`arm64-v8a`, `x86_64`, `x86`, `armeabi-v7a`). The target phone (iQOO Neo 7) is `arm64-v8a` only; the other three were dead weight.
- **~27 MB DEX** — debug builds run without R8 / `shrinkResources`. Compose tooling + lifecycle + Room + AndroidX-security all packaged unstripped.
- 810 KB fonts, 60 KB drawables, 400 KB `resources.arsc` — all fine.

### What we changed in `app/build.gradle.kts`
```kotlin
defaultConfig {
    ndk { abiFilters += "arm64-v8a" }            // drop 3 unused ABIs
    vectorDrawables { useSupportLibrary = true } // shrink generated raster fallbacks
}
buildTypes {
    release {
        isMinifyEnabled = true                   // R8 on for release
        isShrinkResources = true
    }
}
dependenciesInfo {                               // strip Play-store dep metadata blob
    includeInApk = false
    includeInBundle = false
}
packaging {
    resources.excludes += setOf(
        "/META-INF/*.version",
        "/META-INF/*.kotlin_module",
        "/kotlin/**",
        "/DebugProbesKt.bin",
    )
}
```

### `proguard-rules.pro` filled in
Empty before. Release R8 needs explicit keep rules for code accessed by JNI / reflection / annotation processing:
- `net.sqlcipher.**` + `net.zetetic.**` — JNI entry points
- Room runtime + `@Entity` / `@Dao` / `@Database` annotated classes
- Our `data.model.**` and `data.db.**` packages (entity field names matter to Room)
- `-dontwarn` for `kotlinx.coroutines.debug.**`, `com.google.errorprone.annotations.**`, and `javax.annotation.**` — Tink (transitive via `androidx.security.crypto`) references compile-only annotations that aren't on the runtime classpath. Standard fix, harmless.

### Results
| Variant | Before | After |
|---|---:|---:|
| Debug APK | 48 MB | **32 MB** |
| Release APK (R8 + shrinkResources) | — | **8.0 MB** |

Release breakdown: 5.8 MB SQLCipher native lib, 2.0 MB DEX (down from 27 MB), 810 KB fonts. Native lib is now the dominant cost — non-negotiable, it's the only thing keeping the DB encrypted.

### Verification
- `./gradlew assembleDebug` + `assembleRelease` both clean
- Debug APK installs over wireless adb, launches, seeded notes + tasks render, tabs switch, no logcat errors
- Native libs in APK: only `lib/arm64-v8a/libsqlcipher.so` (5.5 MB) — three other ABIs gone

### Source-tree audit
Re-checked the 28 Kotlin files (1781 lines total). The milestone-5 audit pass left things clean — no dead code, no oversized files (largest still `NotesCategoryView.kt` at 216), no layering violations beyond the one already documented. **No source changes needed for this phase.**

One known-residual flagged but deferred to milestone 6 (where it belongs with the rest of the writes work):
- `PinnedIds` set in `NotesData.kt` is still used by `NotesCategoryView` instead of filtering on `note.isPinned`. Coupled to the seeded-row-id assumption — will be replaced when `formatWhen` and `upsert` land.

---

## Remaining roadmap

Everything below comes from `docs/goal.md` (the original technical plan) and `docs/design-spec.md` (the 16 mockup screens + fidelity notes). Order is roughly bottom-up by dependency — each milestone unlocks the next without revisiting earlier work.

Numbering picks up from the milestones already shipped (1–5 + audit). The MVP-1 / MVP-2 / MVP-3 / MVP-4 tier markers come from `docs/design-spec.md` §6 and indicate the slice each milestone belongs to.

### Milestone 6 — Notes editor + first writes  *(MVP-1, blocks everything that mutates)*

Tap the FAB on Notes, get the **New note** screen from `new-note.jsx`. Tap a note, get the **Note editor** from `note-editor.jsx`. Type something, hit back, see it in the list with an updated `updatedAt`. Writes go through `NotesRepository.upsert(note)` — the first time the app actually mutates the DB.

- New screens: `NotesEditorScreen.kt` (write mode), `NewNoteScreen.kt` (empty state with shortcut hint card)
- `NotesRepository.upsert(note: NoteRow): Long`, `delete(id)`
- Real `formatWhen` ("Today" / "Yesterday" / "Tue" / "Apr 28") replacing the days-ago stub
- Drop `PinnedIds` — filter by `NoteEntity.isPinned`
- Navigation-Compose enters the build here — needed for editor pushed over the list with system back

### Milestone 7 — Markdown preview toggle  *(MVP-1)*

Implement the Write / Preview segmented control from `note-editor.jsx`. Tapping "Preview" renders the markdown body fully styled per `markdown-preview.jsx`. Hybrid live-render (headings styled in place while editing) is deferred — straight toggle for now.

- Markdown renderer (lightweight — likely `org.commonmark:commonmark` + a Compose visitor that emits `Text` / `Column` / `Box` nodes; no WebView)
- `MarkdownPreviewScreen.kt`
- Bottom format-strip with the H / B / I / list / code / quote icons (rendering-only; tap behavior deferred to a polish pass)

### Milestone 8 — Task create + alarms  *(MVP-2, the headline feature from the plan §4)*

The whole reason for the app. FAB on Tasks opens the **bottom-sheet variant** from `task-create.jsx` (variant A — locked in `docs/design-spec.md` §6). Saving a task with `level = Alarm` schedules an exact alarm. When it fires, the **`alarm.jsx` full-screen takeover** shows — pulse rings, big time, snooze chips, slide-to-dismiss (user chose dismiss-only at the start; "snooze" chips render but no-op for now, or get cut).

- `TaskCreateSheet.kt` (bottom sheet, time wheel, reminder chips)
- `TasksRepository.upsert(task: TaskRow): Long`
- `AlarmScheduler` wrapping `AlarmManager.setExactAndAllowWhileIdle()` + `USE_EXACT_ALARM` permission in the manifest
- `AlarmReceiver: BroadcastReceiver` — either posts a notification or launches the alarm activity full-screen, per `alarmType`
- `BootCompletedReceiver` to reschedule alarms after a reboot (Android drops them on restart)
- `AlarmRingingActivity` — full-screen, wakes the screen, plays sound + vibrate. Mockup says "calm but unmissable"; that's the design target

### Milestone 9 — Task detail  *(MVP-2)*

Tap a task in the timeline, get `task-detail.jsx` — big time hero in mono, label + notes, field rows (When / Reminder / Repeat / Category), "Mark done" button. Edit returns to the bottom sheet pre-filled.

- `TaskDetailScreen.kt`
- `markDone(id)` repository call (the only real edit for this milestone; deeper editing piggybacks on milestone 8's sheet)

### Milestone 10 — Vault, unlock, locked-note open  *(MVP-3, encryption layer 2)*

The vault tab graduates from placeholder to the full three-screen flow:

1. `vault-unlock.jsx` — fingerprint via `BiometricPrompt`, passphrase fallback
2. `vault.jsx` — locked-notes list with the cool gradient bg, lock chrome, auto-lock countdown
3. `locked-note.jsx` — secret editor with masked rows + reveal toggle

Per-note encryption (plan §5 layer 2): user-set passphrase, derived via PBKDF2/Argon2 with a per-note salt, encrypts the body. Plaintext wiped from the row — only `encryptedBlob` remains. No recovery path.

- `VaultUnlockScreen`, `VaultListScreen`, `LockedNoteScreen` (replace the stub)
- `androidx.biometric:biometric` dependency
- Keystore key wrapping the vault password
- `VaultCrypto.kt` — encrypt/decrypt with per-note salt
- Auto-lock timer hooked into lifecycle (5 min default per mockup)

### Milestone 11 — Category manager + custom categories  *(MVP-4)*

`category-manager.jsx` bottom sheet: built-in vs custom sections, add / rename / reorder, color picker per custom category. Replaces the hardcoded `Categories` list.

- `category` table in Room
- `CategoryManagerSheet.kt`
- `NotesRepository.observeCategories()`
- Note model gains a foreign key to category (we currently store the category name as a string on the note — works but doesn't survive renames)

### Milestone 12 — Quick notes (24h ephemeral)  *(MVP-4)*

The disappearing-notes feature from `quick-notes.jsx` — the prominent strip at top of the notes list, plus the dedicated quick-notes screen and the post-save dialog. Adds a `quick_notes` table with auto-expiry (a periodic `WorkManager` job sweeps expired rows).

- `QuickNotesScreen.kt`, `QuickEntryStrip.kt` wired up (currently just a static placeholder)
- `QuickNoteEntity` table with `expiresAt`
- `WorkManager` daily sweep + an in-process check on screen open

### Milestone 13 — Settings  *(MVP-4)*

`settings.jsx` — all the toggles. Theme/accent/font/density/markdown/live-render/word-count/default-reminder/alarm-ring-length/lock-screen-show/vibrate/auto-lock/hide-previews/export/backup/restore/storage. Replaces the More stub.

- `settings_prefs.xml` via DataStore
- `SettingsScreen.kt`
- Wires individual toggles back to the screens that read them (e.g. alarm ring length read by `AlarmRingingActivity`)

### Milestone 14 — Export for Tailscale + Waybar  *(plan §6)*

The original ask. "Export" action writes:
- One `.md` per non-locked note into `Documents/TaskNotes/`
- `tasks.json` snapshot in the same folder with `{title, dueAt, alarmType, isDone}` + precomputed summary string
- The app never networks; the user points Syncthing / Tailscale-with-rclone / whatever at the folder

User chose manual button only (not auto-export). Probably lands on the settings screen and/or as a menu item in the editor.

### Milestone 15 — Polish pass (the fidelity-notes debt)  *(across all phases)*

`docs/design-spec.md` §10 lists every Compose-vs-CSS gap. Tackle them in one pass after the features land:

- **True 0.5dp hairlines** via `Modifier.drawBehind` 1-physical-pixel strokes
- **Teal glow shadows** on FAB and NOW indicator via stacked `drawBehind` radial gradients
- **Time-wheel mask gradient** via `graphicsLayer + DstIn`
- **Backdrop blur** on scrims via `RenderEffect.createBlurEffect` on SDK 31+
- **Pulse rings** on alarm screen — `rememberInfiniteTransition`
- **Caret blink** in empty-state placeholders
- Hour-label `letterSpacing: 0.05em` we dropped in milestone 3
- Tab-switch animation
- App icon (currently default Android robot)

### Milestone 16 — Ship-readiness

- Real release signing config in `app/build.gradle.kts`
- ProGuard rules audited against SQLCipher / Room / Compose
- Crash reporting? — explicitly out of scope per plan ("no analytics, no cloud"), but local file-logger could be useful
- Versioning + changelog
- First reproducible release build

---

That's the full path from where we are (Notes + Tasks reading from an encrypted DB behind a tab bar) to where `docs/goal.md` says we're going (a calm local-only notes-and-alarm app with a vault and an export hook for the Tailscale workflow).
