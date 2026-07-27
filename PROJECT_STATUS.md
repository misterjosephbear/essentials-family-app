# isaacs-hub — Project Status

Android app (`compileSdk`/`targetSdk` 36, `minSdk` 26, Kotlin/Compose, Java 17). Bundles several
mostly-independent tools: sleep tracking, Route Helper (mail-route building/driving), Time Tracking,
Photo/App Vault, Banking, Feature Funnel, Activity Mapper, and an in-app auto-updater.

Builds/tests are driven headlessly on brownserver2 (JDK 21, Android SDK command-line tools,
Gradle via the wrapper) via a Discord bridge — see `isaacs-hub-storage/discord-bridge/README.md` and
its own `PROJECT_STATUS.md` for how that's wired up.

**Everything below is on `main`.** The mail/envelope-scanning and route-player work described here was
built on branch `route-player-driving-gps` and merged into `main` via PR #5 on 2026-07-15.

## Recent Changes (2026-07-27)

### Activity Mapper (New Feature)
Automation system for triggering actions based on configurable conditions.

- **Data Layer** (`activitymapper/data/`):
  - `ActivityMapperModels.kt` - Data classes for Variables, Conditions, Actions, Rules, and Discord Rich Presence Profiles
  - `ActivityMapperApiClient.kt` - API client extending BaseApiClient for server communication
  - `ActivityMapperRepository.kt` - Repository pattern with StateFlow for reactive UI updates
- **UI Layer** (`activitymapper/ui/`):
  - `ActivityMapperHomeScreen.kt` - Main screen displaying variables, rules, and profiles (with empty states)
  - `EditRuleScreen.kt` - Create/edit automation rules with condition and action builders
  - `EditRichPresenceProfileScreen.kt` - Manage Discord Rich Presence profiles
  - `ActivityMapperViewModel.kt` - ViewModel with Factory pattern
- **Features**:
  - Variable system tracking state (e.g., IS_SLEEPING, CURRENT_ACTIVITY)
  - Automation rules with AND/OR logical operators
  - Condition comparisons (EQUALS, GREATER_THAN, LESS_THAN, etc.)
  - Actions: Discord Rich Presence integration (extensible for future action types)
  - Full CRUD operations via Repository pattern
- **Integration**:
  - Added routes to `navigation/Routes.kt` with helper functions
  - Integrated into `AppNavHost.kt` navigation graph
  - Added landing card with AutoAwesome icon in `LandingScreen.kt`
- **Server Integration**: Communicates with `/api/activity-mapper` endpoints on isaacs-hub-storage server
- **Commits**: `4051f3b` (main implementation), `82e829c` (lint fix for SimpleDateFormat)

## Route Helper

Lets you build a mail-delivery route by driving it once, then replay it turn-by-turn on later days.

- **Builder** (`routehelper/ui/builder/RouteBuilderScreen.kt`): live-records a route while driving it,
  using Census TIGER/Line house-number ranges (replacing an earlier OSM-based source) filtered against
  Microsoft building footprints to drop phantom addresses (`routehelper/domain/` - see commits
  `1261d48`, `3f38dc7`, `68fee72`, `4ba4c22`).
- **Edit screen** (`routehelper/ui/edit/RouteEditScreen.kt`): reorder or remove stops after the fact
  (commit `6093a9d`). Stop taps made while actively driving are queued and planted at the next full
  stop rather than applied mid-drive (commit `5dd7395`).
- **Route Player** (`routehelper/ui/player/RoutePlayerScreen.kt`): the live "GPS" screen for a route
  already built. Rotates the map so direction of travel always faces up, draws the recorded route as a
  polyline, and shows which stop is next. Reached from Route Helper's home screen via the "Play"
  (compass) icon on each route row (`RouteHelperHomeScreen.kt` → `Routes.routePlayer(routeId)` →
  `AppNavHost.kt`).
  - `RoutePlayerViewModel.kt` combines live location (`LocationTracker`), the stop list, and a driving
    route fetched from `RouteDirectionsFetcher.kt` (OSRM-style driving directions).
  - The "Package info" overlay mentioned in the screen's own doc comment is a placeholder - that
    feature doesn't exist yet.

## Mail/Envelope Scanning

Lets the driver, while inside Route Player, point the camera at a mail piece and have it OCR'd into a
new stop.

- Entry point: the camera FAB on `RoutePlayerScreen.kt` ("Scan a mail piece to add a stop") opens
  `MailScanScreen.kt` as an overlay.
- `MailScanScreen.kt` does the CameraX + ML Kit text-recognition plumbing (frame analysis only - no UI
  logic beyond that). Receives the driver's current GPS location as a fallback.
- `MailScanViewModel.kt` owns OCR-to-stop resolution: feeds recognized text into
  `routehelper/domain/MailScanParser.kt`'s `parseScannedAddresses()`, which regex-matches
  name/street/city-state-zip line blocks (a mail piece can have more than one address-shaped block -
  return address, delivery address, forwarding label - so it returns all candidates and lets the
  screen show a chooser when there's more than one). The chosen address is geocoded via
  `routehelper/network/NominatimGeocoder.kt`. **Fallback behavior**: If geocoding fails (address not
  in Nominatim's database), it uses the driver's current GPS location instead, so missing addresses
  can still be added at the correct physical location.
- Once geocoded, `RoutePlayerViewModel.addScannedStop()` plants the new stop right after wherever the
  driver currently is (before the next stop they're heading to, or at the end if every stop's already
  been hit) via `RouteHelperRepository.insertStopBefore()`.
- Tests: `routehelper/domain/MailScanParserTest.kt`, `routehelper/domain/RoutePlaybackTest.kt` - both
  passing as of 2026-07-15.
- Dependency: `mlkit-text-recognition` (`gradle/libs.versions.toml` / `app/build.gradle.kts`,
  alongside the pre-existing `mlkit-barcode-scanning`).

## Also merged as part of PR #5 (unrelated to route player - don't confuse with it)

The `route-player-driving-gps` branch was built as one continuous line of work, so it also brought in:

- **Nap alarm** (`sleep/nap/`: `NapAlarmController/Receiver/Scheduler/Service/Notifications/State.kt`,
  `sleep/ui/nap/NapScreen.kt` + `NapViewModel.kt`): a foreground-service alarm for timed naps, reached
  from the sleep Home screen.
- **Wind-down calculator** (`sleepcore` module, `WindDown.kt` + test): recommends a wind-down time
  driven off a per-day wake-time setting rather than sleep history.
- Vault client-side changes (resumable uploads / remote-tunnel fallback URL) - the
  `isaacs-hub-storage` server side of this already merged separately; check that repo's
  `PROJECT_STATUS.md` if the two sides ever seem out of sync.

## Photo/App Vault

QR-pairs with an `isaacs-hub-storage` instance and auto-backs-up photos, videos (with GPS metadata
kept, not stripped), Room databases, and preferences. Supports letting Photo Vault upload an arbitrary
file, not just already-synced photos. Live progress indicators for Sync now/Back up now.

## Auto-Updater

`update/UpdateInstaller.kt` downloads the latest signed APK from a GitHub Release (published
automatically by `.github/workflows/release.yml` on every push to `main`) and launches Android's
package-install intent. **This always needs one manual tap to actually install** - Android won't allow
a silent/automated install even from a trusted source.

Update checks are authenticated against the private `isaacs-hub` repo using `UPDATE_CHECK_TOKEN` (a
fine-grained GitHub PAT with Contents read-only access, baked in by CI via BuildConfig).

**Known Issue (2026-07-27)**: The `UPDATE_CHECK_TOKEN` GitHub secret has expired or has incorrect
permissions, causing "GitHub API returned HTTP 401: Bad credentials" errors. To fix:
1. Generate a new fine-grained PAT with:
   - Repository access: Only `misterjosephbear/isaacs-hub`
   - Permissions: Contents (read-only)
2. Update the `UPDATE_CHECK_TOKEN` secret in repository settings → Secrets and variables → Actions

Local/debug builds have no token and simply never find an update. Failures are surfaced (not swallowed)
so a broken token/scope shows up as a banner instead of the update silently never appearing.

## Feature Funnel

Queue and manage feature requests for automated development with Discord integration.

- Discord channel ID configuration
- Refresh functionality for syncing with Discord
- Full CRUD interface for feature prompts

## Time Tracking

Log work hours by route with evaluations and overtime tracking.

- Weekly schedule view with past/future week pagination
- Per-day schedulable routes with notes field
- Overtime calculations
- Route selection and time entry management

## Sleep Health

Auto-detect sleep sessions from phone signals (no wearable) and track rolling sleep debt.

- **Detection** (`sleep/detection/SleepDetectionService.kt`) - Foreground service with screen/motion heuristics
- **Sleep debt** (`sleepcore/SleepDebt.kt`) - Rolling window deficit/surplus calculation (14-day default)
- **Nap alarm** (`sleep/nap/`) - Foreground-service timer for timed naps
- **Wind-down calculator** (`sleepcore/WindDown.kt`) - Recommends wind-down time based on wake time
- **Confirmation workflow** - All auto-detected sessions require user confirmation
- **Manual entry** - Add/edit sessions via Material3 date/time pickers

## Banking

View all account balances in one place using Plaid integration.

## Essentials

Manage family chores and device restrictions. **Note**: Now available as a standalone app at
[essentials-family-app](https://github.com/misterjosephbear/essentials-family-app/releases).

The Essentials module was separated from this repository on 2026-07-20 (commit `fd9f19c`) but remains
accessible within Isaac's Hub for existing users.

## Module Structure

- **`:app`** - Main Android app containing all features
- **`:sleepcore`** - Pure Kotlin/JVM module for sleep debt calculation and detection logic (16 passing unit tests)
- **`:essentialscore`** - Shared logic for Essentials features

## Architecture Patterns

- **BaseApiClient** - Base class for API communication with vault connection handling
- **Repository Pattern** - StateFlow-based repositories for reactive data layer
- **ViewModelProvider.Factory** - Dependency injection pattern for ViewModels
- **Jetpack Compose** - Declarative UI with Material 3 components
- **Room + DataStore** - Local persistence (databases + preferences)
- **Coroutines + Flow** - Asynchronous programming and reactive streams

## Testing

- `:sleepcore` module: 16 JUnit tests covering debt calculation and detection state machine
- `routehelper/domain/MailScanParserTest.kt` - Mail OCR parsing tests
- `routehelper/domain/RoutePlaybackTest.kt` - Route playback logic tests
- All tests passing as of latest build

## CI/CD

`.github/workflows/release.yml` automatically builds and publishes signed APKs on every push to `main`:
- Uses GitHub Actions with Android SDK
- Generates version code from `github.run_number`
- Creates GitHub Release with tag `v<versionCode>`
- Attaches signed APK as release asset

## Recent Commits

- `82e829c` - Fix lint warning for SimpleDateFormat in Composable (Activity Mapper)
- `4051f3b` - Add Activity Mapper feature with full CRUD UI and server integration
- `ebcd284` - Fix: Re-add UPDATE_CHECK_TOKEN for Isaac's Hub update checker
- `fd9f19c` - Separate Essentials Family App into its own repository
