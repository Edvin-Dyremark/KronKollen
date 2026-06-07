package com.kronkollen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kronkollen.ui.budget.BudgetScreen
import com.kronkollen.ui.categories.CategoriesScreen
import com.kronkollen.ui.importflow.ImportScreen
import com.kronkollen.ui.navigation.TopDestination
import com.kronkollen.ui.overview.OverviewScreen
import com.kronkollen.ui.settings.SettingsScreen
import com.kronkollen.ui.theme.KronKollenTheme
import com.kronkollen.ui.transactions.TransactionsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App is dark-only for now: force light system-bar icons over transparent bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            KronKollenTheme(darkTheme = true) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopDestination.entries.forEach { dest ->
                    val selected = currentRoute?.startsWith(dest.route) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(TopDestination.Overview.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.Overview.route,
            // Only consume the bottom (nav bar) inset here; each screen's own TopAppBar
            // handles the status-bar inset, so applying the full innerPadding would double
            // it and leave a big gap above the title.
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(TopDestination.Overview.route) {
                OverviewScreen(
                    onCategoryClick = { categoryId ->
                        navController.navigate("${TopDestination.Transactions.route}?categoryId=$categoryId")
                    },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${TopDestination.Transactions.route}?categoryId={categoryId}",
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val categoryId = entry.arguments?.getString("categoryId")?.toLongOrNull()
                TransactionsScreen(initialCategoryId = categoryId)
            }
            composable(TopDestination.Categories.route) { CategoriesScreen() }
            composable(TopDestination.Budget.route) { BudgetScreen() }
            composable(TopDestination.Import.route) { ImportScreen() }
        }
    }
}
