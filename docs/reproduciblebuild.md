# Reproducible builds

Goal: any third party can clone a tagged release, build with documented tooling, and produce an APK / AAB whose SHA-256 matches the artifact we publish. This is the same standard F-Droid uses, and it lets a privacy-focused app prove that the binary on a user's device was built from the source on GitHub.

We are **not** there yet. This doc lists what needs to land before we can claim reproducible builds for v1.0.0 onward.

---

## Status (today)

Already in place:

- `app/build.gradle.kts → android.dependenciesInfo { includeInApk = false; includeInBundle = false }` — strips dependency metadata hashes that would otherwise bake the build host's resolved versions into the APK.
- `gradle-wrapper.properties` pins Gradle 8.10.2 by SHA via the distribution URL — every clone uses the exact same Gradle.
- `libs.versions.toml` pins every library to an exact version (no `+` or dynamic ranges).
- `release.properties` and `*.keystore` are gitignored — the signing key is not in the repo. Signing is a per-builder concern; the *unsigned* APK is the reproducible artifact, and the published signed APK is verified by re-signing locally.
- Single-ABI build (`arm64-v8a` only) — eliminates one source of variance between build hosts.
- No annotation processors that embed timestamps (KSP is deterministic given fixed inputs).

Not in place yet — the checklist below.

---

## What "complete" means

A reproducible-build release means **all** of:

1. A third party can clone the repo at a given tag.
2. With the documented JDK, Gradle, and Android SDK, they run one command.
3. The resulting `app-release-unsigned.apk` (and `app-release.aab`) has a SHA-256 byte-identical to ours.
4. They can then sign their own copy with their own key for personal use, **or** verify our published signed APK by stripping the signature block and comparing against their unsigned build.

The signed APK itself is not byte-identical between builders — every signing key produces a different signature block. Reproducibility is about the *payload*, not the signature.

---

## Checklist

### 1. Pin the JDK exactly

Today the project builds on JDK 21 (any patch level). Reproducibility needs an exact JDK build because `javac` bakes the JDK version into class file metadata.

**Action:**

- Decide on a JDK distribution + version: e.g. `Eclipse Temurin 21.0.5+11` (LTS, widely reproducible across mirrors).
- Document the exact version in `README.md` + this file.
- Add a CI workflow (`.github/workflows/reproducible.yml`) that installs that exact JDK and runs the build twice, asserting the two output SHAs match. CI proves the recipe still works as Android SDK / Gradle move.

### 2. Pin the Android SDK Build-Tools

`aapt2`, `dex2oat`, `zipalign`, and `apksigner` all live under `Android/Sdk/build-tools/<version>/`. The version is auto-selected from `compileSdk`.

**Action:**

- Set `buildToolsVersion = "35.0.0"` explicitly in `app/build.gradle.kts` (currently implicit via `compileSdk = 35`).
- Document the version in `README.md`.

### 3. Strip timestamps from the APK / AAB

The Android build embeds the build time into:

- ZIP entry timestamps inside the APK (every file in the archive)
- The `META-INF/MANIFEST.MF` `Created-On` header (when signed; not relevant for the unsigned reproducible artifact)
- A `BuildConfig.java`-generated timestamp if any code calls `System.currentTimeMillis()` at build time (we don't, currently)

**Action:**

- Honour `SOURCE_DATE_EPOCH` in the build: a standard env var (in seconds-since-epoch) that build tools clamp all internal timestamps to. AGP 8.x partially supports this; the safe move is to set `project.ext["org.gradle.internal.publish.checksums.insecure"] = true` is **not** the answer (that's checksum-related), but rather to:
  - Set `archivesBaseName` / output to a fixed name (not `<version>-<timestamp>.apk`).
  - Add a Gradle task that post-processes the APK ZIP and rewrites all entry mtimes to `SOURCE_DATE_EPOCH` if set, falling back to the commit time of HEAD. F-Droid's `fdroidserver` does exactly this with its `repomaker` tool; we can borrow the script idea.
- Verify by building twice with the same `SOURCE_DATE_EPOCH` and comparing SHAs.

### 4. Disable / pin R8 baseline profile generation

AGP can embed a "baseline profile" into the APK that ART uses to AOT-compile hot paths. Profile generation is sampled and **non-deterministic** between runs. The release APK currently includes one (see `app/build/outputs/apk/release/baselineProfiles/`).

**Action:**

- Either disable baseline profiles for reproducible releases (`androidx.baselineprofile` plugin removed / option to skip), or commit the generated baseline-prof.txt to the repo so every builder uses the same one.
- Verify by inspecting `assets/dexopt/baseline.prof` inside the APK across two builds.

### 5. Strip absolute paths and machine-specific data

R8 sometimes embeds debug paths into the `proguard.map` output, but not into the APK itself. Verify with:

```
unzip -l app-release-unsigned.apk | grep -iE "path|debug|home"
strings app-release-unsigned.apk | grep -iE "/home/|/Users/|C:\\\\"
```

**Action:**

- Run the grep above on a candidate build, document the result. If anything personal appears, identify the source (likely R8 config or a Compose-generated identifier) and gate it.

### 6. Make resource processing deterministic

`aapt2` has historically had non-determinism around resource hash ordering and PNG crunching. Build-tools 35.0.0 is good but worth verifying.

**Action:**

- Set `android.nonFinalResIds = false` (already the default; document it).
- Set `android.enableR8.fullMode = true` (already true via `proguard-android-optimize.txt`; verify).
- Build twice on the same machine with `--rerun-tasks`, diff the resulting unsigned APKs entry-by-entry, document any drift.

### 7. Lock the Kotlin / Compose compiler plugin versions

These are already pinned in `libs.versions.toml`. The reproducible-build risk is `kotlin.compose` plugin embedding the host's Kotlin version metadata.

**Action:**

- Confirm `kotlin = "2.1.0"` and `kotlin-compose` plugin alias resolve to a single version. They do. Document this.

### 8. Document the verification recipe

Once the above land, a third party needs a one-page recipe:

```
# 1. Clone at a tag
git clone https://github.com/<user>/xpotrack
cd xpotrack && git checkout v1.0.0

# 2. Install the pinned JDK + Android SDK
#    (versions documented in README.md)

# 3. Build
SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct) ./gradlew clean assembleRelease

# 4. Compare SHA
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
# Expect: <SHA from CHANGELOG.md>

# 5. (Optional) Verify our published signed APK
#    Strip the signature block from our signed APK, then compare to step 3:
apksigner extract-cert ...   # or unzip + remove META-INF/*.RSA, *.SF, MANIFEST.MF
sha256sum <stripped-apk>
```

**Action:**

- Add this recipe to `docs/RELEASING.md` (also to be created), once steps 1-7 are done.

### 9. Publish artifact SHAs alongside each release

The published SHA is the contract. Without it there's nothing for a third party to compare against.

**Action:**

- In `CHANGELOG.md`, under each release section, add:
  ```
  ### Artifacts
  - app-release-unsigned.apk: sha256:<...>
  - app-release.aab: sha256:<...>
  ```
- Generate these via `sha256sum` and paste into the CHANGELOG before tagging.

### 10. CI guard

Without continuous verification, one of the above subtly breaks and we don't notice for months.

**Action:**

- Add `.github/workflows/reproducible.yml`:
  - On push to `main` and on tag push:
    - Set up exact JDK + Android SDK
    - Build twice in two separate workdirs (use the same `SOURCE_DATE_EPOCH`)
    - `sha256sum` the unsigned APKs
    - Fail the workflow if they differ
- On tag push, also upload the unsigned APK as a release asset (so users can download what we built).

---

## Order of operations when we tackle this

1. Steps 1, 2, 7 — pin JDK + build-tools, document, no code change beyond `buildToolsVersion`. **30 min.**
2. Step 4 — decide on baseline profiles (keep + commit, or drop). **30 min.**
3. Step 3 — `SOURCE_DATE_EPOCH` plumbing, the actual reproducibility lever. **2-4 hours**, plus testing.
4. Steps 5, 6 — verify by diffing two builds, fix whatever non-determinism shows up. **Unknown until step 3 lands.**
5. Steps 8, 9 — docs + CHANGELOG format. **1 hour.**
6. Step 10 — CI workflow. **2 hours.**

Realistic landing: one focused day if no surprises in step 5/6, two days if there are.

---

## What we are NOT trying to solve

- **Signed-APK reproducibility.** Each signing key produces a unique signature block. Reproducibility ends at the unsigned APK; the signed APK is verified by comparing payloads after stripping the signature, not by hashing the signed file directly.
- **Cross-platform build reproducibility (Linux vs macOS vs Windows).** F-Droid's experience is that this is doable but the marginal value over "Linux-only reproducibility" is small for a hobby project. We'll document the OS we built on (Arch Linux) and call non-matching-OS builds out of scope unless someone files an issue.
- **Toolchain bootstrapping.** We assume the user has a JDK and Android SDK from upstream sources. Reproducing the JDK/SDK themselves is a multi-year project that nobody outside Linux distros attempts.

---

## Why bother

For a single-user app, reproducibility is theatre. For an **open-source privacy app that handles encrypted vault data**, it is the strongest possible signal that the source on GitHub is what's on the user's phone — there is no hidden code path, no backdoor inserted at build time, no analytics quietly added between source and binary. F-Droid will not accept the app without it. Users who care about local-only crypto are exactly the users who will check.
