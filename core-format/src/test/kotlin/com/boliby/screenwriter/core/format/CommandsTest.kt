package com.boliby.screenwriter.core.format

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.ElementType.DIALOGUE
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.core.model.Span
import com.boliby.screenwriter.core.model.Style.BOLD
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommandsTest {
    private val a = BlockId(0)
    private val b = BlockId(1)
    private val script = Script(listOf(Block(a, ACTION, "Hello world."), Block(b, CHARACTER, "ANN")))

    @Test
    fun everyCommandUndoesExactly() {
        val commands = listOf(
            Command.ReplaceText(a, 6, 11, "there"),
            Command.SetBlock(Block(b, DIALOGUE, "Hi.")),
            Command.InsertBlock(1, Block(BlockId(2), ACTION, "New.")),
            Command.RemoveBlock(a),
        )
        for (command in commands) {
            val (changed, inverse) = script.apply(command)
            assertEquals(script, changed.apply(inverse).first, "$command")
        }
    }

    @Test
    fun replaceTextEditsTheBlock() {
        assertEquals("Hello there.", script.apply(Command.ReplaceText(a, 6, 11, "there")).first.blocks[0].text)
    }

    @Test
    fun splitAndMerge() {
        val split = Edits.split(script, a, 6, ACTION, BlockId(2)).fold(script) { s, c -> s.apply(c).first }
        assertEquals(listOf("Hello ", "world.", "ANN"), split.blocks.map { it.text })
        val merged = Edits.mergeWithPrevious(split, BlockId(2)).fold(split) { s, c -> s.apply(c).first }
        assertEquals(listOf("Hello world.", "ANN"), merged.blocks.map { it.text })
    }

    @Test
    fun splitMovesStylesWithTheText() {
        val bold = script.copy(blocks = listOf(script.blocks[0].copy(spans = listOf(Span(0, 11, BOLD)))))
        val split = Edits.split(bold, a, 6, ACTION, BlockId(2)).fold(bold) { s, c -> s.apply(c).first }
        assertEquals(listOf(Span(0, 6, BOLD)), split.blocks[0].spans)
        assertEquals(listOf(Span(0, 5, BOLD)), split.blocks[1].spans)
    }

    @Test
    fun spansFollowEdits() {
        val span = listOf(Span(2, 6, BOLD))
        assertEquals(listOf(Span(2, 8, BOLD)), Spans.replace(span, 4, 4, 2), "typed inside")
        assertEquals(listOf(Span(2, 6, BOLD)), Spans.replace(span, 6, 6, 2), "typed at the end")
        assertEquals(listOf(Span(4, 8, BOLD)), Spans.replace(span, 2, 2, 2), "typed at the start")
        assertEquals(listOf(Span(1, 5, BOLD)), Spans.replace(span, 0, 1, 0), "deleted before")
        assertEquals(listOf(Span(2, 4, BOLD)), Spans.replace(span, 3, 5, 0), "deleted inside")
        assertEquals(listOf(Span(2, 4, BOLD)), Spans.replace(span, 4, 8, 0), "deleted across the end")
        assertEquals(emptyList(), Spans.replace(span, 1, 7, 0), "deleted around it")
        assertEquals(listOf(Span(2, 7, BOLD)), Spans.replace(span, 3, 5, 3), "word replaced inside")
    }

    @Test
    fun textChangeIsTheSmallestReplacement() {
        assertEquals(TextChange(5, 5, "!"), TextChange.between("Hello world", "Hello! world"))
        assertEquals(TextChange(0, 3, ""), TextChange.between("abcdef", "def"))
        assertEquals(TextChange(1, 2, "ee"), TextChange.between("aba", "aeea"))
        assertNull(TextChange.between("same", "same"))
    }
}
