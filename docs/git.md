# git.md — branch & review workflow

Cheat sheet for the two-branch setup. Read this when you forget how anything works.

## The branches

- **`main`** — dev branch. Commit freely, break things, tinker.
- **`stable`** — known-good bookmark. Only points at commits verified to work on the phone. Never commit directly here.

Both track `origin/` on GitHub: `git@github.com:Xpond/xpotrack.git`.

## Promote `main` → `stable`

Run this when a commit on `main` has been built, installed, and used on the phone without regressions.

```
git checkout stable
git merge --ff-only main
git push origin stable
git checkout main
```

`--ff-only` refuses if `stable` has commits `main` doesn't. If it ever fails, you accidentally committed to `stable` directly — investigate, don't paper over.

Optional: tag the promotion for an audit trail.

```
git tag -a stable-YYYY-MM-DD -m "Verified: <what you tested>"
git push origin stable-YYYY-MM-DD
```

List all promotions: `git tag -l 'stable-*'`

## Roll back to `stable` when `main` is broken

```
git checkout stable
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

When done: `git checkout main`.

## Feature work + agent review (PR flow)

Agents (`/ultrareview`, `/review`) need a PR to look at. So new work goes through a feature branch → PR → `main`.

```
git checkout main
git pull
git checkout -b feature/<name>

# work, commit, commit

git push -u origin feature/<name>
gh pr create --base main --title "..." --body "..."
```

In the PR: run `/ultrareview <PR#>` or `/review`. Address feedback, push more commits. Merge when happy.

After merge:

```
git checkout main
git pull
git branch -d feature/<name>
# test on phone — if good, promote to stable (see above)
```

## Full-project review (whole codebase as one PR)

GitHub PRs need shared history, so the trick is to base the PR off the very first commit (`c921a21` — initial scaffold). The diff = everything built on top of it.

```
git branch review-base c921a21
git push -u origin review-base
gh pr create --base review-base --head main \
  --title "Full project review" \
  --body "Whole-codebase audit pass — do not merge"
```

Run `/ultrareview <PR#>` against it. **Do not merge this PR.**

Cleanup when done:

```
gh pr close <PR#>
git push origin --delete review-base
git branch -d review-base
```

PR discussion stays in the repo's PR history; the branch goes away.

## Protection layers (don't commit/push to main or stable)

Two layers catch direct edits to `main` / `stable`:

**Local pre-commit hook** (`.git/hooks/pre-commit`) — refuses commits when you're on `main` or `stable`. Catches the mistake before any commit exists. If triggered:

```
git checkout -b feature/<name>   # staged changes follow you
git commit -m "..."
```

The hook is not tracked by git. On a fresh clone, reinstall by copying from another checkout, or recreate:

```sh
cat > .git/hooks/pre-commit <<'EOF'
#!/bin/sh
branch=$(git symbolic-ref --short HEAD 2>/dev/null)
if [ "$branch" = "main" ] || [ "$branch" = "stable" ]; then
  echo "ERROR: direct commits to '$branch' are blocked."
  echo "Create a feature branch: git checkout -b feature/<name>"
  exit 1
fi
EOF
chmod +x .git/hooks/pre-commit
```

Bypass (rarely): `git commit --no-verify`.

**GitHub branch protection** (server-side backstop) — set at https://github.com/Xpond/xpotrack/settings/branches for both `main` and `stable`. Enabled:

- Require a pull request before merging
- Do not allow bypassing the above settings
- (Force pushes and deletions left disallowed by default)

Catches the case where the local hook is missing (fresh clone, different machine) or was bypassed. Rejects `git push origin main` with "protected branch hook declined."

## Why `--ff-only` matters

A fast-forward just slides `stable`'s pointer along `main`'s line — no new commit, clean. Without `--ff-only`, if histories ever diverge, git silently creates a merge commit on `stable`, breaking the "stable is just a pointer to a good commit on main" invariant. `--ff-only` enforces that invariant by refusing instead of inventing history.

## Quick reference

| Task | Command |
|---|---|
| Promote main → stable | `git checkout stable && git merge --ff-only main && git push origin stable && git checkout main` |
| Roll back to stable | `git checkout stable` |
| New feature | `git checkout -b feature/<name>` then PR via `gh pr create --base main` |
| Full-project review | branch off `c921a21`, PR against it, run `/ultrareview <PR#>` |
| List promotions | `git tag -l 'stable-*'` |
