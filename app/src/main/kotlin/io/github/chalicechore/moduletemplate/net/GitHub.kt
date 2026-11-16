package io.github.chalicechore.moduletemplate.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.chalicechore.moduletemplate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Topic that marks a repo as one of the author's LSPosed modules (see GITHUB_RELEASE.md). */
const val MODULE_TOPIC = "lsposed-module"

/** The repo owner from `module.repo`, e.g. "ChaliceChore". */
val RepoOwner: String = BuildConfig.REPO.substringBefore('/')

data class LatestRelease(
    val version: String,
    val url: String,
)

data class ModuleRepo(
    val name: String,
    val fullName: String,
    val description: String,
    val stars: Int,
    val url: String,
    val iconUrl: String,
)

/**
 * Small unauthenticated GitHub API client (60 requests an hour per IP). Callers cache results,
 * so normal use stays far below that.
 */
object GitHub {
    private const val API = "https://api.github.com"
    private const val TIMEOUT_MS = 10_000

    /** The repo's latest release, with any leading "v" removed from the tag. */
    suspend fun latestRelease(repo: String = BuildConfig.REPO): LatestRelease =
        withContext(Dispatchers.IO) {
            val json = JSONObject(get("$API/repos/$repo/releases/latest"))
            LatestRelease(
                version = json.getString("tag_name").removePrefix("v"),
                url = json.getString("html_url"),
            )
        }

    /** The owner's public, active, non-fork repos tagged [MODULE_TOPIC], except this module. */
    suspend fun moduleRepos(owner: String = RepoOwner): List<ModuleRepo> =
        withContext(Dispatchers.IO) { parseModuleRepos(get("$API/users/$owner/repos?per_page=100")) }

    fun parseModuleRepos(json: String): List<ModuleRepo> {
        val array = JSONArray(json)
        return (0 until array.length())
            .map(array::getJSONObject)
            .filter { repo ->
                !repo.optBoolean("fork") &&
                    !repo.optBoolean("archived") &&
                    !repo.optBoolean("private") &&
                    repo.optJSONArray("topics")?.let { t ->
                        (0 until t.length()).any { t.getString(it) == MODULE_TOPIC }
                    } == true &&
                    !repo.getString("full_name").equals(BuildConfig.REPO, ignoreCase = true)
            }.map { repo ->
                val fullName = repo.getString("full_name")
                val branch = repo.optString("default_branch", "main")
                ModuleRepo(
                    name = repo.getString("name"),
                    fullName = fullName,
                    description = repo.optString("description").takeUnless { it == "null" }.orEmpty(),
                    stars = repo.optInt("stargazers_count"),
                    url = repo.getString("html_url"),
                    iconUrl = "https://raw.githubusercontent.com/$fullName/$branch/icon.png",
                )
            }.sortedByDescending { it.stars }
    }

    /** Downloads a repo's icon.png into the cache folder; null if it has none. */
    suspend fun icon(
        context: Context,
        repo: ModuleRepo,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "module_icons/${repo.fullName.replace('/', '_')}.png")
            if (!file.exists()) {
                runCatching {
                    file.parentFile?.mkdirs()
                    open(repo.iconUrl).inputStream.use { input -> file.outputStream().use(input::copyTo) }
                }.onFailure { file.delete() }
            }
            if (file.exists()) BitmapFactory.decodeFile(file.path) else null
        }

    private fun get(url: String): String = open(url).inputStream.bufferedReader().use { it.readText() }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP $code for $url")
        }
        return connection
    }
}
