package com.timer99.app

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.timer99.app.data.WidgetKeys
import com.timer99.app.data.widgetDataStore
import com.timer99.app.service.TimerService
import com.timer99.app.ui.theme.Timer99Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerFinishedActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val rampHandler = Handler(Looper.getMainLooper())
    private var startTimeMs = 0L

    // Ticks every 100 ms, ramping volume via smoothstep over 60 seconds.
    private val rampRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            val elapsed = System.currentTimeMillis() - startTimeMs
            val t = (elapsed / 60_000f).coerceIn(0f, 1f)
            // Smoothstep: ease-in-out S-curve
            val volume = t * t * (3f - 2f * t)
            player.setVolume(volume, volume)
            if (t < 1f) rampHandler.postDelayed(this, RAMP_TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val presetName = intent.getStringExtra(EXTRA_PRESET_NAME)

        // Start alarm sound at volume 0.0, then begin ramp immediately.
        lifecycleScope.launch {
            val prefs = applicationContext.widgetDataStore.data.first()
            val uriString = prefs[WidgetKeys.ALARM_SOUND_URI] ?: ""
            val alarmUri: Uri = if (uriString.isNotBlank()) uriString.toUri()
            else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            startAlarm(alarmUri)
        }

        // Vibration starts at 3 seconds.
        rampHandler.postDelayed({ startVibration() }, VIBRATION_DELAY_MS)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismiss()
        })

        setContent {
            Timer99Theme {
                FinishedScreen(
                    presetName = presetName,
                    onDismiss = ::dismiss,
                    onExtend1Min = { stopAlertAndSend(TimerService.ACTION_EXTEND_1MIN) },
                    onExtend5Min = { stopAlertAndSend(TimerService.ACTION_EXTEND_5MIN) },
                )
            }
        }
    }

    override fun onDestroy() {
        rampHandler.removeCallbacksAndMessages(null)
        stopMediaPlayer()
        stopVibrator()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------
    // Alert internals
    // ---------------------------------------------------------------------------

    private fun startAlarm(uri: Uri) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_ALARM)
            .build()

        fun buildPlayer(src: Uri): MediaPlayer = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setDataSource(applicationContext, src)
            setVolume(0f, 0f)   // start silent; ramp brings it up
            isLooping = true
            prepare()
            start()
        }

        mediaPlayer = try {
            buildPlayer(uri)
        } catch (_: Exception) {
            val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (uri != fallback) try { buildPlayer(fallback) } catch (_: Exception) { null }
            else null
        }

        startTimeMs = System.currentTimeMillis()
        rampHandler.post(rampRunnable)
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 500, 500), /* repeat= */ 0),
        )
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun stopVibrator() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun dismiss() {
        stopAlertAndSend(TimerService.ACTION_DISMISS_ALERT)
    }

    /** Stop local sound/vibration, send action to service, finish. */
    private fun stopAlertAndSend(action: String) {
        rampHandler.removeCallbacksAndMessages(null)
        stopMediaPlayer()
        stopVibrator()
        ContextCompat.startForegroundService(
            this,
            Intent(this, TimerService::class.java).apply { this.action = action },
        )
        finish()
    }

    companion object {
        const val EXTRA_PRESET_NAME = "extra_preset_name"
        private const val RAMP_TICK_MS = 100L
        private const val VIBRATION_DELAY_MS = 3_000L
    }
}

@Composable
private fun FinishedScreen(
    presetName: String?,
    onDismiss: () -> Unit,
    onExtend1Min: () -> Unit,
    onExtend5Min: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!presetName.isNullOrBlank()) {
                Text(
                    text = presetName,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = "Timer done!",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(56.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(52.dp),
            ) {
                Text("Dismiss", fontSize = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.65f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onExtend1Min,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("+1 min", fontSize = 15.sp)
                }
                OutlinedButton(
                    onClick = onExtend5Min,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("+5 min", fontSize = 15.sp)
                }
            }
        }
    }
}
