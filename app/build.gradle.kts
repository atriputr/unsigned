plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.un_signed"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // applicationId renamed from `com.example.un_signed` → `com.atriputr.unsigned`
        // because Health Connect (and most Google services) silently reject apps
        // under the `com.example.*` namespace — reserved for sample / debug code.
        // After this change the Android OS treats this APK as a fresh app:
        //   · existing install at the old id must be uninstalled first
        //   · Health Connect permission grants, Settings → App Info, etc. all reset
        //   · locally-stored JSON data lives under /data/data/com.atriputr.unsigned/
        // The Kotlin `namespace` deliberately stays as `com.example.un_signed` so
        // every source file + the R class keep working without a bulk rename.
        applicationId = "com.atriputr.unsigned"
        minSdk = 29
        targetSdk = 36
        versionCode = 46
        versionName = "5.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.activity.compose)

    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.health.connect.client)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}