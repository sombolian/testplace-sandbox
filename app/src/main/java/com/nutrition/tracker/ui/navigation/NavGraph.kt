package com.nutrition.tracker.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nutrition.tracker.ui.addmeal.AddMealScreen
import com.nutrition.tracker.ui.calendar.CalendarScreen
import com.nutrition.tracker.ui.home.HomeScreen
import com.nutrition.tracker.ui.onboarding.OnboardingData
import com.nutrition.tracker.ui.onboarding.OnboardingScreen
import com.nutrition.tracker.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object AddMeal : Screen("add_meal")
    data object Calendar : Screen("calendar")
    data object Settings : Screen("settings")
}

@Composable
fun NutritionNavGraph(
    navController: NavHostController,
    startDestination: String,
    onOnboardingComplete: (OnboardingData) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200))
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = { data ->
                    // Guard against double-navigation which causes crash
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute == Screen.Onboarding.route) {
                        onOnboardingComplete(data)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onAddMeal = { navController.navigate(Screen.AddMeal.route) },
                onOpenCalendar = { navController.navigate(Screen.Calendar.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.AddMeal.route) {
            AddMealScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onDaySelected = { dateStr ->
                    navController.popBackStack()
                    // Navigate home and set the date via shared approach
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
