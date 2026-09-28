package com.boliby.screenwriter

import android.os.Looper
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModelProvider
import com.boliby.screenwriter.core.model.ElementType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Launches the real app on Robolectric's simulated Android and drives the
 * editor, so crashes and broken wiring show up before a build reaches a phone.
 * It can't judge keyboard behavior; that still needs real devices.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val editor get() = ViewModelProvider(compose.activity)[EditorViewModel::class.java].editor

    // Runs posted editor work and recomposes.
    private fun settle() {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    @Test
    fun opensWithTheSampleScene() {
        compose.onNodeWithText("Rain streaks", substring = true).assertExists()
        assertEquals(8, editor.blocks.size)
    }

    @Test
    fun enterSplitsALineAndUndoJoinsIt() {
        val action = compose.onNodeWithText("Rain streaks", substring = true)
        action.performClick()
        action.performTextInputSelection(TextRange(4))
        action.performTextInput("\n")
        settle()
        assertEquals(9, editor.blocks.size)
        assertEquals("Rain", editor.blocks[1].text)
        assertEquals(" streaks the windows.", editor.blocks[2].text.take(21))

        compose.onNodeWithText("Undo").performClick()
        settle()
        assertEquals(8, editor.blocks.size)
        assertEquals("Rain streaks", editor.blocks[1].text.take(12))

        compose.onNodeWithText("Redo").performClick()
        settle()
        assertEquals(9, editor.blocks.size)
    }

    @Test
    fun typingIsUndoable() {
        val heading = compose.onNodeWithText("int. coffee shop", substring = true, ignoreCase = true)
        heading.performClick()
        heading.performTextInputSelection(TextRange(0))
        heading.performTextInput("X")
        settle()
        assertEquals("Xint. coffee shop - night", editor.blocks[0].text)
        compose.onNodeWithText("Undo").performClick()
        settle()
        assertEquals("int. coffee shop - night", editor.blocks[0].text)
    }

    @Test
    fun backspaceAtTheStartChangesThenJoins() {
        // "We close in five." is dialogue under a character cue.
        val dialogue = compose.onNodeWithText("We close in five.")
        dialogue.performClick()
        dialogue.performTextInputSelection(TextRange(0))
        dialogue.performKeyInput { pressKey(Key.Backspace) }
        settle()
        assertEquals(ElementType.CHARACTER, editor.blocks[3].type)
        compose.onNodeWithText("We close in five.").performKeyInput { pressKey(Key.Backspace) }
        settle()
        assertEquals(7, editor.blocks.size)
        assertEquals("Barista (O.S.)We close in five.", editor.blocks[2].text)
        compose.onNodeWithText("Undo").performClick()
        settle()
        compose.onNodeWithText("Undo").performClick()
        settle()
        assertEquals(8, editor.blocks.size)
        assertEquals(ElementType.DIALOGUE, editor.blocks[3].type)
    }

    @Test
    fun tabAndElementChips() {
        compose.onNodeWithText("Blank").performClick()
        settle()
        assertEquals(1, editor.blocks.size)
        compose.onNodeWithText("Tab").performClick()
        settle()
        assertEquals(ElementType.ACTION, editor.blocks[0].type)
        // The bar scrolls sideways; on a narrow screen the chip starts out of view.
        compose.onNodeWithText("Character").performScrollTo().performClick()
        settle()
        assertEquals(ElementType.CHARACTER, editor.blocks[0].type)
    }

    @Test
    fun pastingSeveralLinesMakesBlocksInOneUndoStep() {
        compose.onNodeWithText("Blank").performClick()
        settle()
        compose.onNodeWithText("Tab").performClick()
        settle()
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("One.\nTwo.\nThree.")
        settle()
        assertEquals(listOf("One.", "Two.", "Three."), editor.blocks.map { it.text })
        compose.onNodeWithText("Undo").performClick()
        settle()
        assertEquals(listOf(""), editor.blocks.map { it.text })
    }
}
