package com.boliby.screenwriter.editor

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.boliby.screenwriter.core.format.BackspaceAction
import com.boliby.screenwriter.core.format.ElementKeys
import com.boliby.screenwriter.core.format.EnterAction
import com.boliby.screenwriter.core.format.TabAction
import com.boliby.screenwriter.core.model.ElementType

// Phase 1A spike (ROADMAP.md): a throwaway block editor for testing soft and
// hardware keyboards. The key rules live in core-format; the real model,
// commands, and undo come in Phase 2.

/** One paragraph, shown as its own text field. */
class EditorBlock(val id: Long, type: ElementType, text: String) {
    var type by mutableStateOf(type)
    val state = TextFieldState(text)
    val focusRequester = FocusRequester()

    /** Set by the text field; used to tell whether the cursor is on the first or last line. */
    var layout: (() -> TextLayoutResult?)? = null

    val text: String get() = state.text.toString()
}

data class FocusTarget(val blockId: Long, val cursor: Int)

class SpikeEditorState {
    private val handler = Handler(Looper.getMainLooper())
    private var nextId = 0L

    val blocks = mutableStateListOf<EditorBlock>()

    /** The block whose field last had focus. The element bar acts on it. */
    var focusedId by mutableStateOf<Long?>(null)

    /** A block that should take focus once its field is on screen. */
    var pendingFocus by mutableStateOf<FocusTarget?>(null)
        private set

    val focused: EditorBlock? get() = blocks.firstOrNull { it.id == focusedId }

    init {
        loadSample()
    }

    fun loadSample() {
        replaceAll(SAMPLE)
    }

    fun loadBlank() {
        replaceAll(listOf(ElementType.SCENE_HEADING to ""))
        blocks.firstOrNull()?.let { focusLater(it, 0) }
    }

    private fun replaceAll(content: List<Pair<ElementType, String>>) {
        pendingFocus = null
        focusedId = null
        blocks.clear()
        content.forEach { (type, text) -> blocks.add(newBlock(type, text)) }
    }

    private fun newBlock(type: ElementType, text: String) = EditorBlock(nextId++, type, text)

    fun inputTransformation(block: EditorBlock): InputTransformation = BlockInput(block)

    fun setType(block: EditorBlock, type: ElementType) {
        block.type = type
    }

    fun tab(block: EditorBlock) {
        when (val action = ElementKeys.tab(block.type, block.text)) {
            is TabAction.ChangeType -> block.type = action.type
            is TabAction.InsertAfter -> insertAfter(block, action.type, "")
            TabAction.None -> Unit
        }
    }

    fun shiftTab(block: EditorBlock) {
        block.type = ElementKeys.shiftTab(block.type)
    }

    /** Backspace with the cursor at the start of [block]. Returns false if there was nothing to do. */
    fun backspaceAtStart(block: EditorBlock): Boolean {
        val index = blocks.indexOf(block)
        val previous = blocks.getOrNull(index - 1)
        when (val action = ElementKeys.backspaceAtStart(block.type, block.text, previous?.type)) {
            BackspaceAction.None -> return false
            BackspaceAction.DeleteBlock -> {
                focusNow(previous!!, previous.text.length)
                blocks.remove(block)
            }
            BackspaceAction.MergeWithPrevious -> {
                val join = previous!!.text.length
                previous.state.edit { append(block.text) }
                focusNow(previous, join)
                blocks.remove(block)
            }
            is BackspaceAction.ChangeType -> block.type = action.type
        }
        return true
    }

    fun focusPrevious(block: EditorBlock): Boolean {
        val previous = blocks.getOrNull(blocks.indexOf(block) - 1) ?: return false
        focusNow(previous, previous.text.length)
        return true
    }

    fun focusNext(block: EditorBlock): Boolean {
        val index = blocks.indexOf(block)
        if (index < 0) return false
        val next = blocks.getOrNull(index + 1) ?: return false
        focusNow(next, 0)
        return true
    }

    /** Called by a block's field once it has taken the focus it was asked to. */
    fun focusApplied(target: FocusTarget) {
        if (pendingFocus == target) pendingFocus = null
    }

    private fun insertAfter(block: EditorBlock, type: ElementType, text: String): EditorBlock? {
        val index = blocks.indexOf(block)
        if (index < 0) return null
        val created = newBlock(type, text)
        blocks.add(index + 1, created)
        focusLater(created, 0)
        return created
    }

    // Moves focus straight away so the keyboard stays up, for example before the
    // focused block is removed. Falls back to focusing once the field is composed.
    private fun focusNow(block: EditorBlock, cursor: Int) {
        block.state.edit { selection = TextRange(cursor.coerceIn(0, length)) }
        val focused = try {
            block.focusRequester.requestFocus()
            true
        } catch (_: IllegalStateException) {
            false
        }
        if (!focused) pendingFocus = FocusTarget(block.id, cursor)
    }

    // New blocks aren't composed until the next frame, so their fields take focus then.
    private fun focusLater(block: EditorBlock, cursor: Int) {
        block.state.edit { selection = TextRange(cursor.coerceIn(0, length)) }
        pendingFocus = FocusTarget(block.id, cursor)
    }

    private fun applyEnter(block: EditorBlock, action: EnterAction, after: String) {
        val index = blocks.indexOf(block)
        if (index < 0) return
        when (action) {
            is EnterAction.ChangeType -> block.type = action.type
            EnterAction.InsertAbove -> blocks.add(index, newBlock(block.type, ""))
            is EnterAction.Split -> insertAfter(block, action.newType, after)
        }
    }

    // Pasted text with several line breaks: each non-blank line becomes a block.
    private fun insertLines(block: EditorBlock, lines: List<String>) {
        var last = block
        for (line in lines.filter { it.isNotBlank() }) {
            last = insertAfter(last, ElementKeys.next(last.type), line) ?: return
        }
        if (last !== block) focusLater(last, last.text.length)
    }

    /**
     * Catches line breaks and tabs, whether they come from a soft keyboard, a
     * hardware keyboard, or a paste. It only removes them from this field's text;
     * the block changes are posted to run after the keyboard's edit has finished,
     * so the model never changes in the middle of one.
     */
    private inner class BlockInput(private val block: EditorBlock) : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            // One new tab and nothing else is a Tab key on a soft keyboard. Tabs in
            // pasted text become spaces.
            val tabs = asCharSequence().count { it == '\t' }
            if (tabs == 1 && length == originalText.length + 1) {
                val at = asCharSequence().indexOf('\t')
                delete(at, at + 1)
                handler.post { tab(block) }
            } else if (tabs > 0) {
                replaceTabsWithSpaces()
            }

            val text = asCharSequence().toString()
            val lineBreak = text.indexOf('\n')
            if (lineBreak < 0) return
            val lines = text.split('\n')
            if (lines.size == 2) {
                val action = ElementKeys.enter(block.type, lines[0] + lines[1], lineBreak)
                when (action) {
                    is EnterAction.Split -> delete(lineBreak, length)
                    else -> delete(lineBreak, lineBreak + 1)
                }
                handler.post { applyEnter(block, action, lines[1]) }
            } else {
                delete(lineBreak, length)
                handler.post { insertLines(block, lines.drop(1)) }
            }
        }

        private fun TextFieldBuffer.replaceTabsWithSpaces() {
            for (i in length - 1 downTo 0) {
                if (charAt(i) == '\t') replace(i, i + 1, " ")
            }
        }
    }

    private companion object {
        val SAMPLE = listOf(
            ElementType.SCENE_HEADING to "int. coffee shop - night",
            ElementType.ACTION to "Rain streaks the windows. MAYA (30s) types on a battered laptop, the last customer left.",
            ElementType.CHARACTER to "Barista (O.S.)",
            ElementType.DIALOGUE to "We close in five.",
            ElementType.CHARACTER to "Maya",
            ElementType.PARENTHETICAL to "(not looking up)",
            ElementType.DIALOGUE to "Two more pages.",
            ElementType.TRANSITION to "cut to:",
        )
    }
}

/**
 * Shows the text in uppercase without changing what's stored (§2.6). It swaps
 * characters one for one, so cursor positions and the keyboard's composing
 * region line up with the stored text.
 */
object UppercaseOutput : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        for (i in 0 until length) {
            val c = charAt(i)
            val upper = c.uppercaseChar()
            if (upper != c) replace(i, i + 1, upper.toString())
        }
    }
}
