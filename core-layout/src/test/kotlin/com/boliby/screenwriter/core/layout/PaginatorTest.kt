package com.boliby.screenwriter.core.layout

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.ElementType.DIALOGUE
import com.boliby.screenwriter.core.model.ElementType.PAGE_BREAK
import com.boliby.screenwriter.core.model.ElementType.PARENTHETICAL
import com.boliby.screenwriter.core.model.ElementType.SCENE_HEADING
import com.boliby.screenwriter.core.model.ElementType.TRANSITION
import com.boliby.screenwriter.core.model.Script
import kotlin.test.Test
import kotlin.test.assertEquals

// Pages of 10 body lines and narrow columns keep these cases small.
class PaginatorTest {
    private val template = PageTemplate.US_LETTER.copy(
        bodyLines = 10,
        formats = mapOf(
            SCENE_HEADING to ElementFormat(1.5, 3.4, 1),
            ACTION to ElementFormat(1.5, 3.4, 1),
            CHARACTER to ElementFormat(3.0, 4.9, 1),
            PARENTHETICAL to ElementFormat(2.5, 4.4, 0),
            DIALOGUE to ElementFormat(2.0, 3.9, 0),
            TRANSITION to ElementFormat(3.0, 4.9, 1, Align.RIGHT),
        ),
    )

    private fun paginate(vararg blocks: Pair<ElementType, String>, dual: Set<Int> = emptySet()): List<Page> =
        Paginator(template).paginate(
            Script(blocks.mapIndexed { i, (type, text) -> Block(BlockId(i.toLong()), type, text, dual = i in dual) }),
        )

    // A page as "row:text" lines.
    private fun Page.rows() = lines.map { "${it.row}:${it.text}" }

    @Test
    fun spaceBeforeIsDroppedAtTheTopOfAPage() {
        val pages = paginate(ACTION to "One.", PAGE_BREAK to "", ACTION to "Two.")
        assertEquals(listOf(listOf("1:One."), listOf("1:Two.")), pages.map { it.rows() })
    }

    // Columns hold 20 characters, so each sentence below takes a line of its own.
    private val sentences = List(6) { i -> List(6) { "ABCDEF"[i].toString().repeat(2) }.joinToString(" ") + "." }

    @Test
    fun actionSplitsBetweenSentencesWithTwoLinesEachSide() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.\n5.\n6.",
            ACTION to sentences.take(4).joinToString(" "),
        )
        assertEquals(listOf("8:${sentences[0]}", "9:${sentences[1]}"), pages[0].rows().drop(6))
        assertEquals(listOf("1:${sentences[2]}", "2:${sentences[3]}"), pages[1].rows())
    }

    @Test
    fun actionThatCannotSplitMovesToTheNextPage() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.\n5.\n6.\n7.",
            ACTION to "No sentence ends here so it stays whole",
        )
        assertEquals(listOf("1:No sentence ends", "2:here so it stays", "3:whole"), pages[1].rows())
    }

    @Test
    fun sceneHeadingStaysWithTheStartOfTheNextParagraph() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.\n5.\n6.\n7.",
            SCENE_HEADING to "INT. HOUSE",
            ACTION to "Two lines of action here.",
        )
        assertEquals("INT. HOUSE", pages[1].firstBodyLine?.text)
    }

    @Test
    fun dialogueSplitsWithMoreAndContinued() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.",
            CHARACTER to "Ann (V.O.)",
            DIALOGUE to sentences.joinToString(" "),
        )
        assertEquals(
            listOf("6:ANN (V.O.)") + sentences.take(4).mapIndexed { i, s -> "${7 + i}:$s" } + "11:(MORE)",
            pages[0].rows().drop(4),
        )
        assertEquals(listOf("0:ANN (V.O.) (CONT'D)", "1:${sentences[4]}", "2:${sentences[5]}"), pages[1].rows())
    }

    @Test
    fun continuedCueIsNotDoubled() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.",
            CHARACTER to "Ann (cont'd)",
            DIALOGUE to sentences.joinToString(" "),
        )
        assertEquals("0:ANN (CONT'D)", pages[1].rows().first())
    }

    @Test
    fun parentheticalMayEndAPageAboveMore() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.\n5.",
            CHARACTER to "Ann",
            DIALOGUE to "Aa aa aa.\nBb bb bb.",
            PARENTHETICAL to "(beat)",
            DIALOGUE to "Cc cc cc.\nDd dd dd.",
        )
        assertEquals(listOf("(beat)", "(MORE)"), pages[0].lines.takeLast(2).map { it.text })
        assertEquals("Cc cc cc.", pages[1].firstBodyLine?.text)
    }

    @Test
    fun transitionNeverStartsAPage() {
        val pages = paginate(
            ACTION to "1.\n2.\n3.\n4.\n5.\n6.\n7.",
            ACTION to "Last line.",
            TRANSITION to "CUT TO:",
        )
        assertEquals(listOf("Last line.", "CUT TO:"), pages[1].lines.map { it.text })
    }

    @Test
    fun dualDialogueSitsSideBySide() {
        val pages = paginate(
            CHARACTER to "Ann",
            DIALOGUE to "Yes.",
            CHARACTER to "Bob",
            DIALOGUE to "No.",
            ACTION to "They glare.",
            dual = setOf(2),
        )
        assertEquals(
            listOf("1:0:ANN", "2:0:Yes.", "1:1:BOB", "2:1:No.", "4:0:They glare."),
            pages[0].lines.map { "${it.row}:${it.column}:${it.text}" },
        )
    }
}
