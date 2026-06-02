package com.kronkollen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A wedge of the donut: a colour and a positive value. */
data class DonutSlice(val color: Color, val value: Long)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 48f,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth)
        val arcSize = androidx.compose.ui.geometry.Size(
            size.minDimension - strokeWidth,
            size.minDimension - strokeWidth,
        )
        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - arcSize.width) / 2,
            (size.height - arcSize.height) / 2,
        )
        var startAngle = -90f
        for (slice in slices) {
            val sweep = 360f * (slice.value.toFloat() / total)
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += sweep
        }
        if (slices.isEmpty()) {
            drawArc(
                color = UncategorizedColor.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
    }
}

/** A simple vertical bar chart with labels under each bar. */
@Composable
fun BarChart(
    bars: List<Pair<String, Long>>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val max = bars.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val fraction = value.toFloat() / max
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((4 + 120 * fraction).dp)
                        .padding(horizontal = 2.dp),
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height((4 + 120 * fraction).dp)) {
                        drawRoundRect(
                            color = barColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Small square swatch used in chart legends. */
@Composable
fun LegendSwatch(color: Color) {
    Box(modifier = Modifier.size(12.dp).padding(0.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().size(12.dp)) {
            drawRoundRect(color = color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
        }
    }
}
