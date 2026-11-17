package io.github.chalicechore.moduletemplate.prefs

import android.content.SharedPreferences

// TEMPLATE: replace the placeholder categories, switches and choice with the module's own.
// Shared by the settings app and the hook (ModuleMain), so keep it free of Android UI types.

/**
 * How a setting reaches the target app once applied. Every setting must declare one.
 *
 * - [RESTART]: the hook reads it when the target starts, so applying it restarts the target.
 *   Use this for anything that decides which hooks get installed.
 * - [LIVE]: the hook reads it again whenever it changes (see ModuleMain), so it applies at once
 *   with no restart and no apply sheet. Only tag a setting LIVE when the hook checks it on every
 *   call; test that turning it off really stops the behaviour in a running target.
 */
enum class Applies {
    RESTART,
    LIVE,
}

enum class FeatureCategory {
    GENERAL,
    INTERFACE,
    ADVANCED,
}

enum class FeatureSwitch(
    val key: String,
    val category: FeatureCategory,
    val defaultOn: Boolean = false,
    val applies: Applies = Applies.RESTART,
) {
    FEATURE_ONE("feature_one", FeatureCategory.GENERAL, defaultOn = true),

    // Placeholder LIVE setting, so the template shows both behaviours.
    FEATURE_TWO("feature_two", FeatureCategory.GENERAL, applies = Applies.LIVE),
    FEATURE_THREE("feature_three", FeatureCategory.INTERFACE),
    FEATURE_FOUR("feature_four", FeatureCategory.ADVANCED),
    FEATURE_FIVE("feature_five", FeatureCategory.ADVANCED),
    FEATURE_SIX("feature_six", FeatureCategory.ADVANCED),
}

/** A single-choice option, shown as a radio dialog in its category. */
enum class Choice {
    A,
    B,
    C,
    ;

    companion object {
        // TEMPLATE: how the choice applies.
        val applies = Applies.RESTART
    }
}

data class FeatureSettings(
    /** The master toggle: off means the hook installs nothing. Always applies with a restart. */
    val moduleEnabled: Boolean = true,
    val switches: Set<FeatureSwitch> = FeatureSwitch.entries.filter { it.defaultOn }.toSet(),
    val choice: Choice = Choice.A,
) {
    fun isOn(switch: FeatureSwitch): Boolean = switch in switches

    fun with(
        switch: FeatureSwitch,
        on: Boolean,
    ): FeatureSettings = copy(switches = if (on) switches + switch else switches - switch)

    fun differingSwitches(other: FeatureSettings): Set<FeatureSwitch> =
        (switches - other.switches) + (other.switches - switches)

    fun countDifferences(other: FeatureSettings): Int =
        differingSwitches(other).size +
            (if (choice != other.choice) 1 else 0) +
            (if (moduleEnabled != other.moduleEnabled) 1 else 0)

    /**
     * These settings with every switch of [category] back at its default, and the choice too
     * when [includeChoice] (the choice's category). The module switch is left alone.
     */
    fun resetCategory(
        category: FeatureCategory,
        includeChoice: Boolean,
    ): FeatureSettings {
        val defaults = FeatureSettings()
        val inCategory = FeatureSwitch.entries.filter { it.category == category }.toSet()
        return copy(
            switches = (switches - inCategory) + (defaults.switches intersect inCategory),
            choice = if (includeChoice) defaults.choice else choice,
        )
    }

    /** True when applying the difference from [other] needs the target restarted. */
    fun needsRestart(other: FeatureSettings): Boolean =
        moduleEnabled != other.moduleEnabled ||
            (choice != other.choice && Choice.applies == Applies.RESTART) ||
            differingSwitches(other).any { it.applies == Applies.RESTART }
}

object SettingsKeys {
    /** Remote preferences group the hook reads, and the app's local file name. */
    const val GROUP = "module_settings"
    const val MODULE_ENABLED = "module_enabled"
    const val CHOICE = "choice"

    /** Time (ms) until which the hook logs verbosely. Written live, not through the apply sheet. */
    const val DEBUG_UNTIL = "debug_until"

    fun read(values: Map<String, *>): FeatureSettings =
        FeatureSettings(
            moduleEnabled = (values[MODULE_ENABLED] as? Boolean) ?: true,
            switches =
                FeatureSwitch.entries
                    .filter { (values[it.key] as? Boolean) ?: it.defaultOn }
                    .toSet(),
            choice = Choice.entries.firstOrNull { it.name == values[CHOICE] } ?: Choice.A,
        )

    fun write(
        settings: FeatureSettings,
        editor: SharedPreferences.Editor,
    ): SharedPreferences.Editor {
        editor.putBoolean(MODULE_ENABLED, settings.moduleEnabled)
        FeatureSwitch.entries.forEach { editor.putBoolean(it.key, settings.isOn(it)) }
        editor.putString(CHOICE, settings.choice.name)
        return editor
    }
}
