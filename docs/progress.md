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

## Milestone 8a — Task create + edit, chronological timeline

The MVP-2 headline split into two passes. **8a (this milestone):** sheet + write path + tap-to-edit. **8b (next):** `AlarmManager` + receivers + ringing activity. Splitting keeps each pass to one verify-on-device cycle.

**Domain model surfaced:** `data/model/Task.kt` (pure data class, no UI / Room imports) anchors the layer line in advance of 8b — the alarm scheduler will live under `data/` and can't see `TaskRow`. Repository now emits `Task`; the UI mapping `Task.toRow()` moved to `ui/tasks/TasksData.kt`, called inside `TasksViewModel`. This resolves the layering exception flagged in the post-milestone-5 audit — repos no longer import `ui/`.

**Schema bumped v2 → v3.** `TaskEntity` gains `updatedAt: Long` and `reminderAt: Long` (absolute epoch ms; 0 until 8b sets it). `fallbackToDestructiveMigrationFrom(1, 2)` — pre-release installs wipe and re-seed. Native lib lock log fires once on first v3 launch (the seed running). `TaskDao` gains `getById` + `upsert`; the speculative `setDone`/`delete` we wrote on the way through got pulled out for being uncalled — they'll come back with milestone 9.

**Bottom-sheet variant A from `task-create.jsx`:** `ModalBottomSheet` (Material 3) with grabber, "NEW TASK" / "EDIT TASK" label, single-line title field, time wheel (HH / MM / AM-PM), Silent/Notify/Alarm chip row, display-only "Repeat — Never" row, full-width teal Schedule button that shows the live time in its label. FAB on Tasks opens it for new tasks; tapping a timeline pill opens it prefilled.

**Course-corrections that cost real cycles in this milestone — write them down so we don't repeat them:**

- **Auto-focus + auto-keyboard on sheet open was a foot-gun.** First pass requested focus inside a `LaunchedEffect`, which fought the sheet's expand animation (laggy entrance) *and* meant system back hit the keyboard before the sheet (double-back to dismiss). Cut entirely. Sheet opens calm; user taps the title field when they want to type.
- **`viewModel(key = "task-create-$id")` reused stale state.** Saving a new task (id=0), dismissing, then opening again returned the *cached* VM instance with the just-saved title still in state. Fixed with a `sheetToken: Int` that increments on every open, baked into the key (`"task-create-$id-$sheetToken"`). Every open gets a fresh VM.
- **Fixed pixel-per-minute timeline grid was unsalvageable.** `HourHeightDp = 56` made a 15-min slot 14dp tall — smaller than the pill's own padding. Tried clamping pill height to next-task offset (with a 28dp floor that lied and let pills overflow anyway), tried bumping to 120dp/hour (wasted space on empty hours, still cramped clusters), tried side-by-side overlap columns (calendar-app correct but the user wanted a list). **The final design is a chronological list:** tasks sorted by minute, each rendered at natural height, stacked top-to-bottom. No hour grid, no fixed vertical scale, no possible cramping. Hour-grid scaffolding (`HourHeightDp`, `MinHeightPx`, `timeToOffsetDp`, `TimelineStartHour/EndHour`) deleted from `TasksData.kt`. The mockup's pixel-grid was a calendar metaphor that doesn't survive a real schedule with closely-spaced tasks.
- **Hand-rolled wheel was broken.** First attempt: custom `pointerInput` + `detectVerticalDragGestures` + manual step accumulator. Jittered on every step, didn't fling, snapped weirdly. Rewrote as `LazyColumn` + `rememberSnapFlingBehavior` — real momentum, real snap. Then made it **infinite-loop** per spec: render `LoopSpan * 2 = 20,000` virtual slots, map slot `i → values[i mod size]`, anchor initial scroll near the middle. Scroll past 59 minutes → wraps to 00 and keeps going. External state changes (e.g. AM/PM swap rewriting the hour) use a `nearestSlotFor` helper that picks the nearest wrap so the wheel never jumps far across the loop boundary.
- **`ModalBottomSheet` ate the wheel's vertical drags as drag-to-dismiss.** A fling on the wheel propagated through nested-scroll once it hit its (effectively endless) bounds and triggered the sheet's dismiss handler — sheet shook and closed mid-spin. Fixed with a `NestedScrollConnection` on the LazyColumn that consumes `available.y` in `onPostScroll` + `onPostFling`, so vertical motion never reaches the sheet.

**Final file shape (tasks/ surface):**

```
ui/tasks/
├── TaskCreateSheet.kt        # sheet chrome + title/chips/repeat/schedule helpers
├── TaskCreateViewModel.kt    # loads by id (0=new), exposes TaskEditState, save()
├── TimeWheel.kt              # 3-column hour:minute AM/PM wheel composed of WheelPickers
├── WheelPicker.kt            # generic infinite-loop snap-to-item wheel
├── TasksData.kt              # TaskRow, Task.toRow(), parseHHmm
├── TasksTimelineScreen.kt    # header + day chips + Timeline + FAB
├── TasksViewModel.kt         # repo.observeAll().map { it.toRow() }
├── Timeline.kt               # chronological list, no grid
└── DayChips.kt               # unchanged
```

**Verified on device:** v2→v3 destructive migration runs cleanly (JNI lock log + re-seed). FAB opens sheet calmly without keyboard. Title + time wheel + chips work — minute wheel scrolls past 59 and wraps to 00 mid-fling, AM/PM swap rewrites the hour without yanking the wheel. Save returns to the timeline with the new row sorted into place at its minute. Tap an existing pill → sheet opens prefilled. Second FAB tap after save gets a blank slate (not the previous task's data). Chronological timeline shows every task at its exact time with no cramping regardless of spacing.

**Deferred to 8b (shipped — see below):** `AlarmScheduler`, `AlarmReceiver`, `BootCompletedReceiver`, `AlarmRingingActivity` — the full alarm side-effects.

**Also deferred (carry-forward):** repeat-row picker (currently display-only "Never"), category chip on the sheet, task-done toggle (needs milestone 9 detail screen), delete flow (same).

---

## Milestone 8b — Alarms

The side-effects half of milestone 8. Sheet + DB already wrote the task in 8a; this pass arms the OS, fires on time, and takes over the lock screen for Alarm-level reminders.

**Files added under `data/alarm/`:** `AlarmScheduler.kt` (wraps `setExactAndAllowWhileIdle`, computes next HH:mm occurrence in the device's local zone, rolls to tomorrow if today's slot has passed), `AlarmReceiver.kt` (routes by level — Notify posts a notification, Alarm posts a full-screen-intent notification), `BootCompletedReceiver.kt` (re-arms every Notify/Alarm task after reboot since Android drops exact alarms on restart), `NotificationChannels.kt` (two channels, `.v2` suffix because Android won't honor edits to an existing channel's importance/sound). Plus `ui/alarm/{AlarmRingingActivity, AlarmRingingScreen}.kt` for the full-screen takeover.

**Repository wiring:** `TasksRepository` now takes an `AlarmScheduler` constructor arg; `upsert` recomputes `reminderAt` from `time` (Silent → 0L, others → next occurrence) and calls `scheduler.schedule()` every write. `TaskDao.setReminderAt(id, at)` added for the boot path. `XpApp` constructs the scheduler, ensures channels exist once at process start, and arms anything Notify/Alarm in the seeded table on first launch (seed inserts bypass `upsert`, so the arming has to happen explicitly).

**Permissions in the manifest:** `USE_EXACT_ALARM` + `SCHEDULE_EXACT_ALARM` (alarm scheduling), `POST_NOTIFICATIONS` (Android 13+ runtime), `USE_FULL_SCREEN_INTENT` (lock-screen takeover), `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `VIBRATE`. `AlarmRingingActivity` declared with `showOnLockScreen="true"`, `turnScreenOn="true"`, `singleInstance`, `excludeFromRecents`. The two receivers registered with explicit `intent-filter`s. `MainActivity` requests `POST_NOTIFICATIONS` once at first launch, and on every resume checks `NotificationManager.canUseFullScreenIntent()` — on Android 14+, if it's denied, bounces to Settings → Special Access with a Toast explainer until the user grants it.

**The course-corrections that cost real time, write them down so we don't repeat them:**

- **Direct `context.startActivity()` from a BroadcastReceiver works unlocked, silently fails locked.** First implementation posted the full-screen-intent notification *and* called `context.startActivity(alarmActivity)` defensively. With the screen on, the direct call won the race and the activity appeared. With the screen locked, Android's background-activity-start restriction (API 29+) silently dropped the call, and although the notification with `setFullScreenIntent(pi, true)` was still posted, we never noticed because the redundant `startActivity()` had masked the test on every prior unlocked verification. The fix: delete the `startActivity()` call. The notification's full-screen-intent is the OS-blessed path for waking a locked device into an activity; nothing else is needed. Lesson: when adding a "fallback" path, verify which path is actually carrying the load — a working unlocked test means nothing about locked behavior if both paths were running.
- **`rememberInfiniteTransition` didn't tick when the activity was launched from a locked-screen BroadcastReceiver.** Pulse rings rendered with constant alpha at the first animation frame and never re-rendered. The `animateFloat` State was being read in the outer Composable scope and captured into the Canvas lambda, but the recomposition trigger somewhere along the path wasn't firing. Switched to an explicit `LaunchedEffect { while (true) t = withFrameNanos { it } - start }` driving a `mutableStateOf<Long>` — direct frame-clock readout, no animation framework. Math for scale/alpha matches the mockup's `@keyframes xp-pulse` (0%/60% breakpoints, scale 0.92→1.18→1.22, alpha 0.8→0). Two rings offset by ⅓ of the 2.4s cycle.
- **Pulse rings were invisible at first because z-order put the radial-glow Box on top of the smaller ring.** Compose paints children in declaration order. The 160dp glow gradient was declared after the Canvas, so it covered the inner ring's draw area. Inverted the order — glow first, Canvas on top — and the rings appeared. (Found this only after first using a constant-alpha magenta diagnostic ring to confirm the Canvas was drawing at all; the diagnostic itself was visible, isolating the issue to the alpha math + z-order.)
- **Notification channels are write-once.** Setting `lockscreenVisibility = PUBLIC` + the alarm sound on an existing channel had no effect — the channel was already created from a previous install. Suffixed both channel IDs with `.v2` to force a fresh creation. Bump the suffix any time channel config changes.
- **Vivo/FuntouchOS overrides `lockscreenVisibility`.** Even with the channel correctly set to `PUBLIC`, the OS shows `mLockscreenVisibility=-1000` in `dumpsys notification`. This is OEM behavior on the test device and can't be worked around in code — the user has to allow lock-screen previews in the device's notification settings. Same OEM gotcha applies to autostart / pop-up-from-background permissions on Vivo, Xiaomi, Oppo etc.

**Final file shape (alarm surface):**

```
data/alarm/
├── AlarmScheduler.kt          # nextOccurrence + schedule/cancel via AlarmManager
├── AlarmReceiver.kt           # routes Notify → notification, Alarm → FSI notification
├── BootCompletedReceiver.kt   # re-arm on boot
└── NotificationChannels.kt    # .v2 channels, lockscreenVisibility=PUBLIC
ui/alarm/
├── AlarmRingingActivity.kt    # showWhenLocked + turnScreenOn, MediaPlayer + Vibrator
└── AlarmRingingScreen.kt      # pulse rings, big mono time, snooze chips (no-op), slide-to-dismiss
```

**Verified on device:** Notify-level task fires a teal-accented heads-up notification at the scheduled minute (lock screen + unlocked). Alarm-level task with phone locked wakes the screen, plays the system alarm sound on loop, vibrates, and brings up the full-screen takeover above the keyguard. Pulse rings animate continuously. Slide-to-dismiss kills sound + finishes the activity and cancels the notification. Re-tested with the screen on — same flow lands. Repository edits to the task's time correctly re-target the scheduler. Seed data on first install arms all 4 non-Silent tasks immediately.

**Carry-forward deferrals:**
- Snooze chips render but no-op per goal §6 ("dismiss only" was the original answer).
- No repeat support — `reminderAt` only handles next-single-occurrence. When milestone 9 adds the detail screen with repeat picker, the receiver will need to re-schedule the next occurrence inside `onReceive`.
- Mark-done from the alarm screen — needs the detail screen's `markDone(id)` repo call (milestone 9).
- Lock-screen content visibility on Vivo/Xiaomi-class OEMs requires manual user setup; document in onboarding when we have one.

---

## Milestone 9 — Task detail

Tap a timeline pill, get `task-detail.jsx`: hero time in mono accent-colored to the reminder level, title, an inline-editable notes paragraph, four field rows (When / Reminder / Repeat / Category) on a surface1 card, full-width teal Mark-done + trash delete. The detail screen owns reads + the notes-edit path; the 8a sheet still owns title / time / level edits, opened by tapping any field-row chevron.

**Files added:** `ui/tasks/TaskDetailScreen.kt`, `ui/tasks/TaskDetailViewModel.kt`. Four vector drawables (`ic_clock`, `ic_repeat`, `ic_tag`, `ic_trash`).

**Schema bumped v3 → v4.** `TaskEntity` gains `notes: String = ""` and `category: String = "General"`. `fallbackToDestructiveMigrationFrom(1, 2, 3)` — pre-release installs wipe and re-seed clean on first v4 launch (JNI lock log + reseed). Seed task rows now carry sensible categories (Ritual/Work/Personal/Health) and notes on the three that benefit from them. `Task` domain model, `TaskRow.toRow()`, `TaskCreateViewModel.TaskEditState`, and `TasksRepository.upsert/toDomain` all propagate the two new fields end-to-end. `TaskDao` gains `markDone(id, now)` and `delete(id)`; `TasksRepository` exposes `markDone(id)` + `delete(id)` that also cancel the scheduled alarm via `scheduler.cancel(id)` before mutating.

**Bug found + fixed in `TaskCreateViewModel.save()`:** the 8a sheet wasn't carrying `notes` or `category` through `Task.toEditState()` ↔ `save()`. Editing an existing task in the sheet would wipe both. Fixed by piping both fields through the edit state and back into the upserted `Task`.

**Navigation rewired.** Added a `task/{id}` route to the existing `NavHost`. Pill tap → `nav.navigate("task/$id")` instead of opening the sheet. FAB still opens the sheet directly (no detail step for new tasks). The sheet state (`sheetTaskId` + `sheetToken`) moved from `TabsScaffold` up to `AppRoot` so the detail-screen route can trigger it too. Detail screen's chevron rows + back affordance both call into the same shared `openSheet` lambda.

**Refresh-in-place instead of re-keying.** First pass keyed `TaskDetailViewModel` on `"task-detail-$id-$sheetToken"` so the VM rebuilt when the sheet closed. That caused a one-frame empty-state flash on every dismiss (screen jitter). Final pass keys on `"task-detail-$id"` only and uses `LaunchedEffect(sheetToken) { vm.refresh() }` to re-read the row in place; old task data stays visible during the (sub-ms) DB read. Same pattern with notes drafts: `refresh()` only seeds `_notesDraft` from DB when `loaded == false`, so opening + dismissing the sheet mid-edit doesn't clobber an unsaved notes draft.

**Inline notes editing on detail.** The mockup shows notes as a static paragraph under the title; we made it a `BasicTextField` with a `"Add notes…"` placeholder. Autosave on system back + on the back chip via a shared `saveAndBack` lambda (mirrors `NotesEditorScreen`'s save-on-back model). `Mark done` also flushes any pending notes draft before flipping `isDone`. ViewModel tracks notes in a separate `MutableStateFlow<String>` so typing doesn't churn the whole detail recomposition.

**Done tasks are read-only.** When `task.isDone`:
- Notes area falls back to plain `Text` (no `BasicTextField`).
- Field-row chevrons are no-ops (the `onAnyRow` callback gates on `!task.isDone`).
- Mark-done button switches to a muted "Done" label on `Surface2`.
- Delete still works (a done task should still be removable).

**Chevrons all open the same sheet.** Per design decision: each `FieldRow` calls `onEdit(task.id)` regardless of which field — there are no per-field pickers yet (the sheet already edits title/time/level; Repeat is display-only "Never"; Category isn't picker-backed until milestone 11). Cleaner than the originally-planned dots-vertical menu, which got removed. Rows also suppress the Material ripple via `indication = null` + `MutableInteractionSource` — same calm-aesthetic pattern as `XpBottomTabs` — because the default 250ms circular ripple visibly jittered while the sheet animated in.

**Course-corrections worth keeping:**

- **Lock-screen alarm "broke" after the v3→v4 destructive migration.** Wasn't actually broken — `dumpsys notification` showed `mLockscreenVisibility=-1000` on the `.v2` channel. Same Vivo OEM quirk documented in 8b: the channel still says `PUBLIC` from our side, the OS overrides it. Bumping `NotificationChannels.NOTIFY_ID` + `ALARM_ID` from `.v2` to `.v3` forces a fresh channel creation so Vivo re-asks for lock-screen permission. (Then the user has to also allow background activity in Vivo's battery settings — autostart + background-power consumption are OEM toggles, not code.) Lesson: any destructive schema bump that wipes an install is a good opportunity to also bump notification channels in case OEM-specific channel quirks have crept in.
- **Notes field looked broken on edit but worked on create.** The sheet's `NotesField` was a `Box` with the placeholder `Text` stacked on top of the `BasicTextField` — `Text` isn't tap-transparent, so taps in the placeholder area never reached the field. On create the empty placeholder filled the touchable area so it worked accidentally; on edit, the saved one-line note shrank the touch target to a single 20dp text line which was hard to hit. Two fixes: move the placeholder into `BasicTextField`'s `decorationBox` slot (same pattern as `NotesEditorScreen.kt:163`), and wrap the field in a 56dp-min-height `Box` whose `clickable` calls `focus.requestFocus()` so taps anywhere in the padded area land focus.
- **Detail screen had no notes-edit path initially.** The first cut showed `task.notes` as a static read-only `Text` (hidden when blank). User pointed out you couldn't edit it without going through the sheet. The right answer was inline editing on detail (notes are heavier than time/level — the chevron-into-sheet pattern doesn't fit) — see "Inline notes editing on detail" above.

**Verified on device:** v3→v4 destructive migration runs cleanly (JNI lock log + reseed). Tap pill → detail with correct time/title/notes/rows. Inline notes editing works on undone tasks; typing + back persists. Field-row chevrons open the sheet pre-filled and edits land back on detail without flash or jitter. Mark done dims the action button + makes everything read-only + cancels scheduled alarm. Delete pops back to timeline + cancels scheduled alarm. System back from detail returns to timeline preserving tab state. Notification channels show `.v3` IDs in `dumpsys notification` after first launch.

**Deferred (carry-forward):**
- Repeat picker (still display-only "Never").
- Category picker (still display-only — milestone 11).
- Per-field pickers in general (When opens the sheet's time wheel, Reminder opens the chip row — fine, but a deeper "tap REPEAT to get a real picker" needs milestone 11+).
- The `task-detail.jsx` "Linked note" card — tasks and notes have no relation in the data model; revisit if a linking feature lands.
- Snooze / mark-done from the alarm ringing activity (was carry-forward from 8b; still carry-forward — needs an alarm-screen → repo path).

---

## Milestone 10 — Vault, unlock, locked-note

The MVP-3 encryption-layer-2 cut. Vault tab graduates from placeholder to a four-phase flow (Setup → Unlock → List → LockedNote), gated by a single passphrase that derives the AES key for every locked-note body. The wider notes table is partitioned by `isLocked` so the regular Notes list never sees vault rows.

**Design decisions locked up front (before writing code):**
- **Single vault passphrase** unlocks all locked notes for the session (not per-note as the goal doc literally reads — that doesn't match the `vault.jsx` mockup which previews all 5 items already unlocked).
- **Freeform markdown body**, encrypted as one blob per note. Mockup's structured `SecretRow` (Bank/Account/Password fields with reveal toggles) is rendered approximately as a mono-font markdown body — the per-row reveal interaction is deferred.
- **First-run setup screen** (create passphrase + confirm + biometric opt-in) before the unlock screen ever shows. No deferred "create the first locked note → prompt for passphrase" flow.
- **Vault-only entry point.** FAB in Vault creates locked notes. Existing-note → lock action deferred. Keeps milestone 10 tight.

**Schema v4 → v5.** `NoteEntity` gains `encryptedBlob: ByteArray?`. When `isLocked = true`, `title`/`category` stay plaintext (the list needs to render them) and `bodyMarkdown = ""` with ciphertext in `encryptedBlob`. `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)`. `NoteDao.observeAll()` now filters `isLocked = 0`, new `observeLocked()` for the vault list. `count()` also filtered so the seed-on-first-launch guard still works.

**Crypto stack:**
- **PBKDF2-HMAC-SHA256, 210k iterations, 256-bit key** (Apple iOS default). Initially 600k per OWASP password-storage guidance, but that derivation takes 1–3 seconds on this phone and the threat model is *device-local* — the DB is already SQLCipher-wrapped, so the vault is layer two. 210k is ~3× faster with no meaningful loss of strength against on-device guessers. Lower than this would start to feel sloppy.
- **AES-256-GCM** for body encryption. Per-note random 16-byte salt + 12-byte IV prepended to ciphertext: blob layout `[salt | iv | ct+tag]`. HMAC-SHA256 stretches the vault key with the per-note salt for domain separation (each note gets a unique AES key without re-running PBKDF2).
- **Verifier**: PBKDF2(passphrase, salt) stored at setup. Unlock re-derives and constant-time compares — passphrase is never stored.
- **Biometric (optional)**: Android Keystore AES key, `setUserAuthenticationRequired(true)` + `setInvalidatedByBiometricEnrollment(true)`. Wraps the passphrase bytes. `BiometricPrompt` authorizes the `Cipher`, then `wrap`/`unwrap` runs. Adding or removing a fingerprint nukes the key — user falls back to passphrase, which is the right semantics.

**Files added under `data/security/`:** `VaultCrypto.kt`, `VaultMetaStore.kt` (EncryptedSharedPreferences for salt + verifier + biometric blob/IV), `VaultKeyStore.kt`, `VaultSession.kt` (in-memory key + auto-lock timer). Under `data/repo/`: `VaultRepository.kt`. Under `ui/vault/`: `VaultData.kt` (`LockedNote` + `LockedNoteRow`), `VaultViewModel.kt` (the `Setup/Unlock/List/Note` phase machine), `VaultBiometric.kt` (BiometricPrompt wrapper), `VaultSetupScreen.kt`, `VaultUnlockScreen.kt`, `VaultListScreen.kt`, `LockedNoteScreen.kt`, `VaultGate.kt` (sub-router replacing the stub). Drawables: `ic_lock`, `ic_fingerprint`, `ic_shield`.

**The course-corrections that cost real time — write them down so we don't repeat them:**

- **`FragmentActivity` switch detonated on clear-data**. `BiometricPrompt` requires `FragmentActivity`, so `MainActivity` switched from `ComponentActivity`. That transitively pulled in `androidx.fragment` 1.3.x via `androidx.biometric:1.2.0-alpha05`. The older fragment lib has a stricter `requestCode` validator (16-bit only) that *clashes with `ComponentActivity`'s `ActivityResultRegistry`*, which uses 32-bit codes. Result: the very first `registerForActivityResult` launch — the `POST_NOTIFICATIONS` prompt on first run — threw `IllegalArgumentException: Can only use lower 16 bits for requestCode` and the activity died before `setContent`. Symptom: the app launched fine on existing installs (permission already granted, no result-launch on resume) but crashed instantly after `pm clear`. Fix: pin `androidx.fragment:fragment-ktx:1.8.5` in the version catalog so the modern fragment lib wins the transitive resolution. **Lesson:** when changing the activity superclass, recheck *every* lifecycle hook touched on first-run. The "works on existing install" check is worthless if it can't survive a fresh install.
- **PBKDF2 was running on the main thread**. `vm.unlockWithPassphrase` was a regular `fun` doing key derivation inline. UI froze for the whole 1–3s derivation, and "Wrong passphrase" feedback was equally delayed. Refactored every key-deriving entrypoint (`setupPassphrase`, `unlockWithPassphrase`, `unlockWithBiometric`) into `viewModelScope.launch { withContext(Dispatchers.Default) { ... } }`. Added a `verifying: StateFlow<Boolean>` so the Unlock button shows "Verifying…" the instant the user taps and disables itself until the result lands. Compose recomposes on the immediate state flip, then again when the derivation finishes — no main-thread stall, no perceived lag.
- **Lock-now stranded the user on the unlock screen**. First cut: tap the teal lock-now button on the vault list → `session.lock()` → `init { session.state.collect }` flips `_phase` to `VaultPhase.Unlock`. The user is *trying to leave the vault*, not unlock it again. Fix: `VaultGate` accepts an `onLockExit` callback; lock-now calls both `vm.lockNow()` and `onLockExit()`. `AppRoot` lifts the active-tab state above `TabsScaffold` so the callback flips back to Notes. Next Vault-tap shows Unlock — which is the right place at that point.
- **`' '` vs `' '` invisible-character edit hazard.** A `java.util.Arrays.fill(chars, ' ')` line ended up in the file as `java.util.Arrays.fill(chars, ' ')` after the initial write. The Read tool displays ` ` as a space, so subsequent Edit calls trying to match `' '` failed silently with "string not found". Resolved by piping through Python with explicit `\x00` in the search string. **Lesson:** when an Edit fails on a string that looks identical to what's in the file, suspect invisible chars — bypass Edit and use a tool that shows bytes.
- **Auto-lock copy + chrome bloat**. Initial vault list had a hardcoded "Auto-lock in 5m · device-only" status strip ripped from the mockup. User pushed back: don't show what's already implicit. Removed the strip; locked-note footer ("Encrypted on this device · Auto-locks in 5m") and unlock screen footer ("Auto-locks after 5 minutes of inactivity") also dropped. Cut auto-lock window from 5 min → **1 min** while at it — the original 5 was mockup-faithful but too lax for a passwords-grade surface.

**Final file shape (vault surface):**

```
data/security/
├── VaultCrypto.kt          # PBKDF2 + AES-GCM, per-note salt
├── VaultMetaStore.kt       # salt/verifier/biometric blob in EncryptedSharedPreferences
├── VaultKeyStore.kt        # biometric-wrapped passphrase via Keystore
└── VaultSession.kt         # in-memory SecretKey + 1-min auto-lock timer
data/repo/
└── VaultRepository.kt      # observeLocked(), open(id,key), upsert(note,key), delete(id)
ui/vault/
├── VaultData.kt            # LockedNote + LockedNoteRow
├── VaultViewModel.kt       # Setup/Unlock/List/Note phase machine
├── VaultBiometric.kt       # BiometricPrompt wrapper
├── VaultGate.kt            # sub-router rendered by TabsScaffold for XpTab.Vault
├── VaultSetupScreen.kt     # first-run passphrase + biometric opt-in
├── VaultUnlockScreen.kt    # fingerprint badge + passphrase fallback + verifying state
├── VaultListScreen.kt      # vault.jsx chrome + masked previews + FAB
└── LockedNoteScreen.kt     # locked-note.jsx chrome + markdown editor
```

**Verified on device:** v4 → v5 destructive migration runs cleanly (JNI lock log + reseed). Clear-data → relaunch lands on Setup (not crash). Setup with biometric toggle creates the vault, prompts for fingerprint, lands on empty List. FAB opens LockedNote with keyboard up. Type + back persists; row appears in the list. Tap the row → reopens with the decrypted body. Tap lock-now → drops back to Notes tab and clears the in-memory key. Re-tap Vault → Unlock with fingerprint or passphrase, lands back on List with the row preserved. Switch to Notes — locked rows do *not* appear. Idle 1 min on the vault tab → re-tap Vault → Unlock. Wrong passphrase fires immediately, Unlock button reads "Verifying…" only during the derivation window.

**Carry-forward deferrals:**
- Per-row "reveal" interaction from `locked-note.jsx` (`SecretRow.reveal`) — we render markdown.
- Live "Auto-lock in 4m 12s" countdown chip from `vault.jsx` — we cut the chip entirely, the session timer is real.
- Moving an existing plain note into the vault (no lock action on `NotesEditorScreen`).
- Passphrase change. No recovery path — by design.
- Category picker inside locked notes (display-only "Vault" until milestone 11).
- Dots-vertical "more" menu in `LockedNoteScreen` (same as `NotesEditorScreen`).

---

## Milestone 11 — Category manager + custom categories

The MVP-4 category cut. `category-manager.jsx` lands as a bottom sheet — create, rename, recolor, delete. Notes gain a real foreign key to a `categories` table, so renames survive on every screen instead of requiring an O(n) string rewrite. Tasks lose category entirely — it never belonged on a reminder.

**Design decisions locked up front (some after a first-pass course-correction):**
- **Notes own categories. Tasks don't.** First pass mirrored the mockup and put a Category row in `TaskCreateSheet` + a Category field-row in `TaskDetailScreen`. After landing it: the user pushed back — a reminder isn't filed under "Work" the way a note is. Stripped category from `Task`, `TaskEntity`, `TaskRow`, `TaskEditState`, `TaskDetailState`, both sheets, and the alarm path (which never read it anyway). The field is dead from the data model up.
- **No built-in categories.** Mockup ships 4 built-ins (Personal/Work/Ideas/Inbox) + 3 customs. After landing: dropped all of them. The seeded list felt presumptuous — every category in the app is now user-created. First-launch state is an empty manager and an "Uncategorized" group on the notes list.
- **Delete reassigns to Uncategorized**, not a protected Inbox category. `NoteEntity.categoryId` is nullable; the `@Transaction` `deleteAndUncategorize` clears it on every note in the deleted category, then drops the row. No `isProtected`, no fallback target. Reversible by editing any orphaned note and picking a new category.
- **Locked vault notes stay outside the category table.** `VaultRepository` writes `categoryId = null` and continues to display the synthetic "Vault" label. The partition holds.

**Schema v5 → v6 → v7.** Two destructive bumps in one milestone because the model changed twice: v6 added the `categories` table + `categoryId` columns on notes + tasks; v7 dropped `categoryId` from tasks. `fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)` so any pre-release install lands clean on first v7 launch (JNI lock log + reseed runs the seed inserts, which now ship Uncategorized notes + plain tasks).

**New files (`ui/categories/`):** `CategoryManagerSheet.kt` (207 lines — the inline rename/create row plus header + delete confirm), `CategoryManagerViewModel.kt` (Setup/Edit/PendingDelete state machine), `CategoryPickerSheet.kt` (the small picker shared by note editor), `CategoryDeleteDialog.kt` (Dialog-based confirm so the chrome matches), `CategoryColor.kt` (`#RRGGBB` → Compose `Color`). New `data/model/Category.kt` + `data/repo/CategoryRepository.kt`. One drawable: `ic_pencil`. One drawable removed: `ic_tag` (was for the killed task category field row).

**Wiring (notes side only):**
- `NotesEditorScreen` — tap the category meta line (`"Uncategorized · N words"`) to open the picker. The picker exposes a `Manage categories…` tail row that swaps in the manager sheet.
- `NotesCategoryView` — `+ New category` button at the end of the list opens the manager; long-press on any category header opens it too. A live `Uncategorized` group renders at the bottom when there are orphaned notes (so they're never invisible after a delete).
- `AppRoot` hosts the manager + picker sheets at the root level so they can stack from any editor.

**Repository-side join.** `NotesRepository.observeAll()` `combine`s the note flow with the category flow and resolves `categoryId → name` per emission. Renames and recolors propagate live without cache invalidation. `NoteRow` carries `categoryId` + `categoryName`; `categoryColor` was on the row in the first pass and got cut during cleanup — the UI reads the live `Category.colorHex` from the categories list, not a stale snapshot on the row.

**Course-corrections worth keeping (write them down so we don't repeat them):**
- **Lambdas can't go through `rememberSaveable`.** First pass at `AppRoot` modeled the picker target as a sealed interface `PickerOwner` carrying an `onPick: (Long) -> Unit` lambda, persisted with `rememberSaveable(stateSaver = autoSaver())`. The autoSaver tried to bundle a function reference and crashed on first picker open. Replaced with plain `remember { mutableStateOf<((Long) -> Unit)?>(null) }` — picker state is transient by nature; losing it on process death is fine.
- **A category color baked into `NoteRow` lies after a recolor.** Initial repo mapping snapshotted `cat.colorHex` into `NoteRow.categoryColor`. Worked, but then a recolor in the manager only updated rows after the next `combine` emission, and any UI that read from a held-on-to `NoteRow` instance would show the old color until the next recomposition. Fixed by reading `Category.colorHex` directly from the live `categories` list in `NotesCategoryView`, and dropping `categoryColor` from `NoteRow` entirely. The lesson — denormalizing live-mutable fields onto downstream models is a cache, and a cache without invalidation is just a bug waiting to fire.
- **A second wipe in one milestone is fine if the data shape changed twice.** First pass shipped v6 with task categories; second pass stripped them. Rather than try to migrate the in-flight v6 install, bumped to v7 and added `fallbackToDestructiveMigrationFrom(1..6)`. Pre-release wipes cost nothing and avoid carrying a junk column for one install cycle.

**Final file shape (categories surface):**

```
data/model/Category.kt                # pure domain, no UI / Room
data/repo/CategoryRepository.kt       # observeAll / add / rename / recolor / deleteAndUncategorize
data/db/Entities.kt :: CategoryEntity # name + colorHex + sortOrder
data/db/Daos.kt :: CategoryDao        # @Transaction deleteAndUncategorize(noteDao)
ui/categories/
├── CategoryManagerSheet.kt           # add/rename/recolor/delete sheet + inline editor row
├── CategoryManagerViewModel.kt       # Edit / PendingDelete state machine
├── CategoryPickerSheet.kt            # tap-to-pick + Manage… tail row
├── CategoryDeleteDialog.kt           # confirm-then-uncategorize
└── CategoryColor.kt                  # hex → Compose Color
```

**Verified on device:** v5 → v7 destructive migration runs cleanly (JNI lock log + reseed). Fresh install lands on Notes tab with all seeded notes under Uncategorized + the "New category" dashed button at the bottom. Tap it → manager sheet opens calmly without keyboard. `New` reveals the inline-create row; pick a color, type a name, `Create` → row appears + sheet stays open for the next one. Pencil → inline rename + color swap; `Save` → row updates everywhere (list group rename propagates instantly via the `combine` join). Trash → confirm dialog with the right `N notes will become uncategorized` summary; confirm → category disappears, orphaned notes fall into the Uncategorized group. Editor's meta-line tap opens the picker prefilled with the current selection; `Manage categories…` tail row swaps sheets; back from manager returns to the picker target (the editor's category updates on next pick). Tasks tab has no category UI anywhere — sheet doesn't show a Category row, detail card doesn't show a Category field, picker can't open from a task.

**Carry-forward deferrals:**
- Reorder. The manager's drag-handle from the mockup is unrendered; sort order is fixed at create time. `CategoryEntity.sortOrder` is in place for when reorder lands.
- Per-category icons (mockup only uses color dots — same here).
- Moving a category between built-in / custom sections — there are no sections now.
- The locked-note category picker (still display-only "Vault").
- `NotesChronoView`'s category prefix renders as the resolved name but doesn't take the category color (the chrono view was never tinted per-category in the mockup either — leaving it).

**Project shape after this milestone:** ~6175 lines of Kotlin across 65 source files. Largest file `NotesEditorScreen.kt` at 251, then `CategoryManagerSheet.kt` at 207, then `NotesCategoryView.kt` at 220 (up from 219 with the Uncategorized group).

---

## Milestone 12 — Quick notes (24h ephemeral)

The disappearing-notes surface from `quick-notes.jsx`. The strip at the top of the notes list graduates from static placeholder to live count + live "oldest expires in …" subtitle. Tap it → the dedicated Quick screen with an inline-expand compose row, per-row countdown chip + progress ring, **Keep** action that promotes to the regular notes table, **Clear all**, and the post-save dialog. Rows live for exactly 24h and disappear via a sweep on screen open + a 24h `WorkManager` periodic best-effort sweep.

**Design decisions locked up front:**
- **Separate table** `quick_notes` (`id`, `text`, `createdAt`, `expiresAt`). Keeps the partition clean — the regular notes flow never sees them, same pattern as `isLocked` for vault.
- **No category, no title, no markdown** on quick notes. The mockup shows a single text body per entry; don't speculate.
- **Inline-expand compose row** instead of a new editor screen or modal sheet. Tap the dashed row → it morphs into a `BasicTextField` with the keyboard up + Save/Cancel inline. No new route, no sheet chrome — calmest path.
- **Keep = move + delete** in a single `withTransaction`: insert into `notes` as Uncategorized (`categoryId = null`), delete the quick row. First line becomes the note title; whole text becomes the body. Row can't end up in both lists.
- **Sweep model**: a single source of truth on screen open via the VM (`repo.sweepExpired()` in `init`). The `WorkManager` `PeriodicWorkRequest` (24h, `KEEP` policy) is best-effort, so the strip on the notes list stays fresh even if the user never visits Quick. Cron jobs aren't reliable on OEM-restricted devices anyway.
- **No "Don't show again" preference** for the post-save dialog yet — DataStore lands in milestone 13 (Settings). The checkbox from the mockup is documented as deferred.

**Schema v7 → v8.** New `quick_notes` table + `QuickNoteDao` + `quickNoteDao()` on `XpDatabase`. `fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)`. Pre-release installs wipe and re-seed (JNI lock log + reseed runs once on first v8 launch).

**Files added:**

```
data/db/Entities.kt :: QuickNoteEntity
data/db/Daos.kt :: QuickNoteDao            # observe(now), getById, upsert, delete, deleteExpired, deleteAll
data/repo/QuickNotesRepository.kt          # observe, add, sweepExpired, deleteAll, keep (@Transaction)
data/quick/QuickNoteSweepWorker.kt         # 24h periodic, KEEP policy, calls repo.sweepExpired
ui/quick/
├── QuickNotesViewModel.kt                 # rows + justSaved + 30s tick; sweep on init
├── QuickNotesScreen.kt                    # header + inline-expand compose row + section meta + list + bottom strip
├── QuickNoteEntry.kt                      # row chrome + countdown chip + progress ring (drawArc)
└── QuickSavedDialog.kt                    # post-save dialog + Clear-all confirm
```

**Wiring:**
- `XpApp` constructs `QuickNotesRepository(db, db.quickNoteDao())` and calls `QuickNoteSweepWorker.enqueue(this)` once per process start (Worker's own `KEEP` policy prevents duplicate registrations).
- `NotesViewModel` takes the repo and exposes `quickSummary: StateFlow<QuickSummary>` (count + oldest-left label). The label is derived from the live `observe()` flow via `map`, recomputed on every emission.
- `QuickEntryStrip` was a static placeholder before this milestone; now takes `count`, `oldestLeft`, and `onClick`. Threaded through `NotesListScreen → NotesCategoryContent / NotesChronoContent` so both list modes light up.
- `AppRoot` registers a `quick` route with `viewModel(key = "quick-notes", factory = QuickNotesViewModel.Factory(app.quickNotesRepo))`. Strip tap → `nav.navigate("quick")`. Back from Quick `popBackStack()` returns to the notes tab preserving tab state.

**Course-corrections worth keeping:**
- **`flatMapLatest` was theater.** First pass wrote `flow { emit(now) }.flatMapLatest { dao.observe(it) }` to "freshen" the `now` parameter on each subscription. The outer flow only ever emits once, so this is identical to `dao.observe(System.currentTimeMillis())` plus an unused opt-in suppression. Cut it. The expiry semantics still hold: `now` is captured at subscribe time, and the DAO's `expiresAt > :now` filter only re-emits when the table mutates — so the sweep on screen open + the periodic worker do the work, exactly as designed.
- **Verification window vs the mockup-faithful copy.** During verification the lifetime briefly went to 1 minute with a 2s tick and a generic "expires at H:mm" string so the full cycle (create → countdown → expire → sweep → row gone) could be observed in under two minutes. Restored to 24h + 30s tick + the mockup's "expires tomorrow, h:mm a" line before commit. The `remainingLabel` helper still has the `< 1 minute → seconds` fall-through from the test pass; harmless and useful for the final-minute strip subtitle.
- **`SavedDialog` carries the just-saved id.** First attempt for "Move to Notes" in the dialog tried `rows.value.firstOrNull()` — racing the `combine` re-emission. Replaced with `SavedDialog(id, expiresAt)` so the dialog knows exactly which row to promote. No race, no ordering assumption.
- **`Dialog`'s `onDismissRequest` already covers outside-tap dismiss.** First pass added an explicit `.clickable` on the dialog scrim with a `remember { MutableInteractionSource() }` to suppress ripple. Redundant + ugly. The platform Dialog fires `onDismissRequest` on outside-tap and on back, both of which we already route to `onGotIt`. Cut.
- **File-size discipline.** `QuickNotesScreen.kt` finished at 271 lines — over the 200-line target. Tried splitting Header / ComposeRow / SectionMeta / BottomStrip into a `QuickNotesChrome.kt` file but every piece is single-purpose and only called from this screen; the file split was pure churn. Documented overage; accept.

**Verified on device:** v7→v8 destructive migration runs cleanly (JNI lock log + reseed). Tap the **Quick** strip on the notes list → Quick screen with empty state and the dashed compose row. Tap the row → it expands inline with keyboard up. Type → **Save** → post-save dialog appears with the correct "expires tomorrow, h:mm AM" line; row appears below the compose row with `23h 59m` countdown. Strip on the notes list now reads `1 · 24H` with the right "oldest expires in" subtitle. **Keep** on a row promotes it to the regular Notes list as Uncategorized (visible in the Uncategorized group, body = full text, title = first line). **Move to Notes** in the dialog does the same on the just-saved row. **Clear all** opens the confirm dialog, then wipes. **Got it** dismisses the post-save dialog. WorkManager registers the periodic sweep at process start and fires successfully on first launch (`WM-WorkerWrapper: Worker result SUCCESS for ... QuickNoteSweepWorker`).

**Cleanup pass (post-verification):**
- Restored 24h lifetime + 30s tick + mockup-faithful dialog copy + `"expires tomorrow, h:mm a"` format.
- Cut the redundant `flatMapLatest` + `@Suppress("OPT_IN_USAGE")` on `observe()`.
- Cut unused `QuickNoteDao.current(now)` (only `observe(now)` is reached).
- Cut unused `QuickNotesRepository.delete(id)` (never called externally; DAO still has it for the `keep` transaction).
- Cut dead `rememberCoroutineScope()` + `val s = scope` placeholder in `ComposeRow`.
- Cut redundant `.clickable` on `QuickSavedDialog`'s scrim — `Dialog.onDismissRequest` covers it.
- Replaced two `Spacer(Modifier.width(0.dp).weight(1f))` with the proper `Spacer(Modifier.weight(1f))`.

**Project shape after this milestone:** ~7030 lines of Kotlin across 71 source files. New files: 6 (4 ui + 1 data/repo + 1 data/quick). Largest milestone-12 file: `QuickNotesScreen.kt` at 271 (documented above). All other new files: 36–169 lines.

**Carry-forward deferrals:**
- "Don't show this again" checkbox on the post-save dialog — waiting on DataStore in milestone 13.
- Live "Auto-expires in 4m 12s"-style per-row countdown via a smoother animation. Currently the 30s `tick` updates the chip text only; the progress ring is a static snapshot per emission. Re-render is fine at 30s granularity for a 24h window; revisit if/when granularity is needed.
- Per-row swipe-to-delete (mockup only shows the inline **Keep** action).
- "Quick" branding on the regular-notes row after a **Keep** — the promoted note lands with `categoryId = null` (Uncategorized). No marker that it was ever a quick note.
- The strip's tap → expand-the-strip-inline interaction from the mockup. Currently the strip just navigates to the Quick screen — clearer and matches the second-page layout in `quick-notes.jsx`.

---

## Remaining roadmap

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
