plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tcar.tmirror"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.tcar.tmirror"
        minSdk = 29
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-alpha"
    }
    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            // AAB is unsigned by design. Sign locally with your Play upload key.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(files("../app/libs/aauto.aar"))
}
