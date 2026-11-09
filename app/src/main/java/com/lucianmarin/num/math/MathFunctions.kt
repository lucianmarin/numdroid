package com.lucianmarin.num.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.math.truncate
import kotlin.random.Random

object MathFunctions {
    val CONSTANTS: Map<String, MathValue> = mapOf(
        "pi" to MathValue.NumberVal(PI),
        "PI" to MathValue.NumberVal(PI),
        "e" to MathValue.NumberVal(Math.E),
        "E" to MathValue.NumberVal(Math.E),
        "tau" to MathValue.NumberVal(2.0 * PI),
        "phi" to MathValue.NumberVal((1.0 + sqrt(5.0)) / 2.0),
        "i" to MathValue.ComplexVal(0.0, 1.0),
        "Infinity" to MathValue.NumberVal(Double.POSITIVE_INFINITY),
        "NaN" to MathValue.NumberVal(Double.NaN),
        "true" to MathValue.BoolVal(true),
        "false" to MathValue.BoolVal(false),
        "null" to MathValue.NullVal,
        "LN2" to MathValue.NumberVal(ln(2.0)),
        "LN10" to MathValue.NumberVal(ln(10.0)),
        "LOG2E" to MathValue.NumberVal(log2(Math.E)),
        "LOG10E" to MathValue.NumberVal(log10(Math.E)),
        "SQRT1_2" to MathValue.NumberVal(sqrt(0.5)),
        "SQRT2" to MathValue.NumberVal(sqrt(2.0)),
        "version" to MathValue.StringVal("13.0.3-kotlin"),

        // Physical constants (MathJS CODATA values with units)
        "speedOfLight" to MathValue.UnitVal(299792458.0, UnitRegistry.resolve("m")!!.divide(UnitRegistry.resolve("s")!!)),
        "gravitationConstant" to MathValue.NumberVal(6.67430e-11),
        "planckConstant" to MathValue.NumberVal(6.62607015e-34),
        "reducedPlanckConstant" to MathValue.NumberVal(1.054571817e-34),
        "magneticConstant" to MathValue.NumberVal(1.25663706212e-6),
        "electricConstant" to MathValue.NumberVal(8.8541878128e-12),
        "vacuumImpedance" to MathValue.NumberVal(376.730313668),
        "coulomb" to MathValue.NumberVal(8.9875517923e9),
        "elementaryCharge" to MathValue.NumberVal(1.602176634e-19),
        "bohrMagneton" to MathValue.NumberVal(9.2740100783e-24),
        "conductanceQuantum" to MathValue.NumberVal(7.748091729e-5),
        "inverseConductanceQuantum" to MathValue.NumberVal(12906.40372),
        "magneticFluxQuantum" to MathValue.NumberVal(2.067833848e-15),
        "nuclearMagneton" to MathValue.NumberVal(5.0507837461e-27),
        "klitzing" to MathValue.NumberVal(25812.80745),
        "bohrRadius" to MathValue.NumberVal(5.29177210903e-11),
        "classicalElectronRadius" to MathValue.NumberVal(2.8179403262e-15),
        "electronMass" to MathValue.NumberVal(9.1093837015e-31),
        "fermiCoupling" to MathValue.NumberVal(1.1663787e-5),
        "fineStructure" to MathValue.NumberVal(7.2973525693e-3),
        "hartreeEnergy" to MathValue.NumberVal(4.3597447222071e-18),
        "protonMass" to MathValue.NumberVal(1.67262192369e-27),
        "deuteronMass" to MathValue.NumberVal(3.3435837724e-27),
        "neutronMass" to MathValue.NumberVal(1.67492749804e-27),
        "quantumOfCirculation" to MathValue.NumberVal(3.6369475516e-4),
        "rydberg" to MathValue.NumberVal(10973731.568160),
        "thomsonCrossSection" to MathValue.NumberVal(6.6524587321e-29),
        "weakMixingAngle" to MathValue.NumberVal(0.22290),
        "efimovFactor" to MathValue.NumberVal(22.7),
        "atomicMass" to MathValue.NumberVal(1.66053906660e-27),
        "avogadro" to MathValue.NumberVal(6.02214076e23),
        "boltzmann" to MathValue.NumberVal(1.380649e-23),
        "faraday" to MathValue.NumberVal(96485.33212),
        "firstRadiation" to MathValue.NumberVal(3.741771852e-16),
        "loschmidt" to MathValue.NumberVal(2.686780111e25),
        "gasConstant" to MathValue.NumberVal(8.314462618),
        "molarPlanckConstant" to MathValue.NumberVal(3.990312712e-10),
        "molarVolume" to MathValue.NumberVal(0.02241396954),
        "sackurTetrode" to MathValue.NumberVal(-1.16487052358),
        "secondRadiation" to MathValue.NumberVal(0.01438776877),
        "stefanBoltzmann" to MathValue.NumberVal(5.670374419e-8),
        "wienDisplacement" to MathValue.NumberVal(2.897771955e-3),
        "gravity" to MathValue.UnitVal(9.80665, UnitRegistry.resolve("m")!!.divide(UnitRegistry.resolve("s2")!!)),
        "atm" to MathValue.UnitVal(101325.0, UnitRegistry.resolve("Pa")!!)
    )

    fun factorial(n: Double): Double {
        if (n < 0) throw MathEvalException("Factorial only defined for non-negative numbers")
        val intN = n.toInt()
        if (abs(n - intN) > 1e-12) {
            return gamma(n + 1.0)
        }
        if (intN > 170) return Double.POSITIVE_INFINITY
        var res = 1.0
        for (i in 2..intN) res *= i.toDouble()
        return res
    }

    // Lanczos approximation for Gamma function
    fun gamma(z: Double): Double {
        if (z < 0.5) {
            return PI / (sin(PI * z) * gamma(1.0 - z))
        }
        val x = z - 1.0
        val p = doubleArrayOf(
            0.9999999999998099,
            676.5203681218851,
            -1259.1392167224028,
            771.3234287776531,
            -176.6150291621406,
            12.507343278686905,
            -0.13857109526572012,
            9.984369578019572e-6,
            1.5056327351493116e-7
        )
        var a = p[0]
        val t = x + 7.5
        for (i in 1 until p.size) {
            a += p[i] / (x + i)
        }
        return sqrt(2.0 * PI) * t.pow(x + 0.5) * exp(-t) * a
    }

    fun erf(x: Double): Double {
        val sign = if (x < 0) -1.0 else 1.0
        val ax = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * ax)
        val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-ax * ax)
        return sign * y
    }

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    fun lcm(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return abs(a / gcd(a, b) * b)
    }

    fun isPrime(n: Long): Boolean {
        if (n < 2L) return false
        if (n == 2L || n == 3L) return true
        if (n % 2L == 0L || n % 3L == 0L) return false
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2L) == 0L) return false
            i += 6L
        }
        return true
    }

    fun combinations(n: Long, k: Long): Double {
        if (k < 0 || k > n) throw MathEvalException("combinations: k must be between 0 and n")
        val kk = min(k, n - k)
        var res = 1.0
        for (i in 1..kk) {
            res = res * (n - kk + i) / i
        }
        return round(res)
    }

    fun permutations(n: Long, k: Long = n): Double {
        if (k < 0 || k > n) throw MathEvalException("permutations: k must be between 0 and n")
        var res = 1.0
        for (i in (n - k + 1)..n) {
            res *= i.toDouble()
        }
        return res
    }

    private fun extractNumbers(args: List<MathValue>): List<Double> {
        val out = mutableListOf<Double>()
        for (arg in args) {
            when (arg) {
                is MathValue.MatrixVal -> out.addAll(arg.flatValues().map { it.asDouble() })
                else -> out.add(arg.asDouble())
            }
        }
        if (out.isEmpty()) throw MathEvalException("Function requires at least one numeric value")
        return out
    }

    private fun applyUnaryMath(arg: MathValue, realOp: (Double) -> MathValue): MathValue {
        return when (arg) {
            is MathValue.MatrixVal -> MatrixOps.mapElementWise(arg) { applyUnaryMath(it, realOp) }
            is MathValue.UnitVal -> {
                if (arg.unit.dimensions == mapOf(UnitDimension.ANGLE to 1)) {
                    realOp(arg.unit.toBase(arg.value))
                } else {
                    realOp(arg.value)
                }
            }
            else -> realOp(arg.asDouble())
        }
    }

    fun invokeBuiltIn(
        name: String,
        args: List<MathValue>,
        evaluator: (MathValue.FunctionVal, List<MathValue>) -> MathValue
    ): MathValue {
        return when (name) {
            // Basic Arithmetic Functions
            "abs" -> {
                val a = args.firstOrNull() ?: throw MathEvalException("abs() requires 1 argument")
                when (a) {
                    is MathValue.ComplexVal -> MathValue.NumberVal(ComplexOps.abs(a))
                    is MathValue.UnitVal -> MathValue.UnitVal(abs(a.value), a.unit)
                    is MathValue.MatrixVal -> MatrixOps.mapElementWise(a) { invokeBuiltIn("abs", listOf(it), evaluator) }
                    else -> MathValue.NumberVal(abs(a.asDouble()))
                }
            }
            "add" -> {
                if (args.size < 2) throw MathEvalException("add() requires 2 arguments")
                args.reduce { acc, v -> MathEvaluator.evalBinaryOp("+", acc, v) }
            }
            "subtract" -> {
                if (args.size != 2) throw MathEvalException("subtract() requires 2 arguments")
                MathEvaluator.evalBinaryOp("-", args[0], args[1])
            }
            "multiply" -> {
                if (args.size < 2) throw MathEvalException("multiply() requires 2 arguments")
                args.reduce { acc, v -> MathEvaluator.evalBinaryOp("*", acc, v) }
            }
            "divide" -> {
                if (args.size != 2) throw MathEvalException("divide() requires 2 arguments")
                MathEvaluator.evalBinaryOp("/", args[0], args[1])
            }
            "mod" -> {
                if (args.size != 2) throw MathEvalException("mod() requires 2 arguments")
                MathEvaluator.evalBinaryOp("%", args[0], args[1])
            }
            "pow" -> {
                if (args.size != 2) throw MathEvalException("pow() requires 2 arguments")
                MathEvaluator.evalBinaryOp("^", args[0], args[1])
            }
            "dotMultiply" -> {
                if (args.size != 2) throw MathEvalException("dotMultiply() requires 2 arguments")
                MathEvaluator.evalBinaryOp(".*", args[0], args[1])
            }
            "dotDivide" -> {
                if (args.size != 2) throw MathEvalException("dotDivide() requires 2 arguments")
                MathEvaluator.evalBinaryOp("./", args[0], args[1])
            }
            "dotPow" -> {
                if (args.size != 2) throw MathEvalException("dotPow() requires 2 arguments")
                MathEvaluator.evalBinaryOp(".^", args[0], args[1])
            }

            // Rounding & Sign
            "round" -> {
                val x = args.getOrNull(0) ?: throw MathEvalException("round() requires 1 or 2 arguments")
                val decimals = args.getOrNull(1)?.asDouble()?.toInt() ?: 0
                val factor = 10.0.pow(decimals)
                applyUnaryMath(x) { MathValue.NumberVal(round(it * factor) / factor) }
            }
            "floor" -> applyUnaryMath(args[0]) { MathValue.NumberVal(floor(it)) }
            "ceil" -> applyUnaryMath(args[0]) { MathValue.NumberVal(ceil(it)) }
            "fix" -> applyUnaryMath(args[0]) { MathValue.NumberVal(truncate(it)) }
            "sign" -> applyUnaryMath(args[0]) { MathValue.NumberVal(sign(it)) }
            "cbrt" -> applyUnaryMath(args[0]) { MathValue.NumberVal(cbrt(it)) }
            "cube" -> applyUnaryMath(args[0]) { MathValue.NumberVal(it * it * it) }
            "square" -> applyUnaryMath(args[0]) { MathValue.NumberVal(it * it) }
            "sqrt" -> {
                val a = args[0]
                if (a is MathValue.ComplexVal) ComplexOps.sqrt(a)
                else if (a is MathValue.NumberVal && a.value < 0) ComplexOps.sqrt(MathValue.ComplexVal(a.value, 0.0))
                else applyUnaryMath(a) {
                    if (it < 0) ComplexOps.sqrt(MathValue.ComplexVal(it, 0.0))
                    else MathValue.NumberVal(sqrt(it))
                }
            }
            "nthRoot" -> {
                val x = args[0].asDouble()
                val n = args.getOrNull(1)?.asDouble() ?: 2.0
                MathValue.NumberVal(x.pow(1.0 / n))
            }
            "hypot" -> {
                val nums = extractNumbers(args)
                MathValue.NumberVal(sqrt(nums.sumOf { it * it }))
            }
            "norm" -> {
                val a = args[0]
                when (a) {
                    is MathValue.MatrixVal -> MathValue.NumberVal(MatrixOps.norm(a))
                    is MathValue.ComplexVal -> MathValue.NumberVal(ComplexOps.abs(a))
                    else -> MathValue.NumberVal(abs(a.asDouble()))
                }
            }

            // Exponentials & Logarithms
            "exp" -> {
                val a = args[0]
                if (a is MathValue.ComplexVal) ComplexOps.exp(a)
                else applyUnaryMath(a) { MathValue.NumberVal(exp(it)) }
            }
            "expm1" -> applyUnaryMath(args[0]) { MathValue.NumberVal(expm1(it)) }
            "log", "ln" -> {
                val a = args[0]
                if (args.size == 2) {
                    val base = args[1].asDouble()
                    MathValue.NumberVal(ln(a.asDouble()) / ln(base))
                } else if (a is MathValue.ComplexVal) {
                    ComplexOps.ln(a)
                } else {
                    applyUnaryMath(a) { MathValue.NumberVal(ln(it)) }
                }
            }
            "log10" -> applyUnaryMath(args[0]) { MathValue.NumberVal(log10(it)) }
            "log2" -> applyUnaryMath(args[0]) { MathValue.NumberVal(log2(it)) }
            "log1p" -> applyUnaryMath(args[0]) { MathValue.NumberVal(ln1p(it)) }

            // Number Theory
            "gcd" -> {
                val nums = extractNumbers(args).map { it.toLong() }
                MathValue.NumberVal(nums.reduce { acc, v -> gcd(acc, v) }.toDouble())
            }
            "lcm" -> {
                val nums = extractNumbers(args).map { it.toLong() }
                MathValue.NumberVal(nums.reduce { acc, v -> lcm(acc, v) }.toDouble())
            }
            "xgcd" -> {
                var a = args[0].asDouble().toLong()
                var b = args[1].asDouble().toLong()
                var x0 = 1L; var x1 = 0L
                var y0 = 0L; var y1 = 1L
                while (b != 0L) {
                    val q = a / b
                    val r = a % b
                    a = b; b = r
                    val nx = x0 - q * x1; x0 = x1; x1 = nx
                    val ny = y0 - q * y1; y0 = y1; y1 = ny
                }
                MathValue.MatrixVal.vector(
                    listOf(
                        MathValue.NumberVal(a.toDouble()),
                        MathValue.NumberVal(x0.toDouble()),
                        MathValue.NumberVal(y0.toDouble())
                    )
                )
            }
            "isPrime" -> MathValue.BoolVal(isPrime(args[0].asDouble().toLong()))
            "isInteger" -> {
                val d = args[0].asDouble()
                MathValue.BoolVal(d.isFinite() && abs(d - round(d)) < 1e-12)
            }
            "isPositive" -> MathValue.BoolVal(args[0].asDouble() > 0.0)
            "isNegative" -> MathValue.BoolVal(args[0].asDouble() < 0.0)
            "isZero" -> MathValue.BoolVal(abs(args[0].asDouble()) < 1e-15)
            "isNaN" -> MathValue.BoolVal(args[0].asDouble().isNaN())
            "isNumeric" -> MathValue.BoolVal(args[0] is MathValue.NumberVal || args[0] is MathValue.ComplexVal)

            // Trigonometry
            "sin" -> {
                val a = args[0]
                if (a is MathValue.ComplexVal) ComplexOps.sin(a)
                else applyUnaryMath(a) { MathValue.NumberVal(sin(it)) }
            }
            "cos" -> {
                val a = args[0]
                if (a is MathValue.ComplexVal) ComplexOps.cos(a)
                else applyUnaryMath(a) { MathValue.NumberVal(cos(it)) }
            }
            "tan" -> {
                val a = args[0]
                if (a is MathValue.ComplexVal) ComplexOps.tan(a)
                else applyUnaryMath(a) { MathValue.NumberVal(tan(it)) }
            }
            "asin" -> applyUnaryMath(args[0]) { MathValue.NumberVal(asin(it)) }
            "acos" -> applyUnaryMath(args[0]) { MathValue.NumberVal(acos(it)) }
            "atan" -> applyUnaryMath(args[0]) { MathValue.NumberVal(atan(it)) }
            "atan2" -> MathValue.NumberVal(atan2(args[0].asDouble(), args[1].asDouble()))
            "sinh" -> applyUnaryMath(args[0]) { MathValue.NumberVal(sinh(it)) }
            "cosh" -> applyUnaryMath(args[0]) { MathValue.NumberVal(cosh(it)) }
            "tanh" -> applyUnaryMath(args[0]) { MathValue.NumberVal(tanh(it)) }
            "asinh" -> applyUnaryMath(args[0]) { MathValue.NumberVal(asinh(it)) }
            "acosh" -> applyUnaryMath(args[0]) { MathValue.NumberVal(acosh(it)) }
            "atanh" -> applyUnaryMath(args[0]) { MathValue.NumberVal(atanh(it)) }
            "sec" -> applyUnaryMath(args[0]) { MathValue.NumberVal(1.0 / cos(it)) }
            "csc" -> applyUnaryMath(args[0]) { MathValue.NumberVal(1.0 / sin(it)) }
            "cot" -> applyUnaryMath(args[0]) { MathValue.NumberVal(1.0 / tan(it)) }
            "asec" -> applyUnaryMath(args[0]) { MathValue.NumberVal(acos(1.0 / it)) }
            "acsc" -> applyUnaryMath(args[0]) { MathValue.NumberVal(asin(1.0 / it)) }
            "acot" -> applyUnaryMath(args[0]) { MathValue.NumberVal(atan(1.0 / it)) }
            "sech" -> applyUnaryMath(args[0]) { MathValue.NumberVal(1.0 / cosh(it)) }
            "csch" -> applyUnaryMath(args[0]) { MathValue.NumberVal(1.0 / sinh(it)) }
            "coth" -> applyUnaryMath(args[0]) { MathValue.NumberVal(cosh(it) / sinh(it)) }
            "asech" -> applyUnaryMath(args[0]) { MathValue.NumberVal(acosh(1.0 / it)) }
            "acsch" -> applyUnaryMath(args[0]) { MathValue.NumberVal(asinh(1.0 / it)) }
            "acoth" -> applyUnaryMath(args[0]) { MathValue.NumberVal(atanh(1.0 / it)) }
            "deg" -> MathValue.NumberVal(args[0].asDouble() * 180.0 / PI)
            "rad" -> MathValue.NumberVal(args[0].asDouble() * PI / 180.0)

            // Complex Functions
            "complex" -> {
                when (args.size) {
                    0 -> MathValue.ComplexVal(0.0, 0.0)
                    1 -> args[0].asComplex()
                    else -> MathValue.complexOrReal(args[0].asDouble(), args[1].asDouble())
                }
            }
            "re" -> MathValue.NumberVal(args[0].asComplex().re)
            "im" -> MathValue.NumberVal(args[0].asComplex().im)
            "conj" -> ComplexOps.conj(args[0].asComplex())
            "arg" -> MathValue.NumberVal(ComplexOps.arg(args[0].asComplex()))

            // Statistics
            "sum" -> MathValue.NumberVal(extractNumbers(args).sum())
            "prod" -> MathValue.NumberVal(extractNumbers(args).fold(1.0) { a, b -> a * b })
            "mean", "avg" -> {
                val nums = extractNumbers(args)
                MathValue.NumberVal(nums.sum() / nums.size)
            }
            "median" -> {
                val sorted = extractNumbers(args).sorted()
                val mid = sorted.size / 2
                val med = if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
                MathValue.NumberVal(med)
            }
            "min" -> MathValue.NumberVal(extractNumbers(args).minOrNull() ?: Double.NaN)
            "max" -> MathValue.NumberVal(extractNumbers(args).maxOrNull() ?: Double.NaN)
            "variance" -> {
                val nums = extractNumbers(args)
                if (nums.size <= 1) return MathValue.NumberVal(0.0)
                val mean = nums.sum() / nums.size
                val sumSq = nums.sumOf { (it - mean) * (it - mean) }
                MathValue.NumberVal(sumSq / (nums.size - 1))
            }
            "std" -> {
                val v = (invokeBuiltIn("variance", args, evaluator) as MathValue.NumberVal).value
                MathValue.NumberVal(sqrt(v))
            }
            "mad" -> {
                val nums = extractNumbers(args)
                val med = (invokeBuiltIn("median", args, evaluator) as MathValue.NumberVal).value
                val devs = nums.map { MathValue.NumberVal(abs(it - med)) }
                invokeBuiltIn("median", listOf(MathValue.MatrixVal.vector(devs)), evaluator)
            }
            "mode" -> {
                val nums = extractNumbers(args)
                val counts = nums.groupingBy { it }.eachCount()
                val maxCount = counts.values.maxOrNull() ?: 0
                val modes = counts.filterValues { it == maxCount }.keys.sorted().map { MathValue.NumberVal(it) }
                MathValue.MatrixVal.vector(modes)
            }
            "cumsum" -> {
                val nums = extractNumbers(args)
                var running = 0.0
                MathValue.MatrixVal.vector(nums.map { running += it; MathValue.NumberVal(running) })
            }
            "diff" -> {
                val nums = extractNumbers(args)
                if (nums.size < 2) MathValue.MatrixVal.vector(emptyList())
                else MathValue.MatrixVal.vector((1 until nums.size).map { MathValue.NumberVal(nums[it] - nums[it - 1]) })
            }
            "corr" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                val ma = a.average()
                val mb = b.average()
                var num = 0.0
                var da = 0.0
                var db = 0.0
                for (i in a.indices) {
                    val xa = a[i] - ma
                    val xb = b[i] - mb
                    num += xa * xb
                    da += xa * xa
                    db += xb * xb
                }
                MathValue.NumberVal(num / sqrt(da * db))
            }
            "quantileSeq" -> {
                val nums = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.sorted()
                val p = args[1].asDouble()
                val pos = p * (nums.size - 1)
                val lower = floor(pos).toInt().coerceIn(0, nums.lastIndex)
                val upper = ceil(pos).toInt().coerceIn(0, nums.lastIndex)
                val weight = pos - lower
                MathValue.NumberVal(nums[lower] * (1.0 - weight) + nums[upper] * weight)
            }

            // Probability & Combinatorics
            "factorial" -> applyUnaryMath(args[0]) { MathValue.NumberVal(factorial(it)) }
            "gamma" -> applyUnaryMath(args[0]) { MathValue.NumberVal(gamma(it)) }
            "lgamma" -> applyUnaryMath(args[0]) { MathValue.NumberVal(ln(abs(gamma(it)))) }
            "erf" -> applyUnaryMath(args[0]) { MathValue.NumberVal(erf(it)) }
            "combinations" -> MathValue.NumberVal(combinations(args[0].asDouble().toLong(), args[1].asDouble().toLong()))
            "combinationsWithRep" -> {
                val n = args[0].asDouble().toLong()
                val k = args[1].asDouble().toLong()
                MathValue.NumberVal(combinations(n + k - 1, k))
            }
            "permutations" -> {
                val n = args[0].asDouble().toLong()
                val k = args.getOrNull(1)?.asDouble()?.toLong() ?: n
                MathValue.NumberVal(permutations(n, k))
            }
            "catalan" -> {
                val n = args[0].asDouble().toLong()
                MathValue.NumberVal(combinations(2 * n, n) / (n + 1))
            }
            "stirlingS2" -> {
                val n = args[0].asDouble().toInt()
                val k = args[1].asDouble().toInt()
                var sum = 0.0
                for (j in 0..k) {
                    val sign = if ((k - j) % 2 == 1) -1.0 else 1.0
                    sum += sign * combinations(k.toLong(), j.toLong()) * j.toDouble().pow(n)
                }
                MathValue.NumberVal(round(sum / factorial(k.toDouble())))
            }
            "bellNumbers" -> {
                val n = args[0].asDouble().toInt()
                var total = 0.0
                for (k in 0..n) {
                    total += (invokeBuiltIn("stirlingS2", listOf(MathValue.NumberVal(n.toDouble()), MathValue.NumberVal(k.toDouble())), evaluator) as MathValue.NumberVal).value
                }
                MathValue.NumberVal(total)
            }
            "random" -> {
                when (args.size) {
                    0 -> MathValue.NumberVal(Random.nextDouble())
                    1 -> MathValue.NumberVal(Random.nextDouble(0.0, args[0].asDouble()))
                    else -> MathValue.NumberVal(Random.nextDouble(args[0].asDouble(), args[1].asDouble()))
                }
            }
            "randomInt" -> {
                when (args.size) {
                    1 -> MathValue.NumberVal(Random.nextLong(0L, args[0].asDouble().toLong()).toDouble())
                    else -> MathValue.NumberVal(Random.nextLong(args[0].asDouble().toLong(), args[1].asDouble().toLong()).toDouble())
                }
            }
            "pickRandom" -> {
                val list = (args[0] as MathValue.MatrixVal).flatValues()
                if (list.isEmpty()) MathValue.NullVal else list[Random.nextInt(list.size)]
            }

            // Matrix & Vector Functions
            "matrix" -> args.firstOrNull() ?: MathValue.MatrixVal(emptyList())
            "det" -> MathValue.NumberVal(MatrixOps.det(args[0] as MathValue.MatrixVal))
            "inv" -> MatrixOps.inv(args[0] as MathValue.MatrixVal)
            "transpose" -> MatrixOps.transpose(args[0] as MathValue.MatrixVal)
            "ctranspose" -> MatrixOps.ctranspose(args[0] as MathValue.MatrixVal)
            "trace" -> MathValue.NumberVal(MatrixOps.trace(args[0] as MathValue.MatrixVal))
            "lusolve", "lsolve", "usolve" -> MatrixOps.lusolve(args[0] as MathValue.MatrixVal, args[1] as MathValue.MatrixVal)
            "dot" -> MathValue.NumberVal(MatrixOps.dot(args[0] as MathValue.MatrixVal, args[1] as MathValue.MatrixVal))
            "cross" -> MatrixOps.cross(args[0] as MathValue.MatrixVal, args[1] as MathValue.MatrixVal)
            "kron" -> MatrixOps.kron(args[0] as MathValue.MatrixVal, args[1] as MathValue.MatrixVal)
            "identity", "eye" -> {
                val r = args.getOrNull(0)?.asDouble()?.toInt() ?: 1
                val c = args.getOrNull(1)?.asDouble()?.toInt() ?: r
                MatrixOps.eye(r, c)
            }
            "zeros" -> {
                val r = args.getOrNull(0)?.asDouble()?.toInt() ?: 1
                val c = args.getOrNull(1)?.asDouble()?.toInt() ?: 1
                MatrixOps.zeros(r, c)
            }
            "ones" -> {
                val r = args.getOrNull(0)?.asDouble()?.toInt() ?: 1
                val c = args.getOrNull(1)?.asDouble()?.toInt() ?: 1
                MatrixOps.ones(r, c)
            }
            "diag" -> MatrixOps.diag(args[0] as MathValue.MatrixVal)
            "flatten" -> {
                val m = args[0] as MathValue.MatrixVal
                MathValue.MatrixVal.vector(m.flatValues())
            }
            "squeeze" -> {
                val m = args[0] as MathValue.MatrixVal
                val flat = m.flatValues()
                if (flat.size == 1) flat[0] else MathValue.MatrixVal.vector(flat)
            }
            "reshape" -> {
                val m = args[0] as MathValue.MatrixVal
                val sizes = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble().toInt() }
                MatrixOps.reshape(m, sizes)
            }
            "size" -> {
                when (val a = args[0]) {
                    is MathValue.MatrixVal -> {
                        if (a.isVector) MathValue.MatrixVal.vector(listOf(MathValue.NumberVal(a.colCount.toDouble())))
                        else MathValue.MatrixVal.vector(
                            listOf(
                                MathValue.NumberVal(a.rowCount.toDouble()),
                                MathValue.NumberVal(a.colCount.toDouble())
                            )
                        )
                    }
                    is MathValue.StringVal -> MathValue.MatrixVal.vector(listOf(MathValue.NumberVal(a.value.length.toDouble())))
                    else -> MathValue.MatrixVal.vector(emptyList())
                }
            }
            "count" -> {
                when (val a = args[0]) {
                    is MathValue.MatrixVal -> MathValue.NumberVal(a.flatValues().size.toDouble())
                    is MathValue.StringVal -> MathValue.NumberVal(a.value.length.toDouble())
                    else -> MathValue.NumberVal(1.0)
                }
            }
            "row" -> {
                val m = args[0] as MathValue.MatrixVal
                val idx = args[1].asDouble().toInt() - 1
                val r = m.rows.getOrElse(idx) { throw MathEvalException("Row index out of bounds") }
                MathValue.MatrixVal.vector(r)
            }
            "column" -> {
                val m = args[0] as MathValue.MatrixVal
                val idx = args[1].asDouble().toInt() - 1
                MathValue.MatrixVal(m.rows.map { listOf(it.getOrElse(idx) { throw MathEvalException("Column index out of bounds") }) })
            }
            "concat" -> {
                val a = args[0] as MathValue.MatrixVal
                val b = args[1] as MathValue.MatrixVal
                if (a.rowCount == b.rowCount) {
                    MathValue.MatrixVal(a.rows.indices.map { r -> a.rows[r] + b.rows[r] })
                } else {
                    MathValue.MatrixVal(a.rows + b.rows)
                }
            }
            "sort" -> {
                val m = args[0] as MathValue.MatrixVal
                val sorted = m.flatValues().sortedBy { it.asDouble() }
                MathValue.MatrixVal.vector(sorted)
            }
            "range" -> {
                val start = args[0].asDouble()
                val end = args[1].asDouble()
                val step = args.getOrNull(2)?.asDouble() ?: 1.0
                val list = mutableListOf<MathValue>()
                var cur = start
                if (step > 0) {
                    while (cur < end - 1e-12) {
                        list.add(MathValue.NumberVal(cur))
                        cur += step
                    }
                } else if (step < 0) {
                    while (cur > end + 1e-12) {
                        list.add(MathValue.NumberVal(cur))
                        cur += step
                    }
                }
                MathValue.MatrixVal.vector(list)
            }
            "map" -> {
                val m = args[0] as MathValue.MatrixVal
                val fn = args[1] as MathValue.FunctionVal
                MatrixOps.mapElementWise(m) { evaluator(fn, listOf(it)) }
            }
            "filter" -> {
                val m = args[0] as MathValue.MatrixVal
                val fn = args[1] as MathValue.FunctionVal
                MathValue.MatrixVal.vector(m.flatValues().filter { evaluator(fn, listOf(it)).asBoolean() })
            }
            "forEach" -> {
                val m = args[0] as MathValue.MatrixVal
                val fn = args[1] as MathValue.FunctionVal
                MathValue.MatrixVal.vector(m.flatValues().map { evaluator(fn, listOf(it)) })
            }
            "index" -> {
                if (args.size == 1) args[0] else MathValue.MatrixVal(args.map { listOf(it) })
            }
            "subset" -> {
                val m = args[0] as MathValue.MatrixVal
                val idxArg = args[1]
                if (idxArg is MathValue.MatrixVal && !idxArg.isVector && idxArg.rowCount == 2) {
                    val rIdx = idxArg.rows[0].first().asDouble().toInt() - 1
                    val cIdx = idxArg.rows[1].first().asDouble().toInt() - 1
                    m.rows[rIdx][cIdx]
                } else if (idxArg is MathValue.MatrixVal) {
                    val flat = m.flatValues()
                    MathValue.MatrixVal.vector(idxArg.flatValues().map { flat[it.asDouble().toInt() - 1] })
                } else {
                    val flat = m.flatValues()
                    flat[idxArg.asDouble().toInt() - 1]
                }
            }
            "matrixFromFunction" -> {
                val dims = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble().toInt() }
                val fn = args[1] as MathValue.FunctionVal
                val rows = dims.getOrElse(0) { 1 }
                val cols = dims.getOrElse(1) { 1 }
                MathValue.MatrixVal(
                    List(rows) { r ->
                        List(cols) { c ->
                            val idxVec = MathValue.MatrixVal.vector(
                                listOf(MathValue.NumberVal((r + 1).toDouble()), MathValue.NumberVal((c + 1).toDouble()))
                            )
                            evaluator(fn, listOf(idxVec))
                        }
                    }
                )
            }
            "lsolveAll", "usolveAll" -> {
                val sol = MatrixOps.lusolve(args[0] as MathValue.MatrixVal, args[1] as MathValue.MatrixVal)
                MathValue.MatrixVal(listOf(sol.flatValues()))
            }
            "solveODE" -> {
                val fn = args[0] as MathValue.FunctionVal
                val span = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                val y0 = args[2].asDouble()
                val t0 = span[0]
                val tEnd = span[1]
                val steps = 20
                val dt = (tEnd - t0) / steps
                var t = t0
                var y = y0
                repeat(steps) {
                    val k1 = evaluator(fn, listOf(MathValue.NumberVal(t), MathValue.NumberVal(y))).asDouble()
                    val k2 = evaluator(fn, listOf(MathValue.NumberVal(t + dt / 2), MathValue.NumberVal(y + dt * k1 / 2))).asDouble()
                    val k3 = evaluator(fn, listOf(MathValue.NumberVal(t + dt / 2), MathValue.NumberVal(y + dt * k2 / 2))).asDouble()
                    val k4 = evaluator(fn, listOf(MathValue.NumberVal(t + dt), MathValue.NumberVal(y + dt * k3))).asDouble()
                    y += (dt / 6.0) * (k1 + 2 * k2 + 2 * k3 + k4)
                    t += dt
                }
                MathValue.ObjectVal(
                    mapOf(
                        "t" to MathValue.NumberVal(tEnd),
                        "y" to MathValue.NumberVal(y)
                    )
                )
            }
            "config" -> MathValue.ObjectVal(
                mapOf(
                    "epsilon" to MathValue.NumberVal(1e-12),
                    "matrix" to MathValue.StringVal("Matrix"),
                    "number" to MathValue.StringVal("number"),
                    "precision" to MathValue.NumberVal(14.0)
                )
            )
            "help" -> {
                val topic = (args.firstOrNull() as? MathValue.StringVal)?.value ?: "math"
                MathValue.StringVal("Help for $topic")
            }

            // Set Operations
            "setUnion" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                MathValue.MatrixVal.vector((a + b).distinct().sorted().map { MathValue.NumberVal(it) })
            }
            "setIntersect" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                MathValue.MatrixVal.vector(a.intersect(b).sorted().map { MathValue.NumberVal(it) })
            }
            "setDifference" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                MathValue.MatrixVal.vector((a - b).sorted().map { MathValue.NumberVal(it) })
            }
            "setSymDifference" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                MathValue.MatrixVal.vector(((a - b) + (b - a)).sorted().map { MathValue.NumberVal(it) })
            }
            "setDistinct" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                MathValue.MatrixVal.vector(a.distinct().sorted().map { MathValue.NumberVal(it) })
            }
            "setIsSubset" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }.toSet()
                MathValue.BoolVal(b.containsAll(a))
            }

            // Geometry
            "distance" -> {
                val a = (args[0] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                val b = (args[1] as MathValue.MatrixVal).flatValues().map { it.asDouble() }
                if (a.size != b.size) throw MathEvalException("distance() points must have same dimensions")
                MathValue.NumberVal(sqrt(a.indices.sumOf { (a[it] - b[it]).pow(2) }))
            }

            // Bitwise Functions
            "bitAnd" -> MathValue.NumberVal((args[0].asDouble().toLong() and args[1].asDouble().toLong()).toDouble())
            "bitOr" -> MathValue.NumberVal((args[0].asDouble().toLong() or args[1].asDouble().toLong()).toDouble())
            "bitXor" -> MathValue.NumberVal((args[0].asDouble().toLong() xor args[1].asDouble().toLong()).toDouble())
            "bitNot" -> MathValue.NumberVal(args[0].asDouble().toLong().inv().toDouble())
            "leftShift" -> MathValue.NumberVal((args[0].asDouble().toLong() shl args[1].asDouble().toInt()).toDouble())
            "rightArithShift" -> MathValue.NumberVal((args[0].asDouble().toLong() shr args[1].asDouble().toInt()).toDouble())
            "rightLogShift" -> MathValue.NumberVal((args[0].asDouble().toInt() ushr args[1].asDouble().toInt()).toDouble())

            // Base & Formatting Conversions
            "hex" -> MathValue.StringVal("0x" + args[0].asDouble().toLong().toString(16))
            "bin" -> MathValue.StringVal("0b" + args[0].asDouble().toLong().toString(2))
            "oct" -> MathValue.StringVal("0o" + args[0].asDouble().toLong().toString(8))
            "format" -> MathValue.StringVal(args[0].format())
            "string" -> MathValue.StringVal(args[0].format())
            "number" -> {
                when (val a = args[0]) {
                    is MathValue.UnitVal -> {
                        if (args.size >= 2 && args[1] is MathValue.StringVal) {
                            val target = UnitRegistry.resolve((args[1] as MathValue.StringVal).value)
                                ?: throw MathEvalException("Unknown unit ${(args[1] as MathValue.StringVal).value}")
                            MathValue.NumberVal(a.unit.convertTo(a.value, target))
                        } else {
                            MathValue.NumberVal(a.value)
                        }
                    }
                    else -> MathValue.NumberVal(a.asDouble())
                }
            }
            "boolean" -> MathValue.BoolVal(args[0].asBoolean())
            "typeOf" -> MathValue.StringVal(args[0].typeName())
            "unit" -> {
                if (args.size == 1 && args[0] is MathValue.StringVal) {
                    val u = UnitRegistry.resolve((args[0] as MathValue.StringVal).value)
                        ?: throw MathEvalException("Unknown unit ${(args[0] as MathValue.StringVal).value}")
                    MathValue.UnitVal(1.0, u)
                } else {
                    val v = args[0].asDouble()
                    val uName = (args[1] as MathValue.StringVal).value
                    val u = UnitRegistry.resolve(uName) ?: throw MathEvalException("Unknown unit $uName")
                    MathValue.UnitVal(v, u)
                }
            }
            "to" -> {
                val uVal = args[0] as? MathValue.UnitVal ?: throw MathEvalException("First argument of to() must be a Unit")
                val target = when (val second = args[1]) {
                    is MathValue.UnitVal -> second.unit
                    is MathValue.StringVal -> UnitRegistry.resolve(second.value) ?: throw MathEvalException("Unknown unit ${second.value}")
                    else -> throw MathEvalException("Invalid target unit")
                }
                MathValue.UnitVal(uVal.unit.convertTo(uVal.value, target), target)
            }
            else -> throw MathEvalException("Unknown function: $name()")
        }
    }

    val BUILTIN_FUNCTIONS: Set<String> = setOf(
        "abs", "add", "subtract", "multiply", "divide", "mod", "pow",
        "dotMultiply", "dotDivide", "dotPow",
        "round", "floor", "ceil", "fix", "sign", "cbrt", "cube", "square", "sqrt", "nthRoot", "hypot", "norm",
        "exp", "expm1", "log", "ln", "log10", "log2", "log1p",
        "gcd", "lcm", "xgcd", "isPrime", "isInteger", "isPositive", "isNegative", "isZero", "isNaN", "isNumeric",
        "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
        "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
        "sec", "csc", "cot", "asec", "acsc", "acot", "sech", "csch", "coth", "asech", "acsch", "acoth",
        "deg", "rad", "complex", "re", "im", "conj", "arg",
        "sum", "prod", "mean", "avg", "median", "min", "max", "variance", "std", "mad", "mode", "cumsum", "diff", "corr", "quantileSeq",
        "factorial", "gamma", "lgamma", "erf", "combinations", "combinationsWithRep", "permutations", "catalan", "stirlingS2", "bellNumbers",
        "random", "randomInt", "pickRandom",
        "matrix", "det", "inv", "transpose", "ctranspose", "trace", "lusolve", "lsolve", "usolve", "dot", "cross", "kron",
        "identity", "eye", "zeros", "ones", "diag", "flatten", "squeeze", "reshape", "size", "count", "row", "column", "concat", "sort", "range", "map", "filter", "forEach", "index", "subset", "matrixFromFunction", "lsolveAll", "usolveAll", "solveODE", "config", "help",
        "setUnion", "setIntersect", "setDifference", "setSymDifference", "setDistinct", "setIsSubset",
        "distance",
        "bitAnd", "bitOr", "bitXor", "bitNot", "leftShift", "rightArithShift", "rightLogShift",
        "hex", "bin", "oct", "format", "string", "number", "boolean", "typeOf", "unit", "to"
    )
}
