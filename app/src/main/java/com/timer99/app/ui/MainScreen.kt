package com.timer99.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
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
import com.timer99.app.model.Palette
import com.timer99.app.model.TimerState
import com.timer99.app.model.formatMillis
import com.timer99.app.ui.theme.LocalZen
import com.timer99.app.ui.theme.Timer99Theme
import kotlinx.coroutines.flow.filter

private val HPad = 24.dp
private val VPad = 48.dp

@Composable
fun MainScreen(
    state: TimerState,
    presets: List<Preset>,
    selectedPalette: Palette,
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
    onSelectPalette: (Palette) -> Unit,
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
                selectedPalette = selectedPalette,
                onStart = onStart,
                onSetDuration = onSetDuration,
                onLoadPreset = onLoadPreset,
                onDeletePreset = onDeletePreset,
                onPickAlarmSound = onPickAlarmSound,
                onShowSaveDialog = { showSaveDialog = true },
                onSelectPalette = onSelectPalette,
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
// Picker layout — theme chip at top, Rolodex centered, secondary actions at bottom
// ---------------------------------------------------------------------------

@Composable
private fun PickerLayout(
    state: TimerState,
    presets: List<Preset>,
    selectedPalette: Palette,
    onStart: () -> Unit,
    onSetDuration: (Long) -> Unit,
    onLoadPreset: (Preset) -> Unit,
    onDeletePreset: (Preset) -> Unit,
    onPickAlarmSound: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onSelectPalette: (Palette) -> Unit,
) {
    val zen = LocalZen.current
    var showThemePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Theme chip — top-start corner
        ThemeChip(
            palette = selectedPalette,
            onClick = { showThemePicker = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = HPad, top = VPad / 2),
        )

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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = zen.accent,
                    contentColor = zen.accentForeground,
                ),
            ) {
                Text(
                    stringResource(R.string.start),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Secondary actions pinned to the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedButton(
                onClick = onShowSaveDialog,
                enabled = state.totalMillis > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HPad),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = zen.foregroundMuted,
                    disabledContentColor = zen.foregroundSubtle,
                ),
                border = BorderStroke(1.dp, zen.border),
            ) {
                Text(stringResource(R.string.save_as_preset), fontSize = 15.sp)
            }

            if (presets.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
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
                    .padding(horizontal = HPad)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.alarm_sound),
                    style = MaterialTheme.typography.bodyMedium,
                    color = zen.foregroundSubtle,
                )
                TextButton(onClick = onPickAlarmSound) {
                    Text(stringResource(R.string.change), color = zen.accent)
                }
            }

            Spacer(Modifier.height(VPad))
        }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            selectedPalette = selectedPalette,
            onSelect = onSelectPalette,
            onDismiss = { showThemePicker = false },
        )
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
    val zen = LocalZen.current

    Box(modifier = Modifier.fillMaxSize()) {

        // Timer — truly centered.
        // Glows in the accent color while running, fades to foreground when paused.
        Text(
            text = formatMillis(state.remainingMillis),
            fontSize = 128.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Default,
            color = if (state.isRunning) zen.accent else zen.foreground,
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
// Theme chip — tappable pill showing the active palette
// ---------------------------------------------------------------------------

@Composable
private fun ThemeChip(
    palette: Palette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zen = LocalZen.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(zen.backgroundSubtle)
            .border(1.dp, zen.accentBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(palette.primary),
        )
        Text(
            text = palette.displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = zen.foreground,
        )
        Text(
            text = "›",
            fontSize = 15.sp,
            color = zen.foregroundMuted,
        )
    }
}

// ---------------------------------------------------------------------------
// Theme picker dialog
// ---------------------------------------------------------------------------

@Composable
private fun ThemePickerDialog(
    selectedPalette: Palette,
    onSelect: (Palette) -> Unit,
    onDismiss: () -> Unit,
) {
    val zen = LocalZen.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = zen.backgroundSubtle,
        titleContentColor = zen.foreground,
        textContentColor = zen.foreground,
        title = {
            Text(
                "Theme",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Palette.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { palette ->
                            val isSelected = palette == selectedPalette
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) palette.primary.copy(alpha = 0.18f)
                                        else zen.backgroundMuted,
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) palette.primary else zen.border,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onSelect(palette); onDismiss() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary),
                                )
                                Text(
                                    text = palette.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) zen.foreground else zen.foregroundMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = zen.accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
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
    val zen = LocalZen.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = zen.backgroundSubtle,
            contentColor = zen.foreground,
            disabledContainerColor = zen.backgroundMuted,
            disabledContentColor = zen.foregroundSubtle,
        ),
        border = BorderStroke(1.dp, if (enabled) zen.accentBorder else zen.border),
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
    val zen = LocalZen.current
    Text(
        text = stringResource(R.string.presets_title).uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color = zen.accent,
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
    val zen = LocalZen.current
    Box(
        modifier = modifier
            .width(155.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(zen.backgroundSubtle)
            .border(1.dp, zen.accentBorder, RoundedCornerShape(16.dp))
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
                color = zen.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatPresetDuration(preset.durationSeconds),
                fontSize = 16.sp,
                color = zen.foregroundMuted,
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
    val zen = LocalZen.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = zen.backgroundSubtle,
        titleContentColor = zen.foreground,
        textContentColor = zen.foreground,
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
                Text(stringResource(R.string.save), color = zen.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = zen.foregroundMuted)
            }
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
    val zen = LocalZen.current
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
                modifier = Modifier.width(100.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unit_min),
                style = MaterialTheme.typography.labelMedium,
                color = zen.foregroundMuted,
            )
        }
        Text(
            text = ":",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = zen.accent,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WheelNumberPicker(
                range = 0..59,
                selected = seconds,
                onSelected = { seconds = it },
                modifier = Modifier.width(100.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unit_sec),
                style = MaterialTheme.typography.labelMedium,
                color = zen.foregroundMuted,
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
    val zen = LocalZen.current
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
                .background(zen.accentSubtle),
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
                        color = if (isSelected) zen.accent else zen.foregroundSubtle,
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
                    Brush.verticalGradient(listOf(zen.background, zen.background.copy(alpha = 0f))),
                ),
        )
        // Fade bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(zen.background.copy(alpha = 0f), zen.background)),
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Picker — Indigo (default)")
@Composable
private fun PreviewPickerIndigo() {
    Timer99Theme(palette = Palette.INDIGO) {
        MainScreen(
            state = TimerState.initial(300_000L),
            presets = listOf(
                Preset(id = 1, name = "Pomodoro", durationSeconds = 1500),
                Preset(id = 2, name = "Gym rest", durationSeconds = 90),
                Preset(id = 3, name = "Break", durationSeconds = 900),
            ),
            selectedPalette = Palette.INDIGO,
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {}, onSelectPalette = {},
        )
    }
}

@Preview(showBackground = true, name = "Picker — Emerald")
@Composable
private fun PreviewPickerEmerald() {
    Timer99Theme(palette = Palette.EMERALD) {
        MainScreen(
            state = TimerState.initial(300_000L),
            presets = listOf(Preset(id = 1, name = "Morning run", durationSeconds = 1200)),
            selectedPalette = Palette.EMERALD,
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {}, onSelectPalette = {},
        )
    }
}

@Preview(showBackground = true, name = "Running — Rose")
@Composable
private fun PreviewRunningRose() {
    Timer99Theme(palette = Palette.ROSE) {
        MainScreen(
            state = TimerState(remainingMillis = 97_000L, totalMillis = 300_000L, isRunning = true),
            presets = listOf(Preset(id = 1, name = "Pomodoro", durationSeconds = 1500)),
            selectedPalette = Palette.ROSE,
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {}, onSelectPalette = {},
        )
    }
}

@Preview(showBackground = true, name = "Paused — Amber")
@Composable
private fun PreviewPausedAmber() {
    Timer99Theme(palette = Palette.AMBER) {
        MainScreen(
            state = TimerState(remainingMillis = 120_000L, totalMillis = 300_000L, isRunning = false),
            presets = emptyList(),
            selectedPalette = Palette.AMBER,
            onStart = {}, onPause = {}, onAddMinute = {}, onSubtractMinute = {}, onReset = {},
            onSetDuration = {}, onLoadPreset = {}, onSavePreset = { _, _ -> },
            onDeletePreset = {}, onPickAlarmSound = {}, onSelectPalette = {},
        )
    }
}
