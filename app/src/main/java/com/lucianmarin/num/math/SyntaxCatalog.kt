package com.lucianmarin.num.math

data class SyntaxCatalogItem(
    val name: String,
    val syntax: String,
    val expr: String,
    val desc: String,
    val subcategory: String
)

data class SyntaxCatalogGroup(
    val category: String,
    val items: List<SyntaxCatalogItem>
)

object SyntaxCatalog {
    val groups: List<SyntaxCatalogGroup> = listOf(
        SyntaxCatalogGroup(
            category = "Basic",
            items = listOf(
                // Syntax & Operators
                SyntaxCatalogItem(
                    name = "Arithmetic Operators",
                    syntax = "+  -  *  /  ^  %  !",
                    expr = "(12 + 8) * 3 / 4 ^ 2 + 5!",
                    desc = "Addition, subtraction, multiplication, division, power, modulus, factorial.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Variables & Assignment",
                    syntax = "name = expr",
                    expr = "tax = 19%; price = 250; price * (1 + tax)",
                    desc = "Assign variables and chain multiple statements with ;.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Custom Functions",
                    syntax = "f(x, y) = expr",
                    expr = "hypot2(a, b) = sqrt(a^2 + b^2); hypot2(3, 4)",
                    desc = "Define reusable single- or multi-parameter functions.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Previous Result (ans)",
                    syntax = "ans",
                    expr = "ans * 2",
                    desc = "Reference the evaluated result from the previous calculation.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Conditional Operator (Ternary)",
                    syntax = "condition ? trueExpr : falseExpr",
                    expr = "15 > 10 ? 100 : 0",
                    desc = "Evaluate branches conditionally based on a boolean expression.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Relational Operators",
                    syntax = "==  !=  <  >  <=  >=",
                    expr = "2^10 >= 1000",
                    desc = "Compare numbers, strings, units, or matrices.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Logical Operators",
                    syntax = "and  or  xor  not",
                    expr = "(5 > 3) and not(2 == 4)",
                    desc = "Boolean conjunction, disjunction, exclusive OR, and negation.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Ranges & Sequences",
                    syntax = "start:end  or  start:step:end",
                    expr = "0:2:10",
                    desc = "Generate numeric ranges with optional step size.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Comments",
                    syntax = "# comment  or  // comment",
                    expr = "42 * 2 // inline comment",
                    desc = "Full-line or inline comments using # or //.",
                    subcategory = "Syntax & Operators"
                ),

                // Arithmetic
                SyntaxCatalogItem("abs", "abs(x)", "abs(-4.2)", "Calculate the absolute value of a number, complex, or array.", "Arithmetic"),
                SyntaxCatalogItem("add", "x + y  |  add(x, y)", "2.1 + 3.6", "Add two or more values together.", "Arithmetic"),
                SyntaxCatalogItem("cbrt", "cbrt(x)", "cbrt(64)", "Calculate the cubic root of a value.", "Arithmetic"),
                SyntaxCatalogItem("ceil", "ceil(x)", "ceil(4.2)", "Round a value towards plus infinity.", "Arithmetic"),
                SyntaxCatalogItem("cube", "cube(x)", "cube(3)", "Compute the cube of a value (x * x * x).", "Arithmetic"),
                SyntaxCatalogItem("divide", "x / y  |  divide(x, y)", "4.5 / 2", "Divide two values.", "Arithmetic"),
                SyntaxCatalogItem("exp", "exp(x)", "exp(2)", "Calculate the exponent of a value (e^x).", "Arithmetic"),
                SyntaxCatalogItem("expm1", "expm1(x)", "expm1(1)", "Calculate the value of exp(x) - 1.", "Arithmetic"),
                SyntaxCatalogItem("fix", "fix(x)", "fix(-3.7)", "Round a value towards zero.", "Arithmetic"),
                SyntaxCatalogItem("floor", "floor(x)", "floor(4.8)", "Round a value towards minus infinity.", "Arithmetic"),
                SyntaxCatalogItem("gcd", "gcd(a, b)", "gcd(24, 36)", "Calculate the greatest common divisor of two or more integers.", "Arithmetic"),
                SyntaxCatalogItem("hypot", "hypot(a, b, ...)", "hypot(3, 4)", "Calculate the hypotenuse of a list of numbers.", "Arithmetic"),
                SyntaxCatalogItem("lcm", "lcm(a, b)", "lcm(4, 6)", "Calculate the least common multiple of two or more integers.", "Arithmetic"),
                SyntaxCatalogItem("ln", "ln(x)", "ln(e^3)", "Calculate the natural logarithm of a value.", "Arithmetic"),
                SyntaxCatalogItem("log", "log(x)  |  log(x, base)", "log(1000, 10)", "Calculate the logarithm of a value with optional base.", "Arithmetic"),
                SyntaxCatalogItem("log10", "log10(x)", "log10(10000)", "Calculate the 10-base logarithm of a value.", "Arithmetic"),
                SyntaxCatalogItem("log1p", "log1p(x)", "log1p(1)", "Calculate the natural logarithm of x + 1.", "Arithmetic"),
                SyntaxCatalogItem("log2", "log2(x)", "log2(1024)", "Calculate the 2-base logarithm of a value.", "Arithmetic"),
                SyntaxCatalogItem("mod", "x % y  |  mod(x, y)", "17 % 5", "Calculate the modulus (remainder of integer division).", "Arithmetic"),
                SyntaxCatalogItem("multiply", "x * y  |  multiply(x, y)", "2.1 * 3.4", "Multiply two or more values.", "Arithmetic"),
                SyntaxCatalogItem("norm", "norm(x)", "norm([3, 4])", "Calculate the Euclidean norm of a vector or value.", "Arithmetic"),
                SyntaxCatalogItem("nthRoot", "nthRoot(a, root)", "nthRoot(81, 4)", "Calculate the nth root of a value.", "Arithmetic"),
                SyntaxCatalogItem("pow", "x ^ y  |  pow(x, y)", "2 ^ 10", "Calculates the power of x to y.", "Arithmetic"),
                SyntaxCatalogItem("round", "round(x)  |  round(x, n)", "round(pi, 4)", "Round a value towards the nearest integer or n decimals.", "Arithmetic"),
                SyntaxCatalogItem("sign", "sign(x)", "sign(-7)", "Compute the sign of a value (-1, 0, or 1).", "Arithmetic"),
                SyntaxCatalogItem("sqrt", "sqrt(x)", "sqrt(25)", "Calculate the principal square root of a value.", "Arithmetic"),
                SyntaxCatalogItem("square", "square(x)", "square(9)", "Compute the square of a value (x * x).", "Arithmetic"),
                SyntaxCatalogItem("subtract", "x - y  |  subtract(x, y)", "5.3 - 2", "Subtract two values.", "Arithmetic"),
                SyntaxCatalogItem("xgcd", "xgcd(a, b)", "xgcd(8, 12)", "Calculate the extended greatest common divisor [gcd, x, y].", "Arithmetic"),

                // Constants
                SyntaxCatalogItem("pi", "pi  |  PI", "2 * pi * 5", "The number pi (3.141592653589793).", "Constants"),
                SyntaxCatalogItem("e", "e  |  E", "e ^ 2", "Euler's number, the base of the natural logarithm.", "Constants"),
                SyntaxCatalogItem("tau", "tau", "tau / 4", "The circle constant tau (2 * pi).", "Constants"),
                SyntaxCatalogItem("phi", "phi", "phi ^ 2 - phi", "The golden ratio (1.618033988749895).", "Constants"),
                SyntaxCatalogItem("i", "i", "i ^ 2", "Imaginary unit, defined as i*i = -1.", "Constants"),
                SyntaxCatalogItem("Infinity", "Infinity", "1 / 0", "Positive infinity constant.", "Constants"),
                SyntaxCatalogItem("LN2", "LN2", "LN2", "Returns the natural logarithm of 2 (~0.693).", "Constants"),
                SyntaxCatalogItem("LN10", "LN10", "LN10", "Returns the natural logarithm of 10 (~2.302).", "Constants"),
                SyntaxCatalogItem("SQRT2", "SQRT2", "SQRT2", "Returns the square root of 2 (~1.414).", "Constants")
            )
        ),
        SyntaxCatalogGroup(
            category = "Advanced",
            items = listOf(
                SyntaxCatalogItem(
                    name = "Matrices & Vectors",
                    syntax = "[a, b; c, d]",
                    expr = "[1, 2, 3; 4, 5, 6]",
                    desc = "Construct row vectors, column vectors, and 2D matrices.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Matrix Transpose",
                    syntax = "A'",
                    expr = "[1, 2, 3; 4, 5, 6]'",
                    desc = "Conjugate transpose of a matrix or vector.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Element-wise Operators",
                    syntax = ".*  ./  .^",
                    expr = "[1, 2, 3] .* [4, 5, 6]",
                    desc = "Element-wise matrix multiplication, division, and exponentiation.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Indexing & Slicing (1-based)",
                    syntax = "A[row, col]  or  A[1:end]",
                    expr = "[10, 20, 30, 40, 50][2:end-1]",
                    desc = "1-based indexing, submatrix ranges, and end keyword.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Complex Numbers",
                    syntax = "a + bi",
                    expr = "(2 + 3i) * (4 - i)",
                    desc = "Complex arithmetic with imaginary unit i.",
                    subcategory = "Syntax & Operators"
                ),

                // Trigonometry
                SyntaxCatalogItem("sin", "sin(x)", "sin(pi / 2)", "Calculate the sine of a value in radians or angle units.", "Trigonometry"),
                SyntaxCatalogItem("cos", "cos(x)", "cos(60 deg)", "Calculate the cosine of a value in radians or angle units.", "Trigonometry"),
                SyntaxCatalogItem("tan", "tan(x)", "tan(45 deg)", "Calculate the tangent of a value.", "Trigonometry"),
                SyntaxCatalogItem("asin", "asin(x)", "asin(0.5)", "Calculate the inverse sine in radians.", "Trigonometry"),
                SyntaxCatalogItem("acos", "acos(x)", "acos(0.5)", "Calculate the inverse cosine in radians.", "Trigonometry"),
                SyntaxCatalogItem("atan", "atan(x)", "atan(1)", "Calculate the inverse tangent in radians.", "Trigonometry"),
                SyntaxCatalogItem("atan2", "atan2(y, x)", "atan2(1, 1)", "Calculate the inverse tangent of y/x with quadrant.", "Trigonometry"),
                SyntaxCatalogItem("sinh", "sinh(x)", "sinh(1)", "Calculate the hyperbolic sine of a value.", "Trigonometry"),
                SyntaxCatalogItem("cosh", "cosh(x)", "cosh(1)", "Calculate the hyperbolic cosine of a value.", "Trigonometry"),
                SyntaxCatalogItem("tanh", "tanh(x)", "tanh(1)", "Calculate the hyperbolic tangent of a value.", "Trigonometry"),
                SyntaxCatalogItem("sec", "sec(x)", "sec(pi / 3)", "Calculate the secant of a value (1/cos(x)).", "Trigonometry"),
                SyntaxCatalogItem("csc", "csc(x)", "csc(pi / 6)", "Calculate the cosecant of a value (1/sin(x)).", "Trigonometry"),
                SyntaxCatalogItem("cot", "cot(x)", "cot(pi / 4)", "Calculate the cotangent of a value (1/tan(x)).", "Trigonometry"),

                // Statistics
                SyntaxCatalogItem("mean", "mean(a, b, ...)", "mean([10, 20, 35, 55])", "Compute the arithmetic mean of values.", "Statistics"),
                SyntaxCatalogItem("median", "median(a, b, ...)", "median([5, 2, 7, 1, 9])", "Compute the median of a list of values.", "Statistics"),
                SyntaxCatalogItem("min", "min(a, b, ...)", "min([42, 17, 89, 8])", "Compute the minimum value of a list.", "Statistics"),
                SyntaxCatalogItem("max", "max(a, b, ...)", "max([42, 17, 89, 8])", "Compute the maximum value of a list.", "Statistics"),
                SyntaxCatalogItem("sum", "sum(a, b, ...)", "sum([1, 2, 3, 4, 5])", "Compute the sum of all values.", "Statistics"),
                SyntaxCatalogItem("prod", "prod(a, b, ...)", "prod([2, 3, 4])", "Compute the product of all values.", "Statistics"),
                SyntaxCatalogItem("std", "std(array)", "std([2, 4, 6, 8])", "Compute the sample standard deviation.", "Statistics"),
                SyntaxCatalogItem("variance", "variance(array)", "variance([2, 4, 6, 8])", "Compute the sample variance.", "Statistics"),
                SyntaxCatalogItem("mad", "mad(array)", "mad([10, 20, 30])", "Compute the median absolute deviation.", "Statistics"),
                SyntaxCatalogItem("mode", "mode(array)", "mode([1, 2, 2, 3, 4])", "Compute the mode (most frequent values).", "Statistics"),
                SyntaxCatalogItem("cumsum", "cumsum(array)", "cumsum([1, 2, 3, 4])", "Compute the cumulative sum of a vector.", "Statistics"),
                SyntaxCatalogItem("corr", "corr(A, B)", "corr([1, 2, 3, 4], [1, 3, 5, 8])", "Compute the Pearson correlation coefficient.", "Statistics"),
                SyntaxCatalogItem("quantileSeq", "quantileSeq(A, prob)", "quantileSeq([1, 2, 3, 4, 5], 0.75)", "Compute the prob-quantile of a numeric sequence.", "Statistics"),

                // Matrix & Algebra
                SyntaxCatalogItem("det", "det(x)", "det([1, 2; 3, 4])", "Calculate the determinant of a square matrix.", "Matrix"),
                SyntaxCatalogItem("inv", "inv(x)", "inv([1, 2; 3, 4])", "Calculate the inverse of a square matrix.", "Matrix"),
                SyntaxCatalogItem("transpose", "transpose(x)", "transpose([1, 2, 3; 4, 5, 6])", "Transpose a matrix.", "Matrix"),
                SyntaxCatalogItem("trace", "trace(x)", "trace([1, 2; 3, 4])", "Calculate the trace (sum of diagonal) of a square matrix.", "Matrix"),
                SyntaxCatalogItem("lusolve", "lusolve(A, b)", "lusolve([-2, 3; 2, 1], [11, 9])", "Solve the linear system A * x = b.", "Algebra"),
                SyntaxCatalogItem("dot", "dot(x, y)", "dot([1, 2, 3], [4, 5, 6])", "Calculate the scalar dot product of two vectors.", "Matrix"),
                SyntaxCatalogItem("cross", "cross(x, y)", "cross([1, 1, 0], [0, 1, 1])", "Calculate the cross product for two 3D vectors.", "Matrix"),
                SyntaxCatalogItem("kron", "kron(x, y)", "kron([1, 0; 0, 1], [1, 2; 3, 4])", "Calculate the Kronecker product of two matrices.", "Matrix"),
                SyntaxCatalogItem("identity", "identity(n)", "identity(3)", "Create an n x n identity matrix.", "Matrix"),
                SyntaxCatalogItem("diag", "diag(X)", "diag([1, 2, 3])", "Create a diagonal matrix or extract the diagonal.", "Matrix"),
                SyntaxCatalogItem("zeros", "zeros(m, n)", "zeros(2, 3)", "Create a matrix filled with zeros.", "Matrix"),
                SyntaxCatalogItem("ones", "ones(m, n)", "ones(2, 3)", "Create a matrix filled with ones.", "Matrix"),
                SyntaxCatalogItem("flatten", "flatten(x)", "flatten([1, 2, 3; 4, 5, 6])", "Flatten a multi-dimensional matrix into a 1D vector.", "Matrix"),
                SyntaxCatalogItem("reshape", "reshape(x, sizes)", "reshape([1, 2, 3, 4, 5, 6], [2, 3])", "Reshape a matrix or array to the specified dimensions.", "Matrix"),
                SyntaxCatalogItem("size", "size(x)", "size([1, 2, 3; 4, 5, 6])", "Calculate the dimensions of a matrix or string.", "Matrix"),
                SyntaxCatalogItem("count", "count(x)", "count([1, 2, 3; 4, 5, 6])", "Count the total number of elements in a matrix.", "Matrix"),
                SyntaxCatalogItem("diff", "diff(x)", "diff([1, 2, 4, 7, 0])", "Compute differences between adjacent elements.", "Matrix"),

                // Combinatorics & Probability
                SyntaxCatalogItem("factorial", "n!  |  factorial(n)", "6!", "Compute the factorial of a non-negative integer.", "Probability"),
                SyntaxCatalogItem("combinations", "combinations(n, k)", "combinations(7, 5)", "Compute the number of ways of picking k unordered outcomes from n.", "Probability"),
                SyntaxCatalogItem("combinationsWithRep", "combinationsWithRep(n, k)", "combinationsWithRep(7, 5)", "Combinations of k items from n with replacement.", "Probability"),
                SyntaxCatalogItem("permutations", "permutations(n, k)", "permutations(5, 3)", "Compute the number of ordered k-permutations of n.", "Probability"),
                SyntaxCatalogItem("gamma", "gamma(n)", "gamma(5)", "Compute the gamma function of a value.", "Probability"),
                SyntaxCatalogItem("catalan", "catalan(n)", "catalan(7)", "Compute the nth Catalan number.", "Combinatorics"),
                SyntaxCatalogItem("bellNumbers", "bellNumbers(n)", "bellNumbers(6)", "Compute the nth Bell number (partitions of a set).", "Combinatorics"),
                SyntaxCatalogItem("stirlingS2", "stirlingS2(n, k)", "stirlingS2(5, 3)", "Stirling numbers of the second kind.", "Combinatorics"),
                SyntaxCatalogItem("random", "random(min, max)", "random(0, 100)", "Return a random floating-point number in [min, max).", "Probability"),
                SyntaxCatalogItem("randomInt", "randomInt(min, max)", "randomInt(1, 100)", "Return a random integer in [min, max).", "Probability"),

                // Complex, Geometry, Set
                SyntaxCatalogItem("complex", "complex(re, im)", "complex(3, -4)", "Create a complex value from real and imaginary parts.", "Complex"),
                SyntaxCatalogItem("re", "re(x)", "re(2 + 3i)", "Get the real part of a complex number.", "Complex"),
                SyntaxCatalogItem("im", "im(x)", "im(2 + 3i)", "Get the imaginary part of a complex number.", "Complex"),
                SyntaxCatalogItem("conj", "conj(x)", "conj(2 + 3i)", "Compute the complex conjugate of a complex value.", "Complex"),
                SyntaxCatalogItem("arg", "arg(x)", "arg(1 + i)", "Compute the argument (phase angle) of a complex value.", "Complex"),
                SyntaxCatalogItem("distance", "distance(p1, p2)", "distance([0, 0], [3, 4])", "Calculate the Euclidean distance between two points.", "Geometry"),
                SyntaxCatalogItem("setUnion", "setUnion(a, b)", "setUnion([1, 2, 3], [3, 4, 5])", "Create the union of two sets.", "Set"),
                SyntaxCatalogItem("setIntersect", "setIntersect(a, b)", "setIntersect([1, 2, 3], [2, 3, 4])", "Create the intersection of two sets.", "Set"),
                SyntaxCatalogItem("setDifference", "setDifference(a, b)", "setDifference([1, 2, 3, 4], [3, 4])", "Create the difference of two sets (a \\ b).", "Set")
            )
        ),
        SyntaxCatalogGroup(
            category = "Science",
            items = listOf(
                SyntaxCatalogItem(
                    name = "Unit Conversion",
                    syntax = "value unit in targetUnit",
                    expr = "100 km/h in m/s",
                    desc = "Convert between compatible units using \"in\" or \"to\".",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Scientific Notation",
                    syntax = "1.23e-4  5e6",
                    expr = "6.022e23 / 1e20",
                    desc = "Floating-point numbers with base-10 exponent.",
                    subcategory = "Syntax & Operators"
                ),

                // Units & Prefixes
                SyntaxCatalogItem(
                    name = "Length Units",
                    syntax = "m, km, cm, mm, in, ft, yd, mi, nmi, au, ly",
                    expr = "100 m in ft",
                    desc = "Units: m, meter, in, inch, ft, foot, yd, yard, mi, mile, nmi, angstrom, au, ly.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Area / Surface Units",
                    syntax = "m2, km2, cm2, sqin, sqft, sqyd, sqmi, acre, hectare",
                    expr = "1 hectare in acre",
                    desc = "Units: m2, cm2, mm2, km2, sqin, sqft, sqyd, sqmi, acre, hectare.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Volume Units",
                    syntax = "m3, L, mL, cc, gal, qt, pt, cup, floz, tbsp, tsp",
                    expr = "1 gal in L",
                    desc = "Units: m3, cm3, L, liter, gal, gallon, qt, pt, cup, floz, tbsp, tsp.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Mass & Weight Units",
                    syntax = "g, kg, mg, ug, tonne, lb, oz, stone, ton",
                    expr = "1 kg in lb",
                    desc = "Units: g, gram, kg, mg, ug, tonne, lb, pound, oz, ounce, stone, ton.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Time & Duration Units",
                    syntax = "s, ms, us, ns, min, h, day, week, month, year",
                    expr = "1 day in seconds",
                    desc = "Units: s, sec, second, min, minute, h, hr, hour, day, week, month, year.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Temperature Units",
                    syntax = "K, degC, degF, degR",
                    expr = "98.6 degF in degC",
                    desc = "Units: K, kelvin, degC, celsius, degF, fahrenheit, degR, rankine.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Frequency & Angle Units",
                    syntax = "Hz, kHz, MHz, GHz  |  rad, deg, grad, arcmin, arcsec",
                    expr = "180 deg in rad",
                    desc = "Frequency (Hz) and Angle (rad, deg, grad, arcmin, arcsec) units.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Force, Energy & Power Units",
                    syntax = "N, lbf, dyn  |  J, kWh, cal, kcal, BTU, eV  |  W, kW, hp",
                    expr = "1 hp in W",
                    desc = "Force (N, lbf), Energy (J, kWh, cal, kcal, BTU, eV), Power (W, hp).",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Pressure Units",
                    syntax = "Pa, kPa, bar, atm, torr, mmHg, psi",
                    expr = "1 atm in psi",
                    desc = "Units: Pa, pascal, bar, atm, torr, mmHg, psi.",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Electrical & Magnetic Units",
                    syntax = "A, V, ohm, F, C, S, H, Wb, T",
                    expr = "4.7 kohm in ohm",
                    desc = "Current (A), Voltage (V), Resistance (ohm), Capacitance (F), Charge (C), Inductance (H), Flux (Wb, T).",
                    subcategory = "Units & Prefixes"
                ),
                SyntaxCatalogItem(
                    name = "Digital Storage & SI/Binary Prefixes",
                    syntax = "b, B, KB, MB, GB, TB, KiB, MiB, GiB, TiB",
                    expr = "5 GiB in MB",
                    desc = "Metric (y..Y) and Binary (Ki, Mi, Gi, Ti, Pi, Ei, Zi, Yi) prefixes on bits and bytes.",
                    subcategory = "Units & Prefixes"
                ),

                // Physical Constants & Special
                SyntaxCatalogItem("speedOfLight", "speedOfLight", "speedOfLight", "Speed of light in vacuum (299792458 m/s).", "Physical Constants"),
                SyntaxCatalogItem("gravity", "gravity", "gravity", "Standard acceleration of gravity on Earth (9.80665 m/s^2).", "Physical Constants"),
                SyntaxCatalogItem("planckConstant", "planckConstant", "planckConstant", "Planck constant h (6.62607015e-34 J*s).", "Physical Constants"),
                SyntaxCatalogItem("gravitationConstant", "gravitationConstant", "gravitationConstant", "Newtonian constant of gravitation G (6.67430e-11).", "Physical Constants"),
                SyntaxCatalogItem("avogadro", "avogadro", "avogadro", "Avogadro's number NA (6.02214076e23 mol^-1).", "Physical Constants"),
                SyntaxCatalogItem("boltzmann", "boltzmann", "boltzmann", "Boltzmann constant k (1.380649e-23 J/K).", "Physical Constants"),
                SyntaxCatalogItem("gasConstant", "gasConstant", "gasConstant", "Molar gas constant R (8.314462618 J/(mol*K)).", "Physical Constants"),
                SyntaxCatalogItem("elementaryCharge", "elementaryCharge", "elementaryCharge", "Elementary electric charge e (1.602176634e-19 C).", "Physical Constants"),
                SyntaxCatalogItem("electronMass", "electronMass", "electronMass", "Mass of an electron (9.1093837015e-31 kg).", "Physical Constants"),
                SyntaxCatalogItem("protonMass", "protonMass", "protonMass", "Mass of a proton (1.67262192369e-27 kg).", "Physical Constants"),
                SyntaxCatalogItem("erf", "erf(x)", "erf(0.5)", "Compute the Gauss error function of x.", "Special"),
                SyntaxCatalogItem("solveODE", "solveODE(f, [t0, tEnd], y0)", "f(t, y) = y; solveODE(f, [0, 1], 1)", "Numerically integrate an ordinary differential equation using RK4.", "Numeric")
            )
        ),
        SyntaxCatalogGroup(
            category = "Extra",
            items = listOf(
                SyntaxCatalogItem(
                    name = "Bitwise Operators",
                    syntax = "&  |  ^|  ~  <<  >>  >>>",
                    expr = "(0b1010 & 0b1100) | (1 << 4)",
                    desc = "Bitwise AND, OR, XOR (^|), NOT (~), and bit shifts.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Number Radixes (Hex, Bin, Oct)",
                    syntax = "0xff  0b1010  0o77",
                    expr = "0xff + 0b1010 + 0o77",
                    desc = "Hexadecimal, binary, and octal number literals.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Objects & Properties",
                    syntax = "{ key: value }  obj.key",
                    expr = "pt = {x: 3, y: 4}; sqrt(pt.x^2 + pt.y^2)",
                    desc = "Create key-value objects and access properties via dot or bracket notation.",
                    subcategory = "Syntax & Operators"
                ),
                SyntaxCatalogItem(
                    name = "Strings & Manipulation",
                    syntax = "\"text\"",
                    expr = "\"Mathematical\"[1:4]",
                    desc = "String literals and 1-based character slicing.",
                    subcategory = "Syntax & Operators"
                ),

                // Bitwise & Utils
                SyntaxCatalogItem("bitAnd", "x & y  |  bitAnd(x, y)", "bitAnd(14, 9)", "Bitwise AND of two integers.", "Bitwise"),
                SyntaxCatalogItem("bitOr", "x | y  |  bitOr(x, y)", "bitOr(6, 3)", "Bitwise OR of two integers.", "Bitwise"),
                SyntaxCatalogItem("bitXor", "x ^| y  |  bitXor(x, y)", "bitXor(7, 3)", "Bitwise XOR of two integers.", "Bitwise"),
                SyntaxCatalogItem("bitNot", "~x  |  bitNot(x)", "bitNot(0)", "Bitwise NOT (complement) of an integer.", "Bitwise"),
                SyntaxCatalogItem("leftShift", "x << y  |  leftShift(x, y)", "leftShift(1, 8)", "Bitwise left shift.", "Bitwise"),
                SyntaxCatalogItem("rightArithShift", "x >> y  |  rightArithShift(x, y)", "rightArithShift(32, 2)", "Bitwise signed right shift.", "Bitwise"),
                SyntaxCatalogItem("hex", "hex(x)", "hex(255)", "Format a number as hexadecimal string.", "Utils"),
                SyntaxCatalogItem("bin", "bin(x)", "bin(42)", "Format a number as binary string.", "Utils"),
                SyntaxCatalogItem("oct", "oct(x)", "oct(64)", "Format a number as octal string.", "Utils"),
                SyntaxCatalogItem("isPrime", "isPrime(x)", "isPrime(97)", "Test whether an integer is prime.", "Utils"),
                SyntaxCatalogItem("isInteger", "isInteger(x)", "isInteger(4.0)", "Test whether a value is an integer.", "Utils"),
                SyntaxCatalogItem("isPositive", "isPositive(x)", "isPositive(3.5)", "Test whether a value is strictly positive.", "Utils"),
                SyntaxCatalogItem("isNegative", "isNegative(x)", "isNegative(-2)", "Test whether a value is strictly negative.", "Utils"),
                SyntaxCatalogItem("typeOf", "typeOf(x)", "typeOf(2 + 3i)", "Determine the data type of a value.", "Utils"),
                SyntaxCatalogItem("map", "map(array, fn)", "f(x) = x^2; map([1, 2, 3, 4], f)", "Transform each element of a vector or matrix using a function.", "Matrix"),
                SyntaxCatalogItem("filter", "filter(array, fn)", "isBig(x) = x > 5; filter([2, 8, 4, 10], isBig)", "Filter elements of a vector using a predicate function.", "Matrix")
            )
        )
    )

    val totalCount: Int = groups.sumOf { it.items.size }
}
