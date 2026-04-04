# V3.0 Reference Card (Quick Lookup)

## 🚀 Quick Commands

```bash
# Set webhooks
export KOTLIN_WEBHOOK_URL="https://discord.com/api/webhooks/ID/TOKEN"
export ANDROID_WEBHOOK_URL="https://discord.com/api/webhooks/ID/TOKEN"

# Build
cd /Users/anmolsahi/IdeaProjects/UpToDateDeveloper
./gradlew clean build

# Run
./gradlew run

# Commit when done
git add -A
git commit -m "v3.0: Soft UpdateTag system tested"
git push origin main
```

---

## 🎨 Color Palette

| Tag | Emoji | Hex | Color Name |
|-----|-------|-----|-----------|
| RELEASE | 🚀 | 0x7F7FFF | Soft Blue |
| FEATURE | ✨ | 0x7FFF7F | Soft Green |
| BREAKING | ⚠️ | 0xFFB347 | Soft Orange |
| EXPERIMENTAL | 🧪 | 0xDDA0DD | Plum |
| FIX | 🔧 | 0x87CEEB | Sky Blue |
| NEWS | 📰 | 0xA9A9A9 | Gray |

---

## 📋 Discord Message Format

```
[emoji tagname] Original Entry Title
┌─────────────────────────────────┐
│ First 100 chars of description  │
│ ...                             │
├─────────────────────────────────┤
│ Source:    Feed Name            │
│ Published: YYYY-MM-DD HH:MM:SS  │
│ Author:    Author Name          │
└─────────────────────────────────┘
[Color Bar: tag.color]
```

---

## 🔍 Tag Detection Keywords

**RELEASE 🚀:** "release" + "version", "now available", "shipped"

**FEATURE ✨:** "introducing", "new feature", "launch" (not beta), "support for"

**BREAKING ⚠️:** "breaking", "incompatible", "migration required"

**EXPERIMENTAL 🧪:** "beta", "alpha", "preview", "experimental"

**FIX 🔧:** "fix", "fixed", "bug" + "resolved", "patch"

**NEWS 📰:** Everything else (default)

---

## ✅ Testing Checklist

- [ ] Build successful (no errors)
- [ ] Messages appear in Discord
- [ ] Title has emoji + tag name: `[🚀 Release]`
- [ ] Color bars are SOFT (not harsh red)
- [ ] Tags match content (release → 🚀, introducing → ✨, etc.)
- [ ] All fields present (Source, Published, Author)
- [ ] Multiple messages appear
- [ ] Correct webhook routing (if separate webhooks)

---

## 📚 Documentation Map

| Document | Purpose | Time |
|----------|---------|------|
| TESTING_INDEX.md | Roadmap & overview | 5 min |
| QUICK_TEST.md | 2-minute quick start | 2 min |
| VISUAL_TESTING_GUIDE.md | Message examples | 3 min |
| TESTING_V3.md | Full step-by-step | 5-10 min |
| CHANGELOG_V3.md | Technical details | 5 min |

---

## 🛠️ What Changed

| Component | Change | Impact |
|-----------|--------|--------|
| Models.kt | Importance → UpdateTag | Tag types increased 3→6 |
| UpdateTagCalculator | NEW file | Intelligent tag detection |
| DiscordWebhookClient | Emoji + soft colors | Better visual presentation |
| RssFeedAggregatorService | Integration | Tags flow through pipeline |
| RssFeedFetcher | Default tag | Initialize with UpdateTag.NEWS |
| Main.kt | v3.0 version | Component setup |

---

## 🚨 Common Issues

| Problem | Solution |
|---------|----------|
| Config validation errors | Export webhook URLs: `export KOTLIN_WEBHOOK_URL="..."`  |
| No Discord messages | Check webhook URL, check console logs, try again |
| Colors still harsh/red | Clear cache, refresh Discord, restart app |
| Tags all showing as NEWS | Check entry titles, review UpdateTagCalculator logic |
| Build fails | Run `./gradlew clean` then `./gradlew build` |

---

## 📊 Expected Output

```
[INFO] RssFeedAggregatorService - Starting RSS aggregation for 2 feeds
[INFO] RssFeedFetcher - ✓ Fetched 24 entries from Kotlin Blog
[INFO] RssFeedFetcher - ✓ Fetched 23 entries from Android Developers
[DEBUG] RssFeedAggregatorService - Calculated update tags for all entries
[INFO] RssFeedAggregatorService - Found 3 new entries
[INFO] DiscordWebhookClient - Routing 3 entries to Discord
[INFO] RssFeedAggregatorService - Sent 3 entries to Discord
[INFO] RssFeedAggregatorService - === Aggregation Results ===
[INFO] RssFeedAggregatorService - Total Fetched: 47
[INFO] RssFeedAggregatorService - New Entries: 3
[INFO] RssFeedAggregatorService - Sent to Discord: 3
```

---

## 🎯 Success Indicators

✅ **You know it's working when:**
1. Build completes (1 second)
2. No errors in console
3. 3-10 messages in Discord
4. Emoji + tag name visible
5. Soft colors (not red)
6. Tags match content

---

## 📂 Key Files

**Code:**
- `src/main/kotlin/.../domain/Models.kt` (UpdateTag enum)
- `src/main/kotlin/.../service/UpdateTagCalculator.kt` (tag detection)
- `src/main/kotlin/.../delivery/DiscordWebhookClient.kt` (display)

**Docs:**
- `TESTING_INDEX.md` ← START HERE
- `QUICK_TEST.md` (2-min overview)
- `VISUAL_TESTING_GUIDE.md` (examples)
- `TESTING_V3.md` (full guide)
- `CHANGELOG_V3.md` (technical)

---

## 🔗 Quick Links

| Need | Do This |
|------|---------|
| Quick overview | Read QUICK_TEST.md |
| See examples | Read VISUAL_TESTING_GUIDE.md |
| Detailed steps | Read TESTING_V3.md |
| Tech details | Read CHANGELOG_V3.md |
| Full roadmap | Read TESTING_INDEX.md |

---

## 💡 Pro Tips

1. **First Run:** May have more messages (all from last 24 hours)
2. **Subsequent Runs:** Only new entries since last run
3. **Colors:** Takes a moment to render in Discord
4. **Webhook:** One URL can handle both feed types
5. **Tags:** Case-insensitive, analyzes title + description

---

## 🎉 You're Set!

Everything is ready to test. Follow TESTING_INDEX.md for full roadmap.

**Main goal:** Verify soft colors and emoji tags work in Discord.

**Then:** Commit, push, and GitHub Actions runs every 6 hours!

Good luck! 🚀
