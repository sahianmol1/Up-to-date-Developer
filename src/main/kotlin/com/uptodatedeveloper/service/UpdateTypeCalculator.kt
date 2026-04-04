package com.uptodatedeveloper.service

import com.uptodatedeveloper.domain.RssEntry
import com.uptodatedeveloper.domain.UpdateType
import org.slf4j.LoggerFactory

/**
 * Detects the update type of RSS entries based on content analysis
 */
class UpdateTypeCalculator {
    private val logger = LoggerFactory.getLogger(UpdateTypeCalculator::class.java)

    /**
     * Detects update type from entry title and description
     */
    fun calculate(entry: RssEntry): UpdateType {
        val content = "${entry.title} ${entry.description}"
        return UpdateType.detect(content)
    }

    /**
     * Calculate update type for multiple entries
     */
    fun calculateAll(entries: List<RssEntry>): List<RssEntry> {
        return entries.map { entry ->
            val updateType = calculate(entry)
            entry.copy(updateType = updateType)
        }
    }
}
