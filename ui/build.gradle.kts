@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val pkg: String = providers.gradleProperty("amneziawgPackageName").get()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22"  // Add the Kotlin serialization plugin
}

android {
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    namespace = pkg
    defaultConfig {
        applicationId = pkg
        targetSdk = 34
        versionCode = providers.gradleProperty("amneziawgVersionCode").get().toInt()
        versionName = providers.gradleProperty("amneziawgVersionName").get()
        buildConfigField("int", "MIN_SDK_VERSION", minSdk.toString())
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    signingConfigs {
        create("signed_release") {
            storeFile = file(project.property("MYAPP_RELEASE_STORE_FILE") as String)
            keyAlias = project.property("MYAPP_RELEASE_KEY_ALIAS") as String
            storePassword = project.property("MYAPP_RELEASE_STORE_PASSWORD") as String
            keyPassword = project.property("MYAPP_RELEASE_KEY_PASSWORD") as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.txt"
            )
            packaging {
                resources {
                    excludes += "DebugProbesKt.bin"
                    excludes += "kotlin-tooling-metadata.json"
                    excludes += "META-INF/*.version"
                }
            }
            signingConfig = signingConfigs.getByName("signed_release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("signed_release")
        }
        create("googleplay") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            signingConfig = signingConfigs.getByName("signed_release")
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    lint {
        disable += "LongLogTag"
        warning += "MissingTranslation"
        warning += "ImpliedQuantity"
    }
}

dependencies {
    implementation(project(":tunnel"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.material)
    implementation(libs.zxing.android.embedded)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugarJdkLibs)
    implementation("io.ktor:ktor-client-core:1.6.7")
    implementation("io.ktor:ktor-client-json:1.6.7")
    implementation("io.ktor:ktor-client-serialization:1.6.7")
    implementation("io.ktor:ktor-client-android:1.6.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2") // Add this line
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.register("printProguardFiles") {
    doLast {
        println("ProGuard files: ${android.buildTypes.getByName("release").proguardFiles}")
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn("printProguardFiles")
}
