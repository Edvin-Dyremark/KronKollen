package com.kronkollen.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level bottom-navigation destinations. */
enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Overview("overview", "Översikt", Icons.Filled.PieChart),
    Transactions("transactions", "Historik", Icons.AutoMirrored.Filled.ListAlt),
    Categories("categories", "Kategorier", Icons.Filled.Sell),
    Import("import", "Importera", Icons.Filled.UploadFile),
}
