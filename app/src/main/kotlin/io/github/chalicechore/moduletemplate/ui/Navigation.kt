@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.ModuleConfig
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.FeatureCategory
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.chalicechore.moduletemplate.ui.component.ApplySheetContent
import io.github.chalicechore.moduletemplate.ui.component.ApplySheetScaffold
import io.github.chalicechore.moduletemplate.ui.component.PendingChange
import io.github.chalicechore.moduletemplate.ui.screen.AboutScreen
import io.github.chalicechore.moduletemplate.ui.screen.BackupRestoreGroup
import io.github.chalicechore.moduletemplate.ui.screen.ComponentsDemoScreen
import io.github.chalicechore.moduletemplate.ui.screen.AppearanceScreen
import io.github.chalicechore.moduletemplate.ui.screen.CategoryScreen
import io.github.chalicechore.moduletemplate.ui.screen.HomeScreen
import io.github.chalicechore.moduletemplate.ui.screen.LicensesScreen
import io.github.chalicechore.moduletemplate.ui.screen.FeatureHealthScreen
import io.github.chalicechore.moduletemplate.ui.screen.MoreModulesScreen
import io.github.chalicechore.moduletemplate.ui.screen.SearchScreen
import io.github.chalicechore.moduletemplate.ui.screen.TroubleshootingGroup
import io.github.chalicechore.moduletemplate.ui.theme.ModuleTheme
import io.github.chalicechore.moduletemplate.ui.theme.isReducedMotion

/** Screens. Routes are strings so the back stack survives process death. */
sealed interface Destination {
    data object Home : Destination

    /** @property highlight key of a row to flash briefly, when opened from search. */
    data class Category(
        val category: FeatureCategory,
        val highlight: String? = null,
    ) : Destination

    data object Search : Destination

    data object Appearance : Destination

    data object About : Destination

    data object Licenses : Destination

    data object MoreModules : Destination

    data object FeatureHealth : Destination

    /** TEMPLATE demo of the optional components (ModuleConfig.DEMO_COMPONENTS). */
    data object Components : Destination

    fun toRoute(): String =
        when (this) {
            Home -> HOME
            is Category -> "$CATEGORY${category.name}" + (highlight?.let { "#$it" } ?: "")
            Search -> SEARCH
            Appearance -> APPEARANCE
            About -> ABOUT
            Licenses -> LICENSES
            MoreModules -> MORE_MODULES
            FeatureHealth -> FEATURE_HEALTH
            Components -> COMPONENTS
        }

    companion object {
        private const val HOME = "home"
        private const val CATEGORY = "category:"
        private const val APPEARANCE = "appearance"
        private const val ABOUT = "about"
        private const val LICENSES = "licenses"
        private const val SEARCH = "search"
        private const val MORE_MODULES = "more_modules"
        private const val FEATURE_HEALTH = "feature_health"
        private const val COMPONENTS = "components"

        fun fromRoute(route: String): Destination =
            when {
                route == APPEARANCE -> {
                    Appearance
                }

                route == ABOUT -> {
                    About
                }

                route == LICENSES -> {
                    Licenses
                }

                route == MORE_MODULES -> {
                    MoreModules
                }

                route == FEATURE_HEALTH -> {
                    FeatureHealth
                }

                route == COMPONENTS -> {
                    Components
                }

                route == SEARCH -> {
                    Search
                }

                route.startsWith(CATEGORY) -> {
                    val rest = route.removePrefix(CATEGORY)
                    FeatureCategory.entries
                        .firstOrNull { it.name == rest.substringBefore('#') }
                        ?.let { Category(it, rest.substringAfter('#', "").ifEmpty { null }) }
                        ?: Home
                }

                else -> {
                    Home
                }
            }
    }
}

private val BackStackSaver =
    listSaver<SnapshotStateList<Destination>, String>(
        save = { stack -> stack.map(Destination::toRoute) },
        restore = { routes -> routes.map(Destination::fromRoute).toMutableStateList() },
    )

private val ScreenSlide = 24.dp

private class ScreenTransitions(
    val forward: ContentTransform,
    val back: ContentTransform,
)

@Composable
private fun rememberScreenTransitions(): ScreenTransitions {
    if (isReducedMotion()) {
        val instant = EnterTransition.None togetherWith ExitTransition.None
        return ScreenTransitions(instant, instant)
    }
    val slide = with(LocalDensity.current) { ScreenSlide.roundToPx() }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    fun slideBy(sign: Int) =
        (slideInHorizontally(spatial) { sign * direction * slide } + fadeIn(effects)) togetherWith
            (slideOutHorizontally(spatial) { -sign * direction * slide } + fadeOut(effects))
    return ScreenTransitions(forward = slideBy(1), back = slideBy(-1))
}

@Composable
fun SettingsApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = state as? UiState.Ready ?: return
    ModuleTheme(appearance = ready.appearance) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            SettingsNavigation(state = ready, viewModel = viewModel)
        }
    }
}

@Composable
private fun SettingsNavigation(
    state: UiState.Ready,
    viewModel: MainViewModel,
) {
    val backStack =
        rememberSaveable(saver = BackStackSaver) {
            mutableStateListOf<Destination>(Destination.Home)
        }

    fun popIfTop(destination: Destination) {
        if (backStack.size > 1 && backStack.last() == destination) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
    ApplyHaptics(state.phase)
    val troubleshooting by viewModel.troubleshooting.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val transitions = rememberScreenTransitions()
    var restartDismissed by rememberSaveable { mutableStateOf(false) }
    ApplySheetScaffold(
        content = applySheetContent(state, viewModel, restartDismissed),
        onApply = viewModel::apply,
        onDiscard = viewModel::discard,
        onRestartTarget = viewModel::restartTarget,
        onDismissRestart = { restartDismissed = true },
        onOpenTargetInfo = {
            activity?.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", BuildConfig.TARGET_PACKAGE, null),
                ),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            transitionSpec = { transitions.forward },
            popTransitionSpec = { transitions.back },
            predictivePopTransitionSpec = { transitions.back },
            entryProvider =
                entryProvider {
                    entry<Destination.Home> {
                        HomeScreen(
                            state = state,
                            onOpenCategory = { backStack.add(Destination.Category(it)) },
                            onOpenAppearance = { backStack.add(Destination.Appearance) },
                            onOpenAbout = { backStack.add(Destination.About) },
                            onModuleEnabledChange = viewModel::setModuleEnabled,
                            onOpenHealth = { backStack.add(Destination.FeatureHealth) },
                            onOpenComponents = if (ModuleConfig.DEMO_COMPONENTS) ({ backStack.add(Destination.Components) }) else null,
                            onApply = viewModel::apply,
                            onOpenSearch = if (ModuleConfig.SEARCH) ({ backStack.add(Destination.Search) }) else null,
                        )
                    }
                    entry<Destination.Category> { key ->
                        CategoryScreen(
                            category = key.category,
                            settings = state.desired,
                            onSwitchChange = viewModel::setSwitch,
                            onChoiceChange = viewModel::setChoice,
                            failed = state.failedFeatures,
                            highlight = key.highlight,
                            onBack = { popIfTop(key) },
                        )
                    }
                    entry<Destination.About> {
                        AboutScreen(
                            onOpenLicenses = { backStack.add(Destination.Licenses) },
                            onOpenMoreModules = { backStack.add(Destination.MoreModules) },
                            troubleshooting = {
                                TroubleshootingGroup(
                                    state = state,
                                    troubleshooting = troubleshooting,
                                    onOpenHealth = { backStack.add(Destination.FeatureHealth) },
                                    onDebugLoggingChange = viewModel::setDebugLogging,
                                )
                            },
                            backup = {
                                if (ModuleConfig.BACKUP_RESTORE) {
                                    BackupRestoreGroup(
                                        state = state,
                                        onRestoreSettings = viewModel::restoreSettings,
                                        onRestoreAppearance = viewModel::restoreAppearance,
                                    )
                                }
                            },
                            onBack = { popIfTop(Destination.About) },
                        )
                    }
                    entry<Destination.Components> {
                        ComponentsDemoScreen(onBack = { popIfTop(Destination.Components) })
                    }
                    entry<Destination.Search> {
                        SearchScreen(
                            onOpenResult = { category, row ->
                                popIfTop(Destination.Search)
                                backStack.add(Destination.Category(category, row))
                            },
                            onBack = { popIfTop(Destination.Search) },
                        )
                    }
                    entry<Destination.FeatureHealth> {
                        FeatureHealthScreen(
                            state = state,
                            troubleshooting = troubleshooting,
                            onHealthAlertsChange = viewModel::setHealthAlerts,
                            onBack = { popIfTop(Destination.FeatureHealth) },
                        )
                    }
                    entry<Destination.MoreModules> {
                        MoreModulesScreen(onBack = { popIfTop(Destination.MoreModules) })
                    }
                    entry<Destination.Licenses> {
                        LicensesScreen(onBack = { popIfTop(Destination.Licenses) })
                    }
                    entry<Destination.Appearance> {
                        AppearanceScreen(
                            appearance = state.appearance,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColor,
                            onPureBlackChange = viewModel::setPureBlack,
                            onHomeLayoutChange = viewModel::setHomeLayout,
                            onBack = { popIfTop(Destination.Appearance) },
                        )
                    }
                },
        )
    }
}

@Composable
private fun applySheetContent(
    state: UiState.Ready,
    viewModel: MainViewModel,
    restartDismissed: Boolean,
): ApplySheetContent? =
    when {
        state.phase.isRunning -> {
            null
        }

        state.phase is ApplyPhase.Failed -> {
            ApplySheetContent.Failure(state.phase.reason)
        }

        state.pendingCount > 0 -> {
            ApplySheetContent.Pending(
                canApply = state.framework != null,
                restart = state.restartNeeded,
                changes = pendingChanges(state, viewModel),
            )
        }

        state.targetNeedsRestart && !restartDismissed -> {
            ApplySheetContent.RestartToLoad
        }

        else -> {
            null
        }
    }

@Composable
private fun pendingChanges(
    state: UiState.Ready,
    viewModel: MainViewModel,
): List<PendingChange> {
    val desired = state.desired
    val on = stringResource(R.string.pending_change_on)
    val off = stringResource(R.string.pending_change_off)
    val master =
        PendingChange(
            key = SettingsKeys.MODULE_ENABLED,
            titleRes = R.string.master_switch_title,
            areaRes = R.string.pending_area_home,
            value = if (desired.moduleEnabled) on else off,
            highlighted = desired.moduleEnabled,
            onRevert = viewModel::revertModuleEnabled,
        ).takeIf { state.moduleEnabledChanged }
    val switches =
        desired
            .differingSwitches(state.appliedOrDefault)
            .sorted()
            .map {
                val enabled = desired.isOn(it)
                PendingChange(
                    key = it.key,
                    titleRes = it.titleRes,
                    areaRes = it.category.titleRes,
                    value = if (enabled) on else off,
                    highlighted = enabled,
                    onRevert = { viewModel.revertSwitch(it) },
                )
            }
    val choice =
        PendingChange(
            key = SettingsKeys.CHOICE,
            titleRes = R.string.option_choice_title,
            areaRes = ChoiceCategory.titleRes,
            value = stringResource(desired.choice.labelRes),
            highlighted = true,
            onRevert = viewModel::revertChoice,
        ).takeIf { state.choiceChanged }
    return listOfNotNull(master) + switches + listOfNotNull(choice)
}

@Composable
private fun ApplyHaptics(phase: ApplyPhase) {
    val haptics = LocalHapticFeedback.current
    var previous by remember { mutableStateOf(phase) }
    LaunchedEffect(phase) {
        if (previous.isRunning && !phase.isRunning) {
            val failed = phase is ApplyPhase.Failed
            haptics.performHapticFeedback(
                if (failed) HapticFeedbackType.Reject else HapticFeedbackType.Confirm,
            )
        }
        previous = phase
    }
}
