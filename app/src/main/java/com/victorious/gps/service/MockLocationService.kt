package com.victorious.gps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.victorious.gps.R
import com.victorious.gps.domain.Coordinates
import com.victorious.gps.domain.CoordinateResult
import com.victorious.gps.domain.CoordinateValidator
import com.victorious.gps.mock.MockLocationController
import com.victorious.gps.mock.MockStatus
import com.victorious.gps.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** Keeps the mock fix pushed (~1 Hz) while showing an ongoing notification with a Stop action. */
@AndroidEntryPoint
class MockLocationService : Service() {
    @Inject lateinit var controller: MockLocationController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown(); return START_NOT_STICKY
        }
        val r = CoordinateValidator.validate(
            intent?.getDoubleExtra(EXTRA_LAT, Double.NaN) ?: Double.NaN,
            intent?.getDoubleExtra(EXTRA_LNG, Double.NaN) ?: Double.NaN,
        )
        if (r !is CoordinateResult.Valid) {
            _status.value = MockStatus.Error("Invalid coordinates"); shutdown(); return START_NOT_STICKY
        }
        val accuracy = intent?.getFloatExtra(EXTRA_ACC, 5f) ?: 5f
        val label = intent?.getStringExtra(EXTRA_LABEL)
        try {
            startInForeground(r.coordinates, label)
        } catch (e: SecurityException) {
            // Android 14+: a location-type foreground service needs a granted location permission.
            _status.value = MockStatus.Error("Grant location permission first"); stopSelf(); return START_NOT_STICKY
        }

        loop?.cancel()
        controller.stop()
        val st = controller.start()
        _status.value = st
        if (st !is MockStatus.Running) { shutdown(); return START_NOT_STICKY }
        _current.value = r.coordinates
        loop = scope.launch {
            while (isActive) {
                controller.push(r.coordinates, accuracy, 0.0)
                delay(1_000)
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(c: Coordinates, label: String?) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MockLocationService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val text = String.format(Locale.US, "%s%.5f, %.5f", label?.let { "$it · " } ?: "", c.latitude, c.longitude)
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pin)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_stop), stop)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIF_ID, n, type)
    }

    private fun shutdown() {
        loop?.cancel(); loop = null
        controller.stop()
        _current.value = null
        if (_status.value is MockStatus.Running) _status.value = MockStatus.Idle
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        loop?.cancel()
        controller.stop()
        _current.value = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "mock_location"
        private const val NOTIF_ID = 42
        const val ACTION_STOP = "com.victorious.gps.STOP"
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"
        private const val EXTRA_ACC = "acc"
        private const val EXTRA_LABEL = "label"

        private val _status = MutableStateFlow<MockStatus>(MockStatus.Idle)
        val status: StateFlow<MockStatus> = _status
        private val _current = MutableStateFlow<Coordinates?>(null)
        val current: StateFlow<Coordinates?> = _current

        fun start(context: Context, c: Coordinates, accuracy: Float = 5f, label: String? = null) {
            val i = Intent(context, MockLocationService::class.java)
                .putExtra(EXTRA_LAT, c.latitude).putExtra(EXTRA_LNG, c.longitude)
                .putExtra(EXTRA_ACC, accuracy).putExtra(EXTRA_LABEL, label)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MockLocationService::class.java).setAction(ACTION_STOP))
        }
    }
}
