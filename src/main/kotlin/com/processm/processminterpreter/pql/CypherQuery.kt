package com.processm.processminterpreter.pql

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
