import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Private, developer-owned configuration; excluded from Git.
val cloudConfig = Properties().apply {
    val configFile = rootProject.file("cloud.properties")
    if (configFile.exists()) configFile.inputStream().use { load(it) }
}
fun cloudString(key: String): String {
    val value = cloudConfig.getProperty(key, "")
    return "\"" + value.replace("\\", "\\\\")
        .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
}


android {
    namespace = "com.example.iot3"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "NLE_ACCOUNT", cloudString("NLE_ACCOUNT"))
        buildConfigField("String", "NLE_PASSWORD", cloudString("NLE_PASSWORD"))
        buildConfigField("String", "NLE_DEVICE_ID", cloudString("NLE_DEVICE_ID"))
        applicationId = "com.example.iot3"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}