package com.processm.processminterpreter.pql.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Function class.
 *
 * Adapted from ProcessM FunctionTests.kt:
 * https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/test/kotlin/processm/core/querylanguage/FunctionTests.kt
 */
class FunctionTests {

    // ========================================
    // SCALAR FUNCTION TESTS
    // ========================================

    /**
     * Test valid scalar function.
     * ProcessM test: year function with correct type and child node.
     */
    @Test
    fun validScalarFunctionTest() {
        val attr = Attribute("e:timestamp")
        val func = Function("year", args = arrayOf(attr))

        assertEquals("year", func.name, "Function name should be 'year'")
        assertEquals(FunctionType.SCALAR, func.functionType, "Should be SCALAR function")
        assertEquals(Type.NUMBER, func.type, "year() returns NUMBER")
        assertEquals(1, func.children.size, "Should have 1 child")
        assertEquals(attr, func.children[0], "Child should be the timestamp attribute")
        assertNull(func.scope, "No scope prefix")
    }

    /**
     * Test scalar function with scope prefix.
     */
    @Test
    fun scalarFunctionWithScopeTest() {
        val attr = Attribute("e:timestamp")
        val func = Function("e:year", args = arrayOf(attr))

        assertEquals("year", func.name)
        assertEquals(Scope.EVENT, func.scope, "Should have EVENT scope")
        assertEquals(FunctionType.SCALAR, func.functionType)
    }

    /**
     * Test all scalar date/time functions.
     */
    @Test
    fun scalarDateTimeFunctionsTest() {
        val timestamp = Attribute("e:timestamp")

        // Date/time extraction functions (return NUMBER)
        val yearFunc = Function("year", args = arrayOf(timestamp))
        assertEquals(Type.NUMBER, yearFunc.type)

        val monthFunc = Function("month", args = arrayOf(timestamp))
        assertEquals(Type.NUMBER, monthFunc.type)

        val dayFunc = Function("day", args = arrayOf(timestamp))
        assertEquals(Type.NUMBER, dayFunc.type)

        val hourFunc = Function("hour", args = arrayOf(timestamp))
        assertEquals(Type.NUMBER, hourFunc.type)

        val minuteFunc = Function("minute", args = arrayOf(timestamp))
        assertEquals(Type.NUMBER, minuteFunc.type)

        val secondFunc = Function("second", args = arrayOf(timestamp))
        assertEquals(Type.NUMBER, secondFunc.type)

        // Date/time construction functions (return DATETIME)
        val dateFunc = Function("date", args = arrayOf(timestamp))
        assertEquals(Type.DATETIME, dateFunc.type)

        val timeFunc = Function("time", args = arrayOf(timestamp))
        assertEquals(Type.DATETIME, timeFunc.type)
    }

    /**
     * Test now() function (no arguments).
     */
    @Test
    fun nowFunctionTest() {
        val nowFunc = Function("now")
        assertEquals("now", nowFunc.name)
        assertEquals(FunctionType.SCALAR, nowFunc.functionType)
        assertEquals(Type.DATETIME, nowFunc.type)
        assertEquals(0, nowFunc.children.size, "now() has no arguments")
    }

    /**
     * Test scalar string functions.
     */
    @Test
    fun scalarStringFunctionsTest() {
        val name = Attribute("e:name")

        val upperFunc = Function("upper", args = arrayOf(name))
        assertEquals("upper", upperFunc.name)
        assertEquals(FunctionType.SCALAR, upperFunc.functionType)
        assertEquals(Type.STRING, upperFunc.type)

        val lowerFunc = Function("lower", args = arrayOf(name))
        assertEquals("lower", lowerFunc.name)
        assertEquals(Type.STRING, lowerFunc.type)
    }

    // ========================================
    // AGGREGATION FUNCTION TESTS
    // ========================================

    /**
     * Test valid aggregation function.
     * ProcessM test: avg function with correct type and child node.
     */
    @Test
    fun validAggregateFunctionTest() {
        val attr = Attribute("e:cost_total")
        val func = Function("avg", args = arrayOf(attr))

        assertEquals("avg", func.name, "Function name should be 'avg'")
        assertEquals(FunctionType.AGGREGATION, func.functionType, "Should be AGGREGATION function")
        assertEquals(Type.NUMBER, func.type, "avg() returns NUMBER")
        assertEquals(1, func.children.size, "Should have 1 child")
        assertEquals(attr, func.children[0], "Child should be the cost attribute")
    }

    /**
     * Test all aggregation functions.
     */
    @Test
    fun allAggregationFunctionsTest() {
        val attr = Attribute("e:id")

        // count
        val countFunc = Function("count", args = arrayOf(attr))
        assertEquals("count", countFunc.name)
        assertEquals(FunctionType.AGGREGATION, countFunc.functionType)
        assertEquals(Type.NUMBER, countFunc.type)

        // sum
        val sumFunc = Function("sum", args = arrayOf(attr))
        assertEquals("sum", sumFunc.name)
        assertEquals(Type.NUMBER, sumFunc.type)

        // avg
        val avgFunc = Function("avg", args = arrayOf(attr))
        assertEquals("avg", avgFunc.name)
        assertEquals(Type.NUMBER, avgFunc.type)

        // min (returns ANY - can be any type)
        val minFunc = Function("min", args = arrayOf(attr))
        assertEquals("min", minFunc.name)
        assertEquals(Type.ANY, minFunc.type)

        // max (returns ANY - can be any type)
        val maxFunc = Function("max", args = arrayOf(attr))
        assertEquals("max", maxFunc.name)
        assertEquals(Type.ANY, maxFunc.type)
    }

    // ========================================
    // INVALID FUNCTION TESTS
    // ========================================

    /**
     * Test invalid function name throws exception.
     * ProcessM test: unknown function "XYZ" should throw.
     */
    @Test
    fun invalidFunctionTest() {
        val attr = Attribute("e:name")

        // Unknown function name
        assertThrows(InvalidFunctionException::class.java) {
            Function("XYZ", args = arrayOf(attr))
        }

        assertThrows(InvalidFunctionException::class.java) {
            Function("unknown", args = arrayOf(attr))
        }
    }

    /**
     * Test wrong number of arguments throws exception.
     * ProcessM test: avg and year require arguments.
     */
    @Test
    fun wrongNumberOfArgumentsTest() {
        // avg requires 1 argument
        assertThrows(InvalidFunctionException::class.java) {
            Function("avg") // No arguments
        }

        // year requires 1 argument
        assertThrows(InvalidFunctionException::class.java) {
            Function("year") // No arguments
        }

        // now requires 0 arguments
        val attr = Attribute("e:timestamp")
        assertThrows(InvalidFunctionException::class.java) {
            Function("now", args = arrayOf(attr)) // Should have no arguments
        }

        // Too many arguments
        val attr1 = Attribute("e:name")
        val attr2 = Attribute("e:timestamp")
        assertThrows(InvalidFunctionException::class.java) {
            Function("year", args = arrayOf(attr1, attr2)) // year takes only 1 argument
        }
    }

    // ========================================
    // SCOPE VALIDATION TESTS
    // ========================================

    /**
     * Test that scalar function scope must not be greater than argument scope.
     * LOG > TRACE > EVENT (in hierarchy)
     */
    @Test
    fun scalarFunctionScopeValidationTest() {
        // Valid: EVENT scope function with EVENT scope argument
        val eventAttr = Attribute("e:timestamp")
        assertDoesNotThrow {
            Function("e:year", args = arrayOf(eventAttr))
        }

        // Valid: TRACE scope function with EVENT scope argument (TRACE > EVENT allowed)
        assertDoesNotThrow {
            Function("t:year", args = arrayOf(eventAttr))
        }

        // Valid: LOG scope function with EVENT scope argument (LOG > EVENT allowed)
        assertDoesNotThrow {
            Function("l:year", args = arrayOf(eventAttr))
        }

        // Invalid: EVENT scope function with TRACE scope argument
        val traceAttr = Attribute("t:name")
        assertThrows(PQLSemanticException::class.java) {
            Function("e:year", args = arrayOf(traceAttr))
        }

        // Invalid: EVENT scope function with LOG scope argument
        val logAttr = Attribute("l:name")
        assertThrows(PQLSemanticException::class.java) {
            Function("e:year", args = arrayOf(logAttr))
        }
    }

    /**
     * Test that aggregation functions don't have scope restrictions.
     */
    @Test
    fun aggregationFunctionNoScopeRestrictionTest() {
        // Aggregation functions can have any scope with any argument scope
        val eventAttr = Attribute("e:id")

        assertDoesNotThrow {
            Function("count", args = arrayOf(eventAttr))
        }

        assertDoesNotThrow {
            Function("t:count", args = arrayOf(eventAttr))
        }

        assertDoesNotThrow {
            Function("l:count", args = arrayOf(eventAttr))
        }
    }

    // ========================================
    // HELPER METHOD TESTS
    // ========================================

    /**
     * Test isScalar() and isAggregation() helper methods.
     */
    @Test
    fun helperMethodsTest() {
        assertTrue(Function.isScalar("year"))
        assertTrue(Function.isScalar("YEAR")) // Case insensitive
        assertTrue(Function.isScalar("upper"))
        assertFalse(Function.isScalar("count"))
        assertFalse(Function.isScalar("unknown"))

        assertTrue(Function.isAggregation("count"))
        assertTrue(Function.isAggregation("COUNT")) // Case insensitive
        assertTrue(Function.isAggregation("sum"))
        assertFalse(Function.isAggregation("year"))
        assertFalse(Function.isAggregation("unknown"))
    }

    /**
     * Test toString() representation.
     */
    @Test
    fun toStringTest() {
        val attr = Attribute("e:timestamp")

        val func1 = Function("year", args = arrayOf(attr))
        assertEquals("year(e:timestamp)", func1.toString())

        val func2 = Function("t:count", args = arrayOf(attr))
        assertEquals("t:count(e:timestamp)", func2.toString())

        val func3 = Function("now")
        assertEquals("now()", func3.toString())
    }

    /**
     * Test that functions are not terminal (they have children).
     */
    @Test
    fun functionsAreNotTerminalTest() {
        val attr = Attribute("e:id")
        val func = Function("count", args = arrayOf(attr))

        assertFalse(func.isTerminal, "Functions with arguments are not terminal")

        val nowFunc = Function("now")
        assertTrue(nowFunc.isTerminal, "now() has no arguments, so it's terminal")
    }
}
