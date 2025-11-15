package com.processm.processminterpreter.pql.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Scope enum.
 *
 * Adapted from ProcessM ScopeTests.kt:
 * https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/test/kotlin/processm/core/querylanguage/ScopeTests.kt
 */
class ScopeTests {

    /**
     * Test parsing of scope strings.
     * Verifies that Scope.parse() correctly handles all valid scope representations.
     */
    @Test
    fun parseTest() {
        // Test full names
        assertEquals(Scope.LOG, Scope.parse("log"))
        assertEquals(Scope.TRACE, Scope.parse("trace"))
        assertEquals(Scope.EVENT, Scope.parse("event"))

        // Test short names
        assertEquals(Scope.LOG, Scope.parse("l"))
        assertEquals(Scope.TRACE, Scope.parse("t"))
        assertEquals(Scope.EVENT, Scope.parse("e"))

        // Test case insensitivity
        assertEquals(Scope.LOG, Scope.parse("LOG"))
        assertEquals(Scope.LOG, Scope.parse("Log"))
        assertEquals(Scope.TRACE, Scope.parse("TRACE"))
        assertEquals(Scope.EVENT, Scope.parse("EVENT"))

        // Test default parameter
        assertEquals(Scope.EVENT, Scope.parse("invalid", Scope.EVENT))
        assertEquals(Scope.LOG, Scope.parse("xyz", Scope.LOG))
    }

    /**
     * Test that parsing invalid scope throws IllegalArgumentException.
     */
    @Test
    fun invalidParseTest() {
        assertThrows(IllegalArgumentException::class.java) {
            Scope.parse("XYZ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            Scope.parse("invalid")
        }

        assertThrows(IllegalArgumentException::class.java) {
            Scope.parse("")
        }
    }

    /**
     * Test scope hierarchy navigation (upper/lower).
     * Verifies that navigating up then down (or down then up) returns the original scope.
     */
    @Test
    fun lowerAndUpperTest() {
        // Test upper navigation
        assertNull(Scope.LOG.upper, "LOG should have no upper scope")
        assertEquals(Scope.LOG, Scope.TRACE.upper, "TRACE.upper should be LOG")
        assertEquals(Scope.TRACE, Scope.EVENT.upper, "EVENT.upper should be TRACE")

        // Test lower navigation
        assertEquals(Scope.TRACE, Scope.LOG.lower, "LOG.lower should be TRACE")
        assertEquals(Scope.EVENT, Scope.TRACE.lower, "TRACE.lower should be EVENT")
        assertNull(Scope.EVENT.lower, "EVENT should have no lower scope")

        // Test round-trip: upper then lower
        assertEquals(Scope.TRACE, Scope.TRACE.upper?.lower, "TRACE.upper.lower should be TRACE")
        assertEquals(Scope.EVENT, Scope.EVENT.upper?.lower, "EVENT.upper.lower should be EVENT")

        // Test round-trip: lower then upper
        assertEquals(Scope.LOG, Scope.LOG.lower?.upper, "LOG.lower.upper should be LOG")
        assertEquals(Scope.TRACE, Scope.TRACE.lower?.upper, "TRACE.lower.upper should be TRACE")

        // Test boundary cases with null handling
        assertNull(Scope.LOG.upper?.upper, "LOG.upper.upper should be null")
        assertNull(Scope.EVENT.lower?.lower, "EVENT.lower.lower should be null")
    }

    /**
     * Test toString() method.
     */
    @Test
    fun toStringTest() {
        assertEquals("log", Scope.LOG.toString())
        assertEquals("trace", Scope.TRACE.toString())
        assertEquals("event", Scope.EVENT.toString())
    }

    /**
     * Test scopeName and shortName properties.
     */
    @Test
    fun propertiesTest() {
        assertEquals("log", Scope.LOG.scopeName)
        assertEquals("l", Scope.LOG.shortName)

        assertEquals("trace", Scope.TRACE.scopeName)
        assertEquals("t", Scope.TRACE.shortName)

        assertEquals("event", Scope.EVENT.scopeName)
        assertEquals("e", Scope.EVENT.shortName)
    }

    /**
     * Test prefix extension property.
     */
    @Test
    fun prefixTest() {
        assertEquals("log:", Scope.LOG.prefix)
        assertEquals("trace:", Scope.TRACE.prefix)
        assertEquals("event:", Scope.EVENT.prefix)

        // Test null scope
        val nullScope: Scope? = null
        assertEquals("", nullScope.prefix)
    }
}
