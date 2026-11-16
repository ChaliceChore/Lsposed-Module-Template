package io.github.chalicechore.moduletemplate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.chalicechore.moduletemplate.HomeLayout
import io.github.chalicechore.moduletemplate.ModuleApp
import io.github.chalicechore.moduletemplate.prefs.Appearance
import io.github.chalicechore.moduletemplate.prefs.Applies
import io.github.chalicechore.moduletemplate.prefs.AppearanceRepository
import io.github.chalicechore.moduletemplate.prefs.Choice
import io.github.chalicechore.moduletemplate.prefs.FeatureSettings
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.FrameworkConnection
import io.github.chalicechore.moduletemplate.prefs.FrameworkInfo
import io.github.chalicechore.moduletemplate.prefs.HookStatus
import io.github.chalicechore.moduletemplate.prefs.HookStatusRepository
import io.github.chalicechore.moduletemplate.prefs.PrefsRepository
import io.github.chalicechore.moduletemplate.prefs.TargetRestarter
import io.github.chalicechore.moduletemplate.prefs.ThemeMode
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.chalicechore.moduletemplate.prefs.Troubleshooting
import io.github.chalicechore.moduletemplate.prefs.TroubleshootingRepository
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
        val hookStatus: HookStatus = HookStatus(),
    ) : UiState {
        val appliedOrDefault: FeatureSettings get() = applied ?: FeatureSettings()
        val pendingCount: Int get() = desired.countDifferences(appliedOrDefault)
        val choiceChanged: Boolean get() = desired.choice != appliedOrDefault.choice
        val moduleEnabledChanged: Boolean get() = desired.moduleEnabled != appliedOrDefault.moduleEnabled

        /** Applying the pending changes restarts the target (false: only LIVE ones are left). */
        val restartNeeded: Boolean get() = desired.needsRestart(appliedOrDefault)

        /**
         * The module is on and nothing is pending, but the running target never loaded it
         * (it started before the module was enabled in LSPosed): restarting the target fixes it.
         */
        val targetNeedsRestart: Boolean
            get() =
                framework != null && appliedOrDefault.moduleEnabled && pendingCount == 0 &&
                    !hookStatus.loadedSinceBoot

        /** Features whose hooks failed at the target's last start. */
        val failedFeatures get() = hookStatus.failed
    }
}

private data class Stored(
    val desired: FeatureSettings,
    val applied: FeatureSettings?,
    val appearance: Appearance,
    val hook: HookStatus,
)

class MainViewModel(
    private val preferences: PrefsRepository,
    private val appearanceRepository: AppearanceRepository,
    private val hookStatusRepository: HookStatusRepository,
    private val troubleshootingRepository: TroubleshootingRepository,
    private val framework: FrameworkConnection,
    private val restarter: TargetRestarter,
) : ViewModel() {
    private val phase = MutableStateFlow<ApplyPhase>(ApplyPhase.Idle)
    private val applyLock = Mutex()

    // One writer at a time, so quick taps never overwrite each other with stale settings.
    private val writer = Dispatchers.IO.limitedParallelism(1)

    val uiState: StateFlow<UiState> =
        combine(
            combine(
                preferences.observeDesired(),
                preferences.observeApplied(),
                appearanceRepository.observe(),
                hookStatusRepository.observe(),
            ) { desired, applied, appearance, hook -> Stored(desired, applied, appearance, hook) }
                .flowOn(Dispatchers.IO),
            framework.framework,
            phase,
        ) { stored, info, phase ->
            UiState.Ready(stored.desired, stored.applied, stored.appearance, info, phase, stored.hook)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun setSwitch(
        switch: FeatureSwitch,
        on: Boolean,
    ) {
        clearFailure()
        viewModelScope.launch(writer) {
            preferences.save(preferences.desiredSettings().with(switch, on))
            if (switch.applies == Applies.LIVE) {
                // Straight to the hook. If that fails (no framework), it stays pending instead.
                val sent = framework.publish { it.putBoolean(switch.key, on) }
                if (sent) {
                    val applied = preferences.appliedSettings() ?: FeatureSettings()
                    preferences.markApplied(applied.with(switch, on))
                }
            }
        }
    }

    fun setModuleEnabled(on: Boolean) {
        clearFailure()
        viewModelScope.launch(writer) {
            preferences.save(preferences.desiredSettings().copy(moduleEnabled = on))
        }
    }

    fun setChoice(choice: Choice) {
        clearFailure()
        viewModelScope.launch(writer) {
            preferences.save(preferences.desiredSettings().copy(choice = choice))
        }
    }

    val troubleshooting: StateFlow<Troubleshooting> =
        troubleshootingRepository
            .observe()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Eagerly, Troubleshooting())

    fun setHealthAlerts(on: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { troubleshootingRepository.setHealthAlerts(on) }
    }

    /**
     * Debug logging: on for [TroubleshootingRepository.DEBUG_HOURS] at most. Sent to the hook
     * straight away (it is read live), so it takes effect without a restart.
     */
    fun setDebugLogging(on: Boolean) {
        val until =
            if (on) System.currentTimeMillis() + TroubleshootingRepository.DEBUG_HOURS * 3_600_000L else 0L
        viewModelScope.launch(writer) {
            troubleshootingRepository.setDebugUntil(until)
            framework.publish { it.putLong(SettingsKeys.DEBUG_UNTIL, until) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { appearanceRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { appearanceRepository.setDynamicColor(enabled) }
    }

    fun setHomeLayout(layout: HomeLayout) {
        viewModelScope.launch(Dispatchers.IO) { appearanceRepository.setHomeLayout(layout) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { appearanceRepository.setPureBlack(enabled) }
    }

    fun apply() {
        viewModelScope.launch {
            applyLock.withLock {
                val ready = uiState.value as? UiState.Ready ?: return@withLock
                val settings = ready.desired
                val restart = ready.restartNeeded
                phase.value = ApplyPhase.Saving
                if (!framework.publish(settings)) {
                    phase.value = ApplyPhase.Failed(ApplyFailure.NotSent)
                    return@withLock
                }
                preferences.markApplied(settings)
                if (!restart) {
                    phase.value = ApplyPhase.Idle
                    return@withLock
                }
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

    /** A restored backup's settings become pending changes, applied through the sheet. */
    fun restoreSettings(settings: FeatureSettings) {
        clearFailure()
        viewModelScope.launch(writer) { preferences.save(settings) }
    }

    fun restoreAppearance(appearance: Appearance) {
        viewModelScope.launch(Dispatchers.IO) {
            appearanceRepository.setThemeMode(appearance.themeMode)
            appearanceRepository.setDynamicColor(appearance.dynamicColor)
            appearanceRepository.setPureBlack(appearance.pureBlack)
            appearanceRepository.setHomeLayout(appearance.homeLayout)
        }
    }

    /** Puts every pending setting back to what was last applied. */
    fun discard() {
        clearFailure()
        viewModelScope.launch(writer) {
            preferences.save(preferences.appliedSettings() ?: FeatureSettings())
        }
    }

    /** Puts one switch back to its applied value. */
    fun revertSwitch(switch: FeatureSwitch) {
        val applied = (uiState.value as? UiState.Ready)?.appliedOrDefault ?: return
        setSwitch(switch, applied.isOn(switch))
    }

    fun revertChoice() {
        val applied = (uiState.value as? UiState.Ready)?.appliedOrDefault ?: return
        setChoice(applied.choice)
    }

    fun revertModuleEnabled() {
        val applied = (uiState.value as? UiState.Ready)?.appliedOrDefault ?: return
        setModuleEnabled(applied.moduleEnabled)
    }

    /** Restarts the target with nothing to apply, so it loads the module. */
    fun restartTarget() {
        viewModelScope.launch {
            applyLock.withLock {
                phase.value = ApplyPhase.Restarting
                phase.value =
                    if (restarter.restart()) ApplyPhase.Idle else ApplyPhase.Failed(ApplyFailure.RestartFailed)
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
                        hookStatusRepository = app.hookStatusRepository,
                        troubleshootingRepository = app.troubleshootingRepository,
                        framework = app.frameworkConnection,
                        restarter = app.targetRestarter,
                    )
                }
            }
    }
}
