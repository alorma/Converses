import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.androidx.baselineprofile)
}

android {
  namespace = "com.alorma.contactnotes.baselineprofile"

  compileSdk =
    libs.versions.android.compileSdk
      .get()
      .toInt()
  defaultConfig {
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

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  targetProjectPath = ":app"
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_21
  }
}

// The profile is generated on the connected device/emulator (adb); there are no managed devices
baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(libs.androidx.junit)
  implementation(libs.androidx.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
}
