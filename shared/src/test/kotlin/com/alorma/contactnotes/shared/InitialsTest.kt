package com.alorma.contactnotes.ui.people

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
  @Test
  fun usesTheFirstLetterOfTheFirstAndLastWords() {
    assertEquals("MS", initialsOf("Marta Soler"))
    assertEquals("MG", initialsOf("marta de la garcia"))
    assertEquals("M", initialsOf("  marta "))
  }

  @Test
  fun aBlankNameGivesAQuestionMark() {
    assertEquals("?", initialsOf(""))
    assertEquals("?", initialsOf("   "))
  }
}
