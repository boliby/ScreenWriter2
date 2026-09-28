package com.boliby.screenwriter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.ui.theme.ScreenWriterTheme

private val sampleScene = listOf(
    ElementType.SCENE_HEADING to "int. coffee shop - night",
    ElementType.ACTION to "Rain streaks the windows. MAYA (30s) types on a battered laptop, the last customer left.",
    ElementType.CHARACTER to "Barista (O.S.)",
    ElementType.DIALOGUE to "We close in five.",
    ElementType.CHARACTER to "Maya",
    ElementType.PARENTHETICAL to "(not looking up)",
    ElementType.DIALOGUE to "Two more pages.",
    ElementType.TRANSITION to "cut to:",
).mapIndexed { i, (type, text) -> Block(BlockId(i.toLong()), type, text) }

/** Phase 0 placeholder: proves the app builds, installs, and uses `core-model`. */
@Composable
fun PlaceholderScreen() {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ScreenWriter", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Development build. There's no editor yet; this screen shows that the app installs and runs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SampleScene(sampleScene)
        }
    }
}

// Indents are the US Letter positions from §3.1, measured from the 1.5" left
// margin and scaled to the screen: the 6" text column fills the width.
@Composable
private fun SampleScene(blocks: List<Block>) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        val inch = maxWidth / 6f
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            blocks.forEachIndexed { i, block ->
                val gapBefore = i > 0 && block.type !in setOf(ElementType.DIALOGUE, ElementType.PARENTHETICAL)
                SceneLine(block, inch, if (gapBefore) 16.dp else 0.dp)
            }
        }
    }
}

@Composable
private fun SceneLine(block: Block, inch: Dp, gapBefore: Dp) {
    val (start, width) = when (block.type) {
        ElementType.CHARACTER -> inch * 2.2f to inch * 3.8f
        ElementType.PARENTHETICAL -> inch * 1.6f to inch * 2.5f
        ElementType.DIALOGUE -> inch * 1.0f to inch * 3.5f
        else -> 0.dp to inch * 6f
    }
    Text(
        text = if (block.type.displaysUppercase) block.text.uppercase() else block.text,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = if (block.type == ElementType.TRANSITION) TextAlign.End else TextAlign.Start,
        modifier = Modifier
            .padding(start = start, top = gapBefore)
            .width(width),
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    ScreenWriterTheme { PlaceholderScreen() }
}
