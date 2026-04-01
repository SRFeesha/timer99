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

const val DEFAULT_TOTAL_MILLIS = 300_000L // 5 minutes

fun formatMillis(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
