package com.glasspdf.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val layout = stringPreferencesKey("layout")
        val theme = stringPreferencesKey("theme")
        val sortField = stringPreferencesKey("sort_field")
        val sortDesc = booleanPreferencesKey("sort_desc")
        val showTitles = booleanPreferencesKey("show_titles")
        val showBadge = booleanPreferencesKey("show_badge")
        val coverStyle = stringPreferencesKey("cover_style")
        val arrConf = floatPreferencesKey("arr_conf")
        val arrGroup = intPreferencesKey("arr_group")
        val arrUnfiled = booleanPreferencesKey("arr_unfiled")
        val arrContent = booleanPreferencesKey("arr_content")
        val sources = stringSetPreferencesKey("sources")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            layout = p[Keys.layout].toEnum(d.layout),
            theme = p[Keys.theme].toEnum(d.theme),
            sortField = p[Keys.sortField].toEnum(d.sortField),
            sortDescending = p[Keys.sortDesc] ?: d.sortDescending,
            showTitles = p[Keys.showTitles] ?: d.showTitles,
            showPageBadge = p[Keys.showBadge] ?: d.showPageBadge,
            coverStyle = p[Keys.coverStyle].toEnum(d.coverStyle),
            arrangeMinConfidence = p[Keys.arrConf] ?: d.arrangeMinConfidence,
            arrangeMinGroupSize = p[Keys.arrGroup] ?: d.arrangeMinGroupSize,
            arrangeOnlyUnfiled = p[Keys.arrUnfiled] ?: d.arrangeOnlyUnfiled,
            arrangeUseContent = p[Keys.arrContent] ?: d.arrangeUseContent,
            sources = p[Keys.sources] ?: emptySet()
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit { block(it) }
    }

    suspend fun setLayout(v: LayoutMode) = edit { it[Keys.layout] = v.name }
    suspend fun setTheme(v: ThemeMode) = edit { it[Keys.theme] = v.name }
    suspend fun setSort(field: SortField, desc: Boolean) = edit {
        it[Keys.sortField] = field.name
        it[Keys.sortDesc] = desc
    }
    suspend fun setShowTitles(v: Boolean) = edit { it[Keys.showTitles] = v }
    suspend fun setShowBadge(v: Boolean) = edit { it[Keys.showBadge] = v }
    suspend fun setCoverStyle(v: CoverStyle) = edit { it[Keys.coverStyle] = v.name }
    suspend fun setArrangeConfidence(v: Float) = edit { it[Keys.arrConf] = v }
    suspend fun setArrangeGroupSize(v: Int) = edit { it[Keys.arrGroup] = v }
    suspend fun setArrangeUnfiled(v: Boolean) = edit { it[Keys.arrUnfiled] = v }
    suspend fun setArrangeContent(v: Boolean) = edit { it[Keys.arrContent] = v }
    suspend fun setSources(v: Set<String>) = edit { it[Keys.sources] = v }

    suspend fun current(): AppSettings = settings.first()
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    if (this == null) default else runCatching { enumValueOf<E>(this) }.getOrDefault(default)
