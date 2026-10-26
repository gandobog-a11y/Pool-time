plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
}

android {
namespace = "ru.example.pool_time"
compileSdk = 36

defaultConfig {
    applicationId = "ru.example.pool_time"
    minSdk = 23
    targetSdk = 35
    versionCode = 2
    versionName = "1.1"
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}

}
