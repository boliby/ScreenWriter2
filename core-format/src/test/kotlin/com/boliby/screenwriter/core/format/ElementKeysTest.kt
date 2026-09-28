package com.boliby.screenwriter.core.format

import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.ElementType.DIALOGUE
import com.boliby.screenwriter.core.model.ElementType.PARENTHETICAL
import com.boliby.screenwriter.core.model.ElementType.SCENE_HEADING
import com.boliby.screenwriter.core.model.ElementType.SHOT
import com.boliby.screenwriter.core.model.ElementType.TRANSITION
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementKeysTest {
    // The §2.4 table: Enter (has text), Enter (blank), Tab (blank), Tab (has text).
    private val table = mapOf(
        SCENE_HEADING to listOf(ACTION, ACTION, ACTION, null),
        ACTION to listOf(ACTION, CHARACTER, CHARACTER, null),
        CHARACTER to listOf(DIALOGUE, ACTION, TRANSITION, PARENTHETICAL),
        PARENTHETICAL to listOf(DIALOGUE, DIALOGUE, DIALOGUE, DIALOGUE),
        DIALOGUE to listOf(ACTION, ACTION, PARENTHETICAL, PARENTHETICAL),
        TRANSITION to listOf(SCENE_HEADING, SCENE_HEADING, SCENE_HEADING, null),
    )

    @Test
    fun enterWithTextSplitsAtCursor() {
        for ((type, row) in table) {
            assertEquals(EnterAction.Split(3, row[0]!!), ElementKeys.enter(type, "abcdef", 3), "$type")
        }
    }

    @Test
    fun enterAtEndSplitsIntoBlankBlock() {
        assertEquals(EnterAction.Split(4, DIALOGUE), ElementKeys.enter(CHARACTER, "MAYA", 4))
    }

    @Test
    fun enterInBlankBlockChangesType() {
        for ((type, row) in table) {
            assertEquals(EnterAction.ChangeType(row[1]!!), ElementKeys.enter(type, "", 0), "$type")
            assertEquals(EnterAction.ChangeType(row[1]!!), ElementKeys.enter(type, "  ", 1), "$type")
        }
    }

    @Test
    fun enterAtStartOfTextInsertsAbove() {
        assertEquals(EnterAction.InsertAbove, ElementKeys.enter(DIALOGUE, "Hello.", 0))
    }

    @Test
    fun tabInBlankBlockChangesType() {
        for ((type, row) in table) {
            assertEquals(TabAction.ChangeType(row[2]!!), ElementKeys.tab(type, ""), "$type")
        }
    }

    @Test
    fun tabWithTextInsertsNextElementOrDoesNothing() {
        for ((type, row) in table) {
            val expected = row[3]?.let { TabAction.InsertAfter(it) } ?: TabAction.None
            assertEquals(expected, ElementKeys.tab(type, "text"), "$type")
        }
    }

    @Test
    fun otherElementsReturnToAction() {
        assertEquals(EnterAction.Split(4, ACTION), ElementKeys.enter(SHOT, "WIDE", 4))
        assertEquals(EnterAction.ChangeType(ACTION), ElementKeys.enter(SHOT, "", 0))
        assertEquals(TabAction.ChangeType(ACTION), ElementKeys.tab(SHOT, ""))
        assertEquals(TabAction.None, ElementKeys.tab(SHOT, "WIDE"))
    }

    @Test
    fun shiftTabStepsBackwardAndWraps() {
        assertEquals(TRANSITION, ElementKeys.shiftTab(SCENE_HEADING))
        assertEquals(CHARACTER, ElementKeys.shiftTab(PARENTHETICAL))
        assertEquals(ACTION, ElementKeys.shiftTab(SHOT))
        var type: ElementType = DIALOGUE
        repeat(ElementKeys.cycle.size) { type = ElementKeys.shiftTab(type) }
        assertEquals(DIALOGUE, type)
    }

    @Test
    fun backspaceAtStart() {
        assertEquals(BackspaceAction.None, ElementKeys.backspaceAtStart(ACTION, "", previous = null))
        assertEquals(BackspaceAction.DeleteBlock, ElementKeys.backspaceAtStart(DIALOGUE, "", CHARACTER))
        assertEquals(BackspaceAction.MergeWithPrevious, ElementKeys.backspaceAtStart(ACTION, "More.", ACTION))
        assertEquals(BackspaceAction.ChangeType(CHARACTER), ElementKeys.backspaceAtStart(DIALOGUE, "Hi.", CHARACTER))
    }
}
