package com.boliby.screenwriter.core.layout

/**
 * Word wrap on a fixed character grid (Courier: every character is 0.1" wide).
 * Lines break at spaces or after a hyphen. Spaces at a break are dropped, and a
 * hyphen at the end of a line may hang one column past [maxChars], as it does in
 * Final Draft. A word longer than a whole line is cut at [maxChars].
 * A `\n` in [text] always starts a new line. The very first line may have its
 * own width, for a first-line indent.
 */
object LineWrapper {
    fun wrap(text: String, maxChars: Int, firstLineMaxChars: Int = maxChars): List<String> {
        val lines = text.split('\n')
        return wrapLine(lines.first(), maxChars, firstLineMaxChars) + lines.drop(1).flatMap { wrapLine(it, maxChars, maxChars) }
    }

    private fun fits(line: CharSequence, maxChars: Int) =
        line.length <= maxChars || (line.length == maxChars + 1 && line.last() == '-')

    private fun wrapLine(text: String, maxChars: Int, firstLineMaxChars: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        val line = StringBuilder()
        fun width() = if (lines.isEmpty()) firstLineMaxChars else maxChars
        for ((spaces, piece) in pieces(text)) {
            val candidate = if (line.isEmpty() && lines.isNotEmpty()) piece else line.toString() + " ".repeat(spaces) + piece
            if (fits(candidate, width())) {
                line.setLength(0)
                line.append(candidate)
                continue
            }
            if (line.isNotEmpty()) {
                lines += line.toString()
                line.setLength(0)
            }
            var rest = piece
            while (!fits(rest, width())) {
                val cut = width()
                lines += rest.take(cut)
                rest = rest.drop(cut)
            }
            line.append(rest)
        }
        lines += line.toString()
        return lines
    }

    // Splits text into pieces that can end a line: runs of non-space characters,
    // further split after each hyphen that follows a letter or digit
    // ("hundred-dollar" is "hundred-" + "dollar"). Each piece carries the number
    // of spaces before it.
    private fun pieces(text: String): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        var i = 0
        while (i < text.length) {
            var spaces = 0
            while (i < text.length && text[i] == ' ') {
                spaces++
                i++
            }
            if (i >= text.length) break
            val start = i
            while (i < text.length && text[i] != ' ') {
                val c = text[i++]
                if (c == '-' && i - 2 >= start && text[i - 2].isLetterOrDigit() && i < text.length && text[i].isLetterOrDigit()) break
            }
            result += spaces to text.substring(start, i)
        }
        return result
    }
}
