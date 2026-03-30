package com.timer99.app.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timer99.app.MainActivity
import com.timer99.app.R
import com.timer99.app.data.WidgetKeys
import com.timer99.app.data.widgetDataStore
import com.timer99.app.model.formatMillis

private fun formatWidgetTime(millis: Long): String =
    if (millis > 60_000L) "~${(millis + 29_999L) / 60_000L}m" else formatMillis(millis)

class TimerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs by context.widgetDataStore.data.collectAsState(initial = emptyPreferences())
            WidgetContent(prefs, context)
        }
    }
}

@Composable
private fun WidgetContent(prefs: Preferences, context: Context) {
    val isRunning = prefs[WidgetKeys.IS_RUNNING] ?: false
    val remainingMillis = prefs[WidgetKeys.REMAINING_MILLIS] ?: 0L
    val presetName = prefs[WidgetKeys.PRESET_NAME] ?: ""

    // Explicit intent used for both states — reliable from a widget host process.
    val openMainIntent = Intent().apply {
        component = ComponentName(context, MainActivity::class.java)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    if (!isRunning) {
        // Idle — transparent, but tapping still opens the app.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(openMainIntent)),
        ) {}
        return
    }

    // Active — semi-transparent dark card. Tapping anywhere brings the app to foreground.
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg_active))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(actionStartActivity(openMainIntent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Large countdown
        Text(
            text = formatWidgetTime(remainingMillis),
            style = TextStyle(
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White),
                textAlign = TextAlign.Center,
            ),
        )

        // Active preset label
        if (presetName.isNotBlank()) {
            Text(
                text = presetName,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color(0xFF94A3B8)),
                    textAlign = TextAlign.Center,
                ),
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        // Pause / +1 min / New timer buttons
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.glance.Button(
                text = "Pause",
                onClick = actionRunCallback<PauseCallback>(),
            )
            Spacer(GlanceModifier.width(8.dp))
            androidx.glance.Button(
                text = "+1 min",
                onClick = actionRunCallback<AddMinuteCallback>(),
            )
        }
    }
}
