import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.compose.screenshot)
}

room {
  schemaDirectory("$projectDir/schemas")
}

android {
  namespace = "com.alorma.contactnotes"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    applicationId = "com.alorma.contactnotes"
    minSdk = 36
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Keystore de debug compartida (al repo): l'APK de debug sempre té la mateixa signatura,
  // es construeixi on es construeixi, i es pot instal·lar a sobre sense esborrar l'app
  signingConfigs {
    getByName("debug") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
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

  experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// El plugin de captures no té DSL per al llindar des d'AGP 9; es posa a la tasca de validació
tasks.withType<PreviewScreenshotValidationTask>().configureEach {
  testEngineInput.threshold.set(0.01f)
}

dependencies {
  implementation(project(":designsystem"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.android)
  implementation(libs.koin.compose)
  implementation(libs.koin.compose.viewmodel)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.compose.settings.core)
  implementation(libs.compose.settings.expressive)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  screenshotTestImplementation(libs.compose.screenshot.validation)
  screenshotTestImplementation(platform(libs.androidx.compose.bom))
  screenshotTestImplementation(libs.androidx.compose.ui)
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
