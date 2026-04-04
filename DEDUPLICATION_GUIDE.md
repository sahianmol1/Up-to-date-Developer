# Deduplication & GitHub Actions Guide

## How Deduplication Works

The RSS Feed Aggregator prevents sending duplicate messages to Discord using a **link-based tracking system**.

### Architecture

```
RSS Entry → Fetch → Filter by Time → Check Dedup → Send to Discord → Save Link
                                         ↓
                            sent_entries_<feedtype>.json
```

### Files

- **`sent_entries_kotlin.json`** - Tracks all Kotlin Blog entries sent (up to 1000)
- **`sent_entries_android.json`** - Tracks all Android Developer entries sent (up to 1000)

Each file is a JSON array of `SentEntryLog` objects:

```json
[
  {
    "link": "https://blog.jetbrains.com/kotlin/2026/04/example/",
    "sentAt": "2026-04-04T11:55:10.082991Z",
    "title": "Example Post Title"
  }
]
```

### How It Works

1. **On Startup**: Load all tracked links from JSON files into memory
2. **Fetch Entries**: Get all entries from RSS feeds
3. **Filter by Time**: Keep only entries from last N hours (default: 24)
4. **Filter Duplicates**: Check if entry link is in tracked set
   - ✅ New link → Include in send batch
   - ❌ Duplicate link → Skip (don't send)
5. **Send to Discord**: Send new entries as embeds
6. **Save to File**: Add sent entry links to JSON file for next run

### Limits

- **Per feed type**: Tracks last 1000 entries
- **Persistence**: Between application restarts (local file system)
- **Time window**: Only entries from last 24 hours are considered (configurable)

---

## GitHub Actions & Deduplication

### The Challenge

GitHub Actions runs on a schedule (every 6 hours). Each run:
1. Checks out a fresh copy of the repo
2. Runs the aggregator
3. Exits

**Problem**: `sent_entries_*.json` files are not committed to git, so they're lost after each run. The next scheduled run won't know what was sent before → **duplicate messages to Discord**.

### The Solution: GitHub Actions Cache

The workflow now uses `actions/cache@v3` to persist dedup files **between workflow runs**:

```yaml
- name: Cache deduplication files
  uses: actions/cache@v3
  with:
    path: sent_entries_*.json
    key: rss-dedup-cache
    restore-keys: |
      rss-dedup-cache
```

**How it works:**

1. **First run**: Dedup files don't exist
   - App creates them with sent entries
   - Cache saves them for next run
   
2. **Subsequent runs**: Cache is restored
   - App loads dedup files
   - Skips entries already sent
   - Updates files with new entries
   - Cache is updated with new data

3. **Cache expires**: If not used for 7 days, GitHub automatically deletes it
   - Safe: Next run will just resend entries from last 24 hours

### Benefits

✅ **No git pollution** - Dedup files never committed to repo  
✅ **Automatic cleanup** - GitHub deletes stale cache after 7 days  
✅ **Resilient** - If cache is lost, app works normally (resends recent entries)  
✅ **Free** - Part of GitHub's free tier (up to 10GB)

---

## Configuration for GitHub Actions

### Environment Variables

The workflow sets these environment variables:

```yaml
KOTLIN_WEBHOOK_URL: ${{ secrets.KOTLIN_WEBHOOK_URL }}
ANDROID_WEBHOOK_URL: ${{ secrets.ANDROID_WEBHOOK_URL }}
FILTER_HOURS: 24          # Only fetch entries from last 24 hours
KEYWORDS: "Kotlin,Compose,Android,Coroutine,KMP"  # Optional filtering
```

### Secrets Setup

You need to add two secrets to your GitHub repository:

1. Go to **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Add:
   - **Name:** `KOTLIN_WEBHOOK_URL`
     **Value:** Your Kotlin Discord webhook URL
   - **Name:** `ANDROID_WEBHOOK_URL`
     **Value:** Your Android Discord webhook URL

⚠️ **NEVER commit webhook URLs to git!** Always use GitHub Secrets.

---

## Scheduling & Frequency

### Current Schedule

```yaml
cron: '0 0,6,12,18 * * *'
```

Runs at:
- 00:00 UTC (midnight)
- 06:00 UTC
- 12:00 UTC (noon)
- 18:00 UTC

**Every 6 hours = 4 runs per day**

### Time Window

`FILTER_HOURS: 24` means:
- Only fetch entries published in the last 24 hours
- Combined with dedup: Never sends same entry twice within 24 hours
- If cache is lost: Will resend entries from last 24 hours

### Tuning

**More frequent runs** (every 3 hours):
```yaml
cron: '0 0,3,6,9,12,15,18,21 * * *'
```

**Less frequent** (every 12 hours):
```yaml
cron: '0 0,12 * * *'
```

**Real-time** (not recommended - GitHub limits):
```yaml
schedule:
  - cron: '*/15 * * * *'  # Every 15 minutes (5760 runs/month)
```

---

## Troubleshooting

### Issue: Duplicate Messages in Discord

**Possible causes:**

1. **Cache expired** (7+ days without run)
   - Solution: Run manually (`workflow_dispatch`)
   - Next run will resend entries from last 24 hours only

2. **Cache deleted manually**
   - Solution: Same as above

3. **Dedup file corrupted**
   - Solution: Delete cache, app will recreate fresh files

**To manually trigger:**

1. Go to **Actions → RSS Feed Aggregator → Run workflow**
2. Click the **Run workflow** button

### Issue: Workflow Failing

Check the workflow logs:

1. Go to **Actions → RSS Feed Aggregator**
2. Click the failed run
3. Expand the **Run RSS Feed Aggregator** step
4. Look for error messages

Common issues:
- ❌ Invalid webhook URL in secrets
- ❌ Webhook URL expired
- ❌ Discord API rate limiting (unlikely with 4 runs/day)

### Issue: No Messages Sent

Check:
1. Is `FILTER_HOURS=24` set in workflow? (Yes)
2. Are there new entries in last 24 hours? (Check RSS feeds directly)
3. Are webhook URLs valid? (Test locally first)
4. Is the app reaching Discord? (Check Discord audit logs)

---

## Local Testing vs. GitHub Actions

### Local Testing

```bash
export KOTLIN_WEBHOOK_URL="your-webhook"
export ANDROID_WEBHOOK_URL="your-webhook"
export FILTER_HOURS=720  # 30 days for testing
./gradlew run
```

**Dedup files** are saved locally and persist between runs.

### GitHub Actions

```yaml
env:
  KOTLIN_WEBHOOK_URL: ${{ secrets.KOTLIN_WEBHOOK_URL }}
  ANDROID_WEBHOOK_URL: ${{ secrets.ANDROID_WEBHOOK_URL }}
  FILTER_HOURS: 24
```

**Dedup files** are cached between runs (GitHub Actions cache).

### Key Difference

| Aspect | Local | GitHub Actions |
|--------|-------|----------------|
| Dedup Storage | Local filesystem | GitHub Actions cache |
| Persistence | Until you delete file | 7 days (auto-expire) |
| History | Cumulative | Limited to 1000 entries |
| Cost | Free | Free (10GB tier) |

---

## Performance Considerations

### Cache Size

- Each dedup file: ~1-5 KB per 100 entries
- Max tracked per feed: 1000 entries
- Estimated cache size: **10-50 KB total**
- GitHub free tier: **10 GB available**

Cache is tiny compared to limits.

### Execution Time

- Typical run: **20-30 seconds**
- Breakdown:
  - Checkout: 2-3s
  - Build: 8-12s
  - Run: 5-8s
  - Cache operations: 1-2s

### Cost

**Free** under GitHub's free tier (4 runs/day × 30 seconds = 2 mins/day).

---

## Best Practices

### 1. Use Strict Time Windows

✅ **Good**: `FILTER_HOURS=24` (default)
- Prevents stale entries
- Reduces dedup file size
- Natural cleanup

❌ **Avoid**: `FILTER_HOURS=720+` (30+ days)
- Dedup files grow large
- Old entries never cleaned up

### 2. Rotate Webhook URLs Periodically

Discord webhooks don't expire, but it's good practice to regenerate them quarterly.

### 3. Monitor Logs

GitHub Actions keeps logs for 90 days. Review occasionally to catch issues.

### 4. Test Locally First

Before relying on GitHub Actions:
1. Test locally with real webhooks
2. Verify dedup works (run twice, check for duplicates)
3. Then deploy to GitHub Actions

### 5. Manual Trigger for Testing

Use `workflow_dispatch` to test without waiting for schedule:

```yaml
on:
  schedule:
    - cron: '0 0,6,12,18 * * *'
  workflow_dispatch:  # ← Allows manual trigger
```

---

## Diagram: Full Flow

```
┌─────────────────────────────────────────────────────────────┐
│           GitHub Actions Scheduled Run (Every 6h)           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  1. Checkout repo                                           │
│  2. Restore cache → sent_entries_*.json (from prior runs)   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  3. Build project                                           │
│  4. Run aggregator                                          │
│     - Load dedup files from filesystem                      │
│     - Fetch RSS feeds                                       │
│     - Filter by FILTER_HOURS=24                            │
│     - Skip entries already in dedup file                    │
│     - Send new entries to Discord                          │
│     - Save links to dedup file                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  5. Save cache → updated sent_entries_*.json                │
│  6. Next run (6 hours later) restores this cache            │
└─────────────────────────────────────────────────────────────┘
```

---

## Questions?

Refer to `.github/copilot-instructions.md` for general project changes.  
For dedup-specific questions, this guide covers all scenarios.
