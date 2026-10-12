plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.version)
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
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.graphics.shapes)
  implementation(libs.wear.compose.foundation)
  implementation(libs.wear.compose.material3)
  implementation(libs.wear.tooling.preview)
  debugImplementation(libs.wear.compose.ui.tooling)
  testImplementation(libs.junit)
}
