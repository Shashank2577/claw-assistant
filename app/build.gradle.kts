import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.hilt.application)
    alias(libs.plugins.ksp)
}

val versionPropsFile = file("version.properties")

fun getVersionProps(): Properties {
    val p = Properties()
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { p.load(it) }
    } else {
        p["VERSION_CODE"] = "1"
        p["VERSION_NAME"] = "0.1.0"
    }
    return p
}

val props = getVersionProps()

android {
    namespace = "com.phoneclaw.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phoneclaw.ai"
        minSdk = 31
        targetSdk = 35
        versionCode = props["VERSION_CODE"].toString().toInt()
        versionName = props["VERSION_NAME"].toString()

        manifestPlaceholders["appAuthRedirectScheme"] = "com.phoneclaw.ai.auth"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "PhoneClaw-AI-v${variant.versionName}-${variant.buildType.name}.apk"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("build/generated/sources/proto/debug/java")
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.exifinterface)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.material.icon.extended)

    // Serialization & Reflection
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.com.google.code.gson)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore & Protobuf
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.protobuf.javalite)

    // Hilt DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    // On-device LLM & TFLite
    implementation(libs.litertlm)
    implementation(libs.tflite)
    implementation(libs.tflite.gpu)
    implementation(libs.tflite.support)

    // Markdown rendering
    implementation(libs.commonmark)
    implementation(libs.richtext)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)

    // Image loading
    implementation(libs.coil.compose)

    // Camera
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Security
    implementation(libs.androidx.security.crypto)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Webview & Auth
    implementation(libs.androidx.webkit)
    implementation(libs.openid.appauth)

    // Splash screen
    implementation(libs.androidx.splashscreen)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.26.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

tasks.register("incrementVersion") {
    doLast {
        val p = Properties()
        if (versionPropsFile.exists()) {
            versionPropsFile.inputStream().use { p.load(it) }
        } else {
            p["VERSION_CODE"] = "1"
            p["VERSION_NAME"] = "0.1.0"
        }
        
        val code = p["VERSION_CODE"].toString().toInt() + 1
        val name = p["VERSION_NAME"].toString()
        val parts = name.split(".").toMutableList()
        
        val isMajor = project.hasProperty("majorUpgrade") && project.property("majorUpgrade") == "true"
        
        if (isMajor) {
            val major = parts[0].toInt() + 1
            p["VERSION_NAME"] = "$major.0.0"
        } else {
            if (parts.size >= 2) {
                val minor = parts[1].toInt() + 1
                p["VERSION_NAME"] = "${parts[0]}.$minor.0"
            } else {
                p["VERSION_NAME"] = "0.1.0"
            }
        }
        
        p["VERSION_CODE"] = code.toString()
        FileOutputStream(versionPropsFile).use { p.store(it, null) }
        println("Version incremented to ${p["VERSION_NAME"]} (${p["VERSION_CODE"]})")
    }
}
