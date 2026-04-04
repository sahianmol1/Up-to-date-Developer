package com.uptodatedeveloper.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.uptodatedeveloper.domain.FeedType
import com.uptodatedeveloper.domain.RssEntry
import com.uptodatedeveloper.domain.SentEntryLog
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Manages deduplication of RSS entries using separate JSON files per feed type
 * Files are organized as: sent_entries_<feedtype>.json
 * Example: sent_entries_kotlin.json, sent_entries_android.json
 */
class DuplicateDetector(
    private val baseLogDir: String = "."
) {
    private val logger = LoggerFactory.getLogger(DuplicateDetector::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    // Separate tracking per feed type
    private val sentEntriesByFeedType = mutableMapOf<FeedType, MutableSet<String>>()

    init {
        loadSentEntries()
    }

    /**
     * Filters out entries that have already been sent for their respective feed type
     */
    fun filterNew(entries: List<RssEntry>): List<RssEntry> {
        val newEntries = entries.filter { entry ->
            val feedTypeSentLinks = sentEntriesByFeedType.getOrDefault(entry.feedType, mutableSetOf())
            !feedTypeSentLinks.contains(entry.link)
        }
        logger.debug("Detected ${entries.size - newEntries.size} duplicates from ${entries.size} entries")
        return newEntries
    }

    /**
     * Records entries as sent, grouped by feed type
     */
    fun markAsSent(entries: List<RssEntry>) {
        // Group entries by feed type
        val groupedByFeedType = entries.groupBy { it.feedType }
        
        groupedByFeedType.forEach { (feedType, feedTypeEntries) ->
            // Update in-memory tracking
            val feedTypeSentLinks = sentEntriesByFeedType.getOrPut(feedType) { mutableSetOf() }
            feedTypeEntries.forEach { entry ->
                feedTypeSentLinks.add(entry.link)
            }
            
            // Persist to feed-type-specific file
            saveSentEntriesByFeedType(feedType, feedTypeEntries)
        }
    }

    /**
     * Loads previously sent entries from JSON log files for all feed types
     */
    private fun loadSentEntries() {
        FeedType.values().forEach { feedType ->
            val file = File(getLogFilePathForFeedType(feedType))
            if (!file.exists()) {
                logger.info("Sent entries log file not found: ${file.name}. Starting fresh for ${feedType.displayName}.")
                sentEntriesByFeedType[feedType] = mutableSetOf()
                return@forEach
            }

            try {
                val content = file.readText().trim()
                // Handle empty or whitespace-only files
                if (content.isEmpty() || content == "[]") {
                    logger.info("Sent entries log is empty for ${feedType.displayName}. Starting fresh.")
                    sentEntriesByFeedType[feedType] = mutableSetOf()
                    return@forEach
                }
                
                val logs = objectMapper.readValue(content, Array<SentEntryLog>::class.java)
                val links = logs.map { it.link }.toMutableSet()
                sentEntriesByFeedType[feedType] = links
                logger.info("Loaded ${links.size} previously sent ${feedType.displayName} entries")
            } catch (e: Exception) {
                logger.error("Error loading sent entries log for ${feedType.displayName}", e)
                sentEntriesByFeedType[feedType] = mutableSetOf()
            }
        }
    }

    /**
     * Saves newly sent entries to the feed-type-specific JSON log file
     */
    private fun saveSentEntriesByFeedType(feedType: FeedType, entries: List<RssEntry>) {
        try {
            val filePath = getLogFilePathForFeedType(feedType)
            val file = File(filePath)
            
            // Load existing entries from this feed type's file
            val existing = if (file.exists()) {
                val content = file.readText().trim()
                // Handle empty or whitespace-only files
                if (content.isEmpty() || content == "[]") {
                    mutableListOf()
                } else {
                    objectMapper.readValue(content, Array<SentEntryLog>::class.java).toMutableList()
                }
            } else {
                mutableListOf()
            }

            // Add new entries
            val newLogs = entries.map { entry ->
                SentEntryLog(
                    link = entry.link,
                    sentAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    title = entry.title
                )
            }
            existing.addAll(newLogs)

            // Keep only last 1000 entries per feed type for performance
            val trimmed = if (existing.size > 1000) {
                existing.takeLast(1000)
            } else {
                existing
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, trimmed)
            logger.debug("Saved ${entries.size} new ${feedType.displayName} entries to $filePath (total: ${trimmed.size})")
        } catch (e: Exception) {
            logger.error("Error saving sent entries for ${feedType.displayName}", e)
        }
    }

    /**
     * Generates the log file path for a specific feed type
     * Format: sent_entries_<feedtype_lowercase>.json
     */
    private fun getLogFilePathForFeedType(feedType: FeedType): String {
        val feedTypeName = feedType.displayName.lowercase()
        return if (baseLogDir == ".") {
            "sent_entries_$feedTypeName.json"
        } else {
            "$baseLogDir/sent_entries_$feedTypeName.json"
        }
    }

    /**
     * Returns the count of tracked entries for a specific feed type
     */
    fun getTrackedCount(feedType: FeedType): Int = sentEntriesByFeedType[feedType]?.size ?: 0

    /**
     * Returns the count of tracked entries across all feed types
     */
    fun getTotalTrackedCount(): Int = sentEntriesByFeedType.values.sumOf { it.size }
}
