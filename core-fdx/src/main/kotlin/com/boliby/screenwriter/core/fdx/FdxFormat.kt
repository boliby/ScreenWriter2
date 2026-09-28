package com.boliby.screenwriter.core.fdx

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.ElementType

/** Names and keys shared by the reader and writer (§4.2). */
internal object FdxFormat {
    /** Script extra: the original document with its script paragraphs removed. */
    const val DOCUMENT = "fdx.document"

    /** Block extra: the paragraph exactly as it was read. */
    const val XML = "fdx.xml"

    /** Block extra: [signature] when read; the paragraph is written back verbatim while it matches. */
    const val SIGNATURE = "fdx.signature"

    /** Block extra on the first block of a dual-dialogue pair: the container paragraph's attributes. */
    const val DUAL_CONTAINER = "fdx.dualContainer"

    val TYPES = mapOf(
        "Scene Heading" to ElementType.SCENE_HEADING,
        "Action" to ElementType.ACTION,
        "Character" to ElementType.CHARACTER,
        "Parenthetical" to ElementType.PARENTHETICAL,
        "Dialogue" to ElementType.DIALOGUE,
        "Transition" to ElementType.TRANSITION,
        "Shot" to ElementType.SHOT,
    )

    /** Final Draft has no lyric element; scripts add their own, under names like these. */
    private val LYRIC_NAMES = setOf("Singing", "Lyrics", "Lyric", "Song")

    fun typeFor(name: String): ElementType = when {
        name in TYPES -> TYPES.getValue(name)
        name in LYRIC_NAMES -> ElementType.LYRIC
        else -> ElementType.ACTION
    }

    fun typeName(type: ElementType): String = when (type) {
        ElementType.SCENE_HEADING -> "Scene Heading"
        ElementType.CHARACTER -> "Character"
        ElementType.PARENTHETICAL -> "Parenthetical"
        ElementType.DIALOGUE, ElementType.LYRIC -> "Dialogue"
        ElementType.TRANSITION -> "Transition"
        ElementType.SHOT -> "Shot"
        else -> "Action"
    }

    /** Everything about a block that the written paragraph depends on. */
    fun signature(block: Block, startsNewPage: Boolean): String =
        listOf(
            block.type, block.text, block.spans, block.dual, block.sceneNumber,
            startsNewPage, block.extras["element"],
        ).joinToString("\u0000")
}
