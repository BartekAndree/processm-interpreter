package com.processm.processminterpreter.pql.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for OrderDirection enum.
 *
 * Adapted from ProcessM OrderDirectionTests.kt:
 * https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/test/kotlin/processm/core/querylanguage/OrderDirectionTests.kt
 */
class OrderDirectionTests {

    /**
     * Test parsing of order direction strings.
     * Verifies that OrderDirection.parse() correctly converts strings to enum values.
     */
    @Test
    fun parseTest() {
        // Test ascending variants
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse("asc"))
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse("ascending"))
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse("ASC"))
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse("ASCENDING"))

        // Test descending variants
        assertEquals(OrderDirection.DESCENDING, OrderDirection.parse("desc"))
        assertEquals(OrderDirection.DESCENDING, OrderDirection.parse("descending"))
        assertEquals(OrderDirection.DESCENDING, OrderDirection.parse("DESC"))
        assertEquals(OrderDirection.DESCENDING, OrderDirection.parse("DESCENDING"))

        // Test null defaults to ASCENDING
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse(null))
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse(null, OrderDirection.ASCENDING))

        // Test with custom default
        assertEquals(OrderDirection.DESCENDING, OrderDirection.parse(null, OrderDirection.DESCENDING))

        // Test that toString() values can be parsed back
        assertEquals(OrderDirection.ASCENDING, OrderDirection.parse(OrderDirection.ASCENDING.toString()))
        assertEquals(OrderDirection.DESCENDING, OrderDirection.parse(OrderDirection.DESCENDING.toString()))
    }

    /**
     * Test that parsing invalid direction throws IllegalArgumentException.
     */
    @Test
    fun invalidParseTest() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderDirection.parse("XYZ")
        }

        assertThrows(IllegalArgumentException::class.java) {
            OrderDirection.parse("invalid")
        }

        assertThrows(IllegalArgumentException::class.java) {
            OrderDirection.parse("")
        }

        assertThrows(IllegalArgumentException::class.java) {
            OrderDirection.parse("up")
        }
    }

    /**
     * Test toString() method.
     */
    @Test
    fun toStringTest() {
        assertEquals("asc", OrderDirection.ASCENDING.toString())
        assertEquals("desc", OrderDirection.DESCENDING.toString())
    }

    /**
     * Test enum values.
     */
    @Test
    fun valuesTest() {
        val values = OrderDirection.values()
        assertEquals(2, values.size)
        assertTrue(values.contains(OrderDirection.ASCENDING))
        assertTrue(values.contains(OrderDirection.DESCENDING))
    }
}
