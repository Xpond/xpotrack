# xpotrack

Local-only Android notes + tasks app. Single user, single device, no network.

## What's in it

- **Notes** — chronological list, categories, search, multi-select, share, markdown editor with Write/Preview toggle, pinch-to-zoom
- **Tasks** — per-day timeline, three-column time wheel, recurrence (none/daily/weekly/weekdays), Silent/Notify/Alarm reminders
- **Quick notes** — 24h ephemeral scratch with countdown chip; Keep promotes into regular notes
- **Vault** — passphrase + optional biometric, AES-256-GCM per note, FLAG_SECURE, 1-min auto-lock
- **Backup** — encrypted `.xpb` export/restore via SAF, in-app relaunch splash
- **Categories** — user-created, color picker (hue ring), one sheet for pick + manage
- **Alarms** — heads-up notifications + full-screen-intent ringing screen with snooze and hold-to-done
- Light + Dark theme

## What it isn't

- **No cloud.** No sync, no accounts, no servers.
- **No analytics, no telemetry, no crash reporters.** Nothing leaves the device.
- **No network code at all.** The app has no `INTERNET` permission.
- **No ads.** No tracking SDKs.
- **No multi-user.** It assumes one user on one device.

This is a personal app published as source for transparency, not a product. Uninstall destroys the Keystore key and renders the encrypted DB unrecoverable — feature, not bug.

## Stack

Kotlin 2.1.0, Jetpack Compose (BOM 2024.12), Room 2.6.1 over SQLCipher 4.6.1 (whole-DB AES), AndroidX EncryptedSharedPreferences for the DB passphrase. `compileSdk = 35`, `minSdk = 29`, `targetSdk = 35`, `arm64-v8a` only.

## Build

Requirements: JDK 21, Android SDK with build-tools 35.0.0.

```
git clone https://github.com/Xpond/xpotrack.git
cd xpotrack
echo "sdk.dir=/path/to/your/Android/Sdk" > local.properties
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.xpotrack.app/.MainActivity
```

For a release build, see `docs/reproduciblebuild.md` and the release section in `CLAUDE.md`. Release signing uses a personal keystore — see `release.properties` (gitignored).

## Versioning

SemVer. `versionCode` derives from `versionName` (`MAJOR*10000 + MINOR*100 + PATCH`), so only bump `versionName` in `app/build.gradle.kts`. See `CHANGELOG.md` for release history.

## License

MIT — see [LICENSE](LICENSE).
