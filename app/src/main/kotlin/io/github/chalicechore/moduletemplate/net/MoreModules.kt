package io.github.chalicechore.moduletemplate.net

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The author's other modules for "More modules", from three sources, newest first:
 * 1. a cache of the last GitHub refresh, used while it is less than a day old;
 * 2. a fresh GitHub API call;
 * 3. the snapshot bundled in the APK (assets/more_modules.json, written by the
 *    `updateMoreModules` Gradle task before each release build), when offline.
 */
class MoreModules(
    private val context: Context,
) {
    private val cache = File(context.filesDir, CACHE_FILE)

    suspend fun load(forceRefresh: Boolean = false): List<ModuleRepo> =
        withContext(Dispatchers.IO) {
            val fresh = cache.exists() && System.currentTimeMillis() - cache.lastModified() < MAX_AGE_MS
            if (fresh && !forceRefresh) parsedOrNull(cache.readText())?.let { return@withContext it }
            try {
                GitHub.moduleRepos().also { repos ->
                    // Store in the API's own shape so the same parser reads all three sources.
                    cache.writeText(toJson(repos))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                (if (cache.exists()) parsedOrNull(cache.readText()) else null) ?: snapshot()
            }
        }

    private fun snapshot(): List<ModuleRepo> =
        runCatching {
            context.assets.open(SNAPSHOT_ASSET).bufferedReader().use { parsedOrNull(it.readText()) }
        }.getOrNull().orEmpty()

    private fun parsedOrNull(json: String): List<ModuleRepo>? = runCatching { GitHub.parseModuleRepos(json) }.getOrNull()

    private fun toJson(repos: List<ModuleRepo>): String =
        org.json
            .JSONArray(
                repos.map { repo ->
                    org.json.JSONObject().apply {
                        put("name", repo.name)
                        put("full_name", repo.fullName)
                        put("description", repo.description)
                        put("stargazers_count", repo.stars)
                        put("html_url", repo.url)
                        put("default_branch", repo.iconUrl.substringAfter("${repo.fullName}/").substringBefore('/'))
                        put("topics", org.json.JSONArray(listOf(MODULE_TOPIC)))
                    }
                },
            ).toString()

    private companion object {
        const val CACHE_FILE = "more_modules_cache.json"
        const val SNAPSHOT_ASSET = "more_modules.json"
        const val MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
