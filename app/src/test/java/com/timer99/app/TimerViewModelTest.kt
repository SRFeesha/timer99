package com.timer99.app

import app.cash.turbine.test
import com.timer99.app.data.Preset
import com.timer99.app.model.DEFAULT_TOTAL_MILLIS
import com.timer99.app.model.TimerState
import com.timer99.app.util.TestDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TimerViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var repository: FakePresetRepository
    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        repository = FakePresetRepository()
        viewModel = TimerViewModel(repository)
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `uiState initial value matches TimerState initial`() {
        assertEquals(TimerState.initial(), viewModel.uiState.value)
    }

    // -------------------------------------------------------------------------
    // setDuration (no service attached)
    // -------------------------------------------------------------------------

    @Test
    fun `setDuration without service updates uiState totalMillis and remainingMillis`() {
        viewModel.setDuration(60_000L)

        assertEquals(TimerState.initial(60_000L), viewModel.uiState.value)
    }

    @Test
    fun `setDuration keeps timer not running`() {
        viewModel.setDuration(120_000L)

        assertEquals(false, viewModel.uiState.value.isRunning)
        assertEquals(false, viewModel.uiState.value.isFinished)
    }

    // -------------------------------------------------------------------------
    // resetTimer (no service attached)
    // -------------------------------------------------------------------------

    @Test
    fun `resetTimer without service restores default initial state`() {
        viewModel.setDuration(90_000L)
        viewModel.resetTimer()

        assertEquals(TimerState.initial(), viewModel.uiState.value)
    }

    @Test
    fun `resetTimer without service sets DEFAULT_TOTAL_MILLIS`() {
        viewModel.resetTimer()

        assertEquals(DEFAULT_TOTAL_MILLIS, viewModel.uiState.value.totalMillis)
        assertEquals(DEFAULT_TOTAL_MILLIS, viewModel.uiState.value.remainingMillis)
    }

    // -------------------------------------------------------------------------
    // loadPreset (no service attached)
    // -------------------------------------------------------------------------

    @Test
    fun `loadPreset without service sets duration from preset durationSeconds`() {
        val preset = Preset(id = 1, name = "Pomodoro", durationSeconds = 1500)

        viewModel.loadPreset(preset)

        assertEquals(TimerState.initial(1_500_000L), viewModel.uiState.value)
    }

    @Test
    fun `loadPreset converts seconds to millis correctly`() {
        val preset = Preset(id = 2, name = "Short Break", durationSeconds = 300)

        viewModel.loadPreset(preset)

        assertEquals(300_000L, viewModel.uiState.value.totalMillis)
        assertEquals(300_000L, viewModel.uiState.value.remainingMillis)
    }

    // -------------------------------------------------------------------------
    // savePreset
    // -------------------------------------------------------------------------

    @Test
    fun `savePreset inserts a Preset with the given name and duration`() = runTest {
        viewModel.savePreset("Work", 1500)

        assertEquals(1, repository.insertedPresets.size)
        assertEquals("Work", repository.insertedPresets[0].name)
        assertEquals(1500, repository.insertedPresets[0].durationSeconds)
    }

    @Test
    fun `savePreset inserts with default id zero`() = runTest {
        viewModel.savePreset("Break", 300)

        assertEquals(0, repository.insertedPresets[0].id)
    }

    // -------------------------------------------------------------------------
    // deletePreset
    // -------------------------------------------------------------------------

    @Test
    fun `deletePreset forwards the preset to the repository`() = runTest {
        val preset = Preset(id = 5, name = "Lunch", durationSeconds = 3600)

        viewModel.deletePreset(preset)

        assertEquals(listOf(preset), repository.deletedPresets)
    }

    // -------------------------------------------------------------------------
    // presets StateFlow
    // -------------------------------------------------------------------------

    @Test
    fun `presets emits empty list initially then updates when repository emits`() = runTest {
        val testPresets = listOf(
            Preset(id = 1, name = "Pomodoro", durationSeconds = 1500),
            Preset(id = 2, name = "Short Break", durationSeconds = 300),
        )

        viewModel.presets.test {
            assertEquals(emptyList<Preset>(), awaitItem()) // stateIn initialValue

            repository.sendPresets(testPresets)
            assertEquals(testPresets, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `presets reflects successive repository emissions`() = runTest {
        val first = listOf(Preset(id = 1, name = "Pomodoro", durationSeconds = 1500))
        val second = first + Preset(id = 2, name = "Short Break", durationSeconds = 300)

        viewModel.presets.test {
            awaitItem() // discard initial emptyList

            repository.sendPresets(first)
            assertEquals(first, awaitItem())

            repository.sendPresets(second)
            assertEquals(second, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
