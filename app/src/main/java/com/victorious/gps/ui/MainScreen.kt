package com.victorious.gps.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.victorious.gps.data.Profile
import com.victorious.gps.domain.CoordinateResult
import com.victorious.gps.domain.CoordinateValidator
import com.victorious.gps.domain.Coordinates
import com.victorious.gps.mock.MockStatus
import com.victorious.gps.service.MockLocationService
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

private fun fmt(d: Double) = String.format(Locale.US, "%.6f", d)

@Composable
fun MainScreen(vm: MainViewModel, onShowSetup: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var latText by remember { mutableStateOf("40.748817") }
    var lngText by remember { mutableStateOf("-73.985428") }
    val snack = remember { SnackbarHostState() }
    val msg by vm.message.collectAsState()
    val status by MockLocationService.status.collectAsState()
    val current by MockLocationService.current.collectAsState()
    LaunchedEffect(msg) { msg?.let { snack.showSnackbar(it); vm.message.value = null } }
    LaunchedEffect(status) {
        when (val s = status) {
            MockStatus.NotSelected -> snack.showSnackbar("Select VictorGPS as the mock location app in Developer options")
            is MockStatus.Error -> snack.showSnackbar(s.message)
            else -> Unit
        }
    }

    fun startAt(c: Coordinates, acc: Float = 5f, label: String? = null) {
        MockLocationService.start(context, c, acc, label)
    }

    Scaffold(snackbarHost = { SnackbarHost(snack) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Map") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Profiles") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Help") })
            }
            StatusBar(current, onStop = { MockLocationService.stop(context) })
            when (tab) {
                0 -> MapTab(
                    latText, lngText,
                    onLatLng = { a, b -> latText = a; lngText = b },
                    onStart = { c -> startAt(c) },
                    onSave = { p -> vm.save(p) },
                    onError = { vm.message.value = it },
                )
                1 -> ProfilesTab(
                    vm.profiles.collectAsState().value,
                    onUse = { p ->
                        latText = fmt(p.latitude); lngText = fmt(p.longitude)
                        startAt(Coordinates(p.latitude, p.longitude), p.accuracyMeters, p.name)
                    },
                    onOpenTarget = { p -> launchTarget(context, p.targetPackage) { vm.message.value = it } },
                    onSave = { p -> vm.save(p) },
                    onDelete = { vm.delete(it) },
                )
                else -> HelpTab(onShowSetup)
            }
        }
    }
}

@Composable
private fun StatusBar(current: Coordinates?, onStop: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (current == null) "Mock location: off"
            else "Mocking ${fmt(current.latitude)}, ${fmt(current.longitude)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (current != null) TextButton(onClick = onStop) { Text("Stop") }
    }
}

@Composable
private fun MapTab(
    latText: String, lngText: String,
    onLatLng: (String, String) -> Unit,
    onStart: (Coordinates) -> Unit,
    onSave: (Profile) -> Unit,
    onError: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var saveFor by remember { mutableStateOf<Coordinates?>(null) }
    val parsed = CoordinateValidator.parse(latText, lngText)
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Search \"lat, lng\"") }, modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                when (val r = CoordinateValidator.parseQuery(query)) {
                    is CoordinateResult.Valid -> onLatLng(fmt(r.coordinates.latitude), fmt(r.coordinates.longitude))
                    is CoordinateResult.Invalid -> onError(r.reason)
                }
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Go") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            OutlinedTextField(latText, { onLatLng(it, lngText) }, Modifier.weight(1f), label = { Text("Latitude") }, singleLine = true, keyboardOptions = num)
            OutlinedTextField(lngText, { onLatLng(latText, it) }, Modifier.weight(1f), label = { Text("Longitude") }, singleLine = true, keyboardOptions = num)
        }
        if (parsed is CoordinateResult.Invalid) {
            Text(parsed.reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OsmMap(
            point = (parsed as? CoordinateResult.Valid)?.coordinates,
            onPick = { c -> onLatLng(fmt(c.latitude), fmt(c.longitude)) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            Button(enabled = parsed is CoordinateResult.Valid, onClick = {
                (parsed as? CoordinateResult.Valid)?.let { onStart(it.coordinates) }
            }) { Text("Start mocking") }
            OutlinedButton(enabled = parsed is CoordinateResult.Valid, onClick = {
                saveFor = (parsed as? CoordinateResult.Valid)?.coordinates
            }) { Text("Save profile") }
        }
    }
    saveFor?.let { c ->
        ProfileDialog(Profile(name = "", latitude = c.latitude, longitude = c.longitude), onDismiss = { saveFor = null }) {
            onSave(it); saveFor = null
        }
    }
}

@Composable
private fun OsmMap(point: Coordinates?, onPick: (Coordinates) -> Unit, modifier: Modifier) {
    val marker = remember { arrayOfNulls<Marker>(1) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        onPick(Coordinates(p.latitude, p.longitude)); return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean = false
                }))
                marker[0] = Marker(this).also { overlays.add(it) }
                point?.let { controller.setCenter(GeoPoint(it.latitude, it.longitude)) }
            }
        },
        update = { map ->
            point?.let {
                val gp = GeoPoint(it.latitude, it.longitude)
                marker[0]?.position = gp
                if (!map.boundingBox.contains(gp)) map.controller.animateTo(gp)
                map.invalidate()
            }
        },
    )
}

@Composable
private fun ProfilesTab(
    profiles: List<Profile>,
    onUse: (Profile) -> Unit,
    onOpenTarget: (Profile) -> Unit,
    onSave: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
) {
    var editing by remember { mutableStateOf<Profile?>(null) }
    if (profiles.isEmpty()) {
        Text("No profiles yet. Pick a spot on the Map tab and tap \"Save profile\".", Modifier.padding(16.dp))
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(profiles, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth().clickable { editing = p }) {
                Column(Modifier.padding(12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text("${fmt(p.latitude)}, ${fmt(p.longitude)} · ±${p.accuracyMeters.toInt()} m", style = MaterialTheme.typography.bodySmall)
                    p.targetPackage?.let { Text("Tagged for: $it (applies system-wide)", style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUse(p) }) { Text("Use") }
                        if (p.targetPackage != null) {
                            OutlinedButton(onClick = { onUse(p); onOpenTarget(p) }) { Text("Use + open app") }
                        }
                        TextButton(onClick = { onDelete(p) }) { Text("Delete") }
                    }
                }
            }
        }
    }
    editing?.let { p -> ProfileDialog(p, onDismiss = { editing = null }) { onSave(it); editing = null } }
}

@Composable
private fun ProfileDialog(initial: Profile, onDismiss: () -> Unit, onConfirm: (Profile) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var acc by remember { mutableStateOf(initial.accuracyMeters.toString()) }
    var pkg by remember { mutableStateOf(initial.targetPackage.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Save profile" else "Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${fmt(initial.latitude)}, ${fmt(initial.longitude)}")
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(acc, { acc = it }, label = { Text("Accuracy (m)") }, singleLine = true)
                OutlinedTextField(pkg, { pkg = it }, label = { Text("Target app package (optional)") }, singleLine = true)
                Text(
                    "The target app is just a tag and shortcut. Mock location applies to all apps.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(initial.copy(name = name, accuracyMeters = acc.toFloatOrNull() ?: -1f, targetPackage = pkg))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HelpTab(onShowSetup: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("VictorGPS", style = MaterialTheme.typography.titleLarge)
        Text("Lawful personal and testing use only. Don't use it to defraud apps or services.")
        Text(
            "Per-app profiles are quick-switch profiles with an optional target-app tag. Android mock " +
                "location without root is system-wide, so every app sees the active profile."
        )
        Text("Full manual: docs/MANUAL.md in the repository.")
        Button(onClick = { openDeveloperSettings(context) }) { Text("Open Developer settings") }
        OutlinedButton(onClick = onShowSetup) { Text("Show setup again") }
    }
}

private fun launchTarget(context: Context, pkg: String?, onError: (String) -> Unit) {
    if (pkg == null) return
    val i = context.packageManager.getLaunchIntentForPackage(pkg)
    if (i == null) onError("$pkg is not installed or has no launcher") else context.startActivity(i)
}
