package io.github.chalicechore.moduletemplate.prefs

import android.content.SharedPreferences

// TEMPLATE: replace the placeholder categories, switches and choice with the module's own.
// Shared by the settings app and the hook (ModuleMain), so keep it free of Android UI types.

enum class FeatureCategory {
    GENERAL,
    INTERFACE,
    ADVANCED,
}

enum class FeatureSwitch(
    val key: String,
    val category: FeatureCategory,
    val defaultOn: Boolean = false,
) {
    FEATURE_ONE("feature_one", FeatureCategory.GENERAL, defaultOn = true),
    FEATURE_TWO("feature_two", FeatureCategory.GENERAL),
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
}

data class FeatureSettings(
    val enabled: Set<FeatureSwitch> = FeatureSwitch.entries.filter { it.defaultOn }.toSet(),
    val choice: Choice = Choice.A,
) {
    fun isOn(switch: FeatureSwitch): Boolean = switch in enabled

    fun with(
        switch: FeatureSwitch,
        on: Boolean,
    ): FeatureSettings = copy(enabled = if (on) enabled + switch else enabled - switch)

    fun differingSwitches(other: FeatureSettings): Set<FeatureSwitch> =
        (enabled - other.enabled) + (other.enabled - enabled)

    fun countDifferences(other: FeatureSettings): Int =
        differingSwitches(other).size + if (choice != other.choice) 1 else 0
}

object SettingsKeys {
    /** Remote preferences group the hook reads, and the app's local file name. */
    const val GROUP = "module_settings"
    const val CHOICE = "choice"

    fun read(values: Map<String, *>): FeatureSettings =
        FeatureSettings(
            enabled =
                FeatureSwitch.entries
                    .filter { (values[it.key] as? Boolean) ?: it.defaultOn }
                    .toSet(),
            choice = Choice.entries.firstOrNull { it.name == values[CHOICE] } ?: Choice.A,
        )

    fun write(
        settings: FeatureSettings,
        editor: SharedPreferences.Editor,
    ): SharedPreferences.Editor {
        FeatureSwitch.entries.forEach { editor.putBoolean(it.key, settings.isOn(it)) }
        editor.putString(CHOICE, settings.choice.name)
        return editor
    }
}
