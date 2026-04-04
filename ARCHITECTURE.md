# Architecture Document

## Overview

The UpToDate Developer RSS Feed Aggregator Bot follows a **layered clean architecture** pattern with clear separation of concerns. The application pipeline is: Fetch → Filter → Deduplicate → Send → Log.

## Layers

### 1. **Data Layer** (`data/`)
Responsible for fetching and parsing RSS feeds.

**Components:**
- `RssFeedFetcher`: Fetches feeds using OkHttp, parses using Rome library
  - Handles network timeouts (10s)
  - Graceful error handling
  - Batch fetching with individual error tracking
  - Logs fetched entry count per feed

**Key Decisions:**
- Uses Rome library (standard Java RSS parser)
- OkHttp for HTTP with connection timeouts
- Result<T> pattern for error handling

### 2. **Domain Layer** (`domain/`)
Contains data models and core value objects.

**Models:**
- `RssEntry`: A single feed entry (title, link, date, description)
- `FeedConfig`: Feed source configuration (name, URL, tags)
- `DiscordMessage`, `DiscordEmbed`: Discord API structures
- `AggregationResult`: Pipeline execution summary

**Design Pattern:**
- Immutable data classes (Kotlin's default)
- No business logic, only data representation

### 3. **Service Layer** (`service/`)
Business logic and orchestration.

**Components:**
- `RssEntryFilter`: Filtering logic
  - Time-based filtering (configurable hours window)
  - Keyword filtering (case-insensitive)
  - Chainable filter operations
  
- `DuplicateDetector`: Deduplication with persistence
  - Stores sent links in JSON file (`sent_entries.json`)
  - O(1) deduplication lookup using Set
  - Trims log to last 1000 entries for performance
  - Jackson serialization with pretty-printing
  
- `RssFeedAggregatorService`: Main orchestrator
  - Coordinates the entire pipeline
  - Error aggregation and reporting
  - Grouping entries by source for Discord
  - Result logging

**Pipeline Flow:**
```
Fetch (RssFeedFetcher)
  ↓
Filter by Time (RssEntryFilter)
  ↓
Filter by Keywords (RssEntryFilter)
  ↓
Remove Duplicates (DuplicateDetector)
  ↓
Send to Discord (DiscordWebhookClient)
  ↓
Mark as Sent (DuplicateDetector)
  ↓
Log Results (RssFeedAggregatorService)
```

### 4. **Delivery Layer** (`delivery/`)
External integrations and APIs.

**Components:**
- `DiscordWebhookClient`: Discord webhook integration
  - Converts RSS entries to Discord embeds
  - Source-based color coding (Kotlin purple, Android green)
  - Batch sending (up to 10 embeds per message)
  - Proper error handling with logging

**Discord Format Features:**
- Rich embeds with metadata fields
- Source-specific tags and colors
- ISO 8601 timestamps
- Author information
- Clickable links

### 5. **Configuration Layer** (`config/`)
Centralized configuration management.

**Components:**
- `AppConfig`: Single-responsibility configuration object
  - Loads from environment variables
  - Provides default feeds
  - Configuration validation
  - Friendly logging of active configuration

**Environment Variables:**
```
DISCORD_WEBHOOK_URL (required) - Discord webhook URL
FILTER_HOURS (optional) - Time window in hours (default: 24)
KEYWORDS (optional) - Comma-separated keyword filter
FEEDS (optional) - Custom feeds in "name:url" format
```

### 6. **Main Layer** (`Main.kt`)
Entry point and application bootstrap.

**Responsibilities:**
- Component initialization
- Configuration validation
- Error handling and exit codes
- Graceful shutdown

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Main.kt (Entry Point)                     │
│  - Validate configuration                                    │
│  - Initialize components                                    │
│  - Run aggregation                                          │
│  - Exit with status code                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌──────────────────────────────────────────────────────────────┐
│         RssFeedAggregatorService.aggregate()                │
│  Orchestrates the complete pipeline                         │
└──────┬───────────────┬───────────────┬───────────┬──────────┘
       │               │               │           │
       ↓               ↓               ↓           ↓
   ┌────────┐    ┌──────────┐   ┌──────────┐  ┌──────┐
   │ Fetch  │→   │ Filter   │→  │ Duplicate│→ │Send  │
   │        │    │          │   │  Detect  │  │      │
   └────────┘    └──────────┘   └──────────┘  └──────┘
       │              │              │           │
       ↓              ↓              ↓           ↓
   RssFeedFetcher  RssEntry  DuplicateDetector Discord
   (Rome + OkHttp)  Filter    (JSON storage)   WebhookClient
```

## Error Handling

**Strategy:** Fail gracefully, continue with other operations

- **Network Errors**: OkHttp timeouts + retries via Result
- **Parse Errors**: Rome handles malformed XML gracefully
- **Feed Errors**: Skip failing feed, continue with others
- **Discord Errors**: Log and continue, don't crash
- **Configuration Errors**: Validate at startup, fail fast

**Error Tracking:**
- Fetch errors: Logged per feed
- Send errors: Logged per source
- All errors aggregated in `AggregationResult`

## State Management

**Persistent State:** `sent_entries.json`
```json
[
  {
    "link": "https://...",
    "sentAt": "2024-04-04T10:30:00Z",
    "title": "Entry Title"
  }
]
```

**In-Memory State:** None (stateless execution)

**Considerations:**
- File-based state is safe for scheduled execution
- JSON format is human-readable for debugging
- Trimmed to 1000 entries to prevent unbounded growth
- No database required (production-ready without external deps)

## Testability

Each component is independently testable:

```kotlin
// Example: Test filtering
val filter = RssEntryFilter(hoursWindow = 24)
val entries = listOf(...)
val filtered = filter.filterByTimeWindow(entries)

// Example: Test deduplication
val detector = DuplicateDetector()
val newEntries = detector.filterNew(entries)

// Example: Test Discord formatting
val client = DiscordWebhookClient("url")
val embed = client.createEmbed(entry)
```

## Performance Characteristics

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| Fetch | O(n) | Sequential per feed |
| Filter by time | O(n) | Single pass, fast |
| Filter by keyword | O(n×k) | n=entries, k=keywords |
| Deduplication | O(1) | Set lookup per entry |
| Discord send | O(n/10) | Batches up to 10 embeds |
| File I/O | O(1) | Append-only JSON writes |

**Scalability:**
- Can handle ~100-1000 entries per run easily
- For larger scales: add database, parallel feeds, async sending
- Current JSON log trimmed to 1000 entries

## Configuration Management

Single responsibility: `AppConfig.kt` object

**Advantages:**
- Centralized environment variable loading
- Type-safe configuration
- Validation on startup
- Default values provided
- Logging of active configuration

## Logging Strategy

Using SLF4J + Logback with structured logging:

**Log Levels:**
- **INFO**: Major operations (fetch, send, results)
- **DEBUG**: Filtering operations, counts
- **WARN**: Skipped feeds, errors encountered
- **ERROR**: Fatal errors, invalid config

**Output:**
- Console: Real-time visibility
- File: `logs/app.log` with rotation

## Extensibility Points

### Adding a New Filter
```kotlin
fun filterByCategory(entries: List<RssEntry>, category: String): List<RssEntry> {
    // Add to RssEntryFilter or create new filter
}
```

### Adding a New Feed Source
Update `AppConfig.kt` or use environment variable:
```kotlin
FeedConfig("New Source", "https://example.com/feed.xml")
```

### Adding a New Destination
Create new client in `delivery/`:
```kotlin
class SlackWebhookClient : NotificationClient { ... }
```

### Custom Deduplication
Replace JSON with database:
```kotlin
class DatabaseDuplicateDetector : DuplicateDetector { ... }
```

## Security Considerations

- **Webhook URL**: Stored as environment variable (not in code)
- **Secrets**: Never logged or exposed
- **Feed Validation**: URL scheme validation
- **Network**: 10-second timeouts prevent hanging
- **File I/O**: Trims history to prevent disk exhaustion
- **No Authentication**: Relies on webhook security

## Future Improvements

**Performance:**
- [ ] Parallel feed fetching (coroutines)
- [ ] Redis cache for deduplication
- [ ] Batch Discord messages

**Features:**
- [ ] Database backend for state
- [ ] Advanced filtering (regex, categories)
- [ ] Multiple Discord channels
- [ ] Slack/Teams integration

**Reliability:**
- [ ] Retry logic with exponential backoff
- [ ] Dead letter queue for failed sends
- [ ] Metrics/monitoring integration
- [ ] Health checks and alerting

---

**Architecture Summary:** Clean, modular, testable, and maintainable. Ready for production use with optional extensions.
