package com.lucianmarin.num.math

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
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

@Immutable
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
    plain = Color(0xFF111827),
    comment = Color(0xFF9CA3AF),
    string = Color(0xFFB45309),
    number = Color(0xFF111827),
    operator = Color(0xFFF85ACB),
    function = Color(0xFF6441B3),
    variable = Color(0xFF059669),
    constant = Color(0xFFF85ACB),
    keyword = Color(0xFF432B7A),
    unit = Color(0xFF6441B3),
    punctuation = Color(0xFF6B7280)
)

val DarkSyntaxPalette = SyntaxPalette(
    plain = Color(0xFFF3F4F6),
    comment = Color(0xFF6B7280),
    string = Color(0xFFFBBF24),
    number = Color(0xFFF3F4F6),
    operator = Color(0xFF432B7A),
    function = Color(0xFF6441B3),
    variable = Color(0xFF34D399),
    constant = Color(0xFF432B7A),
    keyword = Color(0xFFF85ACB),
    unit = Color(0xFF6441B3),
    punctuation = Color(0xFF9CA3AF)
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

    private data class HighlightCacheKey(
        val text: String,
        val palette: SyntaxPalette,
        val scopeNames: Set<String>
    )

    private const val MAX_HIGHLIGHT_CACHE_SIZE = 256
    private val highlightCache = object : LinkedHashMap<HighlightCacheKey, AnnotatedString>(
        MAX_HIGHLIGHT_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<HighlightCacheKey, AnnotatedString>?
        ): Boolean = size > MAX_HIGHLIGHT_CACHE_SIZE
    }

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

            val type = when {
                wsTok != null -> SyntaxTokenType.PLAIN
                strTok != null -> SyntaxTokenType.STRING
                numTok != null -> SyntaxTokenType.NUMBER
                idTok != null -> {
                    var lookahead = nextPos
                    while (lookahead < codePart.length && codePart[lookahead].isWhitespace()) {
                        lookahead++
                    }
                    val nextChar = if (lookahead < codePart.length) codePart[lookahead] else '\u0000'
                    when {
                        nextChar == '(' -> SyntaxTokenType.FUNCTION
                        nextChar == '=' && (lookahead + 1 >= codePart.length || codePart[lookahead + 1] != '=') ->
                            SyntaxTokenType.VARIABLE
                        KEYWORDS.contains(full) -> SyntaxTokenType.KEYWORD
                        CONSTANTS.contains(full) || MathFunctions.CONSTANTS.containsKey(full) -> SyntaxTokenType.CONSTANT
                        MathEngine.LINE_VAR_REGEX.matches(full) -> SyntaxTokenType.VARIABLE
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
        if (text.isEmpty()) return AnnotatedString("")
        val key = HighlightCacheKey(text, palette, scopeNames)
        synchronized(highlightCache) {
            highlightCache[key]?.let { return it }
        }

        val tokens = tokenize(text, scopeNames)
        val built = buildAnnotatedString {
            for (tok in tokens) {
                val start = length
                append(tok.text)
                val end = length
                val style = when (tok.type) {
                    SyntaxTokenType.PLAIN -> null
                    SyntaxTokenType.COMMENT -> SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
                    SyntaxTokenType.STRING -> SpanStyle(color = palette.string)
                    SyntaxTokenType.NUMBER -> SpanStyle(color = palette.number)
                    SyntaxTokenType.OPERATOR -> SpanStyle(color = palette.operator)
                    SyntaxTokenType.FUNCTION -> SpanStyle(color = palette.function)
                    SyntaxTokenType.VARIABLE -> SpanStyle(color = palette.variable)
                    SyntaxTokenType.CONSTANT -> SpanStyle(color = palette.constant)
                    SyntaxTokenType.KEYWORD -> SpanStyle(color = palette.keyword)
                    SyntaxTokenType.UNIT -> SpanStyle(color = palette.unit)
                    SyntaxTokenType.PUNCTUATION -> SpanStyle(color = palette.punctuation)
                }
                if (style != null) {
                    addStyle(style, start, end)
                }
            }
        }

        synchronized(highlightCache) {
            highlightCache[key] = built
        }
        return built
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
