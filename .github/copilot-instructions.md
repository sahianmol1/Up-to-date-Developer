# Copilot Instructions: How to Modify This Project

This document provides instructions for me (GitHub Copilot) on how to safely and effectively make changes to the UpToDate Developer RSS Feed Aggregator Bot. Use this alongside ARCHITECTURE.md and PROJECT_SUMMARY.md.

---

## 1. Code Organization & Patterns

### Project Structure (DO NOT CHANGE)
```
src/main/kotlin/com/uptodatedeveloper/
├── config/               → Configuration (env vars, defaults)
├── data/                 → RSS fetching & parsing
├── domain/               → Models & enums (FeedType, Priority, RssEntry)
├── service/              → Business logic (filtering, dedup, priority calc)
├── delivery/             → Discord API integration
└── Main.kt               → Entry point
```

### Data Flow (MUST MAINTAIN)
```
Fetch (data/)
  ↓
Filter (service/)
  ↓
Deduplicate (service/)
  ↓
Calculate Priority (service/)
  ↓
Send to Discord (delivery/)
  ↓
Mark Sent (service/)
```

### File Naming Convention
- **Data Layer**: `RssFeedFetcher.kt`
- **Service Layer**: `RssEntryFilter.kt`, `DuplicateDetector.kt`, `PriorityCalculator.kt`
- **Delivery Layer**: `DiscordWebhookClient.kt`
- **Config**: `AppConfig.kt` (singleton object)
- **Models**: `Models.kt` (all data classes and enums)

---

## 2. Making Common Changes

### ✅ How to Add a New RSS Feed

**Step 1: Add to AppConfig.kt**
```kotlin
private val defaultFeeds = listOf(
    FeedConfig(
        name = "New Feed Name",
        url = "https://example.com/feed.xml",
        feedType = FeedType.KOTLIN,  // Or ANDROID (or new type if adding)
        tags = listOf("#tag1", "#tag2")
    ),
    // ... existing feeds
)
```

**Step 2: Add environment variable support (optional)**
```
FEEDS=<json-encoded-feed-list>
```
Format in `getFeeds()` method in AppConfig.

**Step 3: Verify**
```bash
./gradlew run
# Should show: "Fetched X entries from New Feed Name"
```

### ✅ How to Add a New Discord Channel

**Step 1: Add to FeedType enum (domain/Models.kt)**
```kotlin
enum class FeedType(val displayName: String) {
    KOTLIN("Kotlin"),
    ANDROID("Android"),
    NEW_CHANNEL("New Channel")  // Add here
}
```

**Step 2: Add webhook URL to AppConfig.kt**
```kotlin
val newChannelWebhookUrl: String = System.getenv("NEW_CHANNEL_WEBHOOK_URL") ?: ""

fun getWebhookUrl(feedType: FeedType): String {
    return when (feedType) {
        FeedType.KOTLIN -> kotlinWebhookUrl
        FeedType.ANDROID -> androidWebhookUrl
        FeedType.NEW_CHANNEL -> newChannelWebhookUrl  // Add here
    }
}
```

**Step 3: Map a feed to the new channel (in AppConfig.kt)**
```kotlin
FeedConfig(
    name = "Some Feed",
    url = "https://...",
    feedType = FeedType.NEW_CHANNEL,  // Route to new channel
    tags = listOf()
)
```

**Step 4: Verify deduplication file is created**
- New file should be: `sent_entries_new_channel.json`
- Check app logs for: "Saved N new New Channel entries to sent_entries_new_channel.json"

### ✅ How to Modify Filtering Logic

**Location**: `service/RssEntryFilter.kt`

```kotlin
fun filter(entries: List<RssEntry>, keywords: List<String> = emptyList()): List<RssEntry> {
    return entries
        .filter { filterByTime(it) }        // ← Modify time window here
        .filter { filterByKeywords(it, keywords) }  // ← Modify keyword logic here
}
```

**To change time window (currently 24 hours)**:
```kotlin
private fun filterByTime(entry: RssEntry): Boolean {
    val now = Instant.now()
    val cutoff = now.minusSeconds(AppConfig.filterHours * 3600)  // Change multiplier
    return entry.publishedDate.isAfter(cutoff)
}
```

### ✅ How to Update Deduplication

**Location**: `service/DuplicateDetector.kt`

**Current behavior**: Tracks per feed type (Kotlin/Android separately)

**To change retention (currently 1000 entries per feed)**:
```kotlin
val trimmed = if (existing.size > 1000) {  // ← Change this
    existing.takeLast(1000)
} else {
    existing
}
```

**To clear a feed's history** (add new method):
```kotlin
fun clearFeedType(feedType: FeedType) {
    sentEntriesByFeedType[feedType] = mutableSetOf()
    // Also delete the file: File(getLogFilePathForFeedType(feedType)).delete()
}
```

### ✅ How to Change Priority Classification

**Location**: `service/PriorityCalculator.kt`

Current rules:
- HIGH: "release", "stable", "breaking", "major versions"
- MEDIUM: "beta", "preview", "milestone", "rc"
- LOW: everything else

To modify:
```kotlin
fun calculate(entry: RssEntry): Priority {
    val text = (entry.title + " " + entry.description).lowercase()
    
    return when {
        text.contains("new keyword") -> Priority.HIGH  // Add here
        text.contains("your pattern") -> Priority.MEDIUM
        else -> Priority.LOW
    }
}
```

**Important**: Always test with real feeds after changes:
```bash
export FILTER_HOURS=720  # 30 days
./gradlew run
# Verify priority distribution in logs
```

---

## 3. Architecture Rules (MUST FOLLOW)

### ✅ Clean Architecture Layers

**DO**: Keep concerns separated
```kotlin
// ✓ GOOD: Filter logic stays in service layer
class RssEntryFilter {
    fun filter(entries: List<RssEntry>): List<RssEntry> { ... }
}

// ✗ BAD: Don't mix Discord sending with filtering
class RssEntryFilter {
    fun filterAndSend(entries: List<RssEntry>) { ... }  // NO!
}
```

**Rule**: 
- **data/**: Only fetching and parsing. NO business logic.
- **service/**: Business logic only. NO HTTP calls, NO Discord API.
- **delivery/**: Discord API only. NO filtering, NO deduplication.
- **domain/**: Models only. NO logic.
- **config/**: Environment variables only. NO computed properties.

### ✅ Dependency Injection Pattern

**DO**: Inject dependencies through constructor
```kotlin
class RssFeedAggregatorService(
    private val fetcher: RssFeedFetcher,
    private val filter: RssEntryFilter,
    private val duplicateDetector: DuplicateDetector
) { ... }
```

**DON'T**: Create instances inside methods
```kotlin
// ✗ BAD
fun aggregate() {
    val fetcher = RssFeedFetcher()  // NO!
}
```

### ✅ Error Handling Pattern

**Use Result<T>** (from stdlib):
```kotlin
fun fetchFeed(): Result<List<RssEntry>> {
    return try {
        // ... fetch logic
        Result.success(entries)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Handle with onSuccess/onFailure**:
```kotlin
fetcher.fetchFeed()
    .onSuccess { entries -> logger.info("Fetched ${entries.size}") }
    .onFailure { error -> logger.error("Failed: ${error.message}") }
```

### ✅ Configuration Pattern

**All secrets from env vars** (AppConfig.kt):
```kotlin
val kotlinWebhookUrl: String = System.getenv("KOTLIN_WEBHOOK_URL") ?: ""
val androidWebhookUrl: String = System.getenv("ANDROID_WEBHOOK_URL") ?: ""
```

**NEVER hardcode** secrets, URLs, or credentials:
```kotlin
// ✗ BAD
val url = "https://webhook.discord.com/api/..."  // NO!

// ✓ GOOD
val url = System.getenv("KOTLIN_WEBHOOK_URL")
```

---

## 4. Code Style & Patterns

### ✅ Kotlin Idioms Used in This Project

**Data Classes** for models:
```kotlin
data class RssEntry(
    val title: String,
    val link: String,
    val publishedDate: Instant,
    val priority: Priority = Priority.LOW
)
```

**Enum Classes** with properties:
```kotlin
enum class Priority(val emoji: String, val color: Int) {
    HIGH("🔥", 0xFFB347),
    MEDIUM("⚡", 0x4A90E2),
    LOW("🌿", 0x7FFF7F)
}
```

**Extension Functions** for utility:
```kotlin
// If needed, add here
fun String.isValidUrl(): Boolean = startsWith("http")
```

**Scope Functions** for object setup:
```kotlin
val request = Request.Builder()
    .url(webhookUrl)
    .post(body.toRequestBody("application/json".toMediaType()))
    .build()
```

**Sequence & List Operations**:
```kotlin
entries
    .filter { it.publishedDate.isAfter(cutoff) }
    .groupBy { it.feedType }
    .forEach { (type, items) -> ... }
```

### ✅ Logging Standards

**Use SLF4J** (already imported):
```kotlin
private val logger = LoggerFactory.getLogger(ClassName::class.java)
```

**Log Levels**:
- **INFO**: Important events (startup, completion, stats)
- **DEBUG**: Detailed flow (filtered entries, calculated priorities)
- **WARN**: Recoverable issues (missing webhook, malformed entry)
- **ERROR**: Failures (network error, file write failed)

**Examples**:
```kotlin
logger.info("Starting RSS aggregation for ${feeds.size} feeds")
logger.debug("Filtered to ${filtered.size} entries after time and keyword filtering")
logger.warn("Invalid URL in entry: ${entry.title}")
logger.error("Failed to send Discord message", exception)
```

### ✅ Comment Requirements

**ONLY comment non-obvious code**:
```kotlin
// ✓ GOOD: Explains WHY
// Limit to 1000 entries to prevent unbounded disk growth
val trimmed = if (existing.size > 1000) existing.takeLast(1000) else existing

// ✗ BAD: Explains WHAT (obvious from code)
// Loop through entries
entries.forEach { ... }
```

**Use /** */ for functions**:
```kotlin
/**
 * Filters out entries that have already been sent
 */
fun filterNew(entries: List<RssEntry>): List<RssEntry>
```

---

## 5. Build & Verification

### ✅ Build Command
```bash
./gradlew clean build -q
# Should print: BUILD SUCCESSFUL
```

### ✅ Test Command (Run with test webhooks)
```bash
export KOTLIN_WEBHOOK_URL="https://..."
export ANDROID_WEBHOOK_URL="https://..."
./gradlew run
```

### ✅ Verification Checklist (AFTER EVERY CHANGE)

- [ ] Build succeeds: `./gradlew clean build -q`
- [ ] No compilation errors
- [ ] App starts: `./gradlew run` exits with code 0
- [ ] Logs show no ERROR or WARN (unless expected)
- [ ] If added new layer: Check dependency injection
- [ ] If modified data: Update Models.kt types
- [ ] If changed filtering: Test with FILTER_HOURS=720
- [ ] If modified Discord: Check embed payload size
- [ ] Dedup files created correctly (per feed type)

### ✅ Common Build Issues & Solutions

**Issue**: `Unresolved reference 'SomeClass'`
- Cause: Moved file, forgot import
- Solution: Re-check package path, add import statement

**Issue**: `Type mismatch: actual type is 'String?', but 'String' was expected`
- Cause: Nullable type passed to non-nullable parameter
- Solution: Use `?.` or `?:` elvis operator, or make parameter nullable

**Issue**: Build succeeds but logs show HTTP 400 from Discord
- Cause: Embed payload too large or invalid field
- Solution: Check `createEmbed()` in DiscordWebhookClient.kt, truncate fields

---

## 6. Common Mistakes to Avoid

### ❌ Hardcoding Secrets
```kotlin
// NEVER do this:
val webhook = "https://discord.com/api/webhooks/..."  // EXPOSED!

// Always use env vars:
val webhook = System.getenv("KOTLIN_WEBHOOK_URL") ?: ""
```

### ❌ Mixing Business Logic with Infrastructure
```kotlin
// ✗ BAD: Logging in domain model
data class RssEntry(...) {
    init { logger.info("Created entry: $title") }  // NO!
}

// ✓ GOOD: Logic in service layer
class RssFeedFetcher {
    fun fetch(): Result<List<RssEntry>> {
        logger.info("Fetching...")
        ...
    }
}
```

### ❌ Breaking Clean Architecture Layers
```kotlin
// ✗ BAD: Service layer imports delivery layer
import com.uptodatedeveloper.delivery.DiscordWebhookClient
class RssEntryFilter { ... }  // NO!

// ✓ GOOD: Main orchestrates, service doesn't know about delivery
class RssFeedAggregatorService(
    private val discordClient: DiscordWebhookClient  // Injected
) { ... }
```

### ❌ Not Validating Discord Payloads
```kotlin
// ❌ BAD: Send without checking field lengths
val embed = DiscordEmbed(title = veryLongTitle, ...)

// ✓ GOOD: Truncate before sending
val title = if (veryLongTitle.length > 256) 
    veryLongTitle.take(253) + "..." 
else 
    veryLongTitle
```

### ❌ Ignoring Deduplication
```kotlin
// ❌ BAD: Manually mark as sent without feed type check
duplicateDetector.markAsSent(allEntries)

// ✓ GOOD: Feed type is part of RssEntry
// DuplicateDetector automatically groups by feedType
duplicateDetector.markAsSent(entriesToSend)
```

### ❌ Adding Dependencies Without Validation
```kotlin
// ❌ BAD: Add a new library without checking
dependencies {
    implementation("com.example:new-library:1.0.0")  // Check first!
}

// Always validate:
// 1. Does it solve the problem?
// 2. Is it actively maintained?
// 3. Does it add unnecessary bloat?
```

---

## 7. Troubleshooting Guide

### Issue: HTTP 400 from Discord on Specific Entries

**Cause**: Embed payload validation error (too long, invalid characters, etc.)

**Solution** (in order):
1. Check the error log - which entry failed?
2. Verify title length < 256 characters
3. Verify description length < 4096 characters
4. Check for HTML entities: `&amp;` → `&`, `&lt;` → `<`
5. Verify URL is valid and starts with `http://` or `https://`

**Code Location**: `DiscordWebhookClient.kt` → `createEmbed()` method

### Issue: Deduplication File Not Created

**Cause**: Either entries weren't sent (filtered out) OR sending failed

**Check**:
```bash
# 1. Are entries recent enough?
export FILTER_HOURS=720  # 30 days
./gradlew run

# 2. Check logs for:
# - "Filtered to X entries"
# - "Sent Y entries to Discord"
# - "Recovered Z embeds"

# 3. If 0 entries sent, check:
# - Are webhooks configured? (export KOTLIN_WEBHOOK_URL=...)
# - Are there no recent entries? (check FILTER_HOURS)
# - Did Discord reject all? (check logs for HTTP 400)
```

### Issue: Build Fails with "Execution failed"

**Step 1**: Clean and retry
```bash
./gradlew clean build -q
```

**Step 2**: Check for syntax errors
```bash
./gradlew compileKotlin
```

**Step 3**: Look for dependency issues
```bash
./gradlew dependencies
```

### Issue: App Runs but No Messages Sent

**Diagnosis**:
1. **No entries fetched**: RSS feeds unreachable
   - Check logs: "Fetched X entries from..."
   - Check network connectivity

2. **Entries filtered out**: All entries older than 24 hours
   - Use `export FILTER_HOURS=720` for testing
   - Or check if feeds are stale

3. **All entries are duplicates**: Already sent before
   - Check `sent_entries_*.json` files
   - Delete them to reset: `rm sent_entries_*.json`

4. **Discord webhook unreachable**: Invalid URL or Discord down
   - Verify webhook URL is correct
   - Check logs for HTTP errors

### Issue: Dedup Not Working (Sending Duplicates)

**Cause**: Files not being written OR not loaded correctly

**Fix**:
1. Verify files exist and have entries
2. Check file names: `sent_entries_kotlin.json`, `sent_entries_android.json`
3. Restart app (reload files on init)
4. Check logs for "Loaded X previously sent entries"

**For GitHub Actions**: 
- Verify cache is being restored: Check workflow run logs
- Cache expires after 7 days of no use - manually trigger workflow if idle
- See `DEDUPLICATION_GUIDE.md` for full troubleshooting

---

## 8. GitHub Actions & Deduplication (CRITICAL)

### Why Caching is Essential

GitHub Actions runs on a schedule. Each run:
1. Checks out fresh repo
2. `sent_entries_*.json` files don't exist (not committed to git)
3. App sends all new entries to Discord
4. Files are created locally but lost after run ends
5. Next run: Files don't exist → Sends same entries again (duplicates)

**Solution**: Cache persists dedup files between runs:

```yaml
- name: Cache deduplication files
  uses: actions/cache@v3
  with:
    path: sent_entries_*.json
    key: rss-dedup-cache
    restore-keys: |
      rss-dedup-cache
```

### Workflow Configuration

**See `.github/workflows/rss-aggregator.yml`:**
- ✅ Cache step is included
- ✅ `FILTER_HOURS=24` (optimal for 6-hour runs)
- ✅ Secrets used for webhook URLs (not hardcoded)
- ✅ Weekly log retention on failures

**Setup required in GitHub UI:**
1. Go to **Settings → Secrets and variables → Actions**
2. Add secret `KOTLIN_WEBHOOK_URL`
3. Add secret `ANDROID_WEBHOOK_URL`

⚠️ **Never commit webhook URLs to git!**

### Full Reference

See `DEDUPLICATION_GUIDE.md` for complete information:
- How cache works
- Scheduling tuning
- Troubleshooting
- Performance notes

---

## 9. Making a Safe Change: Checklist

**BEFORE starting**:
- [ ] Understand current behavior (read relevant code)
- [ ] Know which layers will be affected
- [ ] Have a rollback plan (git branch)

**DURING changes**:
- [ ] Change ONE thing at a time
- [ ] Keep architecture layers separate
- [ ] Use Result<T> for error handling
- [ ] Add appropriate logging (INFO/DEBUG)
- [ ] Update related comments

**AFTER changes**:
- [ ] Build succeeds (`./gradlew clean build -q`)
- [ ] Run succeeds (`./gradlew run` → exit code 0)
- [ ] Verify behavior (check logs)
- [ ] Commit with descriptive message

**Example Commit Message**:
```
Add support for custom webhook timeout

- Extend AppConfig to read DISCORD_WEBHOOK_TIMEOUT from env
- Increase default timeout from 10s to 30s for slower networks
- Update DiscordWebhookClient to use configured timeout
- Test: Verified with slow network (simulated 5s delay)
- No breaking changes to existing API

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

---

## 10. Quick Reference: File Modification Guide

| Feature | File | Method | Difficulty |
|---------|------|--------|------------|
| Add RSS feed | `AppConfig.kt` | Add to `defaultFeeds` list | ⭐ Easy |
| Add Discord channel | `Models.kt` + `AppConfig.kt` | Add FeedType enum + webhook | ⭐⭐ Medium |
| Change filter window | `RssEntryFilter.kt` | Modify `filterByTime()` | ⭐ Easy |
| Change priority rules | `PriorityCalculator.kt` | Modify `calculate()` keyword patterns | ⭐⭐ Medium |
| Change dedup retention | `DuplicateDetector.kt` | Modify `trimmed` limit | ⭐ Easy |
| Add new error recovery | `DiscordWebhookClient.kt` | Extend `sendMessage()` fallback | ⭐⭐⭐ Hard |
| Add analytics | `RssFeedAggregatorService.kt` | Extend `logResults()` | ⭐⭐ Medium |

---

## 11. Final Notes

**Respect the Design**: This project uses clean architecture intentionally. Don't "optimize" by mixing layers - the current structure is already optimal.

**Test Everything**: Always run the app with real feeds and webhooks before considering a change complete.

**Document Decisions**: If you make a non-obvious change, explain WHY in comments and commit messages.

**Backward Compatibility**: When possible, keep old behavior as defaults. New features should be opt-in via env vars.

**Remember the Goals**:
- ✓ Prevent duplicate messages
- ✓ Route to correct channels
- ✓ Format beautifully
- ✓ Run automatically
- ✓ Fail gracefully

Good luck with future improvements! 🚀
