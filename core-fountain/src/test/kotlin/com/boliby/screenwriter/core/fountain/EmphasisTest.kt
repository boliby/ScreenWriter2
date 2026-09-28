package com.boliby.screenwriter.core.fountain

import com.boliby.screenwriter.core.model.Span
import com.boliby.screenwriter.core.model.Style.BOLD
import com.boliby.screenwriter.core.model.Style.ITALIC
import com.boliby.screenwriter.core.model.Style.UNDERLINE
import com.boliby.screenwriter.core.model.StyledText
import kotlin.test.Test
import kotlin.test.assertEquals

// Examples from the Emphasis section of the Fountain spec.
class EmphasisTest {
    @Test
    fun basicStyles() {
        assertEquals(StyledText("italics", listOf(Span(0, 7, ITALIC))), Emphasis.parse("*italics*"))
        assertEquals(StyledText("bold", listOf(Span(0, 4, BOLD))), Emphasis.parse("**bold**"))
        assertEquals(
            StyledText("bold italics", listOf(Span(0, 12, BOLD), Span(0, 12, ITALIC))),
            Emphasis.parse("***bold italics***"),
        )
        assertEquals(StyledText("underline", listOf(Span(0, 9, UNDERLINE))), Emphasis.parse("_underline_"))
    }

    @Test
    fun nested() {
        val parsed = Emphasis.parse("INCHES AWAY. _Steel's face FILLS the *Leupold Mark 4* scope_.")
        assertEquals("INCHES AWAY. Steel's face FILLS the Leupold Mark 4 scope.", parsed.text)
        assertEquals(listOf(Span(13, 56, UNDERLINE), Span(36, 50, ITALIC)), parsed.spans)
    }

    @Test
    fun titleStyle() {
        val parsed = Emphasis.parse("_**BRICK & STEEL**_")
        assertEquals("BRICK & STEEL", parsed.text)
        assertEquals(setOf(Span(0, 13, BOLD), Span(0, 13, UNDERLINE)), parsed.spans.toSet())
    }

    @Test
    fun escapes() {
        val parsed = Emphasis.parse("""Steel enters the code on the keypad: **\*9765\***""")
        assertEquals("Steel enters the code on the keypad: *9765*", parsed.text)
        assertEquals(listOf(Span(37, 43, BOLD)), parsed.spans)
    }

    @Test
    fun spacesAroundMarkersMatter() {
        assertEquals(StyledText("He dialed *69 and then *23, and then hung up."), Emphasis.parse("He dialed *69 and then *23, and then hung up."))
        assertEquals(
            StyledText("He dialed 69 and then 23, and then hung up.", listOf(Span(10, 24, ITALIC))),
            Emphasis.parse("He dialed *69 and then 23*, and then hung up."),
        )
        assertEquals(
            StyledText("He dialed *69 and then 23*, and then hung up."),
            Emphasis.parse("""He dialed *69 and then 23\*, and then hung up."""),
        )
    }

    @Test
    fun doesNotCrossLineBreaks() {
        val text = "Brick and Steel *share a look.\nThis is going to be BAD.*"
        assertEquals(StyledText(text), Emphasis.parse(text))
    }

    @Test
    fun spansOnLaterLinesAreOffset() {
        val parsed = Emphasis.parse("Then let's retire them.\n_Permanently_.")
        assertEquals("Then let's retire them.\nPermanently.", parsed.text)
        assertEquals(listOf(Span(24, 35, UNDERLINE)), parsed.spans)
    }
}
