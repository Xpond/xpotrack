# xpotrack — Android Design Spec

Distilled from `misc/mockups/screens/`. Single source of truth for Compose
implementation. If a mockup disagrees with this doc, fix the doc.

## 1. App identity

- **Name:** xpotrack
- **Feel:** dark, quiet, single-accent. Calm but precise.
- **App = TaskNotes plan + a vault tab.** Vault is a top-level destination with its own visual identity (cooler gradient, hairline teal lock chrome). Underlying data still lives in the notes table with an `isLocked` flag — that's an implementation detail and must not surface in the UI; every Vault screen renders exactly as mocked.

## 1a. Design fidelity rule

**The mockups are the spec.** Every screen must render to match the JSX in `misc/mockups/screens/`. No "close enough", no Material-default substitutions, no shortcuts. Where Compose cannot replicate a CSS effect 1:1 (see §10), the deviation is flagged explicitly and resolved with the user before shipping.

## 2. Color palette (deep surface, teal accent)

```
bg            #06100F   base
surface1      #0C1A19   cards / sheets
surface2      #122524   raised
surfaceMute   #091716   bottom strips, format bars

teal          #5EEAD4   primary accent
tealDim       #2DD4BF   secondary accent (labels, glyphs)
tealDeep      #0F766E   pressed
tealGlow      rgba(94,234,212,0.14)  ambient halo

ink           #E6F2EF   primary text
ink2          #8FA8A4   secondary text
ink3          #4F6663   muted text / meta
ink4          #2C3F3D   placeholder

hair          rgba(94,234,212,0.07)  hairline strokes
hair2         rgba(94,234,212,0.12)  hairline strokes (heavier)

reminder.silent  #6B807D   no-reminder tasks
reminder.notify  #5EEAD4   notification reminder
reminder.alarm   #FBBF24   full-alarm reminder
```

Vault uses a slightly cooler/darker gradient as background to telegraph
"separate space" (`#050D0C → #06100F`). No second palette.

## 3. Typography

- **Sans (UI default):** Geist — fall back to system-ui.
- **Mono (numbers, time, meta, cues):** Geist Mono.
- **Serif (rare; blockquote in rendered Markdown only):** Instrument Serif.

Compose: use Geist if we ship it as a font asset, else `FontFamily.SansSerif`
with the Material 3 type scale below.

| Role        | Size | Weight | Letter-spacing | Notes                          |
|-------------|------|--------|----------------|--------------------------------|
| h1          | 30sp | 600    | -0.025em       | Screen title                   |
| h2          | 20sp | 600    | -0.015em       | Sheet title                    |
| Body        | 15sp | 400    | -0.005em       | Reading text                   |
| Body strong | 16sp | 500    | -0.005em       | Editor body                    |
| Meta        | 11.5sp | 500  | +0.08em UPPER  | Mono. Section eyebrows.        |
| Cue         | 10sp | 500    | +0.06em UPPER  | Mono. Status copy.             |

## 4. Shape & rhythm

- Default radius **14dp** (`radius`). Small chips **8dp** (`radiusSm`). Big sheets **22dp** (`radiusLg`). Pills **999dp**.
- Hairline borders **0.5dp** in `hair` or `hair2`.
- Standard horizontal padding **22dp** for screen content, **16dp** for cards inside lists.
- FAB: 56dp, circular, teal fill, ink-on-teal glyph, positioned `bottom 80–86dp`, `right 20–22dp`.

## 5. Components (Compose names → mockup origin)

- **`XpScaffold`** — top status bar + content + bottom-tabs + nav-handle. (We rely on Android system bars, but the mockup status bar is a stylized version — we'll just edge-to-edge with `Modifier.systemBarsPadding()`.)
- **`XpBottomTabs(active)`** — 4 tabs: Notes · Tasks · Vault · More. Active = teal text + icon.
- **`XpReminderPill(level, time)`** — icon + colored time chip. Levels: silent / notify / alarm.
- **`XpCard`** — `surface1` background, 0.5dp `hair` stroke, 14dp radius.
- **`XpFab`** — teal circle with `+` glyph.
- **`XpMeta`** — uppercase mono caption.
- **`XpSegmented`** — pill segmented control (Write / Preview).
- **`XpReminderChip`** — bigger button-sized version of the pill for picker UI.

## 6. Screen inventory & priority

| # | Screen              | Slice  | Notes                                       |
|---|---------------------|--------|---------------------------------------------|
| 1 | Notes list (category) | **MVP-1** | Default home. Grouped + pinned + quick strip. |
| 2 | Notes list (chrono) | MVP-1  | Sort toggle alternate.                       |
| 3 | New note            | MVP-1  | Empty editor + shortcut hint card.           |
| 4 | Note editor (write) | MVP-1  | Hybrid inline-rendered markdown.             |
| 5 | Note editor (preview) | MVP-1 | Fully-rendered.                              |
| 6 | Tasks timeline      | **MVP-1** | Hour grid 6 AM–10 PM, day chips, now line.  |
| 7 | Task detail         | MVP-2  | Big time hero + field rows.                  |
| 8 | Task create (sheet) | MVP-2  | Bottom sheet — **chosen variant**. Wheel + chips. |
| 9 | Alarm ringing       | MVP-2  | Full-screen takeover. Dismiss only (plan §4).|
| 10 | Vault locked list  | MVP-3  | Same shape as notes list, lock chrome.       |
| 11 | Vault unlock       | MVP-3  | Fingerprint + passphrase fallback.           |
| 12 | Locked note open   | MVP-3  | Editor with vault chrome + masked rows.      |
| 13 | Category manager   | MVP-4  | Bottom sheet.                                |
| 14 | Quick notes (24h)  | MVP-4  | Ephemeral.                                   |
| 15 | Settings           | MVP-4  | Groups of toggles.                           |

**Walking-skeleton scope (this PR):** screens 1 + 6 with stub data from Room, plus app shell (bottom tabs + nav). No FAB action, no detail, no alarms, no vault, no per-note encryption — DB-level SQLCipher only.

Task-create variant choice locked: **bottom sheet with time wheel + reminder chips** (variant A). Variants B and C are exploration, not shipping.

## 7. Navigation

```
MainActivity
└─ NavHost (Compose-Navigation)
   ├─ /notes           → NotesListScreen
   │  ├─ /notes/new    → NoteEditorScreen (empty)
   │  └─ /notes/{id}   → NoteEditorScreen
   ├─ /tasks           → TasksTimelineScreen
   │  ├─ /tasks/new    → TaskCreateSheet (modal)
   │  └─ /tasks/{id}   → TaskDetailScreen
   ├─ /vault           → VaultUnlockScreen → VaultListScreen → LockedNoteScreen
   └─ /more            → SettingsScreen
```

Bottom tab persists across `/notes`, `/tasks`, `/vault`, `/more`. Editors,
detail, and modals push above the tab bar (hide tabs).

## 8. Open design questions (defer until we hit them)

- **Quick notes** — plan doesn't mention them; mockups do. Defer to MVP-4; don't model the table yet.
- **Linked note** field on Task detail — defer; add `linkedNoteId` to tasks table later.
- **Custom category colors** — mockup shows a color picker per category; defer until categories ship (MVP-4).

## 10. Fidelity notes (CSS effects that need Compose translation)

These are the spots where a naive Compose implementation will visibly diverge from the mockups. Each needs a deliberate approach:

- **Hairline 0.5dp borders.** Compose rounds to the nearest pixel. On a 3x device 0.5dp = 1.5px which renders as 1px or 2px depending on alignment. Use `1.dp / LocalDensity.current.density` and `Modifier.drawBehind { drawLine(strokeWidth = 1f, ...) }` to draw a true 1-physical-pixel stroke.
- **Teal glow on FAB / status-bar halo.** `box-shadow: 0 8px 24px rgba(94,234,212,0.18)` — Compose `Modifier.shadow()` only does ambient grayscale. Replicate via stacked `drawBehind` radial gradients.
- **Status-bar radial glow** (`xp-app::before` in system.jsx) — draw as a top-aligned radial gradient `Brush` behind content.
- **Time wheel mask gradient** (`WebkitMaskImage: linear-gradient(180deg, transparent, #000 35%, #000 65%, transparent)`) — use `Modifier.graphicsLayer { compositingStrategy = Offscreen }` + `drawWithContent` + `drawRect(brush, blendMode = DstIn)`.
- **Backdrop blur on scrims** (`backdropFilter: blur(2px)`) — Compose has `Modifier.blur()` but only blurs the modified composable, not what's behind. On Android 12+ use `RenderEffect.createBlurEffect` via `Modifier.graphicsLayer { renderEffect = ... }` on the scrim. On <12 fall back to a heavier opacity scrim.
- **Animated pulse rings** (alarm screen) — `rememberInfiniteTransition` with scale + alpha. Three concentric `Box`es with staggered `animationDelay`.
- **Caret blink** in placeholders — same infinite transition pattern.
- **Gradient backgrounds** (Vault `linear-gradient(180deg, #050D0C, var(--xp-bg) 60%)`, alarm `radial-gradient(ellipse at center, #0c2a26 0%, ...)`) — `Brush.verticalGradient` / `Brush.radialGradient`.
- **Font hosting.** Mockups load Geist + Geist Mono + Instrument Serif from Google Fonts. Compose can do this via `androidx.compose.ui.text.googlefonts` (network) or by bundling .ttf in `app/src/main/res/font/`. Decision deferred until first screen — Google Fonts provider needs an internet check at first launch; the plan says "no network code." We'll bundle the .ttf files.

## 11. What we explicitly are NOT building yet

- Search (icon shown on Notes list — wire to a no-op stub).
- Pinning, repeat, snooze, category reordering.
- Markdown shortcut auto-expansion in editor (`# `→ heading on space). Plain TextField first.
- Export to `Documents/TaskNotes/` (plan §6) — deferred to MVP-2 once tasks model is stable.
