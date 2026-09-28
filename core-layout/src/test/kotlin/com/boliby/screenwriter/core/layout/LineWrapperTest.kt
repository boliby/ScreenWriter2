package com.boliby.screenwriter.core.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class LineWrapperTest {
    @Test
    fun breaksAtSpacesAndDropsThem() {
        assertEquals(
            listOf("The rain keeps", "falling on the", "roof."),
            LineWrapper.wrap("The rain keeps falling on the roof.", 15),
        )
    }

    @Test
    fun keepsDoubleSpacesInsideALine() {
        assertEquals(listOf("She waits.  He", "leaves."), LineWrapper.wrap("She waits.  He leaves.", 14))
    }

    @Test
    fun breaksAfterAHyphenInsideAWord() {
        assertEquals(listOf("A hundred-", "dollar lure."), LineWrapper.wrap("A hundred-dollar lure.", 12))
    }

    @Test
    fun aHyphenMayHangOneColumnPastTheEdge() {
        assertEquals(listOf("He waits--", "then"), LineWrapper.wrap("He waits-- then", 9))
        assertEquals(listOf("Her mother-", "in-law."), LineWrapper.wrap("Her mother-in-law.", 10))
    }

    @Test
    fun cutsAWordLongerThanTheLine() {
        assertEquals(listOf("AAAAA", "AAAAA", "AA"), LineWrapper.wrap("AAAAAAAAAAAA", 5))
    }

    @Test
    fun keepsLeadingSpacesOnTheFirstLineOnly() {
        assertEquals(listOf("    Scott --", "Jacob"), LineWrapper.wrap("    Scott -- Jacob", 12))
    }

    @Test
    fun lineBreaksInTheTextAlwaysBreak() {
        assertEquals(listOf("Ten.", "", "Four."), LineWrapper.wrap("Ten.\n\nFour.", 30))
    }

    @Test
    fun firstLineCanBeWider() {
        assertEquals(listOf("(looking up at", "him)"), LineWrapper.wrap("(looking up at him)", 13, firstLineMaxChars = 14))
    }

    // Final Draft fits a character if it starts before the right indent.
    @Test
    fun columnWidthsMatchFinalDraft() {
        assertEquals(61, ElementFormat(left = 1.5, right = 7.5, spaceBefore = 1).maxChars)
        assertEquals(39, ElementFormat(left = 2.38, right = 6.25, spaceBefore = 0).maxChars)
        val parenthetical = ElementFormat(left = 2.88, right = 5.75, spaceBefore = 0, firstIndent = -0.1)
        assertEquals(29, parenthetical.maxChars)
        assertEquals(30, parenthetical.firstLineMaxChars)
    }
}
