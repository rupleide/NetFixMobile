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
        targetSdk = 26
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable.add("ExpiredTargetSdkVersion")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.lifecycle:lifecycle-service:2.9.4")
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

tasks.named("preBuild") {
    dependsOn("runNdkBuild")
}

base {
    archivesName.set("NetFix-Mobile-v1.0.2")
}
