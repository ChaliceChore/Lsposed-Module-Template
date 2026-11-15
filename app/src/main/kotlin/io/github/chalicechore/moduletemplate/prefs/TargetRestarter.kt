package io.github.chalicechore.moduletemplate.prefs

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Restarts the hooked app so it picks up new settings. */
fun interface TargetRestarter {
    suspend fun restart(): Boolean
}

/**
 * TEMPLATE: force-stops the target with root. Replace or drop it per module: some hooks
 * re-read settings live and need no restart, others can restart themselves via a broadcast.
 */
class RootTargetRestarter(
    private val targetPackage: String,
) : TargetRestarter {
    override suspend fun restart(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val process =
                    ProcessBuilder("su", "-c", "am force-stop $targetPackage")
                        .redirectErrorStream(true)
                        .start()
                process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0
            } catch (exception: IOException) {
                Log.w(TAG, "root restart failed", exception)
                false
            }
        }

    companion object {
        private const val TAG = "ModuleTemplate"
        private const val TIMEOUT_SECONDS = 10L
    }
}
