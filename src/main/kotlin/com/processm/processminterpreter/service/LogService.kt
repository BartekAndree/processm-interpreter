package com.processm.processminterpreter.service

import com.processm.processminterpreter.model.LogNode
import com.processm.processminterpreter.repository.LogRepository
import com.processm.processminterpreter.repository.LogStatistics
import com.processm.processminterpreter.repository.LogWithStatistics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing process logs
 *
 * Provides business logic for CRUD operations on logs,
 * validation, and error handling.
 */
@Service
@Transactional
class LogService(
    private val logRepository: LogRepository,
) {

    private val logger = LoggerFactory.getLogger(LogService::class.java)

    /**
     * Create a new log
     */
    fun createLog(
        logId: String,
        name: String,
        attributes: Map<String, Any> = emptyMap(),
    ): LogNode {
        logger.info("Creating new log with logId: $logId, name: $name")

        // Validate input
        require(logId.isNotBlank()) { "Log ID cannot be blank" }
        require(name.isNotBlank()) { "Log name cannot be blank" }

        // Check if log already exists
        if (logRepository.existsByLogId(logId)) {
            throw IllegalArgumentException("Log with ID '$logId' already exists")
        }

        val log = LogNode(
            logId = logId,
            name = name,
            attributes = attributes,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        val savedLog = logRepository.save(log)
        logger.info("Successfully created log with ID: ${savedLog.logId}")

        return savedLog
    }

    /**
     * Get log by ID
     */
    @Transactional(readOnly = true)
    fun getLogById(logId: String): LogNode {
        logger.debug("Retrieving log with ID: $logId")

        return logRepository.findByLogId(logId)
            ?: throw LogNotFoundException("Log with ID '$logId' not found")
    }

    /**
     * Get log with statistics
     */
    @Transactional(readOnly = true)
    fun getLogWithStatistics(logId: String): LogStatistics {
        logger.debug("Retrieving log statistics for ID: $logId")

        return logRepository.getLogStatistics(logId)
            ?: throw LogNotFoundException("Log with ID '$logId' not found")
    }

    /**
     * Get all logs
     */
    @Transactional(readOnly = true)
    fun getAllLogs(): List<LogNode> {
        logger.debug("Retrieving all logs")
        return logRepository.findAll().toList()
    }

    /**
     * Get all logs with statistics
     */
    @Transactional(readOnly = true)
    fun getAllLogsWithStatistics(): List<LogWithStatistics> {
        logger.debug("Retrieving all logs with statistics")
        return logRepository.findAllWithStatistics()
    }

    /**
     * Search logs by name
     */
    @Transactional(readOnly = true)
    fun searchLogsByName(name: String): List<LogNode> {
        logger.debug("Searching logs by name: $name")
        require(name.isNotBlank()) { "Search name cannot be blank" }

        return logRepository.findByNameContainingIgnoreCase(name)
    }

    /**
     * Get logs created after specific date
     */
    @Transactional(readOnly = true)
    fun getLogsCreatedAfter(date: LocalDateTime): List<LogNode> {
        logger.debug("Retrieving logs created after: $date")
        return logRepository.findByCreatedAtAfter(date)
    }

    /**
     * Get logs created between dates
     */
    @Transactional(readOnly = true)
    fun getLogsCreatedBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<LogNode> {
        logger.debug("Retrieving logs created between: $startDate and $endDate")
        require(startDate.isBefore(endDate)) { "Start date must be before end date" }

        return logRepository.findByCreatedAtBetween(startDate, endDate)
    }

    /**
     * Update log
     */
    fun updateLog(
        logId: String,
        name: String? = null,
        attributes: Map<String, Any>? = null,
    ): LogNode {
        logger.info("Updating log with ID: $logId")

        val existingLog = getLogById(logId)

        val updatedLog = existingLog.copy(
            name = name ?: existingLog.name,
            attributes = attributes ?: existingLog.attributes,
            updatedAt = LocalDateTime.now(),
        )

        val savedLog = logRepository.save(updatedLog)
        logger.info("Successfully updated log with ID: $logId")

        return savedLog
    }

    /**
     * Delete log by ID
     */
    fun deleteLog(logId: String): Boolean {
        logger.info("Deleting log with ID: $logId")

        if (!logRepository.existsByLogId(logId)) {
            throw LogNotFoundException("Log with ID '$logId' not found")
        }

        val deletedCount = logRepository.deleteByLogId(logId)
        val success = deletedCount > 0

        if (success) {
            logger.info("Successfully deleted log with ID: $logId")
        } else {
            logger.warn("Failed to delete log with ID: $logId")
        }

        return success
    }

    /**
     * Delete log with all related data (traces and events)
     */
    fun deleteLogWithAllData(logId: String): Boolean {
        logger.info("Deleting log with all data for ID: $logId")

        if (!logRepository.existsByLogId(logId)) {
            throw LogNotFoundException("Log with ID '$logId' not found")
        }

        val deletedCount = logRepository.deleteLogWithAllData(logId)
        val success = deletedCount > 0

        if (success) {
            logger.info("Successfully deleted log with all data for ID: $logId")
        } else {
            logger.warn("Failed to delete log with all data for ID: $logId")
        }

        return success
    }

    /**
     * Check if log exists
     */
    @Transactional(readOnly = true)
    fun logExists(logId: String): Boolean {
        return logRepository.existsByLogId(logId)
    }

    /**
     * Find logs by attribute
     */
    @Transactional(readOnly = true)
    fun findLogsByAttribute(attributeKey: String, attributeValue: Any): List<LogNode> {
        logger.debug("Finding logs by attribute: $attributeKey = $attributeValue")
        require(attributeKey.isNotBlank()) { "Attribute key cannot be blank" }

        return logRepository.findByAttribute(attributeKey, attributeValue)
    }

    /**
     * Generate unique log ID
     */
    fun generateLogId(): String {
        var logId: String
        do {
            logId = "log-${UUID.randomUUID().toString().substring(0, 8)}"
        } while (logRepository.existsByLogId(logId))

        return logId
    }
}

/**
 * Exception thrown when log is not found
 */
class LogNotFoundException(message: String) : RuntimeException(message)
