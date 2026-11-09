package com.lucianmarin.num.math

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MatrixOps {
    fun transpose(m: MathValue.MatrixVal): MathValue.MatrixVal {
        if (m.rows.isEmpty()) return m
        val rCount = m.rowCount
        val cCount = m.colCount
        val out = List(cCount) { c ->
            List(rCount) { r ->
                m.rows[r][c]
            }
        }
        return MathValue.MatrixVal(out)
    }

    fun ctranspose(m: MathValue.MatrixVal): MathValue.MatrixVal {
        if (m.rows.isEmpty()) return m
        val rCount = m.rowCount
        val cCount = m.colCount
        val out = List(cCount) { c ->
            List(rCount) { r ->
                val v = m.rows[r][c]
                if (v is MathValue.ComplexVal) ComplexOps.conj(v) else v
            }
        }
        return MathValue.MatrixVal(out)
    }

    fun mapElementWise(m: MathValue.MatrixVal, op: (MathValue) -> MathValue): MathValue.MatrixVal {
        return MathValue.MatrixVal(m.rows.map { row -> row.map(op) })
    }

    fun zipElementWise(
        a: MathValue,
        b: MathValue,
        op: (MathValue, MathValue) -> MathValue
    ): MathValue {
        return when {
            a is MathValue.MatrixVal && b is MathValue.MatrixVal -> {
                if (a.rowCount != b.rowCount || a.colCount != b.colCount) {
                    throw MathEvalException("Matrix dimension mismatch: [${a.rowCount}, ${a.colCount}] vs [${b.rowCount}, ${b.colCount}]")
                }
                MathValue.MatrixVal(
                    List(a.rowCount) { r ->
                        List(a.colCount) { c ->
                            op(a.rows[r][c], b.rows[r][c])
                        }
                    }
                )
            }
            a is MathValue.MatrixVal -> mapElementWise(a) { op(it, b) }
            b is MathValue.MatrixVal -> mapElementWise(b) { op(a, it) }
            else -> op(a, b)
        }
    }

    fun multiply(
        a: MathValue.MatrixVal,
        b: MathValue.MatrixVal,
        addOp: (MathValue, MathValue) -> MathValue,
        mulOp: (MathValue, MathValue) -> MathValue
    ): MathValue {
        // 1xN * 1xN dot product or matrix multiply
        if (a.rowCount == 1 && b.rowCount == 1 && a.colCount == b.colCount) {
            // Vector dot product if both are 1xN row vectors in MathJS [1,2,3] * [4,5,6] -> 32
            var sum: MathValue = MathValue.NumberVal(0.0)
            for (i in 0 until a.colCount) {
                sum = addOp(sum, mulOp(a.rows[0][i], b.rows[0][i]))
            }
            return sum
        }
        // Matrix A (m x n) * vector b (1 x n) treated as column vector (n x 1) -> 1 x m vector
        if (b.rowCount == 1 && a.colCount == b.colCount && a.rowCount > 1) {
            val res = List(a.rowCount) { r ->
                var sum: MathValue = MathValue.NumberVal(0.0)
                for (k in 0 until a.colCount) {
                    sum = addOp(sum, mulOp(a.rows[r][k], b.rows[0][k]))
                }
                sum
            }
            return MathValue.MatrixVal.vector(res)
        }
        if (a.colCount != b.rowCount) {
            throw MathEvalException("Cannot multiply matrices of dimensions [${a.rowCount}, ${a.colCount}] and [${b.rowCount}, ${b.colCount}]")
        }
        val m = a.rowCount
        val kMax = a.colCount
        val n = b.colCount
        val out = List(m) { r ->
            List(n) { c ->
                var sum: MathValue = MathValue.NumberVal(0.0)
                for (k in 0 until kMax) {
                    sum = addOp(sum, mulOp(a.rows[r][k], b.rows[k][c]))
                }
                sum
            }
        }
        return MathValue.MatrixVal(out)
    }

    fun det(m: MathValue.MatrixVal): Double {
        if (m.rowCount != m.colCount) throw MathEvalException("Matrix must be square to compute determinant")
        val n = m.rowCount
        if (n == 0) return 1.0
        if (n == 1) return m.rows[0][0].asDouble()
        if (n == 2) {
            return m.rows[0][0].asDouble() * m.rows[1][1].asDouble() -
                m.rows[0][1].asDouble() * m.rows[1][0].asDouble()
        }
        val a = m.toDoubleMatrix()
        var det = 1.0
        for (i in 0 until n) {
            var pivot = i
            for (r in i + 1 until n) {
                if (abs(a[r][i]) > abs(a[pivot][i])) pivot = r
            }
            if (abs(a[pivot][i]) < 1e-15) return 0.0
            if (pivot != i) {
                val tmp = a[i]
                a[i] = a[pivot]
                a[pivot] = tmp
                det = -det
            }
            det *= a[i][i]
            for (r in i + 1 until n) {
                val factor = a[r][i] / a[i][i]
                for (c in i + 1 until n) {
                    a[r][c] -= factor * a[i][c]
                }
            }
        }
        return det
    }

    fun inv(m: MathValue.MatrixVal): MathValue.MatrixVal {
        if (m.rowCount != m.colCount) throw MathEvalException("Matrix must be square to invert")
        val n = m.rowCount
        val a = Array(n) { r ->
            DoubleArray(2 * n) { c ->
                if (c < n) m.rows[r][c].asDouble()
                else if (c - n == r) 1.0 else 0.0
            }
        }
        for (i in 0 until n) {
            var pivot = i
            for (r in i + 1 until n) {
                if (abs(a[r][i]) > abs(a[pivot][i])) pivot = r
            }
            if (abs(a[pivot][i]) < 1e-14) throw MathEvalException("Cannot invert singular matrix")
            if (pivot != i) {
                val tmp = a[i]
                a[i] = a[pivot]
                a[pivot] = tmp
            }
            val div = a[i][i]
            for (c in 0 until 2 * n) a[i][c] /= div
            for (r in 0 until n) {
                if (r != i) {
                    val factor = a[r][i]
                    for (c in 0 until 2 * n) {
                        a[r][c] -= factor * a[i][c]
                    }
                }
            }
        }
        val invRows = List(n) { r ->
            List(n) { c ->
                MathValue.NumberVal(a[r][c + n])
            }
        }
        return MathValue.MatrixVal(invRows)
    }

    fun trace(m: MathValue.MatrixVal): Double {
        if (m.rowCount != m.colCount) throw MathEvalException("Matrix must be square for trace")
        var sum = 0.0
        for (i in 0 until m.rowCount) {
            sum += m.rows[i][i].asDouble()
        }
        return sum
    }

    fun lusolve(aMat: MathValue.MatrixVal, bMat: MathValue.MatrixVal): MathValue.MatrixVal {
        val n = aMat.rowCount
        if (n != aMat.colCount) throw MathEvalException("Matrix A must be square")
        val bVec = bMat.flatValues().map { it.asDouble() }.toDoubleArray()
        if (bVec.size != n) throw MathEvalException("Dimension mismatch in lusolve")
        val aug = Array(n) { r ->
            DoubleArray(n + 1) { c ->
                if (c < n) aMat.rows[r][c].asDouble() else bVec[r]
            }
        }
        for (i in 0 until n) {
            var pivot = i
            for (r in i + 1 until n) {
                if (abs(aug[r][i]) > abs(aug[pivot][i])) pivot = r
            }
            if (abs(aug[pivot][i]) < 1e-14) throw MathEvalException("Linear system is singular")
            if (pivot != i) {
                val tmp = aug[i]
                aug[i] = aug[pivot]
                aug[pivot] = tmp
            }
            for (r in i + 1 until n) {
                val factor = aug[r][i] / aug[i][i]
                for (c in i until n + 1) {
                    aug[r][c] -= factor * aug[i][c]
                }
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = aug[i][n]
            for (j in i + 1 until n) sum -= aug[i][j] * x[j]
            x[i] = sum / aug[i][i]
        }
        return MathValue.MatrixVal(x.map { listOf(MathValue.NumberVal(it)) })
    }

    fun eye(rows: Int, cols: Int = rows): MathValue.MatrixVal {
        return MathValue.MatrixVal(
            List(rows) { r ->
                List(cols) { c ->
                    MathValue.NumberVal(if (r == c) 1.0 else 0.0)
                }
            }
        )
    }

    fun zeros(rows: Int, cols: Int = 1): MathValue.MatrixVal {
        return if (cols == 1) {
            MathValue.MatrixVal.vector(List(rows) { MathValue.NumberVal(0.0) })
        } else {
            MathValue.MatrixVal(List(rows) { List(cols) { MathValue.NumberVal(0.0) } })
        }
    }

    fun ones(rows: Int, cols: Int = 1): MathValue.MatrixVal {
        return if (cols == 1) {
            MathValue.MatrixVal.vector(List(rows) { MathValue.NumberVal(1.0) })
        } else {
            MathValue.MatrixVal(List(rows) { List(cols) { MathValue.NumberVal(1.0) } })
        }
    }

    fun diag(m: MathValue.MatrixVal): MathValue.MatrixVal {
        return if (m.isVector) {
            val items = m.rows[0]
            val n = items.size
            MathValue.MatrixVal(
                List(n) { r ->
                    List(n) { c ->
                        if (r == c) items[r] else MathValue.NumberVal(0.0)
                    }
                }
            )
        } else {
            val n = min(m.rowCount, m.colCount)
            MathValue.MatrixVal.vector(List(n) { i -> m.rows[i][i] })
        }
    }

    fun dot(a: MathValue.MatrixVal, b: MathValue.MatrixVal): Double {
        val av = a.flatValues().map { it.asDouble() }
        val bv = b.flatValues().map { it.asDouble() }
        if (av.size != bv.size) throw MathEvalException("Vectors must have equal length for dot product")
        return av.indices.sumOf { av[it] * bv[it] }
    }

    fun cross(a: MathValue.MatrixVal, b: MathValue.MatrixVal): MathValue.MatrixVal {
        val av = a.flatValues().map { it.asDouble() }
        val bv = b.flatValues().map { it.asDouble() }
        if (av.size != 3 || bv.size != 3) throw MathEvalException("cross() requires 3D vectors")
        return MathValue.MatrixVal.vector(
            listOf(
                MathValue.NumberVal(av[1] * bv[2] - av[2] * bv[1]),
                MathValue.NumberVal(av[2] * bv[0] - av[0] * bv[2]),
                MathValue.NumberVal(av[0] * bv[1] - av[1] * bv[0])
            )
        )
    }

    fun norm(m: MathValue.MatrixVal): Double {
        val vals = m.flatValues().map { it.asDouble() }
        return sqrt(vals.sumOf { it * it })
    }

    fun kron(a: MathValue.MatrixVal, b: MathValue.MatrixVal): MathValue.MatrixVal {
        val out = mutableListOf<List<MathValue>>()
        for (ar in a.rows) {
            for (br in b.rows) {
                val row = mutableListOf<MathValue>()
                for (av in ar) {
                    for (bv in br) {
                        row.add(MathValue.NumberVal(av.asDouble() * bv.asDouble()))
                    }
                }
                out.add(row)
            }
        }
        return MathValue.MatrixVal(out)
    }

    fun reshape(m: MathValue.MatrixVal, sizes: List<Int>): MathValue.MatrixVal {
        val flat = m.flatValues()
        if (sizes.size == 1) {
            if (sizes[0] != flat.size) throw MathEvalException("Cannot reshape ${flat.size} elements to [${sizes[0]}]")
            return MathValue.MatrixVal.vector(flat)
        }
        val r = sizes[0]
        val c = sizes[1]
        if (r * c != flat.size) throw MathEvalException("Cannot reshape ${flat.size} elements to [$r, $c]")
        return MathValue.MatrixVal(
            List(r) { rowIdx ->
                List(c) { colIdx ->
                    flat[rowIdx * c + colIdx]
                }
            }
        )
    }
}
