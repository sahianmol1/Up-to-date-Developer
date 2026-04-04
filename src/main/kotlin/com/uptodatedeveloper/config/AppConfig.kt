package com.uptodatedeveloper.config

import com.uptodatedeveloper.domain.FeedConfig
import com.uptodatedeveloper.domain.FeedType
import org.slf4j.LoggerFactory

/**
 * Manages application configuration from environment variables and defaults
 */
object AppConfig {
    private val logger = LoggerFactory.getLogger(AppConfig::class.java)

    // Discord webhook URLs (one per channel)
    val kotlinWebhookUrl: String = System.getenv("KOTLIN_WEBHOOK_URL") ?: ""
    val androidWebhookUrl: String = System.getenv("ANDROID_WEBHOOK_URL") ?: ""

    // Filtering
    val filterHours: Long = System.getenv("FILTER_HOURS")?.toLongOrNull() ?: 24L
    val keywords: List<String> = System.getenv("KEYWORDS")?.split(",")?.map { it.trim() } ?: emptyList()

    // Default RSS feeds with their types
    private val defaultFeeds = listOf(
        FeedConfig(
            name = "Kotlin Blog",
            url = "https://blog.jetbrains.com/kotlin/feed/",
            feedType = FeedType.KOTLIN,
            tags = listOf("#kotlin")
        ),
        FeedConfig(
            name = "Android Developers",
            url = "https://android-developers.googleblog.com/atom.xml",
            feedType = FeedType.ANDROID,
            tags = listOf("#android")
        )
    )

    /**
     * Gets configured feeds from environment or returns defaults
     */
    fun getFeeds(): List<FeedConfig> {
        val feedsEnv = System.getenv("FEEDS")
        return if (feedsEnv != null && feedsEnv.isNotEmpty()) {
            parseFeedsFromEnv(feedsEnv)
        } else {
            logger.info("Using default feeds (configure via FEEDS env var)")
            defaultFeeds
        }
    }

    /**
     * Gets the Discord webhook URL for a given feed type
     */
    fun getWebhookUrl(feedType: FeedType): String {
        return when (feedType) {
            FeedType.KOTLIN -> kotlinWebhookUrl
            FeedType.ANDROID -> androidWebhookUrl
        }
    }

    /**
     * Validates configuration
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (kotlinWebhookUrl.isEmpty()) {
            errors.add("KOTLIN_WEBHOOK_URL environment variable is not set")
        } else if (!isValidUrl(kotlinWebhookUrl)) {
            errors.add("KOTLIN_WEBHOOK_URL is not a valid URL")
        }

        if (androidWebhookUrl.isEmpty()) {
            errors.add("ANDROID_WEBHOOK_URL environment variable is not set")
        } else if (!isValidUrl(androidWebhookUrl)) {
            errors.add("ANDROID_WEBHOOK_URL is not a valid URL")
        }

        val feeds = getFeeds()
        if (feeds.isEmpty()) {
            errors.add("No feeds configured")
        }

        feeds.forEach { feed ->
            if (!isValidUrl(feed.url)) {
                errors.add("Invalid feed URL: ${feed.url}")
            }
        }

        return errors
    }

    /**
     * Logs configuration summary
     */
    fun logSummary() {
        logger.info("=== Application Configuration ===")
        logger.info("Kotlin Webhook: ${if (kotlinWebhookUrl.isEmpty()) "NOT SET" else "Configured"}")
        logger.info("Android Webhook: ${if (androidWebhookUrl.isEmpty()) "NOT SET" else "Configured"}")
        logger.info("Filter Hours: $filterHours")
        logger.info("Keywords Filter: ${if (keywords.isEmpty()) "None" else keywords.joinToString(", ")}")
        logger.info("Feeds: ${getFeeds().joinToString(", ") { "${it.name} (${it.feedType.displayName})" }}")
        logger.info("==================================")
    }

    /**
     * Parses feeds from environment variable format: "name1:url1:KOTLIN,name2:url2:ANDROID"
     */
    private fun parseFeedsFromEnv(feedsEnv: String): List<FeedConfig> {
        return feedsEnv.split(";").mapNotNull { feed ->
            val parts = feed.split(":")
            if (parts.size >= 3) {
                try {
                    val feedType = FeedType.valueOf(parts[2].trim().uppercase())
                    FeedConfig(
                        name = parts[0].trim(),
                        url = parts[1].trim(),
                        feedType = feedType
                    )
                } catch (e: IllegalArgumentException) {
                    logger.warn("Invalid feed type in FEEDS env var: ${parts[2]}")
                    null
                }
            } else {
                logger.warn("Invalid feed format in FEEDS env var: $feed (expected name:url:TYPE)")
                null
            }
        }
    }

    /**
     * Simple URL validation
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
