package com.alorma.contactnotes.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.R

private const val DefaultWidth = 100f
private const val TitleWidth = 118f
private val TitleOpticalSize = 72.sp

private fun robotoFlex(
  weight: Int,
  width: Float = DefaultWidth,
  opticalSize: TextUnit? = null,
) = Font(
  resId = R.font.roboto_flex,
  weight = FontWeight(weight),
  variationSettings = FontVariation.Settings(
    *listOfNotNull(
      FontVariation.weight(weight),
      FontVariation.width(width),
      opticalSize?.let { FontVariation.opticalSizing(it) },
    ).toTypedArray(),
  ),
)

val RobotoFlex = FontFamily(
  robotoFlex(400),
  robotoFlex(500),
  robotoFlex(600),
  robotoFlex(700),
  robotoFlex(800),
)

private val RobotoFlexTitle = FontFamily(robotoFlex(800, TitleWidth, TitleOpticalSize))

internal val ConversesTypography: Typography = Typography().let { base ->
  base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = RobotoFlex),
    displayMedium = base.displayMedium.copy(fontFamily = RobotoFlex),
    displaySmall = base.displaySmall.copy(fontFamily = RobotoFlex),
    headlineLarge = base.headlineLarge.copy(fontFamily = RobotoFlex),
    headlineMedium = base.headlineMedium.copy(fontFamily = RobotoFlex),
    headlineSmall = base.headlineSmall.copy(fontFamily = RobotoFlex),
    titleLarge = base.titleLarge.copy(fontFamily = RobotoFlex),
    titleMedium = base.titleMedium.copy(fontFamily = RobotoFlex),
    titleSmall = base.titleSmall.copy(fontFamily = RobotoFlex),
    bodyLarge = base.bodyLarge.copy(fontFamily = RobotoFlex),
    bodyMedium = base.bodyMedium.copy(fontFamily = RobotoFlex),
    bodySmall = base.bodySmall.copy(fontFamily = RobotoFlex),
    labelLarge = base.labelLarge.copy(fontFamily = RobotoFlex),
    labelMedium = base.labelMedium.copy(fontFamily = RobotoFlex),
    labelSmall = base.labelSmall.copy(fontFamily = RobotoFlex),
  )
}

private fun titleStyle(size: Int, lineHeight: Int) = TextStyle(
  fontFamily = RobotoFlexTitle,
  fontWeight = FontWeight.ExtraBold,
  fontSize = size.sp,
  lineHeight = lineHeight.sp,
)

/** Títols del handoff: Roboto Flex, pes 800, amplada 118, mida òptica 72. */
val ListTitleStyle = titleStyle(size = 44, lineHeight = 48)
val OnboardingTitleStyle = titleStyle(size = 34, lineHeight = 40)
val ScreenTitleStyle = titleStyle(size = 40, lineHeight = 44)
val PersonNameStyle = titleStyle(size = 30, lineHeight = 36)
val DialogTitleStyle = titleStyle(size = 28, lineHeight = 34)
val SheetTitleStyle = titleStyle(size = 24, lineHeight = 30)
val SectionTitleStyle = titleStyle(size = 22, lineHeight = 28)
