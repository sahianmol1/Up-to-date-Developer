package com.uptodatedeveloper.data

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.uptodatedeveloper.domain.FeedConfig
import com.uptodatedeveloper.domain.Priority
import com.uptodatedeveloper.domain.RssEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Fetches and parses RSS feeds using Rome library
 */
class RssFeedFetcher(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val logger = LoggerFactory.getLogger(RssFeedFetcher::class.java)

    /**
     * Fetches entries from a given feed configuration
     */
    fun fetch(feedConfig: FeedConfig): Result<List<RssEntry>> {
        return try {
            val request = Request.Builder()
                .url(feedConfig.url)
                .addHeader("User-Agent", "UpToDateDeveloper/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.warn("Failed to fetch feed ${feedConfig.name}: HTTP ${response.code}")
                return Result.failure(Exception("HTTP ${response.code}"))
            }

            response.body?.let { body ->
                val input = SyndFeedInput()
                val feed = input.build(XmlReader(body.byteStream()))

                val entries = feed.entries.map { entry ->
                    RssEntry(
                        title = entry.title ?: "No Title",
                        link = entry.link ?: "",
                        publishedDate = entry.publishedDate?.toInstant() ?: Instant.now(),
                        description = entry.description?.value ?: "",
                        source = feedConfig.name,
                        author = entry.author,
                        feedType = feedConfig.feedType,
                        priority = Priority.LOW  // Will be calculated later
                    )
                }

                logger.info("Fetched ${entries.size} entries from ${feedConfig.name}")
                Result.success(entries)
            } ?: Result.failure(Exception("Empty response body"))
        } catch (e: Exception) {
            logger.error("Error fetching feed ${feedConfig.name}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches multiple feeds and returns all entries
     */
    fun fetchAll(feeds: List<FeedConfig>): Pair<List<RssEntry>, List<Pair<FeedConfig, Exception>>> {
        val allEntries = mutableListOf<RssEntry>()
        val errors = mutableListOf<Pair<FeedConfig, Exception>>()

        feeds.forEach { feed ->
            fetch(feed).onSuccess {
                allEntries.addAll(it)
            }.onFailure { error ->
                errors.add(feed to (error as Exception))
            }
        }

        return allEntries to errors
    }
}
