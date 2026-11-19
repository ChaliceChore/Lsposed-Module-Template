plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
}

// Optional: set buildRoot in local.properties to put build output outside the repo, for example
// when the repo sits on a slow or synced drive.
val buildRoot =
    rootDir
        .resolve("local.properties")
        .takeIf { it.isFile }
        ?.let { f -> java.util.Properties().apply { f.reader().use { load(it) } } }
        ?.getProperty("buildRoot")
if (buildRoot != null) {
    subprojects {
        layout.buildDirectory.set(File(buildRoot, name))
    }
}
