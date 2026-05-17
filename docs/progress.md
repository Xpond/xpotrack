# xpotrack — Progress Log

A running log of milestones shipped, key decisions, and the roadmap. With `docs/design-spec.md` + `docs/goal.md` + the mockups in `misc/mockups/screens/`, this should be enough to pick the project up cold.

---

## Planning & setup

**Plan source:** `docs/goal.md` (Kotlin + Compose + Room + SQLCipher, single-module, local-only, no network). **Design source:** the 16 JSX mockups in `misc/mockups/screens/` — every screen renders to match them; Compose-vs-CSS gaps are flagged in `docs/design-spec.md` §10 and resolved before shipping. **Toolchain:** CLI only on Arch Linux. **Deploy:** physical iQOO Neo 7 (Android 14, SDK 34) over wireless adb on LAN.

Key locked decisions: task-create UI = bottom sheet variant (A in `task-create.jsx`); vault must render exactly as mocked (data still lives in notes table with `isLocked` flag); Geist + Geist Mono + Instrument Serif bundled as `.ttf` assets, not Google Fonts; deferred until later — quick notes, linked-note field on tasks, custom category colors, search, pinning toggle, repeat, snooze, category reordering, markdown shortcut auto-expansion.

**Toolchain installed once:** `jdk21-openjdk`, `android-tools`, Google command-line-tools `13114758` unzipped to `~/Android/Sdk/cmdline-tools/latest`. Env: `ANDROID_HOME=~/Android/Sdk`, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk`. Packages: `platform-tools`, `platforms;android-35`, `build-tools;35.0.0` (AGP 8.7.3 also auto-pulls `34.0.0`). Wireless adb pairing needs same Wi-Fi LAN + `adb pair IP:PORT CODE` then `adb connect IP:PORT` — Tailscale + Arch's adb don't pair (mDNS stripped from `android-tools 35.0.2` + CGNAT mDNS quirks). `targetSdk = 35` is fine on an SDK 34 device — it's a "tested-against" declaration, not runtime requirement.

**Course-correction up front:** original plan was scaffold + Room + SQLCipher + two screens then build once. After accumulating untested code with broken APIs, recut into 5 small milestones, each ending in a verified on-device build. Trades more `assembleDebug` cycles for near-zero risk of compounding mistakes.

---

## Milestone 1 — Launchable hello-world

Scaffold: `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12), `compileSdk = 35`, `minSdk = 29`, JVM 17. `XpTokens.kt` mirrors every color from `system.jsx` exactly. `XpTheme.kt` wraps Material 3 `darkColorScheme`. `XpTypography.kt` is the Material 3 type scale tuned to spec §3.

`MainActivity` renders edge-to-edge with `#06100F` background + a 64dp teal (`#5EEAD4`) dot centered. **Verified:** JDK 21 + Gradle 8.10.2 + AGP 8.7.3 + Kotlin 2.1.0 + Compose BOM 2024.12 compose cleanly; tokens resolve to mockup hex values; edge-to-edge + transparent system bars work; wireless `adb install` round-trips work. First build 1m 41s, incremental drops to seconds. App icon is still default Android robot.

---

## Milestone 2 — Notes list screen (no DB)

Render `notes-list.jsx` (category mode) with hardcoded data. Geist + Geist Mono pulled from `github.com/vercel/geist-font` (OFL) into `app/src/main/res/font/` — 6 `.ttf` files, ~810KB, no network. `XpTypography.kt` switched off `FontFamily.SansSerif` onto the bundled fonts. 7 vector drawables in `res/drawable/` (`ic_search`, `ic_sort_date`, `ic_grouped`, `ic_plus`, `ic_lightning`, `ic_chevron_right`, `ic_star`) — `strokeColor="#FFFFFFFF"` so the Compose `Icon` `tint` can recolor at call sites.

Screen split into `NotesData.kt`, `NotesListScreen.kt`, `NotesCategoryView.kt`, `NotesChronoView.kt`, `QuickEntryStrip.kt`. **Verified:** Geist renders at every weight used; category mode renders complete (header, mode-strip cue, quick-entry strip, pinned strip with horizontal scroll, category groups with correct count chips + "+ N more" overflow + "New category" dashed button); FAB renders; sort toggle swaps to chronological view.

**Deferred:** FAB teal-glow halo (Compose shadows are grayscale — needs stacked `drawBehind` radial gradient per spec §10), true 0.5dp hairlines (currently 1px), dashed border on quick-entry strip (no built-in `Modifier.dashedBorder()`), pixel-perfect category chip spacing.

---

## Milestone 3 — Tasks timeline screen (no DB)

Render `tasks-timeline.jsx` with hardcoded data. Added `ui/components/XpReminderPill.kt` (shared between timeline pills `Sm` and the upcoming task-detail screen `Md`) and `ui/tasks/{TasksData, TasksTimelineScreen, DayChips, Timeline}.kt`. Three reminder-icon drawables (`ic_reminder_silent`, `ic_reminder_notify`, `ic_reminder_alarm`) + `ic_check`.

Key implementation notes worth keeping:
- **Reminder levels are first-class data** — `ReminderLevel` enum carries `accent`, `tint`, `cardBg`, `iconRes` so per-level styling never branches in UI code (later moved to a domain enum + UI-side style helper — see audit phase).
- **Timeline math centralized** — `TimelineStartHour=6`, `TimelineEndHour=22`, `HourHeightDp=56`. `timeToOffsetDp("09:15")` is the single function translating `HH:mm` → vertical offset; `HourGrid`, `TaskPill`, `NowIndicator` all call it.
- **Absolute positioning via `Modifier.offset(x, y)`** — matches CSS `position: absolute; top: X` from the mockup 1:1.
- **Done tasks** dim to 45% opacity, strikethrough label, drop card border/bg, tint rail at 30%.

**Verified:** header + mono "4/9" counter, 7-day chip strip with Friday active, hour labels 6 AM → 10 PM with hairline dividers at correct heights, 9 tasks pinned to correct times with right rail color + card tint, done tasks dimmed with strikethrough + checkmark, NOW indicator at 9:41.

**Deferred:** NOW-dot outer halo (same Compose-shadow limitation as FAB), hour-label `letterSpacing: 0.05em` (dropped after a botched `em` extension), task pill animations, scroll-to-now on open, real date math on the day chip strip (currently hardcoded Thu–Wed of week-of-May-16).

---

## Milestone 4 — Bottom tab navigation

Wired Notes + Tasks behind a bottom tab bar + placeholder screens for Vault and More. New files: `ui/AppRoot.kt` (hosts active-tab state + content), `ui/components/XpBottomTabs.kt` (4 tabs, gradient fade, hairline top), `ui/vault/VaultStubScreen.kt`, `ui/more/MoreStubScreen.kt`. Four tab-icon drawables (`ic_tab_notes/tasks/vault/settings`).

**No Navigation-Compose yet** — `var active by rememberSaveable { mutableStateOf(XpTab.Notes) }` + `when (active)` block. With 4 tabs and no deep linking, simpler and faster to compile than NavHost. (Navigation-Compose came in with milestone 6 when the editor needed a stacked destination.) Active tab is a color swap only — icon + label shift to `Teal`, inactive `Ink3`, ripple suppressed with `indication = null` to match the mockup's calm aesthetic. **Vault stub** uses the cool gradient bg (`#050D0C → Bg`) from the real vault mockup — telegraphs "separate space" even as a placeholder.

**Verified:** opens on Notes; tabs swap content + active highlight; vault shows cool-gradient placeholder; FAB sits above the tab bar.

**Deferred:** tab-switch animation; back-stack handling (system back from Tasks exits the app — fixed in milestone 6 via Navigation-Compose); real Vault + More screens (milestones 10 + 13).

---

## Milestone 5 — Room + SQLCipher backing the screens

Replaced hardcoded `NotesDb` / `TasksDb` with a real encrypted Room database. Screens read via ViewModels exposing `StateFlow<List<…>>` collected with `collectAsStateWithLifecycle()`. DB encrypted at rest with SQLCipher, 32-byte random passphrase generated once on first launch via AndroidX `EncryptedSharedPreferences` (Keystore-backed master key, `AES256_GCM` scheme). Mockup data inserted on first launch only (`dao.count() == 0` guard); subsequent launches load from DB.

New files: `XpApp.kt` (tiny manual DI — no Hilt; two repos), `data/security/PassphraseStore.kt`, `data/db/{Entities, Daos, XpDatabase}.kt`, `data/repo/{NotesRepository, TasksRepository, SeedData}.kt`, `ui/notes/NotesViewModel.kt`, `ui/tasks/TasksViewModel.kt`. Re-added Room + SQLCipher + KSP + lifecycle-runtime-compose to the build. **Repository maps entity → UI model** (`NoteEntity → NoteRow`); screens never see Room types.

**Database encryption verified** — first 16 bytes of `databases/xpotrack.db` are random (a plaintext SQLite file starts with `SQLite format 3\0`). Tasks "done" counter shows truthful `2/9` instead of the mockup's hardcoded `4/9` — user chose truthful over decorative.

Uninstall destroys the Keystore key and makes the DB unrecoverable — feature, not bug.

---

## Audit + APK size sweep

**Source audit after milestone 5:** removed `NotesDb`/`TasksDb` stub constants (kept as screen defaults — silent footgun for previews/tests); deleted a no-op `fillMaxWidthHack` `Modifier` extension; moved `ReminderLevel` enum out of `ui/tasks/` into `data/model/` so repositories don't import UI types (it had been holding `Color` + `R.drawable.*` fields — split into pure domain enum + `ui/components/ReminderStyle.kt`); dropped 11 unused fields from `XpTokens` (aspirational type-size + spacing constants shadowed everywhere by `XpTypography` + per-screen literals).

**Residual layering tension (documented, not fixed):** `data/repo/*Repository.kt` still imports `NoteRow` / `TaskRow` from `ui/notes` / `ui/tasks`. They're plain Kotlin data classes with no Compose imports. Convention until the third feature surface lands: *UI models may be referenced from repositories iff they have no UI imports.* Re-evaluate when alarms need a domain `Task` (milestone 8).

**APK size sweep:** debug APK was 48 MB. SQLCipher AAR ships `libsqlcipher.so` for 4 ABIs; iQOO Neo 7 is `arm64-v8a` only — added `ndk { abiFilters += "arm64-v8a" }` in `defaultConfig` (drops 3 unused ABIs, ~16 MB). Release `isMinifyEnabled = true` + `isShrinkResources = true` + filled `proguard-rules.pro` with `-keep` rules for SQLCipher JNI + Room + `data.model.**` + `data.db.**`; `-dontwarn` for Tink's compile-only annotations (transitive via `androidx.security.crypto`). `dependenciesInfo.includeInApk = false` strips Play-store dep blob. Excluded `META-INF/*.version`, `*.kotlin_module`, `/kotlin/**`, `DebugProbesKt.bin` from `packaging.resources`.

Result: debug **48 MB → 32 MB**, release **8.0 MB** (5.8 MB SQLCipher + 2.0 MB DEX + 810 KB fonts). Native lib is now the dominant cost — non-negotiable.

---

## Milestone 6 — Notes editor + first writes

The first milestone that actually mutates the DB. FAB opens a blank editor; tapping any row opens it prefilled; autosave on back navigation (no Save button); empty new notes get discarded; existing notes cleared to blank get deleted. Updated notes float to the top of the list with a fresh `updatedAt`. Navigation-Compose enters the build for the editor pushed over the list with system back returning to the list (no longer exits the app).

**New files:** `ui/notes/NotesEditorScreen.kt` (editor matching `note-editor.jsx` topbar + `new-note.jsx` empty-state hint card inline; `BasicTextField` for title + body with placeholder text and teal cursor); `ui/notes/NotesEditorViewModel.kt` (loads by id, exposes `EditorState`, `suspend fun save()`); `res/drawable/ic_chevron_left.xml`, `ic_dots_vertical.xml`.

**Save model:** the editor screen wraps `vm.save()` + `onBack()` in a `rememberCoroutineScope().launch { … }` invoked from both the back IconChip and a `BackHandler`. Writes always commit before `popBackStack()` returns. The earlier attempt (`DisposableEffect { onDispose { runBlocking(IO) { save } } }`) raced the ViewModel teardown and silently dropped writes.

**Auto-focus on new note:** `LaunchedEffect(state.loaded, state.id) { if (loaded && id == 0) titleFocus.requestFocus() }` pops the keyboard immediately so you can start typing without a manual tap. Existing notes stay unfocused (you tap to position the caret).

**Repo + DAO writes:** `NoteDao.getById`, `upsert`, `delete`. `NotesRepository.upsert(NoteRow)` reads any existing row to preserve `createdAt` + `isLocked`, writes `updatedAt = now`. `delete(id)`, `getById(id)`. DAO `observeAll` `ORDER BY` switched from `recency DESC` to `updatedAt DESC`.

**Real `formatWhen`:** "Today" / "Yesterday" / "Tue" / "Apr 28" via `java.time` — replaces the days-ago stub. Lives in `NotesRepository.kt` as a top-level `internal` function (testable).

**Dropped `PinnedIds`:** `NoteRow` gains `isPinned` propagated from `NoteEntity.isPinned`; category view filters via `note.isPinned` directly.

**Cleanup pass after the milestone landed:** removed dead `NoteEntity.recency` column (DAO now sorts by `updatedAt` and no consumer reads `recency`) — schema bumped to v2 with `fallbackToDestructiveMigrationFrom(1)`. Existing pre-release install wipes its notes DB on first v2 launch and re-seeds clean. Also removed dead `XpTokens.TealDeep`, `XpTokens.SurfaceMute`, unused `widthIn` import in `NotesCategoryView.kt`, unused `sp` import in `NotesListScreen.kt`, and the stub `recency = N` arg from all 9 seed rows.

**Verified:** build clean, install + launch with no crashes, destructive migration fires + re-seed succeeds (the SQLCipher JNI lock log is the seed running), FAB opens blank editor with keyboard up, typing + back persists with new `updatedAt` floating the row to top, tapping a row opens it prefilled, empty new notes discarded, system back from editor returns to notes list.

**Project shape after this milestone:** ~1750 lines of Kotlin across 29 source files. Largest file `NotesEditorScreen.kt` at 251 (single-purpose; accepted overage); next is `NotesCategoryView.kt` at 219.

```
app/src/main/java/com/xpotrack/app/
├── MainActivity.kt · XpApp.kt
├── data/
│   ├── model/ReminderLevel.kt
│   ├── db/{Entities, Daos, XpDatabase}.kt        # @Database version = 2
│   ├── repo/{NotesRepository, TasksRepository, SeedData}.kt
│   └── security/PassphraseStore.kt
└── ui/
    ├── AppRoot.kt                                # NavHost: tabs + editor/{id}
    ├── theme/{XpTokens, XpTheme, XpTypography}.kt
    ├── components/{XpBottomTabs, XpReminderPill, ReminderStyle}.kt
    ├── notes/{NotesData, NotesListScreen, NotesCategoryView,
    │          NotesChronoView, QuickEntryStrip, NotesViewModel,
    │          NotesEditorScreen, NotesEditorViewModel}.kt
    ├── tasks/{TasksData, TasksTimelineScreen, DayChips, Timeline, TasksViewModel}.kt
    ├── vault/VaultStubScreen.kt                  # placeholder until milestone 10
    └── more/MoreStubScreen.kt                    # placeholder until milestone 13
```

**Deferred from this milestone:**
- Markdown preview toggle + bottom format strip (milestone 7).
- Pin/unpin action in the editor — currently read-only from seed.
- Category picker in the editor — chip is display-only.
- The dots-vertical "more" menu — no-op icon.

---

## Remaining roadmap

Everything below comes from `docs/goal.md` (the original technical plan) and `docs/design-spec.md` (the 16 mockup screens + fidelity notes). Order is roughly bottom-up by dependency — each milestone unlocks the next without revisiting earlier work.

Numbering picks up from the milestones already shipped (1–6 + audit). The MVP-1 / MVP-2 / MVP-3 / MVP-4 tier markers come from `docs/design-spec.md` §6 and indicate the slice each milestone belongs to.

## Milestone 7 — Markdown preview toggle

Write / Preview segmented toggle in the editor topbar. Write mode is unchanged (`BasicTextField` title + body). Preview mode renders the markdown body styled per `markdown-preview.jsx`: H1 with teal underline rule, H2, paragraphs, dash-list with teal em-dash, blockquote with teal left rule + italic body, fenced code blocks in mono on `surface1`, plus inline `**bold**` (teal semibold) and `*italic*`. Same `NotesEditorScreen` swaps the body per mode — no new route, the chrome is shared.

**New file:** `ui/notes/MarkdownRender.kt` — hand-rolled, single-pass line walker. No dependencies (`org.commonmark` was considered and dropped — the mockup uses ~6 block types and 2 inline marks, far below CommonMark's coverage; adding the dep buys edge cases we don't render). Parses and emits in one pass; no AST, no sealed types.

**Editor state:** `EditorState` gains `previewMode: Boolean`. VM exposes `setPreview(on)`. Toggle is two clickable pills in a hairline-bordered pill row, mono label, teal fill on the active segment. Auto-focus on new note now also gates on `!previewMode`.

**No bottom format strip.** Mockup `note-editor.jsx` has an H/B/I/list/code/quote strip and `markdown-preview.jsx` has Export/Edit ghost buttons — both rendered initially as no-ops per the milestone's "rendering-only; tap behavior deferred" note. Cut both during a tightening pass: a render-only no-op bar is textbook speculative work, and the 8 drawables it required were dead weight. Re-introduce when tap behavior actually lands (some of it ties into milestone 14 export).

**Course-correction worth keeping:** first pass landed at ~647 lines across the two files with per-block @Composable helpers, a sealed `Block` hierarchy, a separate parse → render pipeline, and the dead bottom strips. Cut to 394 lines (168 renderer + 226 screen) by inlining helpers, fusing parse+render, and deleting the strips. Same visual output. Lesson: a single-use renderer doesn't need an AST.

**Deferred (carry-forward):**
- Hybrid live-render (headings styled in place while editing) — straight toggle suffices.
- Instrument Serif for blockquote body — using italic Geist; bundle the `.ttf` later if/when fidelity demands it.
- Mockup decoration: `· · ·` footer divider, "min read" estimate, teal-tinted `HH:mm` prefix inside code fences.
- Pin/unpin, category picker, dots-vertical menu in the editor (unchanged from milestone 6).

**Verified:** build clean, install + launch with no crashes; toggle flips body between Write and Preview without losing typed content; existing seeded notes render their `**bold**`/`*italic*`/headings/lists/quotes/code correctly; system back from either mode autosaves and pops to the list.

---

## Remaining roadmap

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

That's the full path from where we are (Notes + Tasks reading from an encrypted DB behind a tab bar, notes editable with autosave) to where `docs/goal.md` says we're going (a calm local-only notes-and-alarm app with a vault and an export hook for the Tailscale workflow).
