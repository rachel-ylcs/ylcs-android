import org.jetbrains.kotlin.cli.jvm.main

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.yinlin.rachel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yinlin.rachel"
        minSdk = 29
        targetSdk = 35
        versionCode = 230
        versionName = "2.3.0"

        ndk {
            abiFilters += arrayOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        register("release") {
            keyAlias = "rachel"
            keyPassword = "rachel1211"
            storeFile = file("key.jks")
            storePassword = "rachel1211"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    viewBinding {
        enable = true
    }
}

dependencies {
    implementation(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.aar", "*.jar"),
    )))

    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")

    implementation("com.haibin:calendarview:3.7.1")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.android.material:material:1.13.0-alpha08")

    implementation("com.github.bumptech.glide:glide:5.0.0-rc01")
    implementation("com.github.CarGuo.GSYVideoPlayer:gsyvideoplayer-java:v10.0.0")
    implementation("com.github.CarGuo.GSYVideoPlayer:gsyvideoplayer-exo2:v10.0.0")

    implementation("com.github.forJrking:KLuban:1.1.0")
    implementation("com.github.gturedi:stateful-layout:1.2.1")
    implementation("com.github.jenly1314:zxing-lite:3.2.0")
    implementation("com.github.rtugeek:colorseekbar:2.0.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.github.lucksiege:pictureselector:v3.11.2")
    implementation("io.github.lucksiege:ucrop:v3.11.2")
    implementation("io.github.sangcomz:StickyTimeLine:1.1.0")
    implementation("io.github.scwang90:refresh-layout-kernel:3.0.0-alpha")
    implementation("io.github.scwang90:refresh-header-classics:3.0.0-alpha")
    implementation("io.github.youth5201314:banner:2.2.3")

    implementation("com.tencent:mmkv:2.0.1")
    implementation("com.tencent.tav:libpag:4.4.15-harfbuzz")

    testImplementation("junit:junit:4.13.2")
}