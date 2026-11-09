package com.lucianmarin.num.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

enum class UnitDimension {
    LENGTH,
    MASS,
    TIME,
    CURRENT,
    TEMPERATURE,
    AMOUNT,
    LUMINOUS_INTENSITY,
    ANGLE,
    BIT
}

data class MathUnit(
    val dimensions: Map<UnitDimension, Int>,
    val scaleToBase: Double,
    val offsetToBase: Double = 0.0,
    val displayName: String
) {
    fun isCompatibleWith(other: MathUnit): Boolean = dimensions == other.dimensions

    fun toBase(value: Double): Double = (value + offsetToBase) * scaleToBase

    fun fromBase(baseValue: Double): Double = (baseValue / scaleToBase) - offsetToBase

    fun convertTo(value: Double, target: MathUnit): Double {
        if (!isCompatibleWith(target)) {
            throw MathEvalException("Cannot convert between incompatible units: ${formatName()} and ${target.formatName()}")
        }
        return target.fromBase(toBase(value))
    }

    fun multiply(other: MathUnit): MathUnit {
        val merged = dimensions.toMutableMap()
        for ((dim, exp) in other.dimensions) {
            val next = (merged[dim] ?: 0) + exp
            if (next == 0) merged.remove(dim) else merged[dim] = next
        }
        val combinedName = when {
            displayName.isEmpty() -> other.displayName
            other.displayName.isEmpty() -> displayName
            displayName == other.displayName -> "${displayName}^2"
            else -> "$displayName*${other.displayName}"
        }
        return MathUnit(
            dimensions = merged,
            scaleToBase = scaleToBase * other.scaleToBase,
            offsetToBase = 0.0,
            displayName = combinedName
        )
    }

    fun divide(other: MathUnit): MathUnit {
        val merged = dimensions.toMutableMap()
        for ((dim, exp) in other.dimensions) {
            val next = (merged[dim] ?: 0) - exp
            if (next == 0) merged.remove(dim) else merged[dim] = next
        }
        val combinedName = when {
            other.displayName.isEmpty() -> displayName
            displayName.isEmpty() -> "1/${other.displayName}"
            else -> "$displayName/${other.displayName}"
        }
        return MathUnit(
            dimensions = merged,
            scaleToBase = scaleToBase / other.scaleToBase,
            offsetToBase = 0.0,
            displayName = combinedName
        )
    }

    fun pow(exponent: Int): MathUnit {
        if (exponent == 0) return MathUnit(emptyMap(), 1.0, 0.0, "")
        val merged = dimensions.mapValues { (_, v) -> v * exponent }.filterValues { it != 0 }
        val name = if (exponent == 1) displayName else "${displayName}$exponent"
        return MathUnit(
            dimensions = merged,
            scaleToBase = scaleToBase.pow(exponent),
            offsetToBase = 0.0,
            displayName = name
        )
    }

    fun formatName(): String = displayName.ifEmpty {
        dimensions.entries.joinToString("*") { (d, e) ->
            val baseSymbol = when (d) {
                UnitDimension.LENGTH -> "m"
                UnitDimension.MASS -> "kg"
                UnitDimension.TIME -> "s"
                UnitDimension.CURRENT -> "A"
                UnitDimension.TEMPERATURE -> "K"
                UnitDimension.AMOUNT -> "mol"
                UnitDimension.LUMINOUS_INTENSITY -> "cd"
                UnitDimension.ANGLE -> "rad"
                UnitDimension.BIT -> "b"
            }
            if (e == 1) baseSymbol else "$baseSymbol^$e"
        }
    }
}

object UnitRegistry {
    private data class BaseUnitDef(
        val canonical: String,
        val aliases: List<String>,
        val dimensions: Map<UnitDimension, Int>,
        val scale: Double,
        val offset: Double = 0.0,
        val allowSiPrefixes: Boolean = false,
        val allowBinaryPrefixes: Boolean = false
    )

    private val SI_PREFIXES = listOf(
        "da" to 1e1,
        "Y" to 1e24,
        "Z" to 1e21,
        "E" to 1e18,
        "P" to 1e15,
        "T" to 1e12,
        "G" to 1e9,
        "M" to 1e6,
        "k" to 1e3,
        "h" to 1e2,
        "d" to 1e-1,
        "c" to 1e-2,
        "m" to 1e-3,
        "u" to 1e-6,
        "µ" to 1e-6,
        "n" to 1e-9,
        "p" to 1e-12,
        "f" to 1e-15,
        "a" to 1e-18,
        "z" to 1e-21,
        "y" to 1e-24
    )

    private val BINARY_PREFIXES = listOf(
        "Yi" to 2.0.pow(80),
        "Zi" to 2.0.pow(70),
        "Ei" to 2.0.pow(60),
        "Pi" to 2.0.pow(50),
        "Ti" to 2.0.pow(40),
        "Gi" to 2.0.pow(30),
        "Mi" to 2.0.pow(20),
        "Ki" to 2.0.pow(10)
    )

    private val exactUnits = mutableMapOf<String, MathUnit>()
    private val prefixableUnits = mutableListOf<BaseUnitDef>()

    init {
        val defs = listOf(
            // Length (base: m)
            BaseUnitDef("m", listOf("meter", "meters", "metre", "metres"), mapOf(UnitDimension.LENGTH to 1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("in", listOf("inch", "inches"), mapOf(UnitDimension.LENGTH to 1), 0.0254),
            BaseUnitDef("ft", listOf("foot", "feet"), mapOf(UnitDimension.LENGTH to 1), 0.3048),
            BaseUnitDef("yd", listOf("yard", "yards"), mapOf(UnitDimension.LENGTH to 1), 0.9144),
            BaseUnitDef("mi", listOf("mile", "miles"), mapOf(UnitDimension.LENGTH to 1), 1609.344),
            BaseUnitDef("nmi", listOf("nauticalmile"), mapOf(UnitDimension.LENGTH to 1), 1852.0),
            BaseUnitDef("angstrom", listOf("angstroms"), mapOf(UnitDimension.LENGTH to 1), 1e-10),
            BaseUnitDef("au", listOf("AU"), mapOf(UnitDimension.LENGTH to 1), 149597870700.0),
            BaseUnitDef("ly", listOf("lightyear", "lightyears"), mapOf(UnitDimension.LENGTH to 1), 9.4607304725808e15),

            // Area (base: m^2)
            BaseUnitDef("m2", listOf("sqm"), mapOf(UnitDimension.LENGTH to 2), 1.0),
            BaseUnitDef("cm2", listOf("sqcm"), mapOf(UnitDimension.LENGTH to 2), 1e-4),
            BaseUnitDef("mm2", listOf("sqmm"), mapOf(UnitDimension.LENGTH to 2), 1e-6),
            BaseUnitDef("km2", listOf("sqkm"), mapOf(UnitDimension.LENGTH to 2), 1e6),
            BaseUnitDef("sqft", listOf("ft2"), mapOf(UnitDimension.LENGTH to 2), 0.09290304),
            BaseUnitDef("sqin", listOf("in2"), mapOf(UnitDimension.LENGTH to 2), 0.00064516),
            BaseUnitDef("sqyd", listOf("yd2"), mapOf(UnitDimension.LENGTH to 2), 0.83612736),
            BaseUnitDef("sqmi", listOf("mi2"), mapOf(UnitDimension.LENGTH to 2), 2589988.110336),
            BaseUnitDef("acre", listOf("acres"), mapOf(UnitDimension.LENGTH to 2), 4046.8564224),
            BaseUnitDef("hectare", listOf("hectares", "ha"), mapOf(UnitDimension.LENGTH to 2), 10000.0),

            // Volume (base: m^3)
            BaseUnitDef("m3", listOf("cum"), mapOf(UnitDimension.LENGTH to 3), 1.0),
            BaseUnitDef("cm3", listOf("cc", "cucm"), mapOf(UnitDimension.LENGTH to 3), 1e-6),
            BaseUnitDef("mm3", listOf("cumm"), mapOf(UnitDimension.LENGTH to 3), 1e-9),
            BaseUnitDef("L", listOf("l", "liter", "liters", "litre", "litres"), mapOf(UnitDimension.LENGTH to 3), 1e-3, allowSiPrefixes = true),
            BaseUnitDef("gal", listOf("gallon", "gallons"), mapOf(UnitDimension.LENGTH to 3), 0.003785411784),
            BaseUnitDef("qt", listOf("quart", "quarts"), mapOf(UnitDimension.LENGTH to 3), 0.000946352946),
            BaseUnitDef("pt", listOf("pint", "pints"), mapOf(UnitDimension.LENGTH to 3), 0.000473176473),
            BaseUnitDef("cup", listOf("cups"), mapOf(UnitDimension.LENGTH to 3), 0.0002365882365),
            BaseUnitDef("floz", listOf("fluidounce", "fluidounces"), mapOf(UnitDimension.LENGTH to 3), 2.95735295625e-5),
            BaseUnitDef("tbsp", listOf("tablespoon", "tablespoons"), mapOf(UnitDimension.LENGTH to 3), 1.478676478125e-5),
            BaseUnitDef("tsp", listOf("teaspoon", "teaspoons"), mapOf(UnitDimension.LENGTH to 3), 4.92892159375e-6),

            // Mass (base: kg)
            BaseUnitDef("g", listOf("gram", "grams"), mapOf(UnitDimension.MASS to 1), 1e-3, allowSiPrefixes = true),
            BaseUnitDef("kg", listOf("kilogram", "kilograms"), mapOf(UnitDimension.MASS to 1), 1.0),
            BaseUnitDef("mg", listOf("milligram", "milligrams"), mapOf(UnitDimension.MASS to 1), 1e-6),
            BaseUnitDef("ug", listOf("microgram", "micrograms"), mapOf(UnitDimension.MASS to 1), 1e-9),
            BaseUnitDef("tonne", listOf("tonnes", "t", "metricton"), mapOf(UnitDimension.MASS to 1), 1000.0),
            BaseUnitDef("lb", listOf("lbs", "lbm", "pound", "pounds"), mapOf(UnitDimension.MASS to 1), 0.45359237),
            BaseUnitDef("oz", listOf("ounce", "ounces"), mapOf(UnitDimension.MASS to 1), 0.028349523125),
            BaseUnitDef("stone", listOf("stones"), mapOf(UnitDimension.MASS to 1), 6.35029318),
            BaseUnitDef("ton", listOf("tons"), mapOf(UnitDimension.MASS to 1), 907.18474),

            // Time (base: s)
            BaseUnitDef("s", listOf("sec", "secs", "second", "seconds"), mapOf(UnitDimension.TIME to 1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("min", listOf("mins", "minute", "minutes"), mapOf(UnitDimension.TIME to 1), 60.0),
            BaseUnitDef("h", listOf("hr", "hrs", "hour", "hours"), mapOf(UnitDimension.TIME to 1), 3600.0),
            BaseUnitDef("day", listOf("days", "d"), mapOf(UnitDimension.TIME to 1), 86400.0),
            BaseUnitDef("week", listOf("weeks", "wk"), mapOf(UnitDimension.TIME to 1), 604800.0),
            BaseUnitDef("month", listOf("months"), mapOf(UnitDimension.TIME to 1), 2629800.0),
            BaseUnitDef("year", listOf("years", "yr", "yrs"), mapOf(UnitDimension.TIME to 1), 31557600.0),

            // Frequency (base: s^-1)
            BaseUnitDef("Hz", listOf("hertz"), mapOf(UnitDimension.TIME to -1), 1.0, allowSiPrefixes = true),

            // Angle (base: rad)
            BaseUnitDef("rad", listOf("radian", "radians"), mapOf(UnitDimension.ANGLE to 1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("deg", listOf("degree", "degrees"), mapOf(UnitDimension.ANGLE to 1), PI / 180.0),
            BaseUnitDef("grad", listOf("gradian", "gradians"), mapOf(UnitDimension.ANGLE to 1), PI / 200.0),
            BaseUnitDef("arcmin", listOf("arcminute", "arcminutes"), mapOf(UnitDimension.ANGLE to 1), PI / 10800.0),
            BaseUnitDef("arcsec", listOf("arcsecond", "arcseconds"), mapOf(UnitDimension.ANGLE to 1), PI / 648000.0),

            // Temperature (base: K)
            BaseUnitDef("K", listOf("kelvin", "kelvins"), mapOf(UnitDimension.TEMPERATURE to 1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("degC", listOf("celsius"), mapOf(UnitDimension.TEMPERATURE to 1), 1.0, offset = 273.15),
            BaseUnitDef("degF", listOf("fahrenheit"), mapOf(UnitDimension.TEMPERATURE to 1), 5.0 / 9.0, offset = 459.67),
            BaseUnitDef("degR", listOf("rankine"), mapOf(UnitDimension.TEMPERATURE to 1), 5.0 / 9.0),

            // Electric current (base: A)
            BaseUnitDef("A", listOf("amp", "amps", "ampere", "amperes"), mapOf(UnitDimension.CURRENT to 1), 1.0, allowSiPrefixes = true),

            // Substance & luminous intensity
            BaseUnitDef("mol", listOf("mole", "moles"), mapOf(UnitDimension.AMOUNT to 1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("cd", listOf("candela"), mapOf(UnitDimension.LUMINOUS_INTENSITY to 1), 1.0, allowSiPrefixes = true),

            // Force (base: kg*m/s^2)
            BaseUnitDef("N", listOf("newton", "newtons"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 1, UnitDimension.TIME to -2), 1.0, allowSiPrefixes = true),
            BaseUnitDef("dyn", listOf("dyne"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 1, UnitDimension.TIME to -2), 1e-5),
            BaseUnitDef("lbf", listOf("poundforce"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 1, UnitDimension.TIME to -2), 4.4482216152605),
            BaseUnitDef("kip", listOf("kips"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 1, UnitDimension.TIME to -2), 4448.2216152605),

            // Energy (base: kg*m^2/s^2)
            BaseUnitDef("J", listOf("joule", "joules"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 1.0, allowSiPrefixes = true),
            BaseUnitDef("Wh", listOf(), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 3600.0, allowSiPrefixes = true),
            BaseUnitDef("kWh", listOf(), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 3.6e6),
            BaseUnitDef("cal", listOf("calorie", "calories"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 4.184, allowSiPrefixes = true),
            BaseUnitDef("kcal", listOf("Cal"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 4184.0),
            BaseUnitDef("BTU", listOf("btu"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 1055.05585262),
            BaseUnitDef("eV", listOf("electronvolt", "electronvolts"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 1.602176634e-19, allowSiPrefixes = true),
            BaseUnitDef("erg", listOf(), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2), 1e-7),

            // Power (base: kg*m^2/s^3)
            BaseUnitDef("W", listOf("watt", "watts"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -3), 1.0, allowSiPrefixes = true),
            BaseUnitDef("hp", listOf("horsepower"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -3), 745.6998715822702),

            // Pressure (base: kg/(m*s^2))
            BaseUnitDef("Pa", listOf("pascal", "pascals"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to -1, UnitDimension.TIME to -2), 1.0, allowSiPrefixes = true),
            BaseUnitDef("bar", listOf("bars"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to -1, UnitDimension.TIME to -2), 1e5, allowSiPrefixes = true),
            BaseUnitDef("atm", listOf("atmosphere", "atmospheres"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to -1, UnitDimension.TIME to -2), 101325.0),
            BaseUnitDef("torr", listOf("Torr"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to -1, UnitDimension.TIME to -2), 101325.0 / 760.0),
            BaseUnitDef("mmHg", listOf(), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to -1, UnitDimension.TIME to -2), 133.322387415),
            BaseUnitDef("psi", listOf(), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to -1, UnitDimension.TIME to -2), 6894.757293168361),

            // Speed (base: m/s)
            BaseUnitDef("mph", listOf(), mapOf(UnitDimension.LENGTH to 1, UnitDimension.TIME to -1), 0.44704),
            BaseUnitDef("kph", listOf("kmh"), mapOf(UnitDimension.LENGTH to 1, UnitDimension.TIME to -1), 1.0 / 3.6),
            BaseUnitDef("knot", listOf("knots", "kt", "kts"), mapOf(UnitDimension.LENGTH to 1, UnitDimension.TIME to -1), 1852.0 / 3600.0),

            // Electromagnetism
            BaseUnitDef("C", listOf("coulomb", "coulombs"), mapOf(UnitDimension.CURRENT to 1, UnitDimension.TIME to 1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("V", listOf("volt", "volts"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -3, UnitDimension.CURRENT to -1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("ohm", listOf("ohms"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -3, UnitDimension.CURRENT to -2), 1.0, allowSiPrefixes = true),
            BaseUnitDef("F", listOf("farad", "farads"), mapOf(UnitDimension.MASS to -1, UnitDimension.LENGTH to -2, UnitDimension.TIME to 4, UnitDimension.CURRENT to 2), 1.0, allowSiPrefixes = true),
            BaseUnitDef("S", listOf("siemens"), mapOf(UnitDimension.MASS to -1, UnitDimension.LENGTH to -2, UnitDimension.TIME to 3, UnitDimension.CURRENT to 2), 1.0, allowSiPrefixes = true),
            BaseUnitDef("Wb", listOf("weber", "webers"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2, UnitDimension.CURRENT to -1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("T", listOf("tesla", "teslas"), mapOf(UnitDimension.MASS to 1, UnitDimension.TIME to -2, UnitDimension.CURRENT to -1), 1.0, allowSiPrefixes = true),
            BaseUnitDef("H", listOf("henry", "henrys"), mapOf(UnitDimension.MASS to 1, UnitDimension.LENGTH to 2, UnitDimension.TIME to -2, UnitDimension.CURRENT to -2), 1.0, allowSiPrefixes = true),

            // Digital Storage (base: bit)
            BaseUnitDef("b", listOf("bit", "bits"), mapOf(UnitDimension.BIT to 1), 1.0, allowSiPrefixes = true, allowBinaryPrefixes = true),
            BaseUnitDef("B", listOf("byte", "bytes"), mapOf(UnitDimension.BIT to 1), 8.0, allowSiPrefixes = true, allowBinaryPrefixes = true)
        )

        for (def in defs) {
            val baseUnit = MathUnit(def.dimensions, def.scale, def.offset, def.canonical)
            exactUnits[def.canonical] = baseUnit
            for (alias in def.aliases) {
                exactUnits[alias] = MathUnit(def.dimensions, def.scale, def.offset, alias)
            }
            if (def.allowSiPrefixes || def.allowBinaryPrefixes) {
                prefixableUnits.add(def)
            }
        }
    }

    fun resolve(token: String): MathUnit? {
        exactUnits[token]?.let { return it }

        // Check power suffix like m^2, s^2, m2, s2
        val powMatch = Regex("^([a-zA-Zµ]+)\\^?(-?\\d+)$").matchEntire(token)
        if (powMatch != null) {
            val baseStr = powMatch.groupValues[1]
            val exp = powMatch.groupValues[2].toIntOrNull()
            if (exp != null) {
                val baseUnit = resolveSimpleOrPrefixed(baseStr)
                if (baseUnit != null) {
                    return baseUnit.pow(exp)
                }
            }
        }

        return resolveSimpleOrPrefixed(token)
    }

    private fun resolveSimpleOrPrefixed(token: String): MathUnit? {
        exactUnits[token]?.let { return it }

        for (def in prefixableUnits) {
            val namesToTry = listOf(def.canonical) + def.aliases
            if (def.allowBinaryPrefixes) {
                for ((prefix, factor) in BINARY_PREFIXES) {
                    for (uName in namesToTry) {
                        if (token == "$prefix$uName") {
                            return MathUnit(def.dimensions, def.scale * factor, 0.0, token)
                        }
                    }
                }
            }
            if (def.allowSiPrefixes) {
                for ((prefix, factor) in SI_PREFIXES) {
                    for (uName in namesToTry) {
                        if (token == "$prefix$uName") {
                            return MathUnit(def.dimensions, def.scale * factor, 0.0, token)
                        }
                    }
                }
            }
        }
        return null
    }

    fun isKnownUnit(token: String): Boolean = resolve(token) != null
}
