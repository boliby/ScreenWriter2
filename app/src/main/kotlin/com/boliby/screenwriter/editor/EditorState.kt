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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.boliby.screenwriter.core.format.BackspaceAction
import com.boliby.screenwriter.core.format.Caret
import com.boliby.screenwriter.core.format.Command
import com.boliby.screenwriter.core.format.Edits
import com.boliby.screenwriter.core.format.ElementKeys
import com.boliby.screenwriter.core.format.EnterAction
import com.boliby.screenwriter.core.format.ScriptDocument
import com.boliby.screenwriter.core.format.TabAction
import com.boliby.screenwriter.core.format.TextChange
import com.boliby.screenwriter.core.format.block
import com.boliby.screenwriter.core.format.indexOf
import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script

/**
 * One paragraph's text field. The script itself is [EditorState.document];
 * these fields show it and report typing back to it.
 */
class EditorBlock(val id: BlockId, type: ElementType, text: String) {
    var type by mutableStateOf(type)
    val state = TextFieldState(text)
    val focusRequester = FocusRequester()

    /** Set by the text field; used to tell whether the cursor is on the first or last line. */
    var layout: (() -> TextLayoutResult?)? = null

    val text: String get() = state.text.toString()

    /** Updates the field to [text] with the smallest edit, so the cursor stays put where it can. */
    fun show(text: String) {
        if (state.text.contentEquals(text)) return
        val change = TextChange.between(this.text, text) ?: return
        state.edit { replace(change.start, change.end, change.text) }
    }
}

data class FocusTarget(val blockId: BlockId, val cursor: Int)

/**
 * The editor: a [ScriptDocument] shown as one text field per block. Typing goes
 * from the fields into the document; every other change is a document edit,
 * after which the fields are updated to match (§6).
 */
class EditorState {
    private val handler = Handler(Looper.getMainLooper())

    var document = ScriptDocument(Script(emptyList()))
        private set

    val blocks = mutableStateListOf<EditorBlock>()

    /** The block whose field last had focus. The element bar acts on it. */
    var focusedId by mutableStateOf<BlockId?>(null)

    /** A block that should take focus once its field is on screen. */
    var pendingFocus by mutableStateOf<FocusTarget?>(null)
        private set

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    // Fields with typing the document hasn't recorded yet.
    private val dirty = mutableSetOf<EditorBlock>()

    val focused: EditorBlock? get() = blocks.firstOrNull { it.id == focusedId }

    init {
        loadSample()
    }

    fun loadSample() = load(SAMPLE)

    fun loadBlank() {
        load(listOf(ElementType.SCENE_HEADING to ""))
        blocks.firstOrNull()?.let { focusLater(it, 0) }
    }

    private fun load(content: List<Pair<ElementType, String>>) {
        pendingFocus = null
        focusedId = null
        dirty.clear()
        document = ScriptDocument(
            Script(content.mapIndexed { i, (type, text) -> Block(BlockId(i.toLong()), type, text) }),
        )
        blocks.clear()
        sync()
    }

    fun inputTransformation(block: EditorBlock): InputTransformation = BlockInput(block)

    // --- Fields to document ------------------------------------------------

    private fun capture(block: EditorBlock) {
        dirty -= block
        if (document.script.block(block.id) != null) document.type(block.id, block.text)
        updateHistory()
    }

    private fun captureAll() = dirty.toList().forEach(::capture)

    // --- Document to fields ------------------------------------------------

    private fun sync() {
        val existing = blocks.associateBy { it.id }
        val next = document.script.blocks.map { model ->
            val field = existing[model.id] ?: EditorBlock(model.id, model.type, model.text)
            if (field.type != model.type) field.type = model.type
            field.show(model.text)
            field
        }
        if (next.size != blocks.size || next.indices.any { next[it] !== blocks[it] }) {
            blocks.clear()
            blocks.addAll(next)
        }
        updateHistory()
    }

    private fun updateHistory() {
        canUndo = document.canUndo
        canRedo = document.canRedo
    }

    /** Makes one undoable change, updates the fields, and moves the caret if the change says where. */
    private fun edit(caretBefore: Caret?, change: ScriptDocument.EditScope.() -> Unit) {
        captureAll()
        val caret = document.edit(caretBefore, change)
        sync()
        caret?.let(::focusLater)
    }

    private fun caretOf(block: EditorBlock) = Caret(block.id, block.state.selection.start)

    // --- Commands from keys and the element bar ----------------------------

    fun setType(block: EditorBlock, type: ElementType) {
        edit(caretOf(block)) { apply(Edits.setType(script, block.id, type)) }
    }

    fun tab(block: EditorBlock) {
        captureAll()
        when (val action = ElementKeys.tab(block.type, block.text)) {
            is TabAction.ChangeType -> setType(block, action.type)
            is TabAction.InsertAfter -> edit(caretOf(block)) {
                val id = newId()
                apply(Command.InsertBlock(script.indexOf(block.id) + 1, Block(id, action.type, "")))
                caretAfter = Caret(id, 0)
            }
            TabAction.None -> Unit
        }
    }

    fun shiftTab(block: EditorBlock) = setType(block, ElementKeys.shiftTab(block.type))

    /** Backspace with the cursor at the start of [block]. Returns false if there was nothing to do. */
    fun backspaceAtStart(block: EditorBlock): Boolean {
        captureAll()
        val index = blocks.indexOf(block)
        val previous = blocks.getOrNull(index - 1)
        when (val action = ElementKeys.backspaceAtStart(block.type, block.text, previous?.type)) {
            BackspaceAction.None -> return false
            BackspaceAction.DeleteBlock -> {
                val end = previous!!.text.length
                // Focus moves before the block goes, so the keyboard stays up.
                focusNow(previous, end)
                edit(Caret(block.id, 0)) {
                    apply(Command.RemoveBlock(block.id))
                    caretAfter = Caret(previous.id, end)
                }
            }
            BackspaceAction.MergeWithPrevious -> {
                val join = previous!!.text.length
                focusNow(previous, join)
                edit(Caret(block.id, 0)) {
                    apply(Edits.mergeWithPrevious(script, block.id))
                    caretAfter = Caret(previous.id, join)
                }
            }
            is BackspaceAction.ChangeType -> setType(block, action.type)
        }
        return true
    }

    fun undo() {
        captureAll()
        val caret = document.undo() ?: return
        sync()
        focusCaret(caret)
    }

    fun redo() {
        captureAll()
        val caret = document.redo() ?: return
        sync()
        focusCaret(caret)
    }

    // Called with the text the field had just before Enter, newline removed.
    private fun applyEnter(block: EditorBlock, content: String, action: EnterAction) {
        if (document.script.block(block.id) == null) return
        dirty -= block
        document.type(block.id, content)
        when (action) {
            is EnterAction.ChangeType -> setType(block, action.type)
            EnterAction.InsertAbove -> edit(Caret(block.id, 0)) {
                apply(Command.InsertBlock(script.indexOf(block.id), Block(newId(), block.type, "")))
            }
            is EnterAction.Split -> edit(Caret(block.id, action.at)) {
                val id = newId()
                apply(Edits.split(script, block.id, action.at, action.newType, id))
                caretAfter = Caret(id, 0)
            }
        }
    }

    // Pasted text with several line breaks: each non-blank line after the first
    // becomes a block. The whole paste is one undo step.
    private fun paste(block: EditorBlock, firstLine: String, lines: List<String>) {
        if (document.script.block(block.id) == null) return
        dirty -= block
        edit(caretOf(block)) {
            apply(Edits.setText(script, block.id, firstLine))
            var last = script.block(block.id)!!
            for (line in lines.filter { it.isNotBlank() }) {
                val next = Block(newId(), ElementKeys.next(last.type), line)
                apply(Command.InsertBlock(script.indexOf(last.id) + 1, next))
                last = next
            }
            caretAfter = Caret(last.id, last.text.length)
        }
    }

    // --- Focus -------------------------------------------------------------

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

    private fun focusCaret(caret: Caret) {
        blocks.firstOrNull { it.id == caret.block }?.let { focusNow(it, caret.offset) }
    }

    // Moves focus straight away so the keyboard stays up, for example before the
    // focused block is removed. Falls back to focusing once the field is composed.
    private fun focusNow(block: EditorBlock, cursor: Int) {
        block.state.edit { selection = TextRange(cursor.coerceIn(0, length)) }
        // Returns false, rather than throwing, when the field isn't composed.
        if (!block.focusRequester.requestFocus(FocusDirection.Enter)) pendingFocus = FocusTarget(block.id, cursor)
    }

    // New blocks aren't composed until the next frame, so their fields take focus then.
    private fun focusLater(caret: Caret) {
        val block = blocks.firstOrNull { it.id == caret.block } ?: return
        block.state.edit { selection = TextRange(caret.offset.coerceIn(0, length)) }
        pendingFocus = FocusTarget(block.id, caret.offset)
    }

    private fun focusLater(block: EditorBlock, cursor: Int) = focusLater(Caret(block.id, cursor))

    /**
     * Catches line breaks and tabs, whether they come from a soft keyboard, a
     * hardware keyboard, or a paste. It only changes this field's text; document
     * changes are posted to run after the keyboard's edit has finished, so
     * nothing else changes in the middle of one.
     */
    private inner class BlockInput(private val block: EditorBlock) : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            dirty += block
            // One new tab and nothing else is a Tab key on a soft keyboard. Tabs in
            // pasted text become spaces.
            val tabs = asCharSequence().count { it == '\t' }
            if (tabs == 1 && length == originalText.length + 1) {
                val at = asCharSequence().indexOf('\t')
                delete(at, at + 1)
                handler.post { tab(block) }
                return
            } else if (tabs > 0) {
                replaceTabsWithSpaces()
            }

            val text = asCharSequence().toString()
            val lineBreak = text.indexOf('\n')
            if (lineBreak < 0) {
                handler.post { capture(block) }
                return
            }
            val lines = text.split('\n')
            if (lines.size == 2) {
                val content = lines[0] + lines[1]
                val action = ElementKeys.enter(block.type, content, lineBreak)
                when (action) {
                    is EnterAction.Split -> delete(lineBreak, length)
                    else -> delete(lineBreak, lineBreak + 1)
                }
                handler.post { applyEnter(block, content, action) }
            } else {
                delete(lineBreak, length)
                handler.post { paste(block, lines[0], lines.drop(1)) }
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
