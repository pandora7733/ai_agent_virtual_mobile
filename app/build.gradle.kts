plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sihoo.ai_agent_virtual_mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sihoo.ai_agent_virtual_mobile"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(project(":cubism:framework"))
    implementation(files("$rootDir/cubism/core/android/Live2DCubismCore.aar"))
}