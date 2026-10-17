package com.socialwatch.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialwatch.app.ui.theme.ChartColors
import com.socialwatch.app.util.TimeFormatter

/**
 * Multi-segment circular progress ring, similar to iOS Screen Time.
 * Each segment represents a tracked app's proportion of total usage.
 *
 * @param segments List of (label, durationMs, color) for each app
 * @param totalMs Total time across all segments
 */
data class RingSegment(
    val label: String,
    val durationMs: Long,
    val color: Color
)

@Composable
fun CircularProgressRing(
    segments: List<RingSegment>,
    totalMs: Long,
    modifier: Modifier = Modifier,
    ringSize: Dp = 200.dp,
    strokeWidth: Dp = 18.dp,
    maxDurationMs: Long = 8 * 60 * 60 * 1000L  // 8 hours as "full circle"
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Draw segments
            var startAngle = -90f
            segments.forEach { segment ->
                val proportion = if (maxDurationMs > 0) {
                    segment.durationMs.toFloat() / maxDurationMs
                } else 0f
                val sweepAngle = (proportion * 360f).coerceAtMost(360f) * animationProgress.value

                if (sweepAngle > 0.5f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                startAngle += sweepAngle
            }
        }

        // Center text: total time
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = TimeFormatter.formatDuration(totalMs),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Total Today",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
