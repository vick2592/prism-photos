package dev.prism.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.prism.gallery.ui.albums.AlbumDetailScreen
import dev.prism.gallery.ui.albums.AlbumsScreen
import dev.prism.gallery.ui.gallery.GalleryScreen
import dev.prism.gallery.ui.search.SearchScreen
import dev.prism.gallery.ui.settings.SettingsScreen
import dev.prism.gallery.ui.slideshow.SlideshowScreen
import dev.prism.gallery.ui.trash.TrashScreen
import dev.prism.gallery.ui.viewer.ViewerScreen

sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object Albums : Screen("albums")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Viewer : Screen("viewer/{mediaId}?filter={filter}") {
        fun createRoute(mediaId: Long, filter: String = "all") =
            "viewer/$mediaId?filter=${android.net.Uri.encode(filter)}"
    }
    data object AlbumDetail : Screen("album_detail/{bucketId}/{albumName}") {
        fun createRoute(bucketId: String, albumName: String) =
            "album_detail/${android.net.Uri.encode(bucketId)}/${android.net.Uri.encode(albumName)}"
    }
    data object Trash : Screen("trash")
    data object Slideshow : Screen("slideshow")
}

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Gallery, Icons.Filled.PhotoLibrary, "Gallery"),
    BottomNavItem(Screen.Albums, Icons.Filled.VideoLibrary, "Albums"),
    BottomNavItem(Screen.Search, Icons.Filled.Search, "Search"),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, "Settings"),
)

@Composable
fun PrismApp(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in listOf(
        Screen.Viewer.route,
        Screen.AlbumDetail.route,
        Screen.Trash.route,
        Screen.Slideshow.route,
    )

    // Delay showing the nav bar so it doesn't flash over the viewer exit transition
    var routeVisible by remember { mutableStateOf(showBottomBar) }
    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            delay(80)
            routeVisible = true
        } else {
            routeVisible = false
        }
    }

    // Scroll-driven hide: gallery scrolling down hides the bar; scrolling up restores it.
    var scrollHideNavBar by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.Gallery.route) scrollHideNavBar = false
    }
    val bottomBarVisible = routeVisible && !scrollHideNavBar

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = bottomBarVisible,
                enter = fadeIn(animationSpec = tween(150)) +
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(120)) +
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150)),
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Gallery.route,
        ) {
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate(Screen.Viewer.createRoute(mediaId, "gallery"))
                    },
                    onSlideshowClick = { navController.navigate(Screen.Slideshow.route) },
                    contentPadding = paddingValues,
                    onScrollDirectionChange = { scrollingDown -> scrollHideNavBar = scrollingDown },
                )
            }
            composable(Screen.Albums.route) {
                AlbumsScreen(
                    contentPadding = paddingValues,
                    onAlbumClick = { bucketId, albumName ->
                        navController.navigate(Screen.AlbumDetail.createRoute(bucketId, albumName))
                },
                onTrashClick = { navController.navigate(Screen.Trash.route) },
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate(Screen.Viewer.createRoute(mediaId, "all"))
                    },
                    contentPadding = paddingValues,
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(contentPadding = paddingValues)
            }
            composable(
                route = Screen.Viewer.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.LongType },
                    navArgument("filter") {
                        type = NavType.StringType
                        defaultValue = "all"
                    },
                ),
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                ViewerScreen(
                    mediaId = mediaId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSlideshow = { navController.navigate(Screen.Slideshow.route) },
                )
            }
            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(
                    navArgument("bucketId") { type = NavType.StringType },
                    navArgument("albumName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val bucketId = android.net.Uri.decode(
                    backStackEntry.arguments?.getString("bucketId") ?: ""
                )
                val albumName = android.net.Uri.decode(
                    backStackEntry.arguments?.getString("albumName") ?: ""
                )
                AlbumDetailScreen(
                    albumName = albumName,
                    onMediaClick = { mediaId ->
                        navController.navigate(
                            Screen.Viewer.createRoute(mediaId, "bucket:$bucketId")
                        )
                    },
                    onNavigateBack = { navController.navigateUp() },
                )
            }
            composable(Screen.Trash.route) {
                TrashScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.Slideshow.route) {
                SlideshowScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}
