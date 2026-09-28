package com.boliby.screenwriter.core.fountain

import com.boliby.screenwriter.core.model.Span
import com.boliby.screenwriter.core.model.Style
import com.boliby.screenwriter.core.model.StyledText

/**
 * Fountain emphasis (§4.1): `*italic*`, `**bold**`, `***bold italic***`, and
 * `_underline_`, with backslash escapes. As in Markdown, an opening marker must
 * be followed by a non-space and a closing one preceded by a non-space.
 * Emphasis never crosses a line break.
 */
object Emphasis {
    fun parse(text: String): StyledText {
        if (text.none { it == '*' || it == '_' || it == '\\' }) return StyledText(text)
        val out = StringBuilder()
        val spans = mutableListOf<Span>()
        text.split('\n').forEachIndexed { i, line ->
            if (i > 0) out.append('\n')
            val parsed = parseLine(line)
            val offset = out.length
            out.append(parsed.text)
            parsed.spans.mapTo(spans) { it.copy(start = it.start + offset, end = it.end + offset) }
        }
        return StyledText(out.toString(), spans)
    }

    private class Token(
        val literal: String,
        val marker: Char? = null,
        val canOpen: Boolean = false,
        val canClose: Boolean = false,
    ) {
        var matched = false
        val usable: Boolean
            get() = when (marker) {
                '*' -> literal.length <= 3
                '_' -> literal.length == 1
                else -> false
            }
    }

    private fun parseLine(line: String): StyledText {
        val tokens = tokenize(line)
        val pairs = matchMarkers(tokens)

        val out = StringBuilder()
        val starts = IntArray(tokens.size)
        tokens.forEachIndexed { i, token ->
            starts[i] = out.length
            if (!token.matched) out.append(token.literal)
        }
        val spans = pairs.flatMap { (open, close) ->
            val start = starts[open]
            val end = starts[close]
            stylesFor(tokens[open]).map { Span(start, end, it) }
        }.sortedWith(compareBy({ it.start }, { it.style }))
        return StyledText(out.toString(), spans)
    }

    private fun tokenize(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val literal = StringBuilder()
        fun flush() {
            if (literal.isNotEmpty()) tokens += Token(literal.toString())
            literal.clear()
        }
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length && line[i + 1] in "*_\\") {
                literal.append(line[i + 1])
                i += 2
            } else if (c == '*' || c == '_') {
                var end = i
                while (end < line.length && line[end] == c) end++
                val before = line.getOrNull(i - 1)
                val after = line.getOrNull(end)
                flush()
                tokens += Token(
                    literal = line.substring(i, end),
                    marker = c,
                    canOpen = after != null && !after.isWhitespace(),
                    canClose = before != null && !before.isWhitespace(),
                )
                i = end
            } else {
                literal.append(c)
                i++
            }
        }
        flush()
        return tokens
    }

    // Pairs each closing marker with the nearest open marker of the same kind and
    // length. Openers skipped over stay literal text.
    private fun matchMarkers(tokens: List<Token>): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        val open = mutableListOf<Int>()
        tokens.forEachIndexed { i, token ->
            if (!token.usable) return@forEachIndexed
            if (token.canClose) {
                val k = open.indexOfLast { tokens[it].literal == token.literal }
                if (k >= 0) {
                    val opener = open[k]
                    tokens[opener].matched = true
                    token.matched = true
                    pairs += opener to i
                    while (open.size > k) open.removeAt(open.lastIndex)
                    return@forEachIndexed
                }
            }
            if (token.canOpen) open += i
        }
        return pairs
    }

    private fun stylesFor(token: Token): List<Style> = when {
        token.marker == '_' -> listOf(Style.UNDERLINE)
        token.literal.length == 1 -> listOf(Style.ITALIC)
        token.literal.length == 2 -> listOf(Style.BOLD)
        else -> listOf(Style.BOLD, Style.ITALIC)
    }
}
