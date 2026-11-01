# AB Cricket

**Established by:** Syed Abdul Basit

AB Cricket is a dark-themed Android cricket scoring app built with Kotlin and Jetpack Compose.

## Current app features

- Local player squad management with player roles
- Match setup with team names, toss choice, and overs limit
- Basic live scoring for runs, wickets, dot balls, wides, no-balls, byes, and leg-byes
- First-innings target and second-innings chase
- Device-local match history
- Player career summaries for recorded batting and bowling figures
- Automatic leaderboard based on the app's displayed points formula
- Dedicated player statistics screen
- Dark green and black interface

## Data storage and privacy

The current app stores its squad, match progress, match history, and career figures locally on the Android device using SharedPreferences. There is no active sign-in, room joining, or cloud-sync flow in the current app.

Uninstalling the app or clearing its storage can remove locally saved records. Keep that in mind before resetting or changing devices.

## Statistics and scoring limitations

This is a basic scorer, not yet a complete professional ball-by-ball scorebook. Current career summaries only include the batting and bowling figures captured by the current match screen. The app does not yet reliably record a complete playing XI scorecard, fall of wickets, catches, run-outs, fours/sixes breakdown, player dismissals, or official batting averages. The displayed runs-per-appearance figure is not an official batting average, and leaderboard points are a simple in-app formula rather than an official cricket rating.

Please verify score entries while using the scorer. Do not treat unrecorded figures as zero-performance data.

## Build

GitHub Actions builds a debug APK when changes are pushed to the `main` branch. After a successful run, download the `AB-Cricket-debug-apk` artifact from the run's **Artifacts** section, extract the ZIP, and install the APK on an Android device.

## Technology

- Kotlin
- Android Jetpack Compose
- Android SharedPreferences for local persistence

The `supabase/` directory contains legacy database scripts and is not required for the current local-only app.
