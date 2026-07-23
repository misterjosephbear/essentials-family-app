# Auto-Update System - How It Works

## Overview

Isaac's Hub has an **automated** update system that requires **ZERO manual intervention**. When you push code to the `main` branch, GitHub Actions automatically builds, signs, and publishes a new release. Users get update notifications automatically when they restart the app.

## How It Works (Step by Step)

### 1. You Push Code to Main
```bash
git push origin main
```

### 2. GitHub Actions Automatically:
- Builds a signed release APK with the release keystore
- Increments version using `github.run_number` (e.g., 126, 127, 128...)
- Embeds the `UPDATE_CHECK_TOKEN` secret into the APK
- Creates a GitHub Release tagged as `v<run_number>` (e.g., `v126`)
- Uploads the APK as a release asset to that GitHub Release

### 3. User's App Automatically:
- Checks GitHub Releases API when launched: `https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/latest`
- Compares the tag version (e.g., 126) with their installed version
- Shows an update banner if a newer version is available
- Downloads and installs directly from GitHub when they tap "Update"

## Architecture

```
┌─────────────────┐
│  git push main  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  GitHub Actions (.github/workflows/     │
│  release.yml)                           │
│  - Builds signed APK                    │
│  - Version = github.run_number          │
│  - Embeds UPDATE_CHECK_TOKEN            │
│  - Creates Release with tag v<version>  │
│  - Uploads APK as release asset         │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  GitHub Release (e.g., v126)            │
│  - Tag: v126                            │
│  - Asset: app-release.apk (81MB)        │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  User's App (UpdateChecker.kt)          │
│  - Fetches latest release from GitHub   │
│  - Parses tag to get versionCode        │
│  - Compares with installed version      │
│  - Shows update banner if newer         │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  User taps "Update"                     │
│  - UpdateInstaller downloads APK        │
│  - Launches Android installer           │
│  - User confirms installation           │
└─────────────────────────────────────────┘
```

## Key Files

### 1. `.github/workflows/release.yml`
The GitHub Actions workflow that automates everything:
- **Trigger**: Runs on every push to `main`
- **Version**: Uses `github.run_number` (auto-incrementing)
- **Secrets Used**:
  - `RELEASE_KEYSTORE_BASE64` - The signing keystore
  - `RELEASE_KEYSTORE_PASSWORD` - Keystore password
  - `RELEASE_KEY_ALIAS` - Key alias
  - `RELEASE_KEY_PASSWORD` - Key password
  - `UPDATE_CHECK_TOKEN` - GitHub PAT for checking releases

### 2. `app/src/main/java/com/isaacshub/app/update/UpdateChecker.kt`
Checks GitHub Releases API for new versions:
- URL: `https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/latest`
- Uses `UPDATE_CHECK_TOKEN` for authentication (private repo)
- Parses tag name (e.g., `v126`) to extract versionCode (126)
- Returns `ReleaseInfo(versionCode, versionName, assetId)`

### 3. `app/src/main/java/com/isaacshub/app/update/UpdateInstaller.kt`
Downloads and installs the APK:
- URL: `https://api.github.com/repos/misterjosephbear/isaacs-hub/releases/assets/<assetId>`
- Uses `UPDATE_CHECK_TOKEN` for authentication
- Handles GitHub's redirect to pre-signed URL
- Launches Android's package installer

### 4. `app/src/main/java/com/isaacshub/app/update/UpdateViewModel.kt`
Manages the update UI:
- Runs update check on app launch
- Shows update banner when available
- Handles download progress
- Dismisses notifications

### 5. `app/build.gradle.kts`
Build configuration:
```kotlin
val releaseVersionCode = System.getenv("RELEASE_VERSION_CODE")?.toIntOrNull() ?: 116
val releaseVersionName = System.getenv("RELEASE_VERSION_NAME") ?: "1.16.0"
```
**NOTE**: The hardcoded values (116, 1.16.0) are only used for local builds. GitHub Actions overrides these with environment variables.

## Normal Workflow (What You Should Do)

### When Making Changes:

1. **Make your code changes**
2. **Commit**: `git add -A && git commit -m "Your message"`
3. **Push**: `git push origin main`
4. **Wait 8-10 minutes** for GitHub Actions to complete
5. **Done!** The new version is automatically available

### Checking Build Status:
```bash
gh run list --limit 1
gh run watch <run-id>
```

### Checking Latest Release:
```bash
gh release list --limit 1
gh release view v<version>
```

### Testing on Emulator (Optional):
```bash
# Download latest release
gh release download v<version> --pattern "app-release.apk" --clobber

# Start emulator
/home/bear/android-sdk/emulator/emulator -avd test_device -no-window -no-audio &
/home/bear/android-sdk/platform-tools/adb wait-for-device

# Install and test
/home/bear/android-sdk/platform-tools/adb install -r app-release.apk
/home/bear/android-sdk/platform-tools/adb shell am start -n com.isaacshub.app/.MainActivity

# Clean up
/home/bear/android-sdk/platform-tools/adb emu kill
```

## CRITICAL RULES - READ THIS

### ❌ DO NOT:
1. **DO NOT** manually create GitHub Releases - the workflow does this automatically
2. **DO NOT** change the version numbers in `build.gradle.kts` - they're overridden by CI
3. **DO NOT** try to use a different update system (like version.json on web server)
4. **DO NOT** modify `.github/workflows/release.yml` without understanding it completely
5. **DO NOT** change `UpdateChecker.kt` to use anything other than GitHub Releases API
6. **DO NOT** change `UpdateInstaller.kt` to download from anywhere except GitHub

### ✅ DO:
1. **DO** just push to main and let GitHub Actions handle everything
2. **DO** wait for the workflow to complete (8-10 minutes)
3. **DO** verify the release was created: `gh release list --limit 1`
4. **DO** test on emulator if making changes to critical systems
5. **DO** trust that the system works - it's fully automated

## Version Numbering

- **versionCode**: Automatically set to `github.run_number` by CI
  - Example: Run #126 → versionCode 126
  - This is an auto-incrementing integer
- **versionName**: Automatically set to `1.0.<run_number>` by CI
  - Example: Run #126 → versionName "1.0.126"
  - This is a user-facing string

## Troubleshooting

### "I pushed but users aren't seeing updates"
1. Check if the workflow ran: `gh run list --limit 1`
2. Check if it succeeded (should say "completed" and "success")
3. Check if a release was created: `gh release list --limit 1`
4. Check if the release has the APK asset: `gh release view v<version>`

### "The workflow failed"
1. View the logs: `gh run view <run-id>`
2. Common issues:
   - Gradle build errors (fix code and push again)
   - Signing errors (check GitHub secrets)
   - Network issues (re-run the workflow)

### "I accidentally broke UpdateChecker.kt"
1. **STOP** - Do not try to fix it with a new system
2. **REVERT** to the last working version:
   ```bash
   git log --oneline app/src/main/java/com/isaacshub/app/update/
   git show <commit>:app/src/main/java/com/isaacshub/app/update/UpdateChecker.kt
   ```
3. Copy the working version back
4. Push the fix

## What Changed (Historical Context)

### Before (Broken):
- Tried to use version.json on web server
- Required manual deployment
- Update detection didn't work because:
  - App checked GitHub Releases API
  - But releases weren't being created
  - APK was deployed to web server without metadata

### After (Working):
- Uses GitHub Releases API (original design)
- Fully automated via GitHub Actions
- Everything happens automatically on push
- No manual steps required

## Testing the Update System

### Test 1: Fresh Install
1. Install older version (e.g., v125)
2. Launch app
3. Should NOT see update banner (v125 is current for that install)

### Test 2: Update Available
1. Install older version (e.g., v124)
2. Launch app
3. Should see "Update available 1.0.125" banner
4. Tap "Update"
5. Should download and prompt to install
6. Install and verify version: `adb shell dumpsys package com.isaacshub.app | grep versionCode`

### Test 3: Already Current
1. Install latest version (e.g., v126)
2. Launch app
3. Should NOT see update banner

## Emergency Rollback

If a release is broken:
```bash
# Delete the bad release
gh release delete v<bad-version>

# Users will stay on the previous version
# Fix the issue and push again - new version will be created
```

## Summary

**The auto-update system works perfectly when left alone.**

1. You push code
2. GitHub Actions builds and publishes
3. Users get updates automatically
4. No manual intervention needed

**DO NOT** try to "improve" or "fix" the update system unless it's actually broken. It's designed to be hands-off and automatic.
