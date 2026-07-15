# isaacs-hub — Project Status

Android app (`compileSdk`/`targetSdk` 36, `minSdk` 26, Kotlin/Compose, Java 17). Bundles several
mostly-independent tools: sleep tracking, Route Helper (mail-route building/driving), a Photo/App
Vault that backs up to `isaacs-hub-storage`, and an in-app auto-updater.

Builds/tests are driven headlessly on brownserver2 too (JDK 21, Android SDK command-line tools,
Gradle via the wrapper) via a Discord bridge — see `isaacs-hub-storage/discord-bridge/README.md` and
its own `PROJECT_STATUS.md` for how that's wired up.

**Everything below is on `main`.** The mail/envelope-scanning and route-player work described here was
built on branch `route-player-driving-gps` and merged into `main` via PR #5 on 2026-07-15. If a
session's checkout still can't find these files, it just needs a `git pull` (e.g. the discord-bridge's
clone on brownserver2, at `/home/bear/projects/isaacs-hub`) - there's no branch to hunt for anymore.

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

## Mail/envelope scanning

Lets the driver, while inside Route Player, point the camera at a mail piece and have it OCR'd into a
new stop.

- Entry point: the camera FAB on `RoutePlayerScreen.kt` ("Scan a mail piece to add a stop") opens
  `MailScanScreen.kt` as an overlay.
- `MailScanScreen.kt` does the CameraX + ML Kit text-recognition plumbing (frame analysis only - no UI
  logic beyond that).
- `MailScanViewModel.kt` owns OCR-to-stop resolution: feeds recognized text into
  `routehelper/domain/MailScanParser.kt`'s `parseScannedAddresses()`, which regex-matches
  name/street/city-state-zip line blocks (a mail piece can have more than one address-shaped block -
  return address, delivery address, forwarding label - so it returns all candidates and lets the
  screen show a chooser when there's more than one). The chosen address is geocoded via
  `routehelper/network/NominatimGeocoder.kt`.
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

## Auto-updater

`update/UpdateInstaller.kt` downloads the latest signed APK from a GitHub Release (published
automatically by `.github/workflows/release.yml` on every push to `main` - including the PR #5 merge
above, so a new release has already gone out) and launches Android's package-install intent. **This
always needs one manual tap to actually install** - Android won't allow a silent/automated install even
from a trusted source. Update checks are authenticated against the private `isaacs-hub` repo
(`UPDATE_CHECK_TOKEN`).

## Other things on `main`

Time Tracking tool, sleep energy forecast, landing page, per-day-schedulable routes with a Notes
field, week-schedule screen that can page through past/future weeks.
