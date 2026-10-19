plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.zhixin"
    compileSdk {
        version = release(37)
    }

    // 统一签名：debug 与 release 都用同一个 keystore，
    // 这样后续版本可以直接覆盖安装升级（同包名 + 同签名 + versionCode 递增），数据不会丢。
    signingConfigs {
        create("zhixin") {
            storeFile = rootProject.file("zhixin.jks")
            storePassword = "zhixin2026"
            keyAlias = "zhixin"
            keyPassword = "zhixin2026"
        }
    }

    defaultConfig {
        applicationId = "com.example.zhixin"
        minSdk = 24
        targetSdk = 37
        // 发新版时：versionCode 必须比已安装的版本大，versionName 同步更新。
        versionCode = 4
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("zhixin")
        }
        release {
            signingConfig = signingConfigs.getByName("zhixin")
            optimization {
                enable = false
            }
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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}