package com.processm.processminterpreter.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import java.time.LocalDateTime

/**
 * Neo4j Node representing a process trace (XES trace)
 *
 * A trace represents a single case/instance of a business process
 * and contains a sequence of events.
 */
@Node("Trace")
data class TraceNode(
    @Id @GeneratedValue
    val id: Long? = null,

    /**
     * Unique identifier for the trace within the log
     */
    val traceId: String,

    /**
     * Case identifier (concept:name from XES trace)
     * This is the business identifier for the process instance
     */
    val caseId: String,

    /**
     * Timestamp when the trace was created/imported
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Additional attributes from XES trace (stored as JSON-like map)
     * Common attributes: concept:name, org:resource, etc.
     */
    val attributes: Map<String, Any> = emptyMap(),

    /**
     * Relationship to events in this trace
     * Note: Relationships will be added after all entities are defined
     */
    // @Relationship(type = "HAS_EVENT", direction = Relationship.Direction.OUTGOING)
    // val events: Set<EventNode> = emptySet()
) {
    /**
     * Get attribute value by key with type casting
     */
    inline fun <reified T> getAttribute(key: String): T? {
        return attributes[key] as? T
    }

    /**
     * Get concept:name attribute (standard XES attribute)
     */
    fun getConceptName(): String? = getAttribute<String>("concept:name")

    /**
     * Get org:resource attribute (standard XES attribute)
     */
    fun getResource(): String? = getAttribute<String>("org:resource")
}
