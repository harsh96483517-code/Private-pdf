package com.glasspdf.app.data

enum class LayoutMode(val label: String) {
    GRID2("2 columns"),
    GRID3("3 columns"),
    LIST("List"),
    STRIP("Strip"),
    STRIP_COMPACT("Compact strip"),
    LARGE("Large cover")
}

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class SortField(val label: String) {
    NAME("Name"),
    MODIFIED("Modified"),
    SIZE("Size"),
    MANUAL("Manual")
}

enum class DateFilter(val label: String) {
    ANY("Any time"),
    TODAY("Today"),
    WEEK("Last 7 days"),
    MONTH("Last 30 days")
}

enum class FilterMode(val label: String) { ALL("All"), RECENT("Recent"), FAVORITES("Favorites") }

enum class CoverStyle(val label: String) { BOOK("Book"), FLAT("Flat") }

data class AppSettings(
    val layout: LayoutMode = LayoutMode.GRID2,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val sortField: SortField = SortField.NAME,
    val sortDescending: Boolean = false,
    val showTitles: Boolean = true,
    val showPageBadge: Boolean = true,
    val coverStyle: CoverStyle = CoverStyle.BOOK,
    val arrangeMinConfidence: Float = 0.5f,
    val arrangeMinGroupSize: Int = 2,
    val arrangeOnlyUnfiled: Boolean = true,
    val arrangeUseContent: Boolean = true,
    val sources: Set<String> = emptySet()
)

data class ScanProgress(val done: Int, val total: Int, val current: String)
