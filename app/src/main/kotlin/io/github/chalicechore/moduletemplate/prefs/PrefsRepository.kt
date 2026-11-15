package io.github.chalicechore.moduletemplate.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Two local copies of the settings: what the user picked ([desired]) and what the hook last
 * received ([applied]). The difference drives the "changes not applied yet" sheet.
 */
class PrefsRepository(
    private val desired: SharedPreferences,
    private val applied: SharedPreferences,
) {
    fun desiredSettings(): FeatureSettings = SettingsKeys.read(desired.all)

    fun appliedSettings(): FeatureSettings? =
        if (applied.contains(SettingsKeys.CHOICE)) SettingsKeys.read(applied.all) else null

    fun observeDesired(): Flow<FeatureSettings> = desired.observe(::desiredSettings)

    fun observeApplied(): Flow<FeatureSettings?> = applied.observe(::appliedSettings)

    fun save(settings: FeatureSettings) {
        SettingsKeys.write(settings, desired.edit()).apply()
    }

    fun markApplied(settings: FeatureSettings) {
        SettingsKeys.write(settings, applied.edit()).apply()
    }

    companion object {
        const val DESIRED_FILE = SettingsKeys.GROUP
        const val APPLIED_FILE = "module_settings_applied"
    }
}

internal fun <T> SharedPreferences.observe(read: () -> T): Flow<T> =
    callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
