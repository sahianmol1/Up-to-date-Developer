# V3.0 Testing Guide

## Quick Test Steps

### Step 1: Set Environment Variables
```bash
export KOTLIN_WEBHOOK_URL="your-webhook-url-here"
export ANDROID_WEBHOOK_URL="your-webhook-url-here"
```

You can use the same webhook for both if you want messages from both feeds in one channel.

### Step 2: Build the Project
```bash
cd /Users/anmolsahi/IdeaProjects/UpToDateDeveloper
./gradlew clean build
```

### Step 3: Run the Aggregator
```bash
./gradlew run
```

### Step 4: Check Discord
Watch your Discord channel for messages with the new v3.0 format.

---

## What to Verify

### Expected Discord Embed Format:

Each message should appear with:

1. **Title Format**: `[emoji tagname] Original Title`
   - Examples:
     - `[🚀 Release] Kotlin 2.1 Released`
     - `[✨ Feature] Introducing New API`
     - `[⚠️ Breaking] Migration Required`
     - `[🧪 Experimental] Beta Available`
     - `[🔧 Fix] Bug Fix Resolved`
     - `[📰 News] Development Update`

2. **Color Bar**: Soft, developer-friendly color (NOT harsh red)
   - Release: Soft Blue (0x7F7FFF)
   - Feature: Soft Green (0x7FFF7F)
   - Breaking: Soft Orange (0xFFB347)
   - Experimental: Plum (0xDDA0DD)
   - Fix: Sky Blue (0x87CEEB)
   - News: Gray (0xA9A9A9)

3. **Content Fields**:
   - Description: First 100 characters of RSS entry
   - Source: Feed name (Kotlin Blog, Android Developers, etc.)
   - Published: Date/time of entry
   - Author: Entry author (if available)

4. **Routing**:
   - Kotlin Blog entries → KOTLIN_WEBHOOK_URL
   - Android Developers entries → ANDROID_WEBHOOK_URL

---

## Tag Detection Examples

The system analyzes title + description to detect tags. Here's what to expect:

### RELEASE 🚀
**Triggers:** "release" + "version", "now available", "shipped"
```
Entry: "Kotlin 2.1 Released with new features"
→ RELEASE tag, Soft Blue color
```

### FEATURE ✨
**Triggers:** "introducing", "new feature", "launch" (not beta), "support for"
```
Entry: "Introducing Jetpack Compose for Android"
→ FEATURE tag, Soft Green color
```

### BREAKING ⚠️
**Triggers:** "breaking", "incompatible", "migration required"
```
Entry: "BREAKING: Migration Guide Required for v3.0"
→ BREAKING tag, Soft Orange color
```

### EXPERIMENTAL 🧪
**Triggers:** "beta", "alpha", "preview", "experimental"
```
Entry: "Beta Preview of New Architecture"
→ EXPERIMENTAL tag, Plum color
```

### FIX 🔧
**Triggers:** "fix", "fixed", "bug" + "resolved", "patch"
```
Entry: "Bug Fix: Resolver Issue Finally Resolved"
→ FIX tag, Sky Blue color
```

### NEWS 📰
**Fallback:** Anything that doesn't match above
```
Entry: "Latest from our development team"
→ NEWS tag, Gray color
```

---

## Sample RSS Entries to Expect

When you run the aggregator, it fetches from:
1. **Kotlin Blog**: https://blog.jetbrains.com/kotlin/feed/
2. **Android Developers**: https://android-developers.googleblog.com/atom.xml

Recent entries typically include:
- Kotlin version releases and features
- Android API updates and new libraries
- Breaking changes and migration guides
- Beta features and experimental APIs
- Bug fixes and patches
- General development news

---

## Console Output to Watch For

You should see logs like:

```
2026-04-04 12:57:54 [INFO ] MainKt - Starting UpToDate Developer RSS Feed Aggregator Bot v3.0
2026-04-04 12:57:54 [INFO ] AppConfig - === Application Configuration ===
2026-04-04 12:57:54 [INFO ] AppConfig - Kotlin Webhook: SET
2026-04-04 12:57:54 [INFO ] AppConfig - Android Webhook: SET
...
2026-04-04 12:58:01 [INFO ] RssFeedAggregatorService - Starting RSS aggregation for 2 feeds
2026-04-04 12:58:02 [INFO ] RssFeedAggregatorService - Fetched 47 total entries
2026-04-04 12:58:02 [DEBUG] RssFeedAggregatorService - Calculated update tags for all entries
2026-04-04 12:58:02 [INFO ] RssFeedAggregatorService - Filtered to 12 entries after time and keyword filtering
2026-04-04 12:58:02 [INFO ] RssFeedAggregatorService - Found 3 new entries (after deduplication)
2026-04-04 12:58:03 [INFO ] DiscordWebhookClient - Routing 3 entries to Discord
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - Sent 3 entries to Discord
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - === Aggregation Results ===
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - Total Fetched: 47
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - New Entries: 3
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - Sent to Discord: 3
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - Tracked Total: 1003
```

### Key Indicators:
- ✅ "Calculated update tags for all entries" = Tags detected successfully
- ✅ "Sent N entries to Discord" = Messages sent with new format
- ✅ "Tracked Total" = Deduplication working

---

## Troubleshooting

### If you get configuration errors:
```
ERROR] MainKt - Configuration validation failed:
  - KOTLIN_WEBHOOK_URL environment variable is not set
  - ANDROID_WEBHOOK_URL environment variable is not set
```

**Fix**: Make sure to export the webhook URLs:
```bash
export KOTLIN_WEBHOOK_URL="your-webhook-url"
export ANDROID_WEBHOOK_URL="your-webhook-url"
```

### If Discord messages don't appear:
1. Check webhook URLs are correct (copy from Discord channel settings)
2. Check logs for error messages
3. Verify webhook is still active in Discord

### If tags aren't detecting correctly:
- Check entry titles in logs
- Verify tag detection keywords (see UpdateTagCalculator.kt)
- Remember: detection is case-insensitive and looks at title + description

---

## Verifying Each Feature

### ✅ Soft Colors (Not Harsh Red)
Look for the color bar on the left of Discord embeds:
- Should be soft/muted colors (blue, green, orange, purple, gray)
- NOT bright red, neon colors, or harsh colors

### ✅ Tag Emoji + Name in Title
Each title should start with emoji + tag name:
- `[🚀 Release]` not just `[High]`
- `[✨ Feature]` not just `[Medium]`
- Emoji should be immediately visible

### ✅ Correct Tag Detection
Compare entry titles with detected tags:
- "Release" in title → Should be 🚀 RELEASE
- "Introducing" in title → Should be ✨ FEATURE
- "Breaking" in title → Should be ⚠️ BREAKING

### ✅ Channel Routing
If using separate webhooks:
- Kotlin Blog entries → #kotlin channel
- Android Developers entries → #android channel

### ✅ Message Batching
- Messages appear in small batches (2 per webhook call)
- Not all at once or one at a time

---

## Next Steps After Testing

If everything works:
1. Commit the changes:
   ```bash
   git add -A
   git commit -m "v3.0: Replace importance with soft UpdateTag system

   - 6 tag types (RELEASE, FEATURE, BREAKING, EXPERIMENTAL, FIX, NEWS)
   - Soft, developer-friendly colors (0x7F7FFF, 0x7FFF7F, etc.)
   - Tag emoji + name displayed in Discord embed title
   - Intelligent keyword-based tag detection
   - No breaking changes to multi-channel routing or deduplication"
   ```

2. Push to main branch (GitHub Actions will run every 6 hours)

3. Update any internal docs/wikis with the new tag system

---

## Performance Notes

- Build time: ~2 seconds
- Runtime: Same as v2.0
- First run may send more messages (if >24 hours since last run)
- Subsequent runs: Only new entries in last 24 hours

---

## Questions?

Refer to:
- **CHANGELOG_V3.md** - Detailed feature list
- **PROJECT_SUMMARY.md** - Project overview
- **ARCHITECTURE.md** - System design
- **domain/Models.kt** - UpdateTag enum definition
- **service/UpdateTagCalculator.kt** - Tag detection logic
