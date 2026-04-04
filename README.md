# UpToDate Developer - RSS Feed Aggregator Bot

A production-quality Kotlin application that fetches updates from multiple RSS feeds, filters them intelligently, and sends formatted messages to a Discord channel via webhook.

## Features

✅ **Multi-Feed Support** - Aggregate from multiple RSS sources (Kotlin Blog, Android Developers)  
✅ **Smart Filtering** - Filter by time window (24h default) and keywords  
✅ **Duplicate Detection** - Prevent duplicate messages using persistent JSON log  
✅ **Rich Discord Messages** - Send entries as color-coded embeds with metadata  
✅ **Error Handling** - Gracefully handles network failures and feed errors  
✅ **Scheduled Execution** - GitHub Actions runs every 6 hours  
✅ **Clean Architecture** - Modular, testable code with clear separation of concerns  
✅ **Production Logging** - Comprehensive logging with SLF4J/Logback  

## Project Structure

```
src/main/kotlin/com/uptodatedeveloper/
├── config/
│   └── AppConfig.kt              # Environment variables & configuration
├── data/
│   └── RssFeedFetcher.kt         # RSS feed fetching & parsing
├── delivery/
│   └── DiscordWebhookClient.kt   # Discord webhook integration
├── domain/
│   └── Models.kt                 # Data classes (RssEntry, etc.)
├── service/
│   ├── RssEntryFilter.kt         # Filtering logic
│   ├── DuplicateDetector.kt      # Deduplication with JSON persistence
│   └── RssFeedAggregatorService.kt # Main orchestrator
└── Main.kt                        # Entry point

.github/workflows/
└── rss-aggregator.yml            # GitHub Actions schedule
```

## Tech Stack

- **Language**: Kotlin 2.2.20
- **Build Tool**: Gradle with Kotlin DSL
- **HTTP Client**: OkHttp 4.11.0
- **RSS Parsing**: Rome 2.1.0
- **JSON**: Jackson with Kotlin module
- **Logging**: SLF4J + Logback 1.4.14
- **Runtime**: JVM 21+

## Configuration

### Environment Variables

```bash
# Required
DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/..."

# Optional
FILTER_HOURS=24                    # Time window for entries (default: 24)
KEYWORDS="Kotlin,Compose,Android"  # Comma-separated keywords filter
FEEDS="name1:url1;name2:url2"      # Custom feeds (uses defaults if not set)
```

### Default Feeds

1. **Kotlin Blog** - https://blog.jetbrains.com/kotlin/feed/
2. **Android Developers** - https://android-developers.googleblog.com/atom.xml

## Setup

### Local Development

#### Prerequisites
- JDK 21+
- Gradle (included via wrapper)

#### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd UpToDateDeveloper
   ```

2. **Get a Discord Webhook URL**
   - Go to your Discord server → Settings → Webhooks
   - Create a new webhook and copy the URL

3. **Set environment variables**
   ```bash
   export DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
   export FILTER_HOURS=24
   export KEYWORDS="Kotlin,Compose,Android"
   ```

4. **Build the project**
   ```bash
   ./gradlew build
   ```

5. **Run locally**
   ```bash
   ./gradlew run
   ```

   Or directly:
   ```bash
   ./gradlew build
   java -jar build/libs/UpToDateDeveloper-1.0.0.jar
   ```

### GitHub Actions Setup

1. **Add Discord webhook as secret**
   - Go to repository → Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `DISCORD_WEBHOOK_URL`
   - Value: Your Discord webhook URL

2. **Workflow runs automatically**
   - Schedule: Every 6 hours (0, 6, 12, 18 UTC)
   - Manual trigger: Go to Actions → RSS Feed Aggregator → Run workflow

## Output

The bot logs results in this format:

```
=== Aggregation Results ===
Total Fetched: 45
New Entries: 8
Sent to Discord: 8
Tracked Total: 156
===========================
```

### Deduplication

Sent entries are tracked in `sent_entries.json`:
```json
[
  {
    "link": "https://blog.jetbrains.com/kotlin/...",
    "sentAt": "2024-04-04T10:30:00Z",
    "title": "Kotlin 1.9.20 Released"
  }
]
```

The log keeps the last 1000 entries for performance.

### Discord Output

Entries appear as rich embeds:

```
📰 New Updates (3 items)

[Kotlin Blog]
Title: Kotlin 2.0 Released
Link: https://blog.jetbrains.com/kotlin/...
Published: 2024-04-03 15:30:00
#kotlin
```

Colors are source-specific:
- 🟣 Kotlin Blog (purple: #7F52FF)
- 🟢 Android Developers (green: #3DDC84)

## Code Quality

### Architecture Principles

- **Separation of Concerns**: Each layer (data, domain, service, delivery) has a single responsibility
- **Dependency Injection**: Components are instantiated in Main.kt and passed to dependents
- **Error Handling**: All external calls use Result<T> pattern with proper logging
- **Testability**: Functions are pure and accept dependencies as parameters
- **Logging**: Comprehensive structured logging at appropriate levels

### Key Design Patterns

1. **Service Layer Pattern** - RssFeedAggregatorService orchestrates the pipeline
2. **Repository Pattern** - RssFeedFetcher abstracts feed fetching
3. **Adapter Pattern** - DiscordWebhookClient adapts RSS entries to Discord format
4. **Result Type** - Error handling without exceptions at service boundaries

## Error Handling

The bot handles failures gracefully:

- **Network Errors**: Retries with OkHttp timeouts, logs failures, continues with other feeds
- **Parse Errors**: Rome library handles malformed RSS, invalid feeds are skipped
- **Discord Errors**: Webhook failures are logged but don't crash the application
- **Configuration Errors**: Validation on startup prevents runtime surprises

## Performance

- **Concurrent Feed Fetching**: All feeds are fetched in sequence (can be parallelized)
- **Efficient Deduplication**: O(1) lookup using Set of links
- **Batch Discord Messages**: Multiple entries sent in one API call (up to 10 embeds)
- **File I/O**: JSON log is only updated with new entries (not on every run)

## Testing

Create tests in `src/test/kotlin/`:

```kotlin
class RssEntryFilterTest {
    @Test
    fun `should filter entries by time window`() {
        val filter = RssEntryFilter(hoursWindow = 24)
        val entries = listOf(
            RssEntry(..., publishedDate = Instant.now().minus(12, ChronoUnit.HOURS)),
            RssEntry(..., publishedDate = Instant.now().minus(36, ChronoUnit.HOURS))
        )
        val filtered = filter.filterByTimeWindow(entries)
        assertEquals(1, filtered.size)
    }
}
```

Run tests:
```bash
./gradlew test
```

## Troubleshooting

### Bot not sending messages

1. **Check Discord webhook URL**
   ```bash
   echo $DISCORD_WEBHOOK_URL
   ```

2. **Check logs**
   ```bash
   tail -f logs/app.log
   ```

3. **Test webhook manually**
   ```bash
   curl -X POST "https://discord.com/api/webhooks/YOUR_WEBHOOK" \
     -H "Content-Type: application/json" \
     -d '{"content":"Test"}'
   ```

### No entries fetched

1. **Verify feed URLs are accessible**
   ```bash
   curl -I https://blog.jetbrains.com/kotlin/feed/
   ```

2. **Check time filter**
   - Default is 24 hours. Increase with `FILTER_HOURS=48`

3. **Check keyword filter**
   - If keywords are set, entries must match them

### Duplicate entries showing

1. **Check `sent_entries.json` exists**
   ```bash
   ls -la sent_entries.json
   ```

2. **Clear duplicate history** (optional)
   ```bash
   rm sent_entries.json
   ```

## Extending the Bot

### Add a New Feed

Update in `AppConfig.kt`:

```kotlin
FeedConfig(
    name = "Your Blog",
    url = "https://example.com/feed.xml",
    tags = listOf("#yourTag")
)
```

Or use environment variable:
```bash
export FEEDS="Kotlin:https://blog.jetbrains.com/kotlin/feed/;Android:https://android-developers.googleblog.com/atom.xml;YourBlog:https://example.com/feed.xml"
```

### Custom Filtering

Extend `RssEntryFilter`:

```kotlin
fun filterByCategory(entries: List<RssEntry>, category: String): List<RssEntry> {
    return entries.filter { it.source.contains(category, ignoreCase = true) }
}
```

### Discord Formatting

Customize in `DiscordWebhookClient.createEmbed()`:

```kotlin
private fun createEmbed(entry: RssEntry): DiscordEmbed {
    return DiscordEmbed(
        title = "📰 ${entry.title}",
        // ... customize colors, fields, etc.
    )
}
```

## Performance Optimization (Future)

- [ ] Parallel feed fetching with coroutines
- [ ] Redis/in-memory cache for deduplication
- [ ] Database backend instead of JSON logs
- [ ] Webhook batching with rate limiting
- [ ] Feed priority/weighting

## Contributing

1. Create a feature branch
2. Make changes following the existing code style
3. Run tests and linter
4. Submit a pull request

## License

MIT License - See LICENSE file for details

## Support

For issues or questions:
1. Check logs: `logs/app.log`
2. Review GitHub Issues
3. Create a new issue with logs attached

---

**Built with ❤️ using Kotlin and production best practices**
