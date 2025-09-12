package com.processm.processminterpreter.xes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = ["spring.neo4j.uri=bolt://localhost:7687"])
class XESParserTest {
    private val xesParser = XESParser()

    @Test
    fun `should parse sample XES file successfully`() {
        // Given
        val resourcePath = "logs/sample_process.xes"
        val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
        assertNotNull(inputStream, "Sample XES file should exist in resources")

        // When
        val result =
            inputStream!!.use { stream ->
                xesParser.parseXES(stream, "test-log-001")
            }

        // Then
        assertNotNull(result)
        assertEquals("test-log-001", result.logNode.logId)
        assertEquals("Sample Process Log", result.logNode.name)
        assertEquals(3, result.traces.size)

        // Verify first trace
        val firstTrace = result.traces[0]
        assertEquals("Case_001", firstTrace.traceNode.caseId)
        assertEquals("test-log-001-trace-Case_001", firstTrace.traceNode.traceId)
        assertEquals(4, firstTrace.events.size)

        // Verify first event
        val firstEvent = firstTrace.events[0]
        assertEquals("Register Request", firstEvent.eventNode.activity)
        assertEquals("John Doe", firstEvent.eventNode.resource)
        assertEquals("complete", firstEvent.eventNode.lifecycle)
        assertEquals(50.0, firstEvent.eventNode.cost)

        println("[DEBUG_LOG] Successfully parsed XES file:")
        println("[DEBUG_LOG] - Log ID: ${result.logNode.logId}")
        println("[DEBUG_LOG] - Log Name: ${result.logNode.name}")
        println("[DEBUG_LOG] - Traces: ${result.traces.size}")
        println("[DEBUG_LOG] - Total Events: ${result.traces.sumOf { it.events.size }}")

        result.traces.forEachIndexed { traceIndex, trace ->
            println("[DEBUG_LOG] - Trace ${traceIndex + 1}: ${trace.traceNode.caseId} (${trace.events.size} events)")
            trace.events.forEachIndexed { eventIndex, event ->
                println("[DEBUG_LOG]   - Event ${eventIndex + 1}: ${event.eventNode.activity} by ${event.eventNode.resource}")
            }
        }
    }

    @Test
    fun `should handle missing log ID by generating one`() {
        // Given
        val resourcePath = "logs/sample_process.xes"
        val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
        assertNotNull(inputStream, "Sample XES file should exist in resources")

        // When
        val result =
            inputStream!!.use { stream ->
                xesParser.parseXES(stream, null) // No logId provided
            }

        // Then
        assertNotNull(result)
        assertTrue(result.logNode.logId.startsWith("log-"))
        assertEquals("Sample Process Log", result.logNode.name)

        println("[DEBUG_LOG] Generated log ID: ${result.logNode.logId}")
    }

    @Test
    fun `should parse all trace attributes correctly`() {
        // Given
        val resourcePath = "logs/sample_process.xes"
        val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
        assertNotNull(inputStream, "Sample XES file should exist in resources")

        // When
        val result =
            inputStream!!.use { stream ->
                xesParser.parseXES(stream, "test-log-002")
            }

        // Then
        val secondTrace = result.traces[1] // Case_002
        assertEquals("Case_002", secondTrace.traceNode.caseId)
        assertEquals("Variant_B", secondTrace.traceNode.attributes["case:variant"])
        assertEquals(2, secondTrace.traceNode.attributes["case:priority"])

        println("[DEBUG_LOG] Trace attributes for ${secondTrace.traceNode.caseId}:")
        secondTrace.traceNode.attributes.forEach { (key, value) ->
            println("[DEBUG_LOG] - $key: $value (${value::class.simpleName})")
        }
    }

    @Test
    fun `should parse all event attributes correctly`() {
        // Given
        val resourcePath = "logs/sample_process.xes"
        val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
        assertNotNull(inputStream, "Sample XES file should exist in resources")

        // When
        val result =
            inputStream!!.use { stream ->
                xesParser.parseXES(stream, "test-log-003")
            }

        // Then
        val firstEvent = result.traces[0].events[0]
        assertEquals("Register Request", firstEvent.eventNode.activity)
        assertEquals("John Doe", firstEvent.eventNode.resource)
        assertEquals("Reception", firstEvent.eventNode.attributes["org:group"])
        assertEquals(50.0, firstEvent.eventNode.cost)
        assertNotNull(firstEvent.eventNode.timestamp)

        println("[DEBUG_LOG] Event attributes for ${firstEvent.eventNode.activity}:")
        firstEvent.eventNode.attributes.forEach { (key, value) ->
            println("[DEBUG_LOG] - $key: $value (${value::class.simpleName})")
        }
    }

    @Test
    fun `should generate unique IDs for traces and events`() {
        // Given
        val resourcePath = "logs/sample_process.xes"
        val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
        assertNotNull(inputStream, "Sample XES file should exist in resources")

        // When
        val result =
            inputStream!!.use { stream ->
                xesParser.parseXES(stream, "test-log-004")
            }

        // Then
        val allTraceIds = result.traces.map { it.traceNode.traceId }
        val allEventIds =
            result.traces.flatMap { trace ->
                trace.events.map { it.eventNode.eventId }
            }

        // Check uniqueness
        assertEquals(allTraceIds.size, allTraceIds.toSet().size, "All trace IDs should be unique")
        assertEquals(allEventIds.size, allEventIds.toSet().size, "All event IDs should be unique")

        println("[DEBUG_LOG] Generated IDs:")
        println("[DEBUG_LOG] - Trace IDs: $allTraceIds")
        println("[DEBUG_LOG] - Event IDs (first 5): ${allEventIds.take(5)}")
    }
}
