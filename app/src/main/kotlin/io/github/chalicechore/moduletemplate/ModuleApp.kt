package io.github.chalicechore.moduletemplate

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.TroubleshootingRepository
import io.github.chalicechore.moduletemplate.ui.titleRes
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

    val troubleshootingRepository: TroubleshootingRepository by lazy {
        TroubleshootingRepository(getSharedPreferences(TroubleshootingRepository.FILE, MODE_PRIVATE))
    }

    /**
     * Called by StatusProvider with the features whose hooks failed on this target start.
     * With "Alert when a feature breaks" on, posts one notification naming them.
     */
    fun onHookReport(failed: Map<String, String>) {
        if (failed.isEmpty() || !troubleshootingRepository.current().healthAlerts) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                HEALTH_CHANNEL,
                getString(R.string.health_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val names =
            failed.keys.mapNotNull { key -> FeatureSwitch.entries.firstOrNull { it.key == key } }
                .joinToString(", ") { getString(it.titleRes) }
        val target = getString(R.string.target_label)
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification
                .Builder(this, HEALTH_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(resources.getQuantityString(R.plurals.health_alert_title, failed.size, failed.size))
                .setContentText(getString(R.string.health_alert_body, names, target))
                .setStyle(Notification.BigTextStyle().bigText(getString(R.string.health_alert_body, names, target)))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        manager.notify(HEALTH_NOTIFICATION, notification)
    }

    val appearanceRepository: AppearanceRepository by lazy {
        AppearanceRepository(getSharedPreferences(AppearanceRepository.FILE, MODE_PRIVATE))
    }

    private companion object {
        const val HEALTH_CHANNEL = "feature_health"
        const val HEALTH_NOTIFICATION = 1
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(frameworkConnection)
    }
}
