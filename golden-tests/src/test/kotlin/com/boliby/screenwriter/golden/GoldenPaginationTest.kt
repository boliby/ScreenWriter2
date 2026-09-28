package com.boliby.screenwriter.golden

import com.boliby.screenwriter.core.fdx.FdxReader
import com.boliby.screenwriter.core.fdx.FdxTemplate
import com.boliby.screenwriter.core.fountain.FountainParser
import com.boliby.screenwriter.core.layout.Page
import com.boliby.screenwriter.core.layout.Paginator
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.testing.Samples
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Lays out the fountain.io samples and compares page breaks with the PDFs that
 * Final Draft printed from the same scripts (Phase 1B). Reports land in
 * `golden-tests/build/reports/pagination/`.
 */
class GoldenPaginationTest {
    @Test
    fun bigFishFromFinalDraft() {
        // The PDF's first two pages are the title page and its quote page.
        val report = fromFdx("Big-Fish", titlePages = 2) ?: return
        assertTrue(abs(report.pageDelta) <= 1, "page count\n$report")
        assertTrue(report.matchingFraction >= 0.5, "page starts\n$report")
    }

    @Test
    fun brickAndSteelFromFinalDraft() {
        val report = fromFdx("Brick-&-Steel", titlePages = 1) ?: return
        assertTrue(report.pageDelta == 0 && report.alignedStarts == report.referencePages, report.toString())
    }

    @Test
    fun lastBirthdayCardFromFinalDraft() {
        val report = fromFdx("The-Last-Birthday-Card", titlePages = 1) ?: return
        assertTrue(report.pageDelta == 0 && report.alignedStarts == report.referencePages, report.toString())
    }

    // The same scripts parsed from Fountain, laid out with their Final Draft
    // template. Fountain has no Shot or General elements, so some drift is
    // expected; this measures the parser, not the paginator.
    @Test
    fun fountainSources() {
        for ((name, titlePages) in listOf("Big-Fish" to 1, "Brick-&-Steel" to 1, "The-Last-Birthday-Card" to 1)) {
            val fountain = Samples.find("$name.fountain") ?: return
            val fdx = Samples.find("$name.fdx") ?: return
            val template = FdxTemplate.from(fdx.inputStream().use(FdxReader::read)) ?: error("no template")
            val pages = Paginator(template).paginate(FountainParser.parse(fountain.readText()))
            val report = compare(name, "fountain", pages, titlePages) ?: return
            assertTrue(abs(report.pageDelta) <= 2, "$name page count\n$report")
        }
    }

    private fun fromFdx(name: String, titlePages: Int): PageBreakReport? {
        val fdx = Samples.find("$name.fdx") ?: return null
        val script = fdx.inputStream().use(FdxReader::read)
        val template = FdxTemplate.from(script) ?: error("$name.fdx has no element settings")
        return compare(name, "fdx", Paginator(template).paginate(script), titlePages)
    }

    private fun compare(name: String, source: String, pages: List<Page>, titlePages: Int): PageBreakReport? {
        val pdf = Samples.find("$name.pdf") ?: return null
        val referencePages = ReferencePdf.pages(pdf).drop(titlePages)
        val reference = referencePages.map { lines -> lines.firstOrNull { it.y >= 75f && it.text.isNotBlank() }?.text ?: "" }
        val report = PageBreakReport.compare(pages.map { it.firstBodyLine?.text ?: "" }, reference)

        val out = File("build/reports/pagination").apply { mkdirs() }
        out.resolve("$name-$source.txt").writeText(report.toString())
        // Both layouts row by row, for finding where they drift apart.
        out.resolve("$name-$source-ours.tsv").writeText(
            pages.flatMap { page -> page.lines.map { "${page.number}\t${it.row}\t${it.type}\t${it.text}" } }.joinToString("\n"),
        )
        out.resolve("$name-reference.tsv").writeText(
            referencePages.withIndex().flatMap { (i, lines) ->
                lines.filter { it.y > 60 }.map { "${i + 1}\t${((it.y - 81) / 12 + 1).toInt()}\t${it.x.toInt()}\t${it.text}" }
            }.joinToString("\n"),
        )
        println("$name ($source): " + report.toString().lineSequence().take(3).joinToString("; "))
        return report
    }
}
