# VictorGPS

A GPS location spoofer for Android, for **lawful personal use and app testing only**.
Don't use it to defraud, cheat, or deceive apps, services, or people.
Package: `com.victorious.gps`. Kotlin, Jetpack Compose, Hilt, Room.

- How-to manual: [docs/MANUAL.md](docs/MANUAL.md)

## What it does

- Pick a spot on an **osmdroid** map (OpenStreetMap tiles, no API key), type lat/lng, or search `lat, lng`.
- Push it as the device location with Android's **mock location provider**:
  `LocationManager` test providers for `gps` and `network`, plus `FusedLocationProviderClient.setMockMode`
  when Google Play services is available.
- A **foreground service** re-pushes the fix about once a second, with an ongoing notification and a **Stop** action.
- **Saved profiles** in Room (name, coordinates, accuracy, optional target-app tag), with one-tap quick switching.
- A first-launch **disclaimer** and an **onboarding** screen with an "Open Developer settings" button.

## Requirements and honest limitations

- You must enable Developer options and choose VictorGPS under **Select mock location app**.
  `ACCESS_MOCK_LOCATION` alone is not enough, and it can't be granted at runtime.
- **Mock location is system-wide.** Without root, Android can't fake the location for just one app.
  The "per-app profiles" here are quick-switch profiles with an *optional target-app tag* and a
  "Use + open app" shortcut. Every app on the device, including maps and emergency features, sees the active mock.
- Apps can detect mock locations (`Location.isMock()`, the mock-app setting, Play Integrity) and may refuse
  to work or flag accounts. VictorGPS makes no attempt to hide itself.
- Android 14+ needs location permission granted before a location-type foreground service can start.
- No address/place geocoding yet. Search accepts coordinates only.

## Build

JDK 17, Android SDK 34, Gradle 8.7 (wrapper).

```bash
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is signed with the committed debug-only keystore `keystore/victorgps-debug.jks`
(see `keystore/README.md`). Release keys are never committed. CI is `.github/workflows/ci.yml`.

## Layout

| Path | What |
|---|---|
| `app/src/main/java/com/victorious/gps/domain` | Coordinate validation and parsing |
| `.../data` | Room entity/DAO/database, `ProfileRepository`, prefs |
| `.../mock` | `MockLocationController` (test providers + fused mock mode) |
| `.../service` | `MockLocationService` foreground service |
| `.../ui` | Compose screens: disclaimer, onboarding, map, profiles, help |
| `app/src/test` | Unit tests for coordinate validation and profile storage |
