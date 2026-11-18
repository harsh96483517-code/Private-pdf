package com.glasspdf.app.domain

import com.glasspdf.app.util.naturalCompare
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Input for the arranger. [title] = custom or embedded document title, [text] = first pages text. */
data class ArrangeDoc(
    val id: Long,
    val fileName: String,
    val title: String?,
    val text: String,
    val modifiedAt: Long
)

data class ArrangeConfig(
    val minConfidence: Float = 0.5f,
    val minGroupSize: Int = 2,
    val useContent: Boolean = true
)

data class ProposedItem(
    val docId: Long,
    val confidence: Float,
    val reason: String,
    val orderLabel: String?
)

data class ProposedGroup(
    val key: String,
    val name: String,
    val items: List<ProposedItem>,
    val isUnsorted: Boolean = false,
    val orderNote: String = ""
) {
    val confidence: Float
        get() = if (items.isEmpty()) 0f else items.map { it.confidence }.average().toFloat()
}

// ---------------------------------------------------------------- vocabulary

private val INTRO_WORDS = setOf("introduction", "intro", "preface", "beginner", "beginners", "prologue", "overview")
private val BASIC_WORDS = setOf("basic", "basics", "fundamental", "fundamentals", "foundation", "foundations", "essentials", "primer")
private val PRACTICE_WORDS = setOf(
    "exercise", "exercises", "practice", "questions", "question", "worksheet", "worksheets",
    "problems", "mcq", "mcqs", "assignment", "assignments", "drill", "workbook"
)
private val ADVANCED_WORDS = setOf("advanced", "expert", "mastery", "masterclass")
private val REVISION_WORDS = setOf("revision", "summary", "formula", "formulas", "formulae", "quick", "short", "recap", "review")
private val EXAM_WORDS = setOf(
    "test", "tests", "exam", "exams", "paper", "papers", "pyq", "mock", "solution", "solutions",
    "answer", "answers", "key", "final"
)

private fun splitWords(s: String): List<String> =
    s.replace(Regex("([a-z])([A-Z])"), "\$1 \$2")
        .lowercase()
        .split(Regex("[^\\p{L}\\p{M}\\p{Nd}]+"))
        .filter { it.isNotEmpty() }

/** Very light stemming so "maths", "math", "physics" and "physic" meet in the middle. */
private fun norm(w: String): String {
    if (w.length > 4 && w.endsWith("ies")) return w.dropLast(3) + "y"
    if (w.length > 3 && w.endsWith("s") && !w.endsWith("ss")) return w.dropLast(1)
    return w
}

private val STOP: Set<String> = run {
    val base = setOf(
        "the", "a", "an", "of", "and", "for", "to", "in", "on", "by", "with", "new", "old", "copy", "pdf",
        "edition", "ed", "book", "books", "ebook", "notes", "note", "part", "pt", "chapter", "chap", "ch",
        "vol", "volume", "unit", "lesson", "module", "latest", "updated", "version", "scan", "scanned",
        "free", "download", "full", "complete", "class", "std", "standard", "grade", "year", "guide",
        "manual", "handbook", "tutorial", "cheatsheet", "cheat", "sheet", "page", "pages", "file", "doc",
        "document", "docs", "vs", "set", "lecture", "topic", "section", "sec", "day", "week", "level", "bk"
    ) + INTRO_WORDS + BASIC_WORDS + PRACTICE_WORDS + ADVANCED_WORDS + REVISION_WORDS + EXAM_WORDS
    base + base.map { norm(it) }
}

private val COMMON: Set<String> = setOf(
    "this", "that", "with", "from", "have", "will", "your", "which", "their", "there", "were", "been",
    "also", "into", "than", "then", "them", "these", "those", "what", "when", "where", "while", "about",
    "after", "before", "other", "some", "such", "only", "over", "more", "most", "very", "can", "may",
    "not", "are", "was", "has", "had", "its", "our", "you", "all", "any", "but", "for", "and", "the",
    "would", "could", "should", "each", "one", "two", "use", "used", "using", "page", "pdf", "copyright",
    "reserved", "rights", "published", "publisher", "printed", "isbn", "www", "http", "https", "com"
)

private class Subject(val name: String, raw: List<String>) {
    val words: Set<String> = raw.map { norm(it) }.toSet()
}

private val SUBJECTS: List<Subject> = listOf(
    Subject("Mathematics", listOf("math", "maths", "mathematics", "mathematical", "algebra", "geometry", "calculus",
        "trigonometry", "arithmetic", "statistics", "probability", "mensuration", "logarithm", "polynomial",
        "equation", "integration", "differentiation", "matrices", "determinant", "ganit", "गणित")),
    Subject("Physics", listOf("physics", "mechanics", "optics", "thermodynamics", "electromagnetism", "magnetism",
        "kinematics", "quantum", "relativity", "electrostatics", "oscillation", "nuclear", "bhautik", "भौतिकी", "भौतिक")),
    Subject("Chemistry", listOf("chemistry", "chemical", "organic", "inorganic", "stoichiometry", "periodic",
        "electrochemistry", "polymer", "biochemistry", "rasayan", "रसायन")),
    Subject("Biology", listOf("biology", "botany", "zoology", "genetics", "ecology", "anatomy", "physiology",
        "microbiology", "biotechnology", "evolution", "jeev", "जीव")),
    Subject("Computer Science", listOf("programming", "python", "java", "kotlin", "javascript", "algorithm",
        "algorithms", "database", "sql", "computer", "software", "coding", "html", "css", "android", "linux",
        "networking", "cpp", "typescript")),
    Subject("History", listOf("history", "ancient", "medieval", "dynasty", "empire", "revolution", "इतिहास")),
    Subject("Geography", listOf("geography", "climate", "atlas", "geology", "monsoon", "भूगोल")),
    Subject("English", listOf("english", "grammar", "literature", "vocabulary", "essay", "poem", "poetry", "novel",
        "comprehension")),
    Subject("Hindi", listOf("hindi", "vyakaran", "हिंदी", "हिन्दी")),
    Subject("Sanskrit", listOf("sanskrit", "संस्कृत")),
    Subject("Economics", listOf("economics", "economy", "microeconomics", "macroeconomics", "gdp", "अर्थशास्त्र")),
    Subject("Accounting & Business", listOf("accounting", "accountancy", "bookkeeping", "finance", "business",
        "management", "marketing", "commerce", "entrepreneurship")),
    Subject("Science", listOf("science", "evs", "environmental")),
    Subject("Political Science", listOf("political", "civics", "constitution", "polity", "democracy", "politics", "राजनीति")),
    Subject("Psychology", listOf("psychology", "psychological", "cognitive")),
    Subject("Law", listOf("law", "legal", "jurisprudence"))
)

// ---------------------------------------------------------------- internals

private class Prep(
    val doc: ArrangeDoc,
    val nameWords: List<String>,
    val display: Map<String, String>,
    val titleWords: Set<String>,
    val textTokens: List<String>
) {
    val nameSet: Set<String> = nameWords.toSet()
    val tokenCount: Int = textTokens.size
    var vec: Map<String, Double> = emptyMap()
    var subject: String? = null
    var subjectConf: Float = 0f
    var reason: String = ""
}

private class Member(val prep: Prep, val conf: Float, val reason: String)

private class RawGroup(val key: String, val name: String, val members: MutableList<Member>)

private fun ArrangeDoc.shown(): String = fileName.removeSuffix(".pdf").removeSuffix(".PDF")

private fun prep(doc: ArrangeDoc, useContent: Boolean): Prep {
    val display = LinkedHashMap<String, String>()
    for (w in splitWords(doc.shown())) {
        val n = norm(w)
        if (n.length >= 2 && !n.all { it.isDigit() } && n !in STOP) display.putIfAbsent(n, w)
    }
    val title = doc.title?.let { t ->
        splitWords(t).map { norm(it) }
            .filter { it.length >= 2 && it !in STOP && !it.all { c -> c.isDigit() } }
            .toSet()
    } ?: emptySet()
    val tokens = if (useContent && doc.text.isNotBlank()) {
        splitWords(doc.text.take(8000)).map { norm(it) }
            .filter { it.length in 3..24 && it !in STOP && it !in COMMON && it.none { c -> c.isDigit() } }
    } else emptyList()
    return Prep(doc, display.keys.toList(), display, title, tokens)
}

private fun buildVectors(preps: List<Prep>) {
    val n = preps.count { it.textTokens.isNotEmpty() }
    if (n == 0) return
    val df = HashMap<String, Int>()
    for (p in preps) for (t in p.textTokens.toSet()) df[t] = (df[t] ?: 0) + 1
    for (p in preps) {
        if (p.textTokens.isEmpty()) continue
        val tf = HashMap<String, Int>()
        for (t in p.textTokens) tf[t] = (tf[t] ?: 0) + 1
        val weights = HashMap<String, Double>()
        for ((t, c) in tf) {
            weights[t] = (1.0 + ln(c.toDouble())) * (ln((n + 1.0) / ((df[t] ?: 1) + 1.0)) + 1.0)
        }
        val top = weights.entries.sortedByDescending { it.value }.take(60)
        val len = sqrt(top.sumOf { it.value * it.value })
        if (len > 0.0) p.vec = top.associate { it.key to it.value / len }
    }
}

private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val small = if (a.size <= b.size) a else b
    val large = if (a.size <= b.size) b else a
    var dot = 0.0
    for ((k, v) in small) {
        val o = large[k]
        if (o != null) dot += v * o
    }
    return dot
}

private fun assignSubject(p: Prep, useContent: Boolean) {
    var best: String? = null
    var top = 0.0
    var second = 0.0
    var nameHit = false
    for (s in SUBJECTS) {
        val nh = p.nameWords.count { it in s.words }
        val th = p.titleWords.count { it in s.words }
        val hits = if (useContent) p.textTokens.count { it in s.words } else 0
        val score = 4.0 * min(nh, 2) + 3.0 * min(th, 2) + min(hits * 0.25, 8.0)
        if (score > top) {
            second = top
            top = score
            best = s.name
            nameHit = nh > 0 || th > 0
        } else if (score > second) {
            second = score
        }
    }
    if (best != null && top >= 3.0) {
        val margin = top - second
        var conf = 0.5 + min(top, 10.0) / 10.0 * 0.3 + min(margin, 6.0) / 6.0 * 0.18
        if (!nameHit) conf *= 0.85
        p.subject = best
        p.subjectConf = min(conf, 0.98).toFloat()
        p.reason = if (nameHit) "Name or title matches $best" else "Content looks like $best"
    }
}

private fun pairScore(a: Prep, b: Prep, useCos: Boolean): Pair<Double, String> {
    var shared = 0
    var sharedWord = ""
    val small = if (a.nameSet.size <= b.nameSet.size) a.nameSet else b.nameSet
    val large = if (a.nameSet.size <= b.nameSet.size) b.nameSet else a.nameSet
    for (w in small) {
        if (w.length >= 3 && w in large) {
            shared++
            if (w.length > sharedWord.length) sharedWord = w
        }
    }
    val denom = min(a.nameSet.size, b.nameSet.size)
    val nameSim = if (denom == 0) 0.0 else shared.toDouble() / denom
    val cos = if (useCos && a.tokenCount >= 40 && b.tokenCount >= 40) cosine(a.vec, b.vec) else 0.0
    var score = min(1.0, 0.9 * nameSim + 0.5 * cos)
    var why = if (shared > 0) "Shared name word “${a.display[sharedWord] ?: sharedWord}”" else ""
    if (cos >= 0.55 && cos > score) {
        score = cos
        why = "Similar content (${(cos * 100).toInt()}%)"
    } else if (cos >= 0.2 && shared > 0) {
        why += " + similar content (${(cos * 100).toInt()}%)"
    }
    return score to why
}

private fun clusterLeftovers(items: List<Prep>, cfg: ArrangeConfig): Pair<List<RawGroup>, List<Prep>> {
    val n = items.size
    if (n < 2) return emptyList<RawGroup>() to items
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) r = parent[r]
        var c = x
        while (parent[c] != r) {
            val nx = parent[c]
            parent[c] = r
            c = nx
        }
        return r
    }
    val bestScore = DoubleArray(n)
    val bestReason = Array(n) { "" }
    val useCos = cfg.useContent && n <= 2500
    for (i in 0 until n) {
        for (j in i + 1 until n) {
            val (score, why) = pairScore(items[i], items[j], useCos)
            if (score >= 0.5) {
                parent[find(i)] = find(j)
                if (score > bestScore[i]) {
                    bestScore[i] = score
                    bestReason[i] = why
                }
                if (score > bestScore[j]) {
                    bestScore[j] = score
                    bestReason[j] = why
                }
            }
        }
    }
    val clusters = LinkedHashMap<Int, MutableList<Int>>()
    for (i in 0 until n) clusters.getOrPut(find(i)) { mutableListOf() }.add(i)

    val groups = ArrayList<RawGroup>()
    val singles = ArrayList<Prep>()
    val usedNames = HashSet<String>()
    for ((_, idx) in clusters) {
        if (idx.size < 2) {
            singles.add(items[idx[0]])
            continue
        }
        val members = idx.map { i ->
            val conf = (0.35 + 0.6 * bestScore[i]).coerceIn(0.4, 0.95).toFloat()
            Member(items[i], conf, bestReason[i].ifEmpty { "Related to other PDFs in this group" })
        }.toMutableList()
        var name = clusterName(idx.map { items[it] })
        var unique = name
        var k = 2
        while (!usedNames.add(unique.lowercase())) {
            unique = "$name $k"
            k++
        }
        name = unique
        groups.add(RawGroup("cluster:$name", name, members))
    }
    return groups to singles
}

private fun clusterName(members: List<Prep>): String {
    val freq = HashMap<String, Int>()
    for (m in members) for (w in m.nameSet) freq[w] = (freq[w] ?: 0) + 1
    val need = max(2, ceil(members.size * 0.6).toInt())
    val first = members.first()
    val common = first.nameWords.filter { (freq[it] ?: 0) >= need }
    val words = if (common.isNotEmpty()) common.map { first.display[it] ?: it }
    else {
        val topWord = freq.entries.maxByOrNull { it.value }?.key
        if (topWord == null) emptyList<String>() else listOf(members.firstNotNullOfOrNull { it.display[topWord] } ?: topWord)
    }
    return if (words.isEmpty()) "Related PDFs" else words.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}

// ---------------------------------------------------------------- sequence / order

private const val NUM_TOKEN = "(\\d{1,4}|[ivx]{1,4}(?![a-z])|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)"
private val PART_RE = Regex(
    "\\b(part|pt|chapter|chap|ch|unit|lesson|module|lecture|topic|section|sec|day|week|level|book|bk)[\\s._#:-]*$NUM_TOKEN",
    RegexOption.IGNORE_CASE
)
private val VOL_RE = Regex("\\b(volume|vol)[\\s._#:-]*$NUM_TOKEN", RegexOption.IGNORE_CASE)
private val LEAD_RE = Regex("^\\s*(\\d{1,3})(?=[\\s._)\\-])")
private val TRAIL_RE = Regex("(?:^|[\\s._-])(\\d{1,3})\\s*\\)?\\s*$")
private val CONTENT_RE = Regex("\\b(chapter|unit|lesson|part|module)\\s+(\\d{1,3})\\b", RegexOption.IGNORE_CASE)
private val SHORT_KW = setOf("pt", "ch", "sec", "bk", "chap")
private val NOT_A_PART = setOf("class", "std", "grade", "standard", "year", "sem", "semester", "edition", "ed", "version", "v")

private val ROMAN = mapOf(
    "i" to 1, "ii" to 2, "iii" to 3, "iv" to 4, "v" to 5, "vi" to 6, "vii" to 7, "viii" to 8,
    "ix" to 9, "x" to 10, "xi" to 11, "xii" to 12
)
private val WORD_NUM = mapOf(
    "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
    "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
)

private fun tokenToInt(tok: String): Int? =
    tok.toIntOrNull() ?: ROMAN[tok.lowercase()] ?: WORD_NUM[tok.lowercase()]

private class SeqInfo(val stage: Int, val volume: Int, val number: Int?, val label: String?)

private fun parseSequence(doc: ArrangeDoc, useContent: Boolean): SeqInfo {
    val base = doc.shown()
    var number: Int? = null
    var keyword: String? = null

    for (m in PART_RE.findAll(base)) {
        val kw = m.groupValues[1].lowercase()
        val tok = m.groupValues[2]
        if (kw in SHORT_KW && tok.toIntOrNull() == null) continue
        val v = tokenToInt(tok) ?: continue
        number = v
        keyword = kw
        break
    }
    val volume = VOL_RE.find(base)?.let { tokenToInt(it.groupValues[2]) } ?: 0
    if (number == null) {
        val lead = LEAD_RE.find(base)
        if (lead != null) {
            number = lead.groupValues[1].toIntOrNull()
            keyword = "#"
        }
    }
    if (number == null) {
        val trail = TRAIL_RE.find(base)
        if (trail != null) {
            val before = base.substring(0, trail.groups[1]!!.range.first)
                .split(Regex("[^\\p{L}]+")).lastOrNull { it.isNotEmpty() }?.lowercase()
            if (before == null || before !in NOT_A_PART) {
                number = trail.groupValues[1].toIntOrNull()
                keyword = "#"
            }
        }
    }
    if (number == null && useContent && doc.text.isNotBlank()) {
        val c = CONTENT_RE.find(doc.text.take(1500))
        if (c != null) {
            number = c.groupValues[2].toIntOrNull()
            keyword = c.groupValues[1].lowercase()
        }
    }

    val words = splitWords(base + " " + (doc.title ?: "")).toSet()
    var stage = 2
    if (words.any { it in INTRO_WORDS }) stage = 0
    else if (words.any { it in BASIC_WORDS }) stage = 1
    if (words.any { it in PRACTICE_WORDS }) stage = max(stage, 3)
    if (words.any { it in ADVANCED_WORDS }) stage = max(stage, 4)
    if (words.any { it in REVISION_WORDS }) stage = max(stage, 5)
    if (words.any { it in EXAM_WORDS }) stage = max(stage, 6)

    val parts = ArrayList<String>()
    if (volume > 0) parts.add("Vol $volume")
    if (number != null) {
        val pretty = when (keyword) {
            "pt", "part" -> "Part"
            "ch", "chap", "chapter" -> "Chapter"
            "#" -> "No."
            null -> "No."
            else -> (keyword ?: "").replaceFirstChar { it.uppercase() }
        }
        parts.add("$pretty $number")
    }
    when (stage) {
        0 -> parts.add("Intro")
        1 -> parts.add("Basics")
        3 -> parts.add("Practice")
        4 -> parts.add("Advanced")
        5 -> parts.add("Revision")
        6 -> parts.add("Exam")
    }
    return SeqInfo(stage, volume, number, parts.takeIf { it.isNotEmpty() }?.joinToString(" · "))
}

private fun buildGroup(key: String, name: String, members: List<Member>, useContent: Boolean): ProposedGroup {
    val infos = HashMap<Long, SeqInfo>()
    for (m in members) infos[m.prep.doc.id] = parseSequence(m.prep.doc, useContent)
    val sorted = members.sortedWith(Comparator<Member> { a, b ->
        val x = infos.getValue(a.prep.doc.id)
        val y = infos.getValue(b.prep.doc.id)
        var c = x.stage.compareTo(y.stage)
        if (c != 0) return@Comparator c
        c = x.volume.compareTo(y.volume)
        if (c != 0) return@Comparator c
        c = (x.number ?: Int.MAX_VALUE).compareTo(y.number ?: Int.MAX_VALUE)
        if (c != 0) return@Comparator c
        naturalCompare(a.prep.doc.fileName, b.prep.doc.fileName)
    })
    val numbered = infos.values.count { it.number != null }
    val note = when {
        numbered == 0 -> "No part numbers found — suggested order is based on names and content"
        numbered == infos.size -> "Ordered by part / chapter number"
        else -> "Ordered by part number where found, otherwise by topic progression"
    }
    return ProposedGroup(
        key = key,
        name = name,
        items = sorted.map { ProposedItem(it.prep.doc.id, it.conf, it.reason, infos[it.prep.doc.id]?.label) },
        orderNote = note
    )
}

// ---------------------------------------------------------------- public API

object AutoArranger {

    fun arrange(docs: List<ArrangeDoc>, cfg: ArrangeConfig): List<ProposedGroup> {
        if (docs.isEmpty()) return emptyList()
        val preps = docs.map { prep(it, cfg.useContent) }
        if (cfg.useContent) buildVectors(preps)
        for (p in preps) assignSubject(p, cfg.useContent)

        val assigned = preps.filter { it.subject != null }
        val leftovers = preps.filter { it.subject == null }.toMutableList()
        val bySubject = LinkedHashMap<String, MutableList<Member>>()
        for (p in assigned) {
            bySubject.getOrPut(p.subject!!) { mutableListOf() }.add(Member(p, p.subjectConf, p.reason))
        }

        // Unknown names, but the text can still point to an existing subject group.
        if (cfg.useContent && assigned.isNotEmpty() && leftovers.size.toLong() * assigned.size < 4_000_000L) {
            val iter = leftovers.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                if (p.vec.isEmpty() || p.tokenCount < 40) continue
                var best: Prep? = null
                var bestCos = 0.0
                for (a in assigned) {
                    val c = cosine(p.vec, a.vec)
                    if (c > bestCos) {
                        bestCos = c
                        best = a
                    }
                }
                if (best != null && bestCos >= 0.5) {
                    val conf = min(0.9, 0.4 + bestCos * 0.5).toFloat()
                    bySubject.getValue(best.subject!!).add(
                        Member(p, conf, "Content similar to “${best.doc.shown()}” (${(bestCos * 100).toInt()}%)")
                    )
                    iter.remove()
                }
            }
        }

        val raw = ArrayList<RawGroup>()
        for ((subject, list) in bySubject) raw.add(RawGroup("subject:$subject", subject, list))
        val (clusters, singles) = clusterLeftovers(leftovers, cfg)
        raw.addAll(clusters)

        val unsorted = ArrayList<Member>()
        val finalGroups = ArrayList<ProposedGroup>()
        for (g in raw) {
            val keep = g.members.filter { it.conf >= cfg.minConfidence }
            val drop = g.members.filter { it.conf < cfg.minConfidence }
            for (d in drop) {
                unsorted.add(Member(d.prep, 0f, "Low confidence (${(d.conf * 100).toInt()}%) for “${g.name}”"))
            }
            if (keep.size < cfg.minGroupSize) {
                for (k in keep) unsorted.add(Member(k.prep, 0f, "Not enough related PDFs for a folder “${g.name}”"))
            } else {
                finalGroups.add(buildGroup(g.key, g.name, keep, cfg.useContent))
            }
        }
        for (s in singles) unsorted.add(Member(s, 0f, "No related PDFs found"))

        val result = ArrayList<ProposedGroup>()
        result.addAll(finalGroups.sortedWith(compareByDescending<ProposedGroup> { it.items.size }.thenBy { it.name.lowercase() }))
        if (unsorted.isNotEmpty()) {
            val items = unsorted
                .sortedWith(Comparator<Member> { a, b -> naturalCompare(a.prep.doc.fileName, b.prep.doc.fileName) })
                .map { ProposedItem(it.prep.doc.id, 0f, it.reason, null) }
            result.add(ProposedGroup("unsorted", "Unsorted", items, isUnsorted = true, orderNote = "Files without a clear match"))
        }
        return result
    }
}
