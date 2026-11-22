plugins { id("com.android.application") }

android {
    namespace = "com.inkspire.rogue"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.inkspire.rogue"
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.0"
        resValue("string", "app_name", "墨塔")
        vectorDrawables.useSupportLibrary = false
    }

    // 公开仓库不带签名密钥：只有 keystore/inkspire.keystore 存在时才签名（密码可用环境变量覆盖）。
    val keystoreFile = rootProject.file("keystore/inkspire.keystore")
    signingConfigs {
        create("inkspire") {
            storeFile = keystoreFile
            storePassword = System.getenv("INKSPIRE_STORE_PASSWORD") ?: "inkspire"
            keyAlias = System.getenv("INKSPIRE_KEY_ALIAS") ?: "inkspire"
            keyPassword = System.getenv("INKSPIRE_KEY_PASSWORD") ?: "inkspire"
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        create("standard") {
            dimension = "edition"
            buildConfigField("boolean", "CHEAT", "false")
        }
        create("cheat") {
            dimension = "edition"
            applicationIdSuffix = ".cheat"
            versionNameSuffix = "-cheat"
            resValue("string", "app_name", "墨塔·作弊版")
            buildConfigField("boolean", "CHEAT", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("inkspire")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("inkspire")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures { buildConfig = true }
    lint { checkReleaseBuilds = false; abortOnError = false }
}

dependencies { }
