package com.glasspdf.app.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return if (i == 0) "${v.toInt()} ${units[i]}" else String.format(Locale.US, "%.1f %s", v, units[i])
}

fun formatDate(millis: Long): String =
    if (millis <= 0) "—" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** Compares strings the way people expect: "Part 2" < "Part 10". */
val NaturalOrder: Comparator<String> = Comparator { a, b -> naturalCompare(a, b) }

fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var si = i
            while (si < a.length && a[si] == '0') si++
            var sj = j
            while (sj < b.length && b[sj] == '0') sj++
            var ei = si
            while (ei < a.length && a[ei].isDigit()) ei++
            var ej = sj
            while (ej < b.length && b[ej].isDigit()) ej++
            val la = ei - si
            val lb = ej - sj
            if (la != lb) return la.compareTo(lb)
            val cmp = a.substring(si, ei).compareTo(b.substring(sj, ej))
            if (cmp != 0) return cmp
            i = ei
            j = ej
        } else {
            val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
            if (cmp != 0) return cmp
            i++
            j++
        }
    }
    return (a.length - i).compareTo(b.length - j)
}
