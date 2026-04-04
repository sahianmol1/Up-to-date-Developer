package com.uptodatedeveloper.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.uptodatedeveloper.config.AppConfig
import com.uptodatedeveloper.domain.DiscordEmbed
import com.uptodatedeveloper.domain.DiscordField
import com.uptodatedeveloper.domain.DiscordMessage
import com.uptodatedeveloper.domain.FeedType
import com.uptodatedeveloper.domain.RssEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Sends formatted messages to Discord via webhook with multi-channel routing
 */
class DiscordWebhookClient(
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
     * Sends multiple entries as Discord embeds, routed by feed type
     */
    fun sendEntries(entries: List<RssEntry>, tags: List<String> = emptyList()): Result<Unit> {
        if (entries.isEmpty()) return Result.success(Unit)

        // Group entries by feed type for proper routing
        val groupedByFeedType = entries.groupBy { it.feedType }

        var totalSent = 0
        val errors = mutableListOf<String>()

        groupedByFeedType.forEach { (feedType, feedEntries) ->
            val webhookUrl = AppConfig.getWebhookUrl(feedType)
            
            if (webhookUrl.isEmpty()) {
                val errorMsg = "No webhook URL configured for ${feedType.displayName}"
                logger.warn(errorMsg)
                errors.add(errorMsg)
                return@forEach
            }

            // Batch entries (smaller batches for safety - each entry can have large descriptions)
            feedEntries.chunked(2).forEachIndexed { batchIndex, batch ->
                val embeds = mutableListOf<DiscordEmbed>()
                val skipped = mutableListOf<String>()

                batch.forEach { entry ->
                    try {
                        val embed = createEmbed(entry, tags)
                        // Validate embed doesn't cause issues
                        val testJson = objectMapper.writeValueAsString(embed)
                        if (testJson.length > 4000) {
                            logger.warn("Skipping entry - payload too large: '${entry.title}'")
                            skipped.add(entry.title)
                        } else {
                            embeds.add(embed)
                        }
                    } catch (e: Exception) {
                        logger.error("Error creating embed for entry '${entry.title}': ${e.message}")
                        skipped.add(entry.title)
                    }
                }

                if (embeds.isEmpty()) {
                    logger.warn("Skipping batch $batchIndex - no valid embeds (skipped: $skipped)")
                    return@forEachIndexed
                }

                val message = DiscordMessage(
                    content = "📰 **${feedType.displayName} Updates** (${embeds.size} items)",
                    embeds = embeds
                )
                
                sendMessage(webhookUrl, message)
                    .onSuccess { 
                        totalSent += embeds.size
                        logger.debug("Successfully sent ${embeds.size} embeds from batch $batchIndex")
                    }
                    .onFailure { error ->
                        // If sending together fails, try sending individually
                        logger.warn("Batch ${batchIndex} failed, attempting to send individually...")
                        var individualSent = 0
                        embeds.forEachIndexed { idx, embed ->
                            val singleMessage = DiscordMessage(
                                content = "📰 **${feedType.displayName} Updates**",
                                embeds = listOf(embed)
                            )
                            sendMessage(webhookUrl, singleMessage)
                                .onSuccess { 
                                    individualSent++
                                    logger.debug("Successfully sent individual embed $idx from batch $batchIndex")
                                }
                                .onFailure { indivError ->
                                    logger.error("Individual embed $idx from batch $batchIndex failed: '${embed.title}' - ${indivError.message}")
                                    // Skip this individual embed but continue with others
                                }
                        }
                        
                        if (individualSent > 0) {
                            totalSent += individualSent
                            logger.info("Recovered ${individualSent}/${embeds.size} embeds from batch $batchIndex by sending individually")
                        } else {
                            val titles = batch.map { it.title }.joinToString(", ")
                            val errorMsg = "Failed to send ${feedType.displayName} batch $batchIndex (titles: $titles): ${error.message}"
                            logger.error(errorMsg)
                            errors.add(errorMsg)
                        }
                    }
            }
        }

        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("${errors.size} errors sending to Discord: ${errors.joinToString("; ")}"))
        }
    }

    /**
     * Sends a raw Discord message to a specific webhook URL
     */
    private fun sendMessage(webhookUrl: String, message: DiscordMessage): Result<Unit> {
        return try {
            val jsonBody = objectMapper.writeValueAsString(message)
            val payloadSize = jsonBody.length
            
            if (payloadSize > 10000) {
                logger.warn("Large payload detected: $payloadSize bytes")
            }
            
            val request = Request.Builder()
                .url(webhookUrl)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "UpToDateDeveloper/2.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = try {
                    response.body?.string() ?: "No error body"
                } catch (e: Exception) {
                    "Could not read error body"
                }
                val embeds = message.embeds.mapIndexed { idx, embed -> "$idx: ${embed.title}" }
                logger.error("Failed to send Discord message (payload: ${payloadSize}B, embeds: $embeds): HTTP ${response.code}, error: $errorBody")
                return Result.failure(Exception("Discord webhook error: ${response.code} - $errorBody"))
            }

            logger.info("Message sent to Discord successfully (${payloadSize}B)")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error sending Discord message", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a Discord embed from an RSS entry with priority-based coloring
     * Validates all fields against Discord's size limits
     */
    private fun createEmbed(entry: RssEntry, tags: List<String> = emptyList()): DiscordEmbed {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val isoFormatter = DateTimeFormatter.ISO_INSTANT
        
        val formattedDate = dateFormatter.format(
            entry.publishedDate.atZone(java.time.ZoneId.systemDefault())
        )
        val isoDate = isoFormatter.format(entry.publishedDate)

        // Clean and sanitize title - remove HTML entities
        val cleanTitle = entry.title
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("<[^>]+>".toRegex(), "")  // Remove HTML tags

        // Build title with priority emoji and update type
        // Format: "🔥 [Release] Title"
        // Discord title limit: 256 characters
        val priorityLabel = "${entry.priority.emoji} [${entry.updateType.displayName}]"
        var titleWithPriority = "$priorityLabel $cleanTitle"
        if (titleWithPriority.length > 256) {
            titleWithPriority = titleWithPriority.take(253) + "..."
        }

        // Clean and sanitize description - remove HTML entities and tags
        var rawDescription = entry.description
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("<[^>]+>".toRegex(), "")  // Remove HTML tags
            .replace("\n\n", " ")  // Remove double newlines
            .replace("\n", " ")    // Replace newlines with spaces
            .replace("\\s+".toRegex(), " ")  // Normalize whitespace

        var description = rawDescription
            .take(300)
            .let { if (rawDescription.length > 300) "$it..." else it }
            .ifEmpty { cleanTitle }
        if (description.length > 4096) {
            description = description.take(4093) + "..."
        }

        // Validate and clean URL
        val url = entry.link.trim().let { link ->
            when {
                link.isEmpty() -> null
                !link.startsWith("http://") && !link.startsWith("https://") -> {
                    logger.warn("Invalid URL scheme: $link")
                    null
                }
                link.length > 2048 -> {
                    logger.warn("URL too long: ${link.length} chars")
                    null
                }
                else -> link
            }
        }

        // Discord field value limit: 1024 characters
        val author = (entry.author ?: "Unknown")
            .replace("<[^>]+>".toRegex(), "")
            .take(1020)
        val source = entry.source
            .replace("<[^>]+>".toRegex(), "")
            .take(1020)
        val formattedDateField = formattedDate.take(1020)

        return DiscordEmbed(
            title = titleWithPriority,
            description = description,
            url = url,
            color = entry.priority.color,
            fields = listOf(
                DiscordField("Type", entry.updateType.displayName, inline = true),
                DiscordField("Priority", entry.priority.displayName, inline = true),
                DiscordField("Source", source, inline = true),
                DiscordField("Published", formattedDateField, inline = true),
                DiscordField("Author", author, inline = true)
            ),
            timestamp = isoDate
        )
    }
}
