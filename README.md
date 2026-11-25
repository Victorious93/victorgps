<div align="center">

<img src="docs/assets/logo.svg" width="420" alt="VictorGPS logo"/>

# VictorGPS

**A transparent mock-location tool for Android app testing.**

<p>
  <a href="https://github.com/Victorious93/victorgps/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/Victorious93/victorgps/ci.yml?branch=main&style=flat-square&labelColor=0B0F1A&logo=githubactions&logoColor=00E5FF&label=CI" alt="CI"/></a>
  <img src="https://img.shields.io/badge/platform-Android-00E5FF?style=flat-square&labelColor=0B0F1A" alt="Platform"/>
  <a href="https://victorious-framework.pages.dev"><img src="https://img.shields.io/badge/Victorious-Framework-FF2BD6?style=flat-square&labelColor=0B0F1A" alt="Victorious Framework"/></a>
</p>

<p><a href="https://github.com/Victorious93/victorgps/releases">Releases</a> · <a href="https://victorious-framework.pages.dev">Victorious Framework</a></p>

</div>

---

> **Lawful personal use and app testing only.** Don't use VictorGPS to defraud, cheat, or deceive apps, services, or people.

## Features

- Pick a spot on an **osmdroid** map (OpenStreetMap tiles, no API key), type latitude/longitude, or search with `lat, lng`.
- Push it as the device location through Android's **mock location provider**: `LocationManager` test providers for `gps` and `network`, plus `FusedLocationProviderClient.setMockMode` when Google Play services is available.
- A **foreground service** re-pushes the fix about once a second, with an ongoing notification and a **Stop** action.
- **Saved profiles** in Room (name, coordinates, accuracy, optional target-app tag) with one-tap quick switching.
- First-launch **disclaimer** and an **onboarding** screen with an "Open Developer settings" button.
- In-app help and a [how-to manual](docs/MANUAL.md).
- Planned: address / place geocoding (search accepts coordinates only today).

Built with Kotlin, Jetpack Compose, Hilt and Room. Package id: `com.victorious.gps`, minSdk 26.

## Install

No release has been published yet. Debug APKs come from CI: open the latest [CI run](https://github.com/Victorious93/victorgps/actions/workflows/ci.yml) and download the APK artifact.

## Setup and usage

1. Enable **Developer options** on the phone.
2. Choose VictorGPS under **Select mock location app**. The `ACCESS_MOCK_LOCATION` permission alone is not enough and cannot be granted at runtime.
3. Pick a location on the map (or enter coordinates) and start mocking. Use **Stop** in the notification to end it.

## Honest limitations

- **Mock location is system-wide.** Without root, Android can't fake the location for a single app. The "per-app profiles" are quick-switch profiles with an optional target-app tag and a "Use + open app" shortcut. Every app on the device, including maps and emergency features, sees the active mock.
- Apps can detect mock locations (`Location.isMock()`, the mock-app setting, Play Integrity) and may refuse to work or flag accounts. VictorGPS makes no attempt to hide itself.
- Android 14+ needs location permission granted before a location-type foreground service can start.

## Permissions

`ACCESS_MOCK_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`, `INTERNET` (map tiles), `ACCESS_NETWORK_STATE`. No root required.

## Build from source

JDK 17, Android SDK 34, Gradle 8.7 (wrapper).

```bash
git clone https://github.com/Victorious93/victorgps.git
cd victorgps
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is signed with the committed debug-only keystore `keystore/victorgps-debug.jks` (see [keystore/README.md](keystore/README.md)). Release keys are never committed.

## Layout

| Path | What |
|---|---|
| `app/src/main/java/com/victorious/gps/domain` | Coordinate validation and parsing |
| `.../data` | Room entity/DAO/database, `ProfileRepository`, prefs |
| `.../mock` | `MockLocationController` (test providers + fused mock mode) |
| `.../service` | `MockLocationService` foreground service |
| `.../ui` | Compose screens: disclaimer, onboarding, map, profiles, help |
| `app/src/test` | Unit tests for coordinate validation and profile storage |

---

## Victorious fleet

VictorGPS is part of the [Victorious Framework](https://victorious-framework.pages.dev), a family of Android and PC tools by Don Victor. **Every app works standalone; fleet integrations are optional** and never required.

Related: [VictorLaunch](https://github.com/Victorious93/victorlaunch) (fleet launcher and theme provider), [droidcommand-AI](https://github.com/Victorious93/droidcommand-AI) (fleet controller), [fleet-manifest](https://github.com/Victorious93/fleet-manifest) (fleet status feed). The full roster lives in [victorious-fleet](https://github.com/Victorious93/victorious-fleet), and live status is in [fleet-manifest](https://github.com/Victorious93/fleet-manifest).

## Contributing

Issues and pull requests are welcome. Keep changes focused, match the existing code style, never commit secrets or release keys, and keep any fleet integration optional so the app still works on its own. Open a PR against the default branch and wait for CI to pass.

## License

This repository does not include a LICENSE file yet, so no license is granted beyond what the author states elsewhere. Please open an issue before reusing the code.
