# AB Cricket

**Established by:** Syed Abdul Basit

AB Cricket is an Android cricket scoring and statistics application.

## Features in progress
- Live match scorecards and match history
- Batting and bowling statistics
- Player profiles and career records
- Weekly, monthly, yearly, and all-time rankings
- Private Cricket Rooms with separate squads and match records
- Room owners/admins can manage scoring and room data
- Room members have read-only access to live scores, scorecards, players, and rankings

## Technology
- Android application (Kotlin + Jetpack Compose)
- Supabase Auth and PostgreSQL with Row Level Security (RLS) for private room data

## Supabase database setup
Run these SQL files in the Supabase SQL Editor, in this order:

1. `supabase/rooms_schema.sql` — profiles, rooms, membership, invite codes, and room joining.
2. `supabase/room_data_schema.sql` — room-specific squads, matches, and ball-by-ball events with read-only member / admin-write permissions.

The second file is a migration and must be run after the first file. It does not replace it.

## Important permission model
- A user can only read rooms they have joined.
- Room members can read room squads, match scorecards, and ball events.
- Only room owners/admins can create, edit, or delete room squads, matches, and scoring events.
- The app must also hide/disable editing controls for ordinary members; database RLS remains the final security boundary.

## Build
GitHub Actions builds a debug APK when changes are pushed to the `main` branch. The APK is published as the `AB-Cricket-debug-apk` workflow artifact after a successful build.
