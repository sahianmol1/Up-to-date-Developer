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
 * Uses priority levels with distinct colors
 */
enum class Priority(
    val displayName: String,
    val emoji: String,
    val color: Int
) {
    HIGH("High", "🔥", 0xFF0000),      // Red
    MEDIUM("Medium", "⚡", 0xFFFF00),  // Yellow
    LOW("Low", "🌿", 0x00FF00)         // Green
}

/**
 * Update type enum for categorizing RSS entry content
 */
enum class UpdateType(
    val displayName: String,
    val keywords: List<String>
) {
    RELEASE("Release", listOf("release", "shipped", "released", "out now")),
    FEATURE("Feature", listOf("feature", "new", "introduces", "support", "added")),
    BUG_FIX("Bug Fix", listOf("bug fix", "fixed", "bugfix", "issue", "fix", "patch")),
    BREAKING_CHANGE("Breaking Change", listOf("breaking", "deprecat", "removed")),
    BLOG("Blog", listOf("blog", "article", "post", "story", "guide", "tutorial", "how-to", "walkthrough", "deep dive", "discussion")),
    BETA("Beta", listOf("beta", "preview", "alpha", "rc", "release candidate")),
    PERFORMANCE("Performance", listOf("performance", "improve", "optimiz", "faster", "speed")),
    SECURITY("Security", listOf("security", "vulnerability", "cve", "exploit")),
    NEWS("News", listOf("announce", "announcement", "news", "update"));

    companion object {
        fun detect(content: String): UpdateType {
            val lowercase = content.lowercase()
            // Return first matching type (order matters!)
            // BLOG is checked before NEWS to avoid false positives
            return values().firstOrNull { updateType ->
                updateType.keywords.any { keyword -> lowercase.contains(keyword) }
            } ?: NEWS  // Default to NEWS if no match
        }
    }
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
    val priority: Priority = Priority.LOW,
    val updateType: UpdateType = UpdateType.NEWS
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
