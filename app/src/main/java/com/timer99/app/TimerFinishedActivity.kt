package com.timer99.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.timer99.app.data.selectedTeamFlow
import com.timer99.app.model.Team
import com.timer99.app.service.TimerService
import com.timer99.app.ui.theme.Timer99Theme

class TimerFinishedActivity : ComponentActivity() {

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismiss()
        })

        setContent {
            val team by applicationContext.selectedTeamFlow
                .collectAsState(initial = Team.DEFAULT_TEAM)
            Timer99Theme(team = team) {
                FinishedScreen(
                    presetName = presetName,
                    onDismiss = ::dismiss,
                    onExtend1Min = { stopAlertAndSend(TimerService.ACTION_EXTEND_1MIN) },
                    onExtend5Min = { stopAlertAndSend(TimerService.ACTION_EXTEND_5MIN) },
                )
            }
        }
    }

    private fun dismiss() {
        stopAlertAndSend(TimerService.ACTION_DISMISS_ALERT)
    }

    /** Tell the service to stop the alarm, then close this activity. */
    private fun stopAlertAndSend(action: String) {
        ContextCompat.startForegroundService(
            this,
            Intent(this, TimerService::class.java).apply { this.action = action },
        )
        finish()
    }

    companion object {
        const val EXTRA_PRESET_NAME = "extra_preset_name"
    }
}

@Preview(showBackground = true, name = "Finished — with preset name")
@Composable
private fun FinishedScreenPreview() {
    Timer99Theme(team = Team.LAKERS) {
        FinishedScreen(
            presetName = "Pomodoro",
            onDismiss = {},
            onExtend1Min = {},
            onExtend5Min = {},
        )
    }
}

@Preview(showBackground = true, name = "Finished — no preset")
@Composable
private fun FinishedScreenNoPresetPreview() {
    Timer99Theme(team = Team.CHIEFS) {
        FinishedScreen(
            presetName = null,
            onDismiss = {},
            onExtend1Min = {},
            onExtend5Min = {},
        )
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
                text = stringResource(R.string.timer_finished),
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(56.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.dismiss), fontSize = 18.sp)
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
                    Text(stringResource(R.string.action_add_minute), fontSize = 15.sp)
                }
                OutlinedButton(
                    onClick = onExtend5Min,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(stringResource(R.string.action_add_five_minutes), fontSize = 15.sp)
                }
            }
        }
    }
}
