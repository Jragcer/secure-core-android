plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.github.jragcer.securitycore"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        val sdkVersion = libs.versions.securitycore.get()
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    lint {
        // Abort the build if any severe or critical errors are detected
        abortOnError = true

        // Perform strict analysis on release builds prior to artifact packaging
        checkReleaseBuilds = true

        // Prevent non-critical warnings from failing the CI build pipeline
        warningsAsErrors = false

        // Suppress benign warnings common in cryptographic and low-level libraries
        disable += setOf(
            "InvalidPackage",
            "UnsafeExperimentalUsageError"
        )
    }
}

dependencies {
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.security.crypto)
    implementation(libs.gson)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}