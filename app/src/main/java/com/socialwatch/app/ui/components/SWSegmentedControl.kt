package com.socialwatch.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS-style pill segmented control with sliding indicator.
 * Uses proportional sizing so the indicator always aligns with the labels.
 */
@Composable
fun SWSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val controlHeight = 36.dp
    val itemCount = items.size

    // Animate the indicator position as a fraction (0f to 1f per segment)
    val indicatorFraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "indicator_fraction"
    )

    val pillColor = MaterialTheme.colorScheme.surface
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(controlHeight + 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackColor)
            .padding(4.dp)
            // Draw the sliding indicator pill behind the text
            .drawBehind {
                val segmentWidth = size.width / itemCount
                val pillPadding = 2f
                val pillWidth = segmentWidth - pillPadding * 2
                val pillHeight = size.height - pillPadding * 2
                val pillX = indicatorFraction * segmentWidth + pillPadding

                // Shadow effect (subtle)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.08f),
                    topLeft = Offset(pillX, pillPadding + 1f),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                // White pill
                drawRoundRect(
                    color = pillColor,
                    topLeft = Offset(pillX, pillPadding),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(controlHeight)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onSelectionChange(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == selectedIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
