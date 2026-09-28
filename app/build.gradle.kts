plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("screenwriter.licenses")
}

android {
    namespace = "com.boliby.screenwriter"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.boliby.screenwriter"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.2.0"
    }

    signingConfigs {
        // Committed on purpose so every debug build, local or CI, has the same
        // signature and installs over the previous one. Never used for release.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core-format"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
