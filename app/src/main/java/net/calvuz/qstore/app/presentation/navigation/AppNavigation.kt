package net.calvuz.qstore.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.calvuz.qstore.app.presentation.ui.articles.add.AddArticleScreen
import net.calvuz.qstore.app.presentation.ui.articles.detail.ArticleDetailScreen
import net.calvuz.qstore.app.presentation.ui.articles.list.ArticleListScreen
import net.calvuz.qstore.app.presentation.ui.camera.CameraScreen
import net.calvuz.qstore.app.presentation.ui.camera.SearchResultsScreen
import net.calvuz.qstore.app.presentation.ui.home.HomeScreen
import net.calvuz.qstore.app.presentation.ui.movements.add.AddMovementScreen
import net.calvuz.qstore.app.presentation.ui.movements.list.MovementListScreen
import net.calvuz.qstore.backup.presentation.BackupRestoreScreen
import net.calvuz.qstore.categories.presentation.ui.categories.edit.CategoryEditScreen
import net.calvuz.qstore.categories.presentation.ui.categories.list.CategoryListScreen
import net.calvuz.qstore.settings.presentation.recognition.RecognitionSettingsScreen
import net.calvuz.qstore.export.presentation.ui.export.ExportScreen
import net.calvuz.qstore.settings.presentation.SettingsScreen
import net.calvuz.qstore.settings.presentation.about.AboutScreen
import net.calvuz.qstore.settings.presentation.display.DisplaySettingsScreen

/**
 * Sealed class per definire tutte le rotte dell'app
 */
sealed class Screen(val route: String) {
    // Home e liste
    data object Home : Screen("home")
    data object ArticleList : Screen("articles")
    data object MovementList : Screen("movements")

    // Camera e ricerca
    data object Camera : Screen("camera")
    data object SearchResults : Screen("search_results/{articleUuids}") {
        fun createRoute(articleUuids: List<String>): String {
            return "search_results/${articleUuids.joinToString(",")}"
        }
    }

    // Articoli
    data object AddArticle : Screen("article/add")
    data object ArticleDetail : Screen("article/{articleId}") {
        fun createRoute(articleId: String) = "article/$articleId"
    }
    data object EditArticle : Screen("article/edit/{articleId}") {
        fun createRoute(articleId: String) = "article/edit/$articleId"
    }

    // Categorie
    data object CategoryList : Screen("categories")
    data object CategoryAdd : Screen("categories/add")
    data object CategoryEdit : Screen("categories/edit/{categoryUuid}") {
        fun createRoute(categoryUuid: String) = "categories/edit/$categoryUuid"
    }

    // Movimenti
    data object AddMovement : Screen("movement/add/{articleId}") {
        fun createRoute(articleId: String) = "movement/add/$articleId"
    }

    // Export
    data object Export: Screen("exports")

    // Backup
    data object BackupRestore: Screen("backup")

    // Impostazioni
    data object Settings : Screen("settings")
    data object AboutSettings: Screen("settings/about")
    data object DisplaySettings : Screen("settings/display")
    data object RecognitionSettings : Screen("settings/recognition")
}

/**
 * Setup della navigazione completa dell'app con settings
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ========== HOME SCREEN ==========
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToArticles = {
                    navController.navigate(Screen.ArticleList.route)
                },
                onNavigateToAddArticle = {
                    navController.navigate(Screen.AddArticle.route)
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToMovements = {
                    navController.navigate(Screen.MovementList.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.Export.route)
                },
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                }
            )
        }

        // ========== ARTICLE LIST SCREEN ==========
        composable(Screen.ArticleList.route) {
            ArticleListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDisplaySettings = {
                    navController.navigate(Screen.DisplaySettings.route)

                },
                onArticleClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                },
                onAddArticleClick = {
                    navController.navigate(Screen.AddArticle.route)
                }
            )
        }

        // ========== ARTICLE DETAIL SCREEN ==========
        composable(
            route = Screen.ArticleDetail.route,
            arguments = listOf(
                navArgument("articleId") {
                    type = NavType.StringType
                }
            )
        ) {
            ArticleDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { articleId ->
                    navController.navigate(Screen.EditArticle.createRoute(articleId))
                },
                onNavigateToAddMovement = { articleId ->
                    navController.navigate(Screen.AddMovement.createRoute(articleId))
                }
            )
        }

        // ========== ADD ARTICLE SCREEN ==========
        composable(Screen.AddArticle.route) {
            AddArticleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== EDIT ARTICLE SCREEN ==========
        composable(
            route = Screen.EditArticle.route,
            arguments = listOf(
                navArgument("articleId") {
                    type = NavType.StringType
                }
            )
        ) {
            AddArticleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== ADD MOVEMENT SCREEN ==========
        composable(
            route = Screen.AddMovement.route,
            arguments = listOf(
                navArgument("articleId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: return@composable

            AddMovementScreen(
                articleId = articleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== CAMERA SCREEN (Ricerca con foto) ==========
        composable(Screen.Camera.route) {
            CameraScreen(
                onSearchResults = { articleUuids ->
                    if (articleUuids.isNotEmpty()) {
                        val route = Screen.SearchResults.createRoute(articleUuids)
                        navController.navigate(route)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== SEARCH RESULTS SCREEN ==========
        composable(
            route = Screen.SearchResults.route,
            arguments = listOf(
                navArgument("articleUuids") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uuidsString = backStackEntry.arguments?.getString("articleUuids") ?: ""
            val articleUuids = uuidsString.split(",").filter { it.isNotBlank() }

            SearchResultsScreen(
                articleUuids = articleUuids,
                onArticleClick = { articleUuid ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleUuid))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== MOVEMENT LIST SCREEN ==========
        composable(Screen.MovementList.route) {
            MovementListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onArticleClick = { articleUuid ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleUuid))
                }
            )
        }

        // ========== SETTINGS SCREEN ==========
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRecognition = {
                    navController.navigate(Screen.RecognitionSettings.route )
                },
                onNavigateToDisplay = {
                    navController.navigate(Screen.DisplaySettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.AboutSettings.route)
                },
                onNavigateToBackupRestore = {
                    navController.navigate(Screen.BackupRestore.route)
                },
                onNavigateToData = {
                    navController.navigate(Screen.Export.route)
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.CategoryList.route)
                },
            )
        }

        // ========== CATEGORY LIST SCREEN ==========
        composable(Screen.CategoryList.route) {
            CategoryListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCategoryClick = { categoryUuid ->
                    navController.navigate(Screen.CategoryEdit.createRoute(categoryUuid))
                },
                onAddCategoryClick = {
                    navController.navigate(Screen.CategoryAdd.route)
                }
            )
        }

        // ========== CATEGORY ADD SCREEN ==========
        composable(Screen.CategoryAdd.route) {
            CategoryEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== CATEGORY EDIT SCREEN ==========
        composable(
            route = Screen.CategoryEdit.route,
            arguments = listOf(
                navArgument("categoryUuid") {
                    type = NavType.StringType
                }
            )
        ) {
            CategoryEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== ABOUT SETTINGS SCREEN ==========
        composable(Screen.AboutSettings.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== DISPLAY SETTINGS SCREEN ==========
        composable(Screen.DisplaySettings.route) {
            DisplaySettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // ========== RECOGNITION SETTINGS SCREEN ==========
        composable(Screen.RecognitionSettings.route) {
            RecognitionSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ========== EXPORT SCREEN ==========
        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ========== BACKUP/RESTORE SCREEN ==========
        composable(Screen.BackupRestore.route) {
            BackupRestoreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}