package com.timer99.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

private val HPad = 24.dp
private val VPad = 48.dp

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
    val showPicker = !state.isRunning && !state.isFinished &&
            state.remainingMillis == state.totalMillis

    var showSaveDialog by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
}

// ---------------------------------------------------------------------------
// Picker layout — Rolodex centered, presets at the bottom, settings top-end
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val cs = MaterialTheme.colorScheme
    var showSettingsSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Duration picker + Start — vertically centered
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = HPad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DurationPicker(
                totalMillis = state.totalMillis,
                onDurationChanged = onSetDuration,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onStart,
                enabled = state.totalMillis > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.start),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        // Presets strip pinned to the bottom
        if (presets.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PresetsStrip(
                    presets = presets,
                    enabled = true,
                    onLoadPreset = onLoadPreset,
                    onDeletePreset = onDeletePreset,
                )
                Spacer(Modifier.height(VPad))
            }
        }

        // Settings button — bottom-end corner
        IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = HPad, bottom = VPad / 2),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = cs.onSurfaceVariant,
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = HPad, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.save_as_preset)) },
                leadingContent = {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                },
                modifier = Modifier
                    .clickable(enabled = state.totalMillis > 0) {
                        showSettingsSheet = false
                        onShowSaveDialog()
                    }
                    .alpha(if (state.totalMillis > 0) 1f else 0.38f),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.alarm_sound)) },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    showSettingsSheet = false
                    onPickAlarmSound()
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Running / paused layout — timer centered, controls + presets at the bottom
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
) {
    val cs = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize()) {

        // Timer — truly centered.
        // Glows in the accent color while running, fades to foreground when paused.
        Text(
            text = formatMillis(state.remainingMillis),
            fontSize = 128.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Default,
            color = if (state.isRunning) cs.primary else cs.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = HPad),
        )

        // Controls + presets pinned to the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pause / Resume — full-width
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
                    .padding(horizontal = HPad),
            )

            Spacer(Modifier.height(10.dp))

            // −1m and +1m side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HPad),
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

            if (presets.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PresetsStrip(
                    presets = presets,
                    enabled = false,
                    onLoadPreset = {},
                    onDeletePreset = onDeletePreset,
                )
            }

            Spacer(Modifier.height(VPad))
        }
    }
}

// ---------------------------------------------------------------------------
// Timer control button (subtle card + accent border)
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
    val cs = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = cs.surfaceVariant,
            contentColor = cs.onSurface,
            disabledContainerColor = cs.surface,
            disabledContentColor = cs.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, if (enabled) cs.primary.copy(alpha = 0.5f) else cs.outlineVariant),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
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
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HPad),
    )
    Spacer(Modifier.height(12.dp))
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = HPad),
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
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(155.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceVariant)
            .border(1.dp, cs.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatPresetDuration(preset.durationSeconds),
                fontSize = 16.sp,
                color = cs.onSurfaceVariant,
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
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceVariant,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurface,
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
                Text(stringResource(R.string.save), color = cs.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = cs.onSurfaceVariant)
            }
        },
    )
}

private fun formatPresetDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
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
    val cs = MaterialTheme.colorScheme
    val totalSec = totalMillis / 1_000L
    val initialHours   = (totalSec / 3600L).toInt().coerceIn(0, 23)
    val initialMinutes = ((totalSec % 3600L) / 60L).toInt().coerceIn(0, 59)
    val initialSeconds = (totalSec % 60L).toInt().coerceIn(0, 59)

    var hours   by remember { mutableIntStateOf(initialHours) }
    var minutes by remember { mutableIntStateOf(initialMinutes) }
    var seconds by remember { mutableIntStateOf(initialSeconds) }

    LaunchedEffect(totalMillis) {
        val ts = totalMillis / 1_000L
        val newH   = (ts / 3600L).toInt().coerceIn(0, 23)
        val newMin = ((ts % 3600L) / 60L).toInt().coerceIn(0, 59)
        val newSec = (ts % 60L).toInt().coerceIn(0, 59)
        if (newH != hours || newMin != minutes || newSec != seconds) {
            hours   = newH
            minutes = newMin
            seconds = newSec
        }
    }

    LaunchedEffect(hours, minutes, seconds) {
        onDurationChanged((hours * 3600L + minutes * 60L + seconds) * 1_000L)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WheelNumberPicker(
                range = 0..23,
                selected = hours,
                onSelected = { hours = it },
                modifier = Modifier.width(80.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unit_hr),
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
        }
        Text(
            text = ":",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = cs.primary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WheelNumberPicker(
                range = 0..59,
                selected = minutes,
                onSelected = { minutes = it },
                modifier = Modifier.width(80.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unit_min),
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
        }
        Text(
            text = ":",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = cs.primary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WheelNumberPicker(
                range = 0..59,
                selected = seconds,
                onSelected = { seconds = it },
                modifier = Modifier.width(80.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unit_sec),
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
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
    val cs = MaterialTheme.colorScheme
    val items = remember(range) { range.map { "%02d".format(it) } }
    val itemHeight = 64.dp
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
        // Selection highlight
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(cs.primaryContainer),
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
                        fontSize = if (isSelected) 40.sp else 26.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) cs.primary else cs.onSurfaceVariant,
                    )
                }
            }
        }

        // Fade top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(cs.background, cs.background.copy(alpha = 0f))),
                ),
        )
        // Fade bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(cs.background.copy(alpha = 0f), cs.background)),
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
            presets = listOf(Preset(id = 1, name = "Pomodoro", durationSeconds = 1500)),
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
