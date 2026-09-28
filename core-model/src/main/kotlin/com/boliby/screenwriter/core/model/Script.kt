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

    /** Whether this element appears on the printed page. */
    val prints: Boolean
        get() = this != SECTION && this != SYNOPSIS && this != NOTE
}

enum class Style { BOLD, ITALIC, UNDERLINE }

/** A style applied to the characters in `[start, end)` of a block's text. */
data class Span(val start: Int, val end: Int, val style: Style)

/** Text with emphasis, such as a title page value. */
data class StyledText(val text: String, val spans: List<Span> = emptyList())

@JvmInline
value class BlockId(val value: Long)

/**
 * One screenplay paragraph. The editor shows each block as its own text field.
 * [text] may contain `\n` for line breaks the writer typed inside a paragraph.
 */
data class Block(
    val id: BlockId,
    val type: ElementType,
    val text: String,
    val spans: List<Span> = emptyList(),
    /** On a CHARACTER block: this speech is the right-hand half of dual dialogue. */
    val dual: Boolean = false,
    val sceneNumber: String? = null,
    /** Values this app doesn't interpret, kept so files round-trip (§4.2). */
    val extras: Map<String, String> = emptyMap(),
)

/** A title page entry such as "Title" or "Contact". Each value is one printed line. */
data class TitleField(val key: String, val values: List<StyledText>)

data class Script(
    val blocks: List<Block>,
    val titlePage: List<TitleField> = emptyList(),
    /** Document-level data this app doesn't interpret, kept so files round-trip. */
    val extras: Map<String, String> = emptyMap(),
)
