package com.uptodatedeveloper

import com.uptodatedeveloper.config.AppConfig
import com.uptodatedeveloper.data.RssFeedFetcher
import com.uptodatedeveloper.delivery.DiscordWebhookClient
import com.uptodatedeveloper.service.DuplicateDetector
import com.uptodatedeveloper.service.PriorityCalculator
import com.uptodatedeveloper.service.RssEntryFilter
import com.uptodatedeveloper.service.RssFeedAggregatorService
import com.uptodatedeveloper.service.UpdateTypeCalculator
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.uptodatedeveloper.MainKt")

fun main() {
    logger.info("Starting UpToDate Developer RSS Feed Aggregator Bot v4.1")

    // Log configuration
    AppConfig.logSummary()

    // Validate configuration
    val configErrors = AppConfig.validate()
    if (configErrors.isNotEmpty()) {
        logger.error("Configuration validation failed:")
        configErrors.forEach { logger.error("  - $it") }
        System.exit(1)
    }

    try {
        // Initialize components
        val fetcher = RssFeedFetcher()
        val filter = RssEntryFilter(hoursWindow = AppConfig.filterHours)
        val duplicateDetector = DuplicateDetector()
        val priorityCalculator = PriorityCalculator()
        val updateTypeCalculator = UpdateTypeCalculator()
        val discordClient = DiscordWebhookClient()

        // Create aggregator service
        val aggregator = RssFeedAggregatorService(
            fetcher = fetcher,
            filter = filter,
            duplicateDetector = duplicateDetector,
            priorityCalculator = priorityCalculator,
            updateTypeCalculator = updateTypeCalculator,
            discordClient = discordClient
        )

        // Run aggregation
        val feeds = AppConfig.getFeeds()
        val result = aggregator.aggregate(feeds, AppConfig.keywords)

        // Exit with appropriate code
        if (result.errors.isNotEmpty()) {
            logger.warn("Completed with ${result.errors.size} errors")
            System.exit(1)
        } else {
            logger.info("Aggregation completed successfully")
            System.exit(0)
        }
    } catch (e: Exception) {
        logger.error("Fatal error during aggregation", e)
        System.exit(1)
    }
}
