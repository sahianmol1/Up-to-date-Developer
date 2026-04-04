package com.uptodatedeveloper.domain

import java.time.Instant

/**
 * Feed type enum for routing to different Discord channels
 */
enum class FeedType(val displayName: String) {
    KOTLIN("Kotlin"),
    ANDROID("Android")
}

/**
 * Priority enum for classifying RSS entries
 * Uses priority levels with developer-friendly colors
 */
enum class Priority(
    val displayName: String,
    val emoji: String,
    val color: Int
) {
    HIGH("High", "🔥", 0xFFB347),      // Soft orange (not harsh red)
    MEDIUM("Medium", "⚡", 0x4A90E2),  // Blue
    LOW("Low", "🌿", 0x7FFF7F)         // Green
}

/**
 * Represents a single entry from an RSS feed
 */
data class RssEntry(
    val title: String,
    val link: String,
    val publishedDate: Instant,
    val description: String = "",
    val source: String = "",
    val author: String? = null,
    val feedType: FeedType = FeedType.KOTLIN,
    val priority: Priority = Priority.LOW
)

/**
 * Configuration for an RSS feed source
 */
data class FeedConfig(
    val name: String,
    val url: String,
    val feedType: FeedType,
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
    val url: String? = null,
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
