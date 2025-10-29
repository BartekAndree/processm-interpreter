package com.processm.processminterpreter.pql

/**
 * Interface for PQL to Cypher translators
 *
 * Implementations can use different parsing strategies (regex, ANTLR, etc.)
 */
interface PQLTranslator {

    /**
     * Translate PQL query to Cypher query
     *
     * @param pqlQuery The PQL query string
     * @param logId Optional log ID to filter results by specific log
     * @return CypherQuery object containing the Cypher query string and parameters
     * @throws IllegalArgumentException if the PQL query is invalid
     */
    fun translateToCypher(pqlQuery: String, logId: String? = null): CypherQuery
}
