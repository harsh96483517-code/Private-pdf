package com.glasspdf.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasspdf.app.data.ThemeMode
import com.glasspdf.app.ui.GlassPdfRoot
import com.glasspdf.app.ui.components.LocalThumbs
import com.glasspdf.app.ui.theme.GlassBackground
import com.glasspdf.app.ui.theme.GlassPdfTheme
import com.glasspdf.app.ui.vm.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GlassPdfApp).container
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalThumbs provides container.thumbnails) {
                GlassPdfTheme(dark) {
                    GlassBackground { GlassPdfRoot(vm) }
                }
            }
        }
    }
}
