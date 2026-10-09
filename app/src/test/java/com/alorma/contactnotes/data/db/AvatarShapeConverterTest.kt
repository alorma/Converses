package com.alorma.contactnotes.data.db

import com.alorma.contactnotes.designsystem.components.AvatarShape
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class AvatarShapeConverterTest {
  private val converter = AvatarShapeConverter()

  @Test
  fun everyShapeSurvivesTheRoundTrip() {
    AvatarShape.entries.forEach { assertEquals(it, converter.fromDb(converter.toDb(it))) }
  }

  @Test
  fun nullIsTheStandardAvatar() {
    assertNull(converter.toDb(null))
    assertNull(converter.fromDb(null))
  }

  @Test
  fun theLegacyFlowerNameReadsAsClover8Leaf() {
    assertEquals(AvatarShape.Clover8Leaf, converter.fromDb("Flower"))
  }

  @Test
  fun anUnknownNameFallsBackToTheStandardAvatar() {
    assertNull(converter.fromDb("Whatever"))
  }
}
