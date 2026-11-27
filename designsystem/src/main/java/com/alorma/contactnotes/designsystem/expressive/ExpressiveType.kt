package com.alorma.contactnotes.designsystem.expressive

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.designsystem.theme.robotoFlex

private const val DisplayWidth = 151f
private val DisplayOpticalSize = 144.sp
private val DisplayFamily = FontFamily(robotoFlex(900, DisplayWidth, DisplayOpticalSize))
private val QuoteFamily = FontFamily(robotoFlex(600))
private val OverlineFamily = FontFamily(robotoFlex(700))
private val BodyFamily = FontFamily(robotoFlex(400))

/** Rols tipogràfics de l'app expressiva (Roboto Flex): display ample i molt gruixut, cita, sobretítol i cos. */
object ExpressiveType {
  /** Títol ample i molt gruixut (pes 900, amplada 151). */
  fun display(size: TextUnit): TextStyle =
    TextStyle(
      fontFamily = DisplayFamily,
      fontWeight = FontWeight(900),
      fontSize = size,
      lineHeight = size * 1.05f,
      letterSpacing = (-0.6).sp,
    )

  /** Cita (pes 600). */
  fun quote(size: TextUnit): TextStyle =
    TextStyle(
      fontFamily = QuoteFamily,
      fontWeight = FontWeight(600),
      fontSize = size,
      letterSpacing = (-0.3).sp,
    )

  /** Sobretítol de 12 sp. Qui el crida passa el text a majúscules (`uppercase()`). */
  val overline: TextStyle =
    TextStyle(
      fontFamily = OverlineFamily,
      fontWeight = FontWeight(700),
      fontSize = 12.sp,
      letterSpacing = 1.1.sp,
    )

  val body: TextStyle =
    TextStyle(
      fontFamily = BodyFamily,
      fontWeight = FontWeight(400),
      fontSize = 16.sp,
    )
}

/** Títol display que s'encongeix de [maxSize] a [minSize] i ocupa com a màxim dues línies. */
@Composable
fun WideTitle(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  maxSize: TextUnit = 44.sp,
  minSize: TextUnit = 28.sp,
) {
  Text(
    text = text,
    modifier = modifier,
    color = color,
    style = ExpressiveType.display(maxSize),
    maxLines = 2,
    autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = maxSize),
  )
}
