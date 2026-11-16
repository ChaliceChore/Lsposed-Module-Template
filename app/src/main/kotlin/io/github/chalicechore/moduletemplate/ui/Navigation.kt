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
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.FeatureCategory
import io.github.chalicechore.moduletemplate.ui.component.ApplySheetContent
import io.github.chalicechore.moduletemplate.ui.component.ApplySheetScaffold
import io.github.chalicechore.moduletemplate.ui.component.PendingChange
import io.github.chalicechore.moduletemplate.ui.screen.AboutScreen
import io.github.chalicechore.moduletemplate.ui.screen.AppearanceScreen
import io.github.chalicechore.moduletemplate.ui.screen.CategoryScreen
import io.github.chalicechore.moduletemplate.ui.screen.HomeScreen
import io.github.chalicechore.moduletemplate.ui.screen.LicensesScreen
import io.github.chalicechore.moduletemplate.ui.theme.ModuleTheme
import io.github.chalicechore.moduletemplate.ui.theme.isReducedMotion

/** Screens. Routes are strings so the back stack survives process death. */
sealed interface Destination {
    data object Home : Destination

    data class Category(
        val category: FeatureCategory,
    ) : Destination

    data object Appearance : Destination

    data object About : Destination

    data object Licenses : Destination

    fun toRoute(): String =
        when (this) {
            Home -> HOME
            is Category -> "$CATEGORY${category.name}"
            Appearance -> APPEARANCE
            About -> ABOUT
            Licenses -> LICENSES
        }

    companion object {
        private const val HOME = "home"
        private const val CATEGORY = "category:"
        private const val APPEARANCE = "appearance"
        private const val ABOUT = "about"
        private const val LICENSES = "licenses"

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

                route.startsWith(CATEGORY) -> {
                    FeatureCategory.entries
                        .firstOrNull { it.name == route.removePrefix(CATEGORY) }
                        ?.let(::Category)
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
    val activity = LocalActivity.current
    val transitions = rememberScreenTransitions()
    ApplySheetScaffold(
        content = applySheetContent(state),
        onApply = viewModel::apply,
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
                            onOpenHealth = { backStack.add(Destination.About) },
                            onApply = viewModel::apply,
                        )
                    }
                    entry<Destination.Category> { key ->
                        CategoryScreen(
                            category = key.category,
                            settings = state.desired,
                            onSwitchChange = viewModel::setSwitch,
                            onChoiceChange = viewModel::setChoice,
                            onBack = { popIfTop(key) },
                        )
                    }
                    entry<Destination.About> {
                        AboutScreen(
                            onOpenLicenses = { backStack.add(Destination.Licenses) },
                            onBack = { popIfTop(Destination.About) },
                        )
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
                            onBack = { popIfTop(Destination.Appearance) },
                        )
                    }
                },
        )
    }
}

@Composable
private fun applySheetContent(state: UiState.Ready): ApplySheetContent? =
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
                changes = pendingChanges(state),
            )
        }

        else -> {
            null
        }
    }

@Composable
private fun pendingChanges(state: UiState.Ready): List<PendingChange> {
    val desired = state.desired
    val on = stringResource(R.string.pending_change_on)
    val off = stringResource(R.string.pending_change_off)
    val switches =
        desired
            .differingSwitches(state.appliedOrDefault)
            .sorted()
            .map {
                val enabled = desired.isOn(it)
                PendingChange(it.titleRes, it.category.titleRes, if (enabled) on else off, enabled)
            }
    val choice =
        PendingChange(
            titleRes = R.string.option_choice_title,
            areaRes = ChoiceCategory.titleRes,
            value = stringResource(desired.choice.labelRes),
            highlighted = true,
        ).takeIf { state.choiceChanged }
    val master =
        PendingChange(
            titleRes = R.string.master_switch_title,
            areaRes = R.string.pending_area_home,
            value = if (desired.moduleEnabled) on else off,
            highlighted = desired.moduleEnabled,
        ).takeIf { state.moduleEnabledChanged }
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
