import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.screenshot)
  alias(libs.plugins.version)
  alias(libs.plugins.play.publisher)
}

// El versionCode és el del mòbil + 2 (així és únic entre form factors a Play)
appVersion {
  formFactor = FormFactor.WEAR
}

android {
  namespace = "com.alorma.contactnotes.wear"
  compileSdk =
    libs.versions.android.compileSdk
      .get()
      .toInt()

  defaultConfig {
    // Mateix applicationId que el mòbil: Play les serveix com una sola app
    applicationId = "com.alorma.contactnotes"
    minSdk =
      libs.versions.android.wearMinSdk
        .get()
        .toInt()
    targetSdk =
      libs.versions.android.targetSdk
        .get()
        .toInt()
  }

  // Mateixa keystore de debug que el mòbil
  signingConfigs {
    getByName("debug") {
      storeFile = rootProject.file("certs/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    // Mateixa keystore de release que el mòbil (fora del repo); Play exigeix la mateixa signatura
    val releaseKeystore = rootProject.file("certs/release.keystore")
    if (releaseKeystore.exists() && System.getenv("KEYSTORE_PASSWORD") != null) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
      applicationIdSuffix = ".dev"
    }
    release {
      optimization {
        enable = true
      }
      signingConfigs.findByName("release")?.let { signingConfig = it }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// El plugin de captures no té DSL per al llindar des d'AGP 9; es posa a la tasca de validació
tasks.withType<PreviewScreenshotValidationTask>().configureEach {
  testEngineInput.threshold.set(0.01f)
}

// El rellotge es publica a la seva pròpia pista de Play (`wear:<pista>`, vegeu release.yml): Play no
// admet un bundle que exigeix android.hardware.type.watch a la pista del mòbil.

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(project(":shared"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.graphics.shapes)
  implementation(libs.wear.compose.foundation)
  implementation(libs.wear.compose.material3)
  implementation(libs.wear.compose.navigation3)
  implementation(libs.wear.navigation3.runtime)
  implementation(libs.wear.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.wear.tooling.preview)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.work.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.play.services.wearable)
  implementation(libs.wear.remote.interactions)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.androidx.fragment)
  implementation(libs.kotlinx.coroutines.play.services)
  debugImplementation(libs.wear.compose.ui.tooling)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  screenshotTestImplementation(libs.compose.screenshot.validation)
  screenshotTestImplementation(platform(libs.androidx.compose.bom))
  screenshotTestImplementation(libs.androidx.compose.ui)
  screenshotTestImplementation(libs.wear.compose.ui.tooling)
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
