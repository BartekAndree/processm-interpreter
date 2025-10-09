package com.processm.processminterpreter.xes

import com.fasterxml.jackson.databind.ObjectMapper
import com.processm.processminterpreter.model.EventNode
import com.processm.processminterpreter.service.LogService
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.LocalDateTime

/**
 * Service for loading XES files into Neo4j database
 *
 * Handles parsing XES files and storing them with proper relationships
 */
@Service
@Transactional
class XESLoader(
    private val xesParser: XESParser,
    private val logService: LogService,
    private val neo4jDriver: Driver,
) {

    private val logger = LoggerFactory.getLogger(XESLoader::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * Sanitize attributes to be Neo4j-compatible.
     * Replaces colons in keys and converts complex values to JSON strings.
     */
    private fun sanitizeAttributes(attributes: Map<String, Any>): Map<String, Any> {
        return attributes.mapKeys { it.key.replace(":", "_").replace(".", "_") }
            .mapValues { (_, value) ->
                when (value) {
                    is Map<*, *>, is Collection<*> -> objectMapper.writeValueAsString(value)
                    else -> value
                }
            }
    }

    /**
     * Load XES file into Neo4j database
     */
    fun loadXESFile(inputStream: InputStream, logId: String? = null): XESLoadResult {
        logger.info("Starting XES file loading for logId: $logId")

        return try {
            // Parse XES file
            val xesLog = xesParser.parseXES(inputStream, logId)

            // Save to Neo4j with relationships
            val savedLogId = saveXESLogToNeo4j(xesLog)

            XESLoadResult(
                success = true,
                logId = savedLogId,
                tracesCount = xesLog.traces.size,
                eventsCount = xesLog.traces.sumOf { it.events.size },
                message = "XES file loaded successfully",
            )
        } catch (e: Exception) {
            logger.error("Error loading XES file", e)
            XESLoadResult(
                success = false,
                error = e.cause?.message ?: e.message ?: "Unknown error occurred",
                message = "Failed to load XES file",
            )
        }
    }

    /**
     * Save parsed XES log to Neo4j with relationships
     */
    private fun saveXESLogToNeo4j(xesLog: XESLog): String {
        logger.info("Saving XES log to Neo4j: ${xesLog.logNode.logId}")

        return neo4jDriver.session().use { session ->
            session.writeTransaction { tx ->
                // Create log node
                val createLogQuery = """
                    CREATE (log:Log {
                        logId: ${'$'}logId,
                        name: ${'$'}name,
                        createdAt: ${'$'}createdAt,
                        updatedAt: ${'$'}updatedAt
                    })
                    SET log += ${'$'}attributes
                    RETURN log.logId as logId
                """

                val logResult = tx.run(
                    createLogQuery,
                    mapOf(
                        "logId" to xesLog.logNode.logId,
                        "name" to xesLog.logNode.name,
                        "createdAt" to xesLog.logNode.createdAt,
                        "updatedAt" to xesLog.logNode.updatedAt,
                        "attributes" to sanitizeAttributes(xesLog.logNode.attributes),
                    ),
                )

                val savedLogId = logResult.single().get("logId").asString()
                logger.debug("Created log node: $savedLogId")

                // Create traces and events with relationships
                xesLog.traces.forEach { xesTrace ->
                    createTraceWithEvents(tx, savedLogId, xesTrace)
                }

                return@writeTransaction savedLogId
            }
        }
    }

    /**
     * Create trace node with events and relationships
     */
    private fun createTraceWithEvents(tx: org.neo4j.driver.Transaction, logId: String, xesTrace: XESTrace) {
        val trace = xesTrace.traceNode

        // Create trace node and relationship to log
        val createTraceQuery = """
            MATCH (log:Log {logId: ${'$'}logId})
            CREATE (trace:Trace {
                traceId: ${'$'}traceId,
                caseId: ${'$'}caseId,
                createdAt: ${'$'}createdAt
            })
            SET trace += ${'$'}attributes
            CREATE (log)-[:CONTAINS]->(trace)
            RETURN trace.traceId as traceId
        """

        val traceResult = tx.run(
            createTraceQuery,
            mapOf(
                "logId" to logId,
                "traceId" to trace.traceId,
                "caseId" to trace.caseId,
                "createdAt" to trace.createdAt,
                "attributes" to sanitizeAttributes(trace.attributes),
            ),
        )

        val savedTraceId = traceResult.single().get("traceId").asString()
        logger.debug("Created trace node: $savedTraceId")

        // Create events with relationships
        var previousEventId: String? = null

        xesTrace.events.forEachIndexed { index, xesEvent ->
            val eventId = createEvent(tx, savedTraceId, xesEvent.eventNode)

            // Create FOLLOWS relationship between consecutive events
            if (previousEventId != null) {
                createFollowsRelationship(tx, previousEventId!!, eventId)
            }

            previousEventId = eventId
        }
    }

    /**
     * Create event node with relationship to trace
     */
    private fun createEvent(tx: org.neo4j.driver.Transaction, traceId: String, event: EventNode): String {
        val createEventQuery = """
            MATCH (trace:Trace {traceId: ${'$'}traceId})
            CREATE (event:Event {
                eventId: ${'$'}eventId,
                activity: ${'$'}activity,
                timestamp: ${'$'}timestamp,
                resource: ${'$'}resource,
                lifecycle: ${'$'}lifecycle,
                cost: ${'$'}cost,
                createdAt: ${'$'}createdAt
            })
            SET event += ${'$'}attributes
            CREATE (trace)-[:HAS_EVENT]->(event)
            RETURN event.eventId as eventId
        """

        val eventResult = tx.run(
            createEventQuery,
            mapOf(
                "traceId" to traceId,
                "eventId" to event.eventId,
                "activity" to event.activity,
                "timestamp" to event.timestamp,
                "resource" to event.resource,
                "lifecycle" to event.lifecycle,
                "cost" to event.cost,
                "createdAt" to event.createdAt,
                "attributes" to sanitizeAttributes(event.attributes),
            ),
        )

        val savedEventId = eventResult.single().get("eventId").asString()
        logger.debug("Created event node: $savedEventId")

        return savedEventId
    }

    /**
     * Create FOLLOWS relationship between events
     */
    private fun createFollowsRelationship(tx: org.neo4j.driver.Transaction, fromEventId: String, toEventId: String) {
        val createFollowsQuery = """
            MATCH (from:Event {eventId: ${'$'}fromEventId})
            MATCH (to:Event {eventId: ${'$'}toEventId})
            CREATE (from)-[:FOLLOWS]->(to)
        """

        tx.run(
            createFollowsQuery,
            mapOf(
                "fromEventId" to fromEventId,
                "toEventId" to toEventId,
            ),
        )

        logger.debug("Created FOLLOWS relationship: $fromEventId -> $toEventId")
    }

    /**
     * Load XES file from classpath resource
     */
    fun loadXESFromResource(resourcePath: String, logId: String? = null): XESLoadResult {
        logger.info("Loading XES from resource: $resourcePath")

        return try {
            val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException("Resource not found: $resourcePath")

            inputStream.use { stream ->
                loadXESFile(stream, logId)
            }
        } catch (e: Exception) {
            logger.error("Error loading XES from resource: $resourcePath", e)
            XESLoadResult(
                success = false,
                error = e.cause?.message ?: e.message ?: "Unknown error occurred",
                message = "Failed to load XES from resource",
            )
        }
    }

    /**
     * Get loading statistics
     */
    fun getLoadingStatistics(): Map<String, Any> {
        // TODO: Implement loading statistics tracking
        return mapOf(
            "totalLoadsAttempted" to 0,
            "successfulLoads" to 0,
            "failedLoads" to 0,
            "totalTracesLoaded" to 0,
            "totalEventsLoaded" to 0,
        )
    }
}

/**
 * Result of XES loading operation
 */
data class XESLoadResult(
    val success: Boolean,
    val logId: String? = null,
    val tracesCount: Int = 0,
    val eventsCount: Int = 0,
    val message: String,
    val error: String? = null,
    val filename: String? = null, // Added for context in the UI
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
