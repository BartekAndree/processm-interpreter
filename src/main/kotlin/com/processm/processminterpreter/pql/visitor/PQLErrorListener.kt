package com.processm.processminterpreter.pql.visitor

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.slf4j.LoggerFactory

/**
 * Custom error listener for PQL parsing
 *
 * Collects syntax errors during parsing and provides detailed error messages.
 */
class PQLErrorListener : BaseErrorListener() {

    private val logger = LoggerFactory.getLogger(PQLErrorListener::class.java)
    private val errors = mutableListOf<String>()

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?,
    ) {
        val error = "Syntax error at line $line:$charPositionInLine - $msg"
        logger.error(error)
        errors.add(error)
    }

    /**
     * Check if any errors were collected
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Get all collected errors
     */
    fun getErrors(): List<String> = errors.toList()

    /**
     * Throw exception if there are any errors
     */
    fun throwIfErrors() {
        if (hasErrors()) {
            throw IllegalArgumentException("PQL parsing failed:\n${errors.joinToString("\n")}")
        }
    }
}
