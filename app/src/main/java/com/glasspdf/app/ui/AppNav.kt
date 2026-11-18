package com.glasspdf.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glasspdf.app.ui.screens.ArrangeScreen
import com.glasspdf.app.ui.screens.FavoritesScreen
import com.glasspdf.app.ui.screens.FoldersScreen
import com.glasspdf.app.ui.screens.HomeScreen
import com.glasspdf.app.ui.screens.ReaderScreen
import com.glasspdf.app.ui.screens.RecentScreen
import com.glasspdf.app.ui.screens.SearchScreen
import com.glasspdf.app.ui.screens.SettingsScreen
import com.glasspdf.app.ui.theme.PillShape
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.MainViewModel

object Routes {
    const val HOME = "home"
    const val FOLDERS = "folders"
    const val RECENT = "recent"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val ARRANGE = "arrange"
    const val READER = "reader/{pdfId}"
    fun reader(id: Long) = "reader/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.FOLDERS, "Folders", Icons.Rounded.Folder),
    Tab(Routes.RECENT, "Recent", Icons.Rounded.History),
    Tab(Routes.FAVORITES, "Favorites", Icons.Rounded.Star),
    Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
)

@Composable
fun GlassPdfRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = Tabs.any { it.route == route }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    fun openTab(r: String) {
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.97f) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.97f) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm,
                    onOpenPdf = { nav.navigate(Routes.reader(it)) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onArrange = { nav.navigate(Routes.ARRANGE) }
                )
            }
            composable(Routes.FOLDERS) { FoldersScreen(vm, onOpenPdf = { nav.navigate(Routes.reader(it)) }) }
            composable(Routes.RECENT) { RecentScreen(vm, onOpenPdf = { nav.navigate(Routes.reader(it)) }) }
            composable(Routes.FAVORITES) { FavoritesScreen(vm, onOpenPdf = { nav.navigate(Routes.reader(it)) }) }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
            composable(Routes.SEARCH) {
                SearchScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onOpenPdf = { nav.navigate(Routes.reader(it)) },
                    onOpenFolder = { id ->
                        vm.pendingFolder.value = id
                        nav.popBackStack()
                        openTab(Routes.FOLDERS)
                    }
                )
            }
            composable(Routes.ARRANGE) {
                ArrangeScreen(
                    onBack = { nav.popBackStack() },
                    onShowFolders = {
                        nav.popBackStack()
                        openTab(Routes.FOLDERS)
                    }
                )
            }
            composable(
                Routes.READER,
                arguments = listOf(navArgument("pdfId") { type = NavType.LongType })
            ) { ReaderScreen(onBack = { nav.popBackStack() }) }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            GlassNavBar(current = route, onSelect = { openTab(it) })
        }
    }
}

@Composable
private fun GlassNavBar(current: String?, onSelect: (String) -> Unit) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = bottom + 10.dp)
            .fillMaxWidth()
            .glass(PillShape, strong = true)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Tabs.forEach { t ->
            val selected = current == t.route
            val pillColor by animateColorAsState(if (selected) primary else Color.Transparent, tween(260), label = "tabBg")
            val contentColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(260), label = "tabFg"
            )
            Column(
                Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(pillColor)
                    .clickable { onSelect(t.route) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(t.icon, t.label, tint = contentColor, modifier = Modifier.size(22.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = contentColor)
            }
        }
    }
}
