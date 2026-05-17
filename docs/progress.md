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

## Remaining roadmap

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
