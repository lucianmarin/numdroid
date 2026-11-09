package com.lucianmarin.num.math

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

enum class SyntaxTokenType {
    PLAIN,
    COMMENT,
    STRING,
    NUMBER,
    OPERATOR,
    FUNCTION,
    VARIABLE,
    CONSTANT,
    KEYWORD,
    UNIT,
    PUNCTUATION
}

data class SyntaxToken(
    val type: SyntaxTokenType,
    val text: String
)

data class SyntaxPalette(
    val plain: Color,
    val comment: Color,
    val string: Color,
    val number: Color,
    val operator: Color,
    val function: Color,
    val variable: Color,
    val constant: Color,
    val keyword: Color,
    val unit: Color,
    val punctuation: Color
)

val LightSyntaxPalette = SyntaxPalette(
    plain = Color(0xFF1A1B1E),
    comment = Color(0xFF9EA2AB),
    string = Color(0xFFB4620A),
    number = Color(0xFF1A1B1E),
    operator = Color(0xFFF04E23),
    function = Color(0xFF4364F7),
    variable = Color(0xFF0D7A5F),
    constant = Color(0xFF8244CC),
    keyword = Color(0xFF687438),
    unit = Color(0xFF687438),
    punctuation = Color(0xFF8C8F96)
)

val DarkSyntaxPalette = SyntaxPalette(
    plain = Color(0xFFF0F1F5),
    comment = Color(0xFF5D6373),
    string = Color(0xFFF0A44C),
    number = Color(0xFFF0F1F5),
    operator = Color(0xFFF2542D),
    function = Color(0xFF5271FF),
    variable = Color(0xFF3DD6AD),
    constant = Color(0xFFB884FF),
    keyword = Color(0xFF879648),
    unit = Color(0xFF879648),
    punctuation = Color(0xFF666B7A)
)

object SyntaxHighlighter {
    private val KEYWORDS = setOf("in", "to", "mod", "and", "or", "xor", "not")
    private val CONSTANTS = setOf(
        "pi", "PI", "e", "E", "tau", "phi", "i",
        "Infinity", "NaN", "true", "false", "ans", "null",
        "speedOfLight", "gravitationConstant", "planckConstant", "reducedPlanckConstant",
        "avogadro", "boltzmann", "faraday", "gasConstant", "gravity", "elementaryCharge"
    )

    private val TOKEN_REGEX = Regex(
        "(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')|(\\b(?:0[xX][0-9a-fA-F]+|0[bB][01]+|0[oO][0-7]+|\\d+(?:\\.\\d*)?(?:[eE][+-]?\\d+)?|\\.\\d+(?:[eE][+-]?\\d+)?)\\b)|([a-zA-Z_\\$\u00B5][a-zA-Z0-9_\\$]*)|(==|!=|<=|>=|>>>|<<|>>|\\.\\*|\\.\\^|\\.\\/|\\^\\||&&|\\|\\||[+\\-*/^%=<>!&|~?:×÷−])|([()\\[\\]{}.,;'])|(\\s+)"
    )

    fun tokenize(rawLine: String, scopeNames: Set<String> = emptySet()): List<SyntaxToken> {
        if (rawLine.isEmpty()) return emptyList()
        val trimmedStart = rawLine.trimStart()
        if (trimmedStart.startsWith("#") || trimmedStart.startsWith("//")) {
            return listOf(SyntaxToken(SyntaxTokenType.COMMENT, rawLine))
        }

        // Find trailing # or // comment outside quotes
        var commentIndex = -1
        var inQuote: Char? = null
        var i = 0
        while (i < rawLine.length) {
            val ch = rawLine[i]
            if (inQuote != null) {
                if (ch == '\\') i++
                else if (ch == inQuote) inQuote = null
            } else if (ch == '"' || ch == '\'') {
                inQuote = ch
            } else if (ch == '#') {
                commentIndex = i
                break
            } else if (ch == '/' && i + 1 < rawLine.length && rawLine[i + 1] == '/') {
                commentIndex = i
                break
            }
            i++
        }

        val codePart = if (commentIndex >= 0) rawLine.substring(0, commentIndex) else rawLine
        val commentPart = if (commentIndex >= 0) rawLine.substring(commentIndex) else ""

        val tokens = mutableListOf<SyntaxToken>()
        var lastIndex = 0

        for (match in TOKEN_REGEX.findAll(codePart)) {
            if (match.range.first > lastIndex) {
                tokens.add(
                    SyntaxToken(
                        SyntaxTokenType.PLAIN,
                        codePart.substring(lastIndex, match.range.first)
                    )
                )
            }
            val full = match.value
            val strTok = match.groups[1]?.value
            val numTok = match.groups[2]?.value
            val idTok = match.groups[3]?.value
            val opTok = match.groups[4]?.value
            val puncTok = match.groups[5]?.value
            val wsTok = match.groups[6]?.value

            val nextPos = match.range.last + 1
            val afterSlice = if (nextPos < codePart.length) codePart.substring(nextPos) else ""

            val type = when {
                wsTok != null -> SyntaxTokenType.PLAIN
                strTok != null -> SyntaxTokenType.STRING
                numTok != null -> SyntaxTokenType.NUMBER
                idTok != null -> {
                    when {
                        afterSlice.trimStart().startsWith("(") -> SyntaxTokenType.FUNCTION
                        Regex("""^\s*=(?!=)""").containsMatchIn(afterSlice) -> SyntaxTokenType.VARIABLE
                        KEYWORDS.contains(full) -> SyntaxTokenType.KEYWORD
                        CONSTANTS.contains(full) || MathFunctions.CONSTANTS.containsKey(full) -> SyntaxTokenType.CONSTANT
                        scopeNames.contains(full) -> SyntaxTokenType.VARIABLE
                        MathFunctions.BUILTIN_FUNCTIONS.contains(full) -> SyntaxTokenType.FUNCTION
                        else -> SyntaxTokenType.UNIT
                    }
                }
                opTok != null -> SyntaxTokenType.OPERATOR
                puncTok != null -> SyntaxTokenType.PUNCTUATION
                else -> SyntaxTokenType.PLAIN
            }
            tokens.add(SyntaxToken(type, full))
            lastIndex = nextPos
        }

        if (lastIndex < codePart.length) {
            tokens.add(SyntaxToken(SyntaxTokenType.PLAIN, codePart.substring(lastIndex)))
        }
        if (commentPart.isNotEmpty()) {
            tokens.add(SyntaxToken(SyntaxTokenType.COMMENT, commentPart))
        }

        return tokens
    }

    fun highlight(
        text: String,
        palette: SyntaxPalette,
        scopeNames: Set<String> = emptySet()
    ): AnnotatedString {
        val tokens = tokenize(text, scopeNames)
        return buildAnnotatedString {
            for (tok in tokens) {
                val start = length
                append(tok.text)
                val end = length
                val style = when (tok.type) {
                    SyntaxTokenType.PLAIN -> SpanStyle(color = palette.plain)
                    SyntaxTokenType.COMMENT -> SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
                    SyntaxTokenType.STRING -> SpanStyle(color = palette.string)
                    SyntaxTokenType.NUMBER -> SpanStyle(color = palette.number)
                    SyntaxTokenType.OPERATOR -> SpanStyle(color = palette.operator, fontWeight = FontWeight.SemiBold)
                    SyntaxTokenType.FUNCTION -> SpanStyle(color = palette.function, fontWeight = FontWeight.Medium)
                    SyntaxTokenType.VARIABLE -> SpanStyle(color = palette.variable, fontWeight = FontWeight.Medium)
                    SyntaxTokenType.CONSTANT -> SpanStyle(color = palette.constant, fontWeight = FontWeight.Medium)
                    SyntaxTokenType.KEYWORD -> SpanStyle(color = palette.keyword, fontWeight = FontWeight.Medium)
                    SyntaxTokenType.UNIT -> SpanStyle(color = palette.unit)
                    SyntaxTokenType.PUNCTUATION -> SpanStyle(color = palette.punctuation)
                }
                addStyle(style, start, end)
            }
        }
    }

    fun visualTransformation(
        palette: SyntaxPalette,
        scopeNames: Set<String> = emptySet()
    ): VisualTransformation = VisualTransformation { original ->
        TransformedText(
            text = highlight(original.text, palette, scopeNames),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
