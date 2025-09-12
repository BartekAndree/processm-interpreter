package com.processm.processminterpreter.controller

import com.processm.processminterpreter.dto.ErrorResponse
import com.processm.processminterpreter.service.PQLQueryService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * REST Controller for PQL query operations
 *
 * Provides HTTP endpoints for executing and validating PQL queries
 */
@RestController
@RequestMapping("/query")
@CrossOrigin(origins = ["*"])
class PQLQueryController(
    private val pqlQueryService: PQLQueryService,
) {
    private val logger = LoggerFactory.getLogger(PQLQueryController::class.java)

    /**
     * Execute PQL query
     * POST /api/query/execute
     */
    @PostMapping("/execute")
    fun executeQuery(
        @RequestBody request: PQLQueryRequest,
    ): ResponseEntity<PQLQueryResponse> {
        logger.info("Executing PQL query: ${request.query}")

        return try {
            val result = pqlQueryService.executePQLQuery(request.query, request.logId)

            val response =
                PQLQueryResponse(
                    success = result.success,
                    query = result.query,
                    cypherQuery = result.cypherQuery,
                    results = result.results,
                    resultCount = result.resultCount,
                    executionTimeMs = result.executionTimeMs,
                    error = result.error,
                    timestamp = LocalDateTime.now(),
                )

            if (result.success) {
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.badRequest().body(response)
            }
        } catch (e: Exception) {
            logger.error("Unexpected error executing PQL query", e)
            ResponseEntity.internalServerError().body(
                PQLQueryResponse(
                    success = false,
                    query = request.query,
                    error = "Internal server error: ${e.message}",
                    results = emptyList(),
                    resultCount = 0,
                    executionTimeMs = 0,
                    timestamp = LocalDateTime.now(),
                ),
            )
        }
    }

    /**
     * Validate PQL query syntax
     * POST /api/query/validate
     */
    @PostMapping("/validate")
    fun validateQuery(
        @RequestBody request: PQLValidationRequest,
    ): ResponseEntity<PQLValidationResponse> {
        logger.debug("Validating PQL query: ${request.query}")

        return try {
            val result = pqlQueryService.validatePQLQuery(request.query)

            val response =
                PQLValidationResponse(
                    valid = result.valid,
                    query = result.query,
                    cypherQuery = result.cypherQuery,
                    message = result.message,
                    error = result.error,
                    timestamp = LocalDateTime.now(),
                )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Unexpected error validating PQL query", e)
            ResponseEntity.internalServerError().body(
                PQLValidationResponse(
                    valid = false,
                    query = request.query,
                    message = "Validation failed",
                    error = "Internal server error: ${e.message}",
                    timestamp = LocalDateTime.now(),
                ),
            )
        }
    }

    /**
     * Get query execution statistics
     * GET /api/query/statistics
     */
    @GetMapping("/statistics")
    fun getQueryStatistics(): ResponseEntity<Map<String, Any>> {
        logger.debug("Retrieving query statistics")

        return try {
            val statistics = pqlQueryService.getQueryStatistics()
            ResponseEntity.ok(statistics)
        } catch (e: Exception) {
            logger.error("Error retrieving query statistics", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * Get supported PQL features
     * GET /api/query/features
     */
    @GetMapping("/features")
    fun getSupportedFeatures(): ResponseEntity<PQLFeaturesResponse> {
        logger.debug("Retrieving supported PQL features")

        val features =
            PQLFeaturesResponse(
                supportedClauses = listOf("SELECT", "FROM", "WHERE"),
                supportedOperators = listOf("=", "!=", "<>", "<", ">", "<=", ">=", "LIKE"),
                supportedEntities = listOf("log", "trace", "event"),
                supportedFields =
                mapOf(
                    "log" to listOf("id", "logId", "name", "createdAt", "updatedAt", "attributes"),
                    "trace" to listOf("id", "traceId", "caseId", "createdAt", "attributes"),
                    "event" to
                        listOf(
                            "id",
                            "eventId",
                            "activity",
                            "timestamp",
                            "resource",
                            "lifecycle",
                            "cost",
                            "createdAt",
                            "attributes",
                        ),
                ),
                limitations =
                listOf(
                    "GROUP BY not yet supported",
                    "Aggregation functions not yet supported",
                    "Complex joins not yet supported",
                    "Subqueries not yet supported",
                ),
                examples =
                listOf(
                    "SELECT * FROM log",
                    "SELECT * FROM trace WHERE caseId = 'case-123'",
                    "SELECT activity, timestamp FROM event WHERE activity = 'Task A'",
                    "SELECT * FROM event WHERE resource LIKE 'John' AND timestamp > '2023-01-01'",
                ),
            )

        return ResponseEntity.ok(features)
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception in PQLQueryController", e)

        val errorResponse =
            ErrorResponse(
                error = e.javaClass.simpleName,
                message = e.message ?: "An unexpected error occurred",
            )

        return ResponseEntity.internalServerError().body(errorResponse)
    }
}

/**
 * DTO for PQL query execution request
 */
data class PQLQueryRequest(
    val query: String,
    val logId: String? = null,
    val timeout: Long? = null,
    val maxResults: Int? = null,
)

/**
 * DTO for PQL query execution response
 */
data class PQLQueryResponse(
    val success: Boolean,
    val query: String,
    val cypherQuery: String? = null,
    val results: List<Map<String, Any?>> = emptyList(),
    val resultCount: Int = 0,
    val executionTimeMs: Long = 0,
    val error: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)

/**
 * DTO for PQL query validation request
 */
data class PQLValidationRequest(
    val query: String,
)

/**
 * DTO for PQL query validation response
 */
data class PQLValidationResponse(
    val valid: Boolean,
    val query: String,
    val cypherQuery: String? = null,
    val message: String,
    val error: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)

/**
 * DTO for supported PQL features
 */
data class PQLFeaturesResponse(
    val supportedClauses: List<String>,
    val supportedOperators: List<String>,
    val supportedEntities: List<String>,
    val supportedFields: Map<String, List<String>>,
    val limitations: List<String>,
    val examples: List<String>,
)
