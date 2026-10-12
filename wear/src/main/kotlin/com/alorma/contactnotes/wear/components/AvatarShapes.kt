package com.alorma.contactnotes.wear.components

import androidx.compose.ui.geometry.Offset
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.star
import com.alorma.contactnotes.shared.AvatarShape
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Les formes de l'avatar del mòbil (`MaterialShapes` de Material 3), redibuixades aquí perquè el rellotge no depèn de
 * Material 3 de telèfon. Les definicions (punts, arrodoniments i repeticions) són les de `MaterialShapes`; si allà
 * canvien, cal revisar-les. Cada forma surt normalitzada, com les del mòbil.
 */
internal fun AvatarShape.polygon(): RoundedPolygon =
  when (this) {
    AvatarShape.Cookie9Sided -> Cookie9
    AvatarShape.Clover4Leaf -> Clover4
    AvatarShape.Clover8Leaf -> Clover8
    AvatarShape.Sunny -> Sunny
    AvatarShape.Ghostish -> Ghostish
    AvatarShape.PuffyDiamond -> PuffyDiamond
  }

private val Cookie9 =
  RoundedPolygon
    .star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRounding(radius = 0.5f))
    .rotatedNeg90()
    .normalized()

private val Sunny =
  RoundedPolygon
    .star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRounding(radius = 0.15f))
    .normalized()

private val Ghostish =
  customPolygon(
    listOf(
      PointNRound(Offset(0.500f, 0f), CornerRounding(1.000f)),
      PointNRound(Offset(1f, 0f), CornerRounding(1.000f)),
      PointNRound(Offset(1f, 1.140f), CornerRounding(0.254f, 0.106f)),
      PointNRound(Offset(0.575f, 0.906f), CornerRounding(0.253f)),
    ),
    reps = 1,
    mirroring = true,
  ).normalized()

private val Clover4 =
  customPolygon(
    listOf(
      PointNRound(Offset(0.500f, 0.074f)),
      PointNRound(Offset(0.725f, -0.099f), CornerRounding(0.476f)),
    ),
    reps = 4,
    mirroring = true,
  ).normalized()

private val Clover8 =
  customPolygon(
    listOf(
      PointNRound(Offset(0.500f, 0.036f)),
      PointNRound(Offset(0.758f, -0.101f), CornerRounding(0.209f)),
    ),
    reps = 8,
  ).normalized()

private val PuffyDiamond =
  customPolygon(
    listOf(
      PointNRound(Offset(0.870f, 0.130f), CornerRounding(0.146f)),
      PointNRound(Offset(0.818f, 0.357f)),
      PointNRound(Offset(1.000f, 0.332f), CornerRounding(0.853f)),
    ),
    reps = 4,
    mirroring = true,
  ).normalized()

private fun RoundedPolygon.rotatedNeg90(): RoundedPolygon = transformed { x, y -> TransformResult(y, -x) }

private data class PointNRound(
  val o: Offset,
  val r: CornerRounding = CornerRounding.Unrounded,
)

private fun doRepeat(
  points: List<PointNRound>,
  reps: Int,
  center: Offset,
  mirroring: Boolean,
): List<PointNRound> =
  if (mirroring) {
    buildList {
      val angles = points.map { (it.o - center).angleDegrees() }
      val distances = points.map { (it.o - center).getDistance() }
      val actualReps = reps * 2
      val sectionAngle = 360f / actualReps
      repeat(actualReps) {
        points.indices.forEach { index ->
          val i = if (it % 2 == 0) index else points.lastIndex - index
          if (i > 0 || it % 2 == 0) {
            val a =
              (
                sectionAngle * it +
                  if (it % 2 == 0) angles[i] else sectionAngle - angles[i] + 2 * angles[0]
              ).toRadians()
            add(PointNRound(Offset(cos(a), sin(a)) * distances[i] + center, points[i].r))
          }
        }
      }
    }
  } else {
    val np = points.size
    (0 until np * reps).map {
      PointNRound(points[it % np].o.rotateDegrees((it / np) * 360f / reps, center), points[it % np].r)
    }
  }

private fun Offset.rotateDegrees(
  angle: Float,
  center: Offset,
): Offset {
  val a = angle.toRadians()
  val off = this - center
  return Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
}

private fun Float.toRadians(): Float = this / 360f * 2 * PI.toFloat()

private fun Offset.angleDegrees(): Float = atan2(y, x) * 180f / PI.toFloat()

private fun customPolygon(
  points: List<PointNRound>,
  reps: Int,
  center: Offset = Offset(0.5f, 0.5f),
  mirroring: Boolean = false,
): RoundedPolygon {
  val actual = doRepeat(points, reps, center, mirroring)
  return RoundedPolygon(
    vertices = FloatArray(actual.size * 2) { ix -> actual[ix / 2].o.let { if (ix % 2 == 0) it.x else it.y } },
    perVertexRounding = actual.map { it.r },
    centerX = center.x,
    centerY = center.y,
  )
}
