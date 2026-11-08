import java.util.Properties

val props = Properties().apply {
    val f = file("app.properties")
    if (f.exists()) f.reader(Charsets.UTF_8).use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fitmaison.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = props.getProperty("appId", "com.fitmaison.pro")
        minSdk = 24
        targetSdk = 34
        versionCode = props.getProperty("versionCode", "1").toInt()
        versionName = props.getProperty("versionName", "1.0")
        manifestPlaceholders["appName"] = props.getProperty("appName", "FIT MAISON PRO")
    }

    // Clé fixe : chaque nouvelle version s'installe par-dessus l'ancienne
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/fit.keystore")
            storePassword = "fitmaison123"
            keyAlias = "fit"
            keyPassword = "fitmaison123"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
