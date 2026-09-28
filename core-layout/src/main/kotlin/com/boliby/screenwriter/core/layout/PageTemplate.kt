package com.boliby.screenwriter.core.layout

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.ElementType

enum class Align { LEFT, CENTER, RIGHT }

/**
 * Where an element sits on the page, as Final Draft stores it: [left] and
 * [right] indents in inches from the paper's left edge, and [firstIndent] added
 * to the first line only (negative for a hanging parenthesis). [spaceBefore] is
 * the number of blank lines above the element, dropped at the top of a page.
 * [lineSpacing] 2 is double spacing: each line takes two rows.
 */
data class ElementFormat(
    val left: Double,
    val right: Double,
    val spaceBefore: Int,
    val align: Align = Align.LEFT,
    val firstIndent: Double = 0.0,
    val lineSpacing: Int = 1,
) {
    /**
     * Characters per line. Courier 12 is 10 characters per inch, and Final Draft
     * fits a character if it starts before the right indent, so a 6" column
     * holds 61 characters. Measured against Final Draft PDFs.
     */
    val maxChars: Int get() = columns(right - left)
    val firstLineMaxChars: Int get() = columns(right - left - firstIndent)

    private fun columns(inches: Double) = kotlin.math.floor(inches * 10 + 1e-6).toInt() + 1
}

/**
 * Page geometry and the page-break rules from §3.3. Every value is a setting so
 * templates can be calibrated against real Final Draft PDFs (§3.1).
 */
data class PageTemplate(
    val formats: Map<ElementType, ElementFormat>,
    /**
     * Formats for a block's original element name (its `element` extra), for
     * imported elements this app has no type for, such as Final Draft's General.
     */
    val namedFormats: Map<String, ElementFormat> = emptyMap(),
    /** Body lines per page: 6 lines per inch between 1" top and bottom margins. */
    val bodyLines: Int = 54,
    val topMargin: Double = 1.0,
    val pageWidth: Double = 8.5,
    val pageHeight: Double = 11.0,
    /** Lines of the next element that must stay on the page with a scene heading. */
    val minLinesAfterHeading: Int = 2,
    /** Lines that must stay on each side when an Action paragraph splits. */
    val minActionLinesPerSide: Int = 2,
    /** Dialogue lines that must stay on each side when a speech splits. */
    val minDialogueLinesBefore: Int = 2,
    val minDialogueLinesAfter: Int = 2,
    /** Split Action and dialogue only between sentences. */
    val breakAtSentences: Boolean = true,
    val more: String = "(MORE)",
    val continued: String = "(CONT'D)",
    /**
     * Final Draft prints (MORE) just below the last line of dialogue and the
     * continued character cue just above the first body line, so neither uses a
     * body line.
     */
    val moreAndContinuedInMargins: Boolean = true,
) {
    fun format(type: ElementType): ElementFormat = formats[type] ?: formats.getValue(ElementType.ACTION)

    fun format(block: Block): ElementFormat =
        block.extras[ELEMENT_EXTRA]?.let(namedFormats::get) ?: format(block.type)

    companion object {
        /** The [Block.extras] key holding an element name from an imported file. */
        const val ELEMENT_EXTRA = "element"

        /** The [Block.extras] key overriding one block's blank lines before it. */
        const val SPACE_BEFORE_EXTRA = "spaceBefore"

        /** US Letter with Final Draft's published element positions (§3.1). */
        val US_LETTER = PageTemplate(
            formats = mapOf(
                ElementType.SCENE_HEADING to ElementFormat(left = 1.5, right = 7.5, spaceBefore = 1),
                ElementType.ACTION to ElementFormat(left = 1.5, right = 7.5, spaceBefore = 1),
                ElementType.SHOT to ElementFormat(left = 1.5, right = 7.5, spaceBefore = 1),
                ElementType.CHARACTER to ElementFormat(left = 3.7, right = 7.5, spaceBefore = 1),
                ElementType.PARENTHETICAL to ElementFormat(left = 3.1, right = 5.6, spaceBefore = 0, firstIndent = -0.1),
                ElementType.DIALOGUE to ElementFormat(left = 2.5, right = 6.0, spaceBefore = 0),
                ElementType.LYRIC to ElementFormat(left = 2.5, right = 6.0, spaceBefore = 0),
                ElementType.TRANSITION to ElementFormat(left = 6.0, right = 7.5, spaceBefore = 1, align = Align.RIGHT),
                ElementType.CENTERED to ElementFormat(left = 1.5, right = 7.5, spaceBefore = 1, align = Align.CENTER),
            ),
        )
    }
}
