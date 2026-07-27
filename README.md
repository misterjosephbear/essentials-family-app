# Isaac's Hub

An Android "all-in-one" productivity app combining multiple independent tools in a single app. Built with Kotlin, Jetpack Compose, and Material 3.

## Features

### Sleep Health Tracker
Auto-detect sleep sessions from phone signals (no wearable required) and track rolling **sleep debt** over time, similar to Rise Sleep Tracker's core concept.

- **Auto-detection** (`sleep/detection/`) - Foreground service monitors screen state and motion sensors
- **Sleep debt calculation** (`sleepcore/SleepDebt.kt`) - Tracks deficit/surplus against your sleep need target
- **Manual entry** - Confirm or edit auto-detected sessions, or add sessions manually
- **Nap alarm** (`sleep/nap/`) - Foreground-service timer for timed naps

### Route Helper
Build mail-delivery routes by driving them once, then replay turn-by-turn on later days.

- **Route Builder** (`routehelper/ui/builder/`) - Live GPS recording with auto-discovered addresses using Census TIGER/Line data and Microsoft building footprints
- **Route Player** (`routehelper/ui/player/`) - GPS navigation with rotating map (direction of travel always faces up)
- **Mail Scanner** (`routehelper/ui/mailscan/`) - Camera + ML Kit OCR to scan addresses from envelopes and add stops on the fly
- **Route Editor** (`routehelper/ui/edit/`) - Reorder or remove stops after recording

### Time Tracking
Log work hours by route with evaluations and overtime tracking.

- Weekly schedule view with past/future week pagination
- Per-day schedulable routes with notes
- Overtime calculations

### Photo & App Vault
QR-pairs with an `isaacs-hub-storage` server instance for automatic backups.

- Auto-backup photos and videos (preserves GPS metadata)
- Backup Room databases and app preferences
- Resumable uploads with remote-tunnel fallback
- Live progress indicators

### Banking
View all account balances in one place using Plaid integration.

### Essentials
Manage family chores and device restrictions (available as standalone app at [essentials-family-app](https://github.com/misterjosephbear/essentials-family-app)).

### Feature Funnel
Queue and manage feature requests for automated development via Discord integration.

### Activity Mapper
Automation system for triggering actions based on conditions.

- **Variables** - Track system state (IS_SLEEPING, CURRENT_ACTIVITY, etc.)
- **Rules** - Create automation rules with conditions and logical operators (AND/OR)
- **Actions** - Currently supports Discord Rich Presence integration
- **CRUD UI** - Full interface for managing rules and Rich Presence profiles

## Module Structure

- **`:app`** - Main Android app with all features
- **`:sleepcore`** - Pure Kotlin/JVM module for sleep debt calculation and detection logic (16 passing unit tests)
- **`:essentialscore`** - Shared logic for Essentials features

## Auto-Updater

The app includes an in-app auto-updater that checks GitHub Releases for new builds. CI automatically publishes signed APKs on every push to `main` via `.github/workflows/release.yml`.

**Note**: Update checks require a valid `UPDATE_CHECK_TOKEN` (fine-grained GitHub PAT with Contents read-only access). If you see a "Bad credentials" error, the token needs to be regenerated in the repository secrets.

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Sleep detection notifications + nap alarms |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep detectors running overnight |
| `RECEIVE_BOOT_COMPLETED` | Restart services after reboot |
| `CAMERA` | Mail envelope scanning (Route Helper) |
| `ACCESS_FINE_LOCATION` | GPS tracking for Route Helper |
| `INTERNET` | Vault sync, update checks, API integrations |

## Building

```bash
./gradlew build              # Full build
./gradlew :sleepcore:test    # Run sleep logic unit tests
./gradlew installDebug       # Install debug build on device/emulator
```

### Signing

Both debug and release builds use the same signing key for in-app update compatibility. CI uses secrets for production builds; local builds use default values from `app/build.gradle.kts`.

## Development Environment

- `compileSdk` / `targetSdk`: 36
- `minSdk`: 26 (Android 8.0+)
- Kotlin 2.0+ with Compose compiler
- Java 17

## Architecture

- **UI**: Jetpack Compose with Material 3
- **Navigation**: Jetpack Navigation Compose
- **Data**: Room + Jetpack DataStore
- **Networking**: OkHttp + Retrofit
- **Dependency Injection**: ViewModelProvider.Factory pattern
- **Concurrency**: Kotlin Coroutines + Flow

## Contributing

This is a personal project but issues and PRs are welcome. See `PROJECT_STATUS.md` for current development status.

## Related Projects

- [isaacs-hub-storage](https://github.com/misterjosephbear/isaacs-hub-storage) - Backend server for Photo/App Vault and other features
- [essentials-family-app](https://github.com/misterjosephbear/essentials-family-app) - Standalone version of Essentials feature
