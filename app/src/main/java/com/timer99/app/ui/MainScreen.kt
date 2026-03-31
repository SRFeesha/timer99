package com.timer99.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timer99.app.R
import com.timer99.app.data.Preset
import com.timer99.app.model.TimerState
import com.timer99.app.model.formatMillis
import com.timer99.app.ui.theme.Timer99Theme
import kotlinx.coroutines.flow.filter

private val PresetCardBackground = Color(0xFF0D1150)
private val PresetCardBorder     = Color(0xFF5558D4)
private val PresetTimeText       = Color(0xFF8087BB)

@Composable
fun MainScreen(
    state: TimerState,
    presets: List<Preset>,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onAddMinute: () -> Unit,
    onSubtractMinute: () -> Unit,
    onReset: () -> Unit,
    onSetDuration: (Long) -> Unit,
    onLoadPreset: (Preset) -> Unit,
    onSavePreset: (name: String, durationSeconds: Int) -> Unit,
    onDeletePreset: (Preset) -> Unit,
    onPickAlarmSound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Picker is visible only when idle (not started yet).
    val showPicker = !state.isRunning && !state.isFinished &&
            state.remainingMillis == state.totalMillis

    var showSaveDialog by remember { mutableStateOf(false) }

    if (showPicker) {
        PickerLayout(
            state = state,
            presets = presets,
            onStart = onStart,
            onSetDuration = onSetDuration,
            onLoadPreset = onLoadPreset,
            onDeletePreset = onDeletePreset,
            onPickAlarmSound = onPickAlarmSound,
            onShowSaveDialog = { showSaveDialog = true },
            modifier = modifier,
        )
    } else {
        RunningLayout(
            state = state,
            presets = presets,
            onStart = onStart,
            onPause = onPause,
            onAddMinute = onAddMinute,
            onSubtractMinute = onSubtractMinute,
            onDeletePreset = onDeletePreset,
            modifier = modifier,
        )
    }

    if (showSaveDialog) {
        SavePresetDialog(
            durationSeconds = (state.totalMillis / 1000L).toInt(),
            onConfirm = { name ->
                onSavePreset(name, (state.totalMillis / 1000L).toInt())
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Picker layout (idle state — scrollable)
// ---------------------------------------------------------------------------

@Composable
private fun PickerLayout(
    state: TimerState,
    presets: List<Preset>,
    onStart: () -> Unit,
    onSetDuration: (Long) -> Unit,
    onLoadPreset: (Preset) -> Unit,
    onDeletePreset: (Preset) -> Unit,
    onPickAlarmSound: () -> Unit,
    onShowSaveDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = stringResource(R.string.set_duration),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            DurationPicker(
                totalMillis = state.totalMillis,
                onDurationChanged = onSetDuration,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onStart,
                enabled = state.totalMillis > 0,
                modifier = Modifier.fillMaxWidth(0.55f),
            ) {
                Text(stringResource(R.string.start), fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onShowSaveDialog,
                enabled = state.totalMillis > 0,
                modifier = Modifier.fillMaxWidth(0.55f),
            ) {
                Text(stringResource(R.string.save_as_preset), fontSize = 15.sp)
            }
        }

        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            PresetsStrip(
                presets = presets,
                enabled = true,
                onLoadPreset = onLoadPreset,
                onDeletePreset = onDeletePreset,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.alarm_sound),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onPickAlarmSound) {
                Text(stringResource(R.string.change))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Running / paused layout (non-scrollable so weight() works)
// ---------------------------------------------------------------------------

@Composable
private fun RunningLayout(
    state: TimerState,
    presets: List<Preset>,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onAddMinute: () -> Unit,
    onSubtractMinute: () -> Unit,
    onDeletePreset: (Preset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Presets at the top with generous top padding.
        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(80.dp))
            PresetsStrip(
                presets = presets,
                enabled = false,
                onLoadPreset = {},
                onDeletePreset = onDeletePreset,
            )
        }

        // Push timer and controls to the lower portion of the screen.
        Spacer(Modifier.weight(1f))

        // Huge countdown.
        Text(
            text = formatMillis(state.remainingMillis),
            fontSize = 128.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Default,
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(16.dp))

        // Pause / Resume — full-width prominent button.
        TimerControlButton(
            onClick = if (state.isRunning) onPause else onStart,
            enabled = !state.isFinished,
            icon = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            label = if (state.isRunning) stringResource(R.string.pause)
                    else stringResource(R.string.resume),
            fontSize = 20.sp,
            iconSize = 20.dp,
            height = 64.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(10.dp))

        // −1m and +1m side by side.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimerControlButton(
                onClick = onSubtractMinute,
                enabled = state.isRunning,
                icon = Icons.Default.FastRewind,
                label = "−1m",
                modifier = Modifier.weight(1f),
            )
            TimerControlButton(
                onClick = onAddMinute,
                enabled = state.isRunning,
                icon = Icons.Default.FastForward,
                label = "+1m",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

// ---------------------------------------------------------------------------
// Timer control button (dark navy + indigo border, used in the running layout)
// ---------------------------------------------------------------------------

@Composable
private fun TimerControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    height: androidx.compose.ui.unit.Dp = 52.dp,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PresetCardBackground,
            contentColor = Color.White,
            disabledContainerColor = PresetCardBackground.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.3f),
        ),
        border = BorderStroke(1.dp, if (enabled) PresetCardBorder else PresetCardBorder.copy(alpha = 0.3f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = fontSize)
    }
}

// ---------------------------------------------------------------------------
// Shared presets strip
// ---------------------------------------------------------------------------

@Composable
private fun PresetsStrip(
    presets: List<Preset>,
    enabled: Boolean,
    onLoadPreset: (Preset) -> Unit,
    onDeletePreset: (Preset) -> Unit,
) {
    Text(
        text = stringResource(R.string.presets_title).uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    )
    Spacer(Modifier.height(12.dp))
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                enabled = enabled,
                onClick = { onLoadPreset(preset) },
                onLongClick = { onDeletePreset(preset) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preset card
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetCard(
    preset: Preset,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(155.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PresetCardBackground)
            .border(1.dp, PresetCardBorder, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (enabled) onClick() },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text = preset.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatPresetDuration(preset.durationSeconds),
                fontSize = 16.sp,
                color = PresetTimeText,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Save preset dialog
// ---------------------------------------------------------------------------

@Composable
private fun SavePresetDialog(
    durationSeconds: Int,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_as_preset)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name_label)) },
                placeholder = { Text(formatPresetDuration(durationSeconds)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun formatPresetDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------------------
// Duration picker
// ---------------------------------------------------------------------------

@Composable
private fun DurationPicker(
    totalMillis: Long,
    onDurationChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialMinutes = (totalMillis / 60_000L).toInt().coerceIn(0, 99)
    val initialSeconds = ((totalMillis % 60_000L) / 1_000L).toInt().coerceIn(0, 59)

    var minutes by remember { mutableIntStateOf(initialMinutes) }
    var seconds by remember { mutableIntStateOf(initialSeconds) }

    LaunchedEffect(totalMillis) {
        val newMin = (totalMillis / 60_000L).toInt().coerceIn(0, 99)
        val newSec = ((totalMillis % 60_000L) / 1_000L).toInt().coerceIn(0, 59)
        if (newMin != minutes || newSec != seconds) {
            minutes = newMin
            seconds = newSec
        }
    }

    LaunchedEffect(minutes, seconds) {
        onDurationChanged((minutes * 60L + seconds) * 1_000L)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WheelNumberPicker(
                range = 0..99,
                selected = minutes,
                onSelected = { minutes = it },
                modifier = Modifier.width(84.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.unit_min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = ":",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WheelNumberPicker(
                range = 0..59,
                selected = seconds,
                onSelected = { seconds = it },
                modifier = Modifier.width(84.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.unit_sec),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Wheel number picker
// ---------------------------------------------------------------------------

@Composable
private fun WheelNumberPicker(
    range: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(range) { range.map { "%02d".format(it) } }
    val itemHeight = 52.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selected - range.first).coerceAtLeast(0),
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)

    LaunchedEffect(selected) {
        val targetIndex = (selected - range.first).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect { onSelected(range.first + listState.firstVisibleItemIndex) }
    }

    Box(modifier = modifier.height(itemHeight * 3)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        )

        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items.size) { index ->
                val item = items[index]
                val isSelected by remember {
                    derivedStateOf { listState.firstVisibleItemIndex == index }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item,
                        fontSize = if (isSelected) 32.sp else 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                    ),
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Idle — picker")
@Composable
private fun MainScreenPickerPreview() {
    Timer99Theme {
        MainScreen(
            state = TimerState.initial(300_000L),
            presets = listOf(
                Preset(id = 1, name = "Pomodoro", durationSeconds = 1500),
                Preset(id = 2, name = "Gym rest", durationSeconds = 90),
                Preset(id = 3, name = "Break", durationSeconds = 900),
            ),
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {},
        )
    }
}

@Preview(showBackground = true, name = "Running")
@Composable
private fun MainScreenRunningPreview() {
    Timer99Theme {
        MainScreen(
            state = TimerState(remainingMillis = 97_000L, totalMillis = 300_000L, isRunning = true),
            presets = listOf(
                Preset(id = 1, name = "Pomodoro", durationSeconds = 1500),
                Preset(id = 2, name = "Gym rest", durationSeconds = 90),
            ),
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {},
        )
    }
}

@Preview(showBackground = true, name = "Paused")
@Composable
private fun MainScreenPausedPreview() {
    Timer99Theme {
        MainScreen(
            state = TimerState(remainingMillis = 120_000L, totalMillis = 300_000L, isRunning = false),
            presets = emptyList(),
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {},
        )
    }
}
