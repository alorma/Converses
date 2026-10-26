import com.android.compose.screenshot.tasks.PreviewScreenshotValidationTask
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.compose.screenshot)
  alias(libs.plugins.version)
  alias(libs.plugins.play.publisher)
  alias(libs.plugins.androidx.baselineprofile)
}

room {
  schemaDirectory("$projectDir/schemas")
}

// Claus de RevenueCat: de `local.properties` en local i de la variable d'entorn (secret del mateix nom) a la CI.
// Si no n'hi ha cap, la clau queda buida i l'app no configura RevenueCat: ningú no és Pro
val localProperties =
  Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
  }

fun secret(name: String): String = System.getenv(name) ?: localProperties.getProperty(name).orEmpty()

android {
  namespace = "com.alorma.contactnotes"
  compileSdk =
    libs.versions.android.compileSdk
      .get()
      .toInt()

  defaultConfig {
    applicationId = "com.alorma.contactnotes"
    minSdk =
      libs.versions.android.minSdk
        .get()
        .toInt()
    targetSdk =
      libs.versions.android.targetSdk
        .get()
        .toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Keystore de debug compartida (al repo): l'APK de debug sempre té la mateixa signatura,
  // es construeixi on es construeixi, i es pot instal·lar a sobre sense esborrar l'app
  signingConfigs {
    getByName("debug") {
      storeFile = rootProject.file("certs/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    // Keystore de release: fora del repo. A la CI es descodifica del secret RELEASE_KEYSTORE_BASE64 a
    // `certs/release.keystore`; les contrasenyes arriben per KEYSTORE_PASSWORD, KEY_ALIAS i KEY_PASSWORD
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
      // Permet tenir instal·lades a la vegada la versió de debug i la de release
      applicationIdSuffix = ".dev"
      // Test Store de RevenueCat: el `.dev` no és a Play, i aquesta clau no cobra mai res
      buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_TEST_API_KEY")}\"")
    }
    release {
      optimization {
        enable = true
      }
      // Símbols de les .so de les llibreries (graphics-path, datastore): Play deixa de avisar que l'AAB no en porta
      ndk {
        debugSymbolLevel = "SYMBOL_TABLE"
      }
      signingConfigs.findByName("release")?.let { signingConfig = it }
      buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_GOOGLE_API_KEY")}\"")
    }
  }
  buildFeatures {
    buildConfig = true
  }
  testOptions {
    // Els tests de ViewModel construeixen classes que reben un `Context` que no arriben a fer servir
    unitTests.isReturnDefaultValues = true
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

// The baseline profile plugin clones `release` into `nonMinifiedRelease` but with the `optimization` DSL it stays
// obfuscated, and the profile would be full of names like `La02;`
androidComponents {
  beforeVariants(selector().withBuildType("nonMinifiedRelease")) { variant ->
    variant.isMinifyEnabled = false
    variant.shrinkResources = false
  }
}

dependencies {
  implementation(project(":designsystem"))
  implementation(project(":shared"))
  implementation(libs.androidx.profileinstaller)
  baselineProfile(project(":baselineprofile"))
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
  implementation(libs.fireandforget.core)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.play.services.wearable)
  implementation(libs.revenuecat.purchases)
  implementation(libs.wear.remote.interactions)
  implementation(libs.kotlinx.coroutines.guava)
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
