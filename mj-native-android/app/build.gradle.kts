plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mj.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mj.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Gemini API key: injected from CI secret or gradle.properties
        // In CI: set GEMINI_API_KEY as a GitHub Actions secret
        // Locally: add GEMINI_API_KEY=your_key to mj-native-android/gradle.properties
        val geminiKey = System.getenv("GEMINI_API_KEY")
            ?: project.findProperty("GEMINI_API_KEY")?.toString()
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // HTTP Calls for Gemini API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
