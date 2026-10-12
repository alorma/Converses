package com.alorma.contactnotes.wear.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Typography
import com.alorma.contactnotes.wear.R

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
  variationSettings =
    FontVariation.Settings(
      *listOfNotNull(
        FontVariation.weight(weight),
        FontVariation.width(width),
        opticalSize?.let { FontVariation.opticalSizing(it) },
      ).toTypedArray(),
    ),
)

val RobotoFlex =
  FontFamily(
    robotoFlex(400),
    robotoFlex(500),
    robotoFlex(600),
    robotoFlex(700),
    robotoFlex(800),
  )

private val RobotoFlexTitle = FontFamily(robotoFlex(800, TitleWidth, TitleOpticalSize))

internal val ConversesWearTypography = Typography(defaultFontFamily = RobotoFlex)

private fun titleStyle(
  size: Int,
  lineHeight: Int,
) = TextStyle(
  fontFamily = RobotoFlexTitle,
  fontWeight = FontWeight.ExtraBold,
  fontSize = size.sp,
  lineHeight = lineHeight.sp,
  letterSpacing = (-0.4).sp,
)

private fun textStyle(
  size: Int,
  lineHeight: Int,
  weight: FontWeight = FontWeight.Normal,
) = TextStyle(
  fontFamily = RobotoFlex,
  fontWeight = weight,
  fontSize = size.sp,
  lineHeight = lineHeight.sp,
)

/** Mides del brief (px del disseny ≈ dp). Els títols: Roboto Flex 800, amplada 118, mida òptica 72. */
val ListTitleStyle = titleStyle(size = 22, lineHeight = 26)
val PersonNameStyle = titleStyle(size = 20, lineHeight = 24)
val WhoTitleStyle = titleStyle(size = 18, lineHeight = 22)
val SavedTitleStyle = titleStyle(size = 28, lineHeight = 32)

val RowTitleStyle = textStyle(size = 15, lineHeight = 20, weight = FontWeight.SemiBold)
val RowSubtitleStyle = textStyle(size = 12, lineHeight = 16)
val EntryTextStyle = textStyle(size = 14, lineHeight = 19)
val DictationTextStyle = textStyle(size = 17, lineHeight = 24)
val OverlineStyle = textStyle(size = 12, lineHeight = 16, weight = FontWeight.Bold)
val ButtonLabelStyle = textStyle(size = 15, lineHeight = 20, weight = FontWeight.Bold)
