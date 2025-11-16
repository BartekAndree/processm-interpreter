package com.processm.processminterpreter.pql.model

/**
 * Represents the sorting direction in ORDER BY clauses.
 *
 * Based on ProcessM: https://github.com/ProcessMPUT/processm
 */
enum class OrderDirection {
    ASCENDING {
        override fun toString() = "asc"
    },
    DESCENDING {
        override fun toString() = "desc"
    }, ;

    companion object {
        /**
         * Parse a string to OrderDirection enum.
         * Accepts: "asc", "ascending", "desc", "descending" (case-insensitive)
         *
         * @param s the string to parse (null defaults to ASCENDING)
         * @param default the default direction if parsing fails (default: ASCENDING)
         * @return the parsed OrderDirection
         * @throws IllegalArgumentException if string is invalid and no default provided
         */
        fun parse(s: String?, default: OrderDirection = ASCENDING): OrderDirection {
            if (s == null) return default

            return when (s.lowercase()) {
                "desc", "descending" -> DESCENDING
                "asc", "ascending" -> ASCENDING
                else -> throw IllegalArgumentException("Invalid order direction: $s")
            }
        }
    }
}
