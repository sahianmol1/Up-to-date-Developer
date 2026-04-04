package com.uptodatedeveloper.service

import com.uptodatedeveloper.data.RssFeedFetcher
import com.uptodatedeveloper.delivery.DiscordWebhookClient
import com.uptodatedeveloper.domain.AggregationResult
import com.uptodatedeveloper.domain.FeedConfig
import org.slf4j.LoggerFactory

/**
 * Main service orchestrating the entire RSS aggregation pipeline
 */
class RssFeedAggregatorService(
    private val fetcher: RssFeedFetcher,
    private val filter: RssEntryFilter,
    private val duplicateDetector: DuplicateDetector,
    private val discordClient: DiscordWebhookClient
) {
    private val logger = LoggerFactory.getLogger(RssFeedAggregatorService::class.java)

    /**
     * Executes the full aggregation pipeline:
     * 1. Fetch entries from all feeds
     * 2. Filter by time window and keywords
     * 3. Remove duplicates
     * 4. Send to Discord
     * 5. Log results
     */
    fun aggregate(
        feeds: List<FeedConfig>,
        keywords: List<String> = emptyList()
    ): AggregationResult {
        logger.info("Starting RSS aggregation for ${feeds.size} feeds")

        val (allEntries, fetchErrors) = fetcher.fetchAll(feeds)
        val totalFetched = allEntries.size
        logger.info("Fetched $totalFetched total entries")

        // Filter by time window and keywords
        val filtered = filter.filter(allEntries, keywords)
        logger.info("Filtered to ${filtered.size} entries after time and keyword filtering")

        // Remove duplicates
        val newEntries = duplicateDetector.filterNew(filtered)
        val newCount = newEntries.size
        logger.info("Found $newCount new entries (after deduplication)")

        // Send to Discord
        var sentCount = 0
        val sendErrors = mutableListOf<String>()

        if (newEntries.isNotEmpty()) {
            // Group entries by source for better formatting
            val groupedBySource = newEntries.groupBy { it.source }

            groupedBySource.forEach { (source, entries) ->
                val sourceTags = feeds.find { it.name == source }?.tags ?: emptyList()
                discordClient.sendEntries(entries, sourceTags)
                    .onSuccess {
                        sentCount += entries.size
                        duplicateDetector.markAsSent(entries)
                        logger.info("Sent ${entries.size} entries from $source to Discord")
                    }
                    .onFailure { error ->
                        val errorMsg = "Failed to send $source entries: ${error.message}"
                        sendErrors.add(errorMsg)
                        logger.error(errorMsg)
                    }
            }
        } else {
            logger.info("No new entries to send")
        }

        // Log fetch errors
        val errorMessages = mutableListOf<String>()
        fetchErrors.forEach { (feed, error) ->
            val msg = "Error fetching ${feed.name}: ${error.message}"
            errorMessages.add(msg)
            logger.warn(msg)
        }
        errorMessages.addAll(sendErrors)

        val result = AggregationResult(
            totalFetched = totalFetched,
            newEntries = newCount,
            sent = sentCount,
            errors = errorMessages
        )

        logResults(result)
        return result
    }

    /**
     * Logs aggregation results in a user-friendly format
     */
    private fun logResults(result: AggregationResult) {
        logger.info("=== Aggregation Results ===")
        logger.info("Total Fetched: ${result.totalFetched}")
        logger.info("New Entries: ${result.newEntries}")
        logger.info("Sent to Discord: ${result.sent}")
        logger.info("Tracked Total: ${duplicateDetector.getTrackedCount()}")
        if (result.errors.isNotEmpty()) {
            logger.warn("Errors (${result.errors.size}):")
            result.errors.forEach { logger.warn("  - $it") }
        }
        logger.info("===========================")
    }
}
