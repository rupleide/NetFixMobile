import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rupleide.netfix"
    ndkVersion = "30.0.14904198"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.rupleide.netfix"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("long", "BETA_EXPIRES_AT_MS", "0L")
        buildConfigField("long", "BETA_BUILT_AT_MS", "0L")
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("beta") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            val betaExpiresAtMs = (project.findProperty("netfixBetaExpiresAtMs") as String?) ?: "1794776400000"
            buildConfigField("long", "BETA_EXPIRES_AT_MS", "${betaExpiresAtMs}L")
            buildConfigField("long", "BETA_BUILT_AT_MS", "${System.currentTimeMillis()}L")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.setSrcDirs(listOf("src/main/jniLibs", "src/main/jniLibsRust"))
        }
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.lifecycle:lifecycle-service:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("dev.chrisbanes.haze:haze:1.6.10")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register<Exec>("runNdkBuild") {
    group = "build"

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    val sdkDir = localProperties.getProperty("sdk.dir")
    val ndkDir = File(sdkDir, "ndk").listFiles()?.firstOrNull { it.isDirectory } ?: File(sdkDir, "ndk-bundle")

    executable = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        "$ndkDir\\ndk-build.cmd"
    } else {
        "$ndkDir/ndk-build"
    }
    setArgs(listOf(
        "NDK_PROJECT_PATH=${project.layout.buildDirectory.get().asFile.absolutePath}/intermediates/ndkBuild",
        "NDK_LIBS_OUT=${project.projectDir.absolutePath}/src/main/jniLibs",
        "APP_BUILD_SCRIPT=${project.projectDir.absolutePath}/src/main/jni/Android.mk",
        "NDK_APPLICATION_MK=${project.projectDir.absolutePath}/src/main/jni/Application.mk"
    ))

    println("Command: $commandLine")
}

fun findGitExecutable(): String? {
    val fromPath = runCatching {
        ProcessBuilder("git", "--version").redirectErrorStream(true).start().waitFor() == 0
    }.getOrDefault(false)
    if (fromPath) return "git"
    val localAppData = System.getenv("LOCALAPPDATA") ?: return null
    return File(localAppData, "GitHubDesktop").listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("app-") }
        ?.sortedBy { it.name }
        ?.map { File(it, "resources/app/git/cmd/git.exe") }
        ?.lastOrNull { it.exists() }
        ?.absolutePath
}

tasks.register("applyByedpiPatch") {
    group = "build"
    val byedpiDir = file("src/main/cpp/byedpi")
    val patchesWithMarkers = listOf(
        file("src/main/cpp/patches/byedpi-cloudflare-redirect.patch") to "redirect_set",
        file("src/main/cpp/patches/byedpi-dns-redirect.patch") to "dns_redirect_addr",
        file("src/main/cpp/patches/byedpi-quic-giveup.patch") to "quic_giveup_ms"
    )
    doLast {
        patchesWithMarkers.forEach { (patchFile, marker) ->
            val alreadyPatched = File(byedpiDir, "params.h").readText().contains(marker)
            if (alreadyPatched) return@forEach
            val git = findGitExecutable()
                ?: throw GradleException("byedpi без правки ${patchFile.name}, а git не найден: выполните «git apply ${patchFile.path}» в ${byedpiDir.path}")
            val process = ProcessBuilder(git, "-C", byedpiDir.absolutePath, "apply", patchFile.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() != 0) throw GradleException("Не удалось наложить правку byedpi ${patchFile.name}: $output")
            println("Правка byedpi наложена: ${patchFile.name}")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("runNdkBuild")
    dependsOn("applyByedpiPatch")
}

base {
    archivesName.set("NetFix-Mobile-v1.0.5")
}
