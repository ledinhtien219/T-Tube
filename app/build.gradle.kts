plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.carhud.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ledinhtien.ttube"
        minSdk = 29
        targetSdk = 36
        versionCode = 303
        versionName = "1.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.outputFileName =
                "TTube_v${variant.versionName}_${variant.buildType.name}.apk"
        }
    }

    signingConfigs {
        create("stableCi") {
            val stableKey = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storeFile = stableKey
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("playUpload") {
            val uploadStore = providers.environmentVariable("PLAY_UPLOAD_KEYSTORE_PATH").orNull
            val uploadStorePassword = providers.environmentVariable("PLAY_UPLOAD_STORE_PASSWORD").orNull
            val uploadAlias = providers.environmentVariable("PLAY_UPLOAD_KEY_ALIAS").orNull
            val uploadKeyPassword = providers.environmentVariable("PLAY_UPLOAD_KEY_PASSWORD").orNull
            if (!uploadStore.isNullOrBlank()) storeFile = file(uploadStore)
            if (!uploadStorePassword.isNullOrBlank()) storePassword = uploadStorePassword
            if (!uploadAlias.isNullOrBlank()) keyAlias = uploadAlias
            if (!uploadKeyPassword.isNullOrBlank()) keyPassword = uploadKeyPassword
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            buildConfigField("boolean", "PLAY_DISTRIBUTION", "true")

            // Google Play uses a dedicated upload key. If CI secrets are not
            // configured yet, bundleRelease still produces an unsigned AAB so
            // the manifest/API gate can be validated before account approval.
            val uploadKeyPath = providers.environmentVariable("PLAY_UPLOAD_KEYSTORE_PATH").orNull
            val uploadKeyReady = !uploadKeyPath.isNullOrBlank() && file(uploadKeyPath).exists() &&
                !providers.environmentVariable("PLAY_UPLOAD_STORE_PASSWORD").orNull.isNullOrBlank() &&
                !providers.environmentVariable("PLAY_UPLOAD_KEY_ALIAS").orNull.isNullOrBlank() &&
                !providers.environmentVariable("PLAY_UPLOAD_KEY_PASSWORD").orNull.isNullOrBlank()
            if (uploadKeyReady) {
                signingConfig = signingConfigs.getByName("playUpload")
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("boolean", "PLAY_DISTRIBUTION", "false")
            // Allow emulator checks alongside an installed APK signed by another key.
            if (providers.gradleProperty("isolatedRuntimeTest").orNull == "true") {
                applicationIdSuffix = ".runtimetest"
            }
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stableCi")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    implementation("androidx.media:media:1.7.0")
    implementation(files("libs/aauto.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.projected)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.remote.creation.compose)
    implementation(libs.androidx.compose.remote.creation)
    implementation(libs.androidx.compose.remote.player.core)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.6")
    implementation("org.slf4j:slf4j-nop:2.0.16")
}
