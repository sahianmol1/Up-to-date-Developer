# V3.0 Quick Test (2 minutes)

## Run These Commands

```bash
# 1. Navigate to project
cd /Users/anmolsahi/IdeaProjects/UpToDateDeveloper

# 2. Set webhook URLs (replace with your actual webhook)
export KOTLIN_WEBHOOK_URL="your-webhook-url"
export ANDROID_WEBHOOK_URL="your-webhook-url"

# 3. Build
./gradlew clean build

# 4. Run
./gradlew run
```

## Watch Discord Channel For Messages Like This:

```
[🚀 Release] Kotlin 2.1 Released
We're excited to announce Kotlin 2.1 with...
Source: Kotlin Blog
Published: 2026-04-04 12:00:00
Author: JetBrains Team
```

Color bar should be **soft blue** (0x7F7FFF), NOT harsh red.

## Expected Output:

```
2026-04-04 12:58:01 [INFO ] RssFeedAggregatorService - Starting RSS aggregation for 2 feeds
2026-04-04 12:58:02 [INFO ] RssFeedAggregatorService - Fetched 47 total entries
2026-04-04 12:58:02 [INFO ] RssFeedAggregatorService - Found 3 new entries (after deduplication)
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - Sent 3 entries to Discord
```

## What to Check ✅

- [ ] Webhook URLs accepted (no config errors)
- [ ] 3-10 messages sent to Discord
- [ ] Each message has emoji + tag name in title
- [ ] Color bars are SOFT colors (not harsh red)
- [ ] Tags detected correctly (Release/Feature/Breaking/etc.)
- [ ] Messages routed correctly (if using separate webhooks)

## Tag Color Palette

| Tag | Emoji | Hex | Color |
|-----|-------|-----|-------|
| RELEASE | 🚀 | 0x7F7FFF | Soft Blue |
| FEATURE | ✨ | 0x7FFF7F | Soft Green |
| BREAKING | ⚠️ | 0xFFB347 | Soft Orange |
| EXPERIMENTAL | 🧪 | 0xDDA0DD | Plum |
| FIX | 🔧 | 0x87CEEB | Sky Blue |
| NEWS | 📰 | 0xA9A9A9 | Gray |

## If Something Goes Wrong

**Config Error**: 
```bash
export KOTLIN_WEBHOOK_URL="url"
export ANDROID_WEBHOOK_URL="url"
```

**No Messages in Discord**:
1. Check webhook URL is correct
2. Check logs for error messages
3. Try running again (may need >24 hours for new entries)

**Harsh Colors Still Showing**:
- Clear browser cache
- Refresh Discord

---

For detailed testing guide, see **TESTING_V3.md**
