package io.github.chalicechore.moduletemplate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalicechore.moduletemplate.ModuleApp
import io.github.chalicechore.moduletemplate.prefs.Appearance
import io.github.chalicechore.moduletemplate.prefs.AppearanceRepository
import io.github.chalicechore.moduletemplate.prefs.Choice
import io.github.chalicechore.moduletemplate.prefs.FeatureSettings
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.FrameworkConnection
import io.github.chalicechore.moduletemplate.prefs.FrameworkInfo
import io.github.chalicechore.moduletemplate.prefs.PrefsRepository
import io.github.chalicechore.moduletemplate.prefs.TargetRestarter
import io.github.chalicechore.moduletemplate.prefs.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ApplyFailure {
    NotSent,
    RestartFailed,
}

sealed interface ApplyPhase {
    data object Idle : ApplyPhase

    data object Saving : ApplyPhase

    data object Restarting : ApplyPhase

    data class Failed(
        val reason: ApplyFailure,
    ) : ApplyPhase

    val isRunning: Boolean
        get() = this == Saving || this == Restarting
}

sealed interface UiState {
    data object Loading : UiState

    data class Ready(
        val desired: FeatureSettings,
        val applied: FeatureSettings?,
        val appearance: Appearance,
        val framework: FrameworkInfo?,
        val phase: ApplyPhase,
    ) : UiState {
        val appliedOrDefault: FeatureSettings get() = applied ?: FeatureSettings()
        val pendingCount: Int get() = desired.countDifferences(appliedOrDefault)
        val choiceChanged: Boolean get() = desired.choice != appliedOrDefault.choice
    }
}

class MainViewModel(
    private val preferences: PrefsRepository,
    private val appearanceRepository: AppearanceRepository,
    private val framework: FrameworkConnection,
    private val restarter: TargetRestarter,
) : ViewModel() {
    private val phase = MutableStateFlow<ApplyPhase>(ApplyPhase.Idle)
    private val applyLock = Mutex()

    // One writer at a time, so quick taps never overwrite each other with stale settings.
    private val writer = Dispatchers.IO.limitedParallelism(1)

    val uiState: StateFlow<UiState> =
        combine(
            preferences.observeDesired().flowOn(Dispatchers.IO),
            preferences.observeApplied().flowOn(Dispatchers.IO),
            appearanceRepository.observe().flowOn(Dispatchers.IO),
            framework.framework,
            phase,
        ) { desired, applied, appearance, info, phase ->
            UiState.Ready(desired, applied, appearance, info, phase)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private fun current(): FeatureSettings? = (uiState.value as? UiState.Ready)?.desired

    fun setSwitch(
        switch: FeatureSwitch,
        on: Boolean,
    ) {
        clearFailure()
        viewModelScope.launch(writer) {
            preferences.save(preferences.desiredSettings().with(switch, on))
        }
    }

    fun setChoice(choice: Choice) {
        clearFailure()
        viewModelScope.launch(writer) {
            preferences.save(preferences.desiredSettings().copy(choice = choice))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { appearanceRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { appearanceRepository.setDynamicColor(enabled) }
    }

    fun apply() {
        viewModelScope.launch {
            applyLock.withLock {
                val settings = current() ?: return@withLock
                phase.value = ApplyPhase.Saving
                if (!framework.publish(settings)) {
                    phase.value = ApplyPhase.Failed(ApplyFailure.NotSent)
                    return@withLock
                }
                preferences.markApplied(settings)
                phase.value = ApplyPhase.Restarting
                phase.value =
                    if (restarter.restart()) {
                        ApplyPhase.Idle
                    } else {
                        ApplyPhase.Failed(ApplyFailure.RestartFailed)
                    }
            }
        }
    }

    private fun clearFailure() {
        if (phase.value is ApplyPhase.Failed) phase.value = ApplyPhase.Idle
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as ModuleApp
                    MainViewModel(
                        preferences = app.prefsRepository,
                        appearanceRepository = app.appearanceRepository,
                        framework = app.frameworkConnection,
                        restarter = app.targetRestarter,
                    )
                }
            }
    }
}
