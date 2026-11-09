package com.lucianmarin.num.math

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

sealed class AstNode {
    data class NumberLiteral(val value: Double) : AstNode()
    data class StringLiteral(val value: String) : AstNode()
    data class Identifier(val name: String) : AstNode()
    data class UnaryOp(val op: String, val operand: AstNode) : AstNode()
    data class PostfixOp(val op: String, val operand: AstNode) : AstNode()
    data class BinaryOp(val op: String, val left: AstNode, val right: AstNode) : AstNode()
    data class TernaryOp(val condition: AstNode, val trueExpr: AstNode, val falseExpr: AstNode) : AstNode()
    data class RangeNode(val start: AstNode, val end: AstNode, val step: AstNode? = null) : AstNode()
    data class MatrixLiteral(val rows: List<List<AstNode>>) : AstNode()
    data class ObjectLiteral(val entries: List<Pair<String, AstNode>>) : AstNode()
    data class FunctionCall(val name: String, val args: List<AstNode>) : AstNode()
    data class IndexAccess(val target: AstNode, val indices: List<AstNode>) : AstNode()
    data class PropertyAccess(val target: AstNode, val property: String) : AstNode()
    data class VarAssign(val name: String, val expr: AstNode) : AstNode()
    data class FuncAssign(val name: String, val params: List<String>, val bodyText: String, val body: AstNode) : AstNode()
    data class UnitConvert(val expr: AstNode, val targetUnitExpr: AstNode) : AstNode()
    data class BlockNode(val statements: List<AstNode>) : AstNode()
}

private enum class TokKind {
    NUMBER,
    STRING,
    IDENT,
    OP,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LBRACE,
    RBRACE,
    COMMA,
    SEMICOLON,
    COLON,
    QUESTION,
    DOT,
    EOF
}

private data class LexToken(
    val kind: TokKind,
    val text: String,
    val numVal: Double = 0.0,
    val pos: Int = 0
)

class MathParser(private val input: String) {
    private val tokens = tokenize(input)
    private var idx = 0
    private var ternaryDepth = 0

    private fun peek(offset: Int = 0): LexToken =
        tokens.getOrElse(idx + offset) { LexToken(TokKind.EOF, "", pos = input.length) }

    private fun next(): LexToken {
        val t = peek()
        if (idx < tokens.size) idx++
        return t
    }

    private fun match(kind: TokKind, text: String? = null): Boolean {
        val t = peek()
        if (t.kind != kind) return false
        if (text != null && t.text != text) return false
        idx++
        return true
    }

    private fun expect(kind: TokKind, msg: String): LexToken {
        val t = peek()
        if (t.kind != kind) throw MathEvalException("Syntax error: expected $msg, got '${t.text}'")
        idx++
        return t
    }

    fun parseProgram(): AstNode {
        val stmts = mutableListOf<AstNode>()
        while (peek().kind != TokKind.EOF) {
            while (match(TokKind.SEMICOLON)) { /* skip leading semicolons */ }
            if (peek().kind == TokKind.EOF) break
            stmts.add(parseStatement())
            while (match(TokKind.SEMICOLON)) { /* skip trailing semicolons */ }
        }
        return when (stmts.size) {
            0 -> AstNode.NumberLiteral(Double.NaN)
            1 -> stmts[0]
            else -> AstNode.BlockNode(stmts)
        }
    }

    private fun parseStatement(): AstNode {
        // Check variable assignment: IDENT = expr (not ==)
        if (peek(0).kind == TokKind.IDENT && peek(1).kind == TokKind.OP && peek(1).text == "=") {
            val varName = next().text
            next() // consume '='
            val expr = parseStatement()
            return AstNode.VarAssign(varName, expr)
        }

        // Check function definition: IDENT ( IDENT, ... ) = expr
        if (peek(0).kind == TokKind.IDENT && peek(1).kind == TokKind.LPAREN) {
            val savedIdx = idx
            val fnName = next().text
            next() // '('
            val params = mutableListOf<String>()
            var validFnDef = true
            if (peek().kind != TokKind.RPAREN) {
                while (true) {
                    if (peek().kind == TokKind.IDENT) {
                        params.add(next().text)
                    } else {
                        validFnDef = false
                        break
                    }
                    if (!match(TokKind.COMMA)) break
                }
            }
            if (validFnDef && match(TokKind.RPAREN) && peek().kind == TokKind.OP && peek().text == "=") {
                val eqTok = next() // '='
                val bodyStart = eqTok.pos + 1
                val bodyNode = parseStatement()
                val bodyEnd = peek().pos.coerceAtMost(input.length)
                val bodyText = input.substring(bodyStart.coerceIn(0, input.length), bodyEnd).trim()
                return AstNode.FuncAssign(fnName, params, bodyText, bodyNode)
            } else {
                idx = savedIdx
            }
        }

        return parseTernary()
    }

    private fun parseTernary(): AstNode {
        val cond = parseUnitConversion()
        if (match(TokKind.QUESTION)) {
            ternaryDepth++
            val trueBranch = try {
                parseStatement()
            } finally {
                ternaryDepth--
            }
            expect(TokKind.COLON, "':' in conditional expression")
            val falseBranch = parseStatement()
            return AstNode.TernaryOp(cond, trueBranch, falseBranch)
        }
        return cond
    }

    private fun parseUnitConversion(): AstNode {
        var left = parseLogicalOr()
        while (peek().kind == TokKind.IDENT && (peek().text == "in" || peek().text == "to")) {
            next() // consume 'in' or 'to'
            val targetUnit = parseAdditive()
            left = AstNode.UnitConvert(left, targetUnit)
        }
        return left
    }

    private fun parseLogicalOr(): AstNode {
        var left = parseLogicalXor()
        while ((peek().kind == TokKind.IDENT && peek().text == "or") || (peek().kind == TokKind.OP && peek().text == "||")) {
            next()
            val right = parseLogicalXor()
            left = AstNode.BinaryOp("or", left, right)
        }
        return left
    }

    private fun parseLogicalXor(): AstNode {
        var left = parseLogicalAnd()
        while (peek().kind == TokKind.IDENT && peek().text == "xor") {
            next()
            val right = parseLogicalAnd()
            left = AstNode.BinaryOp("xor", left, right)
        }
        return left
    }

    private fun parseLogicalAnd(): AstNode {
        var left = parseBitwiseOr()
        while ((peek().kind == TokKind.IDENT && peek().text == "and") || (peek().kind == TokKind.OP && peek().text == "&&")) {
            next()
            val right = parseBitwiseOr()
            left = AstNode.BinaryOp("and", left, right)
        }
        return left
    }

    private fun parseBitwiseOr(): AstNode {
        var left = parseBitwiseXor()
        while (peek().kind == TokKind.OP && peek().text == "|") {
            next()
            val right = parseBitwiseXor()
            left = AstNode.BinaryOp("|", left, right)
        }
        return left
    }

    private fun parseBitwiseXor(): AstNode {
        var left = parseBitwiseAnd()
        while (peek().kind == TokKind.OP && peek().text == "^|") {
            next()
            val right = parseBitwiseAnd()
            left = AstNode.BinaryOp("^|", left, right)
        }
        return left
    }

    private fun parseBitwiseAnd(): AstNode {
        var left = parseEquality()
        while (peek().kind == TokKind.OP && peek().text == "&") {
            next()
            val right = parseEquality()
            left = AstNode.BinaryOp("&", left, right)
        }
        return left
    }

    private fun parseEquality(): AstNode {
        var left = parseRelational()
        while (peek().kind == TokKind.OP && (peek().text == "==" || peek().text == "!=")) {
            val op = next().text
            val right = parseRelational()
            left = AstNode.BinaryOp(op, left, right)
        }
        return left
    }

    private fun parseRelational(): AstNode {
        var left = parseShift()
        while (peek().kind == TokKind.OP && (peek().text == "<" || peek().text == ">" || peek().text == "<=" || peek().text == ">=")) {
            val op = next().text
            val right = parseShift()
            left = AstNode.BinaryOp(op, left, right)
        }
        return left
    }

    private fun parseShift(): AstNode {
        var left = parseRange()
        while (peek().kind == TokKind.OP && (peek().text == "<<" || peek().text == ">>" || peek().text == ">>>")) {
            val op = next().text
            val right = parseRange()
            left = AstNode.BinaryOp(op, left, right)
        }
        return left
    }

    private fun parseRange(): AstNode {
        val first = parseAdditive()
        if (ternaryDepth == 0 && peek().kind == TokKind.COLON) {
            next() // consume first ':'
            val second = parseAdditive()
            if (match(TokKind.COLON)) {
                val third = parseAdditive()
                return AstNode.RangeNode(start = first, step = second, end = third)
            }
            return AstNode.RangeNode(start = first, end = second, step = null)
        }
        return first
    }

    private fun parseAdditive(): AstNode {
        var left = parseMultiplicative()
        while (peek().kind == TokKind.OP && (peek().text == "+" || peek().text == "-")) {
            val op = next().text
            val right = parseMultiplicative()
            left = AstNode.BinaryOp(op, left, right)
        }
        return left
    }

    private fun parseMultiplicative(): AstNode {
        var left = parseUnary()
        while (true) {
            val t = peek()
            if (t.kind == TokKind.OP && (t.text == "*" || t.text == "/" || t.text == "%" || t.text == ".*" || t.text == "./")) {
                val op = next().text
                val right = parseUnary()
                left = AstNode.BinaryOp(op, left, right)
            } else if (t.kind == TokKind.IDENT && t.text == "mod") {
                next()
                val right = parseUnary()
                left = AstNode.BinaryOp("%", left, right)
            } else if (canImplicitMultiply(t)) {
                // Implicit multiplication: e.g. 2 pi, 100 km, 3i, 2(3+4), (2)(3)
                val right = parsePower()
                left = AstNode.BinaryOp("*", left, right)
            } else {
                break
            }
        }
        return left
    }

    private fun canImplicitMultiply(nextTok: LexToken): Boolean {
        if (nextTok.kind == TokKind.IDENT) {
            val w = nextTok.text
            if (w == "in" || w == "to" || w == "and" || w == "or" || w == "xor" || w == "mod" || w == "not") {
                return false
            }
            return true
        }
        if (nextTok.kind == TokKind.LPAREN) {
            return true
        }
        return false
    }

    private fun parseUnary(): AstNode {
        val t = peek()
        if (t.kind == TokKind.OP && (t.text == "+" || t.text == "-" || t.text == "~" || t.text == "!")) {
            val op = next().text
            return AstNode.UnaryOp(op, parseUnary())
        }
        if (t.kind == TokKind.IDENT && t.text == "not") {
            next()
            if (match(TokKind.LPAREN)) {
                val inner = parseStatement()
                expect(TokKind.RPAREN, "')'")
                return AstNode.UnaryOp("not", inner)
            }
            return AstNode.UnaryOp("not", parseUnary())
        }
        return parsePower()
    }

    private fun parsePower(): AstNode {
        val base = parsePostfix()
        if (peek().kind == TokKind.OP && (peek().text == "^" || peek().text == ".^")) {
            val op = next().text
            val exp = parseUnary() // right-associative
            return AstNode.BinaryOp(op, base, exp)
        }
        return base
    }

    private fun parsePostfix(): AstNode {
        var expr = parsePrimary()
        while (true) {
            val t = peek()
            if (t.kind == TokKind.OP && (t.text == "!" || t.text == "'")) {
                val op = next().text
                expr = AstNode.PostfixOp(op, expr)
            } else if (t.kind == TokKind.OP && t.text == "%") {
                // Distinguish percentage postfix (e.g. `19%` or `100 + 19%`) from modulo `10 % 3`
                val nextAfter = peek(1)
                val isBinaryMod = nextAfter.kind == TokKind.NUMBER ||
                    nextAfter.kind == TokKind.IDENT ||
                    nextAfter.kind == TokKind.LPAREN ||
                    nextAfter.kind == TokKind.LBRACKET
                if (!isBinaryMod) {
                    next() // consume '%'
                    expr = AstNode.PostfixOp("%", expr)
                } else {
                    break
                }
            } else if (match(TokKind.LBRACKET)) {
                val savedTernary = ternaryDepth
                ternaryDepth = 0
                val indices = mutableListOf<AstNode>()
                try {
                    if (peek().kind != TokKind.RBRACKET) {
                        indices.add(parseStatement())
                        while (match(TokKind.COMMA)) {
                            indices.add(parseStatement())
                        }
                    }
                    expect(TokKind.RBRACKET, "']'")
                } finally {
                    ternaryDepth = savedTernary
                }
                expr = AstNode.IndexAccess(expr, indices)
            } else if (match(TokKind.DOT)) {
                val prop = expect(TokKind.IDENT, "property name").text
                expr = AstNode.PropertyAccess(expr, prop)
            } else {
                break
            }
        }
        return expr
    }

    private fun parsePrimary(): AstNode {
        val t = peek()
        return when (t.kind) {
            TokKind.NUMBER -> {
                next()
                AstNode.NumberLiteral(t.numVal)
            }
            TokKind.STRING -> {
                next()
                AstNode.StringLiteral(t.text)
            }
            TokKind.IDENT -> {
                val ident = next().text
                if (match(TokKind.LPAREN)) {
                    val args = mutableListOf<AstNode>()
                    if (peek().kind != TokKind.RPAREN) {
                        args.add(parseStatement())
                        while (match(TokKind.COMMA)) {
                            args.add(parseStatement())
                        }
                    }
                    expect(TokKind.RPAREN, "')'")
                    if (args.size == 1 && args[0] is AstNode.RangeNode && (ident == "subset" || ident == "index")) {
                        AstNode.FunctionCall(ident, args)
                    } else {
                        AstNode.FunctionCall(ident, args)
                    }
                } else {
                    AstNode.Identifier(ident)
                }
            }
            TokKind.LPAREN -> {
                next()
                val savedTernary = ternaryDepth
                ternaryDepth = 0
                val inner = try {
                    parseStatement()
                } finally {
                    ternaryDepth = savedTernary
                }
                expect(TokKind.RPAREN, "')'")
                inner
            }
            TokKind.LBRACKET -> {
                next()
                val rows = mutableListOf<List<AstNode>>()
                if (peek().kind != TokKind.RBRACKET) {
                    var currentRow = mutableListOf<AstNode>()
                    currentRow.add(parseStatement())
                    while (true) {
                        if (match(TokKind.COMMA)) {
                            currentRow.add(parseStatement())
                        } else if (match(TokKind.SEMICOLON)) {
                            rows.add(currentRow)
                            currentRow = mutableListOf()
                            if (peek().kind != TokKind.RBRACKET) {
                                currentRow.add(parseStatement())
                            }
                        } else {
                            break
                        }
                    }
                    if (currentRow.isNotEmpty()) rows.add(currentRow)
                }
                expect(TokKind.RBRACKET, "']'")
                // Flatten nested vector rows if user wrote [[1, 2], [3, 4]]
                if (rows.size == 1 && rows[0].isNotEmpty() && rows[0].all { it is AstNode.MatrixLiteral && it.rows.size == 1 }) {
                    AstNode.MatrixLiteral(rows[0].map { (it as AstNode.MatrixLiteral).rows[0] })
                } else {
                    AstNode.MatrixLiteral(rows)
                }
            }
            TokKind.LBRACE -> {
                next()
                val entries = mutableListOf<Pair<String, AstNode>>()
                if (peek().kind != TokKind.RBRACE) {
                    while (true) {
                        val keyTok = next()
                        val key = when (keyTok.kind) {
                            TokKind.IDENT, TokKind.STRING -> keyTok.text
                            else -> throw MathEvalException("Expected object key, got '${keyTok.text}'")
                        }
                        expect(TokKind.COLON, "':' after object key")
                        val valueNode = parseStatement()
                        entries.add(key to valueNode)
                        if (!match(TokKind.COMMA)) break
                    }
                }
                expect(TokKind.RBRACE, "'}'")
                AstNode.ObjectLiteral(entries)
            }
            else -> throw MathEvalException("Unexpected token '${t.text}'")
        }
    }

    companion object {
        private fun tokenize(src: String): List<LexToken> {
            val out = mutableListOf<LexToken>()
            var i = 0
            val n = src.length

            while (i < n) {
                val c = src[i]
                if (c.isWhitespace()) {
                    i++
                    continue
                }
                // Comments (# or //)
                if (c == '#' || (c == '/' && i + 1 < n && src[i + 1] == '/')) {
                    break
                }
                // Strings
                val prevKind = out.lastOrNull()?.kind
                val isTransposeAfterValue = c == '\'' && (
                    prevKind == TokKind.RBRACKET ||
                        prevKind == TokKind.RPAREN ||
                        prevKind == TokKind.IDENT ||
                        prevKind == TokKind.NUMBER
                    )
                if (!isTransposeAfterValue && (c == '"' || c == '\'')) {
                    val quote = c
                    val start = i
                    i++
                    val sb = StringBuilder()
                    while (i < n && src[i] != quote) {
                        if (src[i] == '\\' && i + 1 < n) {
                            i++
                            when (val esc = src[i]) {
                                'n' -> sb.append('\n')
                                't' -> sb.append('\t')
                                else -> sb.append(esc)
                            }
                        } else {
                            sb.append(src[i])
                        }
                        i++
                    }
                    if (i < n && src[i] == quote) i++
                    out.add(LexToken(TokKind.STRING, sb.toString(), pos = start))
                    continue
                }
                // Multi-char operators
                if (i + 2 < n) {
                    val three = src.substring(i, i + 3)
                    if (three == ">>>") {
                        out.add(LexToken(TokKind.OP, three, pos = i))
                        i += 3
                        continue
                    }
                }
                if (i + 1 < n) {
                    val two = src.substring(i, i + 2)
                    if (two in setOf("==", "!=", "<=", ">=", "<<", ">>", ".*", "./", ".^", "^|", "&&", "||")) {
                        out.add(LexToken(TokKind.OP, two, pos = i))
                        i += 2
                        continue
                    }
                }
                // Numbers (including 0x, 0b, 0o, scientific notation)
                if (c.isDigit() || (c == '.' && i + 1 < n && src[i + 1].isDigit())) {
                    val start = i
                    if (c == '0' && i + 1 < n && (src[i + 1] == 'x' || src[i + 1] == 'X')) {
                        i += 2
                        val hexStart = i
                        while (i < n && (src[i].isDigit() || src[i] in 'a'..'f' || src[i] in 'A'..'F')) i++
                        val v = src.substring(hexStart, i).toLong(16).toDouble()
                        out.add(LexToken(TokKind.NUMBER, src.substring(start, i), v, start))
                        continue
                    }
                    if (c == '0' && i + 1 < n && (src[i + 1] == 'b' || src[i + 1] == 'B')) {
                        i += 2
                        val binStart = i
                        while (i < n && (src[i] == '0' || src[i] == '1')) i++
                        val v = src.substring(binStart, i).toLong(2).toDouble()
                        out.add(LexToken(TokKind.NUMBER, src.substring(start, i), v, start))
                        continue
                    }
                    if (c == '0' && i + 1 < n && (src[i + 1] == 'o' || src[i + 1] == 'O')) {
                        i += 2
                        val octStart = i
                        while (i < n && src[i] in '0'..'7') i++
                        val v = src.substring(octStart, i).toLong(8).toDouble()
                        out.add(LexToken(TokKind.NUMBER, src.substring(start, i), v, start))
                        continue
                    }
                    while (i < n && src[i].isDigit()) i++
                    if (i < n && src[i] == '.' && (i + 1 >= n || (src[i + 1] != '*' && src[i + 1] != '/' && src[i + 1] != '^'))) {
                        i++
                        while (i < n && src[i].isDigit()) i++
                    }
                    if (i < n && (src[i] == 'e' || src[i] == 'E')) {
                        val expCheck = i + 1
                        if (expCheck < n && (src[expCheck].isDigit() || ((src[expCheck] == '+' || src[expCheck] == '-') && expCheck + 1 < n && src[expCheck + 1].isDigit()))) {
                            i++
                            if (src[i] == '+' || src[i] == '-') i++
                            while (i < n && src[i].isDigit()) i++
                        }
                    }
                    val raw = src.substring(start, i)
                    val v = raw.toDoubleOrNull() ?: Double.NaN
                    out.add(LexToken(TokKind.NUMBER, raw, v, start))
                    continue
                }
                // Identifiers
                if (c.isLetter() || c == '_' || c == '$' || c == 'µ') {
                    val start = i
                    i++
                    while (i < n && (src[i].isLetterOrDigit() || src[i] == '_' || src[i] == '$')) i++
                    out.add(LexToken(TokKind.IDENT, src.substring(start, i), pos = start))
                    continue
                }
                // Single-char punctuation & operators
                when (c) {
                    '(' -> out.add(LexToken(TokKind.LPAREN, "(", pos = i))
                    ')' -> out.add(LexToken(TokKind.RPAREN, ")", pos = i))
                    '[' -> out.add(LexToken(TokKind.LBRACKET, "[", pos = i))
                    ']' -> out.add(LexToken(TokKind.RBRACKET, "]", pos = i))
                    '{' -> out.add(LexToken(TokKind.LBRACE, "{", pos = i))
                    '}' -> out.add(LexToken(TokKind.RBRACE, "}", pos = i))
                    ',' -> out.add(LexToken(TokKind.COMMA, ",", pos = i))
                    ';' -> out.add(LexToken(TokKind.SEMICOLON, ";", pos = i))
                    ':' -> out.add(LexToken(TokKind.COLON, ":", pos = i))
                    '?' -> out.add(LexToken(TokKind.QUESTION, "?", pos = i))
                    '.' -> out.add(LexToken(TokKind.DOT, ".", pos = i))
                    '+', '-', '*', '/', '^', '%', '=', '<', '>', '!', '&', '|', '~', '\'' ->
                        out.add(LexToken(TokKind.OP, c.toString(), pos = i))
                    '×' -> out.add(LexToken(TokKind.OP, "*", pos = i))
                    '÷' -> out.add(LexToken(TokKind.OP, "/", pos = i))
                    '−' -> out.add(LexToken(TokKind.OP, "-", pos = i))
                    else -> throw MathEvalException("Unexpected character '$c'")
                }
                i++
            }
            out.add(LexToken(TokKind.EOF, "", pos = n))
            return out
        }
    }
}

class MathScope(
    parent: Map<String, MathValue> = emptyMap()
) {
    val variables = parent.toMutableMap()

    val functions: Map<String, MathValue.FunctionVal>
        get() = variables.mapNotNull { (k, v) ->
            if (v is MathValue.FunctionVal && !MathEngine.LINE_VAR_REGEX.matches(k)) k to v else null
        }.toMap()

    fun get(name: String): MathValue? = variables[name] ?: MathFunctions.CONSTANTS[name]

    fun set(name: String, value: MathValue) {
        variables[name] = value
    }

    fun getUserSymbols(): Map<String, MathValue> = variables.toMap()

    fun snapshot(): MathScope = MathScope(variables.toMap())
}

data class EvalLineResult(
    val value: MathValue?,
    val formatted: String?,
    val error: String?,
    val isComment: Boolean
)

object MathEngine {
    val LINE_VAR_REGEX = Regex("""^\$\d+$""")

    fun evaluateSingleLine(rawExpr: String, scope: MathScope = MathScope()): EvalLineResult {
        val stripped = MathEvaluator.stripComments(rawExpr).trim()
        if (stripped.isEmpty()) {
            return EvalLineResult(
                value = null,
                formatted = null,
                error = null,
                isComment = true
            )
        }
        return try {
            val result = MathEvaluator.evaluateLine(rawExpr, scope)
            if (result != null && result !is MathValue.NullVal) {
                scope.set("ans", result)
                EvalLineResult(
                    value = result,
                    formatted = result.format(),
                    error = null,
                    isComment = false
                )
            } else {
                EvalLineResult(
                    value = null,
                    formatted = null,
                    error = null,
                    isComment = true
                )
            }
        } catch (e: Exception) {
            EvalLineResult(
                value = null,
                formatted = null,
                error = e.message ?: "Evaluation error",
                isComment = false
            )
        }
    }

    fun evaluateLines(lines: List<String>): Pair<List<EvalLineResult>, MathScope> {
        val scope = MathScope()
        val results = lines.mapIndexed { idx, line ->
            val res = evaluateSingleLine(line, scope)
            if (res.value != null && res.value !is MathValue.NullVal) {
                scope.set("\$${idx + 1}", res.value)
            }
            res
        }
        return results to scope
    }

    fun extractReferencedLineNumbers(rawExpr: String): Set<Int> {
        val tokens = SyntaxHighlighter.tokenize(rawExpr)
        val result = mutableSetOf<Int>()
        for (tok in tokens) {
            if (tok.type != SyntaxTokenType.COMMENT && tok.type != SyntaxTokenType.STRING) {
                if (LINE_VAR_REGEX.matches(tok.text)) {
                    tok.text.substring(1).toIntOrNull()?.let { result.add(it) }
                }
            }
        }
        return result
    }
}

object MathEvaluator {
    fun evaluateLine(rawExpr: String, scope: MathScope): MathValue? {
        val trimmed = stripComments(rawExpr).trim()
        if (trimmed.isEmpty()) return null
        val ast = MathParser(trimmed).parseProgram()
        return evalNode(ast, scope)
    }

    fun stripComments(line: String): String {
        val trimmedStart = line.trimStart()
        if (trimmedStart.startsWith("#") || trimmedStart.startsWith("//")) return ""
        var quote: Char? = null
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (quote != null) {
                if (ch == '\\') i++
                else if (ch == quote) quote = null
            } else {
                if (ch == '"' || ch == '\'') quote = ch
                else if (ch == '#') return line.substring(0, i)
                else if (ch == '/' && i + 1 < line.length && line[i + 1] == '/') return line.substring(0, i)
            }
            i++
        }
        return line
    }

    fun evalNode(node: AstNode, scope: MathScope): MathValue {
        return when (node) {
            is AstNode.NumberLiteral -> MathValue.NumberVal(node.value)
            is AstNode.StringLiteral -> MathValue.StringVal(node.value)
            is AstNode.Identifier -> {
                scope.get(node.name)?.let { return it }
                UnitRegistry.resolve(node.name)?.let { return MathValue.UnitVal(1.0, it) }
                throw MathEvalException("Undefined symbol ${node.name}")
            }
            is AstNode.VarAssign -> {
                val v = evalNode(node.expr, scope)
                scope.set(node.name, v)
                v
            }
            is AstNode.FuncAssign -> {
                val fn = MathValue.FunctionVal(
                    name = node.name,
                    params = node.params,
                    exprText = node.bodyText,
                    body = node.body,
                    capturedScope = scope.getUserSymbols()
                )
                scope.set(node.name, fn)
                fn
            }
            is AstNode.BlockNode -> {
                var last: MathValue = MathValue.NullVal
                for (stmt in node.statements) {
                    last = evalNode(stmt, scope)
                }
                last
            }
            is AstNode.UnaryOp -> {
                val v = evalNode(node.operand, scope)
                when (node.op) {
                    "+" -> v
                    "-" -> when (v) {
                        is MathValue.NumberVal -> MathValue.NumberVal(-v.value)
                        is MathValue.ComplexVal -> MathValue.ComplexVal(-v.re, -v.im)
                        is MathValue.UnitVal -> MathValue.UnitVal(-v.value, v.unit)
                        is MathValue.MatrixVal -> MatrixOps.mapElementWise(v) { evalNode(AstNode.UnaryOp("-", AstNode.NumberLiteral(it.asDouble())), scope) }
                        else -> MathValue.NumberVal(-v.asDouble())
                    }
                    "not", "!" -> MathValue.BoolVal(!v.asBoolean())
                    "~" -> MathValue.NumberVal(v.asDouble().toLong().inv().toDouble())
                    else -> throw MathEvalException("Unknown unary operator ${node.op}")
                }
            }
            is AstNode.PostfixOp -> {
                val v = evalNode(node.operand, scope)
                when (node.op) {
                    "!" -> when (v) {
                        is MathValue.MatrixVal -> MatrixOps.mapElementWise(v) { MathValue.NumberVal(MathFunctions.factorial(it.asDouble())) }
                        else -> MathValue.NumberVal(MathFunctions.factorial(v.asDouble()))
                    }
                    "%" -> when (v) {
                        is MathValue.NumberVal -> MathValue.NumberVal(v.value / 100.0)
                        else -> MathValue.NumberVal(v.asDouble() / 100.0)
                    }
                    "'" -> when (v) {
                        is MathValue.MatrixVal -> MatrixOps.ctranspose(v)
                        is MathValue.ComplexVal -> ComplexOps.conj(v)
                        else -> v
                    }
                    else -> throw MathEvalException("Unknown postfix operator ${node.op}")
                }
            }
            is AstNode.BinaryOp -> {
                // Handle `100 + 19%` style expressions or standard binary ops
                val left = evalNode(node.left, scope)
                val right = evalNode(node.right, scope)
                evalBinaryOp(node.op, left, right)
            }
            is AstNode.TernaryOp -> {
                val cond = evalNode(node.condition, scope)
                if (cond.asBoolean()) evalNode(node.trueExpr, scope)
                else evalNode(node.falseExpr, scope)
            }
            is AstNode.UnitConvert -> {
                val leftVal = evalNode(node.expr, scope)
                val targetVal = evalNode(node.targetUnitExpr, scope)
                val srcUnitVal = when (leftVal) {
                    is MathValue.UnitVal -> leftVal
                    else -> throw MathEvalException("Left side of unit conversion must have a unit")
                }
                val dstUnit = when (targetVal) {
                    is MathValue.UnitVal -> targetVal.unit
                    else -> throw MathEvalException("Right side of 'in'/'to' must be a valid unit")
                }
                val converted = srcUnitVal.unit.convertTo(srcUnitVal.value, dstUnit)
                MathValue.UnitVal(converted / (if (targetVal is MathValue.UnitVal && targetVal.value != 0.0) targetVal.value else 1.0), dstUnit)
            }
            is AstNode.RangeNode -> {
                val start = evalNode(node.start, scope).asDouble()
                val end = evalNode(node.end, scope).asDouble()
                val step = node.step?.let { evalNode(it, scope).asDouble() } ?: if (end >= start) 1.0 else -1.0
                if (step == 0.0) throw MathEvalException("Range step cannot be zero")
                val items = mutableListOf<MathValue>()
                var cur = start
                var guard = 0
                if (step > 0) {
                    while (cur <= end + 1e-12 && guard < 10000) {
                        items.add(MathValue.NumberVal(if (abs(cur - round(cur)) < 1e-12) round(cur) else cur))
                        cur += step
                        guard++
                    }
                } else {
                    while (cur >= end - 1e-12 && guard < 10000) {
                        items.add(MathValue.NumberVal(if (abs(cur - round(cur)) < 1e-12) round(cur) else cur))
                        cur += step
                        guard++
                    }
                }
                MathValue.MatrixVal.vector(items)
            }
            is AstNode.MatrixLiteral -> {
                val evaluatedRows = node.rows.map { row ->
                    row.map { evalNode(it, scope) }
                }
                MathValue.MatrixVal(evaluatedRows)
            }
            is AstNode.ObjectLiteral -> {
                val map = LinkedHashMap<String, MathValue>()
                for ((k, vNode) in node.entries) {
                    map[k] = evalNode(vNode, scope)
                }
                MathValue.ObjectVal(map)
            }
            is AstNode.PropertyAccess -> {
                val target = evalNode(node.target, scope)
                when (target) {
                    is MathValue.ObjectVal -> target.properties[node.property]
                        ?: throw MathEvalException("Property '${node.property}' not found")
                    is MathValue.ComplexVal -> when (node.property) {
                        "re" -> MathValue.NumberVal(target.re)
                        "im" -> MathValue.NumberVal(target.im)
                        else -> throw MathEvalException("Unknown Complex property '${node.property}'")
                    }
                    else -> throw MathEvalException("Cannot access property '${node.property}' on ${target.typeName()}")
                }
            }
            is AstNode.IndexAccess -> {
                val target = evalNode(node.target, scope)
                evalIndexAccess(target, node.indices, scope)
            }
            is AstNode.FunctionCall -> {
                // Special case: inline function reference in map/filter/forEach e.g. map([1,2,3], f(x) = x^2)
                val evaluatedArgs = node.args.map { evalNode(it, scope) }
                val userFn = scope.get(node.name)
                if (userFn is MathValue.FunctionVal) {
                    invokeUserFunction(userFn, evaluatedArgs, scope)
                } else {
                    MathFunctions.invokeBuiltIn(node.name, evaluatedArgs) { fn, callArgs ->
                        invokeUserFunction(fn, callArgs, scope)
                    }
                }
            }
        }
    }

    private fun invokeUserFunction(
        fn: MathValue.FunctionVal,
        args: List<MathValue>,
        callerScope: MathScope
    ): MathValue {
        if (args.size != fn.params.size) {
            throw MathEvalException("Function ${fn.name} expects ${fn.params.size} argument(s), got ${args.size}")
        }
        val localScope = MathScope(callerScope.getUserSymbols() + fn.capturedScope)
        for (i in fn.params.indices) {
            localScope.set(fn.params[i], args[i])
        }
        return evalNode(fn.body, localScope)
    }

    private fun evalIndexAccess(
        target: MathValue,
        indexNodes: List<AstNode>,
        scope: MathScope
    ): MathValue {
        when (target) {
            is MathValue.ObjectVal -> {
                val key = evalNode(indexNodes.first(), scope)
                val kStr = (key as? MathValue.StringVal)?.value ?: key.format()
                return target.properties[kStr] ?: throw MathEvalException("Property '$kStr' not found")
            }
            is MathValue.StringVal -> {
                val s = target.value
                val localScope = MathScope(scope.getUserSymbols())
                localScope.set("end", MathValue.NumberVal(s.length.toDouble()))
                val idxVal = evalNode(indexNodes.first(), localScope)
                return when (idxVal) {
                    is MathValue.MatrixVal -> {
                        val chars = idxVal.flatValues().map {
                            val pos = it.asDouble().toInt() - 1
                            s.getOrElse(pos) { throw MathEvalException("String index ${pos + 1} out of bounds") }
                        }.joinToString("")
                        MathValue.StringVal(chars)
                    }
                    else -> {
                        val pos = idxVal.asDouble().toInt() - 1
                        val ch = s.getOrElse(pos) { throw MathEvalException("String index ${pos + 1} out of bounds") }
                        MathValue.StringVal(ch.toString())
                    }
                }
            }
            is MathValue.MatrixVal -> {
                if (indexNodes.size == 1) {
                    val flat = target.flatValues()
                    val localScope = MathScope(scope.getUserSymbols())
                    localScope.set("end", MathValue.NumberVal(flat.size.toDouble()))
                    val idxVal = evalNode(indexNodes[0], localScope)
                    return when (idxVal) {
                        is MathValue.MatrixVal -> {
                            val picked = idxVal.flatValues().map {
                                val pos = it.asDouble().toInt() - 1
                                flat.getOrElse(pos) { throw MathEvalException("Index ${pos + 1} out of bounds") }
                            }
                            MathValue.MatrixVal.vector(picked)
                        }
                        else -> {
                            val pos = idxVal.asDouble().toInt() - 1
                            flat.getOrElse(pos) { throw MathEvalException("Index ${pos + 1} out of bounds") }
                        }
                    }
                } else if (indexNodes.size == 2) {
                    val rowScope = MathScope(scope.getUserSymbols()).apply {
                        set("end", MathValue.NumberVal(target.rowCount.toDouble()))
                    }
                    val colScope = MathScope(scope.getUserSymbols()).apply {
                        set("end", MathValue.NumberVal(target.colCount.toDouble()))
                    }
                    val rIdxVal = evalNode(indexNodes[0], rowScope)
                    val cIdxVal = evalNode(indexNodes[1], colScope)
                    val rIndices = if (rIdxVal is MathValue.MatrixVal) rIdxVal.flatValues().map { it.asDouble().toInt() - 1 }
                    else listOf(rIdxVal.asDouble().toInt() - 1)
                    val cIndices = if (cIdxVal is MathValue.MatrixVal) cIdxVal.flatValues().map { it.asDouble().toInt() - 1 }
                    else listOf(cIdxVal.asDouble().toInt() - 1)

                    if (rIndices.size == 1 && cIndices.size == 1) {
                        return target.rows.getOrElse(rIndices[0]) { throw MathEvalException("Row index out of bounds") }
                            .getOrElse(cIndices[0]) { throw MathEvalException("Column index out of bounds") }
                    }
                    val subRows = rIndices.map { r ->
                        val row = target.rows.getOrElse(r) { throw MathEvalException("Row index out of bounds") }
                        cIndices.map { c ->
                            row.getOrElse(c) { throw MathEvalException("Column index out of bounds") }
                        }
                    }
                    return MathValue.MatrixVal(subRows)
                }
                throw MathEvalException("Unsupported number of indices")
            }
            else -> throw MathEvalException("Cannot index value of type ${target.typeName()}")
        }
    }

    fun evalBinaryOp(op: String, left: MathValue, right: MathValue): MathValue {
        // Element-wise matrix operations
        if (op == ".*" || op == "./" || op == ".^") {
            val baseOp = op.removePrefix(".")
            return MatrixOps.zipElementWise(left, right) { a, b -> evalBinaryOp(baseOp, a, b) }
        }

        // Matrix operations
        if (left is MathValue.MatrixVal || right is MathValue.MatrixVal) {
            return when (op) {
                "+", "-" -> MatrixOps.zipElementWise(left, right) { a, b -> evalBinaryOp(op, a, b) }
                "*" -> {
                    if (left is MathValue.MatrixVal && right is MathValue.MatrixVal) {
                        MatrixOps.multiply(
                            left,
                            right,
                            addOp = { a, b -> evalBinaryOp("+", a, b) },
                            mulOp = { a, b -> evalBinaryOp("*", a, b) }
                        )
                    } else {
                        MatrixOps.zipElementWise(left, right) { a, b -> evalBinaryOp("*", a, b) }
                    }
                }
                "/" -> {
                    if (right is MathValue.MatrixVal) {
                        throw MathEvalException("Use ./ for element-wise matrix division or inv() for matrix inverse")
                    }
                    MatrixOps.zipElementWise(left, right) { a, b -> evalBinaryOp("/", a, b) }
                }
                "^" -> {
                    if (left is MathValue.MatrixVal && right is MathValue.NumberVal) {
                        val exp = right.value.toInt()
                        if (exp < 0) throw MathEvalException("Negative matrix power not supported directly; use inv(A)")
                        if (exp == 0) return MatrixOps.eye(left.rowCount)
                        var acc: MathValue = left
                        repeat(exp - 1) {
                            acc = evalBinaryOp("*", acc, left)
                        }
                        acc
                    } else {
                        throw MathEvalException("Invalid matrix exponentiation")
                    }
                }
                "==", "!=", "<", ">", "<=", ">=" -> MatrixOps.zipElementWise(left, right) { a, b -> evalBinaryOp(op, a, b) }
                else -> MatrixOps.zipElementWise(left, right) { a, b -> evalBinaryOp(op, a, b) }
            }
        }

        // Unit operations
        if (left is MathValue.UnitVal || right is MathValue.UnitVal) {
            return evalUnitOp(op, left, right)
        }

        // Complex operations
        if (left is MathValue.ComplexVal || right is MathValue.ComplexVal) {
            val ca = left.asComplex()
            val cb = right.asComplex()
            return when (op) {
                "+" -> ComplexOps.add(ca, cb)
                "-" -> ComplexOps.sub(ca, cb)
                "*" -> ComplexOps.mul(ca, cb)
                "/" -> ComplexOps.div(ca, cb)
                "^" -> ComplexOps.pow(ca, cb)
                "==" -> MathValue.BoolVal(abs(ca.re - cb.re) < 1e-12 && abs(ca.im - cb.im) < 1e-12)
                "!=" -> MathValue.BoolVal(abs(ca.re - cb.re) >= 1e-12 || abs(ca.im - cb.im) >= 1e-12)
                else -> throw MathEvalException("Operator '$op' not supported on Complex numbers")
            }
        }

        // String concatenation / equality
        if (left is MathValue.StringVal || right is MathValue.StringVal) {
            val sa = (left as? MathValue.StringVal)?.value ?: left.format()
            val sb = (right as? MathValue.StringVal)?.value ?: right.format()
            return when (op) {
                "+" -> MathValue.StringVal(sa + sb)
                "==" -> MathValue.BoolVal(sa == sb)
                "!=" -> MathValue.BoolVal(sa != sb)
                else -> throw MathEvalException("Operator '$op' not supported on strings")
            }
        }

        // Logical ops
        when (op) {
            "and" -> return MathValue.BoolVal(left.asBoolean() && right.asBoolean())
            "or" -> return MathValue.BoolVal(left.asBoolean() || right.asBoolean())
            "xor" -> return MathValue.BoolVal(left.asBoolean() xor right.asBoolean())
        }

        // Real number operations
        val a = left.asDouble()
        val b = right.asDouble()
        return when (op) {
            "+" -> MathValue.NumberVal(a + b)
            "-" -> MathValue.NumberVal(a - b)
            "*" -> MathValue.NumberVal(a * b)
            "/" -> MathValue.NumberVal(a / b)
            "%" -> {
                // MathJS modulus: a - b * floor(a / b)
                val r = a % b
                MathValue.NumberVal(r)
            }
            "^" -> {
                if (a < 0 && abs(b - round(b)) > 1e-12) {
                    ComplexOps.pow(MathValue.ComplexVal(a, 0.0), MathValue.ComplexVal(b, 0.0))
                } else {
                    MathValue.NumberVal(a.pow(b))
                }
            }
            "==" -> MathValue.BoolVal(abs(a - b) < 1e-12)
            "!=" -> MathValue.BoolVal(abs(a - b) >= 1e-12)
            "<" -> MathValue.BoolVal(a < b)
            ">" -> MathValue.BoolVal(a > b)
            "<=" -> MathValue.BoolVal(a <= b + 1e-12)
            ">=" -> MathValue.BoolVal(a >= b - 1e-12)
            "&" -> MathValue.NumberVal((a.toLong() and b.toLong()).toDouble())
            "|" -> MathValue.NumberVal((a.toLong() or b.toLong()).toDouble())
            "^|" -> MathValue.NumberVal((a.toLong() xor b.toLong()).toDouble())
            "<<" -> MathValue.NumberVal((a.toLong() shl b.toInt()).toDouble())
            ">>" -> MathValue.NumberVal((a.toLong() shr b.toInt()).toDouble())
            ">>>" -> MathValue.NumberVal((a.toInt() ushr b.toInt()).toDouble())
            else -> throw MathEvalException("Unsupported operator '$op'")
        }
    }

    private fun evalUnitOp(op: String, left: MathValue, right: MathValue): MathValue {
        return when (op) {
            "+" -> {
                if (left is MathValue.UnitVal && right is MathValue.UnitVal) {
                    val rightInLeft = right.unit.convertTo(right.value, left.unit)
                    MathValue.UnitVal(left.value + rightInLeft, left.unit)
                } else {
                    throw MathEvalException("Cannot add unit and dimensionless number")
                }
            }
            "-" -> {
                if (left is MathValue.UnitVal && right is MathValue.UnitVal) {
                    val rightInLeft = right.unit.convertTo(right.value, left.unit)
                    MathValue.UnitVal(left.value - rightInLeft, left.unit)
                } else {
                    throw MathEvalException("Cannot subtract unit and dimensionless number")
                }
            }
            "*" -> {
                when {
                    left is MathValue.UnitVal && right is MathValue.UnitVal -> {
                        val combined = left.unit.multiply(right.unit)
                        val v = left.unit.toBase(left.value) * right.unit.toBase(right.value) / combined.scaleToBase
                        if (combined.dimensions.isEmpty()) MathValue.NumberVal(v)
                        else MathValue.UnitVal(v, combined)
                    }
                    left is MathValue.UnitVal -> MathValue.UnitVal(left.value * right.asDouble(), left.unit)
                    right is MathValue.UnitVal -> MathValue.UnitVal(left.asDouble() * right.value, right.unit)
                    else -> throw MathEvalException("Invalid unit multiplication")
                }
            }
            "/" -> {
                when {
                    left is MathValue.UnitVal && right is MathValue.UnitVal -> {
                        val combined = left.unit.divide(right.unit)
                        val v = (left.unit.toBase(left.value) / right.unit.toBase(right.value)) / combined.scaleToBase
                        if (combined.dimensions.isEmpty()) MathValue.NumberVal(v)
                        else MathValue.UnitVal(v, combined)
                    }
                    left is MathValue.UnitVal -> MathValue.UnitVal(left.value / right.asDouble(), left.unit)
                    right is MathValue.UnitVal -> {
                        val invUnit = MathUnit(emptyMap(), 1.0, 0.0, "").divide(right.unit)
                        MathValue.UnitVal(left.asDouble() / right.value, invUnit)
                    }
                    else -> throw MathEvalException("Invalid unit division")
                }
            }
            "^" -> {
                if (left is MathValue.UnitVal && right is MathValue.NumberVal) {
                    val exp = right.value.toInt()
                    MathValue.UnitVal(left.value.pow(exp), left.unit.pow(exp))
                } else {
                    throw MathEvalException("Invalid unit exponentiation")
                }
            }
            "==", "!=", "<", ">", "<=", ">=" -> {
                if (left is MathValue.UnitVal && right is MathValue.UnitVal) {
                    val a = left.unit.toBase(left.value)
                    val b = right.unit.toBase(right.value)
                    if (!left.unit.isCompatibleWith(right.unit)) {
                        throw MathEvalException("Cannot compare incompatible units")
                    }
                    evalBinaryOp(op, MathValue.NumberVal(a), MathValue.NumberVal(b))
                } else {
                    throw MathEvalException("Cannot compare unit with dimensionless value")
                }
            }
            else -> throw MathEvalException("Operator '$op' not supported on units")
        }
    }
}
