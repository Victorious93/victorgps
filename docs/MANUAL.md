# VictorGPS manual

> Lawful personal and testing use only. Don't use VictorGPS to defraud, cheat, or deceive any app,
> service, employer, or person, to falsify check-ins, deliveries, attendance, or location-based payments,
> or to interfere with emergency services.

## 1. First launch

1. **Disclaimer.** Read it, tick the box, and tap **Accept**. **Exit** closes the app. It's shown once.
2. **Setup.**
   - Enable Developer options: *Settings > About phone >* tap **Build number** 7 times.
   - Tap **Open Developer settings**, find **Select mock location app**, and choose **VictorGPS**.
   - Tap **Grant location & notification permission**. Location is required on Android 14+ to run the
     location foreground service. The notification permission shows the ongoing "Stop" notification.
   - Return to the app. The status line turns to "✓ VictorGPS is the selected mock location app". Tap **Continue**.

You can reopen setup from **Help > Show setup again**.

## 2. Setting a location

On the **Map** tab:
- **Tap the map** to drop the pin, or
- type **Latitude** / **Longitude** (−90…90, −180…180; a comma decimal like `48,85` is accepted), or
- type `lat, lng` into **Search** (for example `40.7128, -74.0060`, `40.7128 -74.0060` or `(40.7128,-74.0060)`) and tap **Go**.

Tap **Start mocking**. A notification "VictorGPS mock location active" appears and the fix is re-sent
about once a second to the `gps` and `network` test providers, and to the fused provider if Google Play services is present.

**Stop** from the notification action or the **Stop** button at the top of the app. Stopping removes the
test providers and turns fused mock mode off, so real location comes back.

## 3. Profiles

- **Save profile** on the Map tab stores the current point with a name, accuracy (meters), and an optional
  **target app package** (for example `com.example.app`). Names are unique, case-insensitive.
- On the **Profiles** tab, **Use** starts mocking that profile. **Use + open app** also launches the tagged app.
  Tap a card to edit it. **Delete** removes it.

### About "per-app" profiles

Android's mock location without root is **system-wide**: there is exactly one mock location app, and every
app sees its location. The target-app tag is only a label and launch shortcut. It does **not** limit the fake
location to that app. While mocking, *all* apps, including navigation and emergency features, see the fake location.

## 4. Troubleshooting

| Symptom | Fix |
|---|---|
| "Select VictorGPS as the mock location app" | Developer options > Select mock location app > VictorGPS |
| "Grant location permission first" | Grant location in app settings (Android 14+ requirement) |
| Some apps still show the real location | They may read the fused provider without Play services mock mode, cache old fixes, or reject mock locations. Try toggling Wi-Fi/location or restarting the app. |
| Map tiles blank | Needs internet for OpenStreetMap tiles. Coordinates can still be typed. |
| An app refuses to work | Many apps detect mock locations on purpose. That's expected. Don't try to circumvent it. |

## 5. Privacy

Profiles stay in a local Room database and are excluded from backup. The only network traffic is
map tile downloads from OpenStreetMap's tile servers.
