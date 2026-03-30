package com.timer99.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.timer99.app.MainActivity
import com.timer99.app.R
import com.timer99.app.TimerFinishedActivity
import com.timer99.app.data.WidgetKeys
import com.timer99.app.data.widgetDataStore
import com.timer99.app.model.TimerState
import com.timer99.app.model.formatMillis
import com.timer99.app.widget.TimerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null

    private val _timerState = MutableStateFlow(TimerState.initial())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var currentPresetName: String? = null

    private val binder = TimerBinder()

    // ---------------------------------------------------------------------------
    // Notification + widget refresh loop
    // > 1 min remaining → every 30 s   |   ≤ 1 min remaining → every 1 s
    // ---------------------------------------------------------------------------
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!_timerState.value.isRunning) return
            nm().notify(NOTIFICATION_ID, buildRunningNotification())
            pushWidgetState()
            val nextMs = if (_timerState.value.remainingMillis > 60_000L) 30_000L else 1_000L
            refreshHandler.postDelayed(this, nextMs)
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createCountdownChannel()
        createAlertChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP        -> pauseTimer()
            ACTION_ADD_MINUTE  -> addMinute()
            ACTION_DISMISS_ALERT -> dismissAlert()
            ACTION_EXTEND_1MIN -> extendAndRestart(60_000L)
            ACTION_EXTEND_5MIN -> extendAndRestart(300_000L)
            else -> {
                startForeground(NOTIFICATION_ID, buildRunningNotification())
                if (!_timerState.value.isRunning) stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable)
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    fun setDuration(totalMillis: Long) {
        if (_timerState.value.isRunning) return
        currentPresetName = null
        _timerState.value = TimerState.initial(totalMillis)
    }

    fun setDurationWithPreset(totalMillis: Long, presetName: String) {
        if (_timerState.value.isRunning) return
        currentPresetName = presetName
        _timerState.value = TimerState.initial(totalMillis)
    }

    fun startTimer() {
        val s = _timerState.value
        if (s.isRunning || s.remainingMillis <= 0L) return
        tickJob?.cancel()
        tickJob = scope.launch {
            _timerState.update { it.copy(isRunning = true, isFinished = false) }
            startForeground(NOTIFICATION_ID, buildRunningNotification())
            startRefreshLoop()
            while (isActive && _timerState.value.remainingMillis > 0) {
                delay(TICK_MS)
                _timerState.update { st ->
                    val next = (st.remainingMillis - TICK_MS).coerceAtLeast(0L)
                    st.copy(remainingMillis = next)
                }
            }
            tickJob = null
            if (_timerState.value.remainingMillis == 0L) {
                _timerState.update { it.copy(isRunning = false, isFinished = true) }
                onTimerFinished()
            } else {
                _timerState.update { it.copy(isRunning = false) }
            }
        }
    }

    fun pauseTimer() {
        tickJob?.cancel()
        tickJob = null
        _timerState.update { it.copy(isRunning = false, isFinished = false) }
        stopRefreshLoop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        pushWidgetState()
    }

    fun resetTimer() {
        tickJob?.cancel()
        tickJob = null
        stopRefreshLoop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        nm().cancel(ALERT_NOTIFICATION_ID)
        val total = _timerState.value.totalMillis
        _timerState.value = TimerState.initial(total)
        pushWidgetState()
    }

    fun addMinute() {
        if (!_timerState.value.isRunning) return
        _timerState.update { st ->
            val newRemaining = st.remainingMillis + 60_000L
            st.copy(
                remainingMillis = newRemaining,
                totalMillis = maxOf(st.totalMillis, newRemaining),
            )
        }
        nm().notify(NOTIFICATION_ID, buildRunningNotification())
        pushWidgetState()
    }

    /** Dismiss — activity already stopped sound/vibration; just remove the notification. */
    fun dismissAlert() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /** +1 min / +5 min — activity already stopped sound/vibration; add time and restart. */
    fun extendAndRestart(extraMillis: Long) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        _timerState.update { st ->
            val newRemaining = st.remainingMillis + extraMillis
            st.copy(
                remainingMillis = newRemaining,
                totalMillis = maxOf(st.totalMillis, newRemaining),
                isFinished = false,
                isRunning = false,
            )
        }
        startTimer()
    }

    // ---------------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------------

    private fun startRefreshLoop() {
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.post(refreshRunnable)
    }

    private fun stopRefreshLoop() {
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    /**
     * Called the instant remainingMillis reaches zero.
     * Sound and vibration start here — before the activity is launched — so
     * the alarm is audible even if the full-screen intent is briefly delayed.
     */
    private fun onTimerFinished() {
        stopRefreshLoop()
        pushWidgetState()

        val presetName = currentPresetName

        // Build the full-screen pending intent first so the notification is ready.
        val fullScreenPi = PendingIntent.getActivity(
            this,
            REQUEST_FINISHED,
            Intent(this, TimerFinishedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (presetName != null) putExtra(TimerFinishedActivity.EXTRA_PRESET_NAME, presetName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissPi = PendingIntent.getService(
            this, REQUEST_ALERT_DISMISS,
            Intent(this, TimerService::class.java).apply { action = ACTION_DISMISS_ALERT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val extend1Pi = PendingIntent.getService(
            this, REQUEST_ALERT_EXTEND1,
            Intent(this, TimerService::class.java).apply { action = ACTION_EXTEND_1MIN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val extend5Pi = PendingIntent.getService(
            this, REQUEST_ALERT_EXTEND5,
            Intent(this, TimerService::class.java).apply { action = ACTION_EXTEND_5MIN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAppPi = PendingIntent.getActivity(
            this, REQUEST_ALERT_OPEN,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(presetName ?: getString(R.string.app_name))
            .setContentText(getString(R.string.timer_finished))
            .setFullScreenIntent(fullScreenPi, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "Stop",     dismissPi)
            .addAction(0, "+1 min",   extend1Pi)
            .addAction(0, "+5 min",   extend5Pi)
            .addAction(0, "Open app", openAppPi)
            .build()

        // Keep service in foreground — required for setFullScreenIntent to fire automatically.
        startForeground(ALERT_NOTIFICATION_ID, alertNotification)
        nm().cancel(NOTIFICATION_ID)

        // Directly launch the activity — reliable when screen is on and app is backgrounded.
        startActivity(
            Intent(this, TimerFinishedActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                if (presetName != null) putExtra(TimerFinishedActivity.EXTRA_PRESET_NAME, presetName)
            },
        )
    }

    private fun pushWidgetState() {
        val state = _timerState.value
        val presetName = currentPresetName ?: ""
        scope.launch {
            applicationContext.widgetDataStore.edit { prefs ->
                prefs[WidgetKeys.IS_RUNNING] = state.isRunning
                prefs[WidgetKeys.REMAINING_MILLIS] = state.remainingMillis
                prefs[WidgetKeys.PRESET_NAME] = presetName
            }
            GlanceAppWidgetManager(applicationContext)
                .getGlanceIds(TimerWidget::class.java)
                .forEach { id -> TimerWidget().update(applicationContext, id) }
        }
    }

    private fun buildRunningNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this, REQUEST_OPEN,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPi = PendingIntent.getService(
            this, REQUEST_STOP,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val addMinutePi = PendingIntent.getService(
            this, REQUEST_ADD_MINUTE,
            Intent(this, TimerService::class.java).apply { action = ACTION_ADD_MINUTE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val state = _timerState.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(currentPresetName ?: getString(R.string.notification_title))
            .setContentText(formatMillis(state.remainingMillis))
            .setContentIntent(openPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.action_stop), stopPi)
            .addAction(0, getString(R.string.action_add_minute), addMinutePi)
            .build()
    }

    private fun createCountdownChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm().createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notification_channel_description) },
        )
    }

    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm().createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.notification_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_alert_channel_description)
                // Sound handled by MediaPlayer; vibration handled by Vibrator.
                // Disabling both here prevents the channel from double-triggering them.
                setSound(null, AudioAttributes.Builder().build())
                enableVibration(false)
            },
        )
    }

    private fun nm() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ALERT_CHANNEL_ID    = "timer99_alert"
        const val ALERT_NOTIFICATION_ID = 100

        private const val CHANNEL_ID      = "timer99_timer"
        private const val NOTIFICATION_ID = 99

        const val ACTION_STOP          = "com.timer99.app.ACTION_STOP"
        const val ACTION_ADD_MINUTE    = "com.timer99.app.ACTION_ADD_MINUTE"
        const val ACTION_DISMISS_ALERT = "com.timer99.app.ACTION_DISMISS_ALERT"
        const val ACTION_EXTEND_1MIN   = "com.timer99.app.ACTION_EXTEND_1MIN"
        const val ACTION_EXTEND_5MIN   = "com.timer99.app.ACTION_EXTEND_5MIN"

        private const val REQUEST_OPEN         = 0
        private const val REQUEST_STOP         = 1
        private const val REQUEST_ADD_MINUTE   = 2
        private const val REQUEST_FINISHED     = 3
        private const val REQUEST_ALERT_DISMISS = 4
        private const val REQUEST_ALERT_EXTEND1 = 5
        private const val REQUEST_ALERT_EXTEND5 = 6
        private const val REQUEST_ALERT_OPEN    = 7

        private const val TICK_MS = 250L
    }
}
