package io.github.chalicechore.moduletemplate

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.util.Log
import io.github.chalicechore.moduletemplate.prefs.HookReport

/**
 * Receives "the target loaded the module" and each feature's hook result from inside the target
 * process, which this app can't observe on its own. Pattern from DualDial and App Info Tweaks.
 *
 * Exported because the caller is another package. Only the target app's own uid is believed.
 */
class StatusProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle? {
        if (method != HookReport.METHOD_LOADED || extras == null) return null
        val app = context?.applicationContext as? ModuleApp ?: return null
        if (!fromTarget(app.packageManager)) {
            Log.w(TAG, "ignored a status report from uid ${Binder.getCallingUid()}")
            return null
        }
        val ok = extras.getStringArray(HookReport.EXTRA_OK).orEmpty().toList()
        val failedKeys = extras.getStringArray(HookReport.EXTRA_FAILED).orEmpty()
        val reasons = extras.getStringArray(HookReport.EXTRA_REASONS).orEmpty()
        val failed = failedKeys.mapIndexed { i, key -> key to reasons.getOrElse(i) { "" } }.toMap()
        app.hookStatusRepository.record(extras.getString(HookReport.EXTRA_TARGET_VERSION), ok, failed)
        app.onHookReport(failed)
        return Bundle.EMPTY
    }

    private fun fromTarget(pm: PackageManager): Boolean =
        runCatching { pm.getPackageUid(BuildConfig.TARGET_PACKAGE, 0) == Binder.getCallingUid() }
            .getOrDefault(false)

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        const val TAG = "ModuleTemplate"
    }
}
