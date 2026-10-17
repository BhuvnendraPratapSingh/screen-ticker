# SocialWatch — Social Media Screen Time Tracker

An Android app that automatically detects when you're using social media apps, tracks your screen time, and helps you build healthier digital habits.

## Features

- **Auto-detect social media apps** — Monitors when Instagram, Facebook, Twitter/X, Reddit (or any custom app) is in the foreground
- **Live notification** — Persistent notification showing the current app and running daily total time
- **Dashboard** — Circular progress ring (iOS Screen Time-inspired) with per-app breakdown
- **Charts** — Daily (hourly), weekly, and monthly usage bar charts
- **Custom app tracking** — Add any installed app to your tracking list
- **Time limits** — Set daily limits per app with notification + overlay warning
- **Auto-start on boot** — Monitoring starts automatically when device turns on
- **Dark mode** — Full dark mode support with iOS-inspired design

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM (ViewModel + Kotlin Flows)
- **Database:** Room (local-only)
- **DI:** Hilt (Dagger)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

## Setup Instructions

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Hedgehog 2023.1.1 or later)
- JDK 17
- Android SDK 34

### Build Steps

1. **Open in Android Studio:**
   - Open Android Studio → File → Open → select the `Mob_app` folder
   - Android Studio will detect the Gradle build files and configure the project
   - Wait for Gradle sync to complete (this downloads dependencies, ~200-300 MB on first run)

2. **If Gradle wrapper is missing:**
   - Android Studio may prompt you to configure the Gradle wrapper — click "OK" or "Fix"
   - Or run from terminal: `gradle wrapper --gradle-version 8.4`

3. **Build the project:**
   - Build → Make Project (or Ctrl+F9 / Cmd+F9)

4. **Run on device/emulator:**
   - Select a device running Android 8.0+ (API 26+)
   - Click Run (or Shift+F10)

### Permissions Required on Device

The app will request these permissions at appropriate times:
1. **Usage Access** — To detect which app is in the foreground (redirects to system settings)
2. **Notifications** (Android 13+) — To show the persistent monitoring notification
3. **Display Over Apps** — To show time limit warning overlays (requested when first limit is set)

## Building for Play Store

1. **Generate a signing keystore:**
   - Build → Generate Signed Bundle/APK → Create new keystore
   - **IMPORTANT:** Back up your `.jks` keystore file! You need it for all future updates.

2. **Build release AAB:**
   - Build → Generate Signed Bundle/APK → Android App Bundle (AAB)
   - Select your keystore and build
   - Output: `app/release/app-release.aab` (~8-12 MB)

3. **Upload to Google Play Console:**
   - Go to [play.google.com/console](https://play.google.com/console)
   - Create a new app → Upload the `.aab` file
   - Fill in the store listing, screenshots, and privacy policy
   - Submit for review

## Project Structure

```
app/src/main/java/com/socialwatch/app/
├── SocialWatchApp.kt           # Hilt Application + notification channels
├── MainActivity.kt             # Entry point + Compose setup
├── ui/
│   ├── theme/                  # iOS-inspired theme (colors, typography)
│   ├── components/             # Reusable composables (SWCard, SWSwitch, etc.)
│   ├── navigation/             # Bottom nav + NavGraph
│   └── screens/
│       ├── dashboard/          # Main screen with progress ring + app list
│       ├── stats/              # Charts screen (daily/weekly/monthly)
│       ├── manageapps/         # Add/remove tracked apps
│       ├── settings/           # App settings
│       ├── permission/         # Usage access permission flow
│       └── onboarding/         # First-launch walkthrough
├── data/
│   ├── db/                     # Room database, entities, DAOs
│   └── repository/             # UsageStats API wrapper
├── di/                         # Hilt dependency injection modules
├── service/                    # Foreground monitoring service
├── receiver/                   # Boot completed broadcast receiver
└── util/                       # Permission helpers, formatters, constants
```

## License

This project is proprietary. All rights reserved.
