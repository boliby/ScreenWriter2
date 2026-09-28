package com.boliby.screenwriter.core.format

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.Script
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptDocumentTest {
    private var now = 0L
    private val a = BlockId(0)
    private fun document() = ScriptDocument(Script(listOf(Block(a, ACTION, ""))), clock = { now }, maxSteps = 3)
    private fun ScriptDocument.texts() = script.blocks.map { it.text }

    @Test
    fun typingWithinASecondIsOneUndoStep() {
        val doc = document()
        doc.type(a, "H")
        now += 300
        doc.type(a, "Hi")
        now += 300
        doc.type(a, "Hi!")
        assertEquals(Caret(a, 0), doc.undo())
        assertEquals(listOf(""), doc.texts())
        assertEquals(Caret(a, 3), doc.redo())
        assertEquals(listOf("Hi!"), doc.texts())
    }

    @Test
    fun aPauseStartsANewStep() {
        val doc = document()
        doc.type(a, "Hi")
        now += 1_500
        doc.type(a, "Hi there")
        doc.undo()
        assertEquals(listOf("Hi"), doc.texts())
    }

    @Test
    fun anEditIsOneStepAndBreaksTyping() {
        val doc = document()
        doc.type(a, "Hello world")
        val caret = doc.edit(Caret(a, 6)) {
            val id = newId()
            apply(Edits.split(script, a, 6, CHARACTER, id))
            caretAfter = Caret(id, 0)
        }
        assertEquals(Caret(BlockId(1), 0), caret)
        assertEquals(listOf("Hello ", "world"), doc.texts())
        doc.type(a, "Hello there ")
        doc.undo()
        assertEquals(listOf("Hello ", "world"), doc.texts())
        assertEquals(Caret(a, 6), doc.undo())
        assertEquals(listOf("Hello world"), doc.texts())
    }

    @Test
    fun typingAfterUndoClearsRedo() {
        val doc = document()
        doc.type(a, "One")
        now += 2_000
        doc.type(a, "One two")
        doc.undo()
        doc.type(a, "One three")
        assertFalse(doc.canRedo)
        doc.undo()
        assertEquals(listOf("One"), doc.texts())
    }

    @Test
    fun keepsOnlyTheLatestSteps() {
        val doc = document()
        repeat(5) {
            now += 2_000
            doc.type(a, "x".repeat(it + 1))
        }
        repeat(3) { doc.undo() }
        assertFalse(doc.canUndo)
        assertEquals(listOf("xx"), doc.texts())
    }

    @Test
    fun emptyEditsAreNotSteps() {
        val doc = document()
        doc.edit(null) { apply(Edits.setType(script, a, ACTION)) }
        assertFalse(doc.canUndo)
        doc.edit(null) { apply(Edits.setType(script, a, CHARACTER)) }
        assertTrue(doc.canUndo)
    }

    @Test
    fun newIdsAreUnique() {
        val doc = ScriptDocument(Script(listOf(Block(BlockId(7), ACTION, ""))))
        assertEquals(BlockId(8), doc.newId())
        assertEquals(BlockId(9), doc.newId())
    }
}
