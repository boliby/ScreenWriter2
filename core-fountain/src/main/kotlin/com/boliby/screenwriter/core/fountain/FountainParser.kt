package com.boliby.screenwriter.core.fountain

import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.BlockId
import com.boliby.screenwriter.core.model.ElementType
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.core.model.TitleField

/**
 * Reads Fountain 1.1 (https://fountain.io/syntax, §4.1) into a [Script].
 * Anything that doesn't match a rule is Action, as the spec asks.
 */
object FountainParser {
    fun parse(source: String): Script {
        val text = removeBoneyard(source.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n'))
        val lines = text.split('\n')
        val (titlePage, bodyStart) = readTitlePage(lines)
        val notes = mutableListOf<String>()
        val body = extractNotes(lines.drop(bodyStart).joinToString("\n"), notes)
        return Script(BodyReader(body.split('\n'), notes).read(), titlePage)
    }

    // Keys that mark the first paragraph as a title page. Other keys are kept too,
    // but "FADE IN:" on its own must not turn the opening line into a title page.
    private val TITLE_KEYS = setOf(
        "title", "credit", "author", "authors", "source", "draft date", "date",
        "contact", "contact info", "notes", "copyright", "revision",
    )
    private val TITLE_KEY_LINE = Regex("""^([^\s:][^:]*):(.*)$""")

    private fun readTitlePage(lines: List<String>): Pair<List<TitleField>, Int> {
        val end = lines.indexOfFirst { it.isBlank() }.let { if (it < 0) lines.size else it }
        val paragraph = lines.subList(0, end)
        val keys = paragraph.mapNotNull { TITLE_KEY_LINE.find(it)?.groupValues?.get(1)?.trim()?.lowercase() }
        if (paragraph.isEmpty() || TITLE_KEY_LINE.find(paragraph[0]) == null || keys.none { it in TITLE_KEYS }) {
            return emptyList<TitleField>() to 0
        }
        val fields = mutableListOf<Pair<String, MutableList<String>>>()
        for (line in paragraph) {
            val match = TITLE_KEY_LINE.find(line)
            if (match != null && !line[0].isWhitespace()) {
                val values = mutableListOf<String>()
                match.groupValues[2].trim().takeIf { it.isNotEmpty() }?.let(values::add)
                fields += match.groupValues[1].trim() to values
            } else if (fields.isNotEmpty() && line.isNotBlank()) {
                fields.last().second += line.trim()
            }
        }
        val titlePage = fields.map { (key, values) -> TitleField(key, values.map(Emphasis::parse)) }
        return titlePage to (end + 1).coerceAtMost(lines.size)
    }

    private val BONEYARD = Regex("""/\*[\s\S]*?\*/""")

    private fun removeBoneyard(text: String) = BONEYARD.replace(text, "")

    // A note may span lines, but not a blank line (a two-space line doesn't count).
    private val NOTE = Regex("""\[\[((?:(?!\n\n)[\s\S])*?)]]""")
    internal const val NOTE_MARKER = '\u0001'

    /**
     * Notes on lines of their own become NOTE blocks, marked here by a placeholder
     * line. Notes inside other text are removed from it.
     */
    private fun extractNotes(body: String, notes: MutableList<String>): String =
        NOTE.replace(body) { match ->
            val lineStart = body.lastIndexOf('\n', match.range.first - 1) + 1
            val lineEnd = body.indexOf('\n', match.range.last + 1).let { if (it < 0) body.length else it }
            val alone = body.substring(lineStart, match.range.first).isBlank() &&
                body.substring(match.range.last + 1, lineEnd).isBlank()
            if (alone) {
                notes += match.groupValues[1].trim()
                "$NOTE_MARKER${notes.lastIndex}"
            } else {
                ""
            }
        }
}

private class BodyReader(private val lines: List<String>, private val notes: List<String>) {
    private val blocks = mutableListOf<Block>()
    private var i = 0

    fun read(): List<Block> {
        var blankLines = 0
        while (i < lines.size) {
            if (isSeparator(lines[i])) {
                blankLines++
                i++
                continue
            }
            // A single blank line separates paragraphs; each extra one is kept as an
            // empty line of Action with no blank line of its own before it.
            if (blankLines > 1 && blocks.isNotEmpty()) {
                repeat(blankLines - 1) { add(ElementType.ACTION, "", extras = mapOf(SPACE_BEFORE to "0")) }
            }
            readParagraphStart(separatedBefore = blankLines > 0 || blocks.isEmpty())
            blankLines = 0
        }
        return blocks
    }

    // Empty lines separate elements. A line of two or more spaces doesn't: it
    // keeps a blank line inside dialogue, action, or a note.
    private fun isSeparator(line: String) = line.isEmpty() || (line.isBlank() && line.length < 2)

    private fun nextIsSeparator() = i + 1 >= lines.size || isSeparator(lines[i + 1])

    private fun readParagraphStart(separatedBefore: Boolean) {
        val raw = lines[i]
        val line = raw.trim()
        when {
            line.startsWith(FountainParser.NOTE_MARKER) -> {
                add(ElementType.NOTE, notes[line.drop(1).toInt()])
                i++
            }
            PAGE_BREAK.matches(line) -> {
                add(ElementType.PAGE_BREAK, "")
                i++
            }
            line.startsWith("#") -> {
                val depth = line.takeWhile { it == '#' }.length
                add(ElementType.SECTION, line.drop(depth).trim(), extras = mapOf("depth" to depth.toString()))
                i++
            }
            line.startsWith("=") -> {
                add(ElementType.SYNOPSIS, line.drop(1).trim())
                i++
            }
            line.startsWith("~") -> readRun(ElementType.LYRIC) { it.trim().startsWith("~") }
            line.startsWith("!") -> readAction()
            line.startsWith("@") -> readCharacter()
            line.startsWith(".") && line.length > 1 && line[1].isLetterOrDigit() -> {
                addHeading(line.drop(1))
                i++
            }
            isCentered(line) -> readRun(ElementType.CENTERED) { isCentered(it.trim()) }
            line.startsWith(">") -> {
                add(ElementType.TRANSITION, line.drop(1).trim())
                i++
            }
            separatedBefore && SCENE_HEADING.containsMatchIn(line) -> {
                addHeading(line)
                i++
            }
            separatedBefore && nextIsSeparator() && isTransition(raw) -> {
                add(ElementType.TRANSITION, line)
                i++
            }
            separatedBefore && !nextIsSeparator() && isCharacterCue(line) -> readCharacter()
            else -> readAction()
        }
    }

    private fun isCentered(line: String) = line.startsWith(">") && line.endsWith("<")

    // "CUT TO: " with a trailing space is Action, so only leading space is ignored.
    private fun isTransition(raw: String): Boolean {
        val line = raw.trimStart()
        return line.endsWith("TO:") && line == line.uppercase() && line.any { it.isLetter() }
    }

    // The name must be uppercase with at least one letter. Extensions such as
    // "(cont'd)" may be any case, and a trailing ^ marks dual dialogue.
    private fun isCharacterCue(line: String): Boolean {
        val name = line.removeSuffix("^").substringBefore('(').trim()
        return name.any { it.isLetter() } && name == name.uppercase()
    }

    private fun addHeading(line: String) {
        val number = SCENE_NUMBER.find(line)
        val text = if (number != null) line.substring(0, number.range.first) else line
        add(ElementType.SCENE_HEADING, text.trim(), sceneNumber = number?.groupValues?.get(1))
    }

    private fun readRun(type: ElementType, belongs: (String) -> Boolean) {
        val parts = mutableListOf<String>()
        while (i < lines.size && !isSeparator(lines[i]) && belongs(lines[i])) {
            val line = lines[i].trim()
            parts += when (type) {
                ElementType.CENTERED -> line.drop(1).dropLast(1).trim()
                else -> line.drop(1).trim()
            }
            i++
        }
        add(type, parts.joinToString("\n"))
    }

    // Action keeps leading spaces (tabs become four spaces) and every line break.
    // It runs until a blank line or a line that forces another element.
    private fun readAction() {
        val parts = mutableListOf<String>()
        while (i < lines.size && !isSeparator(lines[i])) {
            val raw = lines[i]
            val trimmed = raw.trim()
            if (parts.isNotEmpty() && startsOtherElement(trimmed)) break
            val line = if (trimmed.startsWith("!")) raw.replaceFirst("!", "") else raw
            parts += if (line.isBlank()) "" else line.replace("\t", "    ").trimEnd()
            i++
        }
        add(ElementType.ACTION, parts.joinToString("\n"))
    }

    private fun startsOtherElement(line: String) =
        line.startsWith(">") || line.startsWith("~") || line.startsWith("=") ||
            line.startsWith("#") || line.startsWith("@") || line.startsWith(FountainParser.NOTE_MARKER)

    private fun readCharacter() {
        val cue = lines[i].trim().removePrefix("@")
        val dual = cue.endsWith("^")
        add(ElementType.CHARACTER, cue.removeSuffix("^").trim(), dual = dual)
        i++

        val dialogue = mutableListOf<String>()
        fun flushDialogue() {
            if (dialogue.isNotEmpty()) add(ElementType.DIALOGUE, dialogue.joinToString("\n"))
            dialogue.clear()
        }
        while (i < lines.size && !isSeparator(lines[i])) {
            val line = lines[i].trim()
            if (line.startsWith("(") && line.endsWith(")")) {
                flushDialogue()
                add(ElementType.PARENTHETICAL, line)
            } else {
                dialogue += line
            }
            i++
        }
        flushDialogue()
    }

    private fun add(
        type: ElementType,
        text: String,
        dual: Boolean = false,
        sceneNumber: String? = null,
        extras: Map<String, String> = emptyMap(),
    ) {
        val styled = Emphasis.parse(text)
        blocks += Block(
            id = BlockId(blocks.size.toLong()),
            type = type,
            text = styled.text,
            spans = styled.spans,
            dual = dual,
            sceneNumber = sceneNumber,
            extras = extras,
        )
    }

    private companion object {
        // Same key as the layout's PageTemplate.SPACE_BEFORE_EXTRA.
        const val SPACE_BEFORE = "spaceBefore"
        val PAGE_BREAK = Regex("""^={3,}$""")
        val SCENE_HEADING = Regex("""^(int\.?/ext|i/e|int|ext|est)[. ]""", RegexOption.IGNORE_CASE)
        val SCENE_NUMBER = Regex("""\s*#([A-Za-z0-9.\-]+)#\s*$""")
    }
}
