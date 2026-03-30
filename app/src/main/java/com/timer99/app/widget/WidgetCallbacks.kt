package com.timer99.app.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.timer99.app.service.TimerService

/** Pauses the running timer via the foreground service. */
class PauseCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP },
        )
    }
}

/** Adds 60 s to the running timer via the foreground service. */
class AddMinuteCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_ADD_MINUTE
            },
        )
    }
}
