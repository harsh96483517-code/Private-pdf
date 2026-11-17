package com.lumen.pdf.pdf

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Offline auto-arrange engine.
 *
 * It never looks at filenames alone: the caller feeds filename + PDF title + first page
 * text, and the engine builds a weighted bag of words, clusters documents by cosine
 * similarity of their tf-idf vectors, names each cluster from its strongest shared
 * keywords and derives a reading order from part / chapter / volume numbers.
 */
object AutoArrangeEngine {

    data class DocInput(val id: Long, val name: String, val text: String)

    data class Proposal(val name: String, val confidence: Float, val ordered: List<Long>)

    data class Result(val proposals: List<Proposal>, val unsorted: List<Long>) {
        val totalDocuments: Int get() = proposals.sumOf { it.ordered.size } + unsorted.size
    }

    private val stopWords = setOf(
        "the", "and", "for", "with", "from", "this", "that", "are", "was", "were", "will",
        "you", "your", "our", "their", "his", "her", "its", "not", "but", "can", "may",
        "have", "has", "had", "been", "being", "which", "what", "when", "where", "how",
        "all", "any", "each", "other", "some", "such", "than", "then", "there", "these",
        "they", "into", "over", "under", "about", "also", "more", "most", "only", "very",
        "pdf", "file", "files", "document", "documents", "page", "pages", "part", "chapter",
        "unit", "volume", "vol", "book", "notes", "note", "new", "final", "copy",
        "www", "http", "https", "com", "org", "draft", "version", "untitled", "scan", "scanned"
    )

    private val sequenceRegex = Regex(
        "(?i)(?:part|chapter|ch|unit|lesson|lec|volume|vol|book|section|module)[\\s._-]*([0-9]{1,3})"
    )
    private val leadingNumberRegex = Regex("^[\\s._-]*([0-9]{1,3})[\\s._-]+")

    fun keywordsOf(name: String, text: String): String =
        tokenize(name, 2.0, text, 1.0).entries.sortedByDescending { it.value }
            .take(14).joinToString(",") { it.key }

    fun sequenceOf(name: String, text: String): Int {
        sequenceRegex.find(name)?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        leadingNumberRegex.find(name)?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        sequenceRegex.find(text.take(1200))?.let { return it.groupValues[1].toIntOrNull() ?: 0 }
        return 0
    }

    fun arrange(docs: List<DocInput>, threshold: Float = 0.30f): Result {
        if (docs.isEmpty()) return Result(emptyList(), emptyList())

        val tokenWeights: List<Map<String, Double>> = docs.map { doc ->
            tokenize(doc.name, 2.4, doc.text, 1.0)
        }

        val documentFrequency = HashMap<String, Int>()
        tokenWeights.forEach { weights ->
            weights.keys.forEach { token ->
                documentFrequency[token] = (documentFrequency[token] ?: 0) + 1
            }
        }

        val n = docs.size.toDouble()
        val vectors: List<Map<String, Double>> = tokenWeights.map { weights ->
            weights.mapValues { (token, weight) ->
                val df = (documentFrequency[token] ?: 1).toDouble()
                weight * (ln((n + 1.0) / (df + 1.0)) + 1.0)
            }.filterValues { it > 0.0 }
        }

        val clusters = ArrayList<MutableList<Int>>()
        val centroids = ArrayList<HashMap<String, Double>>()

        docs.indices.forEach { index ->
            val vector = vectors[index]
            var bestCluster = -1
            var bestScore = 0.0f
            centroids.forEachIndexed { clusterIndex, centroid ->
                val score = cosine(vector, centroid)
                if (score > bestScore) {
                    bestScore = score
                    bestCluster = clusterIndex
                }
            }
            if (bestCluster >= 0 && bestScore >= threshold) {
                clusters[bestCluster].add(index)
                addTo(centroids[bestCluster], vector)
            } else {
                clusters.add(mutableListOf(index))
                centroids.add(HashMap(vector))
            }
        }

        val proposals = ArrayList<Proposal>()
        val unsorted = ArrayList<Long>()

        clusters.forEach { cluster ->
            if (cluster.size < 2) {
                cluster.forEach { unsorted.add(docs[it].id) }
                return@forEach
            }
            val centroid = HashMap<String, Double>()
            cluster.forEach { addTo(centroid, vectors[it]) }
            val scoreSum = cluster.sumOf { member -> cosine(vectors[member], centroid).toDouble() }
            val confidence = (scoreSum / cluster.size).toFloat().coerceIn(0f, 1f)

            val ordered = cluster.sortedWith(
                compareBy<Int> { index ->
                    val seq = sequenceOf(docs[index].name, docs[index].text)
                    if (seq > 0) seq else Int.MAX_VALUE
                }.thenBy { docs[it].name.lowercase() }
            ).map { docs[it].id }

            proposals.add(Proposal(clusterName(cluster.map { docs[it].id }, vectors, docs), confidence, ordered))
        }

        val merged = proposals.groupBy { it.name.lowercase() }.map { (_, group) ->
            if (group.size == 1) group.first()
            else Proposal(
                group.first().name,
                group.map { it.confidence }.average().toFloat(),
                group.flatMap { it.ordered }.distinct()
            )
        }

        return Result(merged.sortedByDescending { it.ordered.size }, unsorted)
    }

    private fun clusterName(ids: List<Long>, vectors: List<Map<String, Double>>, docs: List<DocInput>): String {
        val totals = HashMap<String, Double>()
        ids.forEach { id ->
            val index = docs.indexOfFirst { it.id == id }
            if (index >= 0) {
                vectors[index].forEach { (token, weight) ->
                    totals[token] = (totals[token] ?: 0.0) + weight
                }
            }
        }
        val best = totals.entries
            .filter { it.key.length >= 3 }
            .maxByOrNull { it.value }?.key
            ?: return "Unsorted"
        return best.replaceFirstChar { it.uppercase() }
    }

    private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val smaller = if (a.size <= b.size) a else b
        val larger = if (a.size <= b.size) b else a
        var dot = 0.0
        smaller.forEach { (token, weight) -> larger[token]?.let { dot += weight * it } }
        if (dot == 0.0) return 0f
        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (normA * normB)).toFloat().coerceIn(0f, 1f)
    }

    private fun addTo(target: HashMap<String, Double>, source: Map<String, Double>) {
        source.forEach { (token, weight) -> target[token] = (target[token] ?: 0.0) + weight }
    }

    private fun tokenize(name: String, nameWeight: Double, text: String, textWeight: Double): Map<String, Double> {
        val weights = HashMap<String, Double>()
        fun add(source: String, weight: Double, limit: Int) {
            source.lowercase()
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .asSequence()
                .filter { it.length >= 3 && it.length <= 24 && it !in stopWords && it.any { ch -> ch.isLetter() } }
                .take(limit)
                .forEach { token ->
                    weights[token] = (weights[token] ?: 0.0) + weight
                }
        }
        add(name, nameWeight, 60)
        add(text, textWeight, 220)
        return weights
    }
}
