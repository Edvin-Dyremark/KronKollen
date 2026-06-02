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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A wedge of the donut: a colour and a positive value. */
data class DonutSlice(val color: Color, val value: Long)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp,
    gapDegrees: Float = 4f,
) {
    val data = slices.filter { it.value > 0 }
    val total = data.sumOf { it.value }.coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val sw = strokeWidth.toPx()
        val diameter = size.minDimension - sw
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

        if (data.isEmpty()) {
            drawArc(
                color = UncategorizedColor.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw),
            )
            return@Canvas
        }

        // Rounded caps + small gaps make the ring read as a designed chart, not a raw arc.
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        val gap = if (data.size > 1) gapDegrees else 0f
        val sweepBudget = 360f - gap * data.size
        var start = -90f + gap / 2f
        for (slice in data) {
            val sweep = sweepBudget * (slice.value.toFloat() / total)
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            start += sweep + gap
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
