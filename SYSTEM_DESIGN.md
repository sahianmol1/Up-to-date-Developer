# System Design & Diagrams

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         GitHub Actions                          │
│                   (Every 6 hours @ UTC 0,6,12,18)              │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ↓
                    ┌──────────────────┐
                    │   JVM Runtime    │
                    │   (Java 21+)     │
                    └─────────┬────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ↓               ↓               ↓
      ┌──────────────┐ ┌────────────┐ ┌─────────────┐
      │ Kotlin Blog  │ │ Android    │ │  (Custom)   │
      │   Feed       │ │ Developers │ │   Feeds     │
      └──────┬───────┘ └─────┬──────┘ └────┬────────┘
             │               │             │
             └───────────────┼─────────────┘
                             │
        (Rome + OkHttp)      ↓
         Fetch & Parse  ┌──────────────────┐
                       │   RssEntries     │
                       │  (37 entries)    │
                       └────────┬─────────┘
                                │
        (Time Filter)           ↓
        24-hour window     ┌──────────────────┐
                          │  Filtered       │
                          │  (30 entries)   │
                          └────────┬────────┘
                                   │
        (Keyword Filter)           ↓
        Optional keywords     ┌──────────────────┐
                             │ Further Filter  │
                             │  (20 entries)   │
                             └────────┬────────┘
                                      │
        (JSON Dedup)                  ↓
        sent_entries.json      ┌──────────────────┐
                              │    New Entries   │
                              │  (10 entries)    │
                              └────────┬─────────┘
                                       │
        (Jackson + HTTP)               ↓
        Discord Embeds         ┌──────────────────┐
        Batch send             │   Discord API   │
        (max 10/msg)           │   Webhook       │
                               └────────┬────────┘
                                        │
                                        ↓
                               ┌──────────────────┐
                               │  Discord Server  │
                               │  Channel Message │
                               └──────────────────┘
```

## Component Interaction Diagram

```
┌────────────────────────────────────────────────────────────┐
│                         Main.kt                            │
│  - Validate Config                                         │
│  - Initialize Components                                   │
│  - Run Aggregation                                         │
│  - Handle Exit Codes                                       │
└──────────────┬─────────────────────────────────────────────┘
               │
               ├─────────────────────────────────────────────┐
               ↓                                             │
    ┌──────────────────────────┐                    ┌────────┴────────┐
    │     AppConfig            │                    │  RssEntry       │
    │  - Load env variables    │                    │  Filter         │
    │  - Validate config       │                    │  - Time window  │
    │  - Provide defaults      │                    │  - Keywords     │
    └──────────────────────────┘                    └─────────────────┘
               │
               ├─────────────────────────────────────────────┐
               ↓                                             │
    ┌──────────────────────────┐                    ┌────────┴────────┐
    │   RssFeedFetcher        │                    │ Duplicate       │
    │  (Rome + OkHttp)        │                    │  Detector       │
    │  - Fetch feeds          │                    │  - Load JSON    │
    │  - Parse RSS            │                    │  - Check sent   │
    │  - Error handling       │                    │  - Save JSON    │
    └──────────────────────────┘                    └─────────────────┘
               │
               ├─────────────────────────────────────────────┐
               ↓                                             │
    ┌──────────────────────────┐                    ┌────────┴────────┐
    │ RssFeedAggregator       │                    │  Discord        │
    │  Service                 │                    │  WebhookClient  │
    │  - Orchestrate pipeline  │                    │  - Format embed │
    │  - Group by source       │                    │  - Send HTTP    │
    │  - Log results           │                    │  - Error handle │
    └──────────────────────────┘                    └─────────────────┘
               │
               └─────────────────────────────────────────────┘
                            │
                            ↓
                   ┌────────────────┐
                   │   Log Output   │
                   │  - Console     │
                   │  - File        │
                   └────────────────┘
```

## Data Flow Through Pipeline

```
START
  │
  ├─→ [CONFIG] Load environment variables
  │      │ KOTLIN_WEBHOOK_URL
  │      │ ANDROID_WEBHOOK_URL
  │      │ FILTER_HOURS
  │      │ KEYWORDS
  │      │ FEEDS
  │      ↓
  ├─→ [VALIDATE] Check configuration
  │      │ Webhook URL formats
  │      │ Feed URLs valid
  │      ↓
  ├─→ [FETCH] Get RSS entries
  │      │ Kotlin Blog      → 12 entries
  │      │ Android Devs     → 25 entries
  │      │ (Custom feeds)   → ? entries
  │      ├─→ Total: 37 entries
  │      ↓
  ├─→ [FILTER TIME] Remove old entries
  │      │ 24 hours default (configurable)
  │      │ Keep entries from last N hours
  │      ├─→ Filtered: 30 entries
  │      ↓
  ├─→ [FILTER KEYWORDS] Match keywords
  │      │ Optional: Kotlin, Compose, Android
  │      │ Skip if not configured
  │      ├─→ Filtered: 30 entries
  │      ↓
  ├─→ [DEDUPLICATE] Check history
  │      │ Load sent_entries.json
  │      │ Check each link against set
  │      │ Skip if seen before
  │      ├─→ New: 10 entries
  │      ↓
  ├─→ [GROUP] By source for Discord
  │      │ Kotlin Blog → 5 entries
  │      │ Android Devs → 5 entries
  │      ↓
  ├─→ [SEND] To Discord webhook
  │      │ Create embeds (color-coded)
  │      │ Batch send (max 10 per msg)
  │      │ Handle errors gracefully
  │      ├─→ Sent: 10 entries
  │      ↓
  ├─→ [PERSIST] Mark as sent
  │      │ Append to sent_entries.json
  │      │ Trim to last 1000
  │      ↓
  ├─→ [LOG] Results
  │      │ Total fetched: 37
  │      │ New entries: 10
  │      │ Sent: 10
  │      │ Errors: 0
  │      ↓
  └─→ EXIT with status code
      (0 = success, 1 = errors)
```

## Class Hierarchy & Relationships

```
┌──────────────────────────────────────────────────────────────┐
│                      Models (domain)                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  RssEntry                  FeedConfig                        │
│  ├─ title: String          ├─ name: String                 │
│  ├─ link: String           ├─ url: String                  │
│  ├─ publishedDate: Instant ├─ tags: List<String>           │
│  ├─ description: String    │                                │
│  ├─ source: String         DiscordMessage                   │
│  └─ author: String?        ├─ content: String              │
│                            └─ embeds: List<DiscordEmbed>   │
│  DiscordEmbed              │                                │
│  ├─ title: String          DiscordField                     │
│  ├─ description: String    ├─ name: String                 │
│  ├─ url: String            ├─ value: String                │
│  ├─ color: Int             └─ inline: Boolean              │
│  ├─ fields: List<Field>    │                                │
│  └─ timestamp: String?     AggregationResult               │
│                            ├─ totalFetched: Int            │
│  SentEntryLog              ├─ newEntries: Int              │
│  ├─ link: String           ├─ sent: Int                    │
│  ├─ sentAt: String         └─ errors: List<String>         │
│  └─ title: String          │                                │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                     Services (business logic)                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  RssFeedAggregatorService (Orchestrator)                    │
│  └─ aggregate(feeds, keywords): AggregationResult          │
│     ├─ Uses: RssFeedFetcher                                │
│     ├─ Uses: RssEntryFilter                                │
│     ├─ Uses: DuplicateDetector                             │
│     └─ Uses: DiscordWebhookClient                          │
│                                                              │
│  RssEntryFilter (Business Logic)                           │
│  ├─ filterByTimeWindow(entries): List<RssEntry>           │
│  ├─ filterByKeywords(entries, keywords): List<RssEntry>   │
│  └─ filter(entries, keywords): List<RssEntry>             │
│                                                              │
│  DuplicateDetector (Persistence)                           │
│  ├─ filterNew(entries): List<RssEntry>                    │
│  ├─ markAsSent(entries): Unit                             │
│  └─ getTrackedCount(): Int                                │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    External Services                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  RssFeedFetcher (Data Layer)                               │
│  ├─ fetch(feedConfig): Result<List<RssEntry>>             │
│  └─ fetchAll(feeds): Pair<List, List<Errors>>             │
│                                                              │
│  DiscordWebhookClient (Delivery Layer)                    │
│  ├─ sendEntry(entry, tags): Result<Unit>                 │
│  ├─ sendEntries(entries, tags): Result<Unit>             │
│  └─ sendMessage(message): Result<Unit>                   │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    Configuration                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  AppConfig (Singleton Object)                              │
│  ├─ discordWebhookUrl: String (env var)                   │
│  ├─ filterHours: Long (env var)                           │
│  ├─ keywords: List<String> (env var)                      │
│  ├─ getFeeds(): List<FeedConfig>                          │
│  ├─ validate(): List<String>                              │
│  └─ logSummary(): Unit                                     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## File I/O Operations

```
┌─────────────────────────────────────────────────────────┐
│                sent_entries.json                        │
│              (Deduplication Log)                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ [                                                       │
│   {                                                     │
│     "link": "https://blog.jetbrains.com/kotlin/...",  │
│     "sentAt": "2024-04-04T10:30:00Z",                 │
│     "title": "Kotlin 1.9.20 Released"                 │
│   },                                                   │
│   {                                                     │
│     "link": "https://android-developers.../...",       │
│     "sentAt": "2024-04-04T10:35:00Z",                 │
│     "title": "Android 15 Beta Available"              │
│   },                                                   │
│   ...                                                  │
│ ]                                                       │
│                                                         │
│ Features:                                              │
│ - Max 1000 entries (auto-trim)                        │
│ - ISO 8601 timestamps                                 │
│ - Pretty-printed (human-readable)                     │
│ - Auto-created on first run                          │
│ - Append-only writes                                 │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                logs/app.log                             │
│              (Application Logs)                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ 2026-04-04 10:30:00 [INFO] MainKt - Starting...      │
│ 2026-04-04 10:30:00 [INFO] AppConfig - === Config === │
│ 2026-04-04 10:30:00 [DEBUG] RssFilter - Filtered...  │
│ 2026-04-04 10:30:01 [ERROR] Discord - HTTP 404...    │
│ ...                                                     │
│                                                         │
│ Features:                                              │
│ - Console + file output                               │
│ - Rolling file appender (10MB per file)               │
│ - Keep last 7 days                                    │
│ - Max 100MB total                                     │
│ - Structured format                                   │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              build/libs/UpToDateDeveloper-1.0.0.jar    │
│              (Executable JAR)                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Features:                                              │
│ - Fat JAR (all dependencies included)                │
│ - Size: ~46 KB                                        │
│ - Runnable: java -jar ...jar                         │
│ - Created on: ./gradlew build                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Error Handling Strategy

```
┌─────────────────────────────────────────────────────────┐
│              Error Handling Flows                       │
└─────────────────────────────────────────────────────────┘

Configuration Errors:
  ├─→ Missing KOTLIN_WEBHOOK_URL or ANDROID_WEBHOOK_URL
  │   └─→ Fail fast at startup ✗
  ├─→ Invalid webhook URL
  │   └─→ Fail fast at startup ✗
  └─→ No feeds configured
      └─→ Fail fast at startup ✗

Network Errors:
  ├─→ Feed unreachable
  │   └─→ Log warning, continue ⚠️
  ├─→ Timeout (10s)
  │   └─→ Catch exception, continue ⚠️
  └─→ Discord webhook 404
      └─→ Log error, mark as failed ⚠️

Data Errors:
  ├─→ Malformed RSS
  │   └─→ Rome handles gracefully ✓
  ├─→ Missing entry fields
  │   └─→ Use defaults ✓
  └─→ Invalid dates
      └─→ Use Instant.now() ✓

File I/O Errors:
  ├─→ Cannot write sent_entries.json
  │   └─→ Log error, continue ⚠️
  ├─→ Corrupted JSON
  │   └─→ Load partial, continue ⚠️
  └─→ Permission denied
      └─→ Log error, continue ⚠️

Result Strategy:
  ├─→ Success
  │   └─→ Exit code 0 ✓
  └─→ Errors encountered
      └─→ Exit code 1 ✗
```

## Performance Metrics

```
┌─────────────────────────────────────────────────────────┐
│             Execution Timeline                         │
└─────────────────────────────────────────────────────────┘

Timeline (milliseconds):
  0ms      Program start
  100ms    Config loaded & validated
  200ms    Components initialized
  500ms    Fetch Kotlin Blog (12 entries)
  1000ms   Fetch Android Devs (25 entries)
  1100ms   Start filtering
  1150ms   Time filter complete
  1200ms   Keyword filter complete
  1250ms   Load dedup JSON
  1300ms   Dedup check (O(1) × 30)
  1400ms   Group entries
  1500ms   Send Kotlin group to Discord
  1700ms   Send Android group to Discord
  1800ms   Save dedup JSON
  1850ms   Log results
  1900ms   Exit

Total: ~1.9 seconds (for 37 entries)

Complexity Analysis:
  - Fetch:     O(n) where n = number of feeds
  - Filter:    O(m) where m = number of entries  
  - Dedup:     O(m) with O(1) lookup
  - Send:      O(m/10) batch sends
  - Persist:   O(1) append + trim

Scaling:
  - 100 entries: ~3 seconds
  - 1000 entries: ~5 seconds
  - Bottleneck: Discord API rate limits
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub.com                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Repository with Secrets                           │   │
│  │  ├─ KOTLIN_WEBHOOK_URL (encrypted)                │   │
│  │  ├─ ANDROID_WEBHOOK_URL (encrypted)               │   │
│  │  ├─ Source code                                    │   │
│  │  └─ .github/workflows/rss-aggregator.yml          │   │
│  └────────────────────────┬────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ↓               ↓               ↓
        Every 6 hours   On Pull Request  Manual trigger
        (Scheduled)        (Optional)      (Optional)
              │               │               │
              └───────────────┼───────────────┘
                              │
              ┌───────────────↓────────────────┐
              │  GitHub Actions Runner         │
              │  └─ Ubuntu Latest             │
              │     └─ JDK 21                │
              │        └─ Gradle            │
              │           └─ Kotlin Compile│
              │              └─ Execute   │
              └───────────────┬────────────┘
                              │
                              ↓
                    ┌──────────────────┐
                    │ Gradle Build     │
                    │ ├─ Compile      │
                    │ ├─ Package      │
                    │ └─ Run          │
                    └────────┬────────┘
                             │
                             ↓
                    ┌──────────────────┐
                    │   Kotlin App     │
                    │   Executes       │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          ↓                  ↓                  ↓
    ┌──────────┐      ┌───────────┐      ┌──────────┐
    │Kotlin    │      │Android    │      │Custom    │
    │Blog Feed │      │Blog Feed  │      │Feed      │
    └──────────┘      └───────────┘      └──────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                             ↓
                    ┌──────────────────┐
                    │  Discord Webhook │
                    │  (HTTPS POST)    │
                    └────────┬────────┘
                             │
                             ↓
                    ┌──────────────────┐
                    │Discord Server    │
                    │Channel Message   │
                    └──────────────────┘

Artifacts:
  ├─ Logs uploaded if failure
  │  └─ logs/app.log
  └─ No output on success
     (Clean deployment)
```

---

**Diagrams Legend:**
- `─→` = Flow/process
- `↓` = Down/next step
- `├─→` = Branch
- `✓` = Success
- `✗` = Failure
- `⚠️` = Warning/continue
