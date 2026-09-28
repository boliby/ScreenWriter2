package com.boliby.screenwriter.core.layout

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script

/**
 * One printed line. Rows 1 to [PageTemplate.bodyLines] are the page body. Row 0
 * is the line above it and `bodyLines + 1` the line below, which only (CONT'D)
 * cues and (MORE) use. [column] is 1 for the right-hand speech of dual dialogue.
 */
data class PageLine(
    val row: Int,
    val text: String,
    val type: ElementType,
    val format: ElementFormat,
    val blockId: BlockId? = null,
    val column: Int = 0,
)

/** A printed page. [number] counts from the first page after the title page. */
data class Page(val number: Int, val lines: List<PageLine>) {
    /** The first printed line of the body, ignoring a (CONT'D) cue above it. */
    val firstBodyLine: PageLine? get() = lines.filter { it.row >= 1 && it.text.isNotBlank() }.minByOrNull { it.row }
}

/**
 * Lays a script out on pages with the §3.3 rules. Like Final Draft, a paragraph
 * that splits across pages breaks between sentences, and each half is wrapped
 * on its own.
 */
class Paginator(private val template: PageTemplate = PageTemplate.US_LETTER) {
    fun paginate(script: Script): List<Page> = Filler(template, chunks(script.blocks)).fill()

    private fun chunks(blocks: List<Block>): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            when {
                !block.type.prints -> i++
                block.type == ElementType.PAGE_BREAK -> {
                    chunks += Chunk.ForcedBreak
                    i++
                }
                block.type == ElementType.CHARACTER -> {
                    var end = i + 1
                    while (end < blocks.size && blocks[end].type.inSpeech()) end++
                    val speech = Chunk.Speech(part(block), blocks.subList(i + 1, end).map(::part))
                    val previous = chunks.lastOrNull()
                    if (block.dual && previous is Chunk.Speech) {
                        chunks[chunks.lastIndex] = Chunk.Dual(previous, speech)
                    } else {
                        chunks += speech
                    }
                    i = end
                }
                else -> {
                    chunks += Chunk.Paragraph(part(block))
                    i++
                }
            }
        }
        return chunks
    }

    private fun ElementType.inSpeech() =
        this == ElementType.DIALOGUE || this == ElementType.PARENTHETICAL || this == ElementType.LYRIC

    private fun part(block: Block): Part {
        val format = template.format(block)
        val text = if (block.type.displaysUppercase) block.text.uppercase() else block.text
        val spaceBefore = block.extras[PageTemplate.SPACE_BEFORE_EXTRA]?.toIntOrNull() ?: format.spaceBefore
        return Part(block.type, block.id, format, text, spaceBefore)
    }
}

internal class Row(val text: String, val type: ElementType, val format: ElementFormat, val blockId: BlockId?)

/** A paragraph's text as it will print, wrapped to its element's width. */
internal class Part(
    val type: ElementType,
    val blockId: BlockId?,
    val format: ElementFormat,
    val text: String,
    val spaceBefore: Int,
    /** Continues a paragraph from the previous page, so the first-line indent doesn't apply. */
    private val continuation: Boolean = false,
    private val fixedRows: List<Row>? = null,
) {
    val rows: List<Row> by lazy {
        fixedRows ?: run {
            val first = if (continuation) format.maxChars else format.firstLineMaxChars
            val blank = Row("", type, format, blockId)
            LineWrapper.wrap(text, format.maxChars, first).flatMap {
                List(format.lineSpacing - 1) { blank } + Row(it, type, format, blockId)
            }
        }
    }

    /** Rows including the blank lines before this part. */
    val height get() = spaceBefore + rows.size

    fun head(end: Int) = Part(type, blockId, format, text.substring(0, end).trimEnd(), spaceBefore)

    fun tail(start: Int) = Part(type, blockId, format, text.substring(start).trimStart(), 0, continuation = true)

    fun headRows(count: Int) = Part(type, blockId, format, text, spaceBefore, fixedRows = rows.take(count))

    fun tailRows(count: Int) = Part(type, blockId, format, text, 0, fixedRows = rows.drop(count))
}

internal sealed class Chunk {
    object ForcedBreak : Chunk()

    class Paragraph(val part: Part) : Chunk()

    /** A character cue and its parentheticals and dialogue. */
    class Speech(
        val cue: Part,
        val parts: List<Part>,
        /** The rest of a speech from the previous page; [cue] is its (CONT'D) cue. */
        val continued: Boolean = false,
    ) : Chunk()

    /** Two speeches side by side. Never split (§3.3). */
    class Dual(val left: Speech, val right: Speech) : Chunk()
}

private class Filler(private val template: PageTemplate, private val chunks: List<Chunk>) {
    private val pages = mutableListOf<Page>()
    private var lines = mutableListOf<PageLine>()
    private var used = 0

    // The last chunk placed whole on this page, so a transition can take it along
    // to the next page instead of starting that page on its own (§3.3).
    private var lastChunk: Chunk? = null
    private var lastChunkStart = 0
    private var lastChunkUsedBefore = 0

    private val bodyLines get() = template.bodyLines
    private val inMargins get() = template.moreAndContinuedInMargins

    fun fill(): List<Page> {
        chunks.forEachIndexed { index, chunk ->
            if (chunk is Chunk.ForcedBreak) {
                if (lines.isNotEmpty()) newPage()
            } else {
                add(chunk, chunks.getOrNull(index + 1))
            }
        }
        if (lines.isNotEmpty()) newPage()
        return pages
    }

    private fun newPage() {
        pages += Page(pages.size + 1, lines)
        lines = mutableListOf()
        used = 0
        lastChunk = null
    }

    private fun add(chunk: Chunk, next: Chunk?) {
        if (fits(chunk, next)) return placeWhole(chunk)
        // A transition never starts a page: the chunk before it comes along (§3.3).
        if (chunk is Chunk.Paragraph && chunk.part.type == ElementType.TRANSITION && used > 0) {
            carryLastChunkToNextPage()
            if (fits(chunk, next)) return placeWhole(chunk)
        }
        if (used > 0) {
            val split = split(chunk, bodyLines - used - gapFor(chunk), forced = false)
            if (split != null) return placeSplit(split)
            newPage()
        }
        placeOnFreshPages(chunk)
    }

    private fun gapFor(chunk: Chunk) = if (used == 0) 0 else spaceBefore(chunk)

    private fun spaceBefore(chunk: Chunk): Int = when (chunk) {
        is Chunk.Paragraph -> chunk.part.spaceBefore
        is Chunk.Speech -> chunk.cue.spaceBefore
        is Chunk.Dual -> chunk.left.cue.spaceBefore
        Chunk.ForcedBreak -> 0
    }

    /** Body rows the chunk takes, not counting its space before. */
    private fun height(chunk: Chunk): Int = when (chunk) {
        is Chunk.Paragraph -> chunk.part.rows.size
        is Chunk.Speech -> cueRows(chunk) + chunk.parts.sumOf { it.height }
        is Chunk.Dual -> maxOf(height(chunk.left), height(chunk.right))
        Chunk.ForcedBreak -> 0
    }

    private fun cueRows(speech: Chunk.Speech) = if (speech.continued && inMargins) 0 else speech.cue.rows.size

    private fun fits(chunk: Chunk, next: Chunk?) =
        used + gapFor(chunk) + height(chunk) + keepWithNext(chunk, next) <= bodyLines

    // A scene heading or shot needs the start of what follows on its page (§3.3):
    // at least a few lines, or all of it if it can't split.
    private fun keepWithNext(chunk: Chunk, next: Chunk?): Int {
        if (chunk !is Chunk.Paragraph) return 0
        if (chunk.part.type != ElementType.SCENE_HEADING && chunk.part.type != ElementType.SHOT) return 0
        if (next == null || next is Chunk.ForcedBreak) return 0
        val smallestStart = splits(next, bodyLines).firstOrNull()?.let { height(it.head) } ?: height(next)
        return spaceBefore(next) + smallestStart
    }

    private class Split(val head: Chunk, val tail: Chunk)

    /**
     * The split that keeps the most on this page within [room] rows, or null if
     * the rules allow none. [forced] splits between any two lines, for a chunk
     * taller than a whole page.
     */
    private fun split(chunk: Chunk, room: Int, forced: Boolean): Split? =
        if (forced) forcedSplit(chunk, room) else splits(chunk, room).lastOrNull()

    /** Every split the rules allow within [room] rows, smallest first page first. */
    private fun splits(chunk: Chunk, room: Int): List<Split> = when (chunk) {
        is Chunk.Paragraph -> paragraphSplits(chunk.part, room)
        is Chunk.Speech -> speechSplits(chunk, room)
        else -> emptyList()
    }

    private fun forcedSplit(chunk: Chunk, room: Int): Split? = when (chunk) {
        is Chunk.Paragraph -> chunk.part.takeIf { room >= 1 && it.rows.size > room }
            ?.let { Split(Chunk.Paragraph(it.headRows(room)), Chunk.Paragraph(it.tailRows(room))) }
        is Chunk.Speech -> speechSplits(chunk, room, forced = true).lastOrNull()
        else -> null
    }

    private fun paragraphSplits(part: Part, room: Int): List<Split> {
        if (part.type != ElementType.ACTION) return emptyList()
        val min = template.minActionLinesPerSide
        return candidates(part)
            .filter { (head, tail) -> head.rows.size in min..room && tail.rows.size >= min }
            .map { (head, tail) -> Split(Chunk.Paragraph(head), Chunk.Paragraph(tail)) }
    }

    // Where a paragraph may break: after each sentence, or after each line when
    // the template doesn't keep sentences together.
    private fun candidates(part: Part): List<Pair<Part, Part>> =
        if (template.breakAtSentences) {
            SENTENCE_BREAK.findAll(part.text).map { it.range.last + 1 }
                .filter { it < part.text.length }
                .map { part.head(it) to part.tail(it) }
                .filter { (_, tail) -> tail.text.isNotEmpty() }
                .toList()
        } else {
            lineCandidates(part)
        }

    private fun lineCandidates(part: Part) = (1 until part.rows.size).map { part.headRows(it) to part.tailRows(it) }

    // A speech breaks inside dialogue or between its parts. Final Draft lets a
    // parenthetical end the page, just above (MORE).
    private fun speechSplits(speech: Chunk.Speech, room: Int, forced: Boolean = false): List<Split> {
        val more = if (inMargins) 0 else 1
        val result = mutableListOf<Split>()
        fun consider(head: List<Part>, tail: List<Part>) {
            if (head.isEmpty() || tail.isEmpty()) return
            if (cueRows(speech) + head.sumOf { it.height } + more > room) return
            if (!forced) {
                if (dialogueRows(head) < template.minDialogueLinesBefore) return
                if (dialogueRows(tail) < template.minDialogueLinesAfter) return
            }
            result += Split(
                Chunk.Speech(speech.cue, head, speech.continued),
                Chunk.Speech(continuedCue(speech.cue), tail, continued = true),
            )
        }
        speech.parts.forEachIndexed { i, part ->
            val before = speech.parts.take(i)
            val after = speech.parts.drop(i + 1)
            if (i > 0) consider(before, speech.parts.drop(i))
            if (part.type == ElementType.DIALOGUE || part.type == ElementType.LYRIC) {
                val pieces = if (forced) lineCandidates(part) else candidates(part)
                pieces.forEach { (head, tail) -> consider(before + head, listOf(tail) + after) }
            }
        }
        return result
    }

    private fun dialogueRows(parts: List<Part>) =
        parts.filter { it.type == ElementType.DIALOGUE || it.type == ElementType.LYRIC }.sumOf { it.rows.size }

    private fun continuedCue(cue: Part): Part {
        val base = CONTINUED_EXTENSION.replace(cue.text, "").trim()
        return Part(cue.type, cue.blockId, cue.format, "$base ${template.continued}", 0)
    }

    private fun carryLastChunkToNextPage() {
        val chunk = lastChunk ?: return
        if (chunk is Chunk.Paragraph && chunk.part.type == ElementType.SCENE_HEADING) return
        repeat(lines.size - lastChunkStart) { lines.removeAt(lines.lastIndex) }
        used = lastChunkUsedBefore
        newPage()
        placeWhole(chunk)
    }

    private fun placeOnFreshPages(chunk: Chunk) {
        if (height(chunk) <= bodyLines) return placeWhole(chunk)
        val split = split(chunk, bodyLines, forced = false) ?: split(chunk, bodyLines, forced = true)
            ?: return placeWhole(chunk)
        placeSplit(split)
    }

    private fun placeSplit(split: Split) {
        placeWhole(split.head)
        val head = split.head
        if (head is Chunk.Speech) {
            lines += PageLine(used + 1, template.more, ElementType.CHARACTER, head.cue.format)
            if (!inMargins) used++
        }
        newPage()
        placeOnFreshPages(split.tail)
    }

    private fun placeWhole(chunk: Chunk) {
        lastChunk = chunk
        lastChunkStart = lines.size
        lastChunkUsedBefore = used
        used += gapFor(chunk)
        when (chunk) {
            is Chunk.Paragraph -> chunk.part.rows.forEach(::put)
            is Chunk.Speech -> placeSpeech(chunk)
            is Chunk.Dual -> placeDual(chunk)
            Chunk.ForcedBreak -> Unit
        }
    }

    private fun placeSpeech(speech: Chunk.Speech) {
        if (speech.continued && inMargins && used == 0) {
            speech.cue.rows.forEach { lines += PageLine(0, it.text, it.type, it.format, it.blockId) }
        } else {
            speech.cue.rows.forEach(::put)
        }
        speech.parts.forEach { part ->
            used += part.spaceBefore
            part.rows.forEach(::put)
        }
    }

    private fun placeDual(dual: Chunk.Dual) {
        val start = used
        listOf(dual.left, dual.right).forEachIndexed { column, speech ->
            var row = start
            (listOf(speech.cue) + speech.parts).forEach { part ->
                if (part !== speech.cue) row += part.spaceBefore
                part.rows.forEach { lines += PageLine(++row, it.text, it.type, it.format, it.blockId, column) }
            }
        }
        used = start + height(dual)
    }

    private fun put(row: Row) {
        used++
        lines += PageLine(used, row.text, row.type, row.format, row.blockId)
    }

    companion object {
        /** The end of a sentence: punctuation, closing quotes or brackets, then a space. */
        val SENTENCE_BREAK = Regex("""[.?!]["'”’)\]]*(?=\s)""")
        val CONTINUED_EXTENSION = Regex("""\s*\(CONT['’]?D\)""", RegexOption.IGNORE_CASE)
    }
}
