package io.github.chalicechore.moduletemplate.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

/**
 * @property healthAlerts notify when a feature's hook fails to install ("Alert when a feature
 *   breaks"). Only kept on while notification permission is granted.
 * @property debugUntil time (ms) until which the hook logs verbosely; 0 when off.
 */
data class Troubleshooting(
    val healthAlerts: Boolean = false,
    val debugUntil: Long = 0L,
) {
    val debugOn: Boolean get() = debugUntil > System.currentTimeMillis()
}

class TroubleshootingRepository(
    private val preferences: SharedPreferences,
) {
    fun current(): Troubleshooting =
        Troubleshooting(
            healthAlerts = preferences.getBoolean(HEALTH_ALERTS, false),
            debugUntil = preferences.getLong(DEBUG_UNTIL, 0L),
        )

    fun observe(): Flow<Troubleshooting> = preferences.observe(::current)

    fun setHealthAlerts(on: Boolean) {
        preferences.edit().putBoolean(HEALTH_ALERTS, on).apply()
    }

    fun setDebugUntil(time: Long) {
        preferences.edit().putLong(DEBUG_UNTIL, time).apply()
    }

    companion object {
        const val FILE = "troubleshooting"
        private const val HEALTH_ALERTS = "health_alerts"
        private const val DEBUG_UNTIL = "debug_until"

        /** How long debug logging stays on if the user leaves it on. */
        const val DEBUG_HOURS = 24
    }
}
