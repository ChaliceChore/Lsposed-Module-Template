package io.github.chalicechore.moduletemplate.prefs

import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.Flow

/** What one feature's hook did the last time the target loaded the module. */
sealed interface FeatureHealth {
    data object Ok : FeatureHealth

    data class Failed(
        val reason: String,
    ) : FeatureHealth
}

/**
 * The target's last report, sent by the hook through StatusProvider.
 *
 * @property loadedAt wall-clock time of the report, or null if the target never reported.
 * @property features only features whose hooks were attempted. A feature missing here was
 *   switched off (RESTART) when the target started, so it wasn't checked.
 */
data class HookStatus(
    val loadedAt: Long? = null,
    val targetVersion: String? = null,
    val features: Map<FeatureSwitch, FeatureHealth> = emptyMap(),
) {
    /** The target loaded the module since the phone booted, so the running target has it. */
    val loadedSinceBoot: Boolean
        get() = loadedAt != null && loadedAt >= System.currentTimeMillis() - SystemClock.elapsedRealtime()

    val failed: Map<FeatureSwitch, FeatureHealth.Failed>
        get() =
            features.entries
                .mapNotNull { (k, v) -> (v as? FeatureHealth.Failed)?.let { k to it } }
                .toMap()
}

/** Wire format shared by the hook (sender) and StatusProvider (receiver). */
object HookReport {
    const val METHOD_LOADED = "loaded"
    const val EXTRA_TARGET_VERSION = "target_version"
    const val EXTRA_OK = "ok"
    const val EXTRA_FAILED = "failed"
    const val EXTRA_REASONS = "reasons"

    fun authority(modulePackage: String) = "$modulePackage.status"
}

class HookStatusRepository(
    private val preferences: SharedPreferences,
) {
    fun status(): HookStatus {
        val loadedAt = preferences.getLong(LOADED_AT, 0L).takeIf { it > 0 }
        val ok = preferences.getStringSet(OK, emptySet()).orEmpty()
        val features =
            FeatureSwitch.entries
                .mapNotNull { switch ->
                    val reason = preferences.getString(FAILED_PREFIX + switch.key, null)
                    when {
                        reason != null -> switch to FeatureHealth.Failed(reason)
                        switch.key in ok -> switch to FeatureHealth.Ok
                        else -> null
                    }
                }.toMap()
        return HookStatus(loadedAt, preferences.getString(TARGET_VERSION, null), features)
    }

    fun observe(): Flow<HookStatus> = preferences.observe(::status)

    /** Replaces the previous report: each target start reports every feature again. */
    fun record(
        targetVersion: String?,
        ok: Collection<String>,
        failed: Map<String, String>,
    ) {
        val editor = preferences.edit().clear()
        editor.putLong(LOADED_AT, System.currentTimeMillis())
        editor.putString(TARGET_VERSION, targetVersion)
        editor.putStringSet(OK, ok.toSet())
        failed.forEach { (key, reason) -> editor.putString(FAILED_PREFIX + key, reason) }
        editor.apply()
    }

    companion object {
        const val FILE = "hook_status"
        private const val LOADED_AT = "loaded_at"
        private const val TARGET_VERSION = "target_version"
        private const val OK = "ok"
        private const val FAILED_PREFIX = "failed:"
    }
}
