package com.processm.processminterpreter.pql.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Tests for Literal classes.
 *
 * Adapted from ProcessM LiteralTests.kt:
 * https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/test/kotlin/processm/core/querylanguage/LiteralTests.kt
 */
class LiteralTests {

    // ========================================
    // STRING LITERAL TESTS
    // ========================================

    /**
     * Test empty string literal.
     */
    @Test
    fun emptyStringTest() {
        val literal1 = StringLiteral.parse("\"\"")
        assertEquals("", literal1.value, "Empty double-quoted string")
        assertEquals(Type.STRING, literal1.type)
        assertTrue(literal1.isTerminal, "Literal should be terminal")

        val literal2 = StringLiteral.parse("''")
        assertEquals("", literal2.value, "Empty single-quoted string")
    }

    /**
     * Test escape character in string.
     * ProcessM test: "abc jr\"" should unescape to: abc jr"
     */
    @Test
    fun escapeCharInStringTest() {
        val literal = StringLiteral.parse("\"abc jr\\\"\"")
        assertEquals("abc jr\"", literal.value, "Should unescape \\\" to \"")
    }

    /**
     * Test escape sequence at the end of string.
     */
    @Test
    fun escapeSequenceAtTheEndOfStringTest() {
        val literal = StringLiteral.parse("\"abc jr\\\"")
        assertEquals("abc jr\"", literal.value, "Should unescape trailing \\\"")
    }

    /**
     * Test special escape characters.
     * ProcessM tests: \t, \r, \n, \f, \b, \\
     */
    @Test
    fun specialEscapeCharactersTest() {
        // Tab
        val tab = StringLiteral.parse("\"hello\\tworld\"")
        assertEquals("hello\tworld", tab.value, "Should unescape \\t to tab")

        // Newline
        val newline = StringLiteral.parse("\"line1\\nline2\"")
        assertEquals("line1\nline2", newline.value, "Should unescape \\n to newline")

        // Carriage return
        val cr = StringLiteral.parse("\"text\\rmore\"")
        assertEquals("text\rmore", cr.value, "Should unescape \\r to CR")

        // Form feed
        val ff = StringLiteral.parse("\"page1\\fpage2\"")
        assertEquals("page1\u000Cpage2", ff.value, "Should unescape \\f to form feed")

        // Backspace
        val bs = StringLiteral.parse("\"abc\\bdef\"")
        assertEquals("abc\bdef", bs.value, "Should unescape \\b to backspace")

        // Backslash
        val backslash = StringLiteral.parse("\"path\\\\file\"")
        assertEquals("path\\file", backslash.value, "Should unescape \\\\ to \\")

        // Multiple escapes
        val multi = StringLiteral.parse("\"\\t\\n\\r\\f\\b\\\\\"")
        assertEquals("\t\n\r\u000C\b\\", multi.value, "Should unescape all sequences")
    }

    /**
     * Test single-quoted strings.
     */
    @Test
    fun singleQuotedStringTest() {
        val literal = StringLiteral.parse("'hello world'")
        assertEquals("hello world", literal.value)
        assertEquals(Type.STRING, literal.type)
    }

    /**
     * Test string without quotes throws exception.
     */
    @Test
    fun unquotedStringThrowsException() {
        assertThrows(PQLSyntaxException::class.java) {
            StringLiteral.parse("unquoted")
        }
    }

    // ========================================
    // NUMBER LITERAL TESTS
    // ========================================

    /**
     * Test integer number.
     */
    @Test
    fun integerNumberTest() {
        val literal = NumberLiteral.parse("42")
        assertEquals(42.0, literal.value)
        assertEquals(Type.NUMBER, literal.type)
    }

    /**
     * Test floating-point number.
     */
    @Test
    fun floatingPointNumberTest() {
        val literal = NumberLiteral.parse("3.14159")
        assertEquals(3.14159, literal.value, 0.00001)
    }

    /**
     * Test negative number.
     */
    @Test
    fun negativeNumberTest() {
        val literal = NumberLiteral.parse("-123.45")
        assertEquals(-123.45, literal.value, 0.00001)
    }

    /**
     * Test invalid number throws exception.
     */
    @Test
    fun invalidNumberThrowsException() {
        assertThrows(PQLSyntaxException::class.java) {
            NumberLiteral.parse("not-a-number")
        }
    }

    // ========================================
    // BOOLEAN LITERAL TESTS
    // ========================================

    /**
     * Test boolean literals.
     */
    @Test
    fun booleanTest() {
        val trueVal = BooleanLiteral.parse("true")
        assertEquals(true, trueVal.value)
        assertEquals(Type.BOOLEAN, trueVal.type)

        val falseVal = BooleanLiteral.parse("false")
        assertEquals(false, falseVal.value)

        // Case insensitive
        assertEquals(true, BooleanLiteral.parse("TRUE").value)
        assertEquals(false, BooleanLiteral.parse("FALSE").value)
    }

    /**
     * Test invalid boolean throws exception.
     */
    @Test
    fun invalidBooleanThrowsException() {
        assertThrows(PQLSyntaxException::class.java) {
            BooleanLiteral.parse("yes")
        }

        assertThrows(PQLSyntaxException::class.java) {
            BooleanLiteral.parse("1")
        }
    }

    // ========================================
    // DATETIME LITERAL TESTS
    // ========================================

    /**
     * Test date-only literal (defaults to 00:00:00).
     */
    @Test
    fun dateOnlyTest() {
        val literal = DateTimeLiteral.parse("D2020-01-15")
        assertEquals(LocalDateTime.of(2020, 1, 15, 0, 0, 0), literal.value)
        assertEquals(Type.DATETIME, literal.type)

        // Without 'D' prefix
        val literal2 = DateTimeLiteral.parse("2020-01-15")
        assertEquals(LocalDateTime.of(2020, 1, 15, 0, 0, 0), literal2.value)
    }

    /**
     * Test date and time literal.
     */
    @Test
    fun dateTimeTest() {
        val literal = DateTimeLiteral.parse("D2020-01-15T14:30:00")
        assertEquals(LocalDateTime.of(2020, 1, 15, 14, 30, 0), literal.value)

        // With lowercase 'd'
        val literal2 = DateTimeLiteral.parse("d2020-12-31T23:59:59")
        assertEquals(LocalDateTime.of(2020, 12, 31, 23, 59, 59), literal2.value)
    }

    /**
     * Test invalid datetime throws exception.
     */
    @Test
    fun invalidDateTimeThrowsException() {
        assertThrows(PQLSyntaxException::class.java) {
            DateTimeLiteral.parse("D2020-13-01")  // Month 13 invalid
        }

        assertThrows(PQLSyntaxException::class.java) {
            DateTimeLiteral.parse("Dnot-a-date")
        }
    }

    // ========================================
    // UUID LITERAL TESTS
    // ========================================

    /**
     * Test valid UUID.
     */
    @Test
    fun validUUIDTest() {
        val uuidStr = "550e8400-e29b-41d4-a716-446655440000"
        val literal = UUIDLiteral.parse(uuidStr)
        assertEquals(uuidStr, literal.value.toString())
        assertEquals(Type.UUID, literal.type)
    }

    /**
     * Test invalid UUID throws exception.
     */
    @Test
    fun invalidUUIDThrowsException() {
        assertThrows(PQLSyntaxException::class.java) {
            UUIDLiteral.parse("not-a-uuid")
        }

        assertThrows(PQLSyntaxException::class.java) {
            UUIDLiteral.parse("123-456-789")
        }
    }

    // ========================================
    // NULL LITERAL TESTS
    // ========================================

    /**
     * Test null literal.
     */
    @Test
    fun nullTest() {
        val literal = NullLiteral()
        assertNull(literal.value, "Null literal should have null value")
        assertEquals(Type.UNKNOWN, literal.type)
        assertTrue(literal.isTerminal)
    }

    // ========================================
    // GENERAL LITERAL TESTS
    // ========================================

    /**
     * Test that literals are terminal expressions (no children).
     */
    @Test
    fun literalsAreTerminalTest() {
        assertTrue(StringLiteral("test").isTerminal)
        assertTrue(NumberLiteral(42.0).isTerminal)
        assertTrue(BooleanLiteral(true).isTerminal)
        assertTrue(DateTimeLiteral(LocalDateTime.now()).isTerminal)
        assertTrue(UUIDLiteral(java.util.UUID.randomUUID()).isTerminal)
        assertTrue(NullLiteral().isTerminal)
    }

    /**
     * Test toString() for literals.
     */
    @Test
    fun toStringTest() {
        assertEquals("\"hello\"", StringLiteral("hello").toString())
        assertEquals("42.0", NumberLiteral(42.0).toString())
        assertEquals("true", BooleanLiteral(true).toString())
        assertEquals("null", NullLiteral().toString())
    }
}
