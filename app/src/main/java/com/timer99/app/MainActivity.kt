package com.timer99.app

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.timer99.app.data.AppDatabase
import com.timer99.app.data.DefaultPresetRepository
import com.timer99.app.data.WidgetKeys
import com.timer99.app.data.encodePresets
import com.timer99.app.data.widgetDataStore
import com.timer99.app.service.TimerService
import com.timer99.app.ui.MainScreen
import com.timer99.app.ui.theme.Timer99Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TimerViewModel

    private var bound: Boolean = false
    private var pendingStartAfterBind: Boolean = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional: handle denial */ }

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            lifecycleScope.launch {
                applicationContext.widgetDataStore.edit { prefs ->
                    prefs[WidgetKeys.ALARM_SOUND_URI] = uri?.toString() ?: ""
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? TimerService.TimerBinder ?: return
            val service = b.getService()
            bound = true
            viewModel.attachService(service)
            if (pendingStartAfterBind) {
                pendingStartAfterBind = false
                service.startTimer()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = DefaultPresetRepository(AppDatabase.getInstance(this).presetDao())
        viewModel = ViewModelProvider(this, TimerViewModel.Factory(repository))[TimerViewModel::class.java]

        // Keep the widget's preset list in sync with Room — stops when the activity is not STARTED.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.presets.collect { presets ->
                    applicationContext.widgetDataStore.edit { prefs ->
                        prefs[WidgetKeys.PRESETS_JSON] = encodePresets(presets)
                    }
                }
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val presets by viewModel.presets.collectAsStateWithLifecycle()
            Timer99Theme {
                MainScreen(
                    state = state,
                    presets = presets,
                    onStart = { handleStartClick() },
                    onPause = { viewModel.pauseTimer() },
                    onAddMinute = { viewModel.addMinute() },
                    onSubtractMinute = { viewModel.subtractMinute() },
                    onReset = { viewModel.resetTimer() },
                    onSetDuration = { millis -> viewModel.setDuration(millis) },
                    onLoadPreset = { preset -> viewModel.loadPreset(preset) },
                    onSavePreset = { name, seconds -> viewModel.savePreset(name, seconds) },
                    onDeletePreset = { preset -> viewModel.deletePreset(preset) },
                    onPickAlarmSound = { openSoundPicker() },
                    modifier = Modifier,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        maybeRequestFullScreenIntentPermission()
        if (isSessionMarked()) {
            ContextCompat.startForegroundService(this, Intent(this, TimerService::class.java))
            bindService(
                Intent(this, TimerService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    override fun onStop() {
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
        super.onStop()
    }

    private fun handleStartClick() {
        maybeRequestNotificationPermission()
        if (!isSessionMarked()) {
            markSession()
            pendingStartAfterBind = true
            ContextCompat.startForegroundService(this, Intent(this, TimerService::class.java))
            bindService(
                Intent(this, TimerService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
            return
        }
        if (bound) {
            viewModel.startTimer()
        } else {
            pendingStartAfterBind = true
            ContextCompat.startForegroundService(this, Intent(this, TimerService::class.java))
            bindService(
                Intent(this, TimerService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    private fun openSoundPicker() {
        lifecycleScope.launch {
            val prefs = applicationContext.widgetDataStore.data
            val currentUriString = try {
                prefs.first()[WidgetKeys.ALARM_SOUND_URI]
            } catch (_: Exception) { null }

            val existingUri: Uri = if (!currentUriString.isNullOrBlank()) {
                currentUriString.toUri()
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(
                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                    RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE,
                )
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
            }
            soundPickerLauncher.launch(intent)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * On Android 14+, USE_FULL_SCREEN_INTENT is a runtime-managed permission
     * that must be granted via the system settings page.
     * If it is not granted, the alarm screen will not appear over the lock screen.
     */
    private fun maybeRequestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.canUseFullScreenIntent()) {
            startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                },
            )
        }
    }

    private fun isSessionMarked(): Boolean =
        getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_SESSION, false)

    private fun markSession() {
        getPreferences(Context.MODE_PRIVATE).edit().putBoolean(PREF_SESSION, true).apply()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_NEW_TIMER) {
            viewModel.resetTimer()
        }
    }

    companion object {
        const val ACTION_NEW_TIMER = "com.timer99.app.ACTION_NEW_TIMER"
        private const val PREF_SESSION = "timer99_foreground_session"
    }
}
