package com.alorma.contactnotes.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
  @Test
  fun usesTheFirstLetterOfTheFirstTwoWords() {
    assertEquals("MS", initialsOf("Marta Soler"))
    assertEquals("AB", initialsOf("ana bea carla"))
    assertEquals("AB", initialsOf("ana  bea carla"))
    assertEquals("MD", initialsOf("marta de la garcia"))
    assertEquals("M", initialsOf("  marta "))
  }

  @Test
  fun aBlankNameGivesAQuestionMark() {
    assertEquals("?", initialsOf(""))
    assertEquals("?", initialsOf("   "))
  }

  @Test
  fun symbolsDoNotCount() {
    assertEquals("PP", initialsOf("Pepe (pis)"))
    assertEquals("M", initialsOf("@marta"))
    assertEquals("MS", initialsOf("Marta & Soler"))
    assertEquals("M", initialsOf("🙂 Marta"))
    assertEquals("OB", initialsOf("O'Brien Bea"))
  }

  @Test
  fun digitsCountAsLetters() {
    assertEquals("A2", initialsOf("Aula 2"))
    assertEquals("7", initialsOf("(7)"))
  }

  @Test
  fun aNameOnlyOfSymbolsGivesAQuestionMark() {
    assertEquals("?", initialsOf("(...)"))
    assertEquals("?", initialsOf("🙂"))
  }
}
