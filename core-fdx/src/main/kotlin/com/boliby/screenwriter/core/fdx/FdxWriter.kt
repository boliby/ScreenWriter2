package com.boliby.screenwriter.core.fdx

import com.boliby.screenwriter.core.layout.PageTemplate
import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.core.model.Style
import com.boliby.screenwriter.core.model.TitleField
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Writes Final Draft `.fdx` files (§4.2). A script read by [FdxReader] is written
 * into its original document, so element settings, SmartType lists, revisions,
 * and anything else this app doesn't interpret come back unchanged. Paragraphs
 * the writer hasn't edited are written back exactly as they were read.
 *
 * Notes, sections, and synopses have no place in the script text and are left
 * out.
 */
object FdxWriter {
    fun write(script: Script): String {
        val document = script.extras[FdxFormat.DOCUMENT]?.let(Xml::parse) ?: newDocument(script.titlePage)
        val root = document.documentElement
        val content = root.child("Content") ?: document.createElement("Content").also {
            root.insertBefore(it, root.firstChild)
        }
        while (content.hasChildNodes()) content.removeChild(content.firstChild)

        ContentWriter(document, content).write(script.blocks)
        content.appendChild(document.createTextNode("\n  "))
        return Xml.serialize(document, declaration = true)
    }

    private fun newDocument(titlePage: List<TitleField>): Document {
        val document = Xml.parse("""<FinalDraft DocumentType="Script" Template="No" Version="1"><Content/></FinalDraft>""")
        if (titlePage.isNotEmpty()) document.documentElement.appendChild(TitlePageWriter.write(document, titlePage))
        return document
    }
}

private class ContentWriter(private val document: Document, private val content: Element) {
    fun write(blocks: List<Block>) {
        var startsNewPage = false
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            when {
                block.type == ElementType.PAGE_BREAK -> {
                    startsNewPage = true
                    i++
                }
                !block.type.prints -> i++
                block.type == ElementType.CHARACTER && dualPartnerAfter(blocks, i) != null -> {
                    val end = speechEnd(blocks, speechEnd(blocks, i))
                    append(dualDialogue(blocks.subList(i, end), startsNewPage))
                    startsNewPage = false
                    i = end
                }
                else -> {
                    append(paragraph(block, startsNewPage, inSpeech = inSpeech(blocks, i)))
                    startsNewPage = false
                    i++
                }
            }
        }
    }

    // Whether the block at [index] belongs to the speech of a character cue above it.
    private fun inSpeech(blocks: List<Block>, index: Int): Boolean {
        val previous = blocks.subList(0, index).lastOrNull { it.type.prints } ?: return false
        return previous.type in SPEECH_TYPES
    }

    private fun append(element: Element) {
        content.appendChild(document.createTextNode("\n    "))
        content.appendChild(element)
    }

    private fun speechEnd(blocks: List<Block>, cue: Int): Int {
        var end = cue + 1
        while (end < blocks.size && blocks[end].type.let {
                it == ElementType.DIALOGUE || it == ElementType.PARENTHETICAL || it == ElementType.LYRIC
            }
        ) end++
        return end
    }

    // The right-hand cue of a dual-dialogue pair, if the speech at [cue] has one.
    private fun dualPartnerAfter(blocks: List<Block>, cue: Int): Int? {
        if (blocks[cue].dual) return null
        val next = speechEnd(blocks, cue)
        return next.takeIf { it < blocks.size && blocks[it].type == ElementType.CHARACTER && blocks[it].dual }
    }

    private fun dualDialogue(speeches: List<Block>, startsNewPage: Boolean): Element {
        val container = speeches.first().extras[FdxFormat.DUAL_CONTAINER]
            ?.let { document.importNode(Xml.parse(it).documentElement, true) as Element }
            ?: document.createElement("Paragraph")
        setStartsNewPage(container, startsNewPage)
        val dual = document.createElement("DualDialogue")
        speeches.forEach { block ->
            dual.appendChild(document.createTextNode("\n        "))
            dual.appendChild(paragraph(block, startsNewPage = false))
        }
        dual.appendChild(document.createTextNode("\n      "))
        container.appendChild(document.createTextNode("\n      "))
        container.appendChild(dual)
        container.appendChild(document.createTextNode("\n    "))
        return container
    }

    private fun paragraph(block: Block, startsNewPage: Boolean, inSpeech: Boolean = true): Element {
        val original = block.extras[FdxFormat.XML]
            ?.let { document.importNode(Xml.parse(it).documentElement, true) as Element }
        if (original != null && block.extras[FdxFormat.SIGNATURE] == FdxFormat.signature(block, startsNewPage)) {
            return original
        }
        // Edited or new: keep the original's other attributes and child elements
        // (such as SceneProperties), and rebuild the type and text.
        val paragraph = original ?: document.createElement("Paragraph")
        val runTemplate = original?.child("Text")
        original?.children("Text")?.forEach { paragraph.removeChild(it) }

        // An imported element name (such as General) only applies while the block
        // is still the type it was read as.
        val element = block.extras[PageTemplate.ELEMENT_EXTRA]?.takeIf {
            FdxFormat.typeFor(it) == block.type || (block.type == ElementType.CENTERED && FdxFormat.typeFor(it) == ElementType.ACTION)
        }
        // Final Draft has no lyric element: lyrics in a speech are dialogue, and
        // lyrics on their own are action.
        val name = when {
            element != null -> element
            block.type == ElementType.LYRIC && !inSpeech -> "Action"
            else -> FdxFormat.typeName(block.type)
        }
        paragraph.setAttribute("Type", name)
        when {
            block.type == ElementType.CENTERED -> paragraph.setAttribute("Alignment", "Center")
            paragraph.getAttribute("Alignment") == "Center" -> paragraph.removeAttribute("Alignment")
        }
        if (block.sceneNumber != null) paragraph.setAttribute("Number", block.sceneNumber) else paragraph.removeAttribute("Number")
        setStartsNewPage(paragraph, startsNewPage)
        block.extras[PageTemplate.SPACE_BEFORE_EXTRA]?.toIntOrNull()?.let { paragraph.setAttribute("SpaceBefore", (it * 12).toString()) }
        textRuns(block, runTemplate).forEach { paragraph.appendChild(it) }
        return paragraph
    }

    private fun setStartsNewPage(paragraph: Element, startsNewPage: Boolean) {
        when {
            startsNewPage -> paragraph.setAttribute("StartsNewPage", "Yes")
            paragraph.hasAttribute("StartsNewPage") -> paragraph.setAttribute("StartsNewPage", "No")
        }
    }

    // One <Text> run per stretch of text with the same styles. Runs copy the
    // original run's font attributes, and any style this app doesn't model.
    private fun textRuns(block: Block, template: Element?): List<Element> {
        val cuts = (listOf(0, block.text.length) + block.spans.flatMap { listOf(it.start, it.end) })
            .filter { it in 0..block.text.length }.distinct().sorted()
        val otherStyles = template?.getAttribute("Style")?.split('+')?.filter { it.isNotEmpty() && it !in STYLE_NAMES.values }.orEmpty()
        val ranges = cuts.zipWithNext().ifEmpty { listOf(0 to 0) }
        return ranges.map { (start, end) ->
            val run = (template?.cloneNode(false) as Element?) ?: document.createElement("Text")
            val styles = block.spans.filter { it.start <= start && it.end >= end && start < end }.map { it.style }.toSet()
            val style = (Style.entries.filter { it in styles }.map { STYLE_NAMES.getValue(it) } + otherStyles).joinToString("+")
            if (style.isNotEmpty() || run.hasAttribute("Style")) run.setAttribute("Style", style)
            run.textContent = block.text.substring(start, end)
            run
        }
    }

    private companion object {
        val SPEECH_TYPES = setOf(ElementType.CHARACTER, ElementType.DIALOGUE, ElementType.PARENTHETICAL, ElementType.LYRIC)
        val STYLE_NAMES = mapOf(Style.BOLD to "Bold", Style.ITALIC to "Italic", Style.UNDERLINE to "Underline")
    }
}

/** A title page for scripts that didn't come from Final Draft: centered credits, contact at the bottom left. */
private object TitlePageWriter {
    private val CENTERED = setOf("title", "credit", "author", "authors", "source")

    fun write(document: Document, fields: List<TitleField>): Element {
        val titlePage = document.createElement("TitlePage")
        val content = document.createElement("Content")
        titlePage.appendChild(content)
        var first = true
        for (field in fields) {
            val centered = field.key.lowercase() in CENTERED
            for ((i, value) in field.values.withIndex()) {
                val paragraph = document.createElement("Paragraph")
                paragraph.setAttribute("Alignment", if (centered) "Center" else "Left")
                val spaceBefore = when {
                    i > 0 -> 0
                    first -> 12 * 20
                    centered -> 24
                    else -> 12 * 4
                }
                paragraph.setAttribute("SpaceBefore", spaceBefore.toString())
                val text = document.createElement("Text")
                text.textContent = value.text
                paragraph.appendChild(text)
                content.appendChild(paragraph)
                first = false
            }
        }
        return titlePage
    }
}
