package com.alorma.contactnotes.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarColorTest {
  @Test
  fun sameNameAlwaysGivesTheSameColor() {
    assertEquals(AvatarColor.forName("Marta Soler"), AvatarColor.forName("Marta Soler"))
  }

  @Test
  fun caseAccentsAndOuterSpacesDoNotChangeTheColor() {
    val expected = AvatarColor.forName("jose garcia")
    assertEquals(expected, AvatarColor.forName("  José García "))
    assertEquals(expected, AvatarColor.forName("JOSE GARCIA"))
  }

  @Test
  fun twentyNamesUseEveryColor() {
    val names =
      listOf(
        "Marta",
        "Jordi",
        "Laia",
        "Pau",
        "Luna",
        "Nyra",
        "Kael",
        "Anna",
        "Pere",
        "Núria",
        "Joan",
        "Maria",
        "Carles",
        "Elena",
        "David",
        "Sara",
        "Marc",
        "Irene",
        "Oriol",
        "Clara",
      )
    val used = names.map { AvatarColor.forName(it) }.toSet()
    assertTrue("only ${used.size} colors: $used", used.size == AvatarColor.entries.size)
  }
}
