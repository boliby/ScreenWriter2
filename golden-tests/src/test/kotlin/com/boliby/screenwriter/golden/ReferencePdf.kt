package com.boliby.screenwriter.golden

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.File

/** Reads page layout back out of a reference PDF, such as one exported by Final Draft. */
object ReferencePdf {
    class Line(val y: Float, val x: Float, val text: String)

    fun pages(file: File): List<List<Line>> = Loader.loadPDF(file).use { document ->
        (1..document.numberOfPages).map { page ->
            val lines = mutableListOf<Line>()
            val stripper = object : PDFTextStripper() {
                override fun writeString(text: String, positions: MutableList<TextPosition>) {
                    val first = positions.first()
                    lines += Line(first.yDirAdj, first.xDirAdj, text)
                }
            }
            stripper.startPage = page
            stripper.endPage = page
            stripper.getText(document)
            lines.sortedBy { it.y }
        }
    }

    /**
     * The first body line of each page. Lines above [bodyTop] points are the page
     * number and (CONT'D) cues, which Final Draft prints above the body.
     */
    fun pageStarts(file: File, bodyTop: Float = 75f): List<String> =
        pages(file).map { lines -> lines.firstOrNull { it.y >= bodyTop && it.text.isNotBlank() }?.text ?: "" }
}

/** How closely a layout's page breaks match a reference PDF (§3.3, §9). */
data class PageBreakReport(
    val pages: Int,
    val referencePages: Int,
    /** Reference pages whose first line starts the same page number here. */
    val alignedStarts: Int,
    /** Reference page starts that start some page here. */
    val matchingStarts: Int,
    val mismatches: List<String>,
) {
    val pageDelta get() = pages - referencePages
    val matchingFraction get() = matchingStarts.toDouble() / referencePages

    override fun toString() = buildString {
        appendLine("pages: $pages, reference: $referencePages (delta $pageDelta)")
        appendLine("page starts at the same page: $alignedStarts / $referencePages")
        appendLine("page starts found anywhere: $matchingStarts / $referencePages (${"%.1f".format(matchingFraction * 100)}%)")
        mismatches.forEach { appendLine(it) }
    }

    companion object {
        // Final Draft's PDF text uses curly quotes and drops some punctuation, so
        // lines are compared on their first letters and digits only.
        private fun key(line: String) = line.filter { it.isLetterOrDigit() }.uppercase().take(24)

        fun compare(starts: List<String>, reference: List<String>): PageBreakReport {
            val ours = starts.map(::key)
            val theirs = reference.map(::key)
            val ourSet = ours.toSet()
            val mismatches = theirs.indices.filter { ours.getOrNull(it) != theirs[it] }.map { i ->
                "page ${i + 1}: reference \"${reference[i].trim()}\" | ours \"${starts.getOrNull(i)?.trim()}\""
            }
            return PageBreakReport(
                pages = starts.size,
                referencePages = reference.size,
                alignedStarts = theirs.indices.count { ours.getOrNull(it) == theirs[it] },
                matchingStarts = theirs.count { it in ourSet },
                mismatches = mismatches,
            )
        }
    }
}
