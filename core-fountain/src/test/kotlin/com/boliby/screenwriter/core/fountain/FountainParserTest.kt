package com.boliby.screenwriter.core.fountain

import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CENTERED
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.ElementType.DIALOGUE
import com.boliby.screenwriter.core.model.ElementType.LYRIC
import com.boliby.screenwriter.core.model.ElementType.NOTE
import com.boliby.screenwriter.core.model.ElementType.PAGE_BREAK
import com.boliby.screenwriter.core.model.ElementType.PARENTHETICAL
import com.boliby.screenwriter.core.model.ElementType.SCENE_HEADING
import com.boliby.screenwriter.core.model.ElementType.SECTION
import com.boliby.screenwriter.core.model.ElementType.SYNOPSIS
import com.boliby.screenwriter.core.model.ElementType.TRANSITION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Cases follow the examples in the Fountain 1.1 spec, section by section.
class FountainParserTest {
    private fun elements(source: String): List<Pair<ElementType, String>> =
        FountainParser.parse(source.trimIndent()).blocks.map { it.type to it.text }

    @Test
    fun sceneHeadings() {
        val script = FountainParser.parse(
            """
            EXT. BRICK'S POOL - DAY

            .SNIPER SCOPE POV

            ...where the second-rate carnival is parked.

            ext. brick's pool - day

            INT./EXT. CAR - MOVING

            I/E TRUCK - NIGHT

            INT. HOUSE - DAY #I-1-A#
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                SCENE_HEADING to "EXT. BRICK'S POOL - DAY",
                SCENE_HEADING to "SNIPER SCOPE POV",
                ACTION to "...where the second-rate carnival is parked.",
                SCENE_HEADING to "ext. brick's pool - day",
                SCENE_HEADING to "INT./EXT. CAR - MOVING",
                SCENE_HEADING to "I/E TRUCK - NIGHT",
                SCENE_HEADING to "INT. HOUSE - DAY",
            ),
            script.blocks.map { it.type to it.text },
        )
        assertEquals("I-1-A", script.blocks.last().sceneNumber)
    }

    @Test
    fun actionKeepsLineBreaksAndIndents() {
        assertEquals(
            listOf(
                ACTION to "And then there's a long beat.\nLonger than is funny.",
                ACTION to "    Scott --\n        Jacob Billups",
            ),
            elements("And then there's a long beat.\nLonger than is funny.\n\n\tScott --\n        Jacob Billups"),
        )
    }

    @Test
    fun extraBlankLinesBecomeEmptyAction() {
        assertEquals(
            listOf(ACTION to "One.", ACTION to "", ACTION to "", ACTION to "Two."),
            elements("One.\n\n\n\nTwo."),
        )
    }

    @Test
    fun charactersAndDialogue() {
        assertEquals(
            listOf(
                CHARACTER to "MOM (O. S.)",
                DIALOGUE to "Luke! Come down for supper!",
                CHARACTER to "HANS (on the radio)",
                DIALOGUE to "What was it you said?",
                CHARACTER to "R2D2",
                DIALOGUE to "Beep.",
                ACTION to "23\nNot a name.",
                CHARACTER to "McCLANE",
                DIALOGUE to "Yippie ki-yay!",
            ),
            elements(
                """
                MOM (O. S.)
                Luke! Come down for supper!

                HANS (on the radio)
                What was it you said?

                R2D2
                Beep.

                23
                Not a name.

                @McCLANE
                Yippie ki-yay!
                """,
            ),
        )
    }

    @Test
    fun dialogueLineBreaksAndParentheticals() {
        assertEquals(
            listOf(
                CHARACTER to "STEEL",
                DIALOGUE to "They're coming out of the woodwork!",
                PARENTHETICAL to "(pause)",
                DIALOGUE to "No, everybody we've put away!",
                CHARACTER to "DEALER",
                DIALOGUE to "Ten.\nFour.\n\nHit or stand sir?",
            ),
            elements("STEEL\nThey're coming out of the woodwork!\n(pause)\nNo, everybody we've put away!\n\nDEALER\nTen.\nFour.\n  \nHit or stand sir?"),
        )
    }

    @Test
    fun allCapsActionNeedsForcing() {
        assertEquals(
            listOf(CHARACTER to "SCANNING THE AISLES...", DIALOGUE to "Where is that pit boss?"),
            elements("SCANNING THE AISLES...\nWhere is that pit boss?"),
        )
        assertEquals(
            listOf(ACTION to "SCANNING THE AISLES...\nWhere is that pit boss?"),
            elements("!SCANNING THE AISLES...\nWhere is that pit boss?"),
        )
    }

    @Test
    fun dualDialogue() {
        val blocks = FountainParser.parse("BRICK\nScrew retirement.\n\nSTEEL ^\nScrew retirement.").blocks
        assertEquals(listOf(false, false, true, false), blocks.map { it.dual })
        assertEquals("STEEL", blocks[2].text)
    }

    @Test
    fun transitions() {
        assertEquals(
            listOf(
                ACTION to "Jack argues.",
                TRANSITION to "CUT TO:",
                SCENE_HEADING to "EXT. BRICK'S POOL - DAY",
                TRANSITION to "Burn to White.",
                ACTION to "CUT TO:", // the trailing space made it Action; Action text is trimmed
            ),
            elements("Jack argues.\n\nCUT TO:\n\nEXT. BRICK'S POOL - DAY\n\n>Burn to White.\n\nCUT TO: "),
        )
    }

    @Test
    fun centeredLyricsAndPageBreaks() {
        assertEquals(
            listOf(
                CENTERED to "THE END",
                CENTERED to "BRICK BRADDOCK\n& DICK STEEL IN",
                PAGE_BREAK to "",
                LYRIC to "Willy Wonka! Willy Wonka!\nEverybody give a cheer!",
            ),
            elements("> THE END <\n\n> BRICK BRADDOCK <\n> & DICK STEEL IN <\n\n===\n\n~Willy Wonka! Willy Wonka!\n~Everybody give a cheer!"),
        )
    }

    @Test
    fun notes() {
        assertEquals(
            listOf(
                ACTION to "This is the home of DAN and JACK. They too are drinking beer.",
                NOTE to "It was supposed to be Vietnamese, right?",
                CHARACTER to "JACK",
                DIALOGUE to "Hi.",
                ACTION to "The phone RINGS. He looks around.",
            ),
            elements(
                """
                This is the home of DAN and JACK[[Or did we think of actual names?]]. They too are drinking beer.

                [[It was supposed to be Vietnamese, right?]]

                JACK
                Hi.

                The phone RINGS.[[This section needs work.
                Either that, or I need coffee.
                  
                Definitely coffee.]] He looks around.
                """,
            ),
        )
    }

    @Test
    fun boneyardIsIgnored() {
        assertEquals(
            listOf(TRANSITION to "CUT TO:", SCENE_HEADING to "EXT. PALATIAL MANSION - DAY"),
            elements("CUT TO:\n/*\nINT. GARAGE - DAY\n\nThey speed off.\n*/\nEXT. PALATIAL MANSION - DAY"),
        )
    }

    @Test
    fun sectionsAndSynopses() {
        val blocks = FountainParser.parse("# ACT I\n\n## Sequence\n\n= Set up the characters.\n\nINT. HOUSE - DAY").blocks
        assertEquals(
            listOf(SECTION to "ACT I", SECTION to "Sequence", SYNOPSIS to "Set up the characters.", SCENE_HEADING to "INT. HOUSE - DAY"),
            blocks.map { it.type to it.text },
        )
        assertEquals("2", blocks[1].extras["depth"])
    }

    @Test
    fun titlePage() {
        val script = FountainParser.parse(
            "Title:\n    _**BRICK & STEEL**_\n    _**FULL RETIRED**_\nCredit: Written by\nAuthor: Stu Maschwitz\n" +
                "Contact:\n\tNext Level Productions\n\t1588 Mission Dr.\n\nEXT. BRICK'S PATIO - DAY",
        )
        assertEquals(listOf("Title", "Credit", "Author", "Contact"), script.titlePage.map { it.key })
        assertEquals(listOf("BRICK & STEEL", "FULL RETIRED"), script.titlePage[0].values.map { it.text })
        assertEquals(listOf("Next Level Productions", "1588 Mission Dr."), script.titlePage[3].values.map { it.text })
        assertEquals(listOf(SCENE_HEADING to "EXT. BRICK'S PATIO - DAY"), script.blocks.map { it.type to it.text })
    }

    @Test
    fun fadeInIsNotATitlePage() {
        val script = FountainParser.parse("FADE IN:\n\nINT. HOUSE - DAY")
        assertTrue(script.titlePage.isEmpty())
        assertEquals(listOf(ACTION to "FADE IN:", SCENE_HEADING to "INT. HOUSE - DAY"), script.blocks.map { it.type to it.text })
    }

    @Test
    fun windowsLineEndings() {
        assertEquals(
            listOf(CHARACTER to "STEEL", DIALOGUE to "Beer's ready!"),
            elements("STEEL\r\nBeer's ready!\r\n"),
        )
    }
}
