package com.boliby.screenwriter.core.fdx

import com.boliby.screenwriter.core.layout.PageTemplate
import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.core.model.Span
import com.boliby.screenwriter.core.model.Style
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Reads Final Draft `.fdx` files (§4.2). Every paragraph keeps its original XML,
 * and the document keeps everything outside the script text, so [FdxWriter] can
 * write back what this app doesn't understand unchanged.
 *
 * The title page stays in the preserved document; it isn't mapped to
 * [Script.titlePage] yet.
 */
object FdxReader {
    fun read(input: InputStream): Script = read(Xml.parse(input))

    fun read(text: String): Script = read(Xml.parse(text))

    private fun read(document: Document): Script {
        val root = document.documentElement
        require(root.tagName == "FinalDraft") { "Not a Final Draft document: <${root.tagName}>" }
        val blocks = mutableListOf<Block>()
        root.child("Content")?.children("Paragraph")?.forEach { readParagraph(it, blocks) }

        val skeleton = document.cloneNode(true) as Document
        skeleton.documentElement.child("Content")?.let { content ->
            while (content.hasChildNodes()) content.removeChild(content.firstChild)
        }
        return Script(blocks, extras = mapOf(FdxFormat.DOCUMENT to Xml.serialize(skeleton, declaration = true)))
    }

    private fun readParagraph(paragraph: Element, blocks: MutableList<Block>) {
        val startsNewPage = paragraph.getAttribute("StartsNewPage") == "Yes"
        if (startsNewPage) blocks += Block(nextId(blocks), ElementType.PAGE_BREAK, "")

        val dual = paragraph.child("DualDialogue")
        if (dual == null) {
            blocks += block(paragraph, nextId(blocks), startsNewPage)
            return
        }
        // Two speeches in one container; the second character cue starts the
        // right-hand speech.
        var cues = 0
        dual.children("Paragraph").forEachIndexed { i, inner ->
            val secondCue = inner.getAttribute("Type") == "Character" && ++cues == 2
            var block = block(inner, nextId(blocks), startsNewPage = false, dual = secondCue)
            if (i == 0) {
                val container = paragraph.cloneNode(false) as Element
                block = block.copy(extras = block.extras + (FdxFormat.DUAL_CONTAINER to Xml.serialize(container)))
            }
            blocks += block
        }
    }

    private fun nextId(blocks: List<Block>) = BlockId(blocks.size.toLong())

    private fun block(paragraph: Element, id: BlockId, startsNewPage: Boolean, dual: Boolean = false): Block {
        val typeName = paragraph.getAttribute("Type")
        var type = FdxFormat.typeFor(typeName)
        if (type == ElementType.ACTION && paragraph.getAttribute("Alignment") == "Center") type = ElementType.CENTERED

        val text = StringBuilder()
        val spans = mutableListOf<Span>()
        for (run in paragraph.children("Text")) {
            val start = text.length
            text.append(run.textContent)
            val styles = run.getAttribute("Style").split('+').mapNotNull(STYLES::get)
            styles.mapTo(spans) { Span(start, text.length, it) }
        }

        val extras = buildMap {
            if (typeName.isNotEmpty() && typeName !in FdxFormat.TYPES) put(PageTemplate.ELEMENT_EXTRA, typeName)
            // Space before, in points, set on this paragraph rather than its element.
            paragraph.attr("SpaceBefore")?.toDoubleOrNull()?.let { put(PageTemplate.SPACE_BEFORE_EXTRA, (it / 12).roundToInt().toString()) }
        }
        val block = Block(
            id = id,
            type = type,
            text = text.toString(),
            spans = mergeAdjacent(spans),
            dual = dual,
            sceneNumber = paragraph.attr("Number")?.takeIf { type == ElementType.SCENE_HEADING },
            extras = extras,
        )
        return block.copy(
            extras = block.extras + mapOf(
                FdxFormat.XML to Xml.serialize(paragraph),
                FdxFormat.SIGNATURE to FdxFormat.signature(block, startsNewPage),
            ),
        )
    }

    // Final Draft often splits one styled phrase across several runs.
    private fun mergeAdjacent(spans: List<Span>): List<Span> =
        spans.groupBy { it.style }.flatMap { (_, same) ->
            same.sortedBy { it.start }.fold(mutableListOf<Span>()) { merged, span ->
                val last = merged.lastOrNull()
                if (last != null && last.end == span.start) merged[merged.lastIndex] = last.copy(end = span.end) else merged += span
                merged
            }
        }.sortedWith(compareBy({ it.start }, { it.style }))

    private val STYLES = mapOf("Bold" to Style.BOLD, "Italic" to Style.ITALIC, "Underline" to Style.UNDERLINE)
}
