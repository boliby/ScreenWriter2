package com.boliby.screenwriter.core.model

/** Screenplay element types (§2.4). Dual dialogue is a flag on [Block], not a type. */
enum class ElementType {
    SCENE_HEADING,
    ACTION,
    CHARACTER,
    PARENTHETICAL,
    DIALOGUE,
    TRANSITION,
    SHOT,
    CENTERED,
    LYRIC,
    SECTION,
    SYNOPSIS,
    NOTE,
    PAGE_BREAK;

    /**
     * Whether this element is shown and exported in uppercase (§2.6). Stored text
     * keeps the case the writer typed; uppercasing is display-only.
     */
    val displaysUppercase: Boolean
        get() = this == SCENE_HEADING || this == CHARACTER || this == TRANSITION || this == SHOT
}

@JvmInline
value class BlockId(val value: Long)

/** One screenplay paragraph. The editor shows each block as its own text field. */
data class Block(
    val id: BlockId,
    val type: ElementType,
    val text: String,
    val dual: Boolean = false,
)

data class Script(val blocks: List<Block>)
