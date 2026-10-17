package com.socialwatch.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Custom bar chart drawn with Compose Canvas.
 * iOS-inspired: rounded bar caps, soft grid, clean labels.
 */
data class BarChartEntry(
    val label: String,
    val segments: List<BarSegment>
)

data class BarSegment(
    val durationMs: Long,
    val color: Color,
    val label: String = ""
)

@Composable
fun SWBarChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 600
) {
    val animProgress = remember { Animatable(0f) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(entries) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(animationDuration))
    }

    if (entries.isEmpty()) return

    val maxTotal = entries.maxOf { entry ->
        entry.segments.sumOf { it.durationMs }
    }.coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val bottomPadding = 36f
        val topPadding = 12f
        val chartAreaHeight = canvasHeight - bottomPadding - topPadding

        val barCount = entries.size
        if (barCount == 0) return@Canvas

        val spacing = canvasWidth / barCount
        // Bar width: proportional to spacing, with a max cap
        val barWidth = (spacing * 0.55f).coerceIn(6f, 44f)

        // Draw horizontal grid lines (3 lines)
        for (i in 1..3) {
            val y = topPadding + chartAreaHeight * (1f - i.toFloat() / 4f)
            drawLine(
                color = gridColor.copy(alpha = 0.25f),
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1f
            )
        }

        // Create label paint once
        val labelPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = if (barCount > 10) 20f else 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // Draw bars and labels
        entries.forEachIndexed { index, entry ->
            val centerX = spacing * index + spacing / 2
            var barBottom = topPadding + chartAreaHeight

            entry.segments.forEach { segment ->
                val segmentHeight = if (maxTotal > 0) {
                    (segment.durationMs.toFloat() / maxTotal) * chartAreaHeight * animProgress.value
                } else 0f

                if (segmentHeight > 1f) {
                    val cornerR = (barWidth / 4).coerceAtMost(8f)
                    drawRoundRect(
                        color = segment.color,
                        topLeft = Offset(centerX - barWidth / 2, barBottom - segmentHeight),
                        size = Size(barWidth, segmentHeight),
                        cornerRadius = CornerRadius(cornerR, cornerR)
                    )
                    barBottom -= segmentHeight
                }
            }

            // Draw bottom label (only non-empty ones)
            if (entry.label.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    entry.label,
                    centerX,
                    canvasHeight - 6f,
                    labelPaint
                )
            }
        }
    }
}
