package com.alorma.contactnotes.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile: startup, onboarding, adding a person and an entry, the people list,
 * the person detail, Archived, the quick note and Settings.
 *
 * It starts from a clean install: the release app has no sample data, so the test creates its own person through
 * the UI. The UI must be in English (views are found by text / description). Run it with
 * `./gradlew :app:generateReleaseBaselineProfile`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule
  val rule = BaselineProfileRule()

  @Test
  fun generate() =
    rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
      pressHome()
      startActivityAndWait()

      // First launch: onboarding
      if (device.wait(Until.hasObject(By.text("Skip")), UI_TIMEOUT)) {
        device.findObject(By.text("Skip")).click()
      }
      device.expectObject(By.desc("Quick note"), "People list not shown after onboarding")

      // collect() repeats this block in the same install: create the person only the first time
      if (!device.hasObject(By.text(PERSON_NAME))) {
        device.findObject(By.desc("Add a person")).click()
        device.expectObject(By.clazz("android.widget.EditText"), "Person editor not shown").text = PERSON_NAME
        device.findObject(By.text("Save")).click()

        device.expectObject(By.text("Note"), "Person detail not shown after saving").click()
        device.expectObject(By.clazz("android.widget.EditText"), "Entry form not shown").text =
          "I told her I can't make it to dinner on Saturday."
        device.findObject(By.text("Save to timeline")).click()
        device.expectObject(By.text("Timeline"), "Timeline not shown after saving the entry")
        device.waitForIdle()
        device.pressBack()
      }

      // People list with data
      device.expectObject(By.text(PERSON_NAME), "Person not in the list")
      device.scrollFirst()

      // Person detail
      device.findObject(By.text(PERSON_NAME)).click()
      device.expectObject(By.text("Timeline"), "Person detail not shown")
      device.scrollFirst()
      device.pressBack()
      device.expectObject(By.desc("Quick note"), "People list not shown after the detail")

      // Quick note
      device.findObject(By.desc("Quick note")).click()
      device.expectObject(By.clazz("android.widget.EditText"), "Quick note not shown")
      device.pressBack()

      // Archived
      device.findObject(By.desc("Archived")).click()
      device.waitForIdle()

      // Settings
      device.findObject(By.desc("Settings")).click()
      device.waitForIdle()
      device.scrollFirst()
    }

  private fun UiDevice.expectObject(
    selector: androidx.test.uiautomator.BySelector,
    message: String,
  ) = checkNotNull(wait(Until.findObject(selector), UI_TIMEOUT)) { message }

  private fun UiDevice.scrollFirst() {
    findObject(By.scrollable(true))?.apply {
      setGestureMargin(displayWidth / 5)
      fling(Direction.DOWN)
      fling(Direction.UP)
    }
  }

  private companion object {
    const val PACKAGE_NAME = "com.alorma.contactnotes"
    const val PERSON_NAME = "Marta Soler"
    const val UI_TIMEOUT = 5_000L
  }
}
