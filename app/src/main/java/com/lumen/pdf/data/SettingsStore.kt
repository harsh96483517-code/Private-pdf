package com.lumen.pdf.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LibraryLayout { GRID2, GRID3, LIST, STRIP }

/** Local only settings, backed by SharedPreferences. Nothing leaves the device. */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("lumen_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemeMode> = _theme

    private val _layout = MutableStateFlow(readLayout())
    val layout: StateFlow<LibraryLayout> = _layout

    private val _showRecentStrip = MutableStateFlow(prefs.getBoolean(KEY_RECENT_STRIP, true))
    val showRecentStrip: StateFlow<Boolean> = _showRecentStrip

    private val _autoArrangeThreshold = MutableStateFlow(prefs.getFloat(KEY_THRESHOLD, 0.30f))
    val autoArrangeThreshold: StateFlow<Float> = _autoArrangeThreshold

    private val _autoCovers = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COVERS, true))
    val autoCovers: StateFlow<Boolean> = _autoCovers

    private val _manualOrder = MutableStateFlow(prefs.getBoolean(KEY_MANUAL_ORDER, false))
    val manualOrder: StateFlow<Boolean> = _manualOrder

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _theme.value = mode
    }

    fun setLayout(layout: LibraryLayout) {
        prefs.edit().putString(KEY_LAYOUT, layout.name).apply()
        _layout.value = layout
    }

    fun setShowRecentStrip(value: Boolean) {
        prefs.edit().putBoolean(KEY_RECENT_STRIP, value).apply()
        _showRecentStrip.value = value
    }

    fun setAutoArrangeThreshold(value: Float) {
        prefs.edit().putFloat(KEY_THRESHOLD, value).apply()
        _autoArrangeThreshold.value = value
    }

    fun setAutoCovers(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COVERS, value).apply()
        _autoCovers.value = value
    }

    fun setManualOrder(value: Boolean) {
        prefs.edit().putBoolean(KEY_MANUAL_ORDER, value).apply()
        _manualOrder.value = value
    }

    fun exportJson(): String = JSONObject().apply {
        put(KEY_THEME, _theme.value.name)
        put(KEY_LAYOUT, _layout.value.name)
        put(KEY_RECENT_STRIP, _showRecentStrip.value)
        put(KEY_THRESHOLD, _autoArrangeThreshold.value.toDouble())
        put(KEY_AUTO_COVERS, _autoCovers.value)
        put(KEY_MANUAL_ORDER, _manualOrder.value)
    }.toString(2)

    fun importJson(raw: String): Boolean = try {
        val json = JSONObject(raw)
        json.optString(KEY_THEME).takeIf { it.isNotEmpty() }?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()?.let { setTheme(it) }
        }
        json.optString(KEY_LAYOUT).takeIf { it.isNotEmpty() }?.let { name ->
            runCatching { LibraryLayout.valueOf(name) }.getOrNull()?.let { setLayout(it) }
        }
        if (json.has(KEY_RECENT_STRIP)) setShowRecentStrip(json.optBoolean(KEY_RECENT_STRIP, true))
        if (json.has(KEY_AUTO_COVERS)) setAutoCovers(json.optBoolean(KEY_AUTO_COVERS, true))
        if (json.has(KEY_MANUAL_ORDER)) setManualOrder(json.optBoolean(KEY_MANUAL_ORDER, false))
        if (json.has(KEY_THRESHOLD)) setAutoArrangeThreshold(json.optDouble(KEY_THRESHOLD, 0.3).toFloat())
        true
    } catch (t: Throwable) {
        false
    }

    private fun readTheme(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)

    private fun readLayout(): LibraryLayout =
        runCatching { LibraryLayout.valueOf(prefs.getString(KEY_LAYOUT, LibraryLayout.GRID2.name) ?: "GRID2") }
            .getOrDefault(LibraryLayout.GRID2)

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_LAYOUT = "layout"
        const val KEY_RECENT_STRIP = "recent_strip"
        const val KEY_THRESHOLD = "arrange_threshold"
        const val KEY_AUTO_COVERS = "auto_covers"
        const val KEY_MANUAL_ORDER = "manual_order"
    }
}
