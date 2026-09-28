package com.boliby.screenwriter.core.fdx

import com.boliby.screenwriter.core.layout.Align
import com.boliby.screenwriter.core.layout.ElementFormat
import com.boliby.screenwriter.core.layout.PageTemplate
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script
import org.w3c.dom.Element
import kotlin.math.roundToInt

/**
 * The page layout stored in an imported `.fdx` file: element indents and
 * spacing, margins, and (MORE)/(CONT'D) text. Laying a script out with its own
 * template reproduces the writer's Final Draft pagination.
 */
object FdxTemplate {
    fun from(script: Script): PageTemplate? {
        val root = script.extras[FdxFormat.DOCUMENT]?.let(Xml::parse)?.documentElement ?: return null
        val named = root.children("ElementSettings").mapNotNull { settings ->
            settings.child("ParagraphSpec")?.let { settings.getAttribute("Type") to format(it) }
        }.toMap()
        val action = named["Action"] ?: return null
        val formats = buildMap {
            for ((name, type) in FdxFormat.TYPES) named[name]?.let { put(type, it) }
            put(ElementType.CENTERED, action.copy(align = Align.CENTER))
            named["Dialogue"]?.let { put(ElementType.LYRIC, it) }
        }
        val layout = root.child("PageLayout")
        val top = layout?.attr("TopMargin")?.toDoubleOrNull() ?: 72.0
        val bottom = layout?.attr("BottomMargin")?.toDoubleOrNull() ?: 72.0
        val breaks = root.child("MoresAndContinueds")?.child("DialogueBreaks")
        val defaults = PageTemplate.US_LETTER
        return defaults.copy(
            formats = formats,
            namedFormats = named,
            bodyLines = ((defaults.pageHeight * 72 - top - bottom) / 12).toInt(),
            topMargin = top / 72,
            breakAtSentences = layout?.attr("BreakDialogueAndActionAtSentences") != "No",
            more = breaks?.attr("DialogueBottom") ?: defaults.more,
            continued = breaks?.attr("DialogueTop") ?: defaults.continued,
        )
    }

    private fun format(spec: Element) = ElementFormat(
        left = spec.number("LeftIndent", 1.5),
        right = spec.number("RightIndent", 7.5),
        spaceBefore = (spec.number("SpaceBefore", 0.0) / 12).roundToInt(),
        align = when (spec.getAttribute("Alignment")) {
            "Center" -> Align.CENTER
            "Right" -> Align.RIGHT
            else -> Align.LEFT
        },
        firstIndent = spec.number("FirstIndent", 0.0),
        lineSpacing = spec.number("Spacing", 1.0).roundToInt().coerceAtLeast(1),
    )

    private fun Element.number(name: String, default: Double) = attr(name)?.toDoubleOrNull() ?: default
}
