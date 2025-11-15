package com.processm.processminterpreter.pql.model

/**
 * Represents the data type of a PQL expression.
 *
 * Used for type checking and validation in query processing.
 *
 * Based on ProcessM: https://github.com/ProcessMPUT/processm
 */
enum class Type {
    /**
     * String type - text values
     */
    STRING,

    /**
     * Number type - integers and floating-point numbers
     */
    NUMBER,

    /**
     * Boolean type - true/false values
     */
    BOOLEAN,

    /**
     * DateTime type - date and time values
     */
    DATETIME,

    /**
     * UUID type - universally unique identifiers
     */
    UUID,

    /**
     * Any type - used for aggregation functions that can return any type
     * (e.g., min/max can return numbers, strings, or dates depending on input)
     */
    ANY,

    /**
     * Unknown type - default when type cannot be determined
     */
    UNKNOWN
}
