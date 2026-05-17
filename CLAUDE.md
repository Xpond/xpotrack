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

## MCP Tools

ResourceBot exposes 10 MCP tools that read from `.rb/filelog.json` and `.rb/sessions.json` — Claude Code's own session JSONLs, parsed into a searchable per-file and per-session history. Use them whenever you need to know **what changed, when, why, or by whom** in this project.

Two axes for picking a tool:

- **Scope** — single file (`rb_file_*`) or cross-file (`rb_recent_activity`, `rb_search_edits`, `rb_session_*`).
- **Question** — *what changed* (history/diff tools), *where was X touched* (search tools), or *why did this happen* (session tools).

When an edit tool shows `[s#N]` next to a line, that's a pointer into `rb_session_read(N)` for the narrative behind that edit.

### `rb_file_log(path)`

**Use when:** you want a quick read on a file's recent history — when it was created, what's changed in the last 10 edits, and which sessions touched it. This is the default entry point for any "what's the story with this file?" question. Last 3 edits render as full diffs, older ones as one-line summaries. Pass full path or just a filename — partial matches are resolved automatically.

```
rb_file_log("rb/server.py")
rb_file_log("server.py")
```

### `rb_file_edit(path, edit_number)`

**Use when:** `rb_file_log` showed a summary line (e.g. `#5  ...  ~ 'def foo'`) and you need the full diff for that specific edit.

```
rb_file_edit("rb/server.py", 5)
```

### `rb_file_edits(path, start, end)`

**Use when:** a file has more than 10 edits and you want to paginate beyond what `rb_file_log` shows. Range is inclusive, max 10 per call, newest first, summary format.

```
rb_file_edits("rb/server.py", 1, 10)
rb_file_edits("rb/server.py", 11, 20)
```

### `rb_file_search(path, keyword)`

**Use when:** you want every edit to a specific file where a function name, variable, or pattern appeared in the old or new content. Useful for tracing the evolution of one symbol within one file. Latest 3 matches shown as full diffs, older as summaries.

```
rb_file_search("rb/server.py", "def retrieve")
rb_file_search("rb/filelog.py", "format_search")
```

### `rb_file_at(path, point)`

**Use when:** you need to see what a file looked like at a specific moment — before a regression, at a tagged edit, or as of a date. Reconstructs the full file by replaying its edit history. `point` is an edit number (matching `#N` in `rb_file_log`) or an ISO timestamp. `point="0"` means the file at creation, before any edits.

```
rb_file_at("rb/server.py", "0")
rb_file_at("rb/server.py", "15")
rb_file_at("rb/server.py", "2026-05-08T17:00:00")
```

### `rb_file_diff(path, point_a, point_b)`

**Use when:** you want to know what changed in a single file between two points — "what's different from edit #10 to now," or "what changed between Monday and Wednesday." Same `point` format as `rb_file_at`. One call instead of two `rb_file_at` reads + manual diff.

```
rb_file_diff("rb/server.py", "0", "15")
rb_file_diff("rb/server.py", "10", "20")
rb_file_diff("rb/server.py", "2026-05-07T00:00:00", "2026-05-08T00:00:00")
```

### `rb_recent_activity(hours=24, limit=50)`

**Use when:** you're orienting yourself — "what's been happening in this project recently?" — or summarizing what a session touched. Flat chronological feed of every Write/Edit across all files, newest first, plus a "files touched" summary sorted by edit count. Default window: last 24 hours.

```
rb_recent_activity()
rb_recent_activity(hours=48)
rb_recent_activity(hours=168, limit=100)
```

### `rb_search_edits(keyword, path_filter=None, appears_in_either=False, limit=20)`

**Use when:** you want every place a keyword was touched **across the whole project** — added, removed, or modified. The cross-file counterpart to `rb_file_search`. Each match is tagged `[added]`, `[removed]`, `[modified]`, or `[context]`. Narrow with `path_filter` (substring match on path). Set `appears_in_either=True` to also surface edits where the keyword existed unchanged in both sides.

```
rb_search_edits("FastMCP")
rb_search_edits("FastMCP", path_filter="rb/")
rb_search_edits("FastMCP", appears_in_either=True)
```

### `rb_session_list(limit=10)`

**Use when:** the user references a past session ("the one where we built X") and you need to find its serial, or you want a quick chronological index of recent work. Each row: `s#N  date  AI-generated title`. The `s#N` serial is what every edit tool prints inline and what `rb_session_read` takes.

```
rb_session_list()
rb_session_list(limit=20)
```

### `rb_session_read(serial)`

**Use when:** an edit tool showed `[s#12]` next to a line and you want the narrative behind it — what the user was trying to do, what the AI decided, which files got touched and why. Returns the AI-generated title, a 300–400 word summary, and a footer listing every file the session created or edited with the matching per-file edit numbers. This is the bridge from receipts (edits) back to story (intent).

```
rb_session_read(11)
```

---

## Inline `[s#N]` annotations

After phase 3, every edit/event line in these tools shows a session tag:
`rb_file_log`, `rb_file_edit`, `rb_file_edits`, `rb_file_search`,
`rb_recent_activity`, `rb_search_edits`.

- `[s#12]` → session is summarized; pass `12` to `rb_session_read` for the story.
- `[s#?]` → session not summarized yet (typically the live session, or one filtered as trivial).

`rb_file_at` and `rb_file_diff` aren't annotated (they reconstruct content, not lists).
