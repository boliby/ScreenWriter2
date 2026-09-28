package com.boliby.screenwriter.core.format

import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.Script

/**
 * A script being edited, with undo and redo (§6). Every change goes through
 * [edit] or [type] as commands. Typing in one block within [coalesceMillis] of
 * the last keystroke joins the same undo step; at most [maxSteps] steps are kept.
 */
class ScriptDocument(
    script: Script,
    private val clock: () -> Long = System::currentTimeMillis,
    private val coalesceMillis: Long = 1_000,
    private val maxSteps: Int = 500,
) {
    var script: Script = script
        private set

    private class Step(
        val forward: MutableList<Command>,
        var inverse: List<Command>,
        val caretBefore: Caret?,
        var caretAfter: Caret?,
        /** The block a typing step changed, while more typing may still join it. */
        var typingIn: BlockId?,
        var time: Long,
    )

    private val undoSteps = ArrayDeque<Step>()
    private val redoSteps = ArrayDeque<Step>()
    private var nextId = (script.blocks.maxOfOrNull { it.id.value } ?: -1) + 1

    val canUndo get() = undoSteps.isNotEmpty()
    val canRedo get() = redoSteps.isNotEmpty()

    /** An id no block in this document has used. */
    fun newId(): BlockId = BlockId(nextId++)

    /** Commands applied inside [EditScope] so far form one undo step. */
    inner class EditScope internal constructor() {
        internal val forward = mutableListOf<Command>()
        internal val inverse = mutableListOf<Command>()

        /** Where the caret goes when this step is redone. */
        var caretAfter: Caret? = null

        val script: Script get() = this@ScriptDocument.script

        fun newId(): BlockId = this@ScriptDocument.newId()

        fun apply(commands: List<Command>) = commands.forEach(::apply)

        fun apply(command: Command) {
            val (next, undo) = this@ScriptDocument.script.apply(command)
            this@ScriptDocument.script = next
            forward += command
            inverse += undo
        }
    }

    /**
     * Makes one undoable change. [caretBefore] is where the caret goes back to on
     * undo. Returns where the caret should go now.
     */
    fun edit(caretBefore: Caret?, change: EditScope.() -> Unit): Caret? {
        val scope = EditScope().apply(change)
        if (scope.forward.isEmpty()) return null
        push(Step(scope.forward, scope.inverse.reversed(), caretBefore, scope.caretAfter, typingIn = null, time = clock()))
        return scope.caretAfter
    }

    /** Records typing that turned a block's text into [text]. */
    fun type(id: BlockId, text: String) {
        val block = script.block(id) ?: return
        val change = TextChange.between(block.text, text) ?: return
        val command = Command.ReplaceText(id, change.start, change.end, change.text)
        val caret = Caret(id, change.start + change.text.length)
        val (next, undo) = script.apply(command)
        script = next

        val last = undoSteps.lastOrNull()
        val now = clock()
        if (last != null && last.typingIn == id && now - last.time < coalesceMillis) {
            // The step's inverse already restores the block from before this typing began.
            last.forward += command
            last.caretAfter = caret
            last.time = now
            redoSteps.clear()
        } else {
            push(Step(mutableListOf(command), listOf(undo), Caret(id, change.start), caret, typingIn = id, time = now))
        }
    }

    /** Undoes the last step. Returns where the caret goes, or null if there was nothing to undo. */
    fun undo(): Caret? {
        val step = undoSteps.removeLastOrNull() ?: return null
        step.inverse.forEach { script = script.apply(it).first }
        step.typingIn = null
        undoSteps.lastOrNull()?.typingIn = null
        redoSteps.addLast(step)
        return step.caretBefore ?: step.caretAfter
    }

    fun redo(): Caret? {
        val step = redoSteps.removeLastOrNull() ?: return null
        step.forward.forEach { script = script.apply(it).first }
        undoSteps.lastOrNull()?.typingIn = null
        undoSteps.addLast(step)
        return step.caretAfter ?: step.caretBefore
    }

    private fun push(step: Step) {
        undoSteps.lastOrNull()?.typingIn = null
        undoSteps.addLast(step)
        if (undoSteps.size > maxSteps) undoSteps.removeFirst()
        redoSteps.clear()
    }
}
