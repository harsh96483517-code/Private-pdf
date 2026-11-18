package com.glasspdf.app.domain

import com.glasspdf.app.data.AppSettings
import com.glasspdf.app.data.DateFilter
import com.glasspdf.app.data.FilterMode
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.SortField
import com.glasspdf.app.data.title
import com.glasspdf.app.util.naturalCompare

fun sortPdfs(list: List<PdfEntity>, s: AppSettings): List<PdfEntity> {
    val cmp: Comparator<PdfEntity> = when (s.sortField) {
        SortField.NAME -> Comparator<PdfEntity> { a, b -> naturalCompare(a.title, b.title) }
        SortField.MODIFIED -> Comparator<PdfEntity> { a, b -> a.modifiedAt.compareTo(b.modifiedAt) }
        SortField.SIZE -> Comparator<PdfEntity> { a, b -> a.sizeBytes.compareTo(b.sizeBytes) }
        SortField.MANUAL -> Comparator<PdfEntity> { a, b ->
            val c = a.sortOrder.compareTo(b.sortOrder)
            if (c != 0) c else naturalCompare(a.title, b.title)
        }
    }
    val sorted = list.sortedWith(cmp)
    return if (s.sortDescending) sorted.reversed() else sorted
}

fun filterPdfs(list: List<PdfEntity>, mode: FilterMode, date: DateFilter): List<PdfEntity> {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000
    val cutoff = when (date) {
        DateFilter.ANY -> 0L
        DateFilter.TODAY -> now - day
        DateFilter.WEEK -> now - 7 * day
        DateFilter.MONTH -> now - 30 * day
    }
    return list.filter { p ->
        val modeOk = when (mode) {
            FilterMode.ALL -> true
            FilterMode.RECENT -> p.lastOpenedAt > 0
            FilterMode.FAVORITES -> p.isFavorite
        }
        modeOk && (cutoff == 0L || p.modifiedAt >= cutoff)
    }
}

fun recentPdfs(list: List<PdfEntity>, limit: Int = Int.MAX_VALUE): List<PdfEntity> =
    list.filter { it.lastOpenedAt > 0 }.sortedByDescending { it.lastOpenedAt }.take(limit)
