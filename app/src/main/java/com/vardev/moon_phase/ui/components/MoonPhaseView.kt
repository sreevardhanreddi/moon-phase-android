package com.vardev.moon_phase.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun MoonPhaseView(
    phase: Double,
    illumination: Double,
    isWaxing: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    moonColor: Color = Color(0xFFF5F5DC),
    shadowColor: Color = Color.Black,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val glowColor = if (isDarkTheme) {
        Color(0xFFF5F5DC) // Warm cream glow in dark mode
    } else {
        Color(0xFF000000) // Dark shadow in light mode
    }

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2 * 0.85f // Slightly smaller to fit glow
        val center = Offset(this.size.width / 2, this.size.height / 2)

        // Draw outer glow/shadow layers
        drawMoonGlow(
            center = center,
            baseRadius = radius,
            glowColor = glowColor,
            isDarkTheme = isDarkTheme
        )

        // Draw the lit portion of the moon (base circle)
        drawCircle(
            color = moonColor,
            radius = radius,
            center = center
        )

        // Draw shadow based on phase
        drawMoonShadow(
            phase = phase,
            illumination = illumination,
            isWaxing = isWaxing,
            radius = radius,
            center = center,
            shadowColor = shadowColor
        )
    }
}

private fun DrawScope.drawMoonGlow(
    center: Offset,
    baseRadius: Float,
    glowColor: Color,
    isDarkTheme: Boolean
) {
    val layers = 5
    val maxGlowRadius = baseRadius * 1.15f

    for (i in layers downTo 1) {
        val fraction = i.toFloat() / layers
        val layerRadius = baseRadius + (maxGlowRadius - baseRadius) * fraction
        val alpha = if (isDarkTheme) {
            // Softer glow for dark theme
            0.15f * (1f - fraction + 0.2f)
        } else {
            // Subtle shadow for light theme
            0.08f * (1f - fraction + 0.2f)
        }

        drawCircle(
            color = glowColor.copy(alpha = alpha),
            radius = layerRadius,
            center = center
        )
    }
}

private fun DrawScope.drawMoonShadow(
    phase: Double,
    illumination: Double,
    isWaxing: Boolean,
    radius: Float,
    center: Offset,
    shadowColor: Color
) {
    // Full moon - no shadow needed
    if (phase > 0.48 && phase < 0.52) return

    // New moon - full shadow
    if (phase < 0.02 || phase > 0.98) {
        drawCircle(
            color = shadowColor,
            radius = radius,
            center = center
        )
        return
    }

    val shadowWidth = radius * abs(1 - 2 * illumination).toFloat()
    val path = Path()

    if (isWaxing) {
        // Shadow on the left side, shrinking as moon waxes
        if (illumination < 0.5) {
            // Crescent phase - shadow covers most of moon
            path.moveTo(center.x, center.y - radius)
            // Left arc (outer edge of shadow on left)
            path.arcTo(
                rect = Rect(
                    center.x - shadowWidth,
                    center.y - radius,
                    center.x + shadowWidth,
                    center.y + radius
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // Right arc back to start (inner edge following moon curve)
            path.arcTo(
                rect = Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
        } else {
            // Gibbous phase - shadow is smaller on left
            path.moveTo(center.x, center.y - radius)
            // Left arc (convex shadow edge)
            path.arcTo(
                rect = Rect(
                    center.x - shadowWidth,
                    center.y - radius,
                    center.x + shadowWidth,
                    center.y + radius
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Continue along moon's left edge back to top
            path.arcTo(
                rect = Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
        }
    } else {
        // Waning - shadow on the right side, growing
        if (illumination > 0.5) {
            // Gibbous waning - small shadow on right
            path.moveTo(center.x, center.y - radius)
            // Right arc (shadow edge)
            path.arcTo(
                rect = Rect(
                    center.x - shadowWidth,
                    center.y - radius,
                    center.x + shadowWidth,
                    center.y + radius
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // Left arc back (moon edge)
            path.arcTo(
                rect = Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        } else {
            // Crescent waning - large shadow on right
            path.moveTo(center.x, center.y - radius)
            // Right arc
            path.arcTo(
                rect = Rect(
                    center.x - shadowWidth,
                    center.y - radius,
                    center.x + shadowWidth,
                    center.y + radius
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Moon edge back
            path.arcTo(
                rect = Rect(
                    center.x - radius,
                    center.y - radius,
                    center.x + radius,
                    center.y + radius
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
    }

    path.close()
    drawPath(path = path, color = shadowColor)
}
