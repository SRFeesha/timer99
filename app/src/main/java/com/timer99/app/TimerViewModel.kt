package com.timer99.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.timer99.app.data.Preset
import com.timer99.app.data.PresetRepository
import com.timer99.app.model.TimerState
import com.timer99.app.service.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(private val repository: PresetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerState.initial())
    val uiState: StateFlow<TimerState> = _uiState.asStateFlow()

    val presets: StateFlow<List<Preset>> = repository.presets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var service: TimerService? = null
    private var collectJob: Job? = null

    fun attachService(timerService: TimerService) {
        service = timerService
        // If the service is idle, push the locally chosen duration into it
        // (covers the case where the user set a custom duration before the first bind).
        val local = _uiState.value
        if (!timerService.timerState.value.isRunning &&
            local.totalMillis != timerService.timerState.value.totalMillis
        ) {
            timerService.setDuration(local.totalMillis)
        }
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            timerService.timerState.collect { _uiState.value = it }
        }
    }

    fun setDuration(totalMillis: Long) {
        val s = service
        if (s == null) {
            _uiState.value = TimerState.initial(totalMillis)
        } else {
            s.setDuration(totalMillis)
        }
    }

    fun startTimer() {
        service?.startTimer()
    }

    fun pauseTimer() {
        service?.pauseTimer()
    }

    fun addMinute() {
        service?.addMinute()
    }

    fun subtractMinute() {
        service?.subtractMinute()
    }

    fun resetTimer() {
        val s = service
        if (s == null) {
            _uiState.value = TimerState.initial()
        } else {
            s.resetTimer()
        }
    }

    fun loadPreset(preset: Preset) {
        val millis = preset.durationSeconds * 1000L
        val s = service
        if (s == null) {
            _uiState.value = TimerState.initial(millis)
        } else {
            s.setDurationWithPreset(millis, preset.name)
        }
    }

    fun savePreset(name: String, durationSeconds: Int) {
        viewModelScope.launch {
            repository.insert(Preset(name = name, durationSeconds = durationSeconds))
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            repository.delete(preset)
        }
    }

    class Factory(private val repository: PresetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TimerViewModel(repository) as T
    }
}
