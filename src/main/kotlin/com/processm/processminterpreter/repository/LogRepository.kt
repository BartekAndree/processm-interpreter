package com.processm.processminterpreter.repository

import com.processm.processminterpreter.model.LogNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Neo4j Repository for LogNode operations
 *
 * Provides CRUD operations and custom queries for process logs
 */
@Repository
interface LogRepository : Neo4jRepository<LogNode, Long> {

    /**
     * Find log by unique logId
     */
    fun findByLogId(logId: String): LogNode?

    /**
     * Find logs by name (case-insensitive)
     */
    fun findByNameContainingIgnoreCase(name: String): List<LogNode>

    /**
     * Find logs created after specific date
     */
    fun findByCreatedAtAfter(date: LocalDateTime): List<LogNode>

    /**
     * Find logs created between dates
     */
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<LogNode>

    /**
     * Check if log with given logId exists
     */
    fun existsByLogId(logId: String): Boolean

    /**
     * Delete log by logId
     */
    fun deleteByLogId(logId: String): Long

    /**
     * Custom query to get log statistics
     */
    @Query(
        """
        MATCH (log:Log {logId: ${'$'}logId})
        OPTIONAL MATCH (log)-[:CONTAINS]->(trace:Trace)
        OPTIONAL MATCH (trace)-[:HAS_EVENT]->(event:Event)
        RETURN log, 
               count(DISTINCT trace) as traceCount,
               count(DISTINCT event) as eventCount
    """,
    )
    fun getLogStatistics(@Param("logId") logId: String): LogStatistics?

    /**
     * Custom query to find logs with specific attribute
     */
    @Query(
        """
        MATCH (log:Log)
        WHERE log.attributes[${'$'}attributeKey] = ${'$'}attributeValue
        RETURN log
    """,
    )
    fun findByAttribute(
        @Param("attributeKey") attributeKey: String,
        @Param("attributeValue") attributeValue: Any,
    ): List<LogNode>

    /**
     * Custom query to get all logs with basic statistics
     */
    @Query(
        """
        MATCH (log:Log)
        OPTIONAL MATCH (log)-[:CONTAINS]->(trace:Trace)
        OPTIONAL MATCH (trace)-[:HAS_EVENT]->(event:Event)
        RETURN log,
               count(DISTINCT trace) as traceCount,
               count(DISTINCT event) as eventCount
        ORDER BY log.createdAt DESC
    """,
    )
    fun findAllWithStatistics(): List<LogWithStatistics>

    /**
     * Custom query to delete log with all related data
     */
    @Query(
        """
        MATCH (log:Log {logId: ${'$'}logId})
        OPTIONAL MATCH (log)-[:CONTAINS]->(trace:Trace)
        OPTIONAL MATCH (trace)-[:HAS_EVENT]->(event:Event)
        DETACH DELETE log, trace, event
        RETURN count(log) as deletedLogs
    """,
    )
    fun deleteLogWithAllData(@Param("logId") logId: String): Long
}

/**
 * Data class for log statistics projection
 */
data class LogStatistics(
    val log: LogNode,
    val traceCount: Long,
    val eventCount: Long,
)

/**
 * Data class for log with statistics projection
 */
data class LogWithStatistics(
    val log: LogNode,
    val traceCount: Long,
    val eventCount: Long,
)
