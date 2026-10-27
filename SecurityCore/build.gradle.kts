plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
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
    testOptions {
        unitTests.all {
            it.jvmArgs("-Dnet.bytebuddy.experimental=true", "-XX:+EnableDynamicAgentLoading")
        }
    }

    lint {
        //Disable lint analysis for instrumentation tests
        ignoreTestSources = true
        checkDependencies = false
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

configurations.all {
    resolutionStrategy {
        force("androidx.test.espresso:espresso-core:3.6.1")
        force("androidx.test.espresso:espresso-idling-resource:3.6.1")
        force("androidx.test.ext:junit:1.2.1")
        force("androidx.test:runner:1.6.2")
        force("androidx.test:core:1.6.1")
    }
}

dependencies {
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.security.crypto)
    implementation(libs.gson)
    implementation(libs.material3)
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation(libs.okhttp)

    // Tests unitarios JVM
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    // Tests instrumentados Android
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.jragcer"
                artifactId = "securitycore"
                version = libs.versions.securitycore.get()

                pom {
                    name.set("SecurityCore")
                    description.set("Lightweight Android security SDK providing hardware-backed encryption (AES-256-GCM & streaming), biometric auth, root/tamper detection, screen capture protection, and network certificate pinning.")
                    url.set("https://github.com/Jragcer/secure-core-android")
                    licenses {
                        license {
                            name.set("The MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("jragcer")
                            name.set("Jragcer")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Jragcer/secure-core-android.git")
                        developerConnection.set("scm:git:ssh://github.com/Jragcer/secure-core-android.git")
                        url.set("https://github.com/Jragcer/secure-core-android")
                    }
                }
            }
        }
    }
}