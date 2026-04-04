package com.uptodatedeveloper.service

import com.uptodatedeveloper.domain.RssEntry
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Filters RSS entries based on time window and keywords
 */
class RssEntryFilter(
    private val hoursWindow: Long = 24
) {
    private val logger = LoggerFactory.getLogger(RssEntryFilter::class.java)

    /**
     * Filters entries to include only those from the last N hours
     */
    fun filterByTimeWindow(entries: List<RssEntry>): List<RssEntry> {
        val cutoffTime = Instant.now().minus(hoursWindow, ChronoUnit.HOURS)
        val filtered = entries.filter { it.publishedDate.isAfter(cutoffTime) }
        logger.debug("Filtered ${entries.size} entries to ${filtered.size} within last $hoursWindow hours")
        return filtered
    }

    /**
     * Filters entries by keywords (case-insensitive)
     * If keywords is empty, all entries pass through
     */
    fun filterByKeywords(
        entries: List<RssEntry>,
        keywords: List<String> = emptyList()
    ): List<RssEntry> {
        if (keywords.isEmpty()) return entries

        val lowerKeywords = keywords.map { it.lowercase() }
        val filtered = entries.filter { entry ->
            val title = entry.title.lowercase()
            val description = entry.description.lowercase()
            lowerKeywords.any { keyword ->
                title.contains(keyword) || description.contains(keyword)
            }
        }

        logger.debug("Filtered ${entries.size} entries to ${filtered.size} by keywords: $keywords")
        return filtered
    }

    /**
     * Applies all filters in sequence
     */
    fun filter(
        entries: List<RssEntry>,
        keywords: List<String> = emptyList()
    ): List<RssEntry> {
        var filtered = filterByTimeWindow(entries)
        filtered = filterByKeywords(filtered, keywords)
        return filtered
    }
}
