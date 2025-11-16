package com.processm.processminterpreter.pql.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Base class for literal values in PQL queries.
 *
 * Literals represent constant values:
 * - Strings: "hello", 'world'
 * - Numbers: 42, 3.14
 * - Booleans: true, false
 * - Dates/Times: D2020-01-01, D2020-01-01T12:30:00
 * - UUIDs: 550e8400-e29b-41d4-a716-446655440000
 * - Null: null
 *
 * Based on ProcessM: https://github.com/ProcessMPUT/processm
 */
sealed class Literal<T>(
    val value: T,
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Expression(line, charPositionInLine) {

    override val type: Type
        get() = when (this) {
            is StringLiteral -> Type.STRING
            is NumberLiteral -> Type.NUMBER
            is BooleanLiteral -> Type.BOOLEAN
            is DateTimeLiteral -> Type.DATETIME
            is UUIDLiteral -> Type.UUID
            is NullLiteral -> Type.UNKNOWN
        }

    override fun toString(): String = value.toString()
}

/**
 * String literal.
 *
 * Supports escape sequences:
 * - \" - double quote
 * - \t - tab
 * - \n - newline
 * - \r - carriage return
 * - \f - form feed
 * - \b - backspace
 * - \\ - backslash
 */
class StringLiteral(
    value: String,
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Literal<String>(value, line, charPositionInLine) {

    companion object {
        /**
         * Parse a string literal from PQL.
         * Removes quotes and unescapes escape sequences.
         *
         * @param s the string with quotes (e.g., "hello" or 'hello')
         * @param line line number for error reporting
         * @param charPos character position for error reporting
         * @return parsed StringLiteral
         */
        fun parse(s: String, line: Int = -1, charPos: Int = -1): StringLiteral {
            if (s.length < 2) {
                throw PQLSyntaxException(line, charPos, "String literal too short: $s")
            }

            // Determine quote type
            val quoteChar = s[0]
            if (quoteChar != '"' && quoteChar != '\'') {
                throw PQLSyntaxException(line, charPos, "String literal must start with quote: $s")
            }

            // Find the closing quote by scanning and skipping escaped quotes
            // This handles edge cases like "abc\"" where the closing quote is escaped
            val closingQuoteIndex = findClosingQuote(s, quoteChar)

            // Extract content between quotes
            val unquoted = s.substring(1, closingQuoteIndex)

            // Unescape Java escape sequences
            val unescaped = unescapeJava(unquoted)
            return StringLiteral(unescaped, line, charPos)
        }

        /**
         * Find the index of the closing quote, properly handling escape sequences.
         *
         * Scans the string from position 1 (after opening quote) and tracks
         * whether the previous character was an escape backslash.
         *
         * Examples:
         * - "hello" → returns 5 (index of closing ")
         * - "abc\"" → returns 6 (skips escaped " at index 4)
         * - "abc jr\" → returns 9 (end of string, closing quote is escaped)
         *
         * @param s the string to scan
         * @param quoteChar the quote character being used (" or ')
         * @return index of closing quote, or s.length if no unescaped closing quote found
         */
        private fun findClosingQuote(s: String, quoteChar: Char): Int {
            var i = 1 // Start after opening quote
            var escaped = false

            while (i < s.length) {
                val currentChar = s[i]

                if (escaped) {
                    // Previous char was \, so this char is escaped
                    escaped = false
                } else if (currentChar == '\\') {
                    // Start of escape sequence
                    escaped = true
                } else if (currentChar == quoteChar) {
                    // Found unescaped closing quote
                    return i
                }

                i++
            }

            // No closing quote found - return end of string
            // This handles malformed strings like "abc jr\" where closing quote is escaped
            return s.length
        }

        /**
         * Unescape Java escape sequences.
         *
         * Supports: \", \t, \n, \r, \f, \b, \\
         *
         * IMPORTANT: \\ must be processed FIRST, otherwise it will unescape
         * the backslashes we just added from other escape sequences.
         */
        private fun unescapeJava(s: String): String {
            return s
                .replace("\\\\", "\u0000") // Temp placeholder for backslash
                .replace("\\\"", "\"")
                .replace("\\t", "\t")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\f", "\u000C")
                .replace("\\b", "\b")
                .replace("\u0000", "\\") // Replace placeholder with actual backslash
        }
    }

    override fun toString(): String = "\"$value\""
}

/**
 * Number literal (integer or floating-point).
 */
class NumberLiteral(
    value: Double,
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Literal<Double>(value, line, charPositionInLine) {

    companion object {
        /**
         * Parse a number literal from PQL.
         *
         * @param s the number string (e.g., "42" or "3.14")
         * @param line line number for error reporting
         * @param charPos character position for error reporting
         * @return parsed NumberLiteral
         */
        fun parse(s: String, line: Int = -1, charPos: Int = -1): NumberLiteral {
            return try {
                NumberLiteral(s.toDouble(), line, charPos)
            } catch (e: NumberFormatException) {
                throw PQLSyntaxException(line, charPos, "Invalid number: $s", e)
            }
        }
    }
}

/**
 * Boolean literal (true or false).
 */
class BooleanLiteral(
    value: Boolean,
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Literal<Boolean>(value, line, charPositionInLine) {

    companion object {
        /**
         * Parse a boolean literal from PQL.
         *
         * @param s the boolean string ("true" or "false", case-insensitive)
         * @param line line number for error reporting
         * @param charPos character position for error reporting
         * @return parsed BooleanLiteral
         */
        fun parse(s: String, line: Int = -1, charPos: Int = -1): BooleanLiteral {
            return when (s.lowercase()) {
                "true" -> BooleanLiteral(true, line, charPos)
                "false" -> BooleanLiteral(false, line, charPos)
                else -> throw PQLSyntaxException(line, charPos, "Invalid boolean: $s (expected 'true' or 'false')")
            }
        }
    }
}

/**
 * DateTime literal.
 *
 * Supports formats:
 * - Date only: D2020-01-01 (defaults to 00:00:00)
 * - Date and time: D2020-01-01T12:30:00
 * - ISO 8601: D2020-01-01T12:30:00.123
 *
 * The 'D' prefix is optional but recommended for clarity.
 */
class DateTimeLiteral(
    value: LocalDateTime,
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Literal<LocalDateTime>(value, line, charPositionInLine) {

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
        private val dateFormatter = DateTimeFormatter.ISO_DATE

        /**
         * Parse a datetime literal from PQL.
         *
         * @param s the datetime string (e.g., "D2020-01-01" or "D2020-01-01T12:30:00")
         * @param line line number for error reporting
         * @param charPos character position for error reporting
         * @return parsed DateTimeLiteral
         */
        fun parse(s: String, line: Int = -1, charPos: Int = -1): DateTimeLiteral {
            // Remove 'D' or 'd' prefix if present
            val cleaned = s.removePrefix("D").removePrefix("d").removePrefix("'").removeSuffix("'")

            return try {
                val dateTime = try {
                    // Try full datetime format first
                    LocalDateTime.parse(cleaned, dateTimeFormatter)
                } catch (e: Exception) {
                    // Fall back to date only (default time to 00:00:00)
                    LocalDate.parse(cleaned, dateFormatter).atStartOfDay()
                }
                DateTimeLiteral(dateTime, line, charPos)
            } catch (e: Exception) {
                throw PQLSyntaxException(
                    line,
                    charPos,
                    "Invalid datetime: $s (expected format: D2020-01-01 or D2020-01-01T12:30:00)",
                    e,
                )
            }
        }
    }

    override fun toString(): String = "D${value.format(dateTimeFormatter)}"
}

/**
 * UUID literal.
 */
class UUIDLiteral(
    value: UUID,
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Literal<UUID>(value, line, charPositionInLine) {

    companion object {
        /**
         * Parse a UUID literal from PQL.
         *
         * @param s the UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000")
         * @param line line number for error reporting
         * @param charPos character position for error reporting
         * @return parsed UUIDLiteral
         */
        fun parse(s: String, line: Int = -1, charPos: Int = -1): UUIDLiteral {
            return try {
                UUIDLiteral(UUID.fromString(s), line, charPos)
            } catch (e: Exception) {
                throw PQLSyntaxException(
                    line,
                    charPos,
                    "Invalid UUID: $s",
                    e,
                )
            }
        }
    }
}

/**
 * Null literal.
 */
class NullLiteral(
    line: Int = -1,
    charPositionInLine: Int = -1,
) : Literal<Nothing?>(null, line, charPositionInLine) {

    override fun toString(): String = "null"
}
