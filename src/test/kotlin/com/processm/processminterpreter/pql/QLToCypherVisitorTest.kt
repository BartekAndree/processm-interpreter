package com.processm.processminterpreter.pql

import QLLexer
import QLParser
import com.processm.processminterpreter.pql.visitor.PQLErrorListener
import com.processm.processminterpreter.pql.visitor.QLToCypherVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Integration tests for QLToCypherVisitor
 * Tests ProcessM PQL queries and their translation to Neo4j Cypher
 */
class QLToCypherVisitorTest {

    private fun translateQuery(pqlQuery: String, logId: String? = null): CypherQuery {
        // Parse PQL query
        val input = CharStreams.fromString(pqlQuery)
        val lexer = QLLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = QLParser(tokens)

        // Add error listener
        val errorListener = PQLErrorListener()
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)

        // Parse and check for errors
        val tree = parser.query()
        errorListener.throwIfErrors()

        // Visit AST and generate Cypher
        val visitor = QLToCypherVisitor(logId)
        return visitor.visit(tree) as CypherQuery
    }

    // ========================================
    // BASIC SELECT TESTS
    // ========================================

    @Test
    fun `test simple select all events`() {
        val pql = "" // Implicit select all in ProcessM
        val result = translateQuery(pql)

        println("PQL: (empty - implicit select all)")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("MATCH"))
        assertTrue(result.query.contains("event"))
        assertTrue(result.query.contains("RETURN"))
    }

    @Test
    fun `test select all events with explicit asterisk`() {
        val pql = "select *"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("MATCH"))
        assertTrue(result.query.contains("RETURN"))
    }

    @Test
    fun `test select specific fields`() {
        val pql = "select event:activity, event:timestamp"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("MATCH"))
        assertTrue(result.query.contains("event.activity"))
        assertTrue(result.query.contains("event.timestamp"))
    }

    // ========================================
    // WHERE CLAUSE TESTS
    // ========================================

    @Test
    fun `test select with simple where clause`() {
        val pql = "where event:activity = 'Registration'"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("event.activity"))
        assertTrue(result.parameters.containsValue("Registration"))
    }

    @Test
    fun `test where with AND condition`() {
        val pql = "where event:activity = 'Registration' and event:resource = 'Nurse'"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("AND"))
        assertTrue(result.parameters.containsValue("Registration"))
        assertTrue(result.parameters.containsValue("Nurse"))
    }

    @Test
    fun `test where with OR condition`() {
        val pql = "where event:activity = 'Registration' or event:activity = 'Triage'"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("OR"))
        assertTrue(result.parameters.containsValue("Registration"))
        assertTrue(result.parameters.containsValue("Triage"))
    }

    @Test
    fun `test where with IN operator`() {
        val pql = "where event:activity in ('Registration', 'Triage', 'Consultation')"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("IN"))
        // Check that the IN list parameter exists
        assertTrue(result.parameters.values.any { it is List<*> })
    }

    @Test
    fun `test where with comparison operators`() {
        val pql = "where event:timestamp > 'D2020-01-01'"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains(">"))
        assertTrue(result.query.contains("event.timestamp"))
    }

    @Test
    fun `test where with NULL check`() {
        val pql = "where event:resource is not null"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("IS NOT NULL"))
    }

    // ========================================
    // ORDER BY / LIMIT / OFFSET TESTS
    // ========================================

    @Test
    fun `test select with order by`() {
        val pql = "order by event:timestamp asc"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("ORDER BY"))
        assertTrue(result.query.contains("event.timestamp"))
        assertTrue(result.query.contains("ASC"))
    }

    @Test
    fun `test select with limit`() {
        val pql = "limit 10"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("LIMIT 10"))
    }

    @Test
    fun `test select with offset and limit`() {
        val pql = "limit 10 offset 20" // Grammar requires limit before offset
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("SKIP 20"))
        assertTrue(result.query.contains("LIMIT 10"))
    }

    // ========================================
    // AGGREGATE FUNCTION TESTS
    // ========================================

    @Test
    fun `test count aggregation`() {
        val pql = "select count(event:id)"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("count"))
        assertTrue(result.query.contains("event.id"))
    }

    @Test
    fun `test select with group by`() {
        val pql = "select event:activity, count(event:id) group by event:activity"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("event.activity"))
        assertTrue(result.query.contains("count"))
    }

    // ========================================
    // SCALAR FUNCTION TESTS
    // ========================================

    @Test
    fun `test date function`() {
        val pql = "select year(event:timestamp)"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("year") || result.query.contains(".year"))
    }

    @Test
    fun `test string function`() {
        val pql = "select upper(event:activity)"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("toUpper") || result.query.contains("upper"))
    }

    // ========================================
    // ARITHMETIC EXPRESSION TESTS
    // ========================================

    @Test
    fun `test arithmetic expression`() {
        val pql = "select event:price * 1.2"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("event.price"))
        assertTrue(result.query.contains("*"))
    }

    // ========================================
    // DELETE QUERY TESTS
    // ========================================

    @Test
    fun `test simple delete`() {
        val pql = "delete event"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("DELETE"))
        assertTrue(result.query.contains("event"))
    }

    @Test
    fun `test delete with where clause`() {
        val pql = "delete event where event:timestamp < 'D2020-01-01'"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("DELETE"))
        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("event.timestamp"))
    }

    @Test
    fun `test delete with limit`() {
        val pql = "delete event where event:activity = 'Test' limit 5"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")

        assertTrue(result.query.contains("DELETE"))
        assertTrue(result.query.contains("LIMIT 5"))
    }

    // ========================================
    // LOG ID FILTERING TESTS
    // ========================================

    @Test
    fun `test select with logId parameter`() {
        val pql = "" // Implicit select all
        val result = translateQuery(pql, logId = "test-log-123")

        println("PQL: $pql (logId: test-log-123)")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("log"))
        assertTrue(result.query.contains("WHERE") || result.query.contains("logId"))
        assertTrue(result.parameters.containsKey("logId"))
        assertEquals("test-log-123", result.parameters["logId"])
    }

    // ========================================
    // COMPLEX QUERY TESTS
    // ========================================

    @Test
    fun `test complex query with multiple clauses`() {
        val pql = """
            select event:activity, count(event:id)
            where event:resource is not null and event:timestamp > 'D2020-01-01'
            group by event:activity
            order by count(event:id) desc
            limit 10
        """.trimIndent()

        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("IS NOT NULL"))
        assertTrue(result.query.contains("AND"))
        assertTrue(result.query.contains("ORDER BY"))
        assertTrue(result.query.contains("DESC"))
        assertTrue(result.query.contains("LIMIT 10"))
    }

    @Test
    fun `test org_group field mapping`() {
        val pql = "select event:org_group, event:activity where event:org_group = 'Radiotherapy' limit 5"
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        // Check that org_group is properly translated
        assertTrue(result.query.contains("event"))
        assertTrue(result.query.contains("org_group"))
    }

    @Test
    fun `test user query from UI`() {
        val pql = """
            select event:org_group, event:activity, count(event:id)
            where event:timestamp > 'D2005-01-01'
            and event:org_group in ('Radiotherapy', 'Obstetrics & Gynaecology clinic')
            group by event:org_group, event:activity
            order by count(event:id) desc
            limit 20
        """.trimIndent().replace("\n", " ")
        val result = translateQuery(pql)

        println("PQL: $pql")
        println("Cypher: ${result.query}")
        println("Params: ${result.parameters}")

        assertTrue(result.query.contains("WHERE"))
        assertTrue(result.query.contains("IN"))
        // Neo4j Cypher doesn't have explicit GROUP BY - it groups implicitly by non-aggregated fields in RETURN
        assertTrue(result.query.contains("count(event.id)"))
        assertTrue(result.query.contains("ORDER BY"))
        // Date should not have 'D' prefix
        assertEquals("2005-01-01", result.parameters["param0"])
    }
}
