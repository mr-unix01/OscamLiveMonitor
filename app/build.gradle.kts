plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mrunix.oscamlivemonitor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mrunix.oscamlivemonitor"
        minSdk = 26
        targetSdk = 37
        versionCode = 921
        versionName = "0.9.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
            }
        }
    }

    flavorDimensions += "device"

    productFlavors {
        create("smartphone") {
            dimension = "device"
            buildConfigField("String", "VARIANTE_APP", "\"Smartphone\"")
            buildConfigField("boolean", "TABLET_MODE", "false")
        }

        create("tablet") {
            dimension = "device"
            buildConfigField("String", "VARIANTE_APP", "\"Tablet\"")
            buildConfigField("boolean", "TABLET_MODE", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.okhttp)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.github.mwiede:jsch:2.28.5")
    implementation("commons-net:commons-net:3.13.0")

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}