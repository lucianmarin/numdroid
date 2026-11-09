package com.lucianmarin.num.math

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

sealed class MathValue {
    data class NumberVal(val value: Double) : MathValue()
    data class ComplexVal(val re: Double, val im: Double) : MathValue()
    data class BoolVal(val value: Boolean) : MathValue()
    data class StringVal(val value: String) : MathValue()
    data class UnitVal(val value: Double, val unit: MathUnit) : MathValue()
    data class MatrixVal(val rows: List<List<MathValue>>) : MathValue() {
        val isVector: Boolean get() = rows.size == 1
        val rowCount: Int get() = rows.size
        val colCount: Int get() = rows.firstOrNull()?.size ?: 0

        fun flatValues(): List<MathValue> = rows.flatten()

        fun toDoubleMatrix(): Array<DoubleArray> =
            Array(rows.size) { r ->
                DoubleArray(rows[r].size) { c ->
                    rows[r][c].asDouble()
                }
            }

        companion object {
            fun vector(items: List<MathValue>): MatrixVal = MatrixVal(listOf(items))
            fun fromDoubles(data: Array<DoubleArray>): MatrixVal =
                MatrixVal(data.map { row -> row.map { NumberVal(it) } })
        }
    }
    data class ObjectVal(val properties: Map<String, MathValue>) : MathValue()
    data class FunctionVal(
        val name: String,
        val params: List<String>,
        val exprText: String,
        val body: AstNode,
        val capturedScope: Map<String, MathValue>
    ) : MathValue() {
        val syntax: String get() = "$name(${params.joinToString(", ")})"
    }
    data object NullVal : MathValue()

    fun asDouble(): Double = when (this) {
        is NumberVal -> value
        is BoolVal -> if (value) 1.0 else 0.0
        is ComplexVal -> if (abs(im) < 1e-12) re else throw MathEvalException("Cannot convert complex number to real number")
        is UnitVal -> value
        is StringVal -> value.toDoubleOrNull() ?: throw MathEvalException("Cannot convert string \"$value\" to number")
        else -> throw MathEvalException("Expected a number, got ${typeName()}")
    }

    fun asComplex(): ComplexVal = when (this) {
        is ComplexVal -> this
        is NumberVal -> ComplexVal(value, 0.0)
        is BoolVal -> ComplexVal(if (value) 1.0 else 0.0, 0.0)
        else -> throw MathEvalException("Cannot convert ${typeName()} to complex")
    }

    fun asBoolean(): Boolean = when (this) {
        is BoolVal -> value
        is NumberVal -> value != 0.0 && !value.isNaN()
        is ComplexVal -> re != 0.0 || im != 0.0
        is StringVal -> value.isNotEmpty()
        is NullVal -> false
        else -> true
    }

    fun typeName(): String = when (this) {
        is NumberVal -> "number"
        is ComplexVal -> "Complex"
        is BoolVal -> "boolean"
        is StringVal -> "string"
        is UnitVal -> "Unit"
        is MatrixVal -> "Matrix"
        is ObjectVal -> "Object"
        is FunctionVal -> "function"
        is NullVal -> "null"
    }

    fun format(precision: Int = 14): String = when (this) {
        is NumberVal -> formatNumber(value, precision)
        is ComplexVal -> formatComplex(re, im, precision)
        is BoolVal -> value.toString()
        is StringVal -> "\"$value\""
        is UnitVal -> "${formatNumber(value, precision)} ${unit.formatName()}"
        is MatrixVal -> {
            if (rows.isEmpty()) "[]"
            else if (rows.size == 1) {
                "[${rows[0].joinToString(", ") { it.format(precision) }}]"
            } else {
                "[${rows.joinToString("; ") { row -> row.joinToString(", ") { it.format(precision) } }}]"
            }
        }
        is ObjectVal -> {
            "{${properties.entries.joinToString(", ") { (k, v) -> "$k: ${v.format(precision)}" }}}"
        }
        is FunctionVal -> syntax
        is NullVal -> "null"
    }

    companion object {
        fun formatNumber(value: Double, precision: Int = 14): String {
            if (value.isNaN()) return "NaN"
            if (value == Double.POSITIVE_INFINITY) return "Infinity"
            if (value == Double.NEGATIVE_INFINITY) return "-Infinity"
            val cleaned = if (abs(value) < 1e-15) 0.0 else value
            if (cleaned == 0.0) return "0"

            val absVal = abs(cleaned)
            val bd = try {
                BigDecimal.valueOf(cleaned).round(java.math.MathContext(precision, RoundingMode.HALF_UP)).stripTrailingZeros()
            } catch (_: Exception) {
                return cleaned.toString()
            }

            return if (absVal >= 1e14 || (absVal > 0 && absVal < 1e-4)) {
                val plain = bd.toEngineeringString()
                // Format as MathJS lower-case scientific e.g. 1.23e+21 or 1.23e-5
                val scale = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
                val mantissa = BigDecimal.valueOf(cleaned / 10.0.pow(scale))
                    .round(java.math.MathContext(precision, RoundingMode.HALF_UP))
                    .stripTrailingZeros()
                    .toPlainString()
                val sign = if (scale >= 0) "+" else ""
                "${mantissa}e$sign$scale"
            } else {
                bd.toPlainString()
            }
        }

        fun formatComplex(re: Double, im: Double, precision: Int = 14): String {
            val cleanRe = if (abs(re) < 1e-14) 0.0 else re
            val cleanIm = if (abs(im) < 1e-14) 0.0 else im
            if (cleanIm == 0.0) return formatNumber(cleanRe, precision)
            val imAbs = abs(cleanIm)
            val imPart = when (imAbs) {
                1.0 -> "i"
                else -> "${formatNumber(imAbs, precision)}i"
            }
            if (cleanRe == 0.0) {
                return if (cleanIm < 0) "-$imPart" else imPart
            }
            val sign = if (cleanIm < 0) " - " else " + "
            return "${formatNumber(cleanRe, precision)}$sign$imPart"
        }

        fun complexOrReal(re: Double, im: Double): MathValue {
            return if (abs(im) < 1e-14) NumberVal(if (abs(re) < 1e-15) 0.0 else re)
            else ComplexVal(re, im)
        }
    }
}

class MathEvalException(message: String) : RuntimeException(message)

object ComplexOps {
    fun add(a: MathValue.ComplexVal, b: MathValue.ComplexVal): MathValue =
        MathValue.complexOrReal(a.re + b.re, a.im + b.im)

    fun sub(a: MathValue.ComplexVal, b: MathValue.ComplexVal): MathValue =
        MathValue.complexOrReal(a.re - b.re, a.im - b.im)

    fun mul(a: MathValue.ComplexVal, b: MathValue.ComplexVal): MathValue =
        MathValue.complexOrReal(
            a.re * b.re - a.im * b.im,
            a.re * b.im + a.im * b.re
        )

    fun div(a: MathValue.ComplexVal, b: MathValue.ComplexVal): MathValue {
        val denom = b.re * b.re + b.im * b.im
        if (denom == 0.0) return MathValue.NumberVal(Double.NaN)
        return MathValue.complexOrReal(
            (a.re * b.re + a.im * b.im) / denom,
            (a.im * b.re - a.re * b.im) / denom
        )
    }

    fun abs(z: MathValue.ComplexVal): Double = hypot(z.re, z.im)
    fun arg(z: MathValue.ComplexVal): Double = atan2(z.im, z.re)
    fun conj(z: MathValue.ComplexVal): MathValue.ComplexVal = MathValue.ComplexVal(z.re, -z.im)

    fun sqrt(z: MathValue.ComplexVal): MathValue {
        val r = abs(z)
        val re = kotlin.math.sqrt((r + z.re) / 2.0)
        val im = (if (z.im < 0) -1.0 else 1.0) * kotlin.math.sqrt(kotlin.math.max(0.0, (r - z.re) / 2.0))
        return MathValue.complexOrReal(re, im)
    }

    fun exp(z: MathValue.ComplexVal): MathValue {
        val er = kotlin.math.exp(z.re)
        return MathValue.complexOrReal(er * cos(z.im), er * sin(z.im))
    }

    fun ln(z: MathValue.ComplexVal): MathValue {
        return MathValue.complexOrReal(kotlin.math.ln(abs(z)), arg(z))
    }

    fun pow(a: MathValue.ComplexVal, b: MathValue.ComplexVal): MathValue {
        if (a.re == 0.0 && a.im == 0.0) {
            return if (b.re > 0) MathValue.NumberVal(0.0) else MathValue.NumberVal(Double.NaN)
        }
        val lnA = ln(a).asComplex()
        val prod = mul(b, lnA).asComplex()
        return exp(prod)
    }

    fun sin(z: MathValue.ComplexVal): MathValue =
        MathValue.complexOrReal(sin(z.re) * cosh(z.im), cos(z.re) * sinh(z.im))

    fun cos(z: MathValue.ComplexVal): MathValue =
        MathValue.complexOrReal(cos(z.re) * cosh(z.im), -sin(z.re) * sinh(z.im))

    fun tan(z: MathValue.ComplexVal): MathValue =
        div(sin(z).asComplex(), cos(z).asComplex())
}
