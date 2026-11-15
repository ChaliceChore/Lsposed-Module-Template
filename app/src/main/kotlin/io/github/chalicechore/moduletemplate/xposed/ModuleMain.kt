package io.github.chalicechore.moduletemplate.xposed

import android.util.Log
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * Xposed entry point, listed in META-INF/xposed/java_init.list.
 *
 * TEMPLATE: install the module's hooks in [onPackageReady]. Settings written by the app arrive
 * through remote preferences ([SettingsKeys.GROUP]); read them once per process start.
 */
class ModuleMain : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "loaded in ${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != BuildConfig.TARGET_PACKAGE || !param.isFirstPackage) return
        val settings = SettingsKeys.read(getRemotePreferences(SettingsKeys.GROUP).all)
        log(
            Log.INFO,
            TAG,
            "ready in ${param.packageName}; enabled=${settings.enabled.map(FeatureSwitch::key)}",
        )
        // TEMPLATE: hook(...) calls go here, each guarded by try/catch.
    }

    companion object {
        const val TAG = "ModuleTemplate"
    }
}
