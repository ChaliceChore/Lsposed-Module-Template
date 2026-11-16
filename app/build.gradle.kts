import java.net.HttpURLConnection
import java.net.URI

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries.android)
}

// Module identity comes from gradle.properties, so a new module only edits that file.
fun prop(name: String): String = providers.gradleProperty(name).get()

val targetPackages = prop("target.package").split(',').map(String::trim).filter(String::isNotEmpty)

// Release key: from environment variables (the release workflow), or from a properties file
// named by releaseSigning in local.properties (storeFile, storePassword, keyAlias, keyPassword).
// Without either, release builds use the debug key, which is fine for test builds.
val releaseSigning: Map<String, String>? =
    System.getenv("SIGNING_STORE_FILE")?.let { store ->
        mapOf(
            "storeFile" to store,
            "storePassword" to System.getenv("SIGNING_STORE_PASSWORD").orEmpty(),
            "keyAlias" to System.getenv("SIGNING_KEY_ALIAS").orEmpty(),
            "keyPassword" to System.getenv("SIGNING_KEY_PASSWORD").orEmpty(),
        )
    } ?: rootProject
        .file("local.properties")
        .takeIf { it.isFile }
        ?.let { f -> Properties().apply { f.reader().use { load(it) } } }
        ?.getProperty("releaseSigning")
        ?.let { path ->
            val p = Properties().apply { File(path).reader().use { load(it) } }
            p.stringPropertyNames().associateWith { p.getProperty(it) }
        }

android {
    namespace = "io.github.chalicechore.moduletemplate"
    compileSdk = 37

    defaultConfig {
        applicationId = prop("module.id")
        minSdk = 31
        targetSdk = 37
        versionCode = prop("version.code").toInt()
        versionName = prop("version.name")

        buildConfigField("String", "TARGET_PACKAGE", "\"${targetPackages.first()}\"")
        buildConfigField("String", "REPO", "\"${prop("module.repo")}\"")
        buildConfigField("String", "LICENSE", "\"${prop("module.license")}\"")
        buildConfigField("String", "TRANSLATE_URL", "\"${providers.gradleProperty("module.translate").orNull.orEmpty()}\"")
        resValue("string", "app_name", prop("module.name"))
        resValue("string", "app_description", prop("module.description"))
        resValue("string", "target_label", prop("target.label"))
    }

    androidResources {
        // TEMPLATE: the app's languages (keep in sync with ui/Languages.kt). Filters out the other
        // languages bundled by libraries, and lists these in the generated locale config.
        localeFilters += listOf("en", "es", "pt-rBR", "fr", "de", "ru", "zh-rCN", "zh-rTW", "ja", "in", "tr", "vi", "ar")
        generateLocaleConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Shrunk: Compose and the Kotlin stdlib are most of the APK otherwise.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig =
                signingConfigs.getByName(if (releaseSigning != null) "release" else "debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            merges += "META-INF/xposed/**"
            excludes +=
                listOf(
                    "META-INF/AL2.0",
                    "META-INF/LGPL2.1",
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/DEPENDENCIES",
                    "META-INF/*.version",
                    "META-INF/*.kotlin_module",
                    "kotlin/**",
                    "DebugProbesKt.bin",
                )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Provided at runtime by the Xposed framework.
    compileOnly(libs.libxposed.api)
    // Talks to the framework from the settings app (status, remote preferences).
    implementation(libs.libxposed.service)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.preferences)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.material.color.utilities)
    debugImplementation(libs.compose.ui.tooling)
}

// Writes META-INF/xposed/module.prop and scope.list from gradle.properties.
abstract class GenerateXposedModuleProp : DefaultTask() {
    @get:Input
    abstract val moduleProp: Property<String>

    @get:Input
    abstract val scopeList: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().dir("META-INF/xposed").asFile
        dir.mkdirs()
        dir.resolve("module.prop").writeText(moduleProp.get())
        dir.resolve("scope.list").writeText(scopeList.get())
    }
}

val generateXposedModuleProp =
    tasks.register<GenerateXposedModuleProp>("generateXposedModuleProp") {
        moduleProp.set(
            """
            id=${prop("module.id")}
            name=${prop("module.name")}
            version=${prop("version.name")}
            versionCode=${prop("version.code")}
            author=${prop("module.author")}
            description=${prop("module.description")}
            minApiVersion=${prop("xposed.api.min")}
            targetApiVersion=${prop("xposed.api.target")}
            staticScope=true
            """.trimIndent() + "\n",
        )
        scopeList.set(targetPackages.joinToString("\n", postfix = "\n"))
        outputDir.set(layout.buildDirectory.dir("generated/xposed"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.resources?.addGeneratedSourceDirectory(
            generateXposedModuleProp,
            GenerateXposedModuleProp::outputDir,
        )
    }
}

/**
 * Refreshes the bundled "More modules" snapshot (src/main/assets/more_modules.json) from the
 * GitHub API: the owner's repos tagged lsposed-module. Runs before every release build; when
 * offline it keeps the existing file. Run it by hand with `./gradlew updateMoreModules`.
 */
abstract class UpdateMoreModules : DefaultTask() {
    @get:Input
    abstract val owner: Property<String>

    @get:Internal
    abstract val snapshot: RegularFileProperty

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun update() {
        val file = snapshot.get().asFile
        try {
            val connection =
                URI("https://api.github.com/users/${owner.get()}/repos?per_page=100")
                    .toURL()
                    .openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "lsposed-module-build")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val keep = listOf("name", "full_name", "description", "stargazers_count", "html_url", "default_branch", "topics", "fork", "archived", "private")
            val repos = groovy.json.JsonSlurper().parseText(body) as List<*>
            val tagged =
                repos.filterIsInstance<Map<*, *>>().filter { repo ->
                    (repo["topics"] as? List<*>)?.contains("lsposed-module") == true
                }.map { repo -> keep.associateWith { repo[it] } }
            file.parentFile.mkdirs()
            file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(tagged)) + "\n")
            logger.lifecycle("More modules snapshot: ${tagged.size} repos")
        } catch (e: Exception) {
            logger.warn("More modules snapshot not refreshed (${e.message}); keeping the existing file")
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.writeText("[]\n")
            }
        }
    }
}

val updateMoreModules =
    tasks.register<UpdateMoreModules>("updateMoreModules") {
        owner.set(prop("module.repo").substringBefore('/'))
        snapshot.set(layout.projectDirectory.file("src/main/assets/more_modules.json"))
    }

tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(updateMoreModules) }
