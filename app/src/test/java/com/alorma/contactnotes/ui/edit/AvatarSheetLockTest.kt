package com.alorma.contactnotes.ui.edit

import com.alorma.contactnotes.shared.AvatarColor
import com.alorma.contactnotes.shared.AvatarShape
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarSheetLockTest {
  @Test
  fun withoutProOnlyCookieAndAutomaticAreFree() {
    val lock = AvatarSheetLock()

    assertTrue(lock.isFree(AvatarShape.Cookie9Sided))
    assertTrue(lock.isFree(null as AvatarColor?))
    AvatarShape.entries.filter { it != AvatarShape.Cookie9Sided }.forEach { assertFalse(lock.isFree(it)) }
    AvatarColor.entries.forEach { assertFalse(lock.isFree(it)) }
  }

  @Test
  fun afterADowngradeTheSavedShapeAndColorStayFree() {
    val lock = AvatarSheetLock(savedShape = AvatarShape.Clover8Leaf, savedColor = AvatarColor.Lavender)

    assertTrue(lock.isFree(AvatarShape.Clover8Leaf))
    assertTrue(lock.isFree(AvatarColor.Lavender))
    assertFalse(lock.isFree(AvatarShape.Sunny))
    assertFalse(lock.isFree(AvatarColor.Rose))
  }

  @Test
  fun aProChoiceIsKeptOnlyWhenTheShapeOrColorIsNotTheDefault() {
    val lock = AvatarSheetLock()

    assertFalse(lock.keepsProChoice(null, null))
    assertFalse(lock.keepsProChoice(AvatarShape.Cookie9Sided, null))
    assertTrue(lock.keepsProChoice(AvatarShape.Clover8Leaf, null))
    assertTrue(lock.keepsProChoice(AvatarShape.Cookie9Sided, AvatarColor.Lavender))
  }
}
