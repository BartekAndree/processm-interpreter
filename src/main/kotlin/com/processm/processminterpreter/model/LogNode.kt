package com.processm.processminterpreter.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import java.time.LocalDateTime

/**
 * Neo4j Node representing a process log (XES log)
 *
 * A log contains multiple traces and represents a complete event log
 * from a business process execution.
 */
@Node("Log")
data class LogNode(
    @Id @GeneratedValue
    val id: Long? = null,

    /**
     * Unique identifier for the log (identity:id from XES)
     */
    val logId: String,

    /**
     * Human-readable name of the log
     */
    val name: String,

    /**
     * Timestamp when the log was created/imported
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when the log was last modified
     */
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Additional attributes from XES log (stored as JSON-like map)
     * Common attributes: concept:name, lifecycle:model, etc.
     */
    val attributes: Map<String, Any> = emptyMap(),

    /**
     * Relationship to traces contained in this log
     * Note: Relationships will be added after all entities are defined
     */
    // @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    // val traces: Set<TraceNode> = emptySet()
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
     * Get lifecycle:model attribute (standard XES attribute)
     */
    fun getLifecycleModel(): String? = getAttribute<String>("lifecycle:model")
}
