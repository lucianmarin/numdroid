package com.lucianmarin.num.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEngineTest {

    private fun eval(expr: String, scope: MathScope = MathScope()): String? {
        val res = MathEngine.evaluateSingleLine(expr, scope)
        assertNull("Expected no error for '$expr', but got: ${res.error}", res.error)
        return res.formatted
    }

    @Test
    fun testArithmeticAndPrecedence() {
        assertEquals("123.75", eval("(12 + 8) * 3 / 4 ^ 2 + 5!"))
        assertEquals("1024", eval("2 ^ 10"))
        assertEquals("2", eval("17 % 5"))
        assertEquals("720", eval("6!"))
        assertEquals("4.2", eval("abs(-4.2)"))
        assertEquals("12", eval("gcd(24, 36)"))
        assertEquals("12", eval("lcm(4, 6)"))
        assertEquals("5", eval("hypot(3, 4)"))
        assertEquals("3.1416", eval("round(pi, 4)"))
    }

    @Test
    fun testVariablesFunctionsAndChaining() {
        val lines = listOf(
            "x = 10",
            "f(a, b) = a^2 + b^2",
            "f(x, 5)",
            "ans * 2"
        )
        val (results, _) = MathEngine.evaluateLines(lines)
        assertEquals("10", results[0].formatted)
        assertEquals("f(a, b)", results[1].formatted)
        assertEquals("125", results[2].formatted)
        assertEquals("250", results[3].formatted)

        assertEquals("297.5", eval("tax = 19%; price = 250; price * (1 + tax)"))
    }

    @Test
    fun testComplexNumbers() {
        assertEquals("11 + 10i", eval("(2 + 3i) * (4 - i)"))
        assertEquals("-1", eval("i ^ 2"))
        assertEquals("2", eval("re(2 + 3i)"))
        assertEquals("3", eval("im(2 + 3i)"))
        assertEquals("2 - 3i", eval("conj(2 + 3i)"))
        assertEquals("5", eval("abs(3 - 4i)"))
    }

    @Test
    fun testMatricesVectorsAndLinearAlgebra() {
        assertEquals("[4, 10, 18]", eval("[1, 2, 3] .* [4, 5, 6]"))
        assertEquals("[20, 30, 40]", eval("[10, 20, 30, 40, 50][2:end-1]"))
        assertEquals("-2", eval("det([1, 2; 3, 4])"))
        assertEquals("5", eval("trace([1, 2; 3, 4])"))
        assertEquals("32", eval("dot([1, 2, 3], [4, 5, 6])"))
        assertEquals("[1, -1, 1]", eval("cross([1, 1, 0], [0, 1, 1])"))
        assertEquals("[2; 5]", eval("lusolve([-2, 3; 2, 1], [11, 9])"))
        assertEquals("[1, 4; 2, 5; 3, 6]", eval("[1, 2, 3; 4, 5, 6]'"))
    }

    @Test
    fun testUnitsAndConversions() {
        assertEquals("27.777777777778 m/s", eval("100 km/h in m/s"))
        assertEquals("37 degC", eval("98.6 degF in degC"))
        assertEquals("212 degF", eval("100 degC in degF"))
        assertEquals("328.08398950131 ft", eval("100 m in ft"))
        assertEquals("2.4710538146717 acre", eval("1 hectare in acre"))
        assertEquals("3.785411784 L", eval("1 gal in L"))
        assertEquals("2.2046226218488 lb", eval("1 kg in lb"))
        assertEquals("86400 seconds", eval("1 day in seconds"))
        assertEquals("745.69987158227 W", eval("1 hp in W"))
        assertEquals("14.695948775513 psi", eval("1 atm in psi"))
        assertEquals("4700 ohm", eval("4.7 kohm in ohm"))
        assertEquals("5368.70912 MB", eval("5 GiB in MB"))
        assertEquals("0.5", eval("cos(60 deg)"))
    }

    @Test
    fun testBitwiseRadixesObjectsAndStrings() {
        assertEquals("24", eval("(0b1010 & 0b1100) | (1 << 4)"))
        assertEquals("328", eval("0xff + 0b1010 + 0o77"))
        assertEquals("5", eval("pt = {x: 3, y: 4}; sqrt(pt.x^2 + pt.y^2)"))
        assertEquals("\"Math\"", eval("\"Mathematical\"[1:4]"))
    }

    @Test
    fun testAllSyntaxCatalogExamplesEvaluateWithoutError() {
        var testedCount = 0
        for (group in SyntaxCatalog.groups) {
            for (item in group.items) {
                val scope = MathScope()
                scope.variables["ans"] = MathValue.NumberVal(21.0)
                val result = MathEngine.evaluateSingleLine(item.expr, scope)
                assertNull(
                    "Catalog item '${item.name}' (${item.expr}) failed with error: ${result.error}",
                    result.error
                )
                assertNotNull(
                    "Catalog item '${item.name}' (${item.expr}) returned null value",
                    result.formatted
                )
                testedCount++
            }
        }
        assertTrue("Expected at least 100 catalog items tested, got $testedCount", testedCount >= 100)
    }
}
