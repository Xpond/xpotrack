# Changelog

Versioning follows SemVer for a single-user, local-only app:

- **MAJOR** — incompatible data changes (backup format, vault crypto). Old backups from a prior MAJOR may not restore.
- **MINOR** — new user-visible features or significant behavior shifts.
- **PATCH** — bug fixes and polish.

`versionCode` is derived from `versionName` in `app/build.gradle.kts`
(`MAJOR*10000 + MINOR*100 + PATCH`), so bumping the name updates the code automatically.

## [1.1.0] - 2026-10-30

Adds the Expense tab.

### Artifacts

Built from commit `9f4fbc5` on `main`.

- `app-release.apk` — 10,046,568 bytes — SHA-256 `5ae7d8467af4ef12cd4a1a679b4cf7c1b973fc4617694848cc46ec3876e31e12`
- `app-release.aab` — 7,578,158 bytes — SHA-256 `2b0e9a3d80caf39749acfd15768a4b37ba1a8f9728b5df4830ab1d168f3e21ea`

Signed with the personal release keystore. As with 1.0.0, these SHAs are an integrity record, not a reproducibility contract.

### Features
- Expense: one free-text entry per day, one expense per line with a trailing amount (`Lunch 260`, `Groceries 1,240.50`, `Lunch Rs.260`, `Bus 20 rupees`). Lines without an amount stay as notes and count zero
- Live day and month totals, parsed from the text and never stored
- Month picker opens past or future days and marks logged dates
- Every edit saves in order and survives leaving the app; clearing a day removes it

### Fixes
- Vault: opening a note and returning without edits no longer changes its timestamp or list position; returning before it loads no longer overwrites or deletes it
- Dialogs: cap width at 420dp on tablets; the month picker scrolls in short windows
- Bottom tabs use equal widths so five labels fit on narrow phones

### Data
- Room schema v12 adds `expense_days` through a non-destructive v11→v12 migration
- Expenses are included in backups; a 1.1.0 backup can't be restored on 1.0.0

## [1.0.0] - 2026-05-26

Initial release. Local-only notes + tasks + vault, no network, no analytics.

### Artifacts

Rebuilt from commit `c61af57` on the `stable` branch after the editor
caret-scroll fix. Supersedes the original `4c97820` build of v1.0.0.

- `app-release.apk` — 10,013,208 bytes — SHA-256 `41d359f2dc461e5c2a1046472421e8cc4be0572da7dfd8d6eac32b95e65e7d94`
- `app-release.aab` — 7,536,459 bytes — SHA-256 `9f5aca0a97375f1fec4b45628493cd08ff3db985810ad575b95d5411998619a4`

APK signed with v2 scheme using the personal release keystore. Re-builds from the same source will not produce byte-identical artifacts until the steps in `docs/reproduciblebuild.md` land — these SHAs are an integrity record, not a reproducibility contract.

### Features
- Notes: chronological list, categories, search, multi-select, share, markdown editor with Write/Preview toggle, pinch-to-zoom
- Tasks: per-day timeline, three-column time wheel, recurrence (none/daily/weekly/weekdays), Silent/Notify/Alarm reminders
- Quick notes: 24h ephemeral scratch with countdown chip; Keep promotes into regular notes
- Vault: passphrase + optional biometric, AES-256-GCM per note, FLAG_SECURE, 1-min auto-lock
- Backup: encrypted `.xpb` export/restore via SAF, in-app relaunch splash
- Categories: user-created, color picker (hue ring), one sheet for pick + manage
- Light + Dark theme
- Alarms: heads-up notifications + full-screen-intent ringing screen with snooze and hold-to-done

### Build
- Kotlin 2.1.0, Compose BOM 2024.12, AGP 8.7.3, Gradle 8.10.2
- Room 2.6.1 over SQLCipher 4.6.1 (whole-DB AES)
- compileSdk 35, minSdk 29, targetSdk 35, arm64-v8a only
- R8 minify + resource shrink, audited keep rules
- Signed with personal release keystore (PKCS12, RSA 4096, 50-year)
- `data-extraction-rules.xml` excludes everything from Google Auto Backup and D2D transfer
