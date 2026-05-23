# xpotrack — Progress

A descriptive snapshot of what ships and the non-obvious decisions worth keeping. Milestone framing has been retired — features are stable enough now to describe by surface area, not in shipping order. Commit history fills in the chronology.

---

## Stack & build

- Kotlin 2.1.0, Compose BOM 2024.12, AGP 8.7.3, Gradle 8.10.2. Builds on JDK 21; compiles to JVM 17 bytecode (`sourceCompatibility = VERSION_17`, `jvmTarget = "17"`).
- `compileSdk = 35`, `minSdk = 29`, `targetSdk = 35`. Single APK pinned to `arm64-v8a` (test device is iQOO Neo 7).
- Room 2.6.1 over SQLCipher 4.6.1 (whole-DB AES). KSP for Room codegen. Manual DI via `XpApp` — no Hilt.
- AndroidX EncryptedSharedPreferences for the 32-byte DB passphrase and vault metadata.
- Navigation-Compose for the editor / detail / vault routes; tab state lives outside the NavHost.
- WorkManager for the 24h quick-note sweep.

Release APK is ~8 MB (5.8 MB SQLCipher native lib + 2 MB DEX + Geist Mono fonts). Debug ~32 MB. Native lib is the dominant cost — non-negotiable.

---

## Notes

A chronological list of notes with a live HH:MM clock, inline search, and a category filter popover. Long-press selects rows; the bottom action bar bulk-shares or bulk-deletes. FAB opens a new note, inheriting the active category filter so a note created while filtered to "Work" lands as a Work note.

Quick notes interleave into this same list, sorted by `updatedAt`/`createdAt`, distinguished only by a countdown chip. There is no separate "Notes vs Quick" surface anymore.

**Gotchas worth keeping:**
- An earlier category-mode + pinned strip design was cut after the search + filter combo covered the same needs more directly.
- Per-row TAG renders only when no category filter is active — redundant otherwise.
- Multi-select Select-All / Share are scoped to the *currently visible* rows (filter + search), not the full table. Otherwise "select all" silently includes hidden rows.
- New-note category inheritance threads through the FAB → `onNewNote(filterId)` → editor query arg. `rememberSaveable` on the filter id survives the round trip into the editor.

---

## Tasks

A per-day timeline. A horizontal date strip scrolls across months with a live month label and a month-picker dialog (also reachable from the create sheet). Each day shows a chronological list of pills sorted by time — no fixed-pixel hour grid. Past dates are read-only: FAB hides, sheet's date picker disables them.

Tasks own `time` (HH:mm), `date` (`dateEpochDay`), reminder level (Silent/Notify/Alarm), an optional recurrence rule (`none` / `daily` / `weekly` / `weekdays`), inline `notes`, and an optional `linkedNoteId`. Recurrence rolls forward in place: marking done or firing an alarm advances `dateEpochDay` to the next valid occurrence and re-arms; missed alarms (phone off) catch up on next launch.

The create sheet is a `ModalBottomSheet` with title field, three-column infinite-loop time wheel (12-hour with AM/PM), reminder chip row, repeat row, linked-note row, full-width teal Schedule CTA. Tap a pill → detail screen with hero time accented by reminder level, inline-editable notes, four field rows (When / Reminder / Repeat / Linked note), Mark-done + delete. Field-row chevrons all reopen the same create sheet pre-filled.

**Gotchas worth keeping:**
- The original fixed-pixel hour grid (`HourHeightDp = 56`) was unsalvageable — 15-min slots collapsed under pill padding. Cut entirely. Chronological list with natural-height pills is the right model for a real schedule.
- `ModalBottomSheet` ate the time wheel's vertical drags as drag-to-dismiss. Fix: a `NestedScrollConnection` on the wheel's LazyColumn consumes `available.y` in `onPostScroll` + `onPostFling`.
- `viewModel(key = "task-create-$id")` reused stale state after save-then-reopen. Fix: a `sheetToken: Int` that increments on every open, baked into the key.
- Wheel callbacks captured stale state via lambda closures (snapped hour back to 4 PM). Switched to `rememberUpdatedState` in `WheelPicker`.
- Recurrence advance fires on user dismissal (`markDone`) only, never inside `AlarmReceiver.onReceive` — pre-rolling on fire caused snooze and hold-to-done to double-advance.
- `cancel()` must clear both regular and snoozed `PendingIntent`s, otherwise editing or deleting a task leaves the in-flight snooze armed.
- Notes textarea on the create sheet and detail screen is capped at ~3 lines with internal scroll — auto-grow pushed the Schedule button off-screen.
- Per-field pickers (When / Reminder / Repeat) all just reopen the create sheet. Cleaner than a dots-menu, fewer surfaces to maintain.

---

## Quick notes

24h ephemeral scratch space in a separate `quick_notes` table (`id`, `text`, `createdAt`, `expiresAt`). The strip at the top of the notes list shows live count + "oldest expires in …". Tapping the strip opens a fullscreen quick editor; saved rows interleave back into the notes index with a countdown chip + progress ring as the disappearing signal. **Keep** promotes a row into the regular `notes` table (Uncategorized; first line becomes title, whole text becomes body) inside a single `withTransaction`.

Expiry runs via `repo.sweepExpired()` on screen open + a 24h `WorkManager` `PeriodicWorkRequest` (KEEP policy). Cron-style timing isn't reliable on OEM-restricted devices, so the in-process sweep is the real source of truth.

**Gotchas worth keeping:**
- First-pass had a separate Quick screen + inline-expand compose row + post-save dialog + Clear all. All cut after the simpler "interleave into the notes index + tap-strip-opens-editor" flow replaced them.
- `flatMapLatest { dao.observe(now) }` to "freshen" `now` was theater — outer flow only emits once. Cut. The DAO's `expiresAt > :now` filter combined with the sweep does the work.

---

## Vault

A four-phase tab (Setup → Unlock → List → LockedNote) gated by a single passphrase. PBKDF2-HMAC-SHA256 (210k iter, 256-bit key) derives an AES-256-GCM key; each note ciphertext is `[salt(16) | iv(12) | ct+tag]`. HMAC-SHA256 stretches the vault key with the per-note salt so every note gets a unique AES key without re-running PBKDF2. A verifier (`PBKDF2(passphrase, salt)`) is stored once at setup; unlock re-derives and constant-time compares.

Biometric unlock is optional. The Android Keystore key (`setUserAuthenticationRequired(true)` + `setInvalidatedByBiometricEnrollment(true)`) wraps the **derived AES key** — not the passphrase — so the fingerprint path skips PBKDF2 entirely and unlocks in <100ms. Adding or removing a fingerprint invalidates the wrapped blob; user falls back to the passphrase.

Vault notes live in the same `notes` table behind an `isLocked` flag with `encryptedBlob: ByteArray?`. Title + category stay plaintext (so the list renders); body is ciphertext. `NoteDao.observeAll()` filters `isLocked = 0`, `observeLocked()` gates the vault list. The Notes tab never sees vault rows. `FLAG_SECURE` is applied to the window while `VaultGate` is on screen — screenshots, screen recording, and the recents thumbnail are blanked. Auto-lock at 1 minute idle.

**Gotchas worth keeping:**
- `BiometricPrompt` forces `FragmentActivity`. Transitively pulled an older `androidx.fragment` whose 16-bit `requestCode` validator clashes with `ComponentActivity`'s 32-bit `ActivityResultRegistry`. The first `registerForActivityResult` (the `POST_NOTIFICATIONS` prompt) crashed on clear-data installs but not on existing ones. Fix: pin `androidx.fragment:fragment-ktx:1.8.5`. Any activity-superclass change deserves a clear-data re-test.
- PBKDF2 on the main thread froze UI 1–3s. Move every key-deriving entrypoint to `Dispatchers.Default`, surface a `verifying: StateFlow<Boolean>` so the Unlock CTA reads "Verifying…" the instant the user taps.
- Lock-now from the vault list flipped phase back to `Unlock` and stranded the user. The intent is "leave the vault" — `VaultGate.onLockExit` flips the active tab back to Notes.
- Biometric wrap originally stored the passphrase bytes, forcing PBKDF2 on every biometric unlock (~300–500ms). Storing the derived AES key instead keeps the same security envelope — the Keystore + biometric gate is what's protecting the blob; PBKDF2 was redundant on that path.
- Initial gradient backdrop (`#030B0A → Bg`) read as a visible band — "a notch" — in both themes. Cut entirely; vault uses flat `Bg` like every other tab.

---

## Categories

User-created only. No built-ins, no "Inbox". A category has `name`, `colorHex`, `sortOrder`. Notes carry a nullable `categoryId`; orphaned notes (after a category delete) fall into a synthetic "Uncategorized" group on the list. Tasks have no category — a reminder isn't filed under "Work" the way a note is.

A single `ModalBottomSheet` houses both the picker and the manager, swapped via `AnimatedContent + SizeTransform`. The picker exposes a `Manage categories…` tail row that morphs the sheet into the manager. The manager supports add / rename / recolor / delete with a `@Transaction deleteAndUncategorize` that clears `categoryId` on every affected note before dropping the row. The color picker is a hue ring + hex input (saturation/value clamped so picks stay legible) — replaces the original 6-color preset palette.

**Gotchas worth keeping:**
- `NoteRow.categoryColorHex` is denormalized onto the row. An earlier pass dropped it to avoid stale snapshots after a recolor, then it came back when the editor + chrono row needed the color inline. Safe now because `NotesRepository.observeAll()` `combine`s the note flow with the categories flow and re-resolves on every emission — a recolor re-fires the whole list. Don't reintroduce a non-`combine` cache.
- Two sheets (picker + manager) stacking caused double-scrim flash + half-open first-tap + snapped-close-on-create animations. Merging into one sheet with `AnimatedContent` swapping inner content killed all three.
- `CategoryManagerViewModel` state must reset on dispose so stale edit rows / pending deletes don't leak between sessions.
- Hue ring tap math: the indicator and tap-hit had a 90° offset that put a tap on red at the green slot. The sweep gradient starts at 3 o'clock; align both to that.
- Manager's new-category editor is pinned below the list with `imePadding` so it stays reachable when the keyboard is up. Name field locks its line metrics so empty vs typing doesn't shift the row.
- Editor VM derives the category label from the live categories flow — deleting the selected category flips the chip to Uncategorized immediately and `save()` can't persist a dangling id.

---

## Editor (notes + quick + vault)

Three editors share the same `BasicTextField` title + body model, autosave on system back (no Save button), and `PinnedHeader` overlay pattern. The notes editor adds:

- **Write/Preview toggle** in the top bar. Hand-rolled single-pass markdown renderer in `MarkdownRender.kt` — no CommonMark dep. Covers H1/H2, paragraphs, dash-list, blockquote, fenced code, inline `**bold**` / `*italic*`, and interactive `[ ]` / `[x]` task checkboxes (tap toggles).
- **Bottom format bar** in write mode: H1/H2/Bold/Italic/List/Check/Quote/Code.
- **Category chip** tinted to the category's color, opens the picker sheet.
- **Pinch-to-zoom** on the body, persisted via `EditorZoomPrefs`. Saves on gesture end, not every frame.
- **Caret-follow scroll** — viewport math subtracts pinned-header height so the caret stays above the IME after layout was restructured around the overlay header.

Existing notes open in Preview; new notes open in Write. Empty new notes are discarded on back; existing notes cleared to blank are deleted. Notes float to the top of the list via `updatedAt` — except category-only changes and deletion-driven `categoryId` fallbacks, which use a column-only update path so reading a note doesn't reorder the list.

Sharing: long-press a note → action sheet with **Share** (writes `# <title>\n\n<body>` to `cache/shared/<slug>.md` via a FileProvider, fires `ACTION_SEND`). Filename slugifies the title; untitled notes fall back to `note-YYYY-MM-DD`. Vault notes never reach this path.

**Gotchas worth keeping:**
- Hand-rolled markdown renderer landed at 647 lines on the first pass with a sealed `Block` AST + parse → render pipeline. Cut to ~170 lines by inlining helpers and fusing parse+render. A single-use renderer doesn't need an AST.
- Save model: `rememberCoroutineScope().launch { vm.save(); onBack() }` from both the back chip and `BackHandler`. The earlier `DisposableEffect { onDispose { runBlocking { save } } }` raced the ViewModel teardown and silently dropped writes.
- Auto-focus on new note: `LaunchedEffect(state.loaded, state.id)` gated on `id == 0 && !previewMode`. Existing notes stay unfocused so taps position the caret.
- Editor open without edits used to bump `updatedAt`. Save now diffs against a pristine snapshot and skips the upsert on no-op.
- Splash gate: if DB preload throws, release the SharedFlow gate in `finally` and log the error — otherwise the splash hangs and the user never sees the app.

---

## Alarms

`AlarmScheduler` wraps `setExactAndAllowWhileIdle` for Notify-level and `setAlarmClock` for Alarm-level (survives app-standby buckets). `nextOccurrence` computes the next HH:mm in the device's local zone; recurring tasks roll forward until the next valid occurrence so missed alarms re-arm instead of stranding `reminderAt` at 0. `BootCompletedReceiver` re-routes every Notify/Alarm task through `upsert` after reboot since Android drops exact alarms on restart.

`AlarmReceiver` posts a teal-accented heads-up notification for Notify-level; for Alarm-level it posts a full-screen-intent notification that lights up `AlarmRingingActivity`. The ringing activity is `showWhenLocked` + `turnScreenOn`, `singleInstance`, `excludeFromRecents`. The redesigned screen is calm — title, note card with a gradient teal accent, snooze list, hold-to-done with an `animateFloatAsState`-driven progress ring (the original pulse-ring design was cut). Snooze pills schedule a one-shot on a separate request-code namespace so recurring alarms aren't clobbered. Hold-to-done marks the task done, advances recurrence, dismisses.

Notification channels suffixed `.v3` (write-once policy means any importance/sound change requires a new ID). `MainActivity` requests `POST_NOTIFICATIONS` on first launch and on every resume checks `canUseFullScreenIntent()` — Android 14+ may have denied it; bounces to Settings → Special Access with a Toast.

**Gotchas worth keeping:**
- Direct `context.startActivity(alarmActivity)` from a `BroadcastReceiver` works unlocked, silently fails locked (background-activity-start restriction since API 29). The full-screen-intent notification is the OS-blessed path. The "defensive" extra `startActivity()` masked the test on every unlocked verification — when adding a fallback path, verify which path is actually carrying the load.
- Vivo/FuntouchOS overrides `lockscreenVisibility` regardless of channel config. `dumpsys notification` shows `mLockscreenVisibility=-1000` even on `PUBLIC`. OEM behavior, not fixable in code. Same family of OEM gotchas applies to autostart / background-power / pop-up permissions on Vivo/Xiaomi/Oppo — document for the user.

---

## Theming & system chrome

Light + Dark palettes swap via a mutable singleton: each `XpTokens.X` is `var ... by mutableStateOf(...)`. Swapping the palette reassigns the underlying values and every reader recomposes. Trade-off: one global palette at a time, `@Preview` renders with whatever was last applied — acceptable, since previews aren't in use. The pure-CompositionLocal alternative would have been a 30-file diff for one toggle.

`ThemePrefs` reads the choice from SharedPreferences synchronously in `Application.onCreate()` before any composition — no first-frame flash. The Settings tab is one centered Dark/Light segmented pill; other toggles are explicitly out of scope unless a real use case appears.

System chrome:
- Status bar hidden; the app paints behind the notch via `shortEdges` cutout mode. Headers carry a `statusBarsIgnoringVisibility` inset so list scrims cover the notch with no seam.
- Bottom tab bar + per-screen headers are translucent (90% Bg) so content faintly shows through as it scrolls past. Both layers absorb taps in their full footprint so dead zones don't fire row taps.
- FABs unified on a shared `XpFab(shadow=true)` at `end=42, bottom=86`.
- Launcher icon: an X mark on white background (replaces the default Android robot).

**Gotchas worth keeping:**
- Vault had hardcoded `Color(0xFF030B0A) → Bg` gradient + `Color(0x0F5EEAD4)` teal-tint chips that didn't flip on theme change. Cut the gradient; added `XpTokens.TealTint` for the chips. The "deeper darkness" mood read as a visible band — flat `Bg` is the right answer.
- Light-mode token palette still has ~20 hardcoded `Color(0x0F5EEAD4)` washes elsewhere — pre-existing, not broken, migrate to `XpTokens.TealTint` if a polish pass picks them up.

---

## Performance

Cold launch is ~260ms from process start to data ready, measured on the iQOO Neo 7. Two wins got us there:

1. **Skip PBKDF2 on DB open.** The 32-byte SQLCipher passphrase from Keystore is already 256 bits of entropy. Pass it as the raw-hex literal SQLCipher recognizes (`x'<64hex>'`) — KDF strengthening adds nothing but ~800ms per cold open. `CipherFastKdf.rawKeyLiteral` handles the formatting.
2. **Splash gate via SplashScreen API.** Launcher icon stays visible until the first DAO emission, so the handoff lands on a populated list instead of a blank one. `NotesViewModel` subscribes to notes + quick-notes separately so notes paint the instant they're ready instead of waiting on the slower stream.

Biometric vault unlock dropped from ~300–500ms to <100ms by wrapping the derived AES key instead of the passphrase (see Vault section).

---

## Architecture & shared primitives

```
app/src/main/java/com/xpotrack/app/
├── MainActivity.kt · XpApp.kt
├── data/
│   ├── alarm/{AlarmScheduler, AlarmReceiver, BootCompletedReceiver, NotificationChannels}.kt
│   ├── db/{Entities, Daos, XpDatabase, CipherFastKdf}.kt
│   ├── model/{Task, Category, ReminderLevel}.kt
│   ├── prefs/{ThemePrefs, EditorZoomPrefs}.kt
│   ├── quick/QuickNoteSweepWorker.kt
│   ├── repo/{Notes, Tasks, Vault, Categories, QuickNotes}Repository.kt
│   ├── repo/RelativeWhen.kt
│   └── security/{PassphraseStore, VaultCrypto, VaultKeyStore, VaultMetaStore, VaultSession, EspHelper}.kt
└── ui/
    ├── AppRoot.kt                          # NavHost: tabs + editor/{id} + task/{id} + quick
    ├── theme/{XpTokens, XpTheme, XpTypography}.kt
    ├── components/                         # ConfirmDeleteDialog, DateTimeStrip, EmptyState,
    │                                       # PinnedHeader, SelectionBar, XpBottomTabs, XpFab,
    │                                       # XpIconBtn, XpPrimaryButton, XpReminderPill, ReminderStyle
    ├── alarm/{AlarmRingingActivity, AlarmRingingScreen, AlarmRingingParts}.kt
    ├── categories/{CategorySheet, ManagerContent, PickerContent, CategoryManagerViewModel,
    │               CategoryDeleteDialog, CategoryColor, HuePicker}.kt
    ├── notes/{NotesListScreen, NotesChronoView, NotesSearchBar, NotesFilterBar, QuickEntryStrip,
    │          NotesEditorScreen, NotesEditorViewModel, NotesFormatBar, MarkdownRender, NoteShare,
    │          EditorCaretScroll, EditorZoom, NotesViewModel, NotesData}.kt
    ├── quick/{QuickEditorScreen, QuickNoteEntry, QuickNotesViewModel}.kt
    ├── tasks/{TasksTimelineScreen, Timeline, DayChips, DateTimeStrip, TasksViewModel,
    │          TaskCreateSheet, TaskCreateSheetParts, TaskCreateViewModel,
    │          TaskDetailScreen, TaskDetailScreenParts, TaskDetailViewModel,
    │          TimeWheel, WheelPicker, RepeatPickerDialog, MonthPickerDialog,
    │          LinkNoteDialog, DialogCard, TaskDateUtils, TasksData}.kt
    ├── settings/SettingsScreen.kt
    └── vault/{VaultGate, VaultSetupScreen, VaultUnlockScreen, VaultListScreen, LockedNoteScreen,
               VaultViewModel, VaultBiometric, VaultData, PassInput}.kt
```

**Layering:** `ui` imports `data`; `data` does not import `ui`. Domain models (`Task`, `Category`, `ReminderLevel`) are pure. UI maps from domain via small adapter functions inside ViewModels — e.g. `Task.toRow()` in `TasksData.kt` called from `TasksViewModel`.

**Shared primitives extracted as the surfaces multiplied:**
- `EspHelper` — single EncryptedSharedPreferences init, used by `PassphraseStore` + `VaultMetaStore`.
- `RelativeWhen` — `formatWhen` for "Today" / "Yesterday" / "Tue" / "Apr 28", used by notes + vault.
- `TaskDateUtils` — `relativeDay`, `dayLabel`, `dayOfWeekTitle` behind one `relativeOr()` core.
- `DialogCard` — the rounded-card surface shared by month / repeat / link-note dialogs.
- `PassInput` — styled password field shared by Setup + Unlock.
- `XpFab`, `XpIconBtn`, `XpPrimaryButton` — replace per-screen FAB/icon/button styling.
- `ConfirmDeleteDialog` — drives delete on notes, tasks, vault, quick notes.
- `EmptyState` — two-line centered copy, per-list adaptive (notes adapts to filter; tasks adapts to past/today/future).
- `PinnedHeader` — overlay header with a 24dp scrim fade so the bottom edge dissolves.
- `SelectionBar` — multi-select count + Select-all + bulk actions.
- Large composable splits: `TaskCreateSheet → +Parts` (151+helpers), `TaskDetailScreen → +Parts` (89 orchestrator + helpers), `AlarmRingingScreen → +Parts`, `CategorySheet → ManagerContent + PickerContent`.

---

## Data model (schema v11)

```
notes         id, title, bodyMarkdown, categoryId?, isPinned, isLocked,
              encryptedBlob?, createdAt, updatedAt
tasks         id, title, time, level, durationMin, notes, isDone,
              reminderAt, dateEpochDay, repeat, linkedNoteId?,
              createdAt, updatedAt
categories    id, name, colorHex, sortOrder
quick_notes   id, text, createdAt, expiresAt
meta          key, value                                    # KV bag
```

**Migration history:** pre-v8 → destructive (`fallbackToDestructiveMigrationFrom(1..7)`). v8 → v9 adds `tasks.dateEpochDay` (backfilled to today). v9 → v10 adds `tasks.repeat` (default `none`). v10 → v11 adds `tasks.linkedNoteId` (nullable). The earlier milestones did destructive bumps freely because there were no real users; real `Migration` objects started landing once the schema stabilized.

Uninstall destroys the Keystore key and the DB is unrecoverable — feature, not bug.

Seed data was removed (commit `6e0705e`). Fresh installs land on empty lists with adaptive `EmptyState` copy across notes / tasks / vault.

---

## What's left

**Ship-readiness.** Real release signing config, ProGuard audited against SQLCipher / Room / Compose, versioning + changelog, first reproducible release build. Crash reporting is explicitly out of scope per plan §1 — a local file logger could be useful instead.

**Remaining polish gaps.** None outstanding. FAB teal glow uses `Modifier.shadow` with `ambientColor`/`spotColor`; `0.5.dp` hairline borders are used consistently across cards and surfaces (the tab bar dropped its top hairline in favor of a 24dp fade-to-transparent edge so content dissolves into the bar instead of meeting a hard line). Empty-state placeholders show a blinking teal caret next to the title for a typing-prompt feel.
