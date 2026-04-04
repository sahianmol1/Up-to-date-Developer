package com.uptodatedeveloper.config

import com.uptodatedeveloper.domain.FeedConfig
import org.slf4j.LoggerFactory

/**
 * Manages application configuration from environment variables and defaults
 */
object AppConfig {
    private val logger = LoggerFactory.getLogger(AppConfig::class.java)

    // Environment variables
    val discordWebhookUrl: String = System.getenv("DISCORD_WEBHOOK_URL") ?: ""
    val filterHours: Long = System.getenv("FILTER_HOURS")?.toLongOrNull() ?: 24L
    val keywords: List<String> = System.getenv("KEYWORDS")?.split(",")?.map { it.trim() } ?: emptyList()

    // Default RSS feeds
    private val defaultFeeds = listOf(
        FeedConfig(
            name = "Kotlin Blog",
            url = "https://blog.jetbrains.com/kotlin/feed/",
            tags = listOf("#kotlin")
        ),
        FeedConfig(
            name = "Android Developers",
            url = "https://android-developers.googleblog.com/atom.xml",
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
     * Validates configuration
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (discordWebhookUrl.isEmpty()) {
            errors.add("DISCORD_WEBHOOK_URL environment variable is not set")
        } else if (!isValidUrl(discordWebhookUrl)) {
            errors.add("DISCORD_WEBHOOK_URL is not a valid URL")
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
        logger.info("Discord Webhook: ${if (discordWebhookUrl.isEmpty()) "NOT SET" else "Configured"}")
        logger.info("Filter Hours: $filterHours")
        logger.info("Keywords Filter: ${if (keywords.isEmpty()) "None" else keywords.joinToString(", ")}")
        logger.info("Feeds: ${getFeeds().joinToString(", ") { it.name }}")
        logger.info("==================================")
    }

    /**
     * Parses feeds from environment variable format: "name1:url1,name2:url2"
     */
    private fun parseFeedsFromEnv(feedsEnv: String): List<FeedConfig> {
        return feedsEnv.split(";").mapNotNull { feed ->
            val parts = feed.split(":")
            if (parts.size >= 2) {
                FeedConfig(
                    name = parts[0].trim(),
                    url = parts[1].trim()
                )
            } else {
                logger.warn("Invalid feed format in FEEDS env var: $feed")
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
