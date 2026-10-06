package com.alorma.contactnotes.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.R

private const val DefaultWidth = 100f
private const val WideWidth = 118f

private fun robotoFlex(weight: Int, width: Float = DefaultWidth) = Font(
  resId = R.font.roboto_flex,
  weight = FontWeight(weight),
  variationSettings = FontVariation.Settings(
    FontVariation.weight(weight),
    FontVariation.width(width),
  ),
)

val RobotoFlex = FontFamily(
  robotoFlex(400),
  robotoFlex(500),
  robotoFlex(600),
  robotoFlex(700),
  robotoFlex(800),
)

private val WideExtraBold = FontFamily(robotoFlex(800, WideWidth))

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

/** Títol de pantalla: 40 sp, pes 800, amplada 118. */
val ScreenTitleStyle = TextStyle(
  fontFamily = WideExtraBold,
  fontWeight = FontWeight.ExtraBold,
  fontSize = 40.sp,
  lineHeight = 44.sp,
)

/** Nom de la persona al detall: 30 sp, pes 800, amplada 118. */
val PersonNameStyle = TextStyle(
  fontFamily = WideExtraBold,
  fontWeight = FontWeight.ExtraBold,
  fontSize = 30.sp,
  lineHeight = 36.sp,
)
