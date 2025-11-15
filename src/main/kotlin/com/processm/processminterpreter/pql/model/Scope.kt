package com.processm.processminterpreter.pql.model

/**
 * Represents the three-level hierarchy of process mining scopes.
 *
 * Hierarchy: LOG (top) → TRACE (middle) → EVENT (bottom/default)
 *
 * Based on ProcessM: https://github.com/ProcessMPUT/processm
 */
enum class Scope(val scopeName: String, val shortName: String) {
    LOG("log", "l"),
    TRACE("trace", "t"),
    EVENT("event", "e");

    /**
     * Returns the parent scope in the hierarchy.
     * - LOG.upper = null (no parent)
     * - TRACE.upper = LOG
     * - EVENT.upper = TRACE
     */
    val upper: Scope?
        get() = when (this) {
            LOG -> null
            TRACE -> LOG
            EVENT -> TRACE
        }

    /**
     * Returns the child scope in the hierarchy.
     * - LOG.lower = TRACE
     * - TRACE.lower = EVENT
     * - EVENT.lower = null (no child)
     */
    val lower: Scope?
        get() = when (this) {
            LOG -> TRACE
            TRACE -> EVENT
            EVENT -> null
        }

    override fun toString(): String = scopeName

    companion object {
        /**
         * Parse a string to Scope enum.
         * Accepts: "log"/"l", "trace"/"t", "event"/"e" (case-insensitive)
         *
         * @param s the string to parse
         * @param default the default scope if parsing fails (default: EVENT)
         * @return the parsed Scope
         * @throws IllegalArgumentException if string is invalid and no default provided
         */
        fun parse(s: String, default: Scope? = null): Scope {
            return when (s.lowercase()) {
                "log", "l" -> LOG
                "trace", "t" -> TRACE
                "event", "e" -> EVENT
                else -> default ?: throw IllegalArgumentException("Invalid scope: $s")
            }
        }
    }
}

/**
 * Extension property to generate scope prefix for attributes.
 *
 * Examples:
 * - Scope.LOG.prefix = "log:"
 * - Scope.EVENT.prefix = "event:"
 * - null.prefix = ""
 */
val Scope?.prefix: String
    get() = this?.let { "${it.scopeName}:" } ?: ""
