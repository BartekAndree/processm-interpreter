package com.processm.processminterpreter.pql.model

/**
 * Base exception for all PQL-related errors.
 *
 * Based on ProcessM: https://github.com/ProcessMPUT/processm
 */
sealed class PQLException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Syntax error in PQL query.
 *
 * Thrown during parsing when the query doesn't match the grammar.
 *
 * Examples:
 * - Missing quotes: select event:name where activity = test
 * - Invalid operator: select event:name where activity == 'test'
 * - Unclosed string: select event:name where activity = 'test
 */
class PQLSyntaxException(
    val line: Int,
    val charPositionInLine: Int,
    message: String,
    cause: Throwable? = null
) : PQLException(
    "Syntax error at $line:$charPositionInLine - $message",
    cause
)

/**
 * Semantic error in PQL query.
 *
 * Thrown when the query is syntactically correct but semantically invalid.
 *
 * Examples:
 * - Using aggregation function in WHERE clause
 * - Using classifier in WHERE clause
 * - Hoisting beyond LOG scope
 * - GROUP BY without all non-aggregated SELECT fields
 */
open class PQLSemanticException(
    message: String,
    cause: Throwable? = null
) : PQLException(message, cause)

/**
 * Invalid scope hoisting.
 *
 * Examples:
 * - ^^^e:name (hoisting beyond LOG)
 * - ^l:name (LOG has no parent scope)
 */
class InvalidScopeHoistingException(
    message: String
) : PQLSemanticException(message)

/**
 * Invalid classifier usage.
 *
 * Classifiers are only allowed in SELECT and GROUP BY clauses,
 * not in WHERE or at LOG scope.
 *
 * Examples:
 * - where c:businesscase = 'xyz' (classifiers not allowed in WHERE)
 * - select l:c:activity (classifiers not allowed at LOG scope)
 */
class InvalidClassifierUsageException(
    message: String
) : PQLSemanticException(message)

/**
 * Invalid GROUP BY clause.
 *
 * Examples:
 * - SELECT e:name, count(e:id) without GROUP BY e:name
 * - GROUP BY on non-selected field
 */
class InvalidGroupByException(
    message: String
) : PQLSemanticException(message)

/**
 * Invalid DELETE clause.
 *
 * Examples:
 * - DELETE with GROUP BY
 * - DELETE with SELECT
 * - SELECT with DELETE
 */
class InvalidDeleteException(
    message: String
) : PQLSemanticException(message)

/**
 * Invalid function usage.
 *
 * Examples:
 * - Unknown function name
 * - Wrong number of arguments
 * - Scalar function with scope greater than argument scope
 */
class InvalidFunctionException(
    message: String
) : PQLSemanticException(message)
