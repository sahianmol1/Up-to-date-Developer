package com.uptodatedeveloper.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.uptodatedeveloper.domain.DiscordEmbed
import com.uptodatedeveloper.domain.DiscordField
import com.uptodatedeveloper.domain.DiscordMessage
import com.uptodatedeveloper.domain.RssEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Sends formatted messages to Discord via webhook
 */
class DiscordWebhookClient(
    private val webhookUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val logger = LoggerFactory.getLogger(DiscordWebhookClient::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    /**
     * Sends a single RSS entry as a Discord embed
     */
    fun sendEntry(entry: RssEntry, tags: List<String> = emptyList()): Result<Unit> {
        val embed = createEmbed(entry, tags)
        val message = DiscordMessage(embeds = listOf(embed))
        return sendMessage(message)
    }

    /**
     * Sends multiple entries as Discord embeds (grouped in one message)
     */
    fun sendEntries(entries: List<RssEntry>, tags: List<String> = emptyList()): Result<Unit> {
        if (entries.isEmpty()) return Result.success(Unit)

        val embeds = entries.map { createEmbed(it, tags) }
        val message = DiscordMessage(
            content = "📰 **New Updates** (${entries.size} items)",
            embeds = embeds.take(10) // Discord limit is 10 embeds per message
        )
        return sendMessage(message)
    }

    /**
     * Sends a raw Discord message
     */
    fun sendMessage(message: DiscordMessage): Result<Unit> {
        return try {
            if (webhookUrl.isEmpty()) {
                logger.warn("Discord webhook URL not configured. Skipping message send.")
                return Result.success(Unit)
            }

            val jsonBody = objectMapper.writeValueAsString(message)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "UpToDateDeveloper/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error("Failed to send Discord message: HTTP ${response.code}")
                return Result.failure(Exception("Discord webhook error: ${response.code}"))
            }

            logger.info("Message sent to Discord successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error sending Discord message", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a Discord embed from an RSS entry
     */
    private fun createEmbed(entry: RssEntry, tags: List<String> = emptyList()): DiscordEmbed {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val isoFormatter = DateTimeFormatter.ISO_INSTANT
        
        val formattedDate = dateFormatter.format(
            entry.publishedDate.atZone(java.time.ZoneId.systemDefault())
        )
        val isoDate = isoFormatter.format(entry.publishedDate)

        val sourceTag = if (entry.source.isNotEmpty()) "#${entry.source.lowercase()}" else ""
        val allTags = (listOf(sourceTag) + tags).filter { it.isNotEmpty() }
        val tagString = if (allTags.isNotEmpty()) "\n${allTags.joinToString(" ")}" else ""

        return DiscordEmbed(
            title = entry.title,
            description = "${entry.description.take(200)}...".takeIf { it.length > 4 } ?: entry.title,
            url = entry.link,
            color = getColorForSource(entry.source),
            fields = listOf(
                DiscordField("Source", entry.source, inline = true),
                DiscordField("Published", formattedDate, inline = true),
                DiscordField("Author", entry.author ?: "Unknown", inline = true)
            ),
            timestamp = isoDate
        )
    }

    /**
     * Returns a color code based on the feed source
     */
    private fun getColorForSource(source: String): Int {
        return when {
            source.contains("Kotlin", ignoreCase = true) -> 0x7F52FF // Kotlin purple
            source.contains("Android", ignoreCase = true) -> 0x3DDC84 // Android green
            else -> 0x3447003 // Default blue
        }
    }
}
