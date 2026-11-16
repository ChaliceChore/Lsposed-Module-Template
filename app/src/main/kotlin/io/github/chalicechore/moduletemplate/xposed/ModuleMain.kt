package io.github.chalicechore.moduletemplate.xposed

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.prefs.Applies
import io.github.chalicechore.moduletemplate.prefs.FeatureSettings
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.HookReport
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * Xposed entry point, listed in META-INF/xposed/java_init.list.
 *
 * Flow on every target start:
 * 1. Read the settings from remote preferences. If the master toggle is off, install nothing.
 * 2. Install each feature with [installFeature], which records whether its hook went in.
 * 3. Report the results to the app (StatusProvider), for the status card and Feature health.
 *
 * LIVE settings: [settings] is re-read whenever the app changes them, so hooks that check
 * `settings.isOn(...)` on each call follow LIVE switches without a restart.
 */
class ModuleMain : XposedModule() {
    @Volatile
    var settings = FeatureSettings()
        private set

    @Volatile
    private var debugUntil = 0L

    private val ok = mutableListOf<String>()
    private val failed = linkedMapOf<String, String>()

    // Held in a field: SharedPreferences keeps listeners weakly.
    private var liveListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "loaded in ${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != BuildConfig.TARGET_PACKAGE || !param.isFirstPackage) return
        val prefs = getRemotePreferences(SettingsKeys.GROUP)
        readSettings(prefs)
        if (!settings.moduleEnabled) {
            log(Log.INFO, TAG, "turned off in the app; leaving ${param.packageName} unchanged")
            reportToApp()
            return
        }
        liveListener =
            SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> readSettings(p) }
                .also(prefs::registerOnSharedPreferenceChangeListener)
        debug("settings: ${settings.switches.map(FeatureSwitch::key)}, choice=${settings.choice}")

        // TEMPLATE: one installFeature block per feature. Put the hook(...) calls inside it.
        FeatureSwitch.entries.forEach { feature ->
            installFeature(feature) {
                // Placeholder: nothing to hook in the template.
            }
        }
        reportToApp()
    }

    /**
     * Installs one feature's hooks and records the result for Feature health.
     *
     * RESTART features that are switched off are skipped: nothing is installed or checked.
     * LIVE features are always installed, because the user can turn them on without a restart;
     * their hooks must check `settings.isOn(feature)` on every call.
     */
    private fun installFeature(
        feature: FeatureSwitch,
        install: () -> Unit,
    ) {
        if (feature.applies == Applies.RESTART && !settings.isOn(feature)) return
        try {
            install()
            ok += feature.key
            debug("installed ${feature.key}")
        } catch (t: Throwable) {
            failed[feature.key] = "${t.javaClass.simpleName}: ${t.message.orEmpty()}".take(MAX_REASON)
            log(Log.WARN, TAG, "${feature.key} failed to install", t)
        }
    }

    private fun readSettings(prefs: SharedPreferences) {
        val values = prefs.all
        settings = SettingsKeys.read(values)
        debugUntil = (values[SettingsKeys.DEBUG_UNTIL] as? Long) ?: 0L
    }

    /** Verbose logging, only while the app's "Debug logging" switch is on (24 hours at most). */
    fun debug(message: String) {
        if (System.currentTimeMillis() < debugUntil) log(Log.DEBUG, TAG, message)
    }

    /**
     * Sends the results once the target's Application exists. onPackageReady runs while the
     * target is still binding its application, so the call is posted to its main thread.
     */
    private fun reportToApp() {
        Handler(Looper.getMainLooper()).post {
            val app = currentApplication() ?: return@post
            val extras =
                Bundle().apply {
                    putString(HookReport.EXTRA_TARGET_VERSION, targetVersion(app))
                    putStringArray(HookReport.EXTRA_OK, ok.toTypedArray())
                    putStringArray(HookReport.EXTRA_FAILED, failed.keys.toTypedArray())
                    putStringArray(HookReport.EXTRA_REASONS, failed.values.toTypedArray())
                }
            try {
                app.contentResolver.call(
                    Uri.parse("content://" + HookReport.authority(BuildConfig.APPLICATION_ID)),
                    HookReport.METHOD_LOADED,
                    null,
                    extras,
                )
            } catch (t: Throwable) {
                // The app may be missing or stopped; the status card then says "not loaded".
                log(Log.WARN, TAG, "status report failed", t)
            }
        }
    }

    private fun targetVersion(app: Application): String? =
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull()

    private fun currentApplication(): Application? =
        runCatching {
            Class
                .forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as Application?
        }.getOrNull()

    companion object {
        const val TAG = "ModuleTemplate"
        private const val MAX_REASON = 300
    }
}
