plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.HeheJuice.OneUISettingsHook"
    compileSdk = 36
    compileSdkVersion(36) 

    defaultConfig {
        applicationId = "com.HeheJuice.OneUISettingsHook"
        minSdk = 35
        targetSdk = 36
        versionCode = 26
        versionName = "V.2.6"
    }

    signingConfigs {
        create("stableDebug") {
            storeFile = file("debug.keystore")
            storePassword = System.getenv("RELEASE_SIGNING_PASSWORD") ?: "android"
            keyAlias = System.getenv("RELEASE_SIGNING_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("RELEASE_SIGNING_KEY_PASSWORD") ?: "android"
        }
    }

    // Both environments grouped together cleanly inside ONE block
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
