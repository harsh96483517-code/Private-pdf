package com.lumen.pdf

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.GlassPanel
import com.lumen.pdf.ui.components.LumenBackground
import com.lumen.pdf.ui.folders.FoldersScreen
import com.lumen.pdf.ui.home.HomeScreen
import com.lumen.pdf.ui.icons.LIcon
import com.lumen.pdf.ui.library.FavoritesScreen
import com.lumen.pdf.ui.library.RecentScreen
import com.lumen.pdf.ui.reader.ReaderScreen
import com.lumen.pdf.ui.search.SearchScreen
import com.lumen.pdf.ui.settings.SettingsScreen
import com.lumen.pdf.ui.theme.LumenTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent?.data
        setContent {
            val vm: LibraryViewModel = viewModel()
            val theme by vm.theme.collectAsStateWithLifecycle()
            val context = LocalContext.current
            LumenTheme(theme) {
                LaunchedEffect(Unit) {
                    vm.messages.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
                LaunchedEffect(incoming) {
                    if (incoming != null) vm.importUris(listOf(incoming))
                }
                LumenRoot(vm)
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("home", "Home", LIcon.Home),
    NavItem("folders", "Folders", LIcon.Folder),
    NavItem("recent", "Recent", LIcon.History),
    NavItem("favorites", "Favorites", LIcon.Favorite),
    NavItem("settings", "Settings", LIcon.Settings)
)

@Composable
fun LumenRoot(vm: LibraryViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "home"
    val showBar = navItems.any { it.route == route }

    LumenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBar) {
                    GlassBottomBar(current = route) { target ->
                        navController.navigate(target) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm = vm,
                            onOpenPdf = { id -> navController.navigate("reader/$id") },
                            onOpenFolder = { id -> navController.navigate("folder/$id") },
                            onSearch = { navController.navigate("search") }
                        )
                    }
                    composable("folders") {
                        FoldersScreen(
                            vm = vm,
                            folderId = null,
                            onOpenFolder = { id -> navController.navigate("folder/$id") },
                            onOpenPdf = { id -> navController.navigate("reader/$id") }
                        )
                    }
                    composable(
                        "folder/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { entry ->
                        FoldersScreen(
                            vm = vm,
                            folderId = entry.arguments?.getLong("id"),
                            onOpenFolder = { id -> navController.navigate("folder/$id") },
                            onOpenPdf = { id -> navController.navigate("reader/$id") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("recent") {
                        RecentScreen(vm) { id -> navController.navigate("reader/$id") }
                    }
                    composable("favorites") {
                        FavoritesScreen(vm) { id -> navController.navigate("reader/$id") }
                    }
                    composable("settings") { SettingsScreen(vm) }
                    composable("search") {
                        SearchScreen(
                            vm = vm,
                            onBack = { navController.popBackStack() },
                            onOpenPdf = { id -> navController.navigate("reader/$id") }
                        )
                    }
                    composable(
                        "reader/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: 0L
                        ReaderScreen(vm = vm, pdfId = id, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBottomBar(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val selected = item.route == current
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(item.route) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.label,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
