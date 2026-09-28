package com.boliby.screenwriter.editor

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boliby.screenwriter.core.model.ElementType

private val barTypes = listOf(
    ElementType.SCENE_HEADING,
    ElementType.ACTION,
    ElementType.CHARACTER,
    ElementType.PARENTHETICAL,
    ElementType.DIALOGUE,
    ElementType.TRANSITION,
    ElementType.SHOT,
)

// Ctrl+1 to Ctrl+7 pick an element (§2.4).
private val shortcutKeys = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven)

private fun label(type: ElementType) = when (type) {
    ElementType.SCENE_HEADING -> "Scene heading"
    ElementType.ACTION -> "Action"
    ElementType.CHARACTER -> "Character"
    ElementType.PARENTHETICAL -> "Parenthetical"
    ElementType.DIALOGUE -> "Dialogue"
    ElementType.TRANSITION -> "Transition"
    ElementType.SHOT -> "Shot"
    else -> type.name.lowercase().replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(editor: EditorState) {
    val listState = rememberLazyListState()
    val nudge = with(LocalDensity.current) { 96.dp.toPx() }

    // A block about to take focus must be on screen so its field exists. A new
    // block just below the screen gets a small scroll rather than a jump.
    LaunchedEffect(editor.pendingFocus) {
        val target = editor.pendingFocus ?: return@LaunchedEffect
        withFrameNanos { } // let the list lay out the new block first
        val index = editor.blocks.indexOfFirst { it.id == target.blockId }
        val visible = listState.layoutInfo.visibleItemsInfo
        if (index < 0 || visible.any { it.index == index }) return@LaunchedEffect
        if (index == (visible.lastOrNull()?.index ?: -2) + 1) {
            listState.scrollBy(nudge)
        } else {
            listState.scrollToItem(index)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenWriter") },
                actions = {
                    TextButton(onClick = editor::loadSample) { Text("Sample") }
                    TextButton(onClick = editor::loadBlank) { Text("Blank") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // §3.1 positions are measured on a 6" text column; it fills the width here.
                val inch = (maxWidth - 32.dp) / 6f
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
                ) {
                    // Keys must be types Android can save in a Bundle, so the raw Long, not BlockId.
                    itemsIndexed(editor.blocks, key = { _, block -> block.id.value }) { index, block ->
                        BlockField(editor, block, inch, first = index == 0)
                    }
                }
            }
            editor.focused?.let { ElementBar(editor, it) }
        }
    }
}

@Composable
private fun BlockField(editor: EditorState, block: EditorBlock, inch: Dp, first: Boolean) {
    val input = remember(block) { editor.inputTransformation(block) }
    val pending = editor.pendingFocus
    LaunchedEffect(pending) {
        if (pending?.blockId == block.id && block.focusRequester.requestFocus(FocusDirection.Enter)) {
            editor.focusApplied(pending)
        }
    }

    val type = block.type
    val (start, width) = when (type) {
        ElementType.CHARACTER -> inch * 2.2f to inch * 3.8f
        ElementType.PARENTHETICAL -> inch * 1.6f to inch * 2.5f
        ElementType.DIALOGUE -> inch * 1.0f to inch * 3.5f
        else -> 0.dp to inch * 6f
    }
    val gapBefore = !first && type != ElementType.DIALOGUE && type != ElementType.PARENTHETICAL
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = if (type == ElementType.TRANSITION) TextAlign.End else TextAlign.Start,
    )

    BasicTextField(
        state = block.state,
        modifier = Modifier
            .padding(start = start, top = if (gapBefore) 20.dp else 0.dp)
            .width(width)
            .focusRequester(block.focusRequester)
            .onFocusChanged { if (it.isFocused) editor.focusedId = block.id }
            .onPreviewKeyEvent { handleKey(it, editor, block) },
        inputTransformation = input,
        outputTransformation = if (type.displaysUppercase) UppercaseOutput else null,
        textStyle = textStyle,
        keyboardOptions = keyboardOptionsFor(type),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { getResult -> block.layout = getResult },
        decorator = { innerTextField ->
            Box {
                if (block.state.text.isEmpty()) {
                    Text(
                        label(type),
                        style = textStyle.copy(color = MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            }
        },
    )
}

private fun keyboardOptionsFor(type: ElementType): KeyboardOptions {
    val upper = type.displaysUppercase
    return KeyboardOptions(
        capitalization = when {
            upper -> KeyboardCapitalization.Characters
            type == ElementType.PARENTHETICAL -> KeyboardCapitalization.None
            else -> KeyboardCapitalization.Sentences
        },
        autoCorrectEnabled = !upper,
    )
}

private fun handleKey(event: KeyEvent, editor: EditorState, block: EditorBlock): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val selection = block.state.selection
    val atStart = selection.collapsed && selection.start == 0
    val atEnd = selection.collapsed && selection.end == block.state.text.length
    val shortcut = shortcutKeys.indexOf(event.key)
    return when {
        event.isCtrlPressed && (event.key == Key.Y || (event.key == Key.Z && event.isShiftPressed)) -> {
            editor.redo()
            true
        }
        event.isCtrlPressed && event.key == Key.Z -> {
            editor.undo()
            true
        }
        event.key == Key.Tab && event.isShiftPressed -> {
            editor.shiftTab(block)
            true
        }
        event.key == Key.Tab -> {
            editor.tab(block)
            true
        }
        event.isCtrlPressed && shortcut >= 0 -> {
            editor.setType(block, barTypes[shortcut])
            true
        }
        event.key == Key.Backspace && atStart -> editor.backspaceAtStart(block)
        event.isShiftPressed -> false
        event.key == Key.DirectionUp && selection.collapsed && onFirstLine(block) -> editor.focusPrevious(block)
        event.key == Key.DirectionDown && selection.collapsed && onLastLine(block) -> editor.focusNext(block)
        event.key == Key.DirectionLeft && atStart -> editor.focusPrevious(block)
        event.key == Key.DirectionRight && atEnd -> editor.focusNext(block)
        else -> false
    }
}

private fun onFirstLine(block: EditorBlock): Boolean {
    val layout = block.layout?.invoke() ?: return block.state.selection.start == 0
    return layout.getLineForOffset(block.state.selection.start) == 0
}

private fun onLastLine(block: EditorBlock): Boolean {
    val layout = block.layout?.invoke() ?: return block.state.selection.end == block.state.text.length
    return layout.getLineForOffset(block.state.selection.end) == layout.lineCount - 1
}

/** Sits above the soft keyboard: undo and redo, Tab, and a chip for each element. */
@Composable
private fun ElementBar(editor: EditorState, block: EditorBlock) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = editor::undo, enabled = editor.canUndo) { Text("Undo") }
            TextButton(onClick = editor::redo, enabled = editor.canRedo) { Text("Redo") }
            OutlinedButton(onClick = { editor.tab(block) }) { Text("Tab") }
            for (type in barTypes) {
                FilterChip(
                    selected = block.type == type,
                    onClick = { editor.setType(block, type) },
                    label = { Text(label(type)) },
                )
            }
        }
    }
}
