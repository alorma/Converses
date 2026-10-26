package com.alorma.contactnotes.designsystem

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.contactnotes.designsystem.components.AvatarPreview
import com.alorma.contactnotes.designsystem.components.CollapsibleFactsPreview
import com.alorma.contactnotes.designsystem.components.ColorSwatchesPreview
import com.alorma.contactnotes.designsystem.components.FilterChipPreview
import com.alorma.contactnotes.designsystem.components.FloatingToolbarPreview
import com.alorma.contactnotes.designsystem.components.GroupedListPreview
import com.alorma.contactnotes.designsystem.components.ProBadgePreview
import com.alorma.contactnotes.designsystem.components.SplitButtonPreview
import com.alorma.contactnotes.designsystem.components.TimelinePreview
import com.alorma.contactnotes.designsystem.components.ToggleGroupPreview
import com.android.tools.screenshot.PreviewTest

/** Cada test es renderitza en clar i en fosc, amb l'idioma fixat perquè no depengui de la màquina. */
@Preview(name = "Light", locale = "en")
@Preview(name = "Dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class ComponentPreviews

/** Amb la lletra gran, perquè el text no es talli. */
@Preview(name = "Light large font", locale = "en", fontScale = 1.3f)
annotation class LargeFontComponentPreviews

class ComponentsScreenshotTests {
  @ComponentPreviews
  @LargeFontComponentPreviews
  @PreviewTest
  @Composable
  fun collapsibleFacts() {
    CollapsibleFactsPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun avatar() {
    AvatarPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun proBadge() {
    ProBadgePreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun filterChip() {
    FilterChipPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun toggleGroup() {
    ToggleGroupPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun timeline() {
    TimelinePreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun splitButton() {
    SplitButtonPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun colorSwatches() {
    ColorSwatchesPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun floatingToolbar() {
    FloatingToolbarPreview()
  }

  @ComponentPreviews
  @PreviewTest
  @Composable
  fun groupedList() {
    GroupedListPreview()
  }
}
