# Changelog

Versioning follows SemVer for a single-user, local-only app:

- **MAJOR** — incompatible data changes (backup format, vault crypto). Old backups from a prior MAJOR may not restore.
- **MINOR** — new user-visible features or significant behavior shifts.
- **PATCH** — bug fixes and polish.

`versionCode` is derived from `versionName` in `app/build.gradle.kts`
(`MAJOR*10000 + MINOR*100 + PATCH`), so bumping the name updates the code automatically.

## [1.0.0] - 2026-05-26

Initial release. Local-only notes + tasks + vault, no network, no analytics.

### Artifacts

Built from commit `4c97820` on the `stable` branch.

- `app-release.apk` — 10,013,208 bytes — SHA-256 `615e6d91d9543606dc4dcd99b014052ab61fdd3c2ea910462a9963c1d47c8a78`
- `app-release.aab` — 7,535,582 bytes — SHA-256 `bcebeeb3f0569fc677adc30c7625eab48e6d1b495dab12ca294647c5b6d6e2e8`

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
