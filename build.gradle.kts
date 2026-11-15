plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
}

// Build output goes outside /storage/emulated (FUSE is slow and sync tools lock files).
// Set buildRoot in local.properties.
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
