plugins {
  alias(libs.plugins.kotlin.jvm)
}

// Domini compartit de l'app (avatars, tipus d'entrada, inicials): Kotlin pur, sense Android, Compose ni Room
kotlin {
  jvmToolchain(21)
}

dependencies {
  testImplementation(libs.junit)
}
