plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.felo.penguinsolver"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.felo.penguinsolver"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    flavorDimensions += "game"
    productFlavors {
        create("penguin") {
            dimension = "game"
            applicationId = "com.felo.penguinsolver"
            buildConfigField("boolean", "HOLE", "false")
        }
        create("hole") {
            dimension = "game"
            applicationId = "com.felo.holebot"
            buildConfigField("boolean", "HOLE", "true")
        }
    }
}
