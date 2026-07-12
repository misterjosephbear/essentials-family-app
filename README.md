# Isaac's Hub

The start of an Android "all-in-one" app. First feature: a sleep tracker that mirrors Rise Sleep
Tracker's core idea - auto-detect sleep from phone signals (no wearable), and track a rolling
**sleep debt** over time instead of just logging individual nights.

## How it works

1. **Detection** (`sleep/detection/SleepDetectionService.kt`) - a foreground service listens for
   `ACTION_SCREEN_ON`/`ACTION_SCREEN_OFF` broadcasts and the `TYPE_SIGNIFICANT_MOTION` trigger
   sensor, and feeds them into a small heuristic state machine
   (`sleepcore/.../SleepDetectionEngine.kt`): screen goes off at night, stays off and still for
   ~10 minutes -> sleep candidate becomes an actual session; screen comes back on (or motion
   resumes) and stays that way for ~3 minutes -> session ends. A brief screen check in the middle
   of the night (checking the time, then putting the phone back down) does not end the session.
   This is a **heuristic approximation**, not a reverse-engineering of Rise's actual (undisclosed)
   algorithm - it's built entirely from public phone signals (screen state + a stock Android
   sensor), documented in the code so it's easy to tune or replace.
2. **Confirmation** - every auto-detected session is inserted unconfirmed, with a notification
   ("Good morning! 7h 42m detected - tap to confirm or edit") so a bad read never silently pollutes
   your sleep debt. Manual entry is always available too (`+` button on Home/History), both as a
   fallback and because the detector needs a session to already be confirmed to count toward debt.
3. **Sleep debt** (`sleepcore/.../SleepDebt.kt`) - each night short of your sleep-need target (set
   in Settings, default 8h) adds to a running debt total; each night over it pays debt back down,
   floored at zero. Only the last N days (default 14, `debtWindowDays`) count, and only nights with
   a confirmed session count - a night with no data is skipped rather than assumed to be a total
   loss.
4. **Persistence** - Room (`sleep/data/`) for sleep sessions, Jetpack DataStore
   (`core/data/prefs/`) for settings (sleep need, auto-detect toggle, night window). Detection
   state itself (which phase the state machine is in, mid-session) is snapshotted to
   `SharedPreferences` after every event so a service restart resumes instead of losing the night.

## Module layout

- **`:sleepcore`** - pure Kotlin/JVM, no Android dependency. Contains `SleepDebtCalculator` and
  `SleepDetectionEngine`, the two pieces of actual logic in this app. Fully unit tested (16 tests,
  all passing - see "What's verified" below).
- **`:app`** - the Android app: Room/DataStore data layer, the foreground detection service,
  notifications, and a 3-tab Jetpack Compose UI (Home with a sleep-debt ring, History, Settings)
  plus a manual add/edit screen with Material3 date/time pickers.

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Foreground-service notification + "sleep detected" morning summary |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the detector running overnight |
| `RECEIVE_BOOT_COMPLETED` | Restarts detection after a reboot, if it was enabled |

No `BODY_SENSORS`, no location, no microphone. Auto-detect is opt-in (off by default, toggled in
Settings) and only requests the notification permission when you turn it on.

## What's verified vs. what isn't

This was built in a sandboxed environment with **no network access to `dl.google.com`** (Google's
Maven host), which is required to resolve the Android Gradle Plugin, AndroidX/Compose/Room, and
the Android SDK itself. So the `:app` module has **not been compiled**, run, or seen a device/
emulator - the Compose UI, Room/KSP annotation processing, and the manifest have not been checked
by a real build.

What *has* been verified: `:sleepcore` has no Android dependency, so it was tested standalone
against Maven Central (which is reachable here) - 16 JUnit tests covering the debt-calculation
math (deficit accumulation, surplus paydown floored at zero, rolling window boundaries, same-day
session summing) and the detection state machine (night-window gating, false-start cancellation,
stillness/wake confirmation timing, mid-sleep screen checks not ending a session, the runaway-
session safety cap, and snapshot/restore). Both files are dependency-free enough to read and
review directly: `sleepcore/src/main/kotlin/com/isaacshub/sleep/core/`.

Before relying on this:

1. Open the project in Android Studio (or run `./gradlew build`) and fix whatever the real
   compiler finds. The highest-risk file is `sleep/ui/edit/EditSessionScreen.kt` - it uses
   Material3's `DatePicker`/`TimePicker`, which are a bit fiddly (the `DatePicker` API works in
   UTC regardless of device timezone, which the code accounts for, but is worth double-checking
   against the real widget).
2. Foreground-service background-start rules are strict on modern Android. The service is only
   ever started from a foreground context (Settings toggle, `MainActivity` launch, or the
   boot-completed exemption) - never from `Application.onCreate()` - but this needs a real-device
   check, since `ForegroundServiceStartNotAllowedException` is easy to trigger without realizing
   it.
3. Detection reliability depends on the OS not fully suspending the CPU overnight in a way that
   starves the tick handler for too long. The design tolerates *late* ticks (every transition is
   duration-based off real timestamps, not tick-count-based), but hasn't been battery/Doze-tested
   on a real device. `SettingsScreen` offers a battery-optimization exemption shortcut for this.
   A tighter version would swap the `Handler.postDelayed` tick loop for
   `AlarmManager.setExactAndAllowWhileIdle`.
4. `TYPE_SIGNIFICANT_MOTION` isn't present on every device; the code null-checks it and degrades
   gracefully (screen-on alone still ends a session, just with a slightly less precise stillness
   read), but that fallback path is untested.

## Building

```
./gradlew build          # full build
./gradlew :sleepcore:test  # just the verified pure-logic tests
./gradlew installDebug    # install on a connected device/emulator
```

## Roadmap ideas (not built yet)

- Sleep debt trend chart / history graph
- Smart alarm (wake within a window near a light-sleep point)
- Additional "all-in-one" features beyond sleep tracking
