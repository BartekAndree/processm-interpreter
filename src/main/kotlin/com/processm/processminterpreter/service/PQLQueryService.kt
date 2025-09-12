package com.processm.processminterpreter.service

import com.processm.processminterpreter.pql.CypherQuery
import com.processm.processminterpreter.pql.PQLTranslator
import org.neo4j.driver.Driver
import org.neo4j.driver.Record
import org.neo4j.driver.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for executing PQL queries
 *
 * Translates PQL queries to Cypher and executes them against Neo4j database
 */
@Service
@Transactional(readOnly = true)
class PQLQueryService(
    private val pqlTranslator: PQLTranslator,
    private val neo4jDriver: Driver,
) {

    private val logger = LoggerFactory.getLogger(PQLQueryService::class.java)

    /**
     * Execute PQL query and return results
     */
    fun executePQLQuery(pqlQuery: String, logId: String? = null): PQLQueryResult {
        logger.info("Executing PQL query: $pqlQuery (logId: $logId)")

        return try {
            // Translate PQL to Cypher
            val cypherQuery = pqlTranslator.translateToCypher(pqlQuery, logId)
            logger.debug("Translated to Cypher: $cypherQuery")

            // Execute Cypher query
            val results = executeCypherQuery(cypherQuery)

            PQLQueryResult(
                success = true,
                query = pqlQuery,
                cypherQuery = cypherQuery.query,
                results = results,
                resultCount = results.size,
                executionTimeMs = 0, // TODO: Add timing
            )
        } catch (e: Exception) {
            logger.error("Error executing PQL query: $pqlQuery", e)
            PQLQueryResult(
                success = false,
                query = pqlQuery,
                error = e.message ?: "Unknown error",
                results = emptyList(),
                resultCount = 0,
                executionTimeMs = 0,
            )
        }
    }

    /**
     * Validate PQL query syntax
     */
    fun validatePQLQuery(pqlQuery: String): PQLValidationResult {
        logger.debug("Validating PQL query: $pqlQuery")

        return try {
            // Try to translate the query
            val cypherQuery = pqlTranslator.translateToCypher(pqlQuery)

            PQLValidationResult(
                valid = true,
                query = pqlQuery,
                cypherQuery = cypherQuery.query,
                message = "Query is valid",
            )
        } catch (e: Exception) {
            logger.warn("Invalid PQL query: $pqlQuery - ${e.message}")
            PQLValidationResult(
                valid = false,
                query = pqlQuery,
                error = e.message ?: "Unknown validation error",
                message = "Query validation failed",
            )
        }
    }

    /**
     * Execute Cypher query against Neo4j
     */
    private fun executeCypherQuery(cypherQuery: CypherQuery): List<Map<String, Any?>> {
        neo4jDriver.session().use { session ->
            val result: Result = session.run(cypherQuery.query, cypherQuery.parameters)

            return result.list { record ->
                convertRecordToMap(record)
            }
        }
    }

    /**
     * Convert Neo4j Record to Map
     */
    private fun convertRecordToMap(record: Record): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        record.keys().forEach { key ->
            val value = record.get(key)
            map[key] = when {
                value.isNull -> null
                value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().NODE()) -> {
                    // Convert Node to Map
                    val node = value.asNode()
                    val nodeMap = mutableMapOf<String, Any?>()
                    nodeMap["id"] = node.id()
                    nodeMap["labels"] = node.labels().toList()
                    node.keys().forEach { nodeKey ->
                        nodeMap[nodeKey] = convertNeo4jValue(node.get(nodeKey))
                    }
                    nodeMap
                }
                value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().RELATIONSHIP()) -> {
                    // Convert Relationship to Map
                    val rel = value.asRelationship()
                    val relMap = mutableMapOf<String, Any?>()
                    relMap["id"] = rel.id()
                    relMap["type"] = rel.type()
                    relMap["startNodeId"] = rel.startNodeId()
                    relMap["endNodeId"] = rel.endNodeId()
                    rel.keys().forEach { relKey ->
                        relMap[relKey] = convertNeo4jValue(rel.get(relKey))
                    }
                    relMap
                }
                else -> convertNeo4jValue(value)
            }
        }

        return map
    }

    /**
     * Convert Neo4j Value to Java object
     */
    private fun convertNeo4jValue(value: org.neo4j.driver.Value): Any? {
        return when {
            value.isNull -> null
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().STRING()) -> value.asString()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().INTEGER()) -> value.asLong()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().FLOAT()) -> value.asDouble()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().BOOLEAN()) -> value.asBoolean()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().DATE_TIME()) -> value.asZonedDateTime()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().LOCAL_DATE_TIME()) -> value.asLocalDateTime()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().DATE()) -> value.asLocalDate()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().TIME()) -> value.asOffsetTime()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().LOCAL_TIME()) -> value.asLocalTime()
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().LIST()) -> value.asList { convertNeo4jValue(it) }
            value.hasType(org.neo4j.driver.types.TypeSystem.getDefault().MAP()) -> {
                val map = mutableMapOf<String, Any?>()
                value.asMap().forEach { (k, v) ->
                    map[k] = convertNeo4jValue(org.neo4j.driver.Values.value(v))
                }
                map
            }
            else -> value.asObject()
        }
    }

    /**
     * Get query execution statistics
     */
    fun getQueryStatistics(): Map<String, Any> {
        // TODO: Implement query statistics tracking
        return mapOf(
            "totalQueries" to 0,
            "successfulQueries" to 0,
            "failedQueries" to 0,
            "averageExecutionTime" to 0.0,
        )
    }
}

/**
 * Result of PQL query execution
 */
data class PQLQueryResult(
    val success: Boolean,
    val query: String,
    val cypherQuery: String? = null,
    val results: List<Map<String, Any?>> = emptyList(),
    val resultCount: Int = 0,
    val executionTimeMs: Long = 0,
    val error: String? = null,
)

/**
 * Result of PQL query validation
 */
data class PQLValidationResult(
    val valid: Boolean,
    val query: String,
    val cypherQuery: String? = null,
    val message: String,
    val error: String? = null,
)
