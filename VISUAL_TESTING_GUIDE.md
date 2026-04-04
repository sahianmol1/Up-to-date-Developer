# V3.0 Visual Testing Guide

## What Discord Messages Should Look Like

### BEFORE V3.0 (v2.0 - Harsh Colors)
```
Title: Kotlin 2.1 Released

Description: We're excited to announce...

Footer: "High | Kotlin Blog"    ← Severity-based
Color: HARSH RED (0xFF0000)     ← Alarming!
```

### AFTER V3.0 (New - Soft Colors & Tags)
```
Title: [🚀 Release] Kotlin 2.1 Released    ← Emoji + Tag!

Description: We're excited to announce...

Source: Kotlin Blog
Published: 2026-04-04 12:00:00
Author: JetBrains Team

Color: SOFT BLUE (0x7F7FFF)     ← Professional!
```

---

## The 6 Message Types You'll See

### 1️⃣ RELEASE 🚀 (Soft Blue - 0x7F7FFF)
```
[🚀 Release] Kotlin 2.1 Released
We're excited to announce...
Source: Kotlin Blog
Published: 2026-04-04 09:15:00
Author: JetBrains
[SOFT BLUE COLOR BAR]
```
**Triggers:** "release" + "version", "now available"

### 2️⃣ FEATURE ✨ (Soft Green - 0x7FFF7F)
```
[✨ Feature] Introducing Jetpack Compose for Android
A new way to build Android UIs...
Source: Android Developers
Published: 2026-04-03 14:20:00
Author: Google
[SOFT GREEN COLOR BAR]
```
**Triggers:** "introducing", "new feature", "launch", "support for"

### 3️⃣ BREAKING ⚠️ (Soft Orange - 0xFFB347)
```
[⚠️ Breaking] Migration Required for v3.0
This incompatible change requires migration...
Source: Kotlin Blog
Published: 2026-04-02 10:30:00
Author: Language Team
[SOFT ORANGE COLOR BAR]
```
**Triggers:** "breaking", "incompatible", "migration required"

### 4️⃣ EXPERIMENTAL 🧪 (Plum - 0xDDA0DD)
```
[🧪 Experimental] Beta Preview of New Platform
Try our beta version...
Source: Kotlin Blog
Published: 2026-04-01 16:45:00
Author: JetBrains
[PLUM COLOR BAR]
```
**Triggers:** "beta", "alpha", "preview", "experimental"

### 5️⃣ FIX 🔧 (Sky Blue - 0x87CEEB)
```
[🔧 Fix] Bug Fix: Resolver Issue Resolved
We've fixed the critical issue...
Source: Android Developers
Published: 2026-03-31 11:00:00
Author: Google
[SKY BLUE COLOR BAR]
```
**Triggers:** "fix", "fixed", "bug" + "resolved", "patch"

### 6️⃣ NEWS 📰 (Gray - 0xA9A9A9)
```
[📰 News] Latest Updates from Development Team
This week we've been working on...
Source: Kotlin Blog
Published: 2026-03-30 08:22:00
Author: Dev Team
[GRAY COLOR BAR]
```
**Triggers:** Everything else (default)

---

## Color Comparison

| V2.0 (OLD) | V3.0 (NEW) | Hex | Note |
|-----------|-----------|-----|------|
| Harsh Red | Soft Blue | 0x7F7FFF | Release - Professional |
| Bright Orange | Soft Green | 0x7FFF7F | Feature - Fresh |
| Neon Green | Soft Orange | 0xFFB347 | Breaking - Warm |
| | Plum | 0xDDA0DD | Experimental - Creative |
| | Sky Blue | 0x87CEEB | Fix - Light |
| | Gray | 0xA9A9A9 | News - Neutral |

**Key Difference:** All V3.0 colors are SOFT and professional, not harsh or alarming.

---

## Testing Checklist

When you run the test, check for ALL of these:

### ✅ Title Format
- [ ] Messages start with `[emoji tagname]`
- [ ] Example: `[🚀 Release] Kotlin 2.1 Released`
- [ ] NOT just `Kotlin 2.1 Released`

### ✅ Color Bars (Most Important!)
- [ ] Colors are SOFT (not harsh/neon)
- [ ] Blue is muted/soft, not bright
- [ ] Green is mint-like, not neon
- [ ] Orange is peachy, not bright
- [ ] No harsh reds or alarming colors

### ✅ Emoji Visibility
- [ ] Each message shows an emoji before tag name
- [ ] 🚀 Release, ✨ Feature, ⚠️ Breaking, etc.
- [ ] Emoji is clearly visible and correctly matched

### ✅ Tag Detection Accuracy
- [ ] "Release" in title → 🚀 RELEASE tag
- [ ] "Introducing" in title → ✨ FEATURE tag
- [ ] "Breaking" in title → ⚠️ BREAKING tag
- [ ] "Beta" in title → 🧪 EXPERIMENTAL tag
- [ ] "Fix" in title → 🔧 FIX tag
- [ ] Generic news → 📰 NEWS tag (default)

### ✅ Message Fields
- [ ] All fields present (Source, Published, Author)
- [ ] Description shows first 100 characters
- [ ] Published date is readable
- [ ] Author name (if available) shows

### ✅ Message Batching
- [ ] Messages appear in batches of 2
- [ ] Not all at once
- [ ] Not one at a time

---

## Expected Console Output

```
2026-04-04 12:58:01 [INFO ] RssFeedAggregatorService - Starting RSS aggregation
2026-04-04 12:58:02 [INFO ] RssFeedFetcher - ✓ Fetched 24 entries from Kotlin Blog
2026-04-04 12:58:03 [INFO ] RssFeedFetcher - ✓ Fetched 23 entries from Android Developers
2026-04-04 12:58:03 [INFO ] RssFeedAggregatorService - Fetched 47 total entries
2026-04-04 12:58:03 [DEBUG] RssFeedAggregatorService - Calculated update tags for all entries
2026-04-04 12:58:03 [INFO ] RssFeedAggregatorService - Filtered to 12 entries
2026-04-04 12:58:04 [INFO ] RssFeedAggregatorService - Found 3 new entries
2026-04-04 12:58:04 [INFO ] DiscordWebhookClient - Routing 3 entries to Discord
2026-04-04 12:58:05 [INFO ] DiscordWebhookClient - Sending 2 entries to Kotlin webhook
2026-04-04 12:58:05 [INFO ] DiscordWebhookClient - Sending 1 entries to Android webhook
2026-04-04 12:58:06 [INFO ] RssFeedAggregatorService - Sent 3 entries to Discord
2026-04-04 12:58:06 [INFO ] RssFeedAggregatorService - === Aggregation Results ===
2026-04-04 12:58:06 [INFO ] RssFeedAggregatorService - Total Fetched: 47
2026-04-04 12:58:06 [INFO ] RssFeedAggregatorService - New Entries: 3
2026-04-04 12:58:06 [INFO ] RssFeedAggregatorService - Sent to Discord: 3
```

**Key Signs of Success:**
- ✅ "Calculated update tags" = Tags working
- ✅ "Routing N entries to Discord" = Correct path
- ✅ "Sent N entries to Discord" = Messages sent
- ✅ Multiple webhooks called = Multi-channel routing

---

## Quick Reference: Tag Keywords

| Tag | Emoji | Keywords |
|-----|-------|----------|
| RELEASE | 🚀 | release+version, now available, shipped |
| FEATURE | ✨ | introducing, new feature, launch, support for |
| BREAKING | ⚠️ | breaking, incompatible, migration required |
| EXPERIMENTAL | 🧪 | beta, alpha, preview, experimental |
| FIX | 🔧 | fix, fixed, bug+resolved, patch |
| NEWS | 📰 | default (everything else) |

---

## Troubleshooting

### Problem: No Discord messages appear
**Solution:**
1. Check webhook URL is correct (copy from Discord settings)
2. Check no errors in console logs
3. Try running again (may need >24 hours for new entries)
4. Check webhook is still active in Discord

### Problem: Colors still harsh/red
**Solution:**
1. Clear Discord browser cache (Ctrl+Shift+Delete)
2. Refresh Discord (F5)
3. Close and reopen Discord app
4. Check that code uses 0x7F7FFF (not 0xFF0000)

### Problem: Tags not detected correctly
**Solution:**
1. Check title/description keywords match tag triggers
2. Check UpdateTagCalculator.kt for detection logic
3. Watch console logs for actual entry titles
4. Verify tag detection order in code

### Problem: Config validation errors
**Solution:**
```bash
export KOTLIN_WEBHOOK_URL="your-webhook-url"
export ANDROID_WEBHOOK_URL="your-webhook-url"
```

Make sure URLs start with `https://discord.com/api/webhooks/`

---

## Success Indicators

✅ **You'll Know V3.0 Works When:**

1. Build completes with no errors
2. Console shows "Calculated update tags for all entries"
3. Discord shows 3+ messages with emoji+tag in title
4. Color bars are SOFT colors (not harsh red)
5. Tags match entry content (Release=version, Feature=introducing, etc.)
6. Each message has all fields (Source, Published, Author)

❌ **If You See These, Something's Wrong:**

- No emoji in titles
- Harsh red color bars
- Messages not appearing at all
- Config validation errors about webhooks
- Tags all showing as "News" (detection not working)

---

## Next Steps After Successful Test

1. **Commit Changes:**
   ```bash
   git add -A
   git commit -m "v3.0: Soft UpdateTag system tested and verified"
   ```

2. **Push to Main:**
   ```bash
   git push origin main
   ```

3. **GitHub Actions Will Auto-Run:**
   - Every 6 hours automatically
   - Uses production Discord webhook
   - Updates continue to your channel

4. **Monitor Results:**
   - Check Discord daily for new entries
   - Verify tags are correctly detected
   - Enjoy the soft colors!

---

For detailed setup, see **TESTING_V3.md**
For quick test steps, see **QUICK_TEST.md**
