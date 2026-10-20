package com.victorious.gps.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.victorious.gps.mock.MockLocationController

@Composable
fun DisclaimerScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Before you start", style = MaterialTheme.typography.headlineSmall)
            Text(
                "VictorGPS replaces your device's reported location with one you choose. " +
                    "It is for lawful personal use and app/software testing only."
            )
            Text("You agree that you will NOT use VictorGPS to:", style = MaterialTheme.typography.titleSmall)
            Text(
                "• defraud, cheat, or deceive any app, game, service, employer, or person\n" +
                    "• evade geo-restrictions, licensing, or terms of service you are bound by\n" +
                    "• falsify attendance, delivery, ride-share, check-in, or location-based payments\n" +
                    "• interfere with emergency services, navigation, or anyone's safety\n" +
                    "• break any law where you live"
            )
            Text(
                "Mock location is system-wide: while it runs, every app (including maps and emergency " +
                    "features) sees the fake location. Stop it when you're done. Many apps detect mock " +
                    "locations and may suspend accounts that use them. You use it at your own risk."
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                Text("I understand and will use VictorGPS lawfully")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDecline) { Text("Exit") }
                Button(onClick = onAccept, enabled = agreed) { Text("Accept") }
            }
        }
    }
}

fun openDeveloperSettings(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (i in intents) {
        try {
            context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
        } catch (_: ActivityNotFoundException) { }
    }
}

@Composable
fun OnboardingScreen(controller: MockLocationController, onDone: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(controller.isSelectedMockApp()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) selected = controller.isSelectedMockApp() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val perms = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set up mock location", style = MaterialTheme.typography.headlineSmall)
            Text("Android only lets one app provide fake locations, and you must pick it yourself:")
            Text(
                "1. Enable Developer options: Settings > About phone > tap \"Build number\" 7 times.\n" +
                    "2. Open Developer options > \"Select mock location app\" and choose VictorGPS.\n" +
                    "3. Come back here. VictorGPS also needs location (required by Android 14 for its " +
                    "foreground service) and notification permission for the ongoing \"Stop\" notification."
            )
            Text(
                "No root is used. Because Android's mock location is system-wide, every app sees the same " +
                    "fake location. VictorGPS cannot spoof just one app.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = { openDeveloperSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Developer settings")
            }
            OutlinedButton(onClick = { launcher.launch(perms) }, modifier = Modifier.fillMaxWidth()) {
                Text("Grant location & notification permission")
            }
            Text(
                if (selected) "✓ VictorGPS is the selected mock location app" else "✗ VictorGPS is not selected yet",
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected) "Continue" else "Continue anyway")
            }
        }
    }
}
