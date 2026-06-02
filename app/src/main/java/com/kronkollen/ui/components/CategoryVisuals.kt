package com.kronkollen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kronkollen.data.entity.CategoryEntity

const val UNCATEGORIZED_LABEL = "Okategoriserat"
val UncategorizedColor = Color(0xFF9E9E9E)

fun colorOf(category: CategoryEntity?): Color =
    if (category == null) UncategorizedColor else Color(category.colorArgb)

fun nameOf(category: CategoryEntity?): String =
    category?.name ?: UNCATEGORIZED_LABEL

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Int = 12) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color, CircleShape),
    )
}

@Composable
fun CategoryChip(name: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
