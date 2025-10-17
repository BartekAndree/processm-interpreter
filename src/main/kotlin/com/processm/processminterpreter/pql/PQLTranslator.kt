package com.processm.processminterpreter.pql

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Simple PQL to Cypher translator for basic queries
 *
 * This is a proof-of-concept implementation that handles basic SELECT and WHERE clauses.
 * In the future, this should be replaced with proper ANTLR4 parser integration from ProcessM.
 */
@Component
class PQLTranslator {

    private val logger = LoggerFactory.getLogger(PQLTranslator::class.java)

    /**
     * Translate PQL query to Cypher query
     */
    fun translateToCypher(pqlQuery: String, logId: String? = null): CypherQuery {
        logger.debug("Translating PQL query: $pqlQuery")

        val normalizedQuery = pqlQuery.trim().lowercase()

        return when {
            normalizedQuery.startsWith("select") -> translateSelectQuery(pqlQuery, logId)
            else -> throw UnsupportedOperationException("Query type not supported: $pqlQuery")
        }
    }

    /**
     * Translate SELECT query to Cypher
     */
    private fun translateSelectQuery(pqlQuery: String, logId: String?): CypherQuery {
        var query = pqlQuery.trim()

        // Parse LIMIT first (from the end)
        val limitRegex = Regex("""\s+limit\s+(\d+)\s*$""", RegexOption.IGNORE_CASE)
        val limitMatch = limitRegex.find(query)
        val limitClause = limitMatch?.groupValues?.get(1)
        if (limitMatch != null) {
            query = query.substring(0, limitMatch.range.first)
        }

        // Parse ORDER BY second (from the end)
        val orderByRegex = Regex("""\s+order\s+by\s+(.+?)\s*$""", RegexOption.IGNORE_CASE)
        val orderByMatch = orderByRegex.find(query)
        val orderByClause = orderByMatch?.groupValues?.get(1)?.trim()
        if (orderByMatch != null) {
            query = query.substring(0, orderByMatch.range.first)
        }

        // Parse the rest with simpler regex
        val selectRegex = Regex(
            """select\s+(distinct\s+)?(.+?)\s+from\s+(\w+)(?:\s+where\s+(.+?))?(?:\s+group by\s+(.+?))?(?:\s+having\s+(.+?))?\s*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val matchResult = selectRegex.find(query)
            ?: throw IllegalArgumentException("Invalid SELECT query format: $query")

        val distinct = matchResult.groupValues[1].isNotBlank()
        val selectClause = matchResult.groupValues[2].trim()
        val fromClause = matchResult.groupValues[3].trim().lowercase()
        val whereClause = matchResult.groupValues[4].takeIf { it.isNotBlank() }
        val groupByClause = matchResult.groupValues[5].takeIf { it.isNotBlank() }
        val havingClause = matchResult.groupValues[6].takeIf { it.isNotBlank() }

        logger.debug("Parsed - DISTINCT: $distinct, SELECT: $selectClause, FROM: $fromClause, WHERE: $whereClause, GROUP BY: $groupByClause, HAVING: $havingClause, ORDER BY: $orderByClause, LIMIT: $limitClause")

        return when (fromClause) {
            "log" -> buildLogQuery(distinct, selectClause, whereClause, groupByClause, havingClause, orderByClause, limitClause, logId)
            "trace" -> buildTraceQuery(distinct, selectClause, whereClause, groupByClause, havingClause, orderByClause, limitClause, logId)
            "event" -> buildEventQuery(distinct, selectClause, whereClause, groupByClause, havingClause, orderByClause, limitClause, logId)
            else -> throw IllegalArgumentException("Unsupported FROM clause: $fromClause")
        }
    }

    /**
     * Build Cypher query for log selection
     */
    private fun buildLogQuery(distinct: Boolean, selectClause: String, whereClause: String?, groupByClause: String?, havingClause: String?, orderByClause: String?, limitClause: String?, logId: String?): CypherQuery {
        val cypherBuilder = StringBuilder()
        val parameters = mutableMapOf<String, Any>()

        // Base MATCH clause
        cypherBuilder.append("MATCH (log:Log)")

        // Add log ID filter if specified
        if (logId != null) {
            cypherBuilder.append(" WHERE log.logId = \"$logId\"")
            parameters["logId"] = logId
        }

        // Add WHERE clause if present
        if (whereClause != null) {
            val whereCondition = translateWhereClause(whereClause, "log", parameters)
            if (logId != null) {
                cypherBuilder.append(" AND $whereCondition")
            } else {
                cypherBuilder.append(" WHERE $whereCondition")
            }
        }

        // Add RETURN clause
        cypherBuilder.append(" RETURN ")
        if (distinct) {
            cypherBuilder.append("DISTINCT ")
        }
        val returnClause = translateSelectClause(selectClause, "log")
        cypherBuilder.append(returnClause)

        // Add ORDER BY clause if present
        if (orderByClause != null) {
            cypherBuilder.append(" ORDER BY ")
            cypherBuilder.append(translateOrderByClause(orderByClause, "log"))
        }

        // Add LIMIT clause if present
        if (limitClause != null) {
            cypherBuilder.append(" LIMIT $limitClause")
        }

        return CypherQuery(cypherBuilder.toString(), parameters)
    }

    /**
     * Build Cypher query for trace selection
     */
    private fun buildTraceQuery(distinct: Boolean, selectClause: String, whereClause: String?, groupByClause: String?, havingClause: String?, orderByClause: String?, limitClause: String?, logId: String?): CypherQuery {
        val cypherBuilder = StringBuilder()
        val parameters = mutableMapOf<String, Any>()

        // Base MATCH clause
        if (logId != null) {
            cypherBuilder.append("MATCH (log:Log {logId: \"$logId\"})-[:CONTAINS]->(trace:Trace)")
            parameters["logId"] = logId
        } else {
            cypherBuilder.append("MATCH (trace:Trace)")
        }

        // Add WHERE clause if present
        if (whereClause != null) {
            val whereCondition = translateWhereClause(whereClause, "trace", parameters)
            cypherBuilder.append(" WHERE $whereCondition")
        }

        // Add RETURN clause
        cypherBuilder.append(" RETURN ")
        if (distinct) {
            cypherBuilder.append("DISTINCT ")
        }
        val returnClause = translateSelectClause(selectClause, "trace")
        cypherBuilder.append(returnClause)

        // Add ORDER BY clause if present
        if (orderByClause != null) {
            cypherBuilder.append(" ORDER BY ")
            cypherBuilder.append(translateOrderByClause(orderByClause, "trace"))
        }

        // Add LIMIT clause if present
        if (limitClause != null) {
            cypherBuilder.append(" LIMIT $limitClause")
        }

        return CypherQuery(cypherBuilder.toString(), parameters)
    }

    /**
     * Build Cypher query for event selection
     */
    private fun buildEventQuery(distinct: Boolean, selectClause: String, whereClause: String?, groupByClause: String?, havingClause: String?, orderByClause: String?, limitClause: String?, logId: String?): CypherQuery {
        val cypherBuilder = StringBuilder()
        val parameters = mutableMapOf<String, Any>()

        // Base MATCH clause
        if (logId != null) {
            cypherBuilder.append("MATCH (log:Log {logId: \"$logId\"})-[:CONTAINS]->(trace:Trace)-[:HAS_EVENT]->(event:Event)")
            parameters["logId"] = logId
        } else {
            cypherBuilder.append("MATCH (event:Event)")
        }

        // Add a WHERE clause if present
        if (whereClause != null) {
            val whereCondition = translateWhereClause(whereClause, "event", parameters)
            cypherBuilder.append(" WHERE $whereCondition")
        }

        val hasAggregation = selectClause.contains(Regex("""(COUNT|AVG|SUM|MIN|MAX)\s*\(""", RegexOption.IGNORE_CASE))

        if (groupByClause != null || hasAggregation) {
            val withClause = translateSelectClause(selectClause, "event", withAliases = true)
            cypherBuilder.append(" WITH $withClause")

            if (havingClause != null) {
                val havingCondition = translateHavingClause(havingClause, parameters, parameters.size)
                cypherBuilder.append(" WHERE $havingCondition")
            }

            cypherBuilder.append(" RETURN ")
            if (distinct) {
                cypherBuilder.append("DISTINCT ")
            }
            val returnFields = selectClause.split(',').map { it.trim() }.map {
                val parts = it.split(Regex("""\s+as\s+""", RegexOption.IGNORE_CASE), 2)
                if (parts.size == 2) parts[1] else {
                    val field = parts[0]
                    if (field.contains("(")) {
                        field.substringBefore("(").trim()
                    } else {
                        field
                    }
                }
            }
            cypherBuilder.append(returnFields.joinToString(", "))

            // Add ORDER BY clause if present (for aggregation queries)
            if (orderByClause != null) {
                cypherBuilder.append(" ORDER BY ")
                cypherBuilder.append(translateOrderByClause(orderByClause, "event"))
            }

            // Add a LIMIT clause if present
            if (limitClause != null) {
                cypherBuilder.append(" LIMIT $limitClause")
            }

        } else {
            // Add a RETURN clause
            cypherBuilder.append(" RETURN ")
            if (distinct) {
                cypherBuilder.append("DISTINCT ")
            }
            val returnClause = translateSelectClause(selectClause, "event")
            cypherBuilder.append(returnClause)

            // Add ORDER BY clause if present
            if (orderByClause != null) {
                cypherBuilder.append(" ORDER BY ")
                cypherBuilder.append(translateOrderByClause(orderByClause, "event"))
            }

            // Add a LIMIT clause if present
            if (limitClause != null) {
                cypherBuilder.append(" LIMIT $limitClause")
            }
        }

        return CypherQuery(cypherBuilder.toString(), parameters)
    }

    private fun getCypherField(pqlField: String, nodeAlias: String): String {
        if (nodeAlias == "event" && pqlField.equals("traceId", ignoreCase = true)) {
            return "trace.traceId"
        }
        return when (pqlField.lowercase()) {
            "id" -> "$nodeAlias.id"
            "name" -> "$nodeAlias.name"
            "activity" -> "$nodeAlias.activity"
            "timestamp" -> "$nodeAlias.timestamp"
            "resource" -> "$nodeAlias.resource"
            "caseid", "case_id" -> "$nodeAlias.caseId"
            else -> "$nodeAlias.$pqlField"
        }
    }

    /**
     * Translate SELECT clause to Cypher RETURN clause
     * withAliases: if true, ensures all fields have aliases (required for WITH clause)
     */
    private fun translateSelectClause(selectClause: String, nodeAlias: String, withAliases: Boolean = false): String {
        return when (selectClause.trim()) {
            "*" -> "properties($nodeAlias) as $nodeAlias"
            else -> {
                val fields = selectClause.split(",").map { it.trim() }
                fields.joinToString(", ") { field ->
                    val aggregationRegex = Regex("""(COUNT|AVG|SUM|MIN|MAX)\s*\((.*?)\)\s*(?:as\s+(\w+))?""", RegexOption.IGNORE_CASE)
                    val match = aggregationRegex.find(field)

                    if (match != null) {
                        val function = match.groupValues[1].lowercase()
                        val innerField = match.groupValues[2].trim()
                        val alias = match.groupValues[3].takeIf { it.isNotBlank() }

                        val cypherField = if (innerField == "*") "*" else getCypherField(innerField, nodeAlias)

                        if (alias != null) {
                            "$function($cypherField) AS $alias"
                        } else {
                            "$function($cypherField)"
                        }
                    } else {
                        // Handle regular fields with optional aliases
                        val aliasRegex = Regex("""(.+?)\s+as\s+(\w+)""", RegexOption.IGNORE_CASE)
                        val aliasMatch = aliasRegex.find(field)

                        if (aliasMatch != null) {
                            val actualField = aliasMatch.groupValues[1].trim()
                            val alias = aliasMatch.groupValues[2].trim()
                            "${getCypherField(actualField, nodeAlias)} AS $alias"
                        } else {
                            val cypherField = getCypherField(field, nodeAlias)
                            // If withAliases is true, add alias for regular fields
                            if (withAliases) {
                                "$cypherField AS $field"
                            } else {
                                cypherField
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Translate ORDER BY clause to Cypher ORDER BY clause
     */
    private fun translateOrderByClause(orderByClause: String, nodeAlias: String): String {
        val orderParts = orderByClause.split(",").map { it.trim() }
        return orderParts.joinToString(", ") { part ->
            val tokens = part.split(Regex("""\s+"""))
            val field = tokens[0]
            val direction = tokens.getOrNull(1)?.uppercase() ?: "ASC"

            val cypherField = getCypherField(field, nodeAlias)
            "$cypherField $direction"
        }
    }

    private fun translateHavingClause(havingClause: String, parameters: MutableMap<String, Any>, paramIndex: Int): String {
        // Simple HAVING clause parsing (proof of concept)
        val conditionRegex = Regex("""(\w+)\s*(=|!=|<|>|<=|>=|<>)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val matchResult = conditionRegex.find(havingClause)
            ?: throw IllegalArgumentException("Invalid HAVING condition: $havingClause")

        val field = matchResult.groupValues[1]
        val operator = matchResult.groupValues[2]
        val value = matchResult.groupValues[3]

        val paramName = "param$paramIndex"
        parameters[paramName] = value.toLong() // Assuming numeric for now

        return "$field $operator \$$paramName"
    }


    /**
     * Translate WHERE clause to Cypher WHERE condition
     * Supports AND, OR operators
     */
    private fun translateWhereClause(whereClause: String, nodeAlias: String, parameters: MutableMap<String, Any>): String {
        // Split by OR first (lower precedence)
        val orParts = whereClause.split(Regex("""\s+or\s+""", RegexOption.IGNORE_CASE))

        if (orParts.size > 1) {
            // Handle OR conditions
            return orParts.mapIndexed { orIndex, orPart ->
                val andConditions = orPart.trim().split(Regex("""\s+and\s+""", RegexOption.IGNORE_CASE))
                val andResult = andConditions.mapIndexed { andIndex, condition ->
                    val paramIndexBase = parameters.size
                    translateSingleCondition(condition.trim(), nodeAlias, parameters, paramIndexBase)
                }.joinToString(" AND ")

                if (andConditions.size > 1) "($andResult)" else andResult
            }.joinToString(" OR ")
        } else {
            // Handle AND conditions only
            val andConditions = whereClause.split(Regex("""\s+and\s+""", RegexOption.IGNORE_CASE))
            return andConditions.mapIndexed { index, condition ->
                translateSingleCondition(condition.trim(), nodeAlias, parameters, parameters.size)
            }.joinToString(" AND ")
        }
    }

    /**
     * Translate a single WHERE condition
     * Supports: =, !=, <, >, <=, >=, LIKE, IN, BETWEEN, IS NULL, IS NOT NULL
     */
    private fun translateSingleCondition(
        condition: String,
        nodeAlias: String,
        parameters: MutableMap<String, Any>,
        paramIndex: Int,
    ): String {
        // Check for IS NULL / IS NOT NULL
        val nullRegex = Regex("""(\w+)\s+is\s+(not\s+)?null""", RegexOption.IGNORE_CASE)
        nullRegex.find(condition)?.let { match ->
            val field = match.groupValues[1].lowercase()
            val isNotNull = match.groupValues[2].isNotBlank()
            val cypherField = getCypherField(field, nodeAlias)
            return if (isNotNull) {
                "$cypherField IS NOT NULL"
            } else {
                "$cypherField IS NULL"
            }
        }

        // Check for BETWEEN
        val betweenRegex = Regex("""(\w+)\s+between\s+['"](.*?)['"]\s+and\s+['"](.*?)['"]""", RegexOption.IGNORE_CASE)
        betweenRegex.find(condition)?.let { match ->
            val field = match.groupValues[1].lowercase()
            val value1 = match.groupValues[2]
            val value2 = match.groupValues[3]
            val cypherField = getCypherField(field, nodeAlias)
            val paramName1 = "param$paramIndex"
            val paramName2 = "param${paramIndex + 1}"
            parameters[paramName1] = value1
            parameters[paramName2] = value2
            return "$cypherField >= \$$paramName1 AND $cypherField <= \$$paramName2"
        }

        // Check for IN operator
        val inRegex = Regex("""(\w+)\s+in\s*\(([^)]+)\)""", RegexOption.IGNORE_CASE)
        inRegex.find(condition)?.let { match ->
            val field = match.groupValues[1].lowercase()
            val valuesList = match.groupValues[2]
                .split(",")
                .map { it.trim().removeSurrounding("'").removeSurrounding("\"") }
            val cypherField = getCypherField(field, nodeAlias)
            val paramName = "param$paramIndex"
            parameters[paramName] = valuesList
            return "$cypherField IN \$$paramName"
        }

        // Standard operators: =, !=, <, >, <=, >=, LIKE
        val conditionRegex = Regex("""(\w+)\s*(=|!=|<>|<|>|<=|>=|like)\s*['"]?([^'"]+?)['"]?\s*${'$'}""", RegexOption.IGNORE_CASE)
        val matchResult = conditionRegex.find(condition)
            ?: throw IllegalArgumentException("Invalid WHERE condition: $condition")

        val field = matchResult.groupValues[1].lowercase()
        val operator = matchResult.groupValues[2].lowercase()
        val value = matchResult.groupValues[3].trim()

        val paramName = "param$paramIndex"

        // Map field names
        val cypherField = getCypherField(field, nodeAlias)

        // Map operators
        val cypherOperator = when (operator) {
            "=" -> "="
            "!=" -> "<>"
            "<>" -> "<>"
            "<" -> "<"
            ">" -> ">"
            "<=" -> "<="
            ">=" -> ">="
            "like" -> "CONTAINS"
            else -> throw IllegalArgumentException("Unsupported operator: $operator")
        }

        // Add parameter
        parameters[paramName] = if (operator == "like") {
            value.replace("%", "") // Simple LIKE to CONTAINS conversion
        } else {
            value
        }

        return "$cypherField $cypherOperator \$$paramName"
    }
}

/**
 * Data class representing a Cypher query with parameters
 */
data class CypherQuery(
    val query: String,
    val parameters: Map<String, Any> = emptyMap(),
) {
    override fun toString(): String {
        return "CypherQuery(query='$query', parameters=$parameters)"
    }
}
