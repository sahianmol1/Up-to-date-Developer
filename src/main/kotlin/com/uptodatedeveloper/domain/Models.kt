package com.uptodatedeveloper.domain

import java.time.Instant

/**
 * Represents a single entry from an RSS feed
 */
data class RssEntry(
    val title: String,
    val link: String,
    val publishedDate: Instant,
    val description: String = "",
    val source: String = "",
    val author: String? = null
)

/**
 * Configuration for an RSS feed source
 */
data class FeedConfig(
    val name: String,
    val url: String,
    val tags: List<String> = emptyList()
)

/**
 * Discord webhook message structure
 */
data class DiscordMessage(
    val content: String = "",
    val embeds: List<DiscordEmbed> = emptyList()
)

/**
 * Discord embed for rich formatting
 */
data class DiscordEmbed(
    val title: String,
    val description: String,
    val url: String,
    val color: Int = 3447003, // Blue
    val fields: List<DiscordField> = emptyList(),
    val timestamp: String? = null
)

/**
 * Discord embed field
 */
data class DiscordField(
    val name: String,
    val value: String,
    val inline: Boolean = true
)

/**
 * Tracking data for sent entries (for deduplication)
 */
data class SentEntryLog(
    val link: String,
    val sentAt: String,
    val title: String
)

/**
 * Result of the aggregation process
 */
data class AggregationResult(
    val totalFetched: Int,
    val newEntries: Int,
    val sent: Int,
    val errors: List<String> = emptyList()
)
