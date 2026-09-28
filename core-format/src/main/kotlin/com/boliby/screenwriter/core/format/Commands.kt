package com.boliby.screenwriter.core.format

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.core.model.Span

/**
 * One change to a script. Every edit is built from these, and applying one
 * yields its exact inverse, which is how undo works (§2.9, §6).
 */
sealed interface Command {
    /** Replaces `[start, end)` of a block's text with [text]. Spans move with the text. */
    data class ReplaceText(val block: BlockId, val start: Int, val end: Int, val text: String) : Command

    /** Replaces the block that has the same id. */
    data class SetBlock(val block: Block) : Command

    data class InsertBlock(val index: Int, val block: Block) : Command

    data class RemoveBlock(val block: BlockId) : Command
}

/** A caret position: an offset in a block's text. */
data class Caret(val block: BlockId, val offset: Int)

fun Script.indexOf(id: BlockId): Int = blocks.indexOfFirst { it.id == id }

fun Script.block(id: BlockId): Block? = blocks.firstOrNull { it.id == id }

/** Applies [command] and returns the new script with the command that undoes it. */
fun Script.apply(command: Command): Pair<Script, Command> = when (command) {
    is Command.ReplaceText -> {
        val index = indexOf(command.block)
        require(index >= 0) { "No block ${command.block}" }
        val old = blocks[index]
        val start = command.start.coerceIn(0, old.text.length)
        val end = command.end.coerceIn(start, old.text.length)
        val updated = old.copy(
            text = old.text.substring(0, start) + command.text + old.text.substring(end),
            spans = Spans.replace(old.spans, start, end, command.text.length),
        )
        replaced(index, updated) to Command.SetBlock(old)
    }
    is Command.SetBlock -> {
        val index = indexOf(command.block.id)
        require(index >= 0) { "No block ${command.block.id}" }
        replaced(index, command.block) to Command.SetBlock(blocks[index])
    }
    is Command.InsertBlock -> {
        require(indexOf(command.block.id) < 0) { "Block ${command.block.id} already exists" }
        val index = command.index.coerceIn(0, blocks.size)
        copy(blocks = blocks.toMutableList().apply { add(index, command.block) }) to Command.RemoveBlock(command.block.id)
    }
    is Command.RemoveBlock -> {
        val index = indexOf(command.block)
        require(index >= 0) { "No block ${command.block}" }
        copy(blocks = blocks.toMutableList().apply { removeAt(index) }) to Command.InsertBlock(index, blocks[index])
    }
}

private fun Script.replaced(index: Int, block: Block) = copy(blocks = blocks.toMutableList().apply { set(index, block) })

/** Where emphasis ends up when text changes under it. */
object Spans {
    /**
     * Adjusts [spans] for `[start, end)` being replaced by [inserted] characters.
     * Text typed inside a span takes its style; text typed at either edge doesn't.
     */
    fun replace(spans: List<Span>, start: Int, end: Int, inserted: Int): List<Span> {
        val delta = inserted - (end - start)
        return spans.mapNotNull { span ->
            val (s, e) = if (start == end) {
                when {
                    span.end <= start -> span.start to span.end
                    span.start >= start -> span.start + inserted to span.end + inserted
                    else -> span.start to span.end + inserted
                }
            } else {
                when {
                    span.end <= start -> span.start to span.end
                    span.start >= end -> span.start + delta to span.end + delta
                    // The span covers everything replaced, so the new text takes its style.
                    span.start <= start && span.end >= end -> span.start to span.end + delta
                    else -> (if (span.start < start) span.start else start + inserted) to
                        (if (span.end > end) span.end + delta else start)
                }
            }
            if (e > s) span.copy(start = s, end = e) else null
        }
    }
}

/** The smallest single replacement that turns [old] into [new]. */
data class TextChange(val start: Int, val end: Int, val text: String) {
    companion object {
        fun between(old: String, new: String): TextChange? {
            if (old == new) return null
            var prefix = 0
            val max = minOf(old.length, new.length)
            while (prefix < max && old[prefix] == new[prefix]) prefix++
            var suffix = 0
            while (suffix < max - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
            return TextChange(prefix, old.length - suffix, new.substring(prefix, new.length - suffix))
        }
    }
}

/** Builds the commands for common edits from the script as it is now. */
object Edits {
    /** Moves the text after [at] into a new block of [newType] below. */
    fun split(script: Script, id: BlockId, at: Int, newType: ElementType, newId: BlockId): List<Command> {
        val block = script.block(id) ?: return emptyList()
        val cut = at.coerceIn(0, block.text.length)
        val tailSpans = block.spans.filter { it.end > cut }.map { it.copy(start = maxOf(it.start, cut) - cut, end = it.end - cut) }
        return listOf(
            Command.ReplaceText(id, cut, block.text.length, ""),
            Command.InsertBlock(script.indexOf(id) + 1, Block(newId, newType, block.text.substring(cut), tailSpans)),
        )
    }

    /** Appends a block's text to the block above it and removes it. */
    fun mergeWithPrevious(script: Script, id: BlockId): List<Command> {
        val index = script.indexOf(id)
        if (index <= 0) return emptyList()
        val previous = script.blocks[index - 1]
        val block = script.blocks[index]
        val offset = previous.text.length
        val merged = previous.copy(
            text = previous.text + block.text,
            spans = previous.spans + block.spans.map { it.copy(start = it.start + offset, end = it.end + offset) },
        )
        return listOf(Command.SetBlock(merged), Command.RemoveBlock(id))
    }

    fun setType(script: Script, id: BlockId, type: ElementType): List<Command> {
        val block = script.block(id) ?: return emptyList()
        return if (block.type == type) emptyList() else listOf(Command.SetBlock(block.copy(type = type)))
    }

    /** Makes a block's text [text], as one replacement. */
    fun setText(script: Script, id: BlockId, text: String): List<Command> {
        val block = script.block(id) ?: return emptyList()
        val change = TextChange.between(block.text, text) ?: return emptyList()
        return listOf(Command.ReplaceText(id, change.start, change.end, change.text))
    }
}
