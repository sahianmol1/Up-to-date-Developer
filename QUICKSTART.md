# Quick Start Guide

## 5-Minute Setup

### 1. Get Discord Webhook URLs
- Open your Discord server
- Settings → Integrations → Webhooks
- Create separate webhooks for Kotlin and Android channels
- Copy both URLs

### 2. Clone & Setup
```bash
git clone <repo>
cd UpToDateDeveloper

# Copy example env file
cp .env.example .env

# Edit .env with your Discord webhook URLs
nano .env
```

### 3. Run Locally
```bash
# Set environment variables
export KOTLIN_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
export ANDROID_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"

# Option A: Using Gradle
./gradlew run

# Option B: Build jar and run
./gradlew build
java -jar build/libs/UpToDateDeveloper-1.0.0.jar
```

### 4. Setup GitHub Actions (Optional)
1. Go to your repo → Settings → Secrets and variables → Actions
2. Add secrets: `KOTLIN_WEBHOOK_URL` and `ANDROID_WEBHOOK_URL`
3. Workflow automatically runs every 6 hours

## Testing

### Test with extended time window:
```bash
export KOTLIN_WEBHOOK_URL="https://discord.com/api/webhooks/..."
export ANDROID_WEBHOOK_URL="https://discord.com/api/webhooks/..."
export FILTER_HOURS=720  # 30 days instead of 24 hours
./gradlew run
```

### Test with keyword filter:
```bash
export KEYWORDS="Kotlin,Compose"
./gradlew run
```

## Logs

View logs after running:
```bash
tail -f logs/app.log
```

## Duplicate Prevention

Previously sent entries are stored in `sent_entries.json`. To reset:
```bash
rm sent_entries.json
```

## Custom Feeds

Edit the feeds in `src/main/kotlin/com/uptodatedeveloper/config/AppConfig.kt` or use env var:

```bash
export FEEDS="Kotlin:https://blog.jetbrains.com/kotlin/feed/:KOTLIN;Android:https://android-developers.googleblog.com/atom.xml:ANDROID"
```

## Troubleshooting

### "Discord webhook not configured"
- Check: `echo $KOTLIN_WEBHOOK_URL && echo $ANDROID_WEBHOOK_URL`
- Both should output their respective webhook URLs

### "No new entries"
- Time filter might be too strict (default: 24 hours)
- Try: `export FILTER_HOURS=720`

### "Build errors"
- Ensure JDK 21: `java -version`
- Clean: `./gradlew clean build`

## Production Deployment

### Docker (Optional)
```dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon
CMD java -jar build/libs/UpToDateDeveloper-1.0.0.jar
```

### GitHub Actions (Already Configured)
- Runs every 6 hours (0, 6, 12, 18 UTC)
- View runs: Actions tab in GitHub
- Edit schedule in `.github/workflows/rss-aggregator.yml`

## Next Steps

- [ ] Add your Discord webhook URLs
- [ ] Run `./gradlew run` to test
- [ ] Review logs in `logs/app.log`
- [ ] Push to GitHub
- [ ] Add `KOTLIN_WEBHOOK_URL` and `ANDROID_WEBHOOK_URL` secrets in GitHub
- [ ] Check Discord channels for updates

---

Need help? Check:
- README.md for full documentation
- logs/app.log for error details
- GitHub Issues to report problems
