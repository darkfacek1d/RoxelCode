package com.roxelcode.app.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object SyntaxHighlighter {

    val DEFAULT_COLOR = Color(0xFFE5E5EA)

    fun highlight(line: String, lang: LanguageDef?, enabled: Boolean): AnnotatedString {
        if (!enabled || lang == null) {
            return AnnotatedString(line)
        }

        val commentIdx = findCommentStart(line, lang)
        if (commentIdx == 0) {
            return AnnotatedString(
                line,
                SpanStyle(color = lang.commentColor, fontStyle = FontStyle.Italic)
            )
        }

        return buildAnnotatedString {
            val codeEnd = if (commentIdx > 0) commentIdx else line.length
            val codePart = line.substring(0, codeEnd)
            appendHighlighted(codePart, lang)
            if (commentIdx > 0) {
                val start = length
                append(line.substring(commentIdx))
                addStyle(
                    SpanStyle(color = lang.commentColor, fontStyle = FontStyle.Italic),
                    start, length
                )
            }
        }
    }

    private fun androidx.compose.ui.text.AnnotatedString.Builder.appendHighlighted(
        text: String, lang: LanguageDef
    ) {
        var i = 0
        while (i < text.length) {
            val c = text[i]

            if (c == '"' || c == '\'') {
                val quote = c
                var j = i + 1
                while (j < text.length && text[j] != quote) {
                    if (text[j] == '\\' && j + 1 < text.length) j++
                    j++
                }
                if (j < text.length) j++
                val start = length
                append(text.substring(i, j))
                addStyle(SpanStyle(color = lang.stringColor), start, length)
                i = j
                continue
            }

            if (c.isDigit()) {
                var j = i
                while (j < text.length && (text[j].isDigit() || text[j] == '.' || text[j] == '_')) j++
                val start = length
                append(text.substring(i, j))
                addStyle(SpanStyle(color = lang.numberColor), start, length)
                i = j
                continue
            }

            if (c.isLetter() || c == '_' || c == '@' || c == '$') {
                var j = i
                while (j < text.length && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '@' || text[j] == '$')) j++
                val word = text.substring(i, j)
                val start = length
                append(word)

                when {
                    lang.keywords.contains(word) -> {
                        addStyle(
                            SpanStyle(color = lang.keywordColor, fontWeight = FontWeight.Medium),
                            start, length
                        )
                    }
                    j < text.length && text[j] == '(' -> {
                        addStyle(SpanStyle(color = lang.functionColor), start, length)
                    }
                    word.firstOrNull()?.isUpperCase() == true -> {
                        addStyle(SpanStyle(color = lang.typeColor), start, length)
                    }
                }
                i = j
                continue
            }

            append(c)
            i++
        }
    }

    private fun findCommentStart(line: String, lang: LanguageDef): Int {
        if (lang.lineComment.isBlank()) return -1
        var i = 0
        var inString = false
        var stringChar = ' '
        while (i < line.length) {
            val c = line[i]
            if (!inString && (c == '"' || c == '\'')) {
                inString = true
                stringChar = c
                i++
                continue
            }
            if (inString && c == stringChar && (i == 0 || line[i - 1] != '\\')) {
                inString = false
                i++
                continue
            }
            if (!inString && line.startsWith(lang.lineComment, i)) {
                return i
            }
            i++
        }
        return -1
    }
}
