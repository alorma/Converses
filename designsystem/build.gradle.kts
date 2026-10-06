plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.alorma.contactnotes.designsystem"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    minSdk = 36
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  api(platform(libs.androidx.compose.bom))
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.foundation)
  api(libs.androidx.compose.material3)
  api(libs.androidx.graphics.shapes)
  implementation(libs.materialkolor)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
}
