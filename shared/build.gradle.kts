plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

// Domini compartit entre el mòbil i el rellotge: Kotlin pur, sense Android, Compose ni Room
kotlin {
  jvmToolchain(21)
}

dependencies {
  api(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
}
