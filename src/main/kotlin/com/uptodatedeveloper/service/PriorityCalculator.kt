package com.uptodatedeveloper.service

import com.uptodatedeveloper.domain.Priority
import com.uptodatedeveloper.domain.RssEntry
import org.slf4j.LoggerFactory

/**
 * Determines the priority level of RSS entries based on content analysis
 */
class PriorityCalculator {
    private val logger = LoggerFactory.getLogger(PriorityCalculator::class.java)

    /**
     * Determines priority from entry title and description
     */
    fun calculate(entry: RssEntry): Priority {
        val content = "${entry.title} ${entry.description}".lowercase()

        return when {
            // HIGH: release, stable, breaking, major version updates
            content.contains("release") && (content.contains("version") || content.contains("stable")) -> Priority.HIGH
            content.contains("breaking") -> Priority.HIGH
            content.contains("major version") -> Priority.HIGH
            content.contains("v2.0") || content.contains("v3.0") || content.contains("v4.0") -> Priority.HIGH
            content.contains("shipped") && content.contains("release") -> Priority.HIGH

            // MEDIUM: beta, preview, milestone, rc (release candidate)
            content.contains("beta") -> Priority.MEDIUM
            content.contains("preview") -> Priority.MEDIUM
            content.contains("milestone") -> Priority.MEDIUM
            content.contains("rc") || content.contains("release candidate") -> Priority.MEDIUM
            content.contains("alpha") -> Priority.MEDIUM

            // LOW: everything else (default fallback)
            else -> Priority.LOW
        }
    }

    /**
     * Calculate priority for multiple entries
     */
    fun calculateAll(entries: List<RssEntry>): List<RssEntry> {
        return entries.map { entry ->
            val priority = calculate(entry)
            entry.copy(priority = priority)
        }
    }
}
