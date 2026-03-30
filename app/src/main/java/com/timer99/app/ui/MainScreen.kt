package com.timer99.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timer99.app.data.Preset
import com.timer99.app.model.TimerState
import com.timer99.app.model.formatMillis
import kotlinx.coroutines.flow.filter

@Composable
fun MainScreen(
    state: TimerState,
    presets: List<Preset>,
    onStart: () -> Unit,
    onPause: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        if (showPicker) {
            Text(
                text = "Set Duration",
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
                Text("Start", fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showSaveDialog = true },
                enabled = state.totalMillis > 0,
                modifier = Modifier.fillMaxWidth(0.55f),
            ) {
                Text("Save as preset", fontSize = 15.sp)
            }
        } else {
            Text(
                text = formatMillis(state.remainingMillis),
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD4BBFF),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    state.isFinished -> "Done"
                    state.isRunning -> "Running"
                    else -> "Paused"
                },
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                if (state.isRunning) {
                    Button(onClick = onPause) { Text("Pause") }
                } else {
                    Button(
                        onClick = onStart,
                        enabled = state.remainingMillis > 0 && !state.isFinished,
                    ) {
                        Text("Start")
                    }
                }
                OutlinedButton(onClick = onReset) { Text("Reset") }
            }
        }

        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    key(preset.id) {
                        PresetItem(
                            preset = preset,
                            canLoad = showPicker,
                            onClick = { onLoadPreset(preset) },
                            onDelete = { onDeletePreset(preset) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Alarm sound setting — always visible at the bottom.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Alarm sound",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onPickAlarmSound) {
                Text("Change")
            }
        }

        Spacer(Modifier.height(16.dp))
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

@Composable
private fun SavePresetDialog(
    durationSeconds: Int,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as preset") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text(formatPresetDuration(durationSeconds)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetItem(
    preset: Preset,
    canLoad: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete preset",
                    tint = Color.White,
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            onClick = onClick,
            enabled = canLoad,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatPresetDuration(preset.durationSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

private fun formatPresetDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

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

    // Sync wheels when duration is set externally (e.g. tapping a preset).
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
                text = "min",
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
                text = "sec",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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

    // Animate wheel to the correct position when selected changes externally.
    LaunchedEffect(selected) {
        val targetIndex = (selected - range.first).coerceAtLeast(0)
        if (listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Report the centred item to the caller each time scrolling comes to rest.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect { onSelected(range.first + listState.firstVisibleItemIndex) }
    }

    Box(modifier = modifier.height(itemHeight * 3)) {
        // Selection-highlight band behind the centre row
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            // The vertical padding keeps the first and last items centrable.
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, item ->
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

        // Gradient fade at the top
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
        // Gradient fade at the bottom
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
