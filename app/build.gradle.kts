plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.studyspace.timer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.studyspace.timer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Explicitly declared (rather than relying on it transitively via
    // material3) so it resolves to the exact compose-bom-aligned version.
    // Without this, some environments pulled a mismatched foundation /
    // foundation-layout pair and Modifier.weight() (a public
    // RowScope/ColumnScope extension) resolved to an internal overload,
    // producing "Cannot access 'weight': it is internal in
    // androidx.compose.foundation.layout" in AnalyticsScreen/HomeScreen/
    // SettingsScreen.
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.2.1")
    // Provides calculateWindowSizeClass(), used once in MainActivity to
    // classify the window into compact/medium/expanded width so screens can
    // make coarse layout decisions without each reinventing dp breakpoints.
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Coroutines (timer engine ticking; previously only pulled in transitively
    // via lifecycle-viewmodel-ktx — declared explicitly now that Stage 3 code
    // depends on it directly, so the version is pinned rather than implicit)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Room (local persistence)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (settings + theme persistence)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil (loads the user's "Personalize" gallery photo from private app
    // storage into a Compose Image — the only non-bundled image source in
    // the app, so this is the one place an image-loading library is needed)
    implementation("io.coil-kt:coil-compose:2.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    // Phase 13: export/import uses Android's built-in org.json. In local unit tests the Android SDK's
    // org.json is only a stub, so the real implementation is added for the test classpath only —
    // it isn't packaged into the app.
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
