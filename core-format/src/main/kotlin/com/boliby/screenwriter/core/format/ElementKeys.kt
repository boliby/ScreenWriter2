package com.boliby.screenwriter.core.format

import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.ElementType.DIALOGUE
import com.boliby.screenwriter.core.model.ElementType.PARENTHETICAL
import com.boliby.screenwriter.core.model.ElementType.SCENE_HEADING
import com.boliby.screenwriter.core.model.ElementType.TRANSITION

/** What pressing Enter does in a block. */
sealed interface EnterAction {
    /** The block is blank: change its type instead of adding another block. */
    data class ChangeType(val type: ElementType) : EnterAction

    /** Text after [at] moves to a new block of [newType] below. */
    data class Split(val at: Int, val newType: ElementType) : EnterAction

    /** The cursor is at the start of a non-blank block: add a blank block of the same type above. */
    data object InsertAbove : EnterAction
}

/** What pressing Tab does in a block. */
sealed interface TabAction {
    data class ChangeType(val type: ElementType) : TabAction

    data class InsertAfter(val type: ElementType) : TabAction

    data object None : TabAction
}

/** What pressing Backspace does with the cursor at the start of a block. */
sealed interface BackspaceAction {
    /** The block is blank: remove it. */
    data object DeleteBlock : BackspaceAction

    /** Append this block's text to the previous block, which has the same type. */
    data object MergeWithPrevious : BackspaceAction

    /** The previous block has a different type: take its type, so a second Backspace merges. */
    data class ChangeType(val type: ElementType) : BackspaceAction

    /** First block: nothing to do. */
    data object None : BackspaceAction
}

/**
 * Enter/Tab/Backspace rules from the §2.4 table. Where §2.4 and the §2.9 sketch
 * disagree (Tab in a non-blank Action block), this follows the table: no-op.
 * Tab accepting suggestions and heading parts arrives with autocomplete (§2.5).
 */
object ElementKeys {
    private class Rule(
        val onEnter: ElementType,
        val onEnterBlank: ElementType,
        val onTabBlank: ElementType,
        val onTab: ElementType?,
    )

    private val rules = mapOf(
        SCENE_HEADING to Rule(ACTION, ACTION, ACTION, onTab = null),
        ACTION to Rule(ACTION, CHARACTER, CHARACTER, onTab = null),
        CHARACTER to Rule(DIALOGUE, ACTION, TRANSITION, onTab = PARENTHETICAL),
        PARENTHETICAL to Rule(DIALOGUE, DIALOGUE, DIALOGUE, onTab = DIALOGUE),
        DIALOGUE to Rule(ACTION, ACTION, PARENTHETICAL, onTab = PARENTHETICAL),
        TRANSITION to Rule(SCENE_HEADING, SCENE_HEADING, SCENE_HEADING, onTab = null),
    )

    // Shot and the other elements go back to Action (§2.1).
    private val fallback = Rule(ACTION, ACTION, ACTION, onTab = null)

    /** The elements that Tab and Shift+Tab step through, in order. */
    val cycle = listOf(SCENE_HEADING, ACTION, CHARACTER, PARENTHETICAL, DIALOGUE, TRANSITION)

    private fun rule(type: ElementType) = rules[type] ?: fallback

    /** The type of the block that Enter creates after a non-blank [type] block. */
    fun next(type: ElementType): ElementType = rule(type).onEnter

    /** [text] is the block's text and [cursor] where Enter was pressed in it. */
    fun enter(type: ElementType, text: String, cursor: Int): EnterAction = when {
        text.isBlank() -> EnterAction.ChangeType(rule(type).onEnterBlank)
        cursor == 0 -> EnterAction.InsertAbove
        else -> EnterAction.Split(cursor.coerceAtMost(text.length), next(type))
    }

    fun tab(type: ElementType, text: String): TabAction {
        val rule = rule(type)
        return when {
            text.isBlank() -> TabAction.ChangeType(rule.onTabBlank)
            rule.onTab != null -> TabAction.InsertAfter(rule.onTab)
            else -> TabAction.None
        }
    }

    /** Shift+Tab steps backward through [cycle]. */
    fun shiftTab(type: ElementType): ElementType {
        val i = cycle.indexOf(type)
        return if (i < 0) ACTION else cycle[(i - 1 + cycle.size) % cycle.size]
    }

    /** [previous] is the type of the block above, or null for the first block. */
    fun backspaceAtStart(type: ElementType, text: String, previous: ElementType?): BackspaceAction = when {
        previous == null -> BackspaceAction.None
        text.isBlank() -> BackspaceAction.DeleteBlock
        previous == type -> BackspaceAction.MergeWithPrevious
        else -> BackspaceAction.ChangeType(previous)
    }
}
