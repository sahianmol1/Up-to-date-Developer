package com.uptodatedeveloper.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.uptodatedeveloper.domain.RssEntry
import com.uptodatedeveloper.domain.SentEntryLog
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Manages deduplication of RSS entries using a local JSON file
 */
class DuplicateDetector(
    private val logFilePath: String = "sent_entries.json"
) {
    private val logger = LoggerFactory.getLogger(DuplicateDetector::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private var sentEntries: MutableSet<String> = mutableSetOf()

    init {
        loadSentEntries()
    }

    /**
     * Filters out entries that have already been sent
     */
    fun filterNew(entries: List<RssEntry>): List<RssEntry> {
        val newEntries = entries.filter { !sentEntries.contains(it.link) }
        logger.debug("Detected ${entries.size - newEntries.size} duplicates from ${entries.size} entries")
        return newEntries
    }

    /**
     * Records entries as sent
     */
    fun markAsSent(entries: List<RssEntry>) {
        entries.forEach { entry ->
            sentEntries.add(entry.link)
        }
        saveSentEntries(entries)
    }

    /**
     * Loads previously sent entries from the JSON log file
     */
    private fun loadSentEntries() {
        val file = File(logFilePath)
        if (!file.exists()) {
            logger.info("Sent entries log file not found: $logFilePath. Starting fresh.")
            return
        }

        try {
            val logs = objectMapper.readValue(file, Array<SentEntryLog>::class.java)
            sentEntries = logs.map { it.link }.toMutableSet()
            logger.info("Loaded ${sentEntries.size} previously sent entries")
        } catch (e: Exception) {
            logger.error("Error loading sent entries log", e)
        }
    }

    /**
     * Saves newly sent entries to the JSON log file
     */
    private fun saveSentEntries(entries: List<RssEntry>) {
        try {
            val file = File(logFilePath)
            
            // Load existing entries
            val existing = if (file.exists()) {
                objectMapper.readValue(file, Array<SentEntryLog>::class.java).toMutableList()
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

            // Keep only last 1000 entries for performance
            val trimmed = if (existing.size > 1000) {
                existing.takeLast(1000)
            } else {
                existing
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, trimmed)
            logger.debug("Saved ${entries.size} new entries to $logFilePath (total: ${trimmed.size})")
        } catch (e: Exception) {
            logger.error("Error saving sent entries", e)
        }
    }

    /**
     * Returns the count of tracked entries
     */
    fun getTrackedCount(): Int = sentEntries.size
}
