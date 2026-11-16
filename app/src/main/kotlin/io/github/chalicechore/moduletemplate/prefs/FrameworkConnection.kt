package io.github.chalicechore.moduletemplate.prefs

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class FrameworkInfo(
    val name: String,
    val version: String,
    val apiVersion: Int,
) {
    val versionName: String
        get() = version.trim().substringBefore(' ')
}

/** Tracks the libxposed service binding and writes settings to remote preferences. */
class FrameworkConnection : XposedServiceHelper.OnServiceListener {
    private class Session(
        val service: XposedService,
        val openRemoteEditor: () -> SharedPreferences.Editor,
    )

    private val session = MutableStateFlow<Session?>(null)
    private val connectedFramework = MutableStateFlow<FrameworkInfo?>(null)
    private val publishLock = Mutex()
    private val connectionLock = Any()

    val framework: StateFlow<FrameworkInfo?> = connectedFramework.asStateFlow()

    override fun onServiceBind(service: XposedService) {
        val info =
            try {
                FrameworkInfo(service.frameworkName, service.frameworkVersion, service.apiVersion)
            } catch (exception: RuntimeException) {
                Log.w(TAG, "framework info read failed", exception)
                null
            }
        synchronized(connectionLock) {
            session.value =
                Session(service) { service.getRemotePreferences(SettingsKeys.GROUP).edit() }
            connectedFramework.value = info
        }
    }

    override fun onServiceDied(service: XposedService) {
        synchronized(connectionLock) {
            if (session.value?.service !== service) return
            session.value = null
            connectedFramework.value = null
        }
    }

    /** Writes every setting, for Apply. */
    suspend fun publish(settings: FeatureSettings): Boolean =
        publish { SettingsKeys.write(settings, it) }

    /**
     * Writes only what [write] puts, for LIVE settings: other settings may still be pending and
     * must not reach the hook before the user applies them.
     */
    suspend fun publish(write: (SharedPreferences.Editor) -> Unit): Boolean =
        publishLock.withLock {
            withContext(Dispatchers.IO) {
                val current = session.value ?: return@withContext false
                try {
                    current.openRemoteEditor().also(write).commit()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: RuntimeException) {
                    Log.w(TAG, "remote preferences commit failed", exception)
                    false
                }
            }
        }

    companion object {
        private const val TAG = "ModuleTemplate"
    }
}
