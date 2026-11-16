package io.github.chalicechore.moduletemplate

import android.app.Application
import io.github.chalicechore.moduletemplate.prefs.AppearanceRepository
import io.github.chalicechore.moduletemplate.prefs.FrameworkConnection
import io.github.chalicechore.moduletemplate.prefs.HookStatusRepository
import io.github.chalicechore.moduletemplate.prefs.PrefsRepository
import io.github.chalicechore.moduletemplate.prefs.RootTargetRestarter
import io.github.chalicechore.moduletemplate.prefs.TargetRestarter
import io.github.libxposed.service.XposedServiceHelper

class ModuleApp : Application() {
    val frameworkConnection = FrameworkConnection()

    // TEMPLATE: swap for a broadcast-based restarter if the hook can restart the target itself.
    val targetRestarter: TargetRestarter by lazy { RootTargetRestarter(BuildConfig.TARGET_PACKAGE) }

    val prefsRepository: PrefsRepository by lazy {
        PrefsRepository(
            desired = getSharedPreferences(PrefsRepository.DESIRED_FILE, MODE_PRIVATE),
            applied = getSharedPreferences(PrefsRepository.APPLIED_FILE, MODE_PRIVATE),
        )
    }

    val hookStatusRepository: HookStatusRepository by lazy {
        HookStatusRepository(getSharedPreferences(HookStatusRepository.FILE, MODE_PRIVATE))
    }

    /** Called by StatusProvider with the features whose hooks failed on this target start. */
    fun onHookReport(failed: Map<String, String>) {
        // Failure alerts (Feature health) hook in here.
    }

    val appearanceRepository: AppearanceRepository by lazy {
        AppearanceRepository(getSharedPreferences(AppearanceRepository.FILE, MODE_PRIVATE))
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(frameworkConnection)
    }
}
