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
        val query = pqlQuery.trim()

        // Parse basic SELECT query structure
        val selectRegex = Regex(
            """select\s+(.+?)\s+from\s+(\w+)(?:\s+where\s+(.+))?""",
            RegexOption.IGNORE_CASE,
        )

        val matchResult = selectRegex.find(query)
            ?: throw IllegalArgumentException("Invalid SELECT query format: $query")

        val selectClause = matchResult.groupValues[1].trim()
        val fromClause = matchResult.groupValues[2].trim().lowercase()
        val whereClause = matchResult.groupValues[3].takeIf { it.isNotBlank() }

        logger.debug("Parsed - SELECT: $selectClause, FROM: $fromClause, WHERE: $whereClause")

        return when (fromClause) {
            "log" -> buildLogQuery(selectClause, whereClause, logId)
            "trace" -> buildTraceQuery(selectClause, whereClause, logId)
            "event" -> buildEventQuery(selectClause, whereClause, logId)
            else -> throw IllegalArgumentException("Unsupported FROM clause: $fromClause")
        }
    }

    /**
     * Build Cypher query for log selection
     */
    private fun buildLogQuery(selectClause: String, whereClause: String?, logId: String?): CypherQuery {
        val cypherBuilder = StringBuilder()
        val parameters = mutableMapOf<String, Any>()

        // Base MATCH clause
        cypherBuilder.append("MATCH (log:Log)")

        // Add log ID filter if specified
        if (logId != null) {
            cypherBuilder.append(" WHERE log.logId = \$logId")
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
        val returnClause = translateSelectClause(selectClause, "log")
        cypherBuilder.append(" RETURN $returnClause")

        return CypherQuery(cypherBuilder.toString(), parameters)
    }

    /**
     * Build Cypher query for trace selection
     */
    private fun buildTraceQuery(selectClause: String, whereClause: String?, logId: String?): CypherQuery {
        val cypherBuilder = StringBuilder()
        val parameters = mutableMapOf<String, Any>()

        // Base MATCH clause
        if (logId != null) {
            cypherBuilder.append("MATCH (log:Log {logId: \$logId})-[:CONTAINS]->(trace:Trace)")
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
        val returnClause = translateSelectClause(selectClause, "trace")
        cypherBuilder.append(" RETURN $returnClause")

        return CypherQuery(cypherBuilder.toString(), parameters)
    }

    /**
     * Build Cypher query for event selection
     */
    private fun buildEventQuery(selectClause: String, whereClause: String?, logId: String?): CypherQuery {
        val cypherBuilder = StringBuilder()
        val parameters = mutableMapOf<String, Any>()

        // Base MATCH clause
        if (logId != null) {
            cypherBuilder.append("MATCH (log:Log {logId: \$logId})-[:CONTAINS]->(trace:Trace)-[:HAS_EVENT]->(event:Event)")
            parameters["logId"] = logId
        } else {
            cypherBuilder.append("MATCH (event:Event)")
        }

        // Add WHERE clause if present
        if (whereClause != null) {
            val whereCondition = translateWhereClause(whereClause, "event", parameters)
            cypherBuilder.append(" WHERE $whereCondition")
        }

        // Add RETURN clause
        val returnClause = translateSelectClause(selectClause, "event")
        cypherBuilder.append(" RETURN $returnClause")

        return CypherQuery(cypherBuilder.toString(), parameters)
    }

    /**
     * Translate SELECT clause to Cypher RETURN clause
     */
    private fun translateSelectClause(selectClause: String, nodeAlias: String): String {
        return when (selectClause.trim()) {
            "*" -> nodeAlias
            else -> {
                // Handle specific field selection
                val fields = selectClause.split(",").map { it.trim() }
                fields.joinToString(", ") { field ->
                    when (field.lowercase()) {
                        "id" -> "$nodeAlias.id"
                        "name" -> "$nodeAlias.name"
                        "activity" -> "$nodeAlias.activity"
                        "timestamp" -> "$nodeAlias.timestamp"
                        "resource" -> "$nodeAlias.resource"
                        "caseid", "case_id" -> "$nodeAlias.caseId"
                        else -> "$nodeAlias.$field"
                    }
                }
            }
        }
    }

    /**
     * Translate WHERE clause to Cypher WHERE condition
     */
    private fun translateWhereClause(whereClause: String, nodeAlias: String, parameters: MutableMap<String, Any>): String {
        // Simple WHERE clause parsing (proof of concept)
        val conditions = whereClause.split(" and ", ignoreCase = true)

        return conditions.mapIndexed { index, condition ->
            translateSingleCondition(condition.trim(), nodeAlias, parameters, index)
        }.joinToString(" AND ")
    }

    /**
     * Translate single WHERE condition
     */
    private fun translateSingleCondition(
        condition: String,
        nodeAlias: String,
        parameters: MutableMap<String, Any>,
        paramIndex: Int,
    ): String {
        // Parse condition: field operator value
        val conditionRegex = Regex("""(\w+)\s*(=|!=|<>|<|>|<=|>=|like)\s*['"]?([^'"]+)['"]?""", RegexOption.IGNORE_CASE)
        val matchResult = conditionRegex.find(condition)
            ?: throw IllegalArgumentException("Invalid WHERE condition: $condition")

        val field = matchResult.groupValues[1].lowercase()
        val operator = matchResult.groupValues[2].lowercase()
        val value = matchResult.groupValues[3]

        val paramName = "param$paramIndex"

        // Map field names
        val cypherField = when (field) {
            "activity" -> "$nodeAlias.activity"
            "resource" -> "$nodeAlias.resource"
            "timestamp" -> "$nodeAlias.timestamp"
            "caseid", "case_id" -> "$nodeAlias.caseId"
            "name" -> "$nodeAlias.name"
            else -> "$nodeAlias.$field"
        }

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

        return if (operator == "like") {
            "$cypherField $cypherOperator \$$paramName"
        } else {
            "$cypherField $cypherOperator \$$paramName"
        }
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
