plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.jetbrains)
}

android {
    namespace = "com.yinlin.rachel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yinlin.rachel"
        minSdk = 29
        targetSdk = 35
        versionCode = 232
        versionName = "2.3.2"

        ndk {
            abiFilters += arrayOf("arm64-v8a")
        }
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

    implementation(libs.data.reflect)
    implementation(libs.data.gson)
    implementation(libs.data.mmkv)
    implementation(libs.data.okhttp)
    implementation(libs.image.glide)
    implementation(libs.image.qrcode)
    implementation(libs.image.selector)
    implementation(libs.image.selector.crop)
    implementation(libs.image.selector.compress)
    implementation(libs.image.libpag)
    implementation(libs.media.player)
    implementation(libs.media.player.dash)
    implementation(libs.media.player.ui)
    implementation(libs.media.player.session)
    implementation(libs.media.videoPlayer)
    implementation(libs.media.videoPlayer.engine)
    implementation(libs.ui.material)
    implementation(libs.ui.calendar)
    implementation(libs.ui.colorSeekbar)
    implementation(libs.ui.refreshLayout)
    implementation(libs.ui.refreshLayout.header)
}