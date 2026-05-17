TaskNotes — Technical Plan
1. Stack & Architecture
A single-module native Android app, Kotlin, Jetpack Compose for UI. Target SDK 35 (Android 15), min SDK 29 (Android 10) — covers essentially any phone you'd use. Local-only, no network code, no analytics, no cloud.
Architecture is standard MVVM: Compose UI → ViewModel → Repository → Room (SQLite). Room is the official SQLite wrapper and pairs cleanly with SQLCipher for the encryption requirement. Dependency setup stays minimal — Room, SQLCipher, AndroidX Biniometric, Compose, and the Markdown bits.
2. Data Model (Room / SQLite)
Three tables.
notes — id, title, bodyMarkdown (TEXT, no length cap — this directly fixes your "few hundred words" wall), createdAt, updatedAt, isLocked (boolean), encryptedBlob (nullable, used only when locked).
tasks — id, title, notes, dueAt (epoch millis), alarmType (enum: NONE / NOTIFICATION / FULL_ALARM), isDone, createdAt, recurrence (nullable; daily support since you mentioned daily tasks).
meta — small key/value table for things like schema version and the salt for key derivation.
The whole database file is encrypted at rest with SQLCipher (see §5), so even non-locked notes aren't sitting in plaintext if someone pulls the .db off the device. The per-note "lock" is a second layer on top, specifically for passwords.
3. Notes
Plain Markdown editor — a Compose TextField over the raw .md text, with a toggle to preview rendered Markdown (using a lightweight Markdown renderer). No rich-text engine, no hidden formatting state — what you type is what gets exported, which keeps it from "getting messy" like your current app. Unlimited length.
4. Tasks & Alarms
Task creation: title + date/time picker + alarm-type selector (none / notification / full alarm), matching the per-task choice you wanted. Daily recurrence as a simple repeat toggle.
Scheduling uses AlarmManager.setExactAndAllowWhileIdle() with USE_EXACT_ALARM declared in the manifest — fires at the precise minute even in Doze. A BroadcastReceiver catches the alarm and either posts a notification or launches a full-screen alarm Activity (the "blaring alarm" path) with sound + vibration + dismiss/snooze. A second receiver on BOOT_COMPLETED reschedules all future alarms after a reboot, since Android drops alarms on restart. Your 4 PM call → full alarm is exactly the headline use case here.
5. Encryption (the locked-notes requirement)
Two layers, as you agreed:
Whole-DB: SQLCipher encrypts the entire SQLite file with AES-256. The database passphrase is generated once, stored in the Android Keystore (hardware-backed), and never written to disk in the clear.
Per-note lock: when you lock a note, its body is encrypted with a key derived from your password via a strong KDF (Argon2 or PBKDF2 with high iteration count), salted per-note. The plaintext is then wiped from the row — only encryptedBlob remains. Without the password the content is unrecoverable, even by the app itself. There is deliberately no recovery path; that's the point of a password vault.
Fingerprint unlock: BiometricPrompt gates a Keystore key that can decrypt a stored copy of your note password. Fingerprint is a convenience unlock over the same encryption — not a replacement for it — so the data is still useless without the underlying key material.
6. Export for Tailscale + Waybar
A single "Export" action (and an auto-export-on-save option) writes to a fixed, easy-to-reach folder: Documents/TaskNotes/.
It produces: one .md file per note (locked notes excluded by default, or exported still-encrypted — your choice at export time), plus a tasks.json snapshot for the Waybar gadget. This folder is the single thing you point Tailscale/Syncthing at. The app never does networking itself — clean separation, nothing to secure.
tasks.json shape will be something like a list of {title, dueAt, alarmType, isDone} plus a precomputed summary string, so the Waybar script is a trivial jq read with zero logic.


Q: When you export, how should locked/password notes be handled?
A: Exclude them entirely (safest)

Q: Auto-export to the Tailscale folder, or manual button only?
A: Manual button only

Q: For the full alarm, snooze behavior?
A: Dismiss only


