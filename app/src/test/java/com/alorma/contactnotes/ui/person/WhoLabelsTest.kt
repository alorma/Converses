package com.alorma.contactnotes.ui.person

import com.alorma.contactnotes.data.db.EntryKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WhoLabelsTest {
  @Test
  fun firstNameIsTheFirstWord() {
    assertEquals("Jordi", firstName("Jordi Puig"))
    assertEquals("Rafa", firstName("  Rafa "))
    assertEquals("Luna", firstName("Luna"))
  }

  @Test
  fun firstNameOfTwelveCharactersIsNotCut() {
    assertEquals("Maximiliano1", firstName("Maximiliano1 Garcia"))
  }

  @Test
  fun firstNameLongerThanTwelveCharactersIsCutWithEllipsis() {
    val result = firstName("Maximiliano12 Garcia")
    assertEquals(12, result.length)
    assertEquals("Maximiliano…", result)
  }

  @Test
  fun initialIsTheUppercaseFirstLetter() {
    assertEquals("J", initialOf("jordi puig"))
  }

  @Test
  fun whoLabelPicksMeThePersonOrTheNote() {
    assertEquals("Jo", whoLabel(EntryKind.SAID, "Jordi", "Jo", "Apunt"))
    assertEquals("Jordi", whoLabel(EntryKind.HEARD, "Jordi", "Jo", "Apunt"))
    assertEquals("Apunt", whoLabel(EntryKind.HAPPENED, "Jordi", "Jo", "Apunt"))
  }
}
