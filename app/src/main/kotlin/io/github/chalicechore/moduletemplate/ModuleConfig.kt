package io.github.chalicechore.moduletemplate

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import io.github.chalicechore.moduletemplate.ui.tour.TourScroll
import io.github.chalicechore.moduletemplate.ui.tour.TourStep
import io.github.chalicechore.moduletemplate.ui.tour.TourTarget

/** The Home screen's Features section: one row per category, or a two-column card grid. */
enum class HomeLayout {
    LIST,
    GRID,
}

/**
 * The "Try it" button on Home: opens the target screen the module changes.
 *
 * @property labelRes button text. The default is "Try it".
 * @property intent builds the intent to start. Return null if there's nothing to open.
 */
class TryItAction(
    @param:StringRes val labelRes: Int = R.string.try_it,
    val intent: (Context) -> Intent?,
)

/**
 * TEMPLATE: per-module switches for the template's optional parts. The template turns everything
 * on so every part can be tested; most modules turn some of them off.
 *
 * Text-only settings that belong in gradle.properties instead: `module.translate` (the Weblate
 * URL for "Help translate"; empty hides the row).
 */
object ModuleConfig {
    /** Search field on Home. Worth it only for modules with many settings. */
    const val SEARCH = true

    /**
     * The "Always on" section on Home: read-only rows for what the module always does. Turn it
     * off for modules where everything is a setting.
     */
    const val ALWAYS_ON = true

    /** "Back up and restore" in About. */
    const val BACKUP_RESTORE = true

    /** "Reset to defaults" in each category's menu, and "Reset all settings" in About. */
    const val RESET = true

    /**
     * The "Components" demo category that shows every optional component. Delete it (and
     * FeatureCategory.COMPONENTS) in a real module.
     */
    const val DEMO_COMPONENTS = true

    /**
     * The first-launch tour of Home, shown once and again from Help. Empty turns it off.
     * Steps for optional parts are left out when those parts are off.
     */
    val tour: List<TourStep> by lazy {
        buildList {
            add(TourStep(null, R.string.tour_welcome_title, R.string.tour_welcome_body))
            add(
                TourStep(
                    TourTarget.STATUS,
                    R.string.tour_status_title,
                    R.string.tour_status_body,
                    bodyInactiveRes = R.string.tour_status_body_inactive,
                    scroll = TourScroll.TOP,
                ),
            )
            add(TourStep(TourTarget.FEATURES, R.string.tour_features_title, R.string.tour_features_body, scroll = TourScroll.TOP))
            if (SEARCH) add(TourStep(TourTarget.SEARCH, R.string.tour_search_title, R.string.tour_search_body))
            if (tryIt != null) add(TourStep(TourTarget.TRY_IT, R.string.tour_try_title, R.string.tour_try_body))
            add(TourStep(TourTarget.APP, R.string.tour_app_title, R.string.tour_app_body, scroll = TourScroll.BOTTOM))
            add(TourStep(TourTarget.SHEET, R.string.tour_sheet_title, R.string.tour_sheet_body))
            add(TourStep(null, R.string.tour_done_title, R.string.tour_done_body))
        }
    }

    /** Home layout used until the user picks one in Appearance. */
    val defaultHomeLayout = HomeLayout.LIST

    /**
     * "Try it" button, or null for modules with no direct way to try them.
     * For a specific screen, build that intent here (DualDial: Intent.ACTION_DIAL to the Dialer).
     */
    val tryIt: TryItAction? =
        TryItAction { context ->
            context.packageManager.getLaunchIntentForPackage(BuildConfig.TARGET_PACKAGE)
        }
}
