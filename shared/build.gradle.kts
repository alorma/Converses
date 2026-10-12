plugins {
  alias(libs.plugins.kotlin.jvm)
}

// Domini compartit entre el mòbil i el rellotge: Kotlin pur, sense Android, Compose ni Room
kotlin {
  jvmToolchain(21)
}

dependencies {
  testImplementation(libs.junit)
}
