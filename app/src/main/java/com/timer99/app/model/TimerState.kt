package com.timer99.app.model

data class TimerState(
    val remainingMillis: Long,
    val totalMillis: Long,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
) {
    companion object {
        fun initial(totalMillis: Long = DEFAULT_TOTAL_MILLIS): TimerState =
            TimerState(
                remainingMillis = totalMillis,
                totalMillis = totalMillis,
                isRunning = false,
                isFinished = false,
            )
    }
}

const val DEFAULT_TOTAL_MILLIS = 5_000L

fun formatMillis(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
