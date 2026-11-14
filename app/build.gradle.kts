import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val baseApplicationId = "com.sxanalys.music"
val applicationIdOverride = System.getenv("SXANALYS_APPLICATION_ID")?.takeIf { it.isNotBlank() }
val appNameOverride = System.getenv("SXANALYS_APP_NAME")?.takeIf { it.isNotBlank() }
val appVersion =
    rootProject
        .file("VERSION")
        .takeIf { it.exists() }
        ?.readText()
        ?.trim()
        ?.takeIf { it.matches(Regex("\\d+(\\.\\d+){1,4}")) }
        ?: "1.0.0.0.0"
val appVersionCode =
    appVersion
        .split(".")
        .map { it.toInt() }
        .let { parts -> parts + List(5 - parts.size) { 0 } }
        .let { p ->
            1_100_000_000 + p[0].coerceIn(0, 99) * 10_000_000 + p[1].coerceIn(0, 99) * 100_000 +
                p[2].coerceIn(0, 99) * 1_000 + p[3].coerceIn(0, 9) * 100 + p[4].coerceIn(0, 99)
        }
val sxUpdateRepo =
    System.getenv("GITHUB_REPOSITORY")
        ?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) }
        ?: ""
val buildCommit =
    System.getenv("SXANALYS_BUILD_COMMIT")
        ?.trim()
        ?.takeIf { it.matches(Regex("[0-9a-fA-F]{7,40}")) }
        ?.take(7)
        ?.lowercase()
val debugKeystorePathOverride = System.getenv("SXANALYS_DEBUG_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val debugKeystorePassword = System.getenv("SXANALYS_DEBUG_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
val debugKeyAlias = System.getenv("SXANALYS_DEBUG_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "androiddebugkey"
val debugKeyPassword = System.getenv("SXANALYS_DEBUG_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
val persistentDebugKeystoreFile = file("persistent-debug.keystore")
val workflowDebugKeystoreFile = debugKeystorePathOverride?.let(::file)

plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.sxanalys.music"
    compileSdk = 37

    defaultConfig {
        applicationId = applicationIdOverride ?: baseApplicationId
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
        val baseVersionName = requireNotNull(versionName)
        buildConfigField("String", "BASE_VERSION_NAME", "\"$baseVersionName\"")
        buildConfigField("String", "UPDATE_REPO", "\"$sxUpdateRepo\"")
        buildCommit?.let { versionName = "$baseVersionName+$it" }
        resValue("string", "app_name", appNameOverride ?: "SX ANLYS")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        // LastFM API keys from GitHub Secrets
        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: ""
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: ""

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")
        val spotifyId = localProperties.getProperty("SPOTIFY_CLIENT_ID") ?: System.getenv("SPOTIFY_CLIENT_ID") ?: ""
        val spotifySecret = localProperties.getProperty("SPOTIFY_CLIENT_SECRET") ?: System.getenv("SPOTIFY_CLIENT_SECRET") ?: ""
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$spotifySecret\"")
        buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        buildConfigField("Long", "DISCORD_APP_ID", "1447278780795064401L")
    }

    flavorDimensions += listOf("variant")
    productFlavors {
        // FOSS - Updater, but no gcast
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        // GMS - Updater and gcast
        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        // IzzyOnDroid - no gcast, no updater - the ONLY F-droid compliant build
        create("izzy") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "false")
        }
    }

    signingConfigs {
        create("persistentDebug") {
            storeFile = persistentDebugKeystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("workflowDebug") {
            storeFile = workflowDebugKeystoreFile ?: persistentDebugKeystoreFile
            storePassword = debugKeystorePassword
            keyAlias = debugKeyAlias
            keyPassword = debugKeyPassword
        }
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            if (applicationIdOverride == null) {
                applicationIdSuffix = ".debug"
            }
            isDebuggable = true
            if (appNameOverride == null) {
                resValue("string", "app_name", "SX ANLYS")
            }
            signingConfig =
                if (workflowDebugKeystoreFile != null) {
                    signingConfigs.getByName("workflowDebug")
                } else {
                    signingConfigs.getByName("persistentDebug")
                }
        }
        create("sx") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig =
                if (workflowDebugKeystoreFile != null) {
                    signingConfigs.getByName("workflowDebug")
                } else {
                    signingConfigs.getByName("persistentDebug")
                }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
        // Lint never gated anything here (abortOnError = false), so the
        // lintVital pass that assembleRelease implicitly triggers was pure
        // build time. Run lint on demand with ./gradlew :app:lintGmsRelease.
        checkReleaseBuilds = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols +=
                listOf(
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so",
                )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}

val cleanLegacyProtoSources = tasks.register<Delete>("cleanLegacyProtoSources") {
    delete(layout.projectDirectory.dir("src/main/java/com/sxanalys/music/listentogether/proto"))
}

tasks.named("preBuild") {
    dependsOn(cleanLegacyProtoSources)
}

// persistent-debug.keystore is gitignored (*.keystore), so it never exists on a fresh
// CI checkout. The `debug` buildType's signingConfig always points at it (or at
// workflowDebugKeystoreFile when SXANALYS_DEBUG_KEYSTORE_PATH is set) regardless of
// whether the file exists yet - that decision is static, made with no I/O, so it is
// safe under the configuration cache. This task creates the missing keystore file at
// *execution* time (running an external process during configuration is disallowed
// under the configuration cache; doing it in a task's execution phase is fine). Only
// plain File/String values are captured into local vals before doLast, so the task
// action holds no reference to the build script object, which is required for the
// configuration cache to serialize this task.
val ensureDebugKeystore = tasks.register("ensureDebugKeystore") {
    val keystoreFile = workflowDebugKeystoreFile ?: persistentDebugKeystoreFile
    val storePass = if (workflowDebugKeystoreFile != null) debugKeystorePassword else "android"
    val alias = if (workflowDebugKeystoreFile != null) debugKeyAlias else "androiddebugkey"
    val keyPass = if (workflowDebugKeystoreFile != null) debugKeyPassword else "android"
    val keytool = File(System.getProperty("java.home"), "bin/keytool").absolutePath

    outputs.file(keystoreFile)
    onlyIf { !keystoreFile.exists() }
    doLast {
        keystoreFile.parentFile?.mkdirs()
        val process = ProcessBuilder(
            keytool,
            "-genkeypair",
            "-keystore", keystoreFile.absolutePath,
            "-storepass", storePass,
            "-alias", alias,
            "-keypass", keyPass,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "Gagal membuat debug keystore (exit $exitCode):\n$output" }
    }
}

tasks.matching { it.name.startsWith("validateSigning") && (it.name.endsWith("Debug") || it.name.endsWith("Sx")) }.configureEach {
    dependsOn(ensureDebugKeystore)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
        suppressWarnings.set(false)
    }
}

// Android provides org.json as a platform API (/apex/com.android.art/javalib/core-libart.jar).
// The standalone org.json:json artefact bundles an older Apache Harmony copy of JSONArray that
// contains an internal `myArrayList` field absent from the platform class.  Without obfuscation
// R8 inlines against this internal field; at runtime the platform class is resolved instead,
// producing a NoSuchFieldError.  Excluding the artefact globally ensures only the platform
// class is ever referenced.
configurations.configureEach {
    exclude(group = "org.json", module = "json")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)

    implementation(libs.activity)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.materialKolor)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.browser)

    implementation(libs.ucrop)

    implementation(libs.shimmer)
    implementation(libs.backdrop)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    // Google Cast - only included in GMS flavor (not available in F-Droid/FOSS builds)
    "gmsImplementation"(libs.media3.cast)
    "gmsImplementation"(libs.mediarouter)
    "gmsImplementation"(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)

    // Protobuf for message serialization (lite version for Android)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.ktor.client.mock)
}
